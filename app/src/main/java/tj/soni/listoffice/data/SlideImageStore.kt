package tj.soni.listoffice.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.LruCache
import java.io.File
import java.io.FileOutputStream

/** Суратҳои слайдҳо: ҳангоми илова кардан хурд (то 1280px) ва ба JPEG табдил дода мешаванд. */
class SlideImageStore(private val context: Context) {

    private val dir: File
        get() = File(context.filesDir, "slide_images").also { if (!it.exists()) it.mkdirs() }

    private val cache = LruCache<String, Bitmap>(6)

    fun file(name: String): File = File(dir, name)

    fun bitmap(name: String): Bitmap? {
        if (name.isBlank()) return null
        cache.get(name)?.let { return it }
        val f = file(name)
        if (!f.exists()) return null
        val b = BitmapFactory.decodeFile(f.path) ?: return null
        cache.put(name, b)
        return b
    }

    fun bytes(name: String): ByteArray? {
        val f = file(name)
        return if (name.isNotBlank() && f.exists()) f.readBytes() else null
    }

    fun size(name: String): Pair<Int, Int> {
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file(name).path, o)
        return Pair(o.outWidth, o.outHeight)
    }

    fun delete(name: String) {
        if (name.isBlank()) return
        cache.remove(name)
        file(name).delete()
    }

    /** Суратро аз [uri] мехонад, мечархонад (EXIF), хурд мекунад ва нигоҳ медорад. Номи файлро бармегардонад. */
    fun importFrom(uri: Uri): String {
        val cr = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IllegalStateException("Сурат хонда нашуд")
        var sample = 1
        val maxSide = maxOf(bounds.outWidth, bounds.outHeight)
        while (maxSide / sample > 2560) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        var bmp = cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            ?: throw IllegalStateException("Сурат хонда нашуд")

        val orientation = try {
            cr.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (e: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
        val deg = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (deg != 0f) {
            val m = Matrix().apply { postRotate(deg) }
            bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
        }
        val ms = maxOf(bmp.width, bmp.height)
        if (ms > 1280) {
            val k = 1280f / ms
            bmp = Bitmap.createScaledBitmap(
                bmp, (bmp.width * k).toInt().coerceAtLeast(1), (bmp.height * k).toInt().coerceAtLeast(1), true
            )
        }
        if (bmp.hasAlpha()) {
            val flat = Bitmap.createBitmap(bmp.width, bmp.height, Bitmap.Config.ARGB_8888)
            flat.eraseColor(Color.WHITE)
            Canvas(flat).drawBitmap(bmp, 0f, 0f, null)
            bmp = flat
        }
        val name = "img_${System.currentTimeMillis()}.jpg"
        FileOutputStream(file(name)).use { bmp.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        return name
    }
}
