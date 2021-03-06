package com.angcyo.uiview.rsen;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.IntDef;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.OverScroller;
import android.widget.TextView;

import com.angcyo.uiview.R;
import com.angcyo.uiview.skin.SkinHelper;
import com.angcyo.uiview.utils.T_;
import com.angcyo.uiview.widget.RTextView;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;

/**
 * Copyright (C) 2016,深圳市红鸟网络科技股份有限公司 All rights reserved.
 * 项目名称：
 * 类的描述：刷新控件
 * 创建人员：Robi
 * 创建时间：2016/12/05 17:30
 * 修改人员：Robi
 * 修改时间：2016/12/05 17:30
 * 修改备注：
 * Version: 1.0.0
 */
public class RefreshLayout extends ViewGroup {
    /**
     * 不支持刷新和上拉
     */
    public static final int NONE = -1;
    /**
     * 支持刷新,或者刷新中
     */
    public static final int TOP = 1;
    /**
     * 支持上拉,或者上拉中
     */
    public static final int BOTTOM = 2;
    /**
     * 支持刷新和上拉
     */
    public static final int BOTH = 3;
    /**
     * 刷新,上拉控件 正在移动中
     */
    public static final int MOVE = 4;
    /**
     * 刷新,上拉 完成, 但是手指还在拖动
     */
    public static final int FINISH = 5;
    /**
     * 正常
     */
    public static final int NORMAL = 0;
    public static final String TOP_VIEW = "top_view";
    public static final String BOTTOM_VIEW = "bottom_view";
    public static final String TARGET_VIEW = "target_view";
    public static final String TIP_VIEW = "tip_view";
    public static final long ANIM_TIME = 300;
    protected View mTopView, mBottomView, mTargetView, mTipView;
    float downY, downX, lastY;
    private OverScroller mScroller;
    private int mTouchSlop;
    /**
     * 支持的滚动方向
     */
    @Direction
    private int mDirection = BOTH;
    /**
     * 当前刷新的状态
     */
    @State
    private int mCurState = NORMAL;
    /**
     * 手指未离屏
     */
    private boolean isTouchDown = false;
    /**
     * 刷新的意向, 比如刷新的时候抓起了View, 那么不允许上拉加载
     */
    private int order = NONE;
    /**
     * 是否激活延迟加载, 防止刷新太快,就结束了.
     */
    private boolean delayLoadEnd = true;
    private long refreshTime = 0;
    /**
     * 是否需要通知事件, 如果为false, 那么只有滑动效果, 没有事件监听
     */
    private boolean mNotifyListener = true;
    private boolean mShowTip = false;
    Runnable hideTipRunnable = new Runnable() {
        @Override
        public void run() {
            mShowTip = false;

            mTipView.clearAnimation();
            ScaleAnimation scaleAnimation = new ScaleAnimation(1, 1, 1, 0,
                    Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0f);
            scaleAnimation.setDuration(ANIM_TIME);
            scaleAnimation.setFillAfter(true);
            mTipView.startAnimation(scaleAnimation);
        }
    };
    private ArrayList<OnTopViewMoveListener> mTopViewMoveListeners = new ArrayList<>();
    private ArrayList<OnBottomViewMoveListener> mBottomViewMoveListeners = new ArrayList<>();
    private ArrayList<OnRefreshListener> mRefreshListeners = new ArrayList<>();

    public RefreshLayout(Context context) {
        this(context, null);
    }

    public RefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        initRefreshView();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        if (isInEditMode()) {
            mTargetView.measure(MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY));
            return;
        }

        if (mTargetView != null) {
            mTargetView.measure(MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY));
        }
        if (mTopView != null) {
            mTopView.measure(MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.AT_MOST));
        }
        if (mBottomView != null) {
            mBottomView.measure(MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.AT_MOST));
        }
        if (mTipView != null) {
            mTipView.measure(MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.AT_MOST));
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

        if (isInEditMode()) {
            mTargetView.layout(0, 0, r, getMeasuredHeight());
            return;
        }

        if (mTargetView != null) {
            mTargetView.layout(0, 0, r, getMeasuredHeight());
        }
        if (mTopView != null) {
            //自动居中布局
            mTopView.layout((r - l) / 2 - mTopView.getMeasuredWidth() / 2, -mTopView.getMeasuredHeight(),
                    (r - l) / 2 + mTopView.getMeasuredWidth() / 2, 0);
        }
        if (mBottomView != null) {
            //自动居中布局
            mBottomView.layout((r - l) / 2 - mBottomView.getMeasuredWidth() / 2, b,
                    (r - l) / 2 + mBottomView.getMeasuredWidth() / 2, b + mBottomView.getMeasuredHeight());
        }

        layoutTipView();
    }

    private void layoutTipView() {
        if (!mShowTip) {
            return;
        }
        if (mTipView != null) {
            mTipView.bringToFront();
            int top = getScrollY();
            mTipView.layout(0, top, getMeasuredWidth(), top + mTipView.getMeasuredHeight());
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (mCurState == TOP) {
            scrollToTop(false);
        } else if (mCurState == BOTTOM) {
            scrollToBottom(false);
        } else {
            resetScroll(false);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mTouchSlop = 0;//ViewConfiguration.get(getContext()).getScaledTouchSlop();

        if (!isInEditMode()) {
            if (mTopView.getParent() == null) {
                addView(mTopView);
            }
            if (mBottomView.getParent() == null) {
                addView(mBottomView);
            }
            if (mTipView.getParent() == null) {
                addView(mTipView);
            }
        }
    }

    @Override
    public void addView(View child, int index, LayoutParams params) {
        super.addView(child, index, params);
        for (int i = 0; i < getChildCount() && mTargetView == null; i++) {
            final View childAt = getChildAt(i);

            if (childAt instanceof OnBottomViewMoveListener &&
                    childAt instanceof OnTopViewMoveListener) {
                continue;
            }

            if (childAt instanceof OnBottomViewMoveListener) {
                mBottomView = childAt;
            } else if (childAt instanceof OnTopViewMoveListener) {
                mTopView = childAt;
            } else if (childAt == mTipView) {
            } else {
                mTargetView = childAt;
            }
        }
    }

    protected void initRefreshView() {
        mScroller = new OverScroller(getContext(), new DecelerateInterpolator());

        if (isInEditMode()) {
            return;
        }
        if (mTopView == null) {
            mTopView = new BaseRefreshTopView(getContext());
            mTopView.setTag(TOP_VIEW);
        }
        if (mBottomView == null) {
            mBottomView = new BaseRefreshBottomView(getContext());
            mBottomView.setTag(TOP_VIEW);
        }
        if (mTipView == null) {
            RTextView textView = new RTextView(getContext());
            textView.setGravity(Gravity.CENTER);
            textView.setTextColor(Color.WHITE);
            textView.setRBackgroundColor(SkinHelper.getSkin().getThemeSubColor());
            textView.setBackgroundResource(R.drawable.base_bg_selector);
            textView.setText("测试专用...");
            textView.setTag(TIP_VIEW);
            int left = getResources().getDimensionPixelOffset(R.dimen.base_xhdpi);
            int top = getResources().getDimensionPixelOffset(R.dimen.base_ldpi);
            textView.setPadding(left, top, left, top);
            mTipView = textView;

            mTipView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    T_.show("Test");
                }
            });
        }
    }

    public void setTopView(View topView) {
        mTopView = topView;
        mTopView.setTag(TOP_VIEW);
    }

    public void setBottomView(View bottomView) {
        mBottomView = bottomView;
        mBottomView.setTag(TOP_VIEW);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        View topView = findViewWithTag(TOP_VIEW);
        if (topView != null) {
            setTopView(topView);
        }

        View bottomView = findViewWithTag(BOTTOM_VIEW);
        if (bottomView != null) {
            setBottomView(bottomView);
        }

        View targetView = findViewWithTag(TARGET_VIEW);
        if (targetView != null) {
            mTargetView = targetView;
        }
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            int currY = mScroller.getCurrY();
            if (currY == 0 && mCurState == FINISH) {
                mCurState = NORMAL;
            }
            scrollTo(mScroller.getCurrX(), currY);
            postInvalidate();
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return super.onInterceptTouchEvent(event);
        }
        int action = MotionEventCompat.getActionMasked(event);//event.getActionMasked();

        if (action == MotionEvent.ACTION_DOWN) {
            handleTouchDown(event);
        } else if (action == MotionEvent.ACTION_MOVE) {
            float y = event.getY();
            float x = event.getX();
            float dy = y - downY;
            float dx = x - downX;
            if ((Math.abs(dy) > Math.abs(dx)) && Math.abs(dy) > mTouchSlop) {
                int scrollY = getScrollY();
                if (mCurState == TOP && dy < 0 && scrollY < 0) {
                    //如果已经处理加载状态, 通过滚动, View 隐藏, 使得内容全屏显示
                    scrollTo(0, (int) Math.min(0, (scrollY - dy)));
                    downY = event.getY();
                    downX = event.getX();
                    //L.e("call: onInterceptTouchEvent([event])-> 1");
                    return super.onInterceptTouchEvent(event);
                } else if (mCurState == BOTTOM && dy > 0 && scrollY > 0) {
                    scrollTo(0, (int) Math.max(0, scrollY - dy));
                    downY = event.getY();
                    downX = event.getX();
                    //L.e("call: onInterceptTouchEvent([event])-> 2");
                    return super.onInterceptTouchEvent(event);
                } else {
                    if (dy > 0 && canScrollDown() &&
                            !innerCanChildScrollVertically(mTargetView, -1, event.getRawX(), event.getRawY())) {
                        order = TOP;
                        //L.e("call: onInterceptTouchEvent([event])-> 3");
                        return true;
                    } else if (dy < 0 && canScrollUp() &&
                            !innerCanChildScrollVertically(mTargetView, 1, event.getRawX(), event.getRawY())) {
                        order = BOTTOM;
                        //L.e("call: onInterceptTouchEvent([event])-> 4");
                        return true;
                    } else {
                        if (getScrollY() > 0 && dy > 0) {
                            return true;
                        }
                        if (getScrollY() < 0 && dy < 0) {
                            return true;
                        }
                    }
                }
            }
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            int count = event.getPointerCount();
            if (count == 1) {
                isTouchDown = false;
            }

            boolean interceptTouchEvent = super.onInterceptTouchEvent(event);
            if (!interceptTouchEvent) {
                handleTouchUp();
                //L.e("call: onInterceptTouchEvent([event])-> 5 " + interceptTouchEvent);
                return interceptTouchEvent;
            }
        }
        return super.onInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = MotionEventCompat.getActionMasked(event);

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            int count = event.getPointerCount();
            if (count == 1) {
                isTouchDown = false;
            }
            handleTouchUp();
        } else if (action == MotionEvent.ACTION_MOVE) {
            float y = event.getY();
            float dy = lastY - y;
            isTouchDown = true;

            if (order == NONE) {
                if (dy > 0) {
                    order = BOTTOM;
                } else {
                    order = TOP;
                }
            }

            if (Math.abs(dy) > mTouchSlop) {
                scrollBy(0, (int) (dy * (1 - 0.4 - Math.abs(getScrollY()) * 1.f / getMeasuredHeight())));
                lastY = y;
                if (mCurState == NORMAL /*|| mCurState == FINISH*/) {
                    mCurState = MOVE;
                }
            }
        } else if (action == MotionEvent.ACTION_POINTER_DOWN) {
            //多个手指按下
            lastY = event.getY();
        } else if (action == MotionEvent.ACTION_DOWN) {
            handleTouchDown(event);
        }
        return true;
    }

    private void handleTouchDown(MotionEvent event) {
        downY = event.getY();
        downX = event.getX();
        lastY = downY;
        mScroller.abortAnimation();
    }

    /**
     * 释放手指之后的处理
     */
    private void handleTouchUp() {
        int scrollY = getScrollY();
        int rawY = Math.abs(scrollY);

        int topHeight = mTopView.getMeasuredHeight();
        int bottomHeight = mBottomView.getMeasuredHeight();

        if (order == NONE) {
            if (scrollY != 0) {
                if (mCurState == FINISH || mCurState == NORMAL) {
                    resetScroll();
                } else if (mCurState == TOP && scrollY > -topHeight) {
                    resetScroll();
                } else if (mCurState == BOTTOM && scrollY < bottomHeight) {
                    resetScroll();
                }
            }
            return;
        }

        order = NONE;

        if (!mNotifyListener) {
            resetScroll();
            return;
        }

        if (scrollY < 0) {
            //处理刷新
            if (mTopView == null || mCurState == FINISH) {
                resetScroll();
                return;
            }

            if (rawY >= topHeight) {
                refreshTop();
            } else {
                resetScroll();
            }
        } else if (scrollY > 0) {
            //处理加载
            if (mBottomView == null || mCurState == FINISH) {
                resetScroll();
                return;
            }

            if (rawY >= bottomHeight) {
                refreshBottom();
            } else {
                resetScroll();
            }
        }

    }

    /**
     * 设置支持刷新的方向
     */
    public void setRefreshDirection(@Direction int direction) {
        mDirection = direction;
    }

    /**
     * 设置当前刷新的状态
     */
    public void setRefreshState(@State int state) {
        if (mCurState == NORMAL || mCurState == FINISH) {
            if (state == TOP) {
                if (mDirection == TOP || mDirection == BOTH) {
                    refreshTop();
                }
            } else if (state == BOTTOM) {
                if (mDirection == BOTTOM || mDirection == BOTH) {
                    refreshBottom();
                }
            }
        } else {
            if (mCurState == TOP && TOP == state) {
                refreshTop();
            }
            if (mCurState == BOTTOM && BOTTOM == state) {
                refreshBottom();
            }
            return;
        }
    }

    /**
     * 结束刷新
     */
    public void setRefreshEnd() {
        /**如果激活了延迟加载, ...*/
        if (delayLoadEnd && System.currentTimeMillis() - refreshTime < 600) {
            post(new Runnable() {
                @Override
                public void run() {
                    setRefreshEnd();
                }
            });
            return;
        }

//        if (mCurState == FINISH || mCurState == NORMAL /*|| mCurState == MOVE*/) {
//            return;
//        }
//
//        if (mCurState == MOVE && isTouchDown) {
//            return;
//        }

        mCurState = FINISH;
        if (isTouchDown) {
            //scrollTo(getScrollX(), getScrollY());
        } else {
            startScroll(0);
        }
    }

    public void setRefreshEnd(boolean anim) {
        if (anim) {
            setRefreshEnd();
        } else {
            mCurState = FINISH;
            if (!isTouchDown) {
                scrollTo(0, 0);
            }
        }
    }

    private void scrollToTop(boolean anim) {
        if (mTopView != null) {
            if (anim) {
                startScroll(-mTopView.getMeasuredHeight());
            } else {
                scrollTo(0, -mTopView.getMeasuredHeight());
            }
        }
    }

    private void scrollToBottom(boolean anim) {
        if (mBottomView != null) {
            if (anim) {
                startScroll(mBottomView.getMeasuredHeight());
            } else {
                scrollTo(0, mBottomView.getMeasuredHeight());
            }
        }
    }

    private void refreshTop() {
        refreshTime = System.currentTimeMillis();
        if (mTopView != null) {
            //设置正在刷新
            mCurState = TOP;
            scrollToTop(true);

            //防止还没回到刷新位置, 就已经调用了刷新结束的方法
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    for (OnRefreshListener listener : mRefreshListeners) {
                        listener.onRefresh(TOP);
                    }
                }
            }, 100);
        }
    }

    private void refreshBottom() {
        refreshTime = System.currentTimeMillis();
        if (mBottomView != null) {
            //设置正在上拉
            mCurState = BOTTOM;
            scrollToBottom(true);

            postDelayed(new Runnable() {
                @Override
                public void run() {
                    for (OnRefreshListener listener : mRefreshListeners) {
                        listener.onRefresh(BOTTOM);
                    }
                }
            }, 100);

        }
    }

    /**
     * 恢复到默认的滚动状态
     */
    private void resetScroll() {
        resetScroll(true);
    }

    private void resetScroll(boolean anim) {
        mScroller.abortAnimation();
        if (mCurState != TOP && mCurState != BOTTOM /*&& mCurState != FINISH*/) {
            mCurState = NORMAL;
        }
        if (anim) {
            startScroll(0);
        } else {
            scrollTo(0, 0);
        }
    }

    private void startScroll(int to) {
        int scrollY = getScrollY();
        mScroller.startScroll(0, scrollY, 0, to - scrollY);
        postInvalidate();
    }

    @Override
    public void scrollBy(int x, int y) {
        int scrollY = getScrollY();
        final int endY = scrollY + y;

        if (order == TOP) {
            if (endY > 0) {
                y = -scrollY;
            }
        } else if (order == BOTTOM) {
            if (endY < 0) {
                y = -scrollY;
            }
        }
        super.scrollBy(x, y);
    }

    @Override
    public void scrollTo(int x, int y) {
        if (getScrollY() == 0) {
            if (y < 0) {
                //需要滚动刷新, 判断刷新的方向是否支持刷新. 不支持不处理滚动
                if (mDirection == TOP || mDirection == BOTH) {

                } else {
                    return;
                }
            }
            if (y > 0) {
                //需要上拉加载
                if (mDirection == BOTTOM || mDirection == BOTH) {

                } else {
                    return;
                }
            }
        }

        //修正滚动y值
        if (mDirection == TOP) {
            y = Math.min(y, 0);
        }
        if (mDirection == BOTTOM) {
            y = Math.max(y, 0);
        }

        if (mCurState == TOP) {
            y = Math.min(y, 0);
        } else if (mCurState == BOTTOM) {
            y = Math.max(y, 0);
        }

        super.scrollTo(0, y);

        layoutTipView();

        int scrollY = getScrollY();
        int rawY = Math.abs(scrollY);

        if (scrollY < 0) {
            //刷新
            notifyTopListener(rawY);
        } else if (scrollY > 0) {
            //加载
            notifyBottomListener(rawY);
        } else {
            if (mCurState == FINISH || mCurState == NORMAL) {
                mCurState = NORMAL;
                notifyTopListener(rawY);
                notifyBottomListener(rawY);
            }
        }
    }

    private void notifyBottomListener(int rawY) {
        if (mBottomView != null /*&& mCurState != BOTTOM*/) {
            if (mBottomView instanceof OnBottomViewMoveListener
                    && !mBottomViewMoveListeners.contains(mBottomView)) {
                ((OnBottomViewMoveListener) mBottomView).onBottomMoveTo(this, rawY, mBottomView.getMeasuredHeight(), mCurState);
            }
            for (OnBottomViewMoveListener listener : mBottomViewMoveListeners) {
                listener.onBottomMoveTo(mBottomView, rawY, mBottomView.getMeasuredHeight(), mCurState);
            }
        }
    }

    private void notifyTopListener(int rawY) {
        if (mTopView != null /*&& mCurState != TOP*/) {
            if (mTopView instanceof OnTopViewMoveListener
                    && !mTopViewMoveListeners.contains(mTopView)) {
                ((OnTopViewMoveListener) mTopView).onTopMoveTo(this, rawY, mTopView.getMeasuredHeight(), mCurState);
            }
            for (OnTopViewMoveListener listener : mTopViewMoveListeners) {
                listener.onTopMoveTo(mTopView, rawY, mTopView.getMeasuredHeight(), mCurState);
            }
        }
    }

    public RefreshLayout addTopViewMoveListener(OnTopViewMoveListener listener) {
        mTopViewMoveListeners.add(listener);
        return this;
    }

    public RefreshLayout addBottomViewMoveListener(OnBottomViewMoveListener listener) {
        mBottomViewMoveListeners.add(listener);
        return this;
    }

    public RefreshLayout addOnRefreshListener(OnRefreshListener listener) {
        mRefreshListeners.add(listener);
        return this;
    }

    public RefreshLayout removeTopViewMoveListener(OnTopViewMoveListener listener) {
        mTopViewMoveListeners.remove(listener);
        return this;
    }

    public RefreshLayout removeBottomViewMoveListener(OnBottomViewMoveListener listener) {
        mBottomViewMoveListeners.remove(listener);
        return this;
    }

    public RefreshLayout removeRefreshListener(OnRefreshListener listener) {
        mRefreshListeners.remove(listener);
        return this;
    }

    /**
     * 是否拦截向下滚动, 影响下拉刷新的功能**
     *
     * @return true 激活下拉刷新功能
     */
    private boolean canScrollDown() {
        if (isEnabled() && mTopView != null &&
                (mDirection == TOP || mDirection == BOTH)) {
            if (mCurState == BOTTOM) {
                //如果当前正在上拉加载,则禁止刷新功能, 当然~~~你可以取消此限制
                return false;
            }
            return true;
        }

        return false;
    }

    /**
     * 是否拦截向上滚动, 影响上拉加载的功能
     *
     * @return true 激活上拉加载功能
     */
    private boolean canScrollUp() {
        if (isEnabled() && mBottomView != null &&
                (mDirection == BOTTOM || mDirection == BOTH)) {
            if (mCurState == TOP) {
                //如果当前正在下拉刷新,则禁止上拉功能, 当然~~~你可以取消此限制
                return false;
            }
            return true;
        }

        return false;
    }

    /**
     * Child是否可以滚动
     *
     * @param direction 如果是大于0, 表示视图底部没有数据了, 即不能向上滚动了, 反之...
     */
    protected boolean innerCanChildScrollVertically(View view, int direction, float rawX, float rawY) {
        //项目特殊处理,可以注释掉
        if (view instanceof RecyclerView) {
            RecyclerView recyclerView = (RecyclerView) view;
            if (recyclerView.getChildCount() > 0) {
                View childAt = recyclerView.getChildAt(0);
                Rect rect = new Rect();
                childAt.getGlobalVisibleRect(rect);
                if (childAt instanceof RecyclerView && rect.contains(((int) rawX), (int) rawY)) {
                    return ViewCompat.canScrollVertically(childAt, direction);
                }
            }
            return ViewCompat.canScrollVertically(view, direction);
        }
        //---------------ebd-----------------

        if (view instanceof ViewGroup) {
            final ViewGroup vGroup = (ViewGroup) view;
            View child;
            boolean result;
            for (int i = 0; i < vGroup.getChildCount(); i++) {
                child = vGroup.getChildAt(i);
                if (child instanceof View) {
                    result = ViewCompat.canScrollVertically(child, direction);
                } else {
                    result = innerCanChildScrollVertically(child, direction, rawX, rawY);
                }

                if (result) {
                    return true;
                }
            }
        }

        return ViewCompat.canScrollVertically(view, direction);
    }

    public void setNotifyListener(boolean notifyListener) {
        this.mNotifyListener = notifyListener;
    }

    public void setShowTip(String tip) {
        boolean oldShow = mShowTip;
        mShowTip = true;
        ((TextView) mTipView).setText(tip);

        if (oldShow) {
            mTipView.removeCallbacks(hideTipRunnable);
        } else {
            mTipView.clearAnimation();
            ScaleAnimation scaleAnimation = new ScaleAnimation(0.6f, 1, 1, 1,
                    Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            scaleAnimation.setDuration(ANIM_TIME);
            mTipView.startAnimation(scaleAnimation);
        }
        mTipView.postDelayed(hideTipRunnable, 1000);
    }

    /**
     * 支持的刷新方向
     */
    @IntDef({TOP, BOTTOM, BOTH, NONE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Direction {
    }

    /**
     * 当前的刷新状态
     */
    @IntDef({TOP, BOTTOM, NORMAL, MOVE, FINISH})
    @Retention(RetentionPolicy.SOURCE)
    public @interface State {
    }

    public interface OnTopViewMoveListener {
        /**
         * @param top       距离父View顶部的距离
         * @param maxHeight view的高度
         */
        void onTopMoveTo(View view, int top, int maxHeight, @State int state);
    }

    public interface OnBottomViewMoveListener {
        /**
         * @param bottom    距离父View底部的距离
         * @param maxHeight view的高度
         */
        void onBottomMoveTo(View view, int bottom, int maxHeight, @State int state);
    }

    /**
     * 刷新,上拉回调
     */
    public interface OnRefreshListener {
        void onRefresh(@Direction int direction);
    }

    /**
     * 基类用来实现 下拉/上拉 放大缩小指示图, 加载中进度变化
     */
    public static abstract class BaseRefreshView extends View implements OnBottomViewMoveListener, OnTopViewMoveListener {

        Drawable mDrawable;
        Bitmap mBitmap;
        Rect mCenterRect, mProgressRect, mDrawRect;
        Paint mPaint;
        ValueAnimator mObjectAnimator;
        float mProgress = 0;
        private PorterDuffXfermode mXfermodeDstIn;
        private int mMoveOffset = 0;
        private int mLastMoveOffset = 0;
        private int mTouchSlop;

        public BaseRefreshView(Context context) {
            super(context);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            mDrawable.draw(canvas);
            int sc = canvas.saveLayer(mCenterRect.left, mCenterRect.top,
                    mCenterRect.right, mCenterRect.bottom,
                    null, Canvas.ALL_SAVE_FLAG);
            canvas.drawRect(mProgressRect, mPaint);
            mPaint.setXfermode(mXfermodeDstIn);
            canvas.drawBitmap(mBitmap, mCenterRect.left, mCenterRect.top, mPaint);
            mPaint.setXfermode(null);
            canvas.restoreToCount(sc);
        }

        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            mBitmap.recycle();
            mBitmap = null;
            mDrawable = null;
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            mDrawable = getResources().getDrawable(R.drawable.base_refresh_top_book);
            mBitmap = getBitmapFromDrawable();
            mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mPaint.setColor(SkinHelper.getSkin().getThemeSubColor());
            mCenterRect = new Rect();
            mProgressRect = new Rect();
            mDrawRect = new Rect();
            mXfermodeDstIn = new PorterDuffXfermode(PorterDuff.Mode.DST_IN);

            mTouchSlop = 0;//(int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics());
            //ViewConfiguration.get(getContext()).getScaledTouchSlop();

            int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics());
            setPadding(0, padding, 0, padding);

            mObjectAnimator = ObjectAnimator.ofFloat(0f, 1f);
            mObjectAnimator.setInterpolator(new LinearInterpolator());
            mObjectAnimator.setRepeatCount(ValueAnimator.INFINITE);
            mObjectAnimator.setRepeatMode(ValueAnimator.RESTART);
            mObjectAnimator.setDuration(1000);

            mObjectAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float value = (float) animation.getAnimatedValue();
                    updateProgress(value);
                }
            });
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int height = mBitmap.getHeight();
            setMeasuredDimension(widthMeasureSpec, height + getPaddingTop() + getPaddingBottom());
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            centerRect();
            updateMove(mMoveOffset, h);
        }

        /**
         * 计算小图显示的矩形区域
         */
        protected abstract void centerRect();

        /**
         * 加载中的进度刷新
         */
        protected abstract void updateProgress(float progress);

        /**
         * 移动的时候,用来放大缩小指示图
         */
        protected abstract void updateMove(int move, int maxHeight);

        protected void startProgress() {
            if (!mObjectAnimator.isRunning()) {
                mObjectAnimator.start();
            }
        }

        protected void endProgress() {
            if (mObjectAnimator.isRunning()) {
                mObjectAnimator.end();
            }
            updateProgress(1);
        }


        private Bitmap getBitmapFromDrawable() {
            return BitmapFactory.decodeResource(getResources(), R.drawable.base_refresh_top_book);
        }

        private void onMove(int move, int maxHeight, @State int state) {
            if (state == FINISH) {
                endProgress();
            } else if (state == MOVE) {
                if (Math.abs(move - mLastMoveOffset) > mTouchSlop) {
                    updateMove(move, maxHeight);
                    mLastMoveOffset = move;
                }
            } else if (state == TOP || state == BOTTOM) {
                startProgress();
            } else if (state == NORMAL) {
                updateProgress(0);
            }

        }

        @Override
        public void onBottomMoveTo(View view, int bottom, int maxHeight, @State int state) {
            onMove(bottom, maxHeight, state);

        }

        @Override
        public void onTopMoveTo(View view, int top, int maxHeight, @State int state) {
            onMove(top, maxHeight, state);
        }
    }

    /**
     * 默认实现的刷新布局
     */
    public static class BaseRefreshTopView extends BaseRefreshView {


        public BaseRefreshTopView(Context context) {
            super(context);
        }

        @Override
        protected void centerRect() {
            int viewWidth = getMeasuredWidth();
            int viewHeight = getMeasuredHeight() - getPaddingTop() - getPaddingBottom();
            int width = mBitmap.getWidth();
            int height = mBitmap.getHeight();
            mCenterRect.set(viewWidth / 2 - width / 2, getPaddingTop() + viewHeight / 2 - height / 2,
                    viewWidth / 2 + width / 2, getPaddingTop() + viewHeight / 2 + height / 2);
        }

        @Override
        protected void updateProgress(float progress) {
            if (mProgress == progress) {
                return;
            }
            mProgress = progress;
            mProgressRect.set(mCenterRect.left, (int) (mCenterRect.bottom - (mCenterRect.height() * progress)),
                    mCenterRect.right, mCenterRect.bottom);
            postInvalidate();
        }

        @Override
        protected void updateMove(int move, int maxHeight) {
            if (move <= getPaddingBottom()) {
                return;
            }

            int viewWidth = getMeasuredWidth();
            int viewHeight = getMeasuredHeight();
            int rawTop = move - getPaddingBottom();
            rawTop = Math.min(rawTop, maxHeight - getPaddingBottom() - getPaddingTop());

            int left = viewWidth / 2 - rawTop / 2;
            mDrawRect.set(left, viewHeight - rawTop - getPaddingBottom(), viewWidth / 2 + rawTop / 2, viewHeight - getPaddingBottom());
            mDrawable.setBounds(mDrawRect);
            postInvalidate();
        }
    }

    /**
     * 默认实现的上拉视图
     */
    public static class BaseRefreshBottomView extends BaseRefreshView {


        public BaseRefreshBottomView(Context context) {
            super(context);
        }

        @Override
        protected void centerRect() {
            int viewWidth = getMeasuredWidth();
            int viewHeight = getMeasuredHeight() - getPaddingTop() - getPaddingBottom();
            int width = mBitmap.getWidth();
            int height = mBitmap.getHeight();
            mCenterRect.set(viewWidth / 2 - width / 2, getPaddingTop() + viewHeight / 2 - height / 2,
                    viewWidth / 2 + width / 2, getPaddingTop() + viewHeight / 2 + height / 2);
        }

        @Override
        protected void updateProgress(float progress) {
            if (mProgress == progress) {
                return;
            }
            mProgress = progress;
            mProgressRect.set(mCenterRect.left, getPaddingTop(),
                    mCenterRect.right, (int) (getPaddingTop() + mCenterRect.height() * progress));
            postInvalidate();
        }

        @Override
        protected void updateMove(int move, int maxHeight) {
            if (move <= getPaddingTop()) {
                return;
            }

            int viewWidth = getMeasuredWidth();
            int viewHeight = getMeasuredHeight();
            int rawBottom = move - getPaddingTop();
            rawBottom = Math.min(rawBottom, maxHeight - getPaddingBottom() - getPaddingTop());

            int left = viewWidth / 2 - rawBottom / 2;
            mDrawRect.set(left, getPaddingTop(), viewWidth / 2 + rawBottom / 2, getPaddingTop() + rawBottom);
            mDrawable.setBounds(mDrawRect);
            postInvalidate();
        }
    }
}
