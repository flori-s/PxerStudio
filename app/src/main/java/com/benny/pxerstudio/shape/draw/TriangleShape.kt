package com.benny.pxerstudio.shape.draw

import android.graphics.Bitmap
import com.benny.pxerstudio.widget.PxerView
import com.benny.pxerstudio.widget.PxerView.Pxer
import kotlin.math.abs

/**
 * This class represents a drawable triangle shape.
 * It extends the DrawShape class and overrides its onDraw and onDrawEnd methods.
 */
class TriangleShape : DrawShape() {
    // A list of Pxer objects that represent the previous state of the PxerView.
    private val previousPxer = ArrayList<Pxer>()

    /**
     * Draws a triangle on the PxerView.
     *
     * @param pxerView The PxerView on which to draw the triangle.
     * @param startX The x-coordinate of the start point of the triangle.
     * @param startY The y-coordinate of the start point of the triangle.
     * @param endX The x-coordinate of the end point of the triangle.
     * @param endY The y-coordinate of the end point of the triangle.
     * @return True if the triangle was drawn successfully, false otherwise.
     */
    override fun onDraw(
        pxerView: PxerView,
        startX: Int,
        startY: Int,
        endX: Int,
        endY: Int,
    ): Boolean {
        if (!super.onDraw(pxerView, startX, startY, endX, endY)) {
            return true
        }

        val layerToDraw = pxerView.pxerLayers[pxerView.currentLayer]!!.bitmap
        draw(layerToDraw!!, previousPxer)

        val triangleWidth = abs(startX - endX)
        val triangleHeight = abs(startY - endY)

        for (i in 0 until triangleHeight + 1) {
            val mY = startY + i * if (endY - startY < 0) -1 else 1
            val mXStart = startX + i * if (endX - startX < 0) -1 else 1
            val mXEnd = endX - i * if (endX - startX < 0) -1 else 1

            for (j in mXStart until mXEnd + 1) {
                addPxerView(previousPxer, pxerView, j, mY)
                drawOnLayer(layerToDraw, pxerView, j, mY)
            }
        }

        pxerView.invalidate()
        return true
    }

    /**
     * Ends the drawing of the triangle on the PxerView.
     *
     * @param pxerView The PxerView on which the triangle was drawn.
     */
    override fun onDrawEnd(pxerView: PxerView) {
        super.onDrawEnd(pxerView)
        pxerView.endDraw(previousPxer)
    }

    /**
     * Draws a line on the layerToDraw Bitmap.
     * This function is not yet implemented.
     *
     * @param layerToDraw The Bitmap on which to draw the line.
     * @param previousPxer The list of Pxer objects that represent the previous state of the PxerView.
     * @param pxerView The PxerView on which to draw the line.
     * @param startX The x-coordinate of the start point of the line.
     * @param startY The y-coordinate of the start point of the line.
     * @param endX The x-coordinate of the end point of the line.
     */
    override fun drawLine(
        layerToDraw: Bitmap,
        previousPxer: ArrayList<Pxer>,
        pxerView: Int,
        startX: Int,
        startY: Int,
        endX: Int
    ) {
        TODO("Not yet implemented")
    }
}
