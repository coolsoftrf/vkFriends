package ru.coolsoft.vkfriends.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import ru.coolsoft.vkfriends.R;

/**
 * Created by Bobby© on 27.11.2016.
 * Helper class that provides initialization methods enabling fast scroll functionality for RecyclerView
 */
public class RecyclerViewFastScrollerLayout extends RelativeLayout {
    private static final int BUBBLE_ANIMATION_DELAY = 1000;
    public static final String TAG = "Scrolling";

    private RecyclerView _recyclerView;
    private View _handlerView;
    private TextView _textView;

    private int _height;

    private ObjectAnimator _currentAnimator;
    private RecyclerView.OnScrollListener _layoutScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            View firstVisibleView = recyclerView.getChildAt(0);
            int firstVisiblePosition = recyclerView.getChildAdapterPosition(firstVisibleView);
            int visibleRange = recyclerView.getChildCount();
            int lastVisiblePosition = firstVisiblePosition + visibleRange;
            int itemCount = recyclerView.getAdapter().getItemCount();
            int position;

            showBubble();

            if (firstVisiblePosition == 0) {
                position = 0;
            } else if (lastVisiblePosition == itemCount - 1) {
                position = itemCount - 1;
            } else {
                position = firstVisiblePosition;
            }

            float fraction = (float) position / (float) itemCount;
            int handleHeight = _handlerView.getHeight();
            setPosition(handleHeight / 2 + ((_height - handleHeight) * fraction));
        }
    };
    private OnTouchListener _handlerTouchListener = new OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            final int action = event.getAction();
            switch(action)
            {
                case MotionEvent.ACTION_DOWN:
                    if(_handlerView.getVisibility() == View.INVISIBLE)
                        showBubble();

                    _handlerView.setSelected(true);
                    return true;

                case MotionEvent.ACTION_MOVE:
                    final int[] location = new int[2];
                    _recyclerView.getLocationOnScreen(location);
                    setRecyclerViewPosition(event.getRawY() - location[1]);
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    _handlerView.setSelected(false);
                    hideBubble();
                    return true;

                default:
                return false;
            }
        }
    };

    public RecyclerViewFastScrollerLayout(Context context) {
        super(context);
    }

    public RecyclerViewFastScrollerLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RecyclerViewFastScrollerLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public RecyclerViewFastScrollerLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //ToDo: add handler behaviour: fade/slide/alwaysShow/plain with RTL support

        //analyze children against layout params indicating recycler, text and handler views
        final int childrenCount = getChildCount();
        for (int i = 0; i < childrenCount; i++) {
            View child = getChildAt(i);
            LayoutParams params = (LayoutParams) child.getLayoutParams();

            if (params.isRecyclerView() && child instanceof RecyclerView){
                _recyclerView = (RecyclerView) child;
            }
            if (params.isHandlerView()){
                _handlerView = child;
                _handlerView.setOnTouchListener(_handlerTouchListener);
            }
            if (params.isHandlerTextView() && child instanceof TextView){
                _textView = (TextView) child;
            }
        }

        //initialize ui interaction listeners
        _recyclerView.addOnScrollListener(_layoutScrollListener);

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }



    public static class LayoutParams extends RelativeLayout.LayoutParams{

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);

            final TypedArray a = c.obtainStyledAttributes(attrs
                    , R.styleable.RecyclerViewFastScrollerLayout_Layout
            );

            mFlags[Attrs.layout_recyclerView.ordinal()] = a.getBoolean(
                    R.styleable.RecyclerViewFastScrollerLayout_Layout_layout_recyclerView, false
            );

            mFlags[Attrs.layout_handlerView.ordinal()] = a.getBoolean(
                    R.styleable.RecyclerViewFastScrollerLayout_Layout_layout_handlerView, false);

            mFlags[Attrs.layout_handlerTextView.ordinal()] = a.getBoolean(
                    R.styleable.RecyclerViewFastScrollerLayout_Layout_layout_handlerTextView, false);

            a.recycle();
        }

        public boolean isRecyclerView(){
            return mFlags[Attrs.layout_recyclerView.ordinal()];
        }
        public boolean isHandlerView(){
            return mFlags[Attrs.layout_handlerView.ordinal()];
        }
        public boolean isHandlerTextView(){
            return mFlags[Attrs.layout_handlerTextView.ordinal()];
        }

        private enum Attrs {
            layout_recyclerView,
            layout_handlerView,
            layout_handlerTextView,

            Count
        }
        private boolean[] mFlags = new boolean[Attrs.Count.ordinal()];

    }

    @Override
    public RelativeLayout.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        _height = h;
    }

    //private methods
    private void setRecyclerViewPosition(float y)
    {
        if(_recyclerView != null)
        {
            int itemCount = _recyclerView.getAdapter().getItemCount();
            int handleHeight = _handlerView.getHeight();
            float fraction = (y - handleHeight / 2) / (float) (_height - handleHeight);
            if(fraction < 0) {
                fraction = 0f;
            } else if(fraction > 1) {
                fraction = 1f;
            }

            int targetPos = getValueInRange(0, itemCount - 1, (int)(fraction * (float)itemCount));
            _recyclerView.scrollToPosition(targetPos);

            if (_textView != null) {
                String bubbleText = String.format(_textView.getContext().getString(R.string.current_position), targetPos, itemCount);
                _textView.setText(bubbleText);
            }
        }
    }

    private int getValueInRange(int min,int max,int value)
    {
        int minimum=Math.max(min,value);
        return Math.min(minimum,max);
    }

    private void setPosition(float y)
    {
        final int handleHeight = _handlerView.getHeight();
        _handlerView.setY(getValueInRange(0, _height - handleHeight, (int)(y - handleHeight / 2)));

        if (_textView != null && _textView != _handlerView) {
            final int bubbleHeight = _textView.getHeight();
            _textView.setY(getValueInRange(0, _height - bubbleHeight - handleHeight / 2, (int) (y - bubbleHeight)));
        }
    }

    private void setAnimator(ObjectAnimator animator){
        synchronized (this) {
            _currentAnimator = animator;
        }
    }
    private void removeAnimator(Animator animator){
        synchronized (this) {
            if (_currentAnimator == animator){
                _currentAnimator = null;
            }
        }
    }

    private void showBubble()
    {
        _handlerView.setVisibility(VISIBLE);
        final float startAlpha;
        if(_currentAnimator != null) {
            if (_currentAnimator.getStartDelay() > 0) {
                //current animator hides the handler
                Log.d(TAG, "showBubble - cancelling hider");
                _currentAnimator.cancel();
                startAlpha = _handlerView.getAlpha();
            } else {
                //let the showing animator continue
                Log.d(TAG, "showBubble - keeping current");
                return;
            }
        } else {
            Log.d(TAG, "showBubble - starting new");
            startAlpha = 0;
        }
        setAnimator(ObjectAnimator.ofFloat(_handlerView, "alpha", startAlpha, 1f));

        _currentAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                Log.d(TAG, "showBubble - ended");
                removeAnimator(_currentAnimator);
                hideBubble();
            }
        });
        _currentAnimator.start();
    }

    private void hideBubble()
    {
        if(_currentAnimator != null) {
            //let current animator continue - whether it is showing or hiding one
            Log.d(TAG, "hideBubble - keep current");
            return;
        }
        setAnimator(ObjectAnimator.ofFloat(_handlerView, "alpha", 1f, 0f));
        _currentAnimator.addListener(new AnimatorListenerAdapter()
        {
            @Override
            public void onAnimationEnd(Animator animation)
            {
                Log.d(TAG, "hideBubble - ended");

                super.onAnimationEnd(animation);
                _handlerView.setVisibility(INVISIBLE);
                removeAnimator(animation);
            }

            @Override
            public void onAnimationCancel(Animator animation)
            {
                Log.d(TAG, "hideBubble - cancelled");

                super.onAnimationCancel(animation);
                //_handlerView.setVisibility(VISIBLE);
                removeAnimator(animation);
            }
        });

        Log.d(TAG, "hideBubble - pend");
        _currentAnimator.setStartDelay(BUBBLE_ANIMATION_DELAY);
        _currentAnimator.start();
    }
}
