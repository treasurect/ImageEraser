package com.treasure.imageeraser.ui.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;

import butterknife.ButterKnife;

/**
 * Created by treasure on 2018/4/26.
 * <p>
 * ------->   treasure <-------
 */

public abstract class BaseActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        loadContentLayout();
        ButterKnife.bind(this);
        initView();
        setListener();
    }
    public abstract void loadContentLayout();
    public abstract void initView();
    public abstract void setListener();

}
