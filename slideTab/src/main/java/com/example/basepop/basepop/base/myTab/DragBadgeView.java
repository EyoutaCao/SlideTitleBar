package com.example.basepop.basepop.base.myTab;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.FloatEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.AnimationDrawable;
import android.os.Handler;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;

import com.example.basepop.R;
import com.example.basepop.basepop.base.utils.PxTool;


/**
 * zbj on 27-07-17 17:44.
 */

public class DragBadgeView extends View {
    private static final String VIEW_TAG = "BadgeView_TAG";

    private String  mText;
    private String  mDrawText;
    private int     mMaxShowValue;
    private float   mMaxMoveRange;
    private float   mTextWidth;
    private float   mTextHeight;
    private float   mFontMetricsTop;
    private float   mFontMetricsBottom;
    private boolean mDragEnable;
    private boolean isDragging;
    private int[] mRootViewLocation = new int[2];

    private Paint     mPaint;
    private TextPaint mTextPaint;
    private RectF     mTextRectF;
    private BadgeView mBadgeView;

    private OnDragBadgeViewListener mListener;

    public interface OnDragBadgeViewListener {
        void onDisappear(String text);
    }

    public void setOnDragBadgeViewListener(OnDragBadgeViewListener listener) {
        mListener = listener;
    }

    public DragBadgeView(Context context) {
        this(context, null);
    }

    public DragBadgeView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DragBadgeView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs);
    }

    //?????????
    private void init(AttributeSet attrs) {
        TypedArray array = getContext().obtainStyledAttributes(attrs, R.styleable.DragBadgeView);
        mText = mDrawText = array.getString(R.styleable.DragBadgeView_text);
        float textSize = array.getDimension(R.styleable.DragBadgeView_textSize, sp2px(10));
        int bgColor = array.getColor(R.styleable.DragBadgeView_bgColor, Color.RED);
        int textColor = array.getColor(R.styleable.DragBadgeView_textColor, Color.WHITE);
        mMaxMoveRange = array.getDimension(R.styleable.DragBadgeView_maxMoveRange, dp2px(40));
        mDragEnable = array.getBoolean(R.styleable.DragBadgeView_dragEnable, true);
        mMaxShowValue = array.getInt(R.styleable.DragBadgeView_maxShowValue, 99);
        array.recycle();

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(bgColor);

        mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextPaint.setTextSize(textSize);
        mTextPaint.setColor(textColor);

        if (mText == null) {
            mText = mDrawText = "-1";
        }

        measureText();

        //??????????????????????????????RectF
        mTextRectF = new RectF();
    }

    /**
     * ?????????????????????
     *
     */
    private void measureText() {
        if (TextUtils.isDigitsOnly(mText)) {
            if (Integer.valueOf(mText) > mMaxShowValue) {
                mDrawText = mMaxShowValue + "+";
            }
        }
        mTextWidth = mTextPaint.measureText(mDrawText) + getPaddingLeft() + getPaddingRight();
        if (mTextWidth<=dp2px(16)){
            mTextWidth=dp2px(16);
        };
        Paint.FontMetrics fontMetrics = mTextPaint.getFontMetrics();
        mFontMetricsTop = fontMetrics.top;
        mFontMetricsBottom = fontMetrics.bottom;
        mTextHeight = Math.abs(mFontMetricsTop - mFontMetricsBottom) + getPaddingTop() +
                getPaddingBottom();
    }

    /**
     * ???????????????????????????
     *
     * @param text ?????????????????????
     */
    public void setText(CharSequence text) {
        if (text.equals("0")||text.equals("-1")){
            setVisibility(GONE);
            return;
        }else {
            setVisibility(VISIBLE);
        }
        mText = mDrawText = text.toString();

        measureText();
        //??????????????????,????????????onMeasure
        requestLayout();
        postInvalidate();

        if (isDragging && mBadgeView != null) {
            updateCacheBitmap();
            mBadgeView.postInvalidate();
        }
    }

    /**
     * ?????????????????????
     * @return String????????????
     */
    public String getStringText() {
        return mText;
    }

    /**
     * ?????????????????????????????????
     * @return int????????????
     */
    public int getIntText() {
        if (TextUtils.isDigitsOnly(mText))
            return Integer.valueOf(mText);
        else
            return -1;
    }

    /**
     * ????????????????????????
     *
     * @param color ?????????
     */
    public void setBgColor(int color) {
        mPaint.setColor(color);
        postInvalidate();
    }

    /**
     * ??????????????????
     *
     * @param textSize ?????????????????? ?????? 0 ,??????sp2px??????
     */
    public void setTextSize(float textSize) {
        if (textSize > 0) {
            mTextPaint.setTextSize(textSize);
            measureText();
            requestLayout();
            postInvalidate();
        }
    }

    /**
     * ??????????????????
     *
     * @param enable true:????????? false:??????
     */
    public void setDragEnable(boolean enable) {
        mDragEnable = enable;
    }

    /**
     * ??????TextView?????????bitmap
     */
    private void updateCacheBitmap() {
        mBadgeView.recycleCacheBitmap();
        setDrawingCacheEnabled(true);
        Bitmap drawingCache = getDrawingCache();
        mBadgeView.cacheBitmap = Bitmap.createBitmap(drawingCache);
        setDrawingCacheEnabled(false);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = measureDimension((int) Math.max(mTextWidth, mTextHeight), widthMeasureSpec);
        int height = measureDimension((int) mTextHeight, heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    private int measureDimension(int defaultSize, int measureSpec) {
        int result;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);
        if (specMode == MeasureSpec.EXACTLY) {//????????????????????????match_parent???????????????????????????
            result = specSize;
        } else if (specMode == MeasureSpec.AT_MOST) {//????????????????????????wrap_content
            result = Math.min(defaultSize, specSize);
        } else {
            result = defaultSize;
        }
        return result;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float tempWidth = getWidth();
        if (getWidth() < getHeight()) {
            tempWidth = getHeight();
        }
        mTextRectF.set(0, 0, tempWidth, getHeight());

        canvas.drawRoundRect(mTextRectF, getHeight() / 2, getHeight() / 2, mPaint);

        //??????drawText
        int centerY = (int) (mTextRectF.centerY() - mFontMetricsTop / 2 - mFontMetricsBottom / 2);

        canvas.drawText(mDrawText, mTextRectF.centerX(), centerY, mTextPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (!mDragEnable) {//???????????????????????????
                    return false;
                }
                View root = getRootView();
                //??????DecorView????????????,?????????ViewGroup
                if (root == null || !(root instanceof ViewGroup)) {
                    return false;
                }
                ViewGroup vg = (ViewGroup) root;
                //????????????Tag???BadgeView,ListView/RecyclerView?????????????????????Item??????Action_Down??????
                View badgeView = vg.findViewWithTag(VIEW_TAG);
                if (badgeView != null) {
                    return false;
                }
                root.getLocationOnScreen(mRootViewLocation);
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);//????????????????????????DOWN??????
                }
                int location[] = new int[2];
                getLocationOnScreen(location);

                int downX = location[0] + (getWidth() / 2) - mRootViewLocation[0];
                int downY = location[1] + (getHeight() / 2) - mRootViewLocation[1];
                int radius = (getHeight()) / 2;

                mBadgeView = new BadgeView(getContext());
//                mBadgeView.setLayoutParams(new ViewGroup.LayoutParams(root.getWidth(),
//                        root.getHeight()));
                if (mBadgeView.isResetAnimatorRunning()) {
                    return false;
                }
                updateCacheBitmap();
                mBadgeView.initPoints(downX, downY, event.getRawX() - mRootViewLocation[0],
                        event.getRawY() - mRootViewLocation[1], radius);
                mBadgeView.setTag(VIEW_TAG);//???BadgeView??????Tag
                View cacheView = vg.findViewWithTag(VIEW_TAG);//??????????????????BadgeView,??????
                if (cacheView != null) {
                    vg.removeView(cacheView);
                }
                vg.addView(mBadgeView);

                setVisibility(View.INVISIBLE);//????????????View??????
                isDragging = true;
                break;
            case MotionEvent.ACTION_MOVE:
                mBadgeView.updateView(event.getRawX() - mRootViewLocation[0],
                        event.getRawY() - mRootViewLocation[1]);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_DOWN://?????????????????????
            case MotionEvent.ACTION_CANCEL:
                //BadgeView??????????????????????????????????????????ListView/RecyclerView???????????????
                isDragging = false;
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                }
                if (mBadgeView == null) {
                    return true;
                }
                if (mBadgeView.isOutOfRange) {
                    mBadgeView.disappear(event.getRawX() - mRootViewLocation[0],
                            event.getRawY() - mRootViewLocation[1]);
                } else if (!mBadgeView.isResetAnimatorRunning()) {
                    mBadgeView.reset();
                }
                break;
            default:
                break;
        }
        return true;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        setBackgroundColor(Color.TRANSPARENT);
    }


    public void setTextColor(int color){
        mTextPaint.setColor(color);
        postInvalidate();
    }


    /**
     * ??????????????????View
     */
    private class BadgeView extends View {
        private Bitmap cacheBitmap;
        private PointF mOriginPoint;
        private PointF mDragPoint;
        private PointF mControlPoint;

        private float   mDragRadius;
        private float   mOriginRadius;
        private boolean isOutOfRange;//??????????????????????????????????????????
        private boolean isBezierBreak;//?????????????????????????????????

        private Path mPath;

        private ValueAnimator mAnimator;

        public BadgeView(Context context) {
            super(context);
            init();
        }

        private void init() {
            mPath = new Path();
        }

        public void initPoints(float originX, float originY, float dragX, float dragY, float r) {
            mOriginPoint = new PointF(originX, originY);
            mDragPoint = new PointF(dragX, dragY);
            mControlPoint = new PointF((originX + dragX) / 2.0f, (originY + dragY) / 2.0f);
            mOriginRadius = r;
            mDragRadius = r;
            isOutOfRange = false;
            isBezierBreak = false;
        }

        @Override
        protected void onDraw(Canvas canvas) {

            if (!isOutOfRange && !isBezierBreak) {
                mPath.reset();

                float dx = mDragPoint.x - mOriginPoint.x;
                float dy = mDragPoint.y - mOriginPoint.y;

                //?????????????????????
                float oDx = mOriginRadius;
                float oDy = 0;
                float dDx = mDragRadius;
                float dDy = 0;

                if (dx != 0) {
                    double a = Math.atan(dy / dx);//pickerview:??????
                    oDx = (float) (Math.sin(a) * mOriginRadius);
                    oDy = (float) (Math.cos(a) * mOriginRadius);
                    dDx = (float) (Math.sin(a) * mDragRadius);
                    dDy = (float) (Math.cos(a) * mDragRadius);
                }

                //????????????????????????
                mControlPoint.set((mOriginPoint.x + mDragPoint.x) / 2.0f,
                        (mOriginPoint.y + mDragPoint.y) / 2.0f);
                //???????????????????????????????????????????????????
                mPath.moveTo(mOriginPoint.x + oDx, mOriginPoint.y - oDy);

                mPath.quadTo(mControlPoint.x, mControlPoint.y,
                        mDragPoint.x + dDx, mDragPoint.y - dDy);

                //???????????????????????????????????????????????????
                mPath.lineTo(mDragPoint.x - dDx, mDragPoint.y + dDy);

                mPath.quadTo(mControlPoint.x, mControlPoint.y, mOriginPoint.x - oDx,
                        mOriginPoint.y + oDy);
                mPath.close();
                canvas.drawPath(mPath, mPaint);

                //????????????
                canvas.drawCircle(mOriginPoint.x, mOriginPoint.y, mOriginRadius, mPaint);

            } else {
                isBezierBreak = true;
            }

            //???????????????
            canvas.drawBitmap(cacheBitmap, mDragPoint.x - cacheBitmap.getWidth() / 2,
                    mDragPoint.y - cacheBitmap.getHeight() / 2, mPaint);
        }

        public void updateDragPoint(float x, float y) {
            mDragPoint.set(x, y);
            BadgeView.this.postInvalidate();
        }

        public void updateView(float x, float y) {
            float distance = (float) Math.sqrt(Math.pow(mOriginPoint.y - mDragPoint.y, 2) +
                    Math.pow(mOriginPoint.x - mDragPoint.x, 2));

            isOutOfRange = distance > mMaxMoveRange;//???????????????????????????????????????
            //?????????,????????????
            mOriginRadius = mDragRadius - distance / 10;
            //????????????5dp
            if (mOriginRadius < dp2px(5)) {
                mOriginRadius = dp2px(5);
            }

            updateDragPoint(x, y);
        }

        //??????
        public void reset() {
            final PointF tempDragPoint = new PointF(mDragPoint.x, mDragPoint.y);
            if (tempDragPoint.x == mOriginPoint.x && tempDragPoint.y == mOriginPoint.y) {
                return;
            }
            final FloatEvaluator evaluator = new FloatEvaluator();
            mAnimator = ValueAnimator.ofFloat(1.0f);
            mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

                @Override
                public void onAnimationUpdate(ValueAnimator mAnim) {
                    float percent = mAnim.getAnimatedFraction();
                    updateDragPoint(evaluator.evaluate(percent, tempDragPoint.x, mOriginPoint.x),
                            evaluator.evaluate(percent, tempDragPoint.y, mOriginPoint.y));
                }
            });
            mAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    clearAnimation();//?????????????????????
                    ViewGroup rootView = (ViewGroup) BadgeView.this.getParent();
                    if (rootView != null) {
                        rootView.removeView(BadgeView.this);
                        DragBadgeView.this.setVisibility(VISIBLE);
                    }
                    recycleCacheBitmap();
                }
            });
            mAnimator.setInterpolator(new OvershootInterpolator());
            mAnimator.setDuration(400);
            mAnimator.start();
        }

        //??????
        public void disappear(float x, final float y) {
            ViewGroup rootView = (ViewGroup) BadgeView.this.getParent();
            if (rootView != null) {
                rootView.removeView(BadgeView.this);//DecorView?????????BadgeView
                //????????????????????????
                addExplodeImageView(x, y, rootView);
            }
            recycleCacheBitmap();


        }

        /**
         * ??????CacheBitmap
         */
        private void recycleCacheBitmap() {
            if (cacheBitmap != null && !cacheBitmap.isRecycled()) {
                cacheBitmap.recycle();
                cacheBitmap = null;
            }
        }

        /**
         * ????????????????????????????????????
         *
         * @return true:???????????? false:??????
         */
        public boolean isResetAnimatorRunning() {
            return mAnimator != null && mAnimator.isRunning();
        }

        /**
         * ??????????????????
         *
         * @param x        BadgeView?????????x??????
         * @param y        BadgeView?????????y??????
         * @param rootView DecorView
         */
        private void addExplodeImageView(final float x, final float y, final ViewGroup rootView) {
            final int totalDuration = 500;//???????????????
            int d = totalDuration / 5;//????????????

            final ImageView explodeImage = new ImageView(getContext());
            final AnimationDrawable explodeAnimation = new AnimationDrawable();//???????????????
            //?????????,???????????????drawable-nodpi???
            explodeAnimation.addFrame(ContextCompat.getDrawable(getContext(), R.drawable.pop1), d);
            explodeAnimation.addFrame(ContextCompat.getDrawable(getContext(), R.drawable.pop2), d);
            explodeAnimation.addFrame(ContextCompat.getDrawable(getContext(), R.drawable.pop3), d);
            explodeAnimation.addFrame(ContextCompat.getDrawable(getContext(), R.drawable.pop4), d);
            explodeAnimation.addFrame(ContextCompat.getDrawable(getContext(), R.drawable.pop5), d);
            //???????????????????????????
            explodeAnimation.setOneShot(true);

            explodeImage.setImageDrawable(explodeAnimation);
            explodeImage.setVisibility(INVISIBLE);

            ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup
                    .LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

            rootView.addView(explodeImage, params);

            explodeImage.post(new Runnable() {
                @Override
                public void run() {
                    explodeImage.setX(x - explodeImage.getWidth() / 2);
                    explodeImage.setY(y - explodeImage.getHeight() / 2);
                    explodeImage.setVisibility(VISIBLE);

                    explodeAnimation.start();

                    Handler handler = explodeImage.getHandler();
                    if (handler != null) {
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                explodeImage.setVisibility(GONE);
                                //???????????????DecorView?????????ImageView??????
                                if (mListener != null) {
                                    mListener.onDisappear(mText);
                                }
                                rootView.removeView(explodeImage);
                                DragBadgeView.this.setVisibility(GONE);
                            }
                        }, totalDuration);
                    }
                }
            });
        }
    }

/*    @Override
    public void setLayoutParams(ViewGroup.LayoutParams params) {
        if (params.width<=dp2px(16)){

            params.width= (int) dp2px(16);
        }else {
            params.width= ViewGroup.LayoutParams.WRAP_CONTENT;
        }
        super.setLayoutParams(params);
    }*/

    public float dp2px(float dp) {
        return PxTool.dpToPx(getContext(),dp);
    }

    public float sp2px(float sp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, Resources.getSystem()
                .getDisplayMetrics());
    }

}
