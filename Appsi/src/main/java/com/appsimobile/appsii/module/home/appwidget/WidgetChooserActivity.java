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

package com.appsimobile.appsii.module.home.appwidget;

import android.app.Activity;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.appsimobile.appsii.AppsiApplication;
import com.appsimobile.appsii.BuildConfig;
import com.appsimobile.appsii.R;
import com.appsimobile.appsii.appwidget.AppWidgetUtils;
import com.appsimobile.appsii.appwidget.AppsiiAppWidgetHost;
import com.appsimobile.appsii.compat.AppWidgetManagerCompat;
import com.appsimobile.appsii.module.home.config.HomeItemConfiguration;
import com.appsimobile.appsii.module.home.config.HomeItemConfigurationHelper;

import java.text.Collator;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * The activity that lets user select and configure a widget to add to a cell.
 * <p/>
 * Created by nick on 19/02/15.
 */
public class WidgetChooserActivity extends Activity
        implements WidgetViewHolder.OnWidgetClickedListener, View.OnClickListener {

    /**
     * The extra describing the cell id. This is the cell to which
     * the widget will be added.
     */
    public static final String EXTRA_CELL_ID = BuildConfig.APPLICATION_ID + ".cell_id";

    /**
     * The request to bind the app-widget, if the user has not allowed this.
     */
    private static final int REQUEST_BIND_APPWIDGET = 105;

    /**
     * The configuration request. Used to set the widget options.
     */
    private static final int REQUEST_CONFIGURE_APPWIDGET = 106;

    /**
     * The ok-button. This binds the app-widget, and starts the configuration
     * activity if needed. This button is only enabled when a view is selected
     */
    View mOkButton;

    /**
     * Pressing the cancel button will finish the activity
     */
    View mCancelButton;

    /**
     * The cell that is being edited
     */
    long mCellId;

    /**
     * The recycler-view showing the list of app-widgets the user can choose from.
     */
    RecyclerView mRecyclerView;

    /**
     * The decoration that draws the selection and adds spacing to the elements
     */
    SingleSelectionDecoration mSingleSelectionDecoration;

    /**
     * The currently selected info. This is retained in the instance state
     */
    AppWidgetProviderInfo mSelectedAppWidgetProviderInfo;

    /**
     * The appWidgetHost. Used to allocate app-widget-ids
     */
    AppWidgetHost mAppWidgetHost;

    /**
     * The currently allocated app-widget-id for which we are binding or configuring.
     * Retained in instance state.
     */
    int mPendingAddWidgetId;

    /**
     * The widget manager. Used to perform most of the operations on compatible with
     * multiple api levels.
     */
    AppWidgetManagerCompat mAppWidgetManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAppWidgetManager = AppWidgetManagerCompat.getInstance(this);

        mAppWidgetHost = new AppsiiAppWidgetHost(this, AppsiApplication.APPWIDGET_HOST_ID);

        setContentView(R.layout.fragment_widget_chooser);

        mOkButton = findViewById(R.id.ok_button);
        mCancelButton = findViewById(R.id.cancel);
        mRecyclerView = (RecyclerView) findViewById(R.id.widget_recycler);
        mRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));

        mOkButton.setOnClickListener(this);
        mCancelButton.setOnClickListener(this);

        mSingleSelectionDecoration = new SingleSelectionDecoration(this);
        mRecyclerView.addItemDecoration(mSingleSelectionDecoration);

        List<AppWidgetProviderInfo> appWidgetProviders =
                AppWidgetUtils.loadAppWidgetProviderInfos(this);

        Collections.sort(appWidgetProviders, new WidgetNameComparator(this));

        WidgetAdapter adapter = new WidgetAdapter(appWidgetProviders, this);
        mRecyclerView.setAdapter(adapter);

        mCellId = getIntent().getLongExtra(EXTRA_CELL_ID, -1);

        // restore the state and the selection
        if (savedInstanceState != null) {
            mPendingAddWidgetId = savedInstanceState.getInt("pendingAppWidgetId");
            mSelectedAppWidgetProviderInfo = savedInstanceState.getParcelable("providerInfo");
            int position = savedInstanceState.getInt("selection");
            mSingleSelectionDecoration.setSelectedPosition(position);
        }

        // disable the ok-button when nothing is selected. Useful in
        // case we where resumed with an active selection, or started
        // without a selection
        if (mSingleSelectionDecoration.mSelectedPosition == -1) {
            mOkButton.setEnabled(false);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("providerInfo", mSelectedAppWidgetProviderInfo);
        outState.putInt("selection", mSingleSelectionDecoration.mSelectedPosition);
        outState.putInt("pendingAppWidgetId", mPendingAddWidgetId);
    }

    @Override
    public void finish() {
        super.finish();
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode,
            final Intent data) {

        // Handle binding app widgets
        final int pendingAddWidgetId = mPendingAddWidgetId;
        if (requestCode == REQUEST_BIND_APPWIDGET) {
            final int appWidgetId = data != null ?
                    data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) : -1;
            if (resultCode == RESULT_CANCELED) {
                mAppWidgetHost.deleteAppWidgetId(mPendingAddWidgetId);
            } else if (resultCode == RESULT_OK) {
                onAppWidgetSelected(appWidgetId, mSelectedAppWidgetProviderInfo);

            }
            return;
        }


        // handle configuration of app-widgets
        boolean isAppWidgetConfig = requestCode == REQUEST_CONFIGURE_APPWIDGET;
        if (isAppWidgetConfig) {
            int widgetId = data != null ?
                    data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) : -1;


            // get the appWidgetId falling back to the pendingId
            final int appWidgetId;
            if (widgetId < 0) {
                appWidgetId = pendingAddWidgetId;
            } else {
                appWidgetId = widgetId;
            }

            // The appwidget has been configured. or the user cancelled the process.
            // When cancelled delete the app-widget-id and clear the pending id.
            if (appWidgetId < 0 || resultCode == RESULT_CANCELED) {
                Log.e("WidgetChooser", "Error: appWidgetId (EXTRA_APPWIDGET_ID) was not " +
                        "returned from the widget configuration activity.");
                mAppWidgetHost.deleteAppWidgetId(mPendingAddWidgetId);
                mPendingAddWidgetId = -1;
            } else {
                // save the widget id to the cell.
                finishAndSaveWidgetToCell(appWidgetId);
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);

    }

    @Override
    public void onWidgetClicked(AppWidgetProviderInfo info, WidgetViewHolder viewHolder) {
        int position = viewHolder.getPosition();
        if (mSingleSelectionDecoration.toggleSelection(position)) {
            mOkButton.setEnabled(true);
            mSelectedAppWidgetProviderInfo = info;
        } else {
            mOkButton.setEnabled(false);
            mSelectedAppWidgetProviderInfo = null;
        }
    }

    @Override
    public void onClick(View v) {
        int viewId = v.getId();
        if (viewId == R.id.ok_button) {
            // TODO: start config activity


            AppWidgetProviderInfo info = mSelectedAppWidgetProviderInfo;

            // In this case, we either need to start an activity to get permission to bind
            // the widget, or we need to start an activity to configure the widget, or both.
            mPendingAddWidgetId = mAppWidgetHost.allocateAppWidgetId();
            boolean success =
                    mAppWidgetManager.bindAppWidgetIdIfAllowed(mPendingAddWidgetId, info, null);
            if (success) {
                onAppWidgetSelected(mPendingAddWidgetId, info);
            } else {
                Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_BIND);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mPendingAddWidgetId);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider);

                mAppWidgetManager.getUser(mSelectedAppWidgetProviderInfo)
                        .addToIntent(intent, AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE);
                // TODO: we need to make sure that this accounts for the options bundle.
//                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_OPTIONS, (Parcelable) null);
                startActivityForResult(intent, REQUEST_BIND_APPWIDGET);
            }

        } else if (viewId == R.id.cancel) {
            finish();
        }
    }

    /**
     * This is called when the app-widget was successfully bound. This method
     * start the configuration activity if needed. If this is not needed the
     * widget is added to the cell.
     */
    void onAppWidgetSelected(final int appWidgetId, final AppWidgetProviderInfo appWidgetInfo) {

        if (appWidgetInfo.configure != null) {
            mPendingAddWidgetId = appWidgetId;
            // Launch over to configure widget, if needed
            mAppWidgetManager.startConfigActivity(appWidgetInfo, appWidgetId, this,
                    mAppWidgetHost, REQUEST_CONFIGURE_APPWIDGET);
        } else {
            // Otherwise just add it
            finishAndSaveWidgetToCell(appWidgetId);
        }
    }

    /**
     * Add a widget to the cell we are editing.
     */
    private void finishAndSaveWidgetToCell(final int appWidgetId) {

        HomeItemConfiguration itemConfigurationHelper =
                HomeItemConfigurationHelper.getInstance(this);

        itemConfigurationHelper.
                updateProperty(mCellId, "app_widget_id", String.valueOf(appWidgetId));
        finish();
    }

    static class WidgetAdapter extends RecyclerView.Adapter<WidgetViewHolder> {

        final List<AppWidgetProviderInfo> mAppWidgetProviders;

        final WidgetViewHolder.OnWidgetClickedListener mOnWidgetClickedListener;

        WidgetAdapter(List<AppWidgetProviderInfo> appWidgetProviders,
                WidgetViewHolder.OnWidgetClickedListener onWidgetClickedListener) {
            mAppWidgetProviders = appWidgetProviders;
            mOnWidgetClickedListener = onWidgetClickedListener;
        }

        @Override
        public WidgetViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            View view = inflater.inflate(R.layout.grid_item_widget, parent, false);
            return new WidgetViewHolder(view, mOnWidgetClickedListener);
        }

        @Override
        public void onBindViewHolder(WidgetViewHolder holder, int position) {
            AppWidgetProviderInfo info = mAppWidgetProviders.get(position);
            holder.bind(info);
        }

        @Override
        public int getItemCount() {
            return mAppWidgetProviders.size();
        }
    }

    public static class WidgetNameComparator implements Comparator<AppWidgetProviderInfo> {

        private final AppWidgetManagerCompat mManager;

        private final PackageManager mPackageManager;

        private final HashMap<Object, String> mLabelCache;

        private final Collator mCollator;

        WidgetNameComparator(Context context) {
            mManager = AppWidgetManagerCompat.getInstance(context);
            mPackageManager = context.getPackageManager();
            mLabelCache = new HashMap<>();
            mCollator = Collator.getInstance();
        }

        public final int compare(AppWidgetProviderInfo lhs, AppWidgetProviderInfo rhs) {
            String labelA, labelB;
            if (mLabelCache.containsKey(lhs)) {
                labelA = mLabelCache.get(lhs);
            } else {
                labelA = mManager.loadLabel(lhs);
                mLabelCache.put(lhs, labelA);
            }
            if (mLabelCache.containsKey(rhs)) {
                labelB = mLabelCache.get(rhs);
            } else {
                labelB = mManager.loadLabel(rhs);
                mLabelCache.put(rhs, labelB);
            }
            return mCollator.compare(labelA, labelB);
        }
    }

    class SingleSelectionDecoration extends RecyclerView.ItemDecoration {

        final Paint mSelectionOutlinePaint;

        final Rect mRect = new Rect();

        final RectF mRectf = new RectF();

        final float mCornerRadius;

        private final int[] ATTRS = new int[]{
                R.attr.colorAccent,
                R.attr.appsiSidebarBackground,
        };

        int mSelectedPosition = -1;

        SingleSelectionDecoration(Context context) {
            mCornerRadius = context.getResources().getDisplayMetrics().density * 1;
            float strokeWidth = context.getResources().getDisplayMetrics().density * 2;

            final TypedArray a = context.obtainStyledAttributes(ATTRS);
            int accentColor = a.getColor(0, Color.WHITE);
            a.recycle();

            mSelectionOutlinePaint = new Paint();
            mSelectionOutlinePaint.setStyle(Paint.Style.STROKE);
            mSelectionOutlinePaint.setStrokeWidth(strokeWidth);
            mSelectionOutlinePaint.setColor(accentColor);

        }

        public void setSelectedPosition(int selectedPosition) {
            mSelectedPosition = selectedPosition;
            mRecyclerView.invalidate();
        }

        @Override
        public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
            super.onDrawOver(c, parent, state);
            if (mSelectedPosition == -1) return;

            int count = parent.getChildCount();
            for (int i = 0; i < count; i++) {
                View child = parent.getChildAt(i);
                int position = parent.getChildLayoutPosition(child);
                mRect.set(0, 0, child.getWidth(), child.getHeight());
                parent.offsetDescendantRectToMyCoords(child, mRect);
                mRectf.set(mRect);

                if (position == mSelectedPosition) {
                    c.drawRoundRect(mRectf, mCornerRadius, mCornerRadius, mSelectionOutlinePaint);
                }
            }
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                RecyclerView.State state) {
            int offset = (int) (2 * getResources().getDisplayMetrics().density);
            outRect.set(offset, offset, offset, offset);
        }

        boolean toggleSelection(int selection) {
            if (mSelectedPosition == selection) {
                mSelectedPosition = -1;
                return false;
            }
            mSelectedPosition = selection;
            mRecyclerView.invalidate();
            return true;
        }
    }


}
