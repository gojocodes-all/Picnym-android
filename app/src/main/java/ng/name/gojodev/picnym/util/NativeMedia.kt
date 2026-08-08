package ng.name.gojodev.picnym.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import ng.name.gojodev.picnym.BuildConfig
import ng.name.gojodev.picnym.data.Message
import java.io.File
import java.io.FileOutputStream

fun copyUriToCache(context: Context, uri: Uri, prefix: String): Pair<File, String> {
    val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
    val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "bin"
    val dir = File(context.cacheDir, "media").apply { mkdirs() }
    val file = File(dir, "${prefix}_${System.currentTimeMillis()}.$ext")
    context.contentResolver.openInputStream(uri).use { input ->
        requireNotNull(input) { "Could not open selected file." }
        FileOutputStream(file).use { output -> input.copyTo(output) }
    }
    return file to mime
}

class NativeVoiceRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    var file: File? = null
        private set

    @Suppress("DEPRECATION")
    fun start() {
        stop(discard = true)
        val dir = File(context.cacheDir, "media").apply { mkdirs() }
        val out = File(dir, "voice_${System.currentTimeMillis()}.m4a")
        val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()
        r.setAudioSource(MediaRecorder.AudioSource.MIC)
        r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        r.setAudioEncodingBitRate(96_000)
        r.setAudioSamplingRate(44_100)
        r.setOutputFile(out.absolutePath)
        r.prepare()
        r.start()
        recorder = r
        file = out
    }

    fun stop(discard: Boolean = false): File? {
        val r = recorder
        recorder = null
        if (r != null) {
            runCatching { r.stop() }
            runCatching { r.reset() }
            runCatching { r.release() }
        }
        val result = file
        if (discard) {
            result?.delete()
            file = null
            return null
        }
        return result
    }
}

fun shareText(context: Context, text: String) {
    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }, "Share with"))
}

fun openUrl(context: Context, url: String) {
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}

object ShareCardRenderer {
    private const val WIDTH = 1080
    private const val HEIGHT = 1350

    private fun block(canvas: Canvas, text: String, x: Int, y: Int, width: Int, size: Float, color: Int, bold: Boolean = false) {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
            typeface = if (bold) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
        }
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setIncludePad(false)
            .setLineSpacing(0f, 1.12f)
            .build()
        canvas.save()
        canvas.translate(x.toFloat(), y.toFloat())
        layout.draw(canvas)
        canvas.restore()
    }

    fun share(context: Context, message: Message, inboxName: String) {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        canvas.drawColor(Color.rgb(241, 242, 237))
        paint.color = Color.rgb(18, 38, 75)
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), 170f, paint)
        paint.color = Color.rgb(185, 201, 178)
        canvas.drawRect(0f, 170f, WIDTH.toFloat(), 184f, paint)
        block(canvas, "PICNYM", 70, 54, 900, 54f, Color.WHITE, true)
        block(canvas, "ANONYMOUS", 70, 118, 900, 23f, Color.WHITE, true)

        paint.color = Color.WHITE
        canvas.drawRoundRect(65f, 245f, 1015f, 715f, 28f, 28f, paint)
        block(canvas, "ANONYMOUS MESSAGE", 105, 290, 850, 24f, Color.rgb(82, 106, 86), true)
        val body = when {
            message.poll != null -> message.poll.question
            message.text.isNotBlank() -> message.text
            message.voiceUrl != null -> "Voice note"
            message.imageUrl != null -> "Image message"
            else -> "Anonymous message"
        }
        block(canvas, body, 105, 365, 850, 42f, Color.rgb(21, 27, 38), true)

        if (message.reply.isNotBlank()) {
            paint.color = Color.rgb(223, 233, 220)
            canvas.drawRoundRect(65f, 775f, 1015f, 1165f, 28f, 28f, paint)
            block(canvas, "${inboxName.uppercase()} REPLIED", 105, 820, 850, 23f, Color.rgb(82, 106, 86), true)
            block(canvas, message.reply, 105, 885, 850, 39f, Color.rgb(21, 27, 38), true)
        }

        block(canvas, "anonymous.gojodev.name.ng", 70, 1265, 650, 22f, Color.rgb(110, 118, 118), true)

        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, "picnym_${message.id}.png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        val uri = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.fileprovider", file)
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }, "Share PICNYM card"))
    }
}
