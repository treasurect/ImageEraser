package com.treasure.imageeraser.ui.activity;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.treasure.imageeraser.R;
import com.treasure.imageeraser.ui.views.BrushImageView;
import com.treasure.imageeraser.ui.views.TouchImageView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Vector;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class BitmapEraserActivity extends BaseActivity {
    @BindView(R.id.drawingImageView)
    TouchImageView mDrawImg;
    @BindView(R.id.brushContainingView)
    BrushImageView mBrushImg;
    @BindView(R.id.rl_image_view_container)
    RelativeLayout rlImageViewContainer;
    @BindView(R.id.iv_undo)
    FrameLayout ivUndo;
    @BindView(R.id.sb_width)
    SeekBar sbWidth;
    @BindView(R.id.edit_bottom_layout)
    LinearLayout mBottomLayout;

    private int initialDrawingCountLimit = 20;
    private int offset = 0;
    private int undoLimit = 10;
    private float brushSize = 95.0f;

    private boolean isMultipleTouchErasing;
    private boolean isTouchOnBitmap;
    private int initialDrawingCount;
    private int updatedBrushSize;
    private int imageViewWidth;

    private int imageViewHeight;

    private Bitmap bitmapMaster;
    private Bitmap lastEditedBitmap;
    private Bitmap originalBitmap;
    private Bitmap highResolutionOutput;

    private Canvas canvasMaster;
    private Point mainViewSize;
    private Path drawingPath;

    private Vector<Integer> brushSizes;
//    private Vector<Integer> redoBrushSizes;

    private ArrayList<Path> paths;
//    private ArrayList<Path> redoPaths;

    private boolean isImageResized;
    private int MODE = 0;

    private String mPath;
    private String mSavePath;

    private View mRootView;
    private PopupWindow mZoomView;
    private ImageView mDestIv;
    private Bitmap mCacheBitmap;
    private Bitmap mDisplayBitmap;

    private int ZOOM_WIDTH;
    private int ZOOM_HEIGHT;
    private int mScreenWidth;

    @Override
    public void loadContentLayout() {
        setContentView(R.layout.activity_bitmap_eraser);
    }

    @Override
    public void initView() {
        handleIntent(getIntent());
        mScreenWidth = getResources().getDisplayMetrics().widthPixels;
        drawingPath = new Path();
        Display display = getWindowManager().getDefaultDisplay();
        mainViewSize = new Point();
        display.getSize(mainViewSize);

        mRootView = getWindow().getDecorView();
        ZOOM_WIDTH = dip2px(this, 116);
        ZOOM_HEIGHT = dip2px(this, 126);
        View convertView = LayoutInflater.from(this).inflate(R.layout.layout_edit_preview, null);
        mZoomView = new PopupWindow(convertView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        mDestIv = (ImageView) convertView.findViewById(R.id.preview_img);

        brushSizes = new Vector();
//        redoBrushSizes = new Vector();
        paths = new ArrayList();
//        redoPaths = new ArrayList();

//        rlImageViewContainer.getLayoutParams().height = mainViewSize.y - (llTopBar.getLayoutParams().height);
        rlImageViewContainer.getLayoutParams().height = mainViewSize.y;
        imageViewWidth = mainViewSize.x;
        imageViewHeight = rlImageViewContainer.getLayoutParams().height;

//        ivRedo.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                redo();
//            }
//        });

        mDrawImg.setOnTouchListener(new OnTouchListener());
        sbWidth.setMax(150);
        sbWidth.setProgress(75);
        sbWidth.setOnSeekBarChangeListener(new OnSizeChangeListener());
        updateBrush((float) (mainViewSize.x / 2), (float) (mainViewSize.y / 2));
//        sbOffset.setMax(350);
//        sbOffset.setProgress(offset);
//        sbOffset.setOnSeekBarChangeListener(new OnOffsetSeekbarChangeListner());
        originalBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.pic_test);

        setBitMap();
    }

    @Override
    public void setListener() {

    }

    private void handleIntent(Intent intent) {
        mPath = intent.getStringExtra("photo_path");
    }


    public void setBitMap() {
        this.isImageResized = false;
        if (bitmapMaster != null) {
            bitmapMaster.recycle();
            bitmapMaster = null;
        }
        canvasMaster = null;
        originalBitmap = resizeBitmapByCanvas();

        lastEditedBitmap = originalBitmap.copy(Config.ARGB_8888, true);
        bitmapMaster = Bitmap.createBitmap(originalBitmap.getWidth(), originalBitmap.getHeight(), Config.ARGB_8888);
        canvasMaster = new Canvas(bitmapMaster);
        canvasMaster.drawBitmap(originalBitmap, 0.0f, 0.0f, null);
        mDrawImg.setImageBitmap(bitmapMaster);
        resetPathArrays();
        mDrawImg.setPan(false);
        mBrushImg.invalidate();
    }

    @OnClick({R.id.iv_undo, R.id.edit_bottom_layout})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.iv_undo:
                undo();
                break;
            case R.id.edit_bottom_layout:

                break;
        }
    }

    public void undo() {
        int size = this.paths.size();
        if (size != 0) {
            if (size == 1) {
                this.ivUndo.setEnabled(false);
            }
            size--;
            paths.remove(size);
//            redoPaths.add(paths.remove(size));
            brushSizes.remove(size);
//            redoBrushSizes.add(brushSizes.remove(size));
//            if (!ivRedo.isEnabled()) {
//                ivRedo.setEnabled(true);
//            }
            UpdateCanvas();
        }
    }

//    public void redo() {
//        int size = redoPaths.size();
//        if (size != 0) {
//            if (size == 1) {
//                ivRedo.setEnabled(false);
//            }
//            size--;
//            paths.add(redoPaths.remove(size));
//            brushSizes.add(redoBrushSizes.remove(size));
//            if (!ivUndo.isEnabled()) {
//                ivUndo.setEnabled(true);
//            }
//            UpdateCanvas();
//        }
//    }


    private void addDrawingPathToArrayList() {
        if (paths.size() >= undoLimit) {
            UpdateLastEiditedBitmapForUndoLimit();
            paths.remove(0);
            brushSizes.remove(0);
        }
        if (paths.size() == 0) {
//            ivUndo.setEnabled(true);
//            ivRedo.setEnabled(false);
        }
        brushSizes.add(updatedBrushSize);
        paths.add(drawingPath);
        drawingPath = new Path();

        if (paths.size() > 0)
            ivUndo.setEnabled(true);
    }

    public Bitmap resizeBitmapByCanvas() {
        float width;
        float heigth;
        float orginalWidth = (float) originalBitmap.getWidth();
        float orginalHeight = (float) originalBitmap.getHeight();
        if (orginalWidth > orginalHeight) {
            width = (float) imageViewWidth;
            heigth = (((float) imageViewWidth) * orginalHeight) / orginalWidth;
        } else {
            heigth = (float) imageViewHeight;
            width = (((float) imageViewHeight) * orginalWidth) / orginalHeight;
        }
        if (width > orginalWidth || heigth > orginalHeight) {
            return originalBitmap;
        }
        Bitmap background = Bitmap.createBitmap((int) width, (int) heigth, Config.ARGB_8888);
        Canvas canvas = new Canvas(background);
        float scale = width / orginalWidth;
        float yTranslation = (heigth - (orginalHeight * scale)) / 2.0f;
        Matrix transformation = new Matrix();
        transformation.postTranslate(0.0f, yTranslation);
        transformation.preScale(scale, scale);
        Paint paint = new Paint();
        paint.setFilterBitmap(true);
        canvas.drawBitmap(originalBitmap, transformation, paint);
        this.isImageResized = true;
        return background;
    }

    public void resetPathArrays() {
        ivUndo.setEnabled(false);
//        ivRedo.setEnabled(false);
        paths.clear();
        brushSizes.clear();
//        redoPaths.clear();
//        redoBrushSizes.clear();
    }

    public void resetRedoPathArrays() {
//        ivRedo.setEnabled(false);
//        redoPaths.clear();
//        redoBrushSizes.clear();
    }

    public void UpdateLastEiditedBitmapForUndoLimit() {
        Canvas canvas = new Canvas(lastEditedBitmap);
        for (int i = 0; i < 1; i += 1) {
            int brushSize = brushSizes.get(i);
            Paint paint = new Paint();
            paint.setColor(0);
            paint.setStrokeWidth((float) brushSize);
            paint.setStyle(Style.STROKE);
            paint.setAntiAlias(true);
            paint.setStrokeJoin(Join.ROUND);
            paint.setStrokeCap(Cap.ROUND);
            paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
            canvas.drawPath(paths.get(i), paint);
        }
    }

    public void UpdateCanvas() {
        canvasMaster.drawColor(0, Mode.CLEAR);
        canvasMaster.drawBitmap(lastEditedBitmap, 0.0f, 0.0f, null);
        int i = 0;
        while (true) {
            if (i >= paths.size()) {
                break;
            }
            int brushSize = brushSizes.get(i);
            Paint paint = new Paint();
            paint.setColor(0);
            paint.setStrokeWidth((float) brushSize);
            paint.setStyle(Style.STROKE);
            paint.setAntiAlias(true);
            paint.setStrokeJoin(Join.ROUND);
            paint.setStrokeCap(Cap.ROUND);
            paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
            canvasMaster.drawPath(paths.get(i), paint);
            i += 1;
        }
        mDrawImg.invalidate();
    }

    public void updateBrushWidth() {
        mBrushImg.width = brushSize / 2.0f;
        mBrushImg.invalidate();
    }

    public void updateBrushOffset() {
        float doffest = ((float) offset) - mBrushImg.offset;
        BrushImageView brushImageViewView = mBrushImg;
        brushImageViewView.centery += doffest;
        mBrushImg.offset = (float) offset;
        mBrushImg.invalidate();
    }

    public void updateBrush(float x, float y) {
        mBrushImg.offset = (float) offset;
        mBrushImg.centerx = x;
        mBrushImg.centery = y;
        mBrushImg.width = brushSize / 2.0f;
        mBrushImg.invalidate();
    }

    public float getImageViewZoom() {
        return mDrawImg.getCurrentZoom();
    }

    public PointF getImageViewTranslation() {
        return mDrawImg.getTransForm();
    }

    private void drawOnTouchMove() {
        Paint paint = new Paint();
        paint.setColor(0);
        paint.setStrokeWidth((float) updatedBrushSize);

        paint.setStyle(Style.STROKE);
        paint.setAntiAlias(true);
        paint.setStrokeJoin(Join.ROUND);
        paint.setStrokeCap(Cap.ROUND);
        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
        canvasMaster.drawPath(drawingPath, paint);
        mDrawImg.invalidate();
    }

    private void moveToPoint(float startX, float startY) {
        float zoomScale = getImageViewZoom();
        startY -= (float) offset;
//        if (redoPaths.size() > 0) {
//            resetRedoPathArrays();
//        }
        PointF transLation = getImageViewTranslation();
        int projectedX = (int) ((float) (((double) (startX - transLation.x)) / ((double) zoomScale)));
        int projectedY = (int) ((float) (((double) (startY - transLation.y)) / ((double) zoomScale)));
        drawingPath.moveTo((float) projectedX, (float) projectedY);

        updatedBrushSize = (int) (brushSize / zoomScale);
    }

    private void lineToPoint(Bitmap bm, float startX, float startY) {
        if (initialDrawingCount < initialDrawingCountLimit) {
            initialDrawingCount += 1;
            if (initialDrawingCount == initialDrawingCountLimit) {
                isMultipleTouchErasing = true;
            }
        }
        float zoomScale = getImageViewZoom();
        startY -= (float) offset;
        PointF transLation = getImageViewTranslation();
        int projectedX = (int) ((float) (((double) (startX - transLation.x)) / ((double) zoomScale)));
        int projectedY = (int) ((float) (((double) (startY - transLation.y)) / ((double) zoomScale)));
        if (!isTouchOnBitmap && projectedX > 0 && projectedX < bm.getWidth() && projectedY > 0 && projectedY < bm.getHeight()) {
            isTouchOnBitmap = true;
        }
        drawingPath.lineTo((float) projectedX, (float) projectedY);
    }

    public void setAnimation(View view, int transX, int transY, int duration) {
        ObjectAnimator translationX = ObjectAnimator.ofFloat(view, "translationX", transX);
        ObjectAnimator translationY = ObjectAnimator.ofFloat(view, "translationY", transY);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.play(translationX).with(translationY);
        animatorSet.setDuration(duration);
        animatorSet.setInterpolator(new DecelerateInterpolator());
        animatorSet.start();
    }

    public int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    private void makeHighResolutionOutput() {
//        if (this.isImageResized) {
//            Bitmap solidColor = Bitmap.createBitmap(this.originalBitmap.getWidth(), this.originalBitmap.getHeight(), this.originalBitmap.getConfig());
//            Canvas canvas = new Canvas(solidColor);
//            Paint paint = new Paint();
//            paint.setColor(Color.argb(255, 255, 255, 255));
//            Rect src = new Rect(0, 0, this.bitmapMaster.getWidth(), this.bitmapMaster.getHeight());
//            Rect dest = new Rect(0, 0, this.originalBitmap.getWidth(), this.originalBitmap.getHeight());
//            canvas.drawRect(dest, paint);
//            paint.setXfermode(new PorterDuffXfermode(Mode.DST_OUT));
//            canvas.drawBitmap(this.bitmapMaster, src, dest, paint);
//            this.highResolutionOutput = null;
//            this.highResolutionOutput = Bitmap.createBitmap(this.originalBitmap.getWidth(), this.originalBitmap.getHeight(), this.originalBitmap.getConfig());
//            Canvas canvas1 = new Canvas(this.highResolutionOutput);
//            canvas1.drawBitmap(this.originalBitmap, 0.0f, 0.0f, null);
//            Paint paint1 = new Paint();
//            paint1.setXfermode(new PorterDuffXfermode(Mode.DST_OUT));
//            canvas1.drawBitmap(solidColor, 0.0f, 0.0f, paint1);
//            if (solidColor != null && !solidColor.isRecycled()) {
//                solidColor.recycle();
//                solidColor = null;
//            }
//            return;
//        }
//        this.highResolutionOutput = null;
//        this.highResolutionOutput = this.bitmapMaster.copy(this.bitmapMaster.getConfig(), true);
        rlImageViewContainer.setDrawingCacheEnabled(true);
        highResolutionOutput = rlImageViewContainer.getDrawingCache();
    }

    private void goCompletePage() {
        if (mSavePath == null)
            return;
//        Intent intent = new Intent(BitmapEraserActivity.this, TigerShare.class);
//        intent.putExtra("save_path", mSavePath);
//        startActivity(intent);
    }

    public String savePhoto(Bitmap bmp) {
        File imageFileName;
        FileOutputStream out;
        File imageFileFolder = new File(Environment.getExternalStorageDirectory(), "ImageEraser");
        imageFileFolder.mkdir();
        Calendar c = Calendar.getInstance();
        String date = String.valueOf(c.get(Calendar.MONTH))
                + String.valueOf(c.get(Calendar.DAY_OF_MONTH))
                + String.valueOf(c.get(Calendar.YEAR))
                + String.valueOf(c.get(Calendar.HOUR_OF_DAY))
                + String.valueOf(c.get(Calendar.MINUTE))
                + String.valueOf(c.get(Calendar.SECOND));
        FileOutputStream out2;


        imageFileName = new File(imageFileFolder, date.toString() + ".png");

        try {
            out2 = new FileOutputStream(imageFileName);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out2);
            out = out2;
            out.flush();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (bmp != null && !bmp.isRecycled()) {
            bmp.recycle();
            bmp = null;
        }
        return imageFileName.getPath();
    }

    private void showZoom(int x, int y) {
        mRootView.setDrawingCacheEnabled(true);
        mCacheBitmap = mRootView.getDrawingCache();
        if (mCacheBitmap == null) {
            return;
        }

        int startX = x;
        int startY = y;

        if (x + ZOOM_WIDTH > mCacheBitmap.getWidth()) {
            if (x + ZOOM_WIDTH / 2 > mCacheBitmap.getWidth()) {
                startX = mCacheBitmap.getWidth() - ZOOM_WIDTH;
            } else {
                startX = x - ZOOM_WIDTH / 2;
            }
        } else {
            if (startX - ZOOM_WIDTH / 2 <= 0) {
                startX = 0;
            } else {
                startX = startX - ZOOM_WIDTH / 2;
            }
        }

        if (y + ZOOM_HEIGHT > mCacheBitmap.getHeight()) {
            if (y + ZOOM_HEIGHT / 2 > mCacheBitmap.getHeight()) {
                startY = mCacheBitmap.getHeight() - ZOOM_HEIGHT;
            } else {
                startY = y - ZOOM_HEIGHT / 2;
            }
        } else {
            if (startY - ZOOM_HEIGHT / 2 <= 0) {
                startY = 0;
            } else {
                startY = startY - ZOOM_HEIGHT / 2;
            }
        }
        if (startY + ZOOM_HEIGHT > mCacheBitmap.getHeight()
                || startX + ZOOM_WIDTH > mCacheBitmap.getWidth()) {
            return;
        }

        mDisplayBitmap = Bitmap.createBitmap(mCacheBitmap, startX, startY, ZOOM_WIDTH, ZOOM_HEIGHT);
        mDestIv.setImageBitmap(mDisplayBitmap);
//        int showX = x - ZOOM_WIDTH / 2;
//        int showY = (int) (y - ZOOM_HEIGHT * 1.5);
        int showX = dip2px(this, 22);
        int showY = dip2px(this, 22);
        if (x <= dip2px(this, 22 + 116) && y <= dip2px(this, 22 + 126)) {
            showX = mScreenWidth - dip2px(this, 22 + 116);
        }
        mZoomView.showAtLocation(rlImageViewContainer, Gravity.NO_GRAVITY, showX, showY);
        mZoomView.update(showX, showY, mZoomView.getWidth(), mZoomView.getHeight(), false);

        mRootView.setDrawingCacheEnabled(false);
    }

    protected void onDestroy() {
        super.onDestroy();
        UpdateCanvas();
        if (lastEditedBitmap != null) {
            lastEditedBitmap.recycle();
            lastEditedBitmap = null;
        }
        if (originalBitmap != null) {
            originalBitmap.recycle();
            originalBitmap = null;
        }
        if (bitmapMaster != null) {
            bitmapMaster.recycle();
            bitmapMaster = null;
        }
        if (this.highResolutionOutput != null) {
            this.highResolutionOutput.recycle();
            this.highResolutionOutput = null;
        }
    }

    private class OnTouchListener implements View.OnTouchListener {

        public boolean onTouch(View v, MotionEvent event) {
            int action = event.getAction();
            if (!(event.getPointerCount() == 1 || isMultipleTouchErasing)) {
                if (initialDrawingCount > 0) {
                    UpdateCanvas();
                    drawingPath.reset();
                    initialDrawingCount = 0;
                }
                mDrawImg.onTouchEvent(event);
                MODE = 2;
            } else if (action == MotionEvent.ACTION_DOWN) {
                isTouchOnBitmap = false;
                mDrawImg.onTouchEvent(event);
                MODE = 1;
                initialDrawingCount = 0;
                isMultipleTouchErasing = false;
                moveToPoint(event.getX(), event.getY());

                updateBrush(event.getX(), event.getY());

                setAnimation(mBottomLayout, 0, dip2px(BitmapEraserActivity.this, 110), 200);

                showZoom((int) event.getX(), (int) event.getY());
            } else if (action == MotionEvent.ACTION_MOVE) {
                if (MODE == 1) {
                    float currentX = event.getX();
                    float currentY = event.getY();

                    updateBrush(currentX, currentY);
                    lineToPoint(bitmapMaster, currentX, currentY);

                    drawOnTouchMove();

                    showZoom((int) event.getX(), (int) event.getY());
                }
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
                if (MODE == 1) {
                    if (isTouchOnBitmap) {
                        addDrawingPathToArrayList();
                    }
                }
                isMultipleTouchErasing = false;
                initialDrawingCount = 0;
                MODE = 0;
            }
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
                MODE = 0;
                setAnimation(mBottomLayout, 0, 0, 200);


                mZoomView.dismiss();
            }
            return true;
        }
    }

    private class OnSizeChangeListener implements OnSeekBarChangeListener {

        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            brushSize = ((float) progress) + 20.0f;
            updateBrushWidth();
        }

        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        public void onStopTrackingTouch(SeekBar seekBar) {
        }
    }


    private class ImageSaveTask extends AsyncTask<String, Void, Boolean> {

        protected void onPreExecute() {
            makeHighResolutionOutput();
            getWindow().setFlags(16, 16);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        protected Boolean doInBackground(String... args) {
            try {
                mSavePath = savePhoto(highResolutionOutput);
                return Boolean.TRUE;
            } catch (Exception e) {
                return Boolean.FALSE;
            }
        }

        protected void onPostExecute(Boolean success) {
            rlImageViewContainer.setDrawingCacheEnabled(false);
            getWindow().clearFlags(16);
            if (success) {
                goCompletePage();
            }
        }
    }
}
