package com.treasure.imageeraser.ui.activity;

import android.content.Intent;
import android.view.View;

import com.treasure.imageeraser.R;

import butterknife.OnClick;

public class MainActivity extends BaseActivity {

    @Override
    public void loadContentLayout() {
        setContentView(R.layout.activity_main);
    }

    @Override
    public void initView() {

    }

    @Override
    public void setListener() {

    }

    @OnClick({R.id.btn_eraser_bmp})
    public void onViewClick(View view){
        switch (view.getId()) {
            case R.id.btn_eraser_bmp:
                startActivity(new Intent(MainActivity.this,BitmapEraserActivity.class));
                break;
        }
    }
}
