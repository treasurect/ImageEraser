package com.treasure.imageeraser.ui.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;

public class BrushImageView extends android.support.v7.widget.AppCompatImageView {
    int alpga;
    public float centerx;
    public float centery;
    int density;
    DisplayMetrics metrics;
    public float offset;
    public float smallRadious;
    public float width,height;
    private RectF rectF;
    private Bitmap bitmap;

    public BrushImageView(Context context) {
        super(context);
        this.metrics = getResources().getDisplayMetrics();
        this.density = (int) this.metrics.density;
        this.alpga = 200;
        this.offset = (float) (this.density * 100);
        this.centerx = (float) (this.density * 166);
        this.centery = (float) (this.density * 200);
        this.width = (float) (this.density * 33);
        this.height = (float) (this.density * 33);
        this.smallRadious = (float) (this.density * 3);
    }

    public BrushImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.metrics = getResources().getDisplayMetrics();
        this.density = (int) this.metrics.density;
        this.alpga = 200;
        this.offset = (float) (this.density * 100);
        this.centerx = (float) (this.density * 166);
        this.centery = (float) (this.density * 200);
        this.width = (float) (this.density * 33);
        this.height = (float) (this.density * 33);
        this.smallRadious = (float) (this.density * 3);
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Log.e("SAVE_COUNT",""+canvas.getSaveCount());
        if (canvas.getSaveCount() > 1)
            canvas.restore();
        canvas.save();
        Paint p;
        if (this.offset > 0.0f) {
            p = new Paint();
            p.setColor(Color.parseColor("#c8597BF2"));
            p.setAntiAlias(true);
            canvas.drawCircle(this.centerx, this.centery, this.smallRadious, p);
        }
        p = new Paint();
        p.setColor(Color.parseColor("#c8597BF2"));
        p.setAntiAlias(true);
//        canvas.drawBitmap(bitmap,null,rectF,p);
        canvas.drawCircle(this.centerx, this.centery - this.offset, this.width, p);
        return;
    }
}
