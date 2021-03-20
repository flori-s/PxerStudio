package com.benny.pxerstudio.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Environment
import android.text.InputType
import android.util.DisplayMetrics
import android.util.Log
import android.widget.Toast
import com.afollestad.materialdialogs.MaterialDialog
import com.benny.pxerstudio.R
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter

/**
 * Created by BennyKok on 10/6/2016.
 */
object Tool {
    @JvmField
    val myType = Typeface.create("sans-serif-light", Typeface.NORMAL)
    fun print(o: Any) {
        Log.d("Hey", o.toString())
    }

    fun print(vararg o: Any) {
        var result = ""
        for (i in 0 until o.size) {
            result += " " + o[i].toString()
        }
        Log.d("Hey", result)
    }

    @JvmStatic
    fun toast(context: Context?, content: String?) {
        Toast.makeText(context, content, Toast.LENGTH_SHORT).show()
    }

    @JvmStatic
    fun drawableToBitmap(drawable: Drawable): Bitmap? {
        var bitmap: Bitmap? = null
        if (drawable is BitmapDrawable) {
            val bitmapDrawable = drawable
            if (bitmapDrawable.bitmap != null) {
                return bitmapDrawable.bitmap
            }
        }
        bitmap = if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
            Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888) // Single color bitmap will be created of 1x1 pixel
        } else {
            Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        }
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    @JvmStatic
    fun saveProject(name: String?, data: String?, context: Context) {
        val dirs = File(context.getExternalFilesDir("/")!!.path + "/PxerStudio/Project")
        if (!dirs.exists()) {
            dirs.mkdirs()
        }
        try {
            val outputStreamWriter = OutputStreamWriter(FileOutputStream(File(dirs, name)))
            outputStreamWriter.write(data)
            outputStreamWriter.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @JvmStatic
    fun stripExtension(str: String?): String? {
        if (str == null) return null
        val pos = str.lastIndexOf(".")
        return if (pos == -1) str else str.substring(0, pos)
    }

    @JvmStatic
    fun convertDpToPixel(dp: Float, context: Context): Float {
        val resources = context.resources
        val metrics = resources.displayMetrics
        return dp * (metrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
    }

    @JvmStatic
    fun freeMemory() {
        System.runFinalization()
        Runtime.getRuntime().gc()
        System.gc()
    }

    fun trimLongString(str: String): String {
        return if (str.length > 25) {
            "..." + str.substring(str.length - 21, str.length)
        } else str
    }

    @JvmStatic
    fun prompt(c: Context?): MaterialDialog {
        return MaterialDialog(c!!)
                .negativeButton(R.string.cancel)
//                .titleGravity(GravityEnum.CENTER)
//                .typeface(myType, myType)
//                .positiveColor(Color.RED)
    }

//    fun promptTextInput(c: Context?, title: String?): MaterialDialog {
//        return MaterialDialog(c!!)
//                .negativeButton(R.string.cancel)
//                .positiveButton(R.string.ok)
//                .title(null,title)
//                .inputType(InputType.TYPE_CLASS_TEXT)
//                .inputRange(0, 20)
//                .titleGravity(GravityEnum.CENTER)
//                .typeface(myType, myType)
//                .positiveColor(Color.GREEN)
//    }
}