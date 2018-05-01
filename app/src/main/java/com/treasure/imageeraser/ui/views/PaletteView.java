package com.treasure.imageeraser.ui.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wensefu on 17-3-21.
 */
public class PaletteView extends View {

    private Paint mPaint;
    private Path mPath;
    private float mLastX;
    private float mLastY;
    private Bitmap mBufferBitmap;
    private Canvas mBufferCanvas;

    private static final int MAX_CACHE_STEP = 20;

    private List<DrawingInfo> mDrawingList = new ArrayList<>(MAX_CACHE_STEP);
//    private List<DrawingInfo> mRemovedList;

    private PorterDuffXfermode mClearMode, mDrawMode;
    private float mDrawSize;
    private float mEraserSize;

    private boolean mCanEraser;

//    private Callback mCallback;

    public enum Mode {
        DRAW,
        ERASER
    }

    private Mode mMode = Mode.DRAW;

    public PaletteView(Context context) {
        this(context, null);
    }

    public PaletteView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDrawingCacheEnabled(true);
        init();
    }

//    public interface Callback {
//        void onUndoRedoStatusChanged();
//    }

//    public void setCallback(Callback callback) {
//        mCallback = callback;
//    }

    private void init() {
        mClearMode = new PorterDuffXfermode(PorterDuff.Mode.CLEAR);
        mDrawMode = new PorterDuffXfermode(PorterDuff.Mode.XOR);

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setFilterBitmap(true);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mDrawSize = 85;
        mEraserSize = 85;
        mPaint.setStrokeWidth(mDrawSize);
        mPaint.setXfermode(mDrawMode);
        mPaint.setColor(Color.parseColor("#597BF2"));
        mPaint.setAlpha(150);
        mPaint.setMaskFilter(new BlurMaskFilter(mDrawSize / 8, BlurMaskFilter.Blur.NORMAL));

    }

    private void initBuffer() {
        mBufferBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        mBufferCanvas = new Canvas(mBufferBitmap);
    }

    public abstract static class DrawingInfo {
        Paint paint;
        Mode mode;

        abstract void draw(Canvas canvas);
    }

    private static class PathDrawingInfo extends DrawingInfo {

        Path path;

        @Override
        void draw(Canvas canvas) {
            if (mode == Mode.DRAW) {
                Paint.Style style = paint.getStyle();
                paint.setStyle(Paint.Style.FILL_AND_STROKE);
                path.close();
                canvas.drawPath(path, paint);
                paint.setStyle(style);
            } else {
                canvas.drawPath(path, paint);
            }
        }
    }

    public Mode getMode() {
        return mMode;
    }

    public void setMode(Mode mode) {
        if (mode != mMode) {
            mMode = mode;
            if (mMode == Mode.DRAW) {
                mPaint.setXfermode(mDrawMode);
                mPaint.setStrokeWidth(mDrawSize);
                mPaint.setMaskFilter(new BlurMaskFilter(mDrawSize / 8, BlurMaskFilter.Blur.NORMAL));
            } else {
                mPaint.setXfermode(mClearMode);
                mPaint.setStrokeWidth(mEraserSize);
                mPaint.setMaskFilter(null);
            }
        }
    }

    public void setEraserSize(float size) {
        mEraserSize = size;
        mPaint.setStrokeWidth(mEraserSize);
    }

    public void setPenRawSize(float size) {
        mDrawSize = size;
        mPaint.setStrokeWidth(mDrawSize);
        mPaint.setMaskFilter(new BlurMaskFilter(mDrawSize / 8, BlurMaskFilter.Blur.NORMAL));
    }

    public float getmDrawSize() {
        return mDrawSize;
    }

    public float getmEraserSize() {
        return mEraserSize;
    }

    public void setPenColor(int color) {
        mPaint.setColor(color);
    }

    public void setPenAlpha(int alpha) {
        mPaint.setAlpha(alpha);
    }

    private void reDraw() {
        if (mDrawingList != null && mBufferBitmap != null) {
            mBufferBitmap.eraseColor(Color.TRANSPARENT);
            for (int i = 0; i < 3; i++) {
                for (DrawingInfo drawingInfo : mDrawingList) {
                    drawingInfo.draw(mBufferCanvas);
//                mBufferCanvas.drawPath(mPath, drawingInfo.paint);
                }
            }
            invalidate();
        }
    }

//    public boolean canRedo() {
//        return mRemovedList != null && mRemovedList.size() > 0;
//    }

    public boolean canUndo() {
        return mDrawingList != null && mDrawingList.size() > 0;
    }

//    public void redo() {
//        int size = mRemovedList == null ? 0 : mRemovedList.size();
//        if (size > 0) {
//            DrawingInfo info = mRemovedList.remove(size - 1);
//            mDrawingList.add(info);
//            mCanEraser = true;
//            reDraw();
////            if (mCallback != null) {
////                mCallback.onUndoRedoStatusChanged();
////            }
//        }
//    }

    public void undo() {
        int size = mDrawingList == null ? 0 : mDrawingList.size();
        if (size > 0) {
            DrawingInfo info = mDrawingList.remove(size - 1);

//            if (mRemovedList == null) {
//                mRemovedList = new ArrayList<>(MAX_CACHE_STEP);
//            }
            if (size == 1) {
                mCanEraser = false;
            }
//            mRemovedList.add(info);
            reDraw();
//            if (mCallback != null) {
//                mCallback.onUndoRedoStatusChanged();
//            }
        }
        if (mDrawingList != null) {
            if (mDrawingList.size() == 0) {
                mAddListener.changeUndo(false);
            } else {
                mAddListener.changeUndo(true);
            }
        }
    }

    public void clear() {
        if (mBufferBitmap != null) {
            if (mDrawingList != null) {
                mDrawingList.clear();
            }
//            if (mRemovedList != null) {
//                mRemovedList.clear();
//            }
            mCanEraser = false;
            mBufferBitmap.eraseColor(Color.TRANSPARENT);
            invalidate();
//            if (mCallback != null) {
//                mCallback.onUndoRedoStatusChanged();
//            }
        }
    }

    public Bitmap buildBitmap() {
        Bitmap mBufferBitmap2 = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas mBufferCanvas2 = new Canvas(mBufferBitmap2);
        if (mDrawingList != null) {
            for (DrawingInfo drawingInfo : mDrawingList) {
                drawingInfo.paint.setColor(Color.parseColor("#597BF2"));
                if (drawingInfo.mode == Mode.ERASER) {
                    drawingInfo.paint.setXfermode(mClearMode);
                } else {
                    drawingInfo.paint.setXfermode(null);
                }
                drawingInfo.draw(mBufferCanvas2);

                drawingInfo.paint.setColor(Color.parseColor("#597BF2"));
                drawingInfo.paint.setAlpha(150);
                if (drawingInfo.mode == Mode.ERASER) {
                    drawingInfo.paint.setXfermode(mClearMode);
                } else {
                    drawingInfo.paint.setXfermode(mDrawMode);
                }
            }
        }


//        Bitmap bm = getDrawingCache();
//        Bitmap result = Bitmap.createBitmap(bm);
//        destroyDrawingCache();
        return mBufferBitmap2;
    }

    private void saveDrawingPath() {
        if (mDrawingList == null) {
            mDrawingList = new ArrayList<>(MAX_CACHE_STEP);
        } else if (mDrawingList.size() == MAX_CACHE_STEP) {
            mDrawingList.remove(0);
        }
        Path cachePath = new Path(mPath);
        Paint cachePaint = new Paint(mPaint);
        PathDrawingInfo info = new PathDrawingInfo();
        info.path = cachePath;
        info.paint = cachePaint;
        info.mode = mMode;
        mDrawingList.add(info);
        mCanEraser = true;
//        if (mCallback != null) {
//            mCallback.onUndoRedoStatusChanged();
//        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mBufferBitmap != null) {
//            canvas.drawPath(mPath, mPaint);
            canvas.drawBitmap(mBufferBitmap, 0, 0, null);
        }
    }

    boolean isFirstInitEdgePath = true;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int action = event.getAction() & MotionEvent.ACTION_MASK;
        final float x = event.getX();
        final float y = event.getY();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mLastX = x;
                mLastY = y;
                if (mPath == null) {
                    mPath = new Path();
                }
                mPath.moveTo(x, y);
                mAddListener.showZoom(event.getX(), event.getY());
                mAddListener.showOrHindTitle(true);

                //初始化外延就是最开始点击的坐标,仅仅记录第一次点击位置作为初始值
                if (isFirstInitEdgePath) {
                    EdgeOfPath.left = (int) x;
                    EdgeOfPath.right = (int) x;
                    EdgeOfPath.bottom = (int) y;
                    EdgeOfPath.top = (int) y;
                    isFirstInitEdgePath = false;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                //这里终点设为两点的中心点的目的在于使绘制的曲线更平滑，如果终点直接设置为x,y，效果和lineto是一样的,实际是折线效果
                mPath.quadTo(mLastX, mLastY, (x + mLastX) / 2, (y + mLastY) / 2);
                if (mBufferBitmap == null) {
                    initBuffer();
                }
                if (mMode == Mode.ERASER && !mCanEraser) {
                    break;
                }
                mBufferCanvas.drawPath(mPath, mPaint);
                invalidate();
                mLastX = x;
                mLastY = y;

                mAddListener.updateBrush(event.getX(), event.getY());
                mAddListener.showZoom(event.getX(), event.getY());

                //记录最外延点
                if (mMode == Mode.DRAW) {
                    if (event.getY() < EdgeOfPath.top) {
                        EdgeOfPath.top = (int) event.getY();
                    }

                    if (event.getY() > EdgeOfPath.bottom) {
                        EdgeOfPath.bottom = (int) event.getY();
                    }

                    if (event.getX() < EdgeOfPath.left) {
                        EdgeOfPath.left = (int) event.getX();
                    }

                    if (event.getX() > EdgeOfPath.right) {
                        EdgeOfPath.right = (int) event.getX();
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mMode == Mode.DRAW || mCanEraser) {
                    saveDrawingPath();
                }
                for (int i = 0; i < 3; i++) {
                    if (mBufferCanvas != null) {
                        if (mMode == Mode.DRAW) {
                            Paint.Style style = mPaint.getStyle();
                            mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
                            mPath.close();
                            mBufferCanvas.drawPath(mPath, mPaint);
                            mPaint.setStyle(style);
                        } else {
                            mBufferCanvas.drawPath(mPath, mPaint);
                        }
                    }
                }
                invalidate();
                mPath.reset();
                mAddListener.hindZoom();
                mAddListener.showOrHindTitle(false);
                if (mDrawingList != null) {
                    if (mDrawingList.size() == 0) {
                        mAddListener.changeUndo(false);
                    } else {
                        mAddListener.changeUndo(true);
                    }
                }
                break;
        }
        return true;
    }

    public interface AddListener {
        void updateBrush(float centerX, float centerY);

        void showZoom(float x, float y);

        void hindZoom();

        void showOrHindTitle(boolean isShow);

        void changeUndo(boolean isPainted);
    }

    private AddListener mAddListener;

    public void setAddListener(AddListener mAddListener) {
        this.mAddListener = mAddListener;
    }

    public List<DrawingInfo> getmDrawingList() {
        return mDrawingList;
    }

    public EdgeOfPath EdgeOfPath = new EdgeOfPath();

    /**
     * path边沿封装类
     */
    public static class EdgeOfPath implements Parcelable {
        public int left;
        public int top;
        public int right;
        public int bottom;

        public int getWidth() {
            return right - left;
        }

        public int getHeight() {
            return bottom - top;
        }

        public int getTop() {

            return top;
        }

        public int getLeft() {
            return left;
        }

        public int getRight() {
            return right;
        }

        public int getBottom() {
            return bottom;
        }

        public EdgeOfPath() {
        }

        public EdgeOfPath( int left, int top,int right, int bottom) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }

        protected EdgeOfPath(Parcel in) {
            top = in.readInt();
            left = in.readInt();
            right = in.readInt();
            bottom = in.readInt();
        }

        public static final Creator<EdgeOfPath> CREATOR = new Creator<EdgeOfPath>() {
            @Override
            public EdgeOfPath createFromParcel(Parcel in) {
                return new EdgeOfPath(in);
            }

            @Override
            public EdgeOfPath[] newArray(int size) {
                return new EdgeOfPath[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeInt(top);
            parcel.writeInt(left);
            parcel.writeInt(right);
            parcel.writeInt(bottom);
        }
    }
}
