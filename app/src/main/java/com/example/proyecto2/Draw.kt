package com.example.proyecto2

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.view.View

class Draw(context: Context?, var rect:Rect, var text:String):View(context) {
    lateinit var boundaryPaint: Paint
    lateinit var textPaint: Paint
    init {
        init()
    }
    private fun init(){
        boundaryPaint=Paint()
        boundaryPaint.color= Color.GREEN
        boundaryPaint.strokeWidth=10f
        boundaryPaint.style=Paint.Style.STROKE

        textPaint=Paint()
        textPaint.color=Color.WHITE
        textPaint.strokeWidth=50f
        textPaint.style=Paint.Style.FILL
        textPaint.textSize=60f
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.drawRect(rect.left.toFloat(),rect.top.toFloat(),rect.right.toFloat(),rect.bottom.toFloat(),boundaryPaint)
        canvas?.drawText(text,rect.centerX().toFloat(),rect.centerY().toFloat(),textPaint)

    }
}