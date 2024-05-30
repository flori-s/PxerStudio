package com.benny.pxerstudio.shape.draw

import android.graphics.Bitmap
import androidx.core.graphics.ColorUtils
import com.benny.pxerstudio.shape.BaseShape
import com.benny.pxerstudio.widget.PxerView

open class DrawShape : BaseShape() {
    protected open fun drawOnLayer(layerToDraw: Bitmap, pxerView: PxerView, x: Int, y: Int) {
        if (x >= 0 && y >= 0 && x < layerToDraw.width && y < layerToDraw.height) {
            val color = pxerView.pxerLayers[pxerView.currentLayer]!!.bitmap?.getPixel(x, y)
            layerToDraw.setPixel(
                x,
                y,
                ColorUtils.compositeColors(
                    pxerView.selectedColor,
                    color ?: pxerView.selectedColor,
                ),
            )
        }
    }

    protected open fun addPxerView(
        previousPxer: ArrayList<PxerView.Pxer>,
        pxerView: PxerView,
        x: Int,
        y: Int,
    ) {
        if (x >= 0 && y >= 0 && x < pxerView.width && y < pxerView.height) {
            val color = pxerView.pxerLayers[pxerView.currentLayer]!!.bitmap?.getPixel(x, y)
            color?.let { PxerView.Pxer(x, y, it) }?.let { previousPxer.add(it) }
        }
    }

    protected fun draw(layerToDraw: Bitmap, previousPxer: ArrayList<PxerView.Pxer>) {
        for (i in previousPxer.indices) {
            val (x, y, color) = previousPxer[i]
            layerToDraw.setPixel(x, y, color)
        }
        previousPxer.clear()
    }

    override fun drawLine(
        layerToDraw: Bitmap,
        previousPxer: ArrayList<PxerView.Pxer>,
        pxerView: Int,
        startX: Int,
        startY: Int,
        endX: Int
    ) {
        TODO("Not yet implemented")
    }
}
