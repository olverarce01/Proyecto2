package com.example.proyecto2;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.view.View;

import androidx.camera.core.ImageAnalysis;


public class Rectangulo extends View {
    private Rect rectangle;
    private Paint paint=new Paint();
    public Rectangulo(Context context){
        super(context);
    }
    public Rectangulo(ImageAnalysis.Analyzer context, Rect rectangulo){
        super((Context) context);
        rectangle=rectangulo;
    }
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRect(rectangle,paint);
    }
}
