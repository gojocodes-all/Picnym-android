package ng.name.gojodev.picnym.data

import org.json.JSONArray
import org.json.JSONObject

private fun JSONObject.text(name: String): String = if (isNull(name)) "" else optString(name, "")
private fun JSONObject.nullable(name: String): String? = text(name).takeIf { it.isNotBlank() }

data class Profile(
    val userId: String = "",
    val username: String = "",
    val displayName: String = "PICNYM user",
    val bio: String = "",
    val avatarUrl: String? = null,
    val premium: Boolean = false,
    val plan: String = "free"
)

data class AccountSettings(
    val theme: String = "system",
    val discoverable: Boolean = true,
    val allowFriendRequests: Boolean = true,
    val showActivity: Boolean = false,
    val browserNotifications: Boolean = false
)

data class InboxSettings(
    val paused: Boolean = false,
    val registeredOnly: Boolean = false,
    val friendsOnly: Boolean = false,
    val allowImages: Boolean = true,
    val allowVoice: Boolean = true,
    val allowPolls: Boolean = true,
    val hiddenWords: List<String> = emptyList()
)

data class Inbox(
    val id: String = "",
    val slug: String = "",
    val displayName: String = "",
    val handle: String = "",
    val createdAt: String = "",
    val messageCount: Int = 0,
    val settings: InboxSettings = InboxSettings(),
    val profile: Profile? = null
)

data class AccountStats(val inboxes: Int = 0, val totalMessages: Int = 0, val friends: Int = 0)
data class Billing(val plan: String = "free", val premiumPriceNgn: Int = 500, val paymentsEnabled: Boolean = false)

data class AccountData(
    val email: String = "",
    val profile: Profile = Profile(),
    val settings: AccountSettings = AccountSettings(),
    val stats: AccountStats = AccountStats(),
    val billing: Billing = Billing(),
    val inboxes: List<Inbox> = emptyList()
)

data class PollOption(val id: String, val text: String, val votes: Int)
data class Poll(
    val id: String,
    val slug: String,
    val question: String,
    val totalVotes: Int,
    val options: List<PollOption>,
    val createdAt: String = ""
)

data class Message(
    val id: String,
    val kind: String,
    val text: String,
    val imageUrl: String? = null,
    val voiceUrl: String? = null,
    val reply: String = "",
    val createdAt: String = "",
    val isBlocked: Boolean = false,
    val favorite: Boolean = false,
    val archived: Boolean = false,
    val isPublic: Boolean = false,
    val senderProfile: Profile? = null,
    val poll: Poll? = null
)

data class InboxMessages(
    val inbox: Inbox,
    val messages: List<Message>,
    val hasMore: Boolean = false,
    val nextCursor: String? = null
)

data class FriendEntry(val id: String, val profile: Profile)
data class FriendBuckets(
    val incoming: List<FriendEntry> = emptyList(),
    val outgoing: List<FriendEntry> = emptyList(),
    val accepted: List<FriendEntry> = emptyList()
)

data class PublicProfilePage(
    val profile: Profile,
    val friendCount: Int,
    val publicAnswerCount: Int,
    val posts: List<Message>
)

fun profileFromJson(o: JSONObject?): Profile? {
    if (o == null) return null
    return Profile(
        userId = o.text("userId"),
        username = o.text("username"),
        displayName = o.text("displayName").ifBlank { "PICNYM user" },
        bio = o.text("bio"),
        avatarUrl = o.nullable("avatarUrl"),
        premium = o.optBoolean("premium", false),
        plan = o.text("plan").ifBlank { if (o.optBoolean("premium", false)) "premium" else "free" }
    )
}

fun accountSettingsFromJson(o: JSONObject?): AccountSettings = AccountSettings(
    theme = o?.text("theme")?.ifBlank { "system" } ?: "system",
    discoverable = o?.optBoolean("discoverable", true) ?: true,
    allowFriendRequests = o?.optBoolean("allowFriendRequests", true) ?: true,
    showActivity = o?.optBoolean("showActivity", false) ?: false,
    browserNotifications = o?.optBoolean("browserNotifications", false) ?: false
)

fun inboxSettingsFromJson(o: JSONObject?): InboxSettings {
    val hidden = mutableListOf<String>()
    val array = o?.optJSONArray("hiddenWords")
    if (array != null) for (i in 0 until array.length()) hidden += array.optString(i)
    return InboxSettings(
        paused = o?.optBoolean("paused", false) ?: false,
        registeredOnly = o?.optBoolean("registeredOnly", false) ?: false,
        friendsOnly = o?.optBoolean("friendsOnly", false) ?: false,
        allowImages = o?.optBoolean("allowImages", true) ?: true,
        allowVoice = o?.optBoolean("allowVoice", true) ?: true,
        allowPolls = o?.optBoolean("allowPolls", true) ?: true,
        hiddenWords = hidden.filter { it.isNotBlank() }
    )
}

fun inboxFromJson(o: JSONObject?): Inbox {
    if (o == null) return Inbox()
    return Inbox(
        id = o.text("id"),
        slug = o.text("slug"),
        displayName = o.text("displayName"),
        handle = o.text("handle"),
        createdAt = o.text("createdAt"),
        messageCount = o.optInt("messageCount", 0),
        settings = inboxSettingsFromJson(o.optJSONObject("settings")),
        profile = profileFromJson(o.optJSONObject("profile"))
    )
}

fun pollFromJson(o: JSONObject?): Poll? {
    if (o == null || o.text("id").isBlank()) return null
    val options = mutableListOf<PollOption>()
    val array = o.optJSONArray("options") ?: JSONArray()
    for (i in 0 until array.length()) {
        val x = array.optJSONObject(i) ?: continue
        options += PollOption(x.text("id"), x.text("text"), x.optInt("votes", 0))
    }
    return Poll(
        id = o.text("id"), slug = o.text("slug"), question = o.text("question"),
        totalVotes = o.optInt("totalVotes", 0), options = options, createdAt = o.text("createdAt")
    )
}

fun messageFromJson(o: JSONObject): Message = Message(
    id = o.text("id"),
    kind = o.text("kind").ifBlank { "text" },
    text = o.text("text"),
    imageUrl = o.nullable("imageUrl"),
    voiceUrl = o.nullable("voiceUrl"),
    reply = o.text("reply"),
    createdAt = o.text("createdAt"),
    isBlocked = o.optBoolean("isBlocked", false),
    favorite = o.optBoolean("favorite", false),
    archived = o.optBoolean("archived", false),
    isPublic = o.optBoolean("isPublic", false),
    senderProfile = profileFromJson(o.optJSONObject("senderProfile")),
    poll = pollFromJson(o.optJSONObject("poll"))
)

fun accountFromJson(o: JSONObject): AccountData {
    val inboxes = mutableListOf<Inbox>()
    val array = o.optJSONArray("inboxes") ?: JSONArray()
    for (i in 0 until array.length()) inboxes += inboxFromJson(array.optJSONObject(i))
    val stats = o.optJSONObject("stats")
    val billing = o.optJSONObject("billing")
    return AccountData(
        email = o.optJSONObject("user")?.text("email").orEmpty(),
        profile = profileFromJson(o.optJSONObject("profile")) ?: Profile(),
        settings = accountSettingsFromJson(o.optJSONObject("settings")),
        stats = AccountStats(stats?.optInt("inboxes", inboxes.size) ?: inboxes.size, stats?.optInt("totalMessages", 0) ?: 0, stats?.optInt("friends", 0) ?: 0),
        billing = Billing(billing?.text("plan")?.ifBlank { "free" } ?: "free", billing?.optInt("premiumPriceNgn", 500) ?: 500, billing?.optBoolean("paymentsEnabled", false) ?: false),
        inboxes = inboxes
    )
}

fun inboxMessagesFromJson(o: JSONObject): InboxMessages {
    val messages = mutableListOf<Message>()
    val array = o.optJSONArray("messages") ?: JSONArray()
    for (i in 0 until array.length()) array.optJSONObject(i)?.let { messages += messageFromJson(it) }
    return InboxMessages(inboxFromJson(o.optJSONObject("inbox")), messages, o.optBoolean("hasMore", false), o.nullable("nextCursor"))
}

fun friendsFromJson(o: JSONObject): FriendBuckets {
    fun list(name: String): List<FriendEntry> {
        val out = mutableListOf<FriendEntry>()
        val a = o.optJSONArray(name) ?: JSONArray()
        for (i in 0 until a.length()) {
            val row = a.optJSONObject(i) ?: continue
            val p = profileFromJson(row.optJSONObject("profile")) ?: continue
            out += FriendEntry(row.text("id"), p)
        }
        return out
    }
    return FriendBuckets(list("incoming"), list("outgoing"), list("accepted"))
}

fun publicProfilePageFromJson(o: JSONObject): PublicProfilePage {
    val stats = o.optJSONObject("stats")
    val posts = mutableListOf<Message>()
    val a = o.optJSONArray("posts") ?: JSONArray()
    for (i in 0 until a.length()) a.optJSONObject(i)?.let { posts += messageFromJson(it) }
    return PublicProfilePage(
        profile = profileFromJson(o.optJSONObject("profile")) ?: Profile(),
        friendCount = stats?.optInt("friends", 0) ?: 0,
        publicAnswerCount = stats?.optInt("publicAnswers", posts.size) ?: posts.size,
        posts = posts
    )
}
