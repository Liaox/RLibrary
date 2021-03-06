package com.angcyo.uiview.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.Gravity;

import com.angcyo.uiview.R;
import com.angcyo.uiview.skin.SkinHelper;

/**
 * Copyright (C) 2016,深圳市红鸟网络科技股份有限公司 All rights reserved.
 * 项目名称：
 * 类的描述：
 * 创建人员：Robi
 * 创建时间：2017/05/09 17:44
 * 修改人员：Robi
 * 修改时间：2017/05/09 17:44
 * 修改备注：
 * Version: 1.0.0
 */
public class Button extends RTextView {
    public Button(Context context) {
        this(context, null);
    }

    public Button(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public Button(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void initView() {
        super.initView();
        setGravity(Gravity.CENTER);
        setClickable(true);
        setTextColor(ColorStateList.valueOf(Color.WHITE));
        if (isInEditMode()) {
            setTextColor(Color.BLACK);
            setBackgroundResource(R.drawable.base_dark_solid_round_shape);
        } else {
            setBackground(SkinHelper.getSkin().getThemeMaskBackgroundRoundSelector());
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        if (heightMode == MeasureSpec.AT_MOST && getPaddingTop() == 0 && getPaddingBottom() == 0) {
            setMeasuredDimension(getMeasuredWidth(), getResources().getDimensionPixelOffset(R.dimen.default_button_height));
        }
    }
}
