package com.treasure.imageeraser.utils;

import android.graphics.Bitmap;
import android.graphics.Matrix;

/**
 * Created by Martin on 2016/7/23 0023.
 */
public class BitmapUtils {

    public static Bitmap scaleBitmap(Bitmap b, float scaleX, float scaleY) {
        Bitmap resizeBitmap = null;
        Matrix scaleMatrix = new Matrix();
        if (b != null && !b.isRecycled()) {
            scaleMatrix.postScale(scaleX, scaleY);
            try {
                resizeBitmap = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), scaleMatrix, false);
            } catch (Exception e) {
                return b;
            }
        }
        return resizeBitmap;
    }

    /**
     * 清除bitmap对象
     *
     * @param bitmap 目标对象
     */
    public static void destroyBitmap(Bitmap bitmap) {
        if (bitmap != null) {
            Bitmap b = bitmap;
            if (b != null && !b.isRecycled()) {
                b.recycle();
            }
            bitmap = null;
        }
    }

}
