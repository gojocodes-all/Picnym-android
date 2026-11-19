package ng.name.gojodev.picnym.data

import ng.name.gojodev.picnym.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private val JSON_BODY = "application/json; charset=utf-8".toMediaType()
private val EMPTY_BODY = ByteArray(0).toRequestBody(null)

data class SendResult(val id: String, val poll: Poll? = null)

class PicnymApi(
    private val store: SessionStore,
    private val authRepository: AuthRepository
) {
    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.toString())

    private suspend fun call(
        path: String,
        method: String = "GET",
        body: RequestBody? = null,
        authRequired: Boolean = true,
        attachSessionWhenPublic: Boolean = true
    ): JSONObject {
        var session = store.current()
        if (authRequired && !session.signedIn) throw ApiException(401, "Sign in required.")

        suspend fun execute(token: String?): NetResponse {
            val builder = Request.Builder().url(BuildConfig.API_BASE + path)
                .header("Accept", "application/json")
                .header("X-PICNYM-Client", "android-native/1")
            if (!token.isNullOrBlank()) builder.header("Authorization", "Bearer $token")
            when (method) {
                "GET" -> builder.get()
                "POST" -> builder.post(body ?: EMPTY_BODY)
                "PATCH" -> builder.patch(body ?: EMPTY_BODY)
                "DELETE" -> if (body == null) builder.delete() else builder.delete(body)
                else -> builder.method(method, body)
            }
            return NetClient.execute(builder.build())
        }

        var token = if (authRequired || attachSessionWhenPublic) session.accessToken.takeIf { it.isNotBlank() } else null
        var response = execute(token)
        if (response.status == 401 && session.refreshToken.isNotBlank()) {
            val refreshed = authRepository.refresh()
            if (refreshed != null) {
                session = refreshed
                token = refreshed.accessToken
                response = execute(token)
            }
        }
        return response.requireSuccess()
    }

    private fun jsonBody(block: JSONObject.() -> Unit): RequestBody {
        val json = JSONObject().apply(block)
        return json.toString().toRequestBody(JSON_BODY)
    }

    suspend fun account(): AccountData = accountFromJson(call("/api/account"))

    suspend fun updateProfile(displayName: String, username: String, bio: String): Profile {
        val json = call("/api/account/profile", "PATCH", jsonBody {
            put("displayName", displayName)
            put("username", username)
            put("bio", bio)
        })
        return profileFromJson(json.optJSONObject("profile")) ?: Profile()
    }

    suspend fun updateAccountSettings(settings: AccountSettings): AccountSettings {
        val json = call("/api/account/settings", "PATCH", jsonBody {
            put("theme", settings.theme)
            put("discoverable", settings.discoverable)
            put("allowFriendRequests", settings.allowFriendRequests)
            put("showActivity", settings.showActivity)
            put("browserNotifications", settings.browserNotifications)
        })
        return accountSettingsFromJson(json.optJSONObject("settings"))
    }

    suspend fun uploadAvatar(file: File, mime: String): Profile {
        val body = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("avatar", file.name, file.asRequestBody(mime.toMediaTypeOrNull()))
            .build()
        val json = call("/api/account/avatar", "POST", body)
        return profileFromJson(json.optJSONObject("profile")) ?: Profile()
    }

    suspend fun removeAvatar(): Profile {
        val json = call("/api/account/avatar", "DELETE")
        return profileFromJson(json.optJSONObject("profile")) ?: Profile()
    }

    suspend fun deleteAccount() {
        call("/api/account", "DELETE")
        store.clearSession()
    }

    suspend fun analytics(): JSONObject = call("/api/account/analytics")

    suspend fun createInbox(displayName: String, handle: String): Inbox {
        val json = call("/api/inboxes", "POST", jsonBody {
            put("displayName", displayName)
            put("handle", handle)
        })
        return inboxFromJson(json)
    }

    suspend fun publicInbox(slug: String): Inbox = inboxFromJson(
        call("/api/inboxes/${encode(slug)}", authRequired = false, attachSessionWhenPublic = true)
    )

    suspend fun updateInbox(oldSlug: String, newSlug: String, displayName: String): Inbox {
        val json = call("/api/inboxes/${encode(oldSlug)}", "PATCH", jsonBody {
            put("slug", newSlug)
            put("displayName", displayName)
        })
        return inboxFromJson(json)
    }

    suspend fun updateInboxSettings(slug: String, settings: InboxSettings): InboxSettings {
        val json = call("/api/inboxes/${encode(slug)}/settings", "PATCH", jsonBody {
            put("paused", settings.paused)
            put("registeredOnly", settings.registeredOnly)
            put("friendsOnly", settings.friendsOnly)
            put("allowImages", settings.allowImages)
            put("allowVoice", settings.allowVoice)
            put("allowPolls", settings.allowPolls)
            put("hiddenWords", JSONArray(settings.hiddenWords))
        })
        return inboxSettingsFromJson(json.optJSONObject("settings"))
    }

    suspend fun deleteInbox(slug: String) {
        call("/api/inboxes/${encode(slug)}", "DELETE")
    }

    suspend fun messages(slug: String, before: String? = null): InboxMessages {
        val suffix = buildString {
            append("?limit=80")
            if (!before.isNullOrBlank()) append("&before=${encode(before)}")
        }
        return inboxMessagesFromJson(call("/api/inboxes/${encode(slug)}/messages$suffix"))
    }

    suspend fun polls(slug: String): List<Poll> {
        val json = call("/api/inboxes/${encode(slug)}/polls?limit=100")
        val array = json.optJSONArray("polls") ?: JSONArray()
        return buildList {
            for (i in 0 until array.length()) pollFromJson(array.optJSONObject(i))?.let(::add)
        }
    }

    private fun messageMultipart(kind: String, revealProfile: Boolean): MultipartBody.Builder =
        MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("kind", kind)
            .apply { if (revealProfile) addFormDataPart("revealProfile", "1") }

    suspend fun sendText(slug: String, text: String, revealProfile: Boolean): SendResult {
        val body = messageMultipart("text", revealProfile).addFormDataPart("text", text).build()
        return send(slug, body)
    }

    suspend fun sendImage(slug: String, file: File, mime: String, caption: String, revealProfile: Boolean): SendResult {
        val body = messageMultipart("image", revealProfile)
            .addFormDataPart("text", caption)
            .addFormDataPart("image", file.name, file.asRequestBody(mime.toMediaTypeOrNull()))
            .build()
        return send(slug, body)
    }

    suspend fun sendVoice(slug: String, file: File, mime: String, revealProfile: Boolean): SendResult {
        val body = messageMultipart("voice", revealProfile)
            .addFormDataPart("voice", file.name, file.asRequestBody(mime.toMediaTypeOrNull()))
            .build()
        return send(slug, body)
    }

    suspend fun sendPoll(slug: String, question: String, options: List<String>, revealProfile: Boolean): SendResult {
        val body = messageMultipart("poll", revealProfile)
            .addFormDataPart("question", question)
            .addFormDataPart("options", JSONArray(options).toString())
            .build()
        return send(slug, body)
    }

    private suspend fun send(slug: String, body: RequestBody): SendResult {
        val json = call(
            "/api/inboxes/${encode(slug)}/messages",
            "POST",
            body,
            authRequired = false,
            attachSessionWhenPublic = true
        )
        return SendResult(json.optString("id"), pollFromJson(json.optJSONObject("poll")))
    }

    suspend fun reply(messageId: String, reply: String) {
        call("/api/messages/${encode(messageId)}/reply", "POST", jsonBody { put("reply", reply) })
    }

    suspend fun blockSender(messageId: String) {
        call("/api/messages/${encode(messageId)}/block", "POST")
    }

    suspend fun deleteMessage(messageId: String) {
        call("/api/messages/${encode(messageId)}", "DELETE")
    }

    suspend fun setMessageState(messageId: String, favorite: Boolean? = null, archived: Boolean? = null, isPublic: Boolean? = null) {
        call("/api/messages/${encode(messageId)}/state", "PATCH", jsonBody {
            favorite?.let { put("favorite", it) }
            archived?.let { put("archived", it) }
            isPublic?.let { put("isPublic", it) }
        })
    }

    suspend fun reportMessage(messageId: String, reason: String, details: String) {
        call("/api/messages/${encode(messageId)}/report", "POST", jsonBody {
            put("reason", reason)
            put("details", details)
        })
    }

    suspend fun friends(): FriendBuckets = friendsFromJson(call("/api/friends"))

    suspend fun searchUsers(query: String): List<Profile> {
        val json = call("/api/users/search?q=${encode(query)}")
        val array = json.optJSONArray("users") ?: JSONArray()
        return buildList {
            for (i in 0 until array.length()) profileFromJson(array.optJSONObject(i))?.let(::add)
        }
    }

    suspend fun requestFriend(username: String) {
        call("/api/friends/request", "POST", jsonBody { put("username", username) })
    }

    suspend fun acceptFriend(userId: String) {
        call("/api/friends/${encode(userId)}/accept", "POST")
    }

    suspend fun removeFriend(userId: String) {
        call("/api/friends/${encode(userId)}", "DELETE")
    }

    suspend fun publicProfile(username: String): PublicProfilePage = publicProfilePageFromJson(
        call("/api/profiles/${encode(username)}", authRequired = false, attachSessionWhenPublic = true)
    )

    suspend fun poll(slug: String): Poll {
        return pollFromJson(call("/api/polls/${encode(slug)}", authRequired = false, attachSessionWhenPublic = false))
            ?: throw ApiException(404, "Poll not found.")
    }

    suspend fun vote(slug: String, optionId: String, clientId: String): Poll {
        val json = call(
            "/api/polls/${encode(slug)}/vote",
            "POST",
            jsonBody { put("optionId", optionId); put("clientId", clientId) },
            authRequired = false,
            attachSessionWhenPublic = false
        )
        return pollFromJson(json.optJSONObject("poll")) ?: throw ApiException(500, "Invalid poll response.")
    }
}
