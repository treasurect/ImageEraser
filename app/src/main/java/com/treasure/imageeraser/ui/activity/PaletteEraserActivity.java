package com.treasure.imageeraser.ui.activity;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Toast;

import com.treasure.imageeraser.R;
import com.treasure.imageeraser.ui.views.BrushImageView;
import com.treasure.imageeraser.ui.views.PaletteView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class PaletteEraserActivity extends BaseActivity {
    @BindView(R.id.image_back)
    ImageView mBackImg;
    @BindView(R.id.palette)
    PaletteView mPaletteView;
    @BindView(R.id.brushContainingView)
    BrushImageView mBrushImg;
    @BindView(R.id.sb_width)
    SeekBar sbWidth;
    @BindView(R.id.preview)
    ImageView mPreView;
    @BindView(R.id.undo_btn)
    RelativeLayout mUndoView;
    @BindView(R.id.draw_btn)
    RelativeLayout mPenView;
    @BindView(R.id.eraser_btn)
    RelativeLayout mEraserView;
    @BindView(R.id.preview_btn)
    RelativeLayout mBtnPreView;
    @BindView(R.id.edit_bottom_layout)
    LinearLayout mBottomLayout;
    @BindView(R.id.edit_bottom_undo_layout)
    LinearLayout mBottomUndoLayout;

    private float brushSize = 95.0f;
    private int offset = 0;
    private int mScreenWidth;
    private int mScreenHeight;
    private ImageSaveTask mSaveTask;
    private ArrayList<String> mSaveList = new ArrayList<>();
    private Bitmap mBackBmp;

    private View mRootView;
    private PopupWindow mZoomView;
    private ImageView mDestIv;
    private Bitmap mCacheBitmap;
    private Bitmap mDisplayBitmap;

    private int ZOOM_WIDTH;
    private int ZOOM_HEIGHT;
    private float absoluteX,absoluteY,scale;

    @Override
    public void loadContentLayout() {
        setContentView(R.layout.activity_palette_eraser);
    }

    @Override
    public void initView() {
        mScreenWidth = getResources().getDisplayMetrics().widthPixels;
        mScreenHeight = getResources().getDisplayMetrics().heightPixels;
        mRootView = getWindow().getDecorView();
        ZOOM_WIDTH = dip2px(this, 114);
        ZOOM_HEIGHT = dip2px(this, 124);
        View convertView = LayoutInflater.from(this).inflate(R.layout.layout_edit_preview, null);
        mZoomView = new PopupWindow(convertView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        mDestIv = convertView.findViewById(R.id.preview_img);
        mZoomView.setContentView(convertView);

        mPenView.setSelected(true);
        mUndoView.setEnabled(false);
//        mRedoView.setEnabled(false);

        updateBrush1((float) (mScreenWidth / 2), (float) (mScreenHeight / 2));
        mBackBmp = BitmapFactory.decodeResource(getResources(),R.mipmap.pic_test);
        mBackImg.setImageBitmap(mBackBmp);
    }

    @Override
    public void setListener() {
        mPaletteView.setAddListener(new PaletteView.AddListener() {
            @Override
            public void updateBrush(float centerX, float centerY) {
                updateBrush1(centerX, centerY);
            }

            @Override
            public void showZoom(float x, float y) {
                showZoom1((int) x, (int) y);
            }

            @Override
            public void hindZoom() {
                mZoomView.dismiss();
            }

            @Override
            public void showOrHindTitle(boolean isShow) {
                if (isShow) {

                    setAnimation(mBottomLayout, 0, dip2px(PaletteEraserActivity.this, 110), 200);
                } else {

                    setAnimation(mBottomLayout, 0, 0, 200);
                }
            }

            @Override
            public void changeUndo(boolean isPainted) {
                if (isPainted) {
                    mUndoView.setEnabled(true);
                } else {
                    mUndoView.setEnabled(false);
                }
            }
        });

        sbWidth.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress < 5) {
                    progress = 5;
                }
                brushSize = ((float) progress);
                mPaletteView.setPenRawSize(progress);
                mPaletteView.setEraserSize(progress);
                updateBrushWidth();
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    @OnClick({R.id.undo_btn, R.id.draw_btn, R.id.eraser_btn, R.id.preview_btn, R.id.edit_bottom_layout, R.id.btn_apply})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.undo_btn:
                mPaletteView.undo();
                break;
//            case R.id.redo_btn:
////                mPaletteView.redo();
//                break;
            case R.id.draw_btn:
                mBottomUndoLayout.setVisibility(View.VISIBLE);
                mBrushImg.setVisibility(View.VISIBLE);
                mPreView.setVisibility(View.GONE);

                view.setSelected(true);
                mEraserView.setSelected(false);
                mBtnPreView.setSelected(false);
                mPaletteView.setMode(PaletteView.Mode.DRAW);
                break;
            case R.id.eraser_btn:
                mBottomUndoLayout.setVisibility(View.VISIBLE);
                mBrushImg.setVisibility(View.VISIBLE);
                mPreView.setVisibility(View.GONE);

                view.setSelected(true);
                mPenView.setSelected(false);
                mBtnPreView.setSelected(false);
                mPaletteView.setMode(PaletteView.Mode.ERASER);
                break;
            case R.id.preview_btn:
                view.setSelected(true);
                mPenView.setSelected(false);
                mEraserView.setSelected(false);
                openPreView();
                break;
            case R.id.edit_bottom_layout:
                break;
            case R.id.btn_apply:
                if (mSaveTask != null && !mSaveTask.isCancelled()) {
                    mSaveTask.cancel(true);
                    mSaveTask = null;
                }
                mSaveTask = new ImageSaveTask();
                mSaveTask.execute();
                break;
        }
    }

    public void updateBrushWidth() {
        mBrushImg.width = brushSize / 2.0f;
        mBrushImg.invalidate();
    }

    public void updateBrush1(float x, float y) {
        mBrushImg.offset = (float) offset;
        mBrushImg.centerx = x;
        mBrushImg.centery = y;
        mBrushImg.width = brushSize / 2.0f;
        mBrushImg.invalidate();
    }

    private void showZoom1(int x, int y) {
        mRootView.setDrawingCacheEnabled(true);
        mCacheBitmap = mRootView.getDrawingCache();

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
        if (x <= dip2px(this, 22 + 114) && y <= dip2px(this, 22 + 124)) {
            showX = mScreenWidth - dip2px(this, 22 + 114);
        }
        mZoomView.showAtLocation(mPaletteView, Gravity.NO_GRAVITY, showX, showY);
        mZoomView.update(showX, showY, mZoomView.getWidth(), mZoomView.getHeight(), false);

        mRootView.setDrawingCacheEnabled(false);
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
//    @Override
//    public void onUndoRedoStatusChanged() {
//        mUndoView.setEnabled(mPaletteView.canUndo());
//        mRedoView.setEnabled(mPaletteView.canRedo());
//    }

    private void openPreView() {
        mBottomUndoLayout.setVisibility(View.GONE);
        mBrushImg.setVisibility(View.GONE);
        mPreView.setVisibility(View.VISIBLE);
        mPreView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return true;
            }
        });

        Bitmap resultBitmap = getResultBitmap();
        mPreView.setImageBitmap(resultBitmap);
    }

    public Bitmap getResultBitmap() {
        if (mBackBmp != null) {
            Bitmap bm = mPaletteView.buildBitmap();

            Bitmap bitmap2 = Bitmap.createBitmap(mScreenWidth, mScreenHeight,
                    Bitmap.Config.ARGB_8888);
            Paint paint = new Paint();
            Canvas canvas = new Canvas(bitmap2);

            canvas.drawBitmap(mBackBmp, new Matrix(), paint);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
            canvas.drawBitmap(bm, new Matrix(), paint);

            return bitmap2;
        } else {
            return null;
        }
    }

    private static String saveImage(Bitmap bmp, int quality) {
        if (bmp == null) {
            return null;
        }
        File appDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        if (appDir == null) {
            return null;
        }
        String fileName = System.currentTimeMillis() + ".png";
        File file = new File(appDir, fileName);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.PNG, quality, fos);
            fos.flush();
            return file.getAbsolutePath();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    private static void scanFile(Context context, String filePath) {
        Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        scanIntent.setData(Uri.fromFile(new File(filePath)));
        context.sendBroadcast(scanIntent);
    }

    public Bitmap getClipBitmap(Bitmap bitmap2) {
        if (bitmap2 == null) {
            return bitmap2;
        }

        //边沿裁剪
        PaletteView.EdgeOfPath EdgeOfPath = mPaletteView.EdgeOfPath;
        //没有移动不裁剪
        if (EdgeOfPath.bottom == EdgeOfPath.top) {
            return bitmap2;
        }
        //如果x，y在0，0右下角超过50，50那么就少50距离
        EdgeOfPath.left = EdgeOfPath.left - 50 < 0 ? 0 : EdgeOfPath.left - 50;
        EdgeOfPath.top = EdgeOfPath.top - 50 < 0 ? 0 : EdgeOfPath.top - 50;
        //处理之后的坐标
        int x = EdgeOfPath.left;
        int y = EdgeOfPath.top;

        //边缘不能超过屏幕大小，
        EdgeOfPath.bottom = EdgeOfPath.bottom > mScreenHeight ? mScreenHeight : EdgeOfPath.bottom;
        EdgeOfPath.right = EdgeOfPath.right > mScreenWidth ? mScreenWidth : EdgeOfPath.right;

        //如果不接近屏幕边缘，那么扩大100
        int w = EdgeOfPath.right + 100 > bitmap2.getWidth() ? EdgeOfPath.right - EdgeOfPath.left : EdgeOfPath.right - EdgeOfPath.left + 100;
        int h = EdgeOfPath.bottom + 100 > bitmap2.getHeight() ? EdgeOfPath.bottom - EdgeOfPath.top : EdgeOfPath.bottom - EdgeOfPath.top + 100;
        absoluteX = (float) (x + w / 2) / bitmap2.getWidth();
        absoluteY = (float) (y + h / 2) / bitmap2.getHeight();
        scale = (float) w / bitmap2.getWidth();

        bitmap2 = Bitmap.createBitmap(bitmap2, x, y, w, h);
        return bitmap2;
    }

    private void goCompletePage() {
//        if (mSaveList == null)
//            return;
//        Intent intent = new Intent(PaletteEraserActivity.this, ABC.class);
//        intent.putStringArrayListExtra("save_list", mSaveList);
//        intent.putExtra("absoluteX", absoluteX);
//        intent.putExtra("absoluteY", absoluteY);
//        intent.putExtra("scale", scale);
//        startActivity(intent);
        Toast.makeText(this, "已保存到相册", Toast.LENGTH_SHORT).show();
    }

    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    @Override
    protected void onDestroy() {
        if (mSaveTask != null && !mSaveTask.isCancelled()) {
            mSaveTask.cancel(true);
            mSaveTask = null;
        }
        super.onDestroy();
    }

    public class ImageSaveTask extends AsyncTask<Void, Void, Boolean> {
        private Bitmap resultBitmap;

        @Override
        protected void onPreExecute() {
            resultBitmap = getResultBitmap();
            resultBitmap = getClipBitmap(resultBitmap);
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(Void... voids) {

            String mSavedFile = saveImage(resultBitmap, 100);
            String mSavedFile2 = saveImage(mBackBmp, 100);
            mSaveList.add(mSavedFile);
            mSaveList.add(mSavedFile2);
            if (mSavedFile != null) {
                scanFile(PaletteEraserActivity.this, mSavedFile);
                return true;
            } else {
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            if (aBoolean) {
                goCompletePage();
            }
        }
    }
}
