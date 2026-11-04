package com.usagetracker.app.data.datasource

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.LruCache

/**
 * In-memory LRU cache of app icons. Compose reads via [getDrawable].
 * INFERRED — NOT VERIFIED.
 */
class AppIconCache private constructor(private val context: Context) {

    private val cache = LruCache<String, Bitmap>(MAX_BYTES)
    private val pm: PackageManager get() = context.packageManager

    fun getDrawable(packageName: String): Drawable? {
        return try {
            val ai = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationIcon(ai)
        } catch (t: Throwable) { null }
    }

    fun getBitmap(packageName: String): Bitmap? {
        cache.get(packageName)?.let { return it }
        val drawable = getDrawable(packageName) ?: return null
        val bmp = drawableToBitmap(drawable, ICON_PX)
        cache.put(packageName, bmp)
        return bmp
    }

    fun clear() = cache.evictAll()

    private fun drawableToBitmap(drawable: Drawable, sizePx: Int): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return Bitmap.createScaledBitmap(drawable.bitmap, sizePx, sizePx, true)
        }
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bmp
    }

    companion object {
        private const val MAX_BYTES = 8 * 1024 * 1024 // 8 MB
        private const val ICON_PX = 96

        @Volatile private var INSTANCE: AppIconCache? = null
        fun get(context: Context): AppIconCache =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppIconCache(context.applicationContext).also { INSTANCE = it }
            }
    }
}
