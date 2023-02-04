package com.example.sfp4.face_recognition

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.core.graphics.toRectF

// Defines an overlay on which the boxes and text will be drawn.
class BoundingBoxOverlay(context: Context, attributeSet: AttributeSet)
    : SurfaceView( context , attributeSet ) , SurfaceHolder.Callback {

    // Variables used to compute output2overlay transformation matrix
    // These are assigned in FrameAnalyser.kt
    var areDimsInit = false
    var frameHeight = 0
    var frameWidth = 0

    // This var is assigned in FrameAnalyser.kt
    var boundingBoxes: ArrayList<Prediction>? = null

    private var output2OverlayTransform: Matrix = Matrix()

    // Paint for boxes and text
    private val boxPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3.0f
    }
    private val textPaint = Paint().apply {
        strokeWidth = 2.0f
        textSize = 50f
        color = Color.GREEN
    }


    override fun surfaceCreated(holder: SurfaceHolder) {
        TODO("Not yet implemented")
    }


    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        TODO("Not yet implemented")
    }


    override fun surfaceDestroyed(holder: SurfaceHolder) {
        TODO("Not yet implemented")
    }


    override fun onDraw(canvas: Canvas?) {
        if (boundingBoxes != null) {
            if (!areDimsInit) {
                val viewWidth = canvas!!.width.toFloat()
                val viewHeight = canvas.height.toFloat()
                val xFactor: Float = viewWidth / frameWidth.toFloat()
                val yFactor: Float = viewHeight / frameHeight.toFloat()
                // Scale and mirror the coordinates ( required for front lens )
                output2OverlayTransform.preScale(xFactor, yFactor)
                output2OverlayTransform.postScale(-1f, 1f, viewWidth / 2f, viewHeight / 2f)
                areDimsInit = true
            } else {
                for (face in boundingBoxes!!) {
                    val boundingBox = face.bbox.toRectF()
                    output2OverlayTransform.mapRect(boundingBox)
                    canvas?.drawRoundRect(boundingBox, 16f, 16f, boxPaint)
                    canvas?.drawText(
                        face.label,
                        boundingBox.left,
                        boundingBox.top,
                        textPaint
                    )
                }
            }
        }
    }
}