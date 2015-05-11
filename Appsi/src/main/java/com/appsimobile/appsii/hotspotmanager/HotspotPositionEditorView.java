/*
 * Copyright 2015. Appsi Mobile
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.appsimobile.appsii.hotspotmanager;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.appsimobile.appsii.AppsiApplication;
import com.appsimobile.appsii.HotspotItem;
import com.appsimobile.appsii.HotspotItem.ConfigurationListener;
import com.appsimobile.appsii.R;
import com.appsimobile.appsii.compat.MapCompat;

import java.util.Map;

/**
 * A view that allows the user to move a hotspot to a different position.
 * <p/>
 * This is largely based on the old-appsi's implementation and needs documentation
 */
public class HotspotPositionEditorView extends ViewGroup
        implements View.OnTouchListener, ConfigurationListener {

    public static final int VIEWHOLDER_IDX_INDICATOR_LEFT = 0;

    public static final int VIEWHOLDER_IDX_INDICATOR_RIGHT = 1;

    private final Map<HotspotItem, LayoutTag> mLayoutTags = MapCompat.createMap();

    boolean mAllowLongPress = true;

    int mBitmapDragX;

    int mBitmapDragY;

    int mDragX;

    int mDragY;

    int mDragOffsetX;

    int mDragOffsetY;

    boolean mDragLeft;

    Bitmap mDragBitmap;

    boolean mHandlingChange;

    boolean mDragging;

    Rect mDragRect = new Rect();

    OnPositionChangedListener mOnPositionChangedListener;

    private HotspotItem mHotspotItem;

    private View mHotspotView;

    private LayoutInflater mInflater;

    private int mInitialTouchX;

    private int mInitialTouchY;

    public HotspotPositionEditorView(Context context) {
        super(context);
        init();
    }

    public HotspotPositionEditorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public HotspotPositionEditorView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    void init() {
    }

    public void setOnPositionChangedListener(
            OnPositionChangedListener onPositionChangedListener) {
        mOnPositionChangedListener = onPositionChangedListener;
    }

    public void setHotspotItem(HotspotItem configuration) {
        mHotspotItem = configuration;
        updateViews();
    }

    private void updateViews() {
        if (mInflater == null) {
            mInflater = LayoutInflater.from(getContext());
        }
        removeAllViews();
        HotspotItem conf = mHotspotItem;
        mHotspotView = setUpView(conf);
        mLayoutTags.put(conf, new LayoutTag(conf.mLeft, conf.mYPosRelativeToView,
                conf.mHeightRelativeToViewHeight));
    }

    private View setUpView(HotspotItem conf) {
        View v = mInflater.inflate(R.layout.plugin_view, this, false);

        View positionIndicatorLeft = v.findViewById(R.id.position_indicator_left);
        View positionIndicatorRight = v.findViewById(R.id.position_indicator_right);

        View clickHandle = v.findViewById(R.id.plugin_click_region);
        clickHandle.setTag(conf);
        clickHandle.setOnTouchListener(this);

        TextView nameView = (TextView) v.findViewById(R.id.plugin_name);
        String preset = conf.mName;
        nameView.setText(preset);

        if (conf.mLeft) {
            positionIndicatorRight.setVisibility(View.GONE);
        } else {
            positionIndicatorLeft.setVisibility(View.GONE);
        }

        v.setTag(R.id.viewholder,
                new View[]{positionIndicatorLeft, positionIndicatorRight});
        v.setTag(conf);

        HotspotItemLayoutParams param = new HotspotItemLayoutParams(1);
        param.mIsLeft = conf.mLeft;
        addView(v, param);
        conf.setConfigurationListener(this);

        return v;
    }

    @Override
    public void onHotspotConfigurationChanged(HotspotItem hotspotItem,
            float heightRelativeToViewHeight, float old) {
        if (mHandlingChange) return;
        mHandlingChange = true;

        View v = mHotspotView;
        if (v != null) {
            HotspotItemLayoutParams lp =
                    (HotspotItemLayoutParams) v.getLayoutParams();


            int y = lp.mYPosition;
            int parentHeight = getHeight();
            int height = (int) (parentHeight * heightRelativeToViewHeight);

            if (y + height > parentHeight) {
                height = parentHeight - y;
                hotspotItem.setHeightRelativeToViewHeight(old);
            }

            lp.mHeight = height;
            requestLayout();
        }

        mHandlingChange = false;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_DOWN) return false;

        if (mAllowLongPress) {
            mDragging = true;
            startDrag(v);
            return true;
        } else {
            mAllowLongPress = true;
            return false;
        }
    }

    private void startDrag(View v) {
        mDragBitmap = Bitmap.createBitmap(v.getWidth(), v.getHeight(), Config.ARGB_8888);
        Canvas tmp = new Canvas();
        tmp.setBitmap(mDragBitmap);
        v.draw(tmp);

        mDragRect.set(0, 0, v.getWidth(), v.getHeight());
        offsetDescendantRectToMyCoords(v, mDragRect);
        mDragOffsetX = mInitialTouchX - mDragRect.left;
        mDragOffsetY = mInitialTouchY - mDragRect.top;
        mBitmapDragX = mInitialTouchX - mDragOffsetX;
        mBitmapDragY = mInitialTouchY - mDragOffsetY;

        updateLayoutParamsForDrag();
        invalidate();
    }

    private void updateLayoutParamsForDrag() {
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View v = getChildAt(i);
            HotspotItemLayoutParams lp =
                    (HotspotItemLayoutParams) v.getLayoutParams();
            lp.mDragIsLeft = lp.mIsLeft;
            lp.mDragYposition = lp.mYPosition;
            //			lp.mHeight = lp.mD
        }
        requestLayout();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mInitialTouchX = (int) ev.getX();
                mInitialTouchY = (int) ev.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                return mDragging;

        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    protected void dispatchDraw(@NonNull Canvas canvas) {
        super.dispatchDraw(canvas);
        if (mDragging && mDragBitmap != null) {
            canvas.save();
            canvas.translate(mBitmapDragX, mBitmapDragY);
            canvas.translate(0, mDragBitmap.getHeight() / 2);
            canvas.scale(1.2f, 1.2f);
            canvas.translate(0, mDragBitmap.getHeight() / -2);
            canvas.drawBitmap(mDragBitmap, 0, 0, null);
            canvas.restore();
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int count = getChildCount();
        int h = b - t;
        int w = r - l;
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            HotspotItemLayoutParams params =
                    (HotspotItemLayoutParams) child.getLayoutParams();
            if (changed) {
                params.setParentHeight(h);
            }
            if (mDragging) {
                boolean left = params.mDragIsLeft;
                if (left) {
                    child.layout(0, params.mDragYposition, child.getMeasuredWidth(),
                            params.mDragYposition + params.mHeight);
                } else {
                    child.layout(w - child.getMeasuredWidth(), params.mDragYposition, w,
                            params.mDragYposition + params.mHeight);
                }
            } else {
                boolean left = params.mIsLeft;
                if (left) {
                    child.layout(0, params.mYPosition, child.getMeasuredWidth(),
                            params.mYPosition + params.mHeight);
                } else {
                    child.layout(w - child.getMeasuredWidth(), params.mYPosition, w,
                            params.mYPosition + params.mHeight);
                }
            }
        }
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent ev) {
        int action = ev.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_MOVE:
                if (mDragging) {
                    if (mDragBitmap != null) {
                        invalidate(mBitmapDragX, mBitmapDragY,
                                mDragBitmap.getWidth() + mBitmapDragX,
                                mBitmapDragY + mDragBitmap.getHeight());
                    }
                    int x = (int) ev.getX();
                    int y = (int) ev.getY();
                    int deltaX = x - mInitialTouchX;
                    int deltaY = y - mInitialTouchY;

                    mBitmapDragX = mInitialTouchX + deltaX - mDragOffsetX;
                    mBitmapDragY = mInitialTouchY + deltaY - mDragOffsetY;
                    mDragX = deltaX;
                    mDragY = deltaY;

                    mDragLeft = x < getWidth() / 2;
                    HotspotItemLayoutParams lp =
                            (HotspotItemLayoutParams) mHotspotView.getLayoutParams();
                    lp.updateDrag(mBitmapDragX, mBitmapDragY);
                    updateViewLocationsForDrag();
                    if (mDragBitmap != null) {
                        invalidate(mBitmapDragX, mBitmapDragY,
                                mDragBitmap.getWidth() + mBitmapDragX,
                                mBitmapDragY + mDragBitmap.getHeight());
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (mDragging) {
                    HotspotItemLayoutParams lp =
                            (HotspotItemLayoutParams) mHotspotView.getLayoutParams();
                    lp.endDrag();
                    invalidate();
                    mAllowLongPress = true;
                    mDragging = false;
                    mDragBitmap = null;
                    snapDraggedViewToPosition();
                }
                break;
        }
        return !mAllowLongPress;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        Context context = getContext();
        int count = getChildCount();
        int ph = getMeasuredHeight();
        int i10 = (int) (5 * AppsiApplication.getDensity(context));
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            HotspotItemLayoutParams params =
                    (HotspotItemLayoutParams) child.getLayoutParams();
            HotspotItem c = (HotspotItem) child.getTag();
            if (c != null && !params.mIsUpdated) {
                params.mYPosition = (int) (ph * c.mYPosRelativeToView);
                params.mHeight = (int) (ph * c.mHeightRelativeToViewHeight);
                params.mIsUpdated = true;
            }

            int h = params.mHeight;
            int w = getMeasuredWidth() / 2 - i10;
            int wspec = MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY);
            int hspec = MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY);
            child.measure(wspec, hspec);
        }
    }

    private void updateViewLocationsForDrag() {
        View dragged = mHotspotView;
        HotspotItemLayoutParams lp =
                (HotspotItemLayoutParams) dragged.getLayoutParams();
        if (lp.mDragIsLeft != mDragLeft) {
            View[] vh = (View[]) dragged.getTag(R.id.viewholder);
            View left = vh[VIEWHOLDER_IDX_INDICATOR_LEFT];
            View right = vh[VIEWHOLDER_IDX_INDICATOR_RIGHT];
            if (mDragLeft) {
                left.setVisibility(View.VISIBLE);
                right.setVisibility(View.GONE);
            } else {
                left.setVisibility(View.GONE);
                right.setVisibility(View.VISIBLE);
            }
            lp.mDragIsLeft = mDragLeft;
            requestLayout();
            invalidate();
        }
        lp.mDragYposition = lp.mYPosition + mDragY;
        requestLayout();
        invalidate();
    }

    private void snapDraggedViewToPosition() {
        int parentHeight = getHeight();
        View v = mHotspotView;
        HotspotItemLayoutParams lp =
                (HotspotItemLayoutParams) v.getLayoutParams();
        lp.mIsLeft = lp.mDragIsLeft;
        lp.mYPosition = lp.mDragYposition;
        if (lp.mYPosition < 0) {
            lp.mYPosition = 0;
        }
        if (lp.mYPosition + lp.mHeight > parentHeight) {
            lp.mYPosition = parentHeight - lp.mHeight;
        }
        requestLayout();
        notifyPositionChanged(mHotspotItem.mId, lp.mIsLeft, lp.mYPosition);
    }

    private void notifyPositionChanged(long id, boolean isLeft, int yPosition) {
        if (mOnPositionChangedListener != null) {
            float newY = yPosition / (float) getHeight();
            mOnPositionChangedListener.onPositionChanged(id, isLeft, newY);
        }
    }

    static interface OnPositionChangedListener {

        void onPositionChanged(long hotspotId, boolean isLeft, float yPosition);
    }

    static class HotspotItemLayoutParams extends LayoutParams {

        int mYPosition;

        int mHeight;

        int mParentHeight;

        boolean mIsLeft;

        boolean mIsDragView;

        int mDraggedX;

        int mDraggedY;

        int mDragYposition;

        boolean mDragIsLeft;

        boolean mIsUpdated;

        public HotspotItemLayoutParams(int parentHeight) {
            super(0, 0);
            mParentHeight = parentHeight;
        }

        void setParentHeight(int height) {
            mParentHeight = height;
            updateConfiguration();
        }

        private void updateConfiguration() {
        }

        public void updateDrag(int dragX, int dragY) {
            mIsDragView = true;
            mDraggedX = dragX;
            mDraggedY = dragY;
        }

        public void endDrag() {
            mIsDragView = false;
        }
    }

    static class LayoutTag {

        float mHeight;

        float mYpos;

        boolean mLeft;

        public LayoutTag(boolean left, float yposRelativeToView, float heightRelativeToViewHeight) {
            mLeft = left;
            mHeight = heightRelativeToViewHeight;
            mYpos = yposRelativeToView;
        }

    }

}
