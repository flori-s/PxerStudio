package com.benny.pxerstudio.shape.draw

import android.graphics.Bitmap
import com.benny.pxerstudio.widget.PxerView
import com.benny.pxerstudio.widget.PxerView.Pxer
import kotlin.math.abs


class TriangleShape : DrawShape() {
    private val previousPxer = ArrayList<Pxer>()

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

    override fun onDrawEnd(pxerView: PxerView) {
        super.onDrawEnd(pxerView)
        pxerView.endDraw(previousPxer)
    }

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
