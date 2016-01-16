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

package com.appsimobile.appsii.module.calls;

import android.Manifest;
import android.animation.LayoutTransition;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.appsimobile.appsii.AnalyticsManager;
import com.appsimobile.appsii.LoaderManager;
import com.appsimobile.appsii.PageController;
import com.appsimobile.appsii.PermissionDeniedException;
import com.appsimobile.appsii.R;
import com.appsimobile.appsii.dagger.AppsiInjector;
import com.appsimobile.appsii.module.AppsiPreferences;
import com.appsimobile.appsii.module.PermissionHelper;
import com.appsimobile.appsii.module.ToolbarScrollListener;
import com.appsimobile.appsii.permissions.PermissionUtils;
import com.appsimobile.util.TimeUtils;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

/**
 * Created by nick on 25/05/14.
 */
public class CallLogController extends PageController
        implements LoaderManager.LoaderCallbacks<CallLogResult>, View.OnClickListener,
        LayoutTransition.TransitionListener,
        BasePeopleAdapter.OnItemClickListener, PermissionHelper.PermissionListener {

    private static final int CALL_LOG_LOADER_ID = 2001;

    private static final int LAST_CALLED_LOADER_ID = 2002;

    ViewGroup mPermissionOverlay;

    boolean mPendingPermissionError;
    @Inject
    SharedPreferences mSharedPreferences;
    @Inject
    PermissionUtils mPermissionUtils;
    private RecyclerView mCallLogList;
    private CallLogAdapter mCallLogAdapter;
    private Toolbar mToolbar;

    public CallLogController(Context context, String title) {
        super(context, title);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.page_calllog, container, false);
    }

    @Override
    protected void onViewDestroyed(View view) {

    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        int displayMode =
                AppsiPreferences.ModulePreferences.Apps.getDisplayMode(mSharedPreferences);
        boolean displayAsList = displayMode == AppsiPreferences.ModulePreferences.VIEW_TYPE_LIST;

        mCallLogList = (RecyclerView) view.findViewById(R.id.callog_recycler_view);

        mPermissionOverlay = (ViewGroup) view.findViewById(R.id.permission_overlay);

        mToolbar = (Toolbar) view.findViewById(R.id.toolbar);
        mToolbar.setTitle(mTitle);
        setToolbarBackgroundAlpha(0);

        mCallLogList.addOnScrollListener(new ToolbarScrollListener(this, mToolbar));

//        int elevation = getResources().getDimensionPixelOffset(R.dimen.toolbar_elevation);
//        ViewCompat.setElevation(mToolbar, elevation);

        // TODO: define these in integers.xml
        if (displayAsList) {
            mCallLogList.setLayoutManager(new LinearLayoutManager(getContext()));
        } else {
            mCallLogList.setLayoutManager(new LinearLayoutManager(getContext()));
        }
        mCallLogList.setAdapter(mCallLogAdapter);

        if (mPendingPermissionError) {
            showPermissionError();
        }

    }

    @Override
    protected void onUserVisible() {
        super.onUserVisible();
        trackPageView(AnalyticsManager.CATEGORY_CALLS);

    }

    @Override
    protected void onUserInvisible() {
        super.onUserInvisible();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppsiInjector.inject(this);

        mCallLogAdapter = new CallLogAdapter(getContext());
        mCallLogAdapter.setOnItemClickListener(this);
        getLoaderManager().initLoader(CALL_LOG_LOADER_ID, null, this);
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
//        if (mCallLogList != null) {
//            mCallLogList.onTrimMemory(level);
//        }
    }

    @Override
    protected void applyToolbarColor(int color) {
        mToolbar.setBackgroundColor(color);
    }

    private void showPermissionError() {
        mPendingPermissionError = false;
        PermissionHelper permissionHelper = new PermissionHelper(
                R.string.permission_reason_calls,
                false, this, Manifest.permission.READ_CALL_LOG, Manifest.permission.READ_CONTACTS);

        permissionHelper.show(mPermissionOverlay);
    }

    @Override
    public Loader<CallLogResult> onCreateLoader(int id, Bundle args) {
        return new CallLogLoader(getContext());
    }

    @Override
    public void onLoadFinished(Loader<CallLogResult> loader, CallLogResult data) {
        if (data.mPermissionDeniedException != null) {
            onPermissionDenied(data.mPermissionDeniedException);
        } else {
            if (mPermissionOverlay != null) {
                mPermissionOverlay.removeAllViews();
            }
            mCallLogAdapter.setData(data.mCallLog);
        }
    }

    private void onPermissionDenied(PermissionDeniedException permissionDeniedException) {
        if (mPermissionOverlay == null) {
            mPendingPermissionError = true;
        } else {
            showPermissionError();
        }
    }

    @Override
    public void onLoaderReset(Loader<CallLogResult> loader) {
        mCallLogAdapter.clear();
    }

    @Override
    public void onClick(View v) {
        // handle header click
    }

    @Override
    public void startTransition(LayoutTransition transition, ViewGroup container, View view,
            int transitionType) {

        if (transitionType == LayoutTransition.CHANGE_APPEARING) {
            mCallLogList.animate().translationY(0);
        }
    }

    @Override
    public void endTransition(LayoutTransition transition, ViewGroup container, View view,
            int transitionType) {

    }

    @Override
    public void onItemClick(CallLogViewHolder callLogViewHolder) {
        NormalItemViewHolder holder = (NormalItemViewHolder) callLogViewHolder;
        CallLogEntry item = holder.mCallLogEntry;
        String phoneNumber = item.mNumber;

        if (!TextUtils.isEmpty(phoneNumber)) {
            Uri uri = Uri.parse("tel:" + phoneNumber);
            Intent intent = new Intent(Intent.ACTION_DIAL, uri);
            track(AnalyticsManager.ACTION_OPEN_ITEM, AnalyticsManager.CATEGORY_CALLS);

            getContext().startActivity(intent);
        }
    }

    @Override
    public void onAccepted(PermissionHelper permissionHelper) {
        Intent intent = mPermissionUtils.
                buildRequestPermissionsIntent(getContext(),
                        PermissionUtils.REQUEST_CODE_PERMISSION_READ_CALL_LOG,
                        permissionHelper.getPermissions());

        getContext().startActivity(intent);

    }

    @Override
    public void onCancelled(PermissionHelper permissionHelper, boolean dontShowAgain) {
        // this option is not available so this method won't be called

    }

    private static class CallLogAdapter extends BasePeopleAdapter {

        private final List<Object> mCallLogEntries = new ArrayList<>();

        private final Context mContext;

        private View mParallaxView;

        public CallLogAdapter(Context context) {
            super(context);
            mContext = context;
            mCallLogEntries.add(null);
        }

        @Override
        protected CallLogViewHolder onCreateParallaxViewHolder(ViewGroup parent) {
            LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
            View result = layoutInflater.inflate(R.layout.page_calllog_px_header, parent, false);
            mParallaxView = result;
            return new ParallaxHeaderViewHolder(result);
        }

        @Override
        protected CallLogViewHolder onCreateHeaderViewHolder(ViewGroup parent) {
            LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
            View result = layoutInflater.inflate(R.layout.list_item_header, parent, false);
            return new SectionHeaderViewHolder(result);
        }

        @Override
        protected CallLogViewHolder onCreateNormalViewHolder(ViewGroup parent) {

            LayoutInflater inflater = LayoutInflater.from(mContext);
            View view = inflater.inflate(R.layout.list_item_callog_entry, parent, false);
            return new NormalItemViewHolder(view);
        }

        public void setData(List<CallLogEntry> data) {
            mCallLogEntries.clear();
            int count = data.size();
            int lastJulianDay = -1;
            int today = TimeUtils.getJulianDay();
            // parallax header
            mCallLogEntries.add(null);

            for (int i = 0; i < count; i++) {
                CallLogEntry entry = data.get(i);
                int day = Time.getJulianDay(entry.mMillis, 0);
                if (day != lastJulianDay) {
                    lastJulianDay = day;
                    HeaderItem headerItem = new HeaderItem();
                    if (day == today) {
                        headerItem.mTitle = mContext.getString(R.string.today);
                    } else if (day == today - 1) {
                        headerItem.mTitle = mContext.getString(R.string.yesterday);
                    } else {
                        if (day < today - 6) {
                            headerItem.mTitle = DateUtils.formatDateTime(mContext, entry.mMillis,
                                    DateUtils.FORMAT_SHOW_DATE);
                        } else {
                            headerItem.mTitle = DateUtils.formatDateTime(mContext, entry.mMillis,
                                    DateUtils.FORMAT_SHOW_WEEKDAY);
                        }
                    }
                    mCallLogEntries.add(headerItem);
                }
                mCallLogEntries.add(entry);
            }
            notifyDataSetChanged();
        }

        @Override
        public void clear() {
            mCallLogEntries.clear();
            mCallLogEntries.add(null);
            notifyDataSetChanged();
        }

        public float getHeaderScrollPercentage() {
            if (mParallaxView == null) return 1f;
            if (!mParallaxView.isShown()) return 1f;
            float top = mParallaxView.getTop();
            if (top > 0) top = 0;
            return top / mParallaxView.getHeight();
        }

        @Override
        public void onBindViewHolder(CallLogViewHolder callLogViewHolder, int position) {
            callLogViewHolder.bind(mCallLogEntries.get(position));
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0) return VIEW_TYPE_PX_HEADER;
            Object item = mCallLogEntries.get(position);
            if (item instanceof CallLogEntry) return VIEW_TYPE_NORMAL;
            return VIEW_TYPE_HEADER;
        }

        @Override
        public long getItemId(int position) {
            if (position == 0) return Long.MIN_VALUE;

            int type = getItemViewType(position);
            if (type == VIEW_TYPE_NORMAL) {
                return ((CallLogEntry) mCallLogEntries.get(position)).mId;
            }
            HeaderItem headerItem = (HeaderItem) mCallLogEntries.get(position);
            return position * headerItem.mTitle.hashCode();
        }

        @Override
        public int getItemCount() {
            return mCallLogEntries.size();
        }


    }

    static class HeaderItem {

        String mTitle;
    }

    public static class ParallaxHeaderViewHolder extends CallLogViewHolder {

        public ParallaxHeaderViewHolder(View itemView) {
            super(itemView);
        }

        @Override
        public void bind(Object object) {

        }
    }

    public static class NormalItemViewHolder extends CallLogViewHolder
            implements View.OnClickListener {

        CallLogEntry mCallLogEntry;

        public NormalItemViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
        }

        public void bind(Object item) {
            mCallLogEntry = (CallLogEntry) item;
            ((CallLogView) itemView).bind(mCallLogEntry);
        }

        @Override
        public void onClick(View v) {
            if (mOnClickListener != null) {
                mOnClickListener.onItemClick(this);
            }
        }
    }

    public static class SectionHeaderViewHolder extends CallLogViewHolder {

        public SectionHeaderViewHolder(View itemView) {
            super(itemView);
        }

        @Override
        public void bind(Object object) {
            HeaderItem item = (HeaderItem) object;
            ((TextView) itemView).setText(item.mTitle);
        }
    }


}