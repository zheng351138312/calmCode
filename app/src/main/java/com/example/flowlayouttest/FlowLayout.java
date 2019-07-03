package com.example.flowlayouttest;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.Scroller;

import androidx.core.view.ViewConfigurationCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * 自定义view流程
 * 先测量父布局的大小，在根据父布局的规则来确定子布局的大小，将测量的大小重新设置到布局中，并保存关键变量。
 * 根据测量值来确定子布局在父布局中的位置。onLayout中需要的参数可以在测量过程中获取并保存，在onLayout方法中提取使用。
 *
 */
public class FlowLayout extends ViewGroup {
    private static final String TAG = "Zero";
    private final Scroller mScroll;

    private List<View> lineViews;//每一行的子View
    private List<List<View>> views;//所有的行 一行一行的存储
    private List<Integer> heights;//每一行的高度

    private int minScroll;    //能被拦截的滑动最小距离

    private  float mLastInterceptX = 0;
    private float mLastInterceptY = 0;
    private float mLastY = 0;  //滑动的最后距离坐标

    public FlowLayout(Context context) {
        this(context, null);
    }

    public FlowLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FlowLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        ViewConfiguration viewConfiguration = ViewConfiguration.get(context);
        minScroll = ViewConfigurationCompat.getScaledPagingTouchSlop(viewConfiguration);
        mScroll = new Scroller(context);
    }

    private void init(){
        views = new ArrayList<>();
        lineViews = new ArrayList<>();
        heights = new ArrayList<>();
    }

    /**
     * 拦截事件主要流程
     * 1.获取点击屏幕的y坐标，即滑动开始y坐标，获取滑动结束时的y坐标，计算出距离
     * 2.获取点击屏幕的x坐标，即滑动开始x坐标，获取滑动结束时的x坐标，计算出距离
     * 3.判断x滑动的距离和y滑动的距离，如果y的距离大于x则拦截事件，属于向下滚动
     * 4.判断滑动距离是否为限制的最小距离，这里使用系统默认值，是则拦截。
     * @param ev
     * @return
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        boolean intercept = false;
        float mInterceptX = ev.getY();
        float mInterceptY = ev.getX();
        switch (action){
            case MotionEvent.ACTION_DOWN:
                mLastInterceptX = mInterceptX;
                mLastInterceptY = mInterceptY;
                intercept = false;
                break;
            case MotionEvent.ACTION_MOVE:
                float dx = mInterceptX - mLastInterceptX;
                float dy = mInterceptY - mLastInterceptY;
                if (Math.abs(dy) > Math.abs(dx) && dy > minScroll){
                    intercept = true;
                }else {
                    intercept = false;
                }
                break;
            case MotionEvent.ACTION_UP:
                intercept = false;
                break;
        }
        mLastInterceptX = mInterceptX;
        mLastInterceptY = mInterceptY;
        return intercept;
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        float currY = event.getY();

        switch (action){
            case MotionEvent.ACTION_DOWN:
                mLastY = currY;
                break;
            case MotionEvent.ACTION_MOVE:
                float dy = mLastY - currY;
                scrollTo(0,(int)dy);
                Log.v("touch","ACTION_MOVE" + dy);
                mLastY = currY;
                break;
            case MotionEvent.ACTION_UP:
                float y2= event.getY();
                Log.v("touch","ACTION_UP" + y2);
                break;
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec,heightMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        //记录当前行的宽度和高度
        int lineWidth = 0;// 宽度是当前行子view的宽度之和
        int lineHeight = 0;// 高度是当前行所有子View中高度的最大值

        //整个流式布局的宽度和高度
        int flowlayoutWidth = 0;//所有行中宽度的最大值
        int flowlayoutHeight = 0;// 所以行的高度的累加

        //初始化参数列表
        init();

        //遍历所有的子View，对子View进行测量，分配到具体的行
        int childCount = this.getChildCount();
        for(int i = 0; i < childCount; i++){

            View child = this.getChildAt(i);
            //测量子View 获取到当前子View的测量的宽度/高度
            measureChild(child,widthMeasureSpec,heightMeasureSpec);
            //获取到当前子View的测量的宽度/高度
            int childWidth = child.getMeasuredWidth();
            int childHeight = child.getMeasuredHeight();

            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            //看下当前的行的剩余的宽度是否可以容纳下一个子View,
            // 如果放不下，换行 保存当前行的所有子View,累加行高，当前的宽度，高度 置零
            if(lineWidth + childWidth > widthSize){//换行
                views.add(lineViews);
                lineViews = new ArrayList<>();//创建新的一行
                flowlayoutWidth = Math.max(flowlayoutWidth,lineWidth);
                flowlayoutHeight  += lineHeight;
                heights.add(lineHeight);
                lineWidth = 0;
                lineHeight = 0;
            }
            lineViews.add(child);
            lineWidth += childWidth;
            lineHeight = Math.max(lineHeight,childHeight);


            //最后只剩一个view不满足换行条件，不会被当地一行显示出来，最后一行要单独添加
            if (i == childCount - 1){
                views.add(lineViews);
                flowlayoutWidth = Math.max(flowlayoutWidth,lineWidth);
                flowlayoutHeight += lineHeight;
                heights.add(lineHeight);
            }
            LayoutParams lpChild = child.getLayoutParams();
            if (lpChild.height == LayoutParams.MATCH_PARENT){
                lpChild.height = LayoutParams.WRAP_CONTENT;
                child.setLayoutParams(lpChild);
            }
        }

        //FlowLayout最终宽高
        setMeasuredDimension(widthMode == MeasureSpec.EXACTLY? widthSize:flowlayoutWidth
                ,heightMode == MeasureSpec.EXACTLY?heightSize:flowlayoutHeight);

    }


    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

        int lineCount = views.size();

        int currX = 0;
        int currY = 0;

        for(int i = 0; i < lineCount; i++){//大循环，所有的子View 一行一行的布局
            List<View> lineViews = views.get(i);//取出一行
            int lineHeight = heights.get(i);// 取出这一行的高度值
            //遍历当前行的子View
            int size = lineViews.size();
            for(int j = 0 ; j < size ; j++ ){//布局当前行的每一个view
                View child = lineViews.get(j);
                int left = currX;
                int top = currY;
                int right = left + child.getMeasuredWidth();
                int bottom = top + child.getMeasuredHeight();
                child.layout(left,top,right,bottom);
                //确定下一个view的left
                currX += child.getMeasuredWidth();
            }
            currY += lineHeight;
            currX = 0;
        }

    }
}
