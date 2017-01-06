package com.bemaxnet.resume.ui.helper;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Build;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.LayoutManager;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;
import android.widget.AbsListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;

@SuppressLint("ClickableViewAccessibility")
@SuppressWarnings("unused")
public class ZSwipeRefreshLayout extends ViewGroup {
    private static final int HEADER_VIEW_HEIGHT = 50;

    private static final float DECELERATE_INTERPOLATION_FACTOR = 2f;
    private static final int INVALID_POINTER = -1;
    private static final float DRAG_RATE = .5f;

    private static final int SCALE_DOWN_DURATION = 150;
    private static final int ANIMATE_TO_TRIGGER_DURATION = 200;
    private static final int ANIMATE_TO_START_DURATION = 200;
    private static final int DEFAULT_CIRCLE_TARGET = 64;

    private View mTarget;

    private OnPullRefreshListener mListener;
    private OnPushLoadMoreListener mOnPushLoadMoreListener;

    private boolean mRefreshing = false;
    private boolean mLoadMore = false;
    private int mTouchSlop;
    private float mTotalDragDistance = -1;
    private int mMediumAnimationDuration;
    private int mCurrentTargetOffsetTop;
    private boolean mOriginalOffsetCalculated = false;

    private float mInitialMotionY;
    private boolean mIsBeingDragged;
    private int mActivePointerId = INVALID_POINTER;
    private boolean mScale;

    private boolean mReturningToStart;
    private final DecelerateInterpolator mDecelerateInterpolator;
    private static final int[] LAYOUT_ATTRS = new int[]{android.R.attr.enabled};

    private HeadViewContainer mHeadViewContainer;
    private RelativeLayout mFooterViewContainer;
    private int mHeaderViewIndex = -1;
    private int mFooterViewIndex = -1;

    protected int mFrom;

    private float mStartingScale;

    protected int mOriginalOffsetTop;

    private float mSpinnerFinalOffset;

    private boolean mNotify;

    private int mHeaderViewWidth;

    private int mFooterViewWidth;

    private int mHeaderViewHeight;

    private int mFooterViewHeight;

    private boolean mUsingCustomStart;

    private boolean targetScrollWithLayout = true;

    private int pushDistance = 0;

    private CircleProgressView defaultProgressView = null;

    private boolean usingDefaultHeader = true;

    private float density = 1.0f;

    private boolean isProgressEnable = true;

    private boolean isPullEnable = true;

    private boolean isPushEnable = true;

    private Animation.AnimationListener mRefreshListener = new Animation.AnimationListener() {
        @Override
        public void onAnimationStart(Animation animation) {
            isProgressEnable = false;
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            isProgressEnable = true;
            if (mRefreshing) {
                if (mNotify) {
                    if (usingDefaultHeader) {
                        ViewCompat.setAlpha(defaultProgressView, 1.0f);
                        defaultProgressView.setOnDraw(true);
                        new Thread(defaultProgressView).start();
                    }
                    if (mListener == null) {
                        setRefreshing(false);
                    } else {
                        mListener.onRefresh();
                    }
                }
            } else {
                mHeadViewContainer.setVisibility(View.GONE);
                if (mScale) {
                    setAnimationProgress(0);
                } else {
                    setTargetOffsetTopAndBottom(mOriginalOffsetTop
                            - mCurrentTargetOffsetTop, true);
                }
            }
            mCurrentTargetOffsetTop = mHeadViewContainer.getTop();
            updateListenerCallBack();
        }
    };

    public void setPullEnable(boolean pullEnable) {
        isPullEnable = pullEnable;
    }

    public void setPushEnable(boolean pushEnable) {
        isPushEnable = pushEnable;
    }

    private void updateListenerCallBack() {
        int distance = mCurrentTargetOffsetTop + mHeadViewContainer.getHeight();
        if (mListener != null) {
            mListener.onPullDistance(distance);
        }
        if (usingDefaultHeader && isProgressEnable) {
            defaultProgressView.setPullDistance(distance);
        }
    }

    public void setHeaderView(View child) {
        if (child == null) {
            return;
        }
        if (mHeadViewContainer == null) {
            return;
        }
        usingDefaultHeader = false;
        mHeadViewContainer.removeAllViews();
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                mHeaderViewWidth, mHeaderViewHeight);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        mHeadViewContainer.addView(child, layoutParams);
    }

    public void setFooterView(View child) {
        if (child == null) {
            return;
        }
        if (mFooterViewContainer == null) {
            return;
        }
        mFooterViewContainer.removeAllViews();
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                mFooterViewWidth, mFooterViewHeight);
        mFooterViewContainer.addView(child, layoutParams);
    }

    public ZSwipeRefreshLayout(Context context) {
        this(context, null);
    }

    @SuppressWarnings("deprecation")
    public ZSwipeRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

        mMediumAnimationDuration = getResources().getInteger(
                android.R.integer.config_mediumAnimTime);

        setWillNotDraw(false);
        mDecelerateInterpolator = new DecelerateInterpolator(
                DECELERATE_INTERPOLATION_FACTOR);

        final TypedArray a = context
                .obtainStyledAttributes(attrs, LAYOUT_ATTRS);
        setEnabled(a.getBoolean(0, true));
        a.recycle();

        WindowManager wm = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        final DisplayMetrics metrics = getResources().getDisplayMetrics();
        mHeaderViewWidth = display.getWidth();
        mFooterViewWidth = display.getWidth();
        mHeaderViewHeight = (int) (HEADER_VIEW_HEIGHT * metrics.density);
        mFooterViewHeight = (int) (HEADER_VIEW_HEIGHT * metrics.density);
        defaultProgressView = new CircleProgressView(getContext());
        createHeaderViewContainer();
        createFooterViewContainer();
        ViewCompat.setChildrenDrawingOrderEnabled(this, true);
        mSpinnerFinalOffset = DEFAULT_CIRCLE_TARGET * metrics.density;
        density = metrics.density;
        mTotalDragDistance = mSpinnerFinalOffset;
    }

    @Override
    protected int getChildDrawingOrder(int childCount, int i) {
        if (mHeaderViewIndex < 0 && mFooterViewIndex < 0) {
            return i;
        }
        if (i == childCount - 2) {
            return mHeaderViewIndex;
        }
        if (i == childCount - 1) {
            return mFooterViewIndex;
        }
        int bigIndex = mFooterViewIndex > mHeaderViewIndex ? mFooterViewIndex
                : mHeaderViewIndex;
        int smallIndex = mFooterViewIndex < mHeaderViewIndex ? mFooterViewIndex
                : mHeaderViewIndex;
        if (i >= smallIndex && i < bigIndex - 1) {
            return i + 1;
        }
        if (i >= bigIndex || (i == bigIndex - 1)) {
            return i + 2;
        }
        return i;
    }

    private void createHeaderViewContainer() {
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                (int) (mHeaderViewHeight * 0.8),
                (int) (mHeaderViewHeight * 0.8));
        layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        mHeadViewContainer = new HeadViewContainer(getContext());
        mHeadViewContainer.setBackgroundColor(ContextCompat.getColor(getContext(), android.R.color.transparent));
        mHeadViewContainer.setVisibility(View.GONE);
        defaultProgressView.setVisibility(View.VISIBLE);
        defaultProgressView.setOnDraw(false);
        mHeadViewContainer.addView(defaultProgressView, layoutParams);
        addView(mHeadViewContainer);
    }

    private void createFooterViewContainer() {
        mFooterViewContainer = new RelativeLayout(getContext());
        mFooterViewContainer.setVisibility(View.GONE);
        addView(mFooterViewContainer);
    }

    public void setOnPullRefreshListener(OnPullRefreshListener listener) {
        mListener = listener;
    }

    public void setHeaderViewBackgroundColor(int color) {
        mHeadViewContainer.setBackgroundColor(color);
    }

    public void setOnPushLoadMoreListener(
            OnPushLoadMoreListener onPushLoadMoreListener) {
        this.mOnPushLoadMoreListener = onPushLoadMoreListener;
    }

    public void setRefreshing(boolean refreshing) {
        if (refreshing && !mRefreshing) {

            mRefreshing = true;
            int endTarget;
            if (!mUsingCustomStart) {
                endTarget = (int) (mSpinnerFinalOffset + mOriginalOffsetTop);
            } else {
                endTarget = (int) mSpinnerFinalOffset;
            }
            setTargetOffsetTopAndBottom(endTarget - mCurrentTargetOffsetTop, true);
            mNotify = false;
            startScaleUpAnimation(mRefreshListener);
        } else {
            setRefreshing(refreshing, false);
            if (usingDefaultHeader) {
                defaultProgressView.setOnDraw(false);
            }
        }
    }

    private void startScaleUpAnimation(AnimationListener listener) {
        mHeadViewContainer.setVisibility(View.VISIBLE);
        Animation mScaleAnimation = new Animation() {
            @Override
            public void applyTransformation(float interpolatedTime,
                                            Transformation t) {
                setAnimationProgress(interpolatedTime);
            }
        };
        mScaleAnimation.setDuration(mMediumAnimationDuration);
        if (listener != null) {
            mHeadViewContainer.setAnimationListener(listener);
        }
        mHeadViewContainer.clearAnimation();
        mHeadViewContainer.startAnimation(mScaleAnimation);
    }

    private void setAnimationProgress(float progress) {
        if (!usingDefaultHeader) {
            progress = 1;
        }
        ViewCompat.setScaleX(mHeadViewContainer, progress);
        ViewCompat.setScaleY(mHeadViewContainer, progress);
    }

    private void setRefreshing(boolean refreshing, final boolean notify) {
        if (mRefreshing != refreshing) {
            mNotify = notify;
            ensureTarget();
            mRefreshing = refreshing;
            if (mRefreshing) {
                animateOffsetToCorrectPosition(mCurrentTargetOffsetTop,
                        mRefreshListener);
            } else {
                startScaleDownAnimation(mRefreshListener);
            }
        }
    }

    private void startScaleDownAnimation(Animation.AnimationListener listener) {
        Animation mScaleDownAnimation = new Animation() {
            @Override
            public void applyTransformation(float interpolatedTime,
                                            Transformation t) {
                setAnimationProgress(1 - interpolatedTime);
            }
        };
        mScaleDownAnimation.setDuration(SCALE_DOWN_DURATION);
        mHeadViewContainer.setAnimationListener(listener);
        mHeadViewContainer.clearAnimation();
        mHeadViewContainer.startAnimation(mScaleDownAnimation);
    }

    public boolean isRefreshing() {
        return mRefreshing;
    }

    private void ensureTarget() {
        if (mTarget == null) {
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                if (!child.equals(mHeadViewContainer)
                        && !child.equals(mFooterViewContainer)) {
                    mTarget = child;
                    break;
                }
            }
        }
    }

    public void setDistanceToTriggerSync(int distance) {
        mTotalDragDistance = distance;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right,
                            int bottom) {
        final int width = getMeasuredWidth();
        final int height = getMeasuredHeight();
        if (getChildCount() == 0) {
            return;
        }
        if (mTarget == null) {
            ensureTarget();
        }
        if (mTarget == null) {
            return;
        }
        int distance = mCurrentTargetOffsetTop + mHeadViewContainer.getHeight();
        if (!targetScrollWithLayout) {
            distance = 0;
        }
        final View child = mTarget;
        final int childLeft = getPaddingLeft();
        final int childTop = getPaddingTop() + distance - pushDistance;
        final int childWidth = width - getPaddingLeft() - getPaddingRight();
        final int childHeight = height - getPaddingTop() - getPaddingBottom();
        child.layout(childLeft, childTop, childLeft + childWidth, childTop
                + childHeight);
        int headViewWidth = mHeadViewContainer.getMeasuredWidth();
        int headViewHeight = mHeadViewContainer.getMeasuredHeight();
        mHeadViewContainer.layout((width / 2 - headViewWidth / 2),
                mCurrentTargetOffsetTop, (width / 2 + headViewWidth / 2),
                mCurrentTargetOffsetTop + headViewHeight);
        int footViewWidth = mFooterViewContainer.getMeasuredWidth();
        int footViewHeight = mFooterViewContainer.getMeasuredHeight();
        mFooterViewContainer.layout((width / 2 - footViewWidth / 2), height
                - pushDistance, (width / 2 + footViewWidth / 2), height
                + footViewHeight - pushDistance);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mTarget == null) {
            ensureTarget();
        }
        if (mTarget == null) {
            return;
        }
        mTarget.measure(MeasureSpec.makeMeasureSpec(getMeasuredWidth()
                        - getPaddingLeft() - getPaddingRight(), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(getMeasuredHeight()
                                - getPaddingTop() - getPaddingBottom(),
                        MeasureSpec.EXACTLY));
        mHeadViewContainer.measure(MeasureSpec.makeMeasureSpec(
                mHeaderViewWidth, MeasureSpec.EXACTLY), MeasureSpec
                .makeMeasureSpec(3 * mHeaderViewHeight, MeasureSpec.EXACTLY));
        mFooterViewContainer.measure(MeasureSpec.makeMeasureSpec(
                mFooterViewWidth, MeasureSpec.EXACTLY), MeasureSpec
                .makeMeasureSpec(mFooterViewHeight, MeasureSpec.EXACTLY));
        if (!mUsingCustomStart && !mOriginalOffsetCalculated) {
            mOriginalOffsetCalculated = true;
            mCurrentTargetOffsetTop = mOriginalOffsetTop = -mHeadViewContainer
                    .getMeasuredHeight();
            updateListenerCallBack();
        }
        mHeaderViewIndex = -1;
        for (int index = 0; index < getChildCount(); index++) {
            if (getChildAt(index) == mHeadViewContainer) {
                mHeaderViewIndex = index;
                break;
            }
        }
        mFooterViewIndex = -1;
        for (int index = 0; index < getChildCount(); index++) {
            if (getChildAt(index) == mFooterViewContainer) {
                mFooterViewIndex = index;
                break;
            }
        }
    }

    public boolean isChildScrollToTop() {
        if (android.os.Build.VERSION.SDK_INT < 14) {
            if (mTarget instanceof AbsListView) {
                final AbsListView absListView = (AbsListView) mTarget;
                return !(absListView.getChildCount() > 0 && (absListView
                        .getFirstVisiblePosition() > 0 || absListView
                        .getChildAt(0).getTop() < absListView.getPaddingTop()));
            } else {
                return !(mTarget.getScrollY() > 0);
            }
        } else {
            return !ViewCompat.canScrollVertically(mTarget, -1);
        }
    }

    public boolean isChildScrollToBottom() {
        if (isChildScrollToTop()) {
            return false;
        }
        if (mTarget instanceof RecyclerView) {
            RecyclerView recyclerView = (RecyclerView) mTarget;
            LayoutManager layoutManager = recyclerView.getLayoutManager();
            int count = recyclerView.getAdapter().getItemCount();
            if (layoutManager instanceof LinearLayoutManager && count > 0) {
                LinearLayoutManager linearLayoutManager = (LinearLayoutManager) layoutManager;
                if (linearLayoutManager.findLastCompletelyVisibleItemPosition() == count - 1) {
                    return true;
                }
            } else if (layoutManager instanceof StaggeredGridLayoutManager) {
                StaggeredGridLayoutManager staggeredGridLayoutManager = (StaggeredGridLayoutManager) layoutManager;
                int[] lastItems = new int[2];
                staggeredGridLayoutManager
                        .findLastCompletelyVisibleItemPositions(lastItems);
                int lastItem = Math.max(lastItems[0], lastItems[1]);
                if (lastItem == count - 1) {
                    return true;
                }
            }
            return false;
        } else if (mTarget instanceof AbsListView) {
            final AbsListView absListView = (AbsListView) mTarget;
            int count = absListView.getAdapter().getCount();
            int fristPos = absListView.getFirstVisiblePosition();
            if (fristPos == 0
                    && absListView.getChildAt(0).getTop() >= absListView
                    .getPaddingTop()) {
                return false;
            }
            int lastPos = absListView.getLastVisiblePosition();
            return lastPos > 0 && count > 0 && lastPos == count - 1;
        } else if (mTarget instanceof ScrollView) {
            ScrollView scrollView = (ScrollView) mTarget;
            View view = scrollView
                    .getChildAt(scrollView.getChildCount() - 1);
            if (view != null) {
                int diff = (view.getBottom() - (scrollView.getHeight() + scrollView
                        .getScrollY()));
                if (diff == 0) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        ensureTarget();

        final int action = MotionEventCompat.getActionMasked(ev);

        if (mReturningToStart && action == MotionEvent.ACTION_DOWN) {
            mReturningToStart = false;
        }
        if (!isEnabled() || mReturningToStart || mRefreshing || mLoadMore
                || (!isChildScrollToTop() && !isChildScrollToBottom())) {

            return false;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                setTargetOffsetTopAndBottom(
                        mOriginalOffsetTop - mHeadViewContainer.getTop(), true);
                mActivePointerId = ev.getPointerId(0);
                mIsBeingDragged = false;
                final float initialMotionY = getMotionEventY(ev, mActivePointerId);
                if (initialMotionY == -1) {
                    return false;
                }
                mInitialMotionY = initialMotionY;

            case MotionEvent.ACTION_MOVE:
                if (mActivePointerId == INVALID_POINTER) {
                    return false;
                }

                final float y = getMotionEventY(ev, mActivePointerId);
                if (y == -1) {
                    return false;
                }
                float yDiff;
                if (isChildScrollToBottom() && isPushEnable) {
                    yDiff = mInitialMotionY - y;
                    if (yDiff > mTouchSlop && !mIsBeingDragged) {
                        mIsBeingDragged = true;
                    }
                } else if (isPullEnable) {
                    yDiff = y - mInitialMotionY;
                    if (yDiff > mTouchSlop && !mIsBeingDragged) {
                        mIsBeingDragged = true;
                    }
                }
                break;

            case MotionEventCompat.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mIsBeingDragged = false;
                mActivePointerId = INVALID_POINTER;
                break;
        }

        return mIsBeingDragged;
    }

    private float getMotionEventY(MotionEvent ev, int activePointerId) {
        final int index = ev.findPointerIndex(activePointerId);
        if (index < 0) {
            return -1;
        }
        return ev.getY(index);
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean b) {
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        final int action = MotionEventCompat.getActionMasked(ev);

        if (mReturningToStart && action == MotionEvent.ACTION_DOWN) {
            mReturningToStart = false;
        }
        if (!isEnabled() || mReturningToStart
                || (!isChildScrollToTop() && !isChildScrollToBottom())) {
            return false;
        }

        if (isChildScrollToBottom()) {
            return handlerPushTouchEvent(ev, action);
        } else {
            return handlerPullTouchEvent(ev, action);
        }
    }

    private boolean handlerPullTouchEvent(MotionEvent ev, int action) {
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mActivePointerId = ev.getPointerId(0);
                mIsBeingDragged = false;
                break;

            case MotionEvent.ACTION_MOVE: {
                final int pointerIndex = ev.findPointerIndex(mActivePointerId);
                if (pointerIndex < 0) {
                    return false;
                }

                final float y = ev.getY(pointerIndex);
                final float overscrollTop = (y - mInitialMotionY) * DRAG_RATE;
                if (mIsBeingDragged) {
                    float originalDragPercent = overscrollTop / mTotalDragDistance;
                    if (originalDragPercent < 0) {
                        return false;
                    }
                    float dragPercent = Math.min(1f, Math.abs(originalDragPercent));
                    float extraOS = Math.abs(overscrollTop) - mTotalDragDistance;
                    float slingshotDist = mUsingCustomStart ? mSpinnerFinalOffset
                            - mOriginalOffsetTop : mSpinnerFinalOffset;
                    float tensionSlingshotPercent = Math.max(0,
                            Math.min(extraOS, slingshotDist * 2) / slingshotDist);
                    float tensionPercent = (float) ((tensionSlingshotPercent / 4) - Math
                            .pow((tensionSlingshotPercent / 4), 2)) * 2f;
                    float extraMove = (slingshotDist) * tensionPercent * 2;

                    int targetY = mOriginalOffsetTop
                            + (int) ((slingshotDist * dragPercent) + extraMove);
                    if (mHeadViewContainer.getVisibility() != View.VISIBLE) {
                        mHeadViewContainer.setVisibility(View.VISIBLE);
                    }
                    if (!mScale) {
                        ViewCompat.setScaleX(mHeadViewContainer, 1f);
                        ViewCompat.setScaleY(mHeadViewContainer, 1f);
                    }
                    if (usingDefaultHeader) {
                        float alpha = overscrollTop / mTotalDragDistance;
                        if (alpha >= 1.0f) {
                            alpha = 1.0f;
                        }
                        ViewCompat.setScaleX(defaultProgressView, alpha);
                        ViewCompat.setScaleY(defaultProgressView, alpha);
                        ViewCompat.setAlpha(defaultProgressView, alpha);
                    }
                    if (overscrollTop < mTotalDragDistance) {
                        if (mScale) {
                            setAnimationProgress(overscrollTop / mTotalDragDistance);
                        }
                        if (mListener != null) {
                            mListener.onPullEnable(false);
                        }
                    } else {
                        if (mListener != null) {
                            mListener.onPullEnable(true);
                        }
                    }
                    setTargetOffsetTopAndBottom(targetY - mCurrentTargetOffsetTop,
                            true);
                }
                break;
            }
            case MotionEventCompat.ACTION_POINTER_DOWN: {
                final int index = MotionEventCompat.getActionIndex(ev);
                mActivePointerId = ev.getPointerId(index);
                break;
            }

            case MotionEventCompat.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                if (mActivePointerId == INVALID_POINTER) {
//                    if (action == MotionEvent.ACTION_UP) {
//                    }
                    return false;
                }
                final int pointerIndex = ev.findPointerIndex(mActivePointerId);
                final float y = ev.getY(pointerIndex);
                final float overscrollTop = (y - mInitialMotionY) * DRAG_RATE;
                mIsBeingDragged = false;
                if (overscrollTop > mTotalDragDistance) {
                    setRefreshing(true, true);
                } else {
                    mRefreshing = false;
                    Animation.AnimationListener listener = null;
                    if (!mScale) {
                        listener = new Animation.AnimationListener() {

                            @Override
                            public void onAnimationStart(Animation animation) {
                            }

                            @Override
                            public void onAnimationEnd(Animation animation) {
                                if (!mScale) {
                                    startScaleDownAnimation(null);
                                }
                            }

                            @Override
                            public void onAnimationRepeat(Animation animation) {
                            }

                        };
                    }
                    animateOffsetToStartPosition(mCurrentTargetOffsetTop, listener);
                }
                mActivePointerId = INVALID_POINTER;
                return false;
            }
        }

        return true;
    }

    private boolean handlerPushTouchEvent(MotionEvent ev, int action) {
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mActivePointerId = ev.getPointerId(0);
                mIsBeingDragged = false;
                break;
            case MotionEvent.ACTION_MOVE: {
                final int pointerIndex = ev.findPointerIndex(mActivePointerId);
                if (pointerIndex < 0) {
                    return false;
                }
                final float y = ev.getY(pointerIndex);
                final float overscrollBottom = (mInitialMotionY - y) * DRAG_RATE;
                if (mIsBeingDragged) {
                    pushDistance = (int) overscrollBottom;
                    updateFooterViewPosition();
                    if (mOnPushLoadMoreListener != null) {
                        mOnPushLoadMoreListener
                                .onPushEnable(pushDistance >= mFooterViewHeight);
                    }
                }
                break;
            }
            case MotionEventCompat.ACTION_POINTER_DOWN: {
                final int index = MotionEventCompat.getActionIndex(ev);
                mActivePointerId = ev.getPointerId(index);
                break;
            }

            case MotionEventCompat.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                if (mActivePointerId == INVALID_POINTER) {
//                    if (action == MotionEvent.ACTION_UP) {
//                    }
                    return false;
                }
                final int pointerIndex = ev.findPointerIndex(mActivePointerId);
                final float y = ev.getY(pointerIndex);
                final float overscrollBottom = (mInitialMotionY - y) * DRAG_RATE;
                mIsBeingDragged = false;
                mActivePointerId = INVALID_POINTER;
                if (overscrollBottom < mFooterViewHeight
                        || mOnPushLoadMoreListener == null) {
                    pushDistance = 0;
                } else {
                    pushDistance = mFooterViewHeight;
                }
                if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
                    updateFooterViewPosition();
                    if (pushDistance == mFooterViewHeight
                            && mOnPushLoadMoreListener != null) {
                        mLoadMore = true;
                        mOnPushLoadMoreListener.onLoadMore();
                    }
                } else {
                    animatorFooterToBottom((int) overscrollBottom, pushDistance);
                }
                return false;
            }
        }
        return true;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void animatorFooterToBottom(int start, final int end) {
        ValueAnimator valueAnimator = ValueAnimator.ofInt(start, end);
        valueAnimator.setDuration(150);
        valueAnimator.addUpdateListener(new AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                pushDistance = (Integer) valueAnimator.getAnimatedValue();
                updateFooterViewPosition();
            }
        });
        valueAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (end > 0 && mOnPushLoadMoreListener != null) {
                    mLoadMore = true;
                    mOnPushLoadMoreListener.onLoadMore();
                } else {
                    resetTargetLayout();
                    mLoadMore = false;
                }
            }
        });
        valueAnimator.setInterpolator(mDecelerateInterpolator);
        valueAnimator.start();
    }

    public void setLoadMore(boolean loadMore) {
        if (!loadMore && mLoadMore) {
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
                mLoadMore = false;
                pushDistance = 0;
                updateFooterViewPosition();
            } else {
                animatorFooterToBottom(mFooterViewHeight, 0);
            }
        }
    }

    private void animateOffsetToCorrectPosition(int from,
                                                AnimationListener listener) {
        mFrom = from;
        mAnimateToCorrectPosition.reset();
        mAnimateToCorrectPosition.setDuration(ANIMATE_TO_TRIGGER_DURATION);
        mAnimateToCorrectPosition.setInterpolator(mDecelerateInterpolator);
        if (listener != null) {
            mHeadViewContainer.setAnimationListener(listener);
        }
        mHeadViewContainer.clearAnimation();
        mHeadViewContainer.startAnimation(mAnimateToCorrectPosition);
    }

    private void animateOffsetToStartPosition(int from,
                                              AnimationListener listener) {
        if (mScale) {
            startScaleDownReturnToStartAnimation(from, listener);
        } else {
            mFrom = from;
            mAnimateToStartPosition.reset();
            mAnimateToStartPosition.setDuration(ANIMATE_TO_START_DURATION);
            mAnimateToStartPosition.setInterpolator(mDecelerateInterpolator);
            if (listener != null) {
                mHeadViewContainer.setAnimationListener(listener);
            }
            mHeadViewContainer.clearAnimation();
            mHeadViewContainer.startAnimation(mAnimateToStartPosition);
        }
        resetTargetLayoutDelay(ANIMATE_TO_START_DURATION);
    }

    public void resetTargetLayoutDelay(int delay) {
        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                resetTargetLayout();
            }
        }, delay);
    }

    public void resetTargetLayout() {
        final int width = getMeasuredWidth();
        final int height = getMeasuredHeight();
        final View child = mTarget;
        final int childLeft = getPaddingLeft();
        final int childTop = getPaddingTop();
        final int childWidth = child.getWidth() - getPaddingLeft()
                - getPaddingRight();
        final int childHeight = child.getHeight() - getPaddingTop()
                - getPaddingBottom();
        child.layout(childLeft, childTop, childLeft + childWidth, childTop
                + childHeight);

        int headViewWidth = mHeadViewContainer.getMeasuredWidth();
        int headViewHeight = mHeadViewContainer.getMeasuredHeight();
        mHeadViewContainer.layout((width / 2 - headViewWidth / 2),
                -headViewHeight, (width / 2 + headViewWidth / 2), 0);
        int footViewWidth = mFooterViewContainer.getMeasuredWidth();
        int footViewHeight = mFooterViewContainer.getMeasuredHeight();
        mFooterViewContainer.layout((width / 2 - footViewWidth / 2), height,
                (width / 2 + footViewWidth / 2), height + footViewHeight);
    }

    private final Animation mAnimateToCorrectPosition = new Animation() {
        @Override
        public void applyTransformation(float interpolatedTime, Transformation t) {
            int targetTop;
            int endTarget;
            if (!mUsingCustomStart) {
                endTarget = (int) (mSpinnerFinalOffset - Math
                        .abs(mOriginalOffsetTop));
            } else {
                endTarget = (int) mSpinnerFinalOffset;
            }
            targetTop = (mFrom + (int) ((endTarget - mFrom) * interpolatedTime));
            int offset = targetTop - mHeadViewContainer.getTop();
            setTargetOffsetTopAndBottom(offset, false);
        }

        @Override
        public void setAnimationListener(AnimationListener listener) {
            super.setAnimationListener(listener);
        }
    };

    private void moveToStart(float interpolatedTime) {
        int targetTop;
        targetTop = (mFrom + (int) ((mOriginalOffsetTop - mFrom) * interpolatedTime));
        int offset = targetTop - mHeadViewContainer.getTop();
        setTargetOffsetTopAndBottom(offset, false);
    }

    private final Animation mAnimateToStartPosition = new Animation() {
        @Override
        public void applyTransformation(float interpolatedTime, Transformation t) {
            moveToStart(interpolatedTime);
        }
    };

    private void startScaleDownReturnToStartAnimation(int from,
                                                      Animation.AnimationListener listener) {
        mFrom = from;
        mStartingScale = ViewCompat.getScaleX(mHeadViewContainer);
        Animation mScaleDownToStartAnimation = new Animation() {
            @Override
            public void applyTransformation(float interpolatedTime,
                                            Transformation t) {
                float targetScale = (mStartingScale + (-mStartingScale * interpolatedTime));
                setAnimationProgress(targetScale);
                moveToStart(interpolatedTime);
            }
        };
        mScaleDownToStartAnimation.setDuration(SCALE_DOWN_DURATION);
        if (listener != null) {
            mHeadViewContainer.setAnimationListener(listener);
        }
        mHeadViewContainer.clearAnimation();
        mHeadViewContainer.startAnimation(mScaleDownToStartAnimation);
    }

    private void setTargetOffsetTopAndBottom(int offset, boolean requiresUpdate) {
        mHeadViewContainer.bringToFront();
        mHeadViewContainer.offsetTopAndBottom(offset);
        mCurrentTargetOffsetTop = mHeadViewContainer.getTop();
        if (requiresUpdate && android.os.Build.VERSION.SDK_INT < 11) {
            invalidate();
        }
        updateListenerCallBack();
    }

    private void updateFooterViewPosition() {
        mFooterViewContainer.setVisibility(View.VISIBLE);
        mFooterViewContainer.bringToFront();
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.KITKAT) {
            mFooterViewContainer.getParent().requestLayout();
        }
        mFooterViewContainer.offsetTopAndBottom(-pushDistance);
        updatePushDistanceListener();
    }

    private void updatePushDistanceListener() {
        if (mOnPushLoadMoreListener != null) {
            mOnPushLoadMoreListener.onPushDistance(pushDistance);
        }
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = MotionEventCompat.getActionIndex(ev);
        final int pointerId = ev.getPointerId(pointerIndex);
        if (pointerId == mActivePointerId) {
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mActivePointerId = ev.getPointerId(newPointerIndex);
        }
    }

    private class HeadViewContainer extends RelativeLayout {

        private Animation.AnimationListener mListener;

        public HeadViewContainer(Context context) {
            super(context);
        }

        public void setAnimationListener(Animation.AnimationListener listener) {
            mListener = listener;
        }

        @Override
        public void onAnimationStart() {
            super.onAnimationStart();
            if (mListener != null) {
                mListener.onAnimationStart(getAnimation());
            }
        }

        @Override
        public void onAnimationEnd() {
            super.onAnimationEnd();
            if (mListener != null) {
                mListener.onAnimationEnd(getAnimation());
            }
        }
    }

    public boolean isTargetScrollWithLayout() {
        return targetScrollWithLayout;
    }

    public void setTargetScrollWithLayout(boolean targetScrollWithLayout) {
        this.targetScrollWithLayout = targetScrollWithLayout;
    }

    public interface OnPullRefreshListener {
        void onRefresh();

        void onPullDistance(int distance);

        void onPullEnable(boolean enable);
    }

    public interface OnPushLoadMoreListener {
        void onLoadMore();

        void onPushDistance(int distance);

        void onPushEnable(boolean enable);
    }

    public void setDefaultCircleProgressColor(int color) {
        if (usingDefaultHeader) {
            defaultProgressView.setProgressColor(color);
        }
    }

    public void setDefaultCircleBackgroundColor(int color) {
        if (usingDefaultHeader) {
            defaultProgressView.setCircleBackgroundColor(color);
        }
    }

    public void setDefaultCircleShadowColor(int color) {
        if (usingDefaultHeader) {
            defaultProgressView.setShadowColor(color);
        }
    }

    public class CircleProgressView extends View implements Runnable {

        private static final int PEROID = 16;
        private Paint progressPaint;
        private Paint bgPaint;
        private int width;
        private int height;

        private boolean isOnDraw = false;
        private boolean isRunning = false;
        private int startAngle = 0;
        private int speed = 8;
        private RectF ovalRect = null;
        private RectF bgRect = null;
        private int swipeAngle;
        private int progressColor = 0xffcccccc;
        private int circleBackgroundColor = 0xffffffff;
        private int shadowColor = 0xff999999;

        public CircleProgressView(Context context) {
            super(context);
        }

        public CircleProgressView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public CircleProgressView(Context context, AttributeSet attrs,
                                  int defStyleAttr) {
            super(context, attrs, defStyleAttr);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawArc(getBgRect(), 0, 360, false, createBgPaint());
            int index = startAngle / 360;
            if (index % 2 == 0) {
                swipeAngle = (startAngle % 720) / 2;
            } else {
                swipeAngle = 360 - (startAngle % 720) / 2;
            }
            canvas.drawArc(getOvalRect(), startAngle, swipeAngle, false,
                    createPaint());
        }

        private RectF getBgRect() {
            width = getWidth();
            height = getHeight();
            if (bgRect == null) {
                int offset = (int) (density * 2);
                bgRect = new RectF(offset, offset, width - offset, height
                        - offset);
            }
            return bgRect;
        }

        private RectF getOvalRect() {
            width = getWidth();
            height = getHeight();
            if (ovalRect == null) {
                int offset = (int) (density * 8);
                ovalRect = new RectF(offset, offset, width - offset, height
                        - offset);
            }
            return ovalRect;
        }

        public void setProgressColor(int progressColor) {
            this.progressColor = progressColor;
        }

        public void setCircleBackgroundColor(int circleBackgroundColor) {
            this.circleBackgroundColor = circleBackgroundColor;
        }

        public void setShadowColor(int shadowColor) {
            this.shadowColor = shadowColor;
        }

        private Paint createPaint() {
            if (this.progressPaint == null) {
                progressPaint = new Paint();
                progressPaint.setStrokeWidth((int) (density * 3));
                progressPaint.setStyle(Paint.Style.STROKE);
                progressPaint.setAntiAlias(true);
            }
            progressPaint.setColor(progressColor);
            return progressPaint;
        }

        private Paint createBgPaint() {
            if (this.bgPaint == null) {
                bgPaint = new Paint();
                bgPaint.setColor(circleBackgroundColor);
                bgPaint.setStyle(Paint.Style.FILL);
                bgPaint.setAntiAlias(true);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    this.setLayerType(LAYER_TYPE_SOFTWARE, bgPaint);
                }
                bgPaint.setShadowLayer(4.0f, 0.0f, 2.0f, shadowColor);
            }
            return bgPaint;
        }

        public void setPullDistance(int distance) {
            this.startAngle = distance * 2;
            postInvalidate();
        }

        @Override
        public void run() {
            while (isOnDraw) {
                isRunning = true;
                long startTime = System.currentTimeMillis();
                startAngle += speed;
                postInvalidate();
                long time = System.currentTimeMillis() - startTime;
                if (time < PEROID) {
                    try {
                        Thread.sleep(PEROID - time);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        public void setOnDraw(boolean isOnDraw) {
            this.isOnDraw = isOnDraw;
        }

        public void setSpeed(int speed) {
            this.speed = speed;
        }

        public boolean isRunning() {
            return isRunning;
        }

        @Override
        public void onWindowFocusChanged(boolean hasWindowFocus) {
            super.onWindowFocusChanged(hasWindowFocus);
        }

        @Override
        protected void onDetachedFromWindow() {
            isOnDraw = false;
            super.onDetachedFromWindow();
        }

    }

}
