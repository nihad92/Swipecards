package com.lorentzos.flingswipe;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.PointF;
import android.os.Build;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.View;
import android.widget.Adapter;
import android.widget.FrameLayout;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Created by dionysis_lorentzos on 5/8/14
 * for package com.lorentzos.swipecards
 * and project Swipe cards.
 * Use with caution dinosaurs might appear!
 */

public class SwipeFlingAdapterView extends BaseFlingAdapterView {

  private int MINIMUM_VISIBLE_CARD = 3;
  private int MAX_VISIBLE = 4;
  private int MIN_ADAPTER_STACK = 6;
  private float ROTATION_DEGREES = 15.f;
  private int ANIMATION_DURATION = 450;

  private Adapter mAdapter;
  private onFlingListener mFlingListener;
  private AdapterDataSetObserver mDataSetObserver;
  private boolean mInLayout = false;
  private View mActiveCard = null;
  private OnItemClickListener mOnItemClickListener;
  private FlingCardListener flingCardListener;
  private PointF mLastTouchPoint;
  private SparseArray<List<View>> cachedViews = new SparseArray<>();
  private Queue<View> visibleCards = new LinkedList<>();

  public SwipeFlingAdapterView(Context context) {
    this(context, null);
  }

  public SwipeFlingAdapterView(Context context, AttributeSet attrs) {
    this(context, attrs, R.attr.SwipeFlingStyle);
  }

  public SwipeFlingAdapterView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);

    TypedArray a =
        context.obtainStyledAttributes(attrs, R.styleable.SwipeFlingAdapterView, defStyle, 0);
    MAX_VISIBLE = Math.max(a.getInt(R.styleable.SwipeFlingAdapterView_max_visible, MAX_VISIBLE), MINIMUM_VISIBLE_CARD);
    MIN_ADAPTER_STACK =
        a.getInt(R.styleable.SwipeFlingAdapterView_min_adapter_stack, MIN_ADAPTER_STACK);
    ROTATION_DEGREES =
        a.getFloat(R.styleable.SwipeFlingAdapterView_rotation_degrees, ROTATION_DEGREES);
    ANIMATION_DURATION =
        a.getInt(R.styleable.SwipeFlingAdapterView_animation_duration, ANIMATION_DURATION);
    a.recycle();
  }

  /**
   * A shortcut method to set both the listeners and the adapter.
   *
   * @param context The activity context which extends onFlingListener, OnItemClickListener or both
   * @param mAdapter The adapter you have to set.
   */
  public void init(final Context context, Adapter mAdapter) {
    if (context instanceof onFlingListener) {
      mFlingListener = (onFlingListener) context;
    } else {
      throw new RuntimeException(
          "Activity does not implement SwipeFlingAdapterView.onFlingListener");
    }
    if (context instanceof OnItemClickListener) {
      mOnItemClickListener = (OnItemClickListener) context;
    }
    setAdapter(mAdapter);
  }

  @Override public View getSelectedView() {
    return mActiveCard;
  }

  @Override public void requestLayout() {
    if (!mInLayout) {
      if(getAdapter() != null && getAdapter().getCount() != 0 && visibleCards.size() == 0) {
        measure(
            View.MeasureSpec.makeMeasureSpec(getMeasuredWidth(), View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(getMeasuredHeight(), View.MeasureSpec.EXACTLY));
        layout(getLeft(), getTop(), getRight(), getBottom());

      }
      super.requestLayout();
    }
  }

  @Override protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    super.onLayout(changed, left, top, right, bottom);
    refreshView();
  }

  public void refreshView() {
    if (mAdapter == null) {
      return;
    }

    mInLayout = true;
    int adapterCount = mAdapter.getCount();

    if (adapterCount != 0) {
      if (visibleCards.isEmpty() && cachedViews.size() == 0) {
        layoutChildren(0, adapterCount);
      } else if (visibleCards.size() != MAX_VISIBLE && adapterCount >= MAX_VISIBLE) {
        for (int i = visibleCards.size(); i < Math.min(adapterCount, MAX_VISIBLE); i++) {
          createOrRecycleViews(i);
        }
      }
    } else {
      clearAdapterView();
    }

    setTopView();

    //Refresh view
    LinkedList<View> visibleCardsLinkedList = (LinkedList<View>) visibleCards;
    for (int i = 0; i < visibleCards.size(); i++) {
      getAdapter().getView(i, visibleCardsLinkedList.get(i), this);
    }
    mInLayout = false;

    if (adapterCount <= MIN_ADAPTER_STACK) mFlingListener.onAdapterAboutToEmpty(adapterCount);
  }

  private void createOrRecycleViews(int index) {
    View cachedView = getCachedView(index);
    View newUnderChild = mAdapter.getView(index, cachedView, this);

    if (cachedView != newUnderChild)  {
      makeAndAddView(newUnderChild);
    } else {
      newUnderChild.animate().setListener(null);
      newUnderChild.setOnTouchListener(null);
    }

    setTopView();

    visibleCards.add(newUnderChild);
    LinkedList<View> visibleCardsLinkedList = (LinkedList<View>) visibleCards;
    for (int i = visibleCards.size() - 1; i >= 0; i--) {
      visibleCardsLinkedList.get(i).bringToFront();
    }

    newUnderChild.setTranslationY(0);
    newUnderChild.setTranslationX(0);
    newUnderChild.setVisibility(VISIBLE);
  }

  private void clearAdapterView() {
    cachedViews.clear();
    visibleCards.clear();
    removeAllViewsInLayout();
    mActiveCard = null;
  }

  private void layoutChildren(int startingIndex, int adapterCount) {
    while (startingIndex < Math.min(adapterCount, MAX_VISIBLE)) {
      View newUnderChild = mAdapter.getView(startingIndex, getCachedView(startingIndex), this);
      visibleCards.add(newUnderChild);
      newUnderChild.setVisibility(VISIBLE);
      makeAndAddView(newUnderChild);
      startingIndex++;
    }
  }

  @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1) private void makeAndAddView(View child) {

    FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) child.getLayoutParams();
    addViewInLayout(child, 0, lp, true);

    final boolean needToMeasure = child.isLayoutRequested();
    if (needToMeasure) {
      int childWidthSpec = getChildMeasureSpec(getWidthMeasureSpec(),
          getPaddingLeft() + getPaddingRight() + lp.leftMargin + lp.rightMargin, lp.width);
      int childHeightSpec = getChildMeasureSpec(getHeightMeasureSpec(),
          getPaddingTop() + getPaddingBottom() + lp.topMargin + lp.bottomMargin, lp.height);
      child.measure(childWidthSpec, childHeightSpec);
    } else {
      cleanupLayoutState(child);
    }

    int w = child.getMeasuredWidth();
    int h = child.getMeasuredHeight();

    int gravity = lp.gravity;
    if (gravity == -1) {
      gravity = Gravity.TOP | Gravity.START;
    }

    int layoutDirection = getLayoutDirection();
    final int absoluteGravity = Gravity.getAbsoluteGravity(gravity, layoutDirection);
    final int verticalGravity = gravity & Gravity.VERTICAL_GRAVITY_MASK;

    int childLeft;
    int childTop;
    switch (absoluteGravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
      case Gravity.CENTER_HORIZONTAL:
        childLeft = (getWidth() + getPaddingLeft() - getPaddingRight() - w) / 2 + lp.leftMargin
            - lp.rightMargin;
        break;
      case Gravity.END:
        childLeft = getWidth() + getPaddingRight() - w - lp.rightMargin;
        break;
      case Gravity.START:
      default:
        childLeft = getPaddingLeft() + lp.leftMargin;
        break;
    }
    switch (verticalGravity) {
      case Gravity.CENTER_VERTICAL:
        childTop = (getHeight() + getPaddingTop() - getPaddingBottom() - h) / 2 + lp.topMargin
            - lp.bottomMargin;
        break;
      case Gravity.BOTTOM:
        childTop = getHeight() - getPaddingBottom() - h - lp.bottomMargin;
        break;
      case Gravity.TOP:
      default:
        childTop = getPaddingTop() + lp.topMargin;
        break;
    }

    child.layout(childLeft, childTop, childLeft + w, childTop + h);
  }

  /**
   * Set the top view and add the fling listener
   */
  private void setTopView() {
    if (getChildCount() > 0) {

      mActiveCard = getChildAt(indexOfChild(visibleCards.peek()));
      if (mActiveCard != null) {

        flingCardListener =
            new FlingCardListener(mActiveCard, mAdapter.getItem(0), ROTATION_DEGREES,
                ANIMATION_DURATION, new FlingCardListener.FlingListener() {

              @Override public void onCardExited() {
                visibleCards.peek().setVisibility(GONE);
                cacheView(0, visibleCards.peek());
                visibleCards.poll();
                mFlingListener.removeFirstObjectInAdapter();
              }

              @Override public void leftExit(Object dataObject) {
                mFlingListener.onLeftCardExit(dataObject);
              }

              @Override public void rightExit(Object dataObject) {
                mFlingListener.onRightCardExit(dataObject);
              }

              @Override public void onClick(Object dataObject) {
                if (mOnItemClickListener != null) {
                  mOnItemClickListener.onItemClicked(0, dataObject);
                }
              }

              @Override public void onScroll(float scrollProgressPercent) {
                mFlingListener.onScroll(scrollProgressPercent);
              }
            });

        mActiveCard.setOnTouchListener(flingCardListener);
      }
    }
  }

  public void cacheView(int position, View view) {
    int viewType = mAdapter.getItemViewType(position);
    if (cachedViews.get(viewType) != null && cachedViews.get(viewType).size() < MAX_VISIBLE) {
      cachedViews.get(viewType).add(view);
    } else if (cachedViews.get(viewType) == null) {
      cachedViews.put(viewType, new ArrayList<View>());
      cachedViews.get(viewType).add(view);
    }
  }

  public View getCachedView(int position) {
    int viewType = mAdapter.getItemViewType(position);

    if (cachedViews.get(viewType) == null) return null;

    for (View v : cachedViews.get(viewType)) {
      if (v.getVisibility() == GONE) return v;
    }
    return null;
  }

  public FlingCardListener getTopCardListener() throws NullPointerException {
    if (flingCardListener == null) {
      throw new NullPointerException();
    }
    return flingCardListener;
  }

  public void setMaxVisible(int MAX_VISIBLE) {
    this.MAX_VISIBLE = MAX_VISIBLE;
  }

  public void setMinStackInAdapter(int MIN_ADAPTER_STACK) {
    this.MIN_ADAPTER_STACK = MIN_ADAPTER_STACK;
  }

  public void setAnimationDuration(int ANIMATION_DURATION) {
    this.ANIMATION_DURATION = ANIMATION_DURATION;
  }

  @Override public Adapter getAdapter() {
    return mAdapter;
  }

  @Override public void setAdapter(Adapter adapter) {
    if (mAdapter != null && mDataSetObserver != null) {
      mAdapter.unregisterDataSetObserver(mDataSetObserver);
      mDataSetObserver = null;
    }

    mAdapter = adapter;

    if (mAdapter != null && mDataSetObserver == null) {
      mDataSetObserver = new AdapterDataSetObserver();
      mAdapter.registerDataSetObserver(mDataSetObserver);
    }
  }

  public void setFlingListener(onFlingListener onFlingListener) {
    this.mFlingListener = onFlingListener;
  }

  public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
    this.mOnItemClickListener = onItemClickListener;
  }

  @Override public LayoutParams generateLayoutParams(AttributeSet attrs) {
    return new FrameLayout.LayoutParams(getContext(), attrs);
  }

  private class AdapterDataSetObserver extends DataSetObserver {
    @Override public void onChanged() {
      requestLayout();
    }

    @Override public void onInvalidated() {
      requestLayout();
    }
  }

  public interface OnItemClickListener {
    void onItemClicked(int itemPosition, Object dataObject);
  }

  public interface onFlingListener {
    void removeFirstObjectInAdapter();

    void onLeftCardExit(Object dataObject);

    void onRightCardExit(Object dataObject);

    void onAdapterAboutToEmpty(int itemsInAdapter);

    void onScroll(float scrollProgressPercent);
  }
}
