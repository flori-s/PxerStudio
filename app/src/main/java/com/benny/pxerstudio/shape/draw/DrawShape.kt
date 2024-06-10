package com.benny.pxerstudio.shape.draw

import android.graphics.Bitmap
import androidx.core.graphics.ColorUtils
import com.benny.pxerstudio.shape.BaseShape
import com.benny.pxerstudio.widget.PxerView

open class DrawShape : BaseShape() {
    // A list of Pxer objects that represent the previous state of the PxerView.
    protected open fun drawOnLayer(layerToDraw: Bitmap, pxerView: PxerView, x: Int, y: Int) {
       // Check if the specified coordinates are within the bounds of the layer.
        if (x >= 0 && y >= 0 && x < layerToDraw.width && y < layerToDraw.height) {
            // Get the color of the pixel at the specified coordinates.
            val color = pxerView.pxerLayers[pxerView.currentLayer]!!.bitmap?.getPixel(x, y)
            layerToDraw.setPixel(
                x,
                y,
                ColorUtils.compositeColors(
                    pxerView.selectedColor,
                    // If the pixel is not transparent, use the selected color.
                    color ?: pxerView.selectedColor,
                ),
            )
        }
    }

    // Add the specified Pxer object to the list of previous Pxer objects.
    protected open fun addPxerView(
        previousPxer: ArrayList<PxerView.Pxer>,
        // The PxerView on which to draw the shape.
        pxerView: PxerView,
        x: Int,
        y: Int,
    ) {
        // Check if the specified coordinates are within the bounds of the PxerView.
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

    // Draw a line on the PxerView.
    override fun drawLine(
        // The layer on which to draw the line.
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
