package com.angcyo.uiview.viewgroup;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import com.angcyo.uiview.R;

/**
 * Copyright (C) 2016,深圳市红鸟网络科技股份有限公司 All rights reserved.
 * 项目名称：
 * 类的描述：在不影响原有的背景上, 再添加一层背景颜色
 * 创建人员：Robi
 * 创建时间：2017/04/27 16:00
 * 修改人员：Robi
 * 修改时间：2017/04/27 16:00
 * 修改备注：
 * Version: 1.0.0
 */
public class RLinearLayout extends LinearLayout {

    private Drawable mBackgroundDrawable;

    public RLinearLayout(Context context) {
        this(context, null);
    }

    public RLinearLayout(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RLinearLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.RLinearLayout);
        mBackgroundDrawable = typedArray.getDrawable(R.styleable.RLinearLayout_r_background);
        typedArray.recycle();
        initLayout();
    }

    private void initLayout() {
        setWillNotDraw(false);
    }

    @Override
    public void draw(Canvas canvas) {
        if (mBackgroundDrawable != null) {
            mBackgroundDrawable.setBounds(canvas.getClipBounds());
            mBackgroundDrawable.draw(canvas);
        }
        super.draw(canvas);
    }

    public void setRBackgroundDrawable(@ColorInt int color) {
        setRBackgroundDrawable(new ColorDrawable(color));
    }

    public void setRBackgroundDrawable(Drawable drawable) {
        mBackgroundDrawable = drawable;
    }
}
