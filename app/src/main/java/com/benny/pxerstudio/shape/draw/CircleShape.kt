package com.benny.pxerstudio.shape.draw

import com.benny.pxerstudio.widget.PxerView
import com.benny.pxerstudio.widget.PxerView.Pxer
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * This class represents a drawable circle shape.
 * It extends the DrawShape class and overrides its onDraw and onDrawEnd methods.
 */
class CircleShape : DrawShape() {
    // A list of Pxer objects that represent the previous state of the PxerView.
    private val previousPxer = ArrayList<Pxer>()

    /**
     * Draws a circle on the PxerView.
     *
     * @param pxerView The PxerView on which to draw the circle.
     * @param startX The x-coordinate of the start point of the circle.
     * @param startY The y-coordinate of the start point of the circle.
     * @param endX The x-coordinate of the end point of the circle.
     * @param endY The y-coordinate of the end point of the circle.
     * @return True if the circle was drawn successfully, false otherwise.
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

        val radius = sqrt(((endX - startX) * (endX - startX) + (endY - startY) * (endY - startY)).toDouble()).toInt()
        val diameter = radius * 2

        for (x in -radius until diameter) {
            for (y in -radius until diameter) {
                val distance = sqrt((x * x + y * y).toDouble())
                if (abs(distance - radius) < 1.0) {
                    val mX = startX + x
                    val mY = startY + y
                    if (mX >= 0 && mY >= 0 && mX < layerToDraw.width && mY < layerToDraw.height) {
                        addPxerView(previousPxer, pxerView, mX, mY)
                        drawOnLayer(layerToDraw, pxerView, mX, mY)
                    }
                }
            }
        }

        pxerView.invalidate()
        return true
    }

    /**
     * Ends the drawing of the circle on the PxerView.
     *
     * @param pxerView The PxerView on which the circle was drawn.
     */
    override fun onDrawEnd(pxerView: PxerView) {
        super.onDrawEnd(pxerView)
        pxerView.endDraw(previousPxer)
    }
}
