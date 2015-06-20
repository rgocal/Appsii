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

package com.appsimobile.appsii.module.apps;

import android.content.ComponentName;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.LongSparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.appsimobile.appsii.ExpandCollapseDrawable;
import com.appsimobile.appsii.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by nick on 09/01/15.
 */
class AppsAdapter extends RecyclerView.Adapter<AppsController.AbstractAppViewHolder> {

    static final int VIEW_TYPE_PARALLAX_HEADER = R.layout.page_apps_px_header;

    static final int VIEW_TYPE_HEADER = R.layout.list_item_header_collapsible_with_options;

    static final int VIEW_TYPE_APP = R.layout.grid_item_app;

    static final int VIEW_TYPE_APP_SINGLE_ROW = R.layout.list_item_app;

    static final int VIEW_TYPE_NO_RECENT_APPS = R.layout.list_item_no_recent_apps;

    static final int VIEW_TYPE_EMPTY_TAG = R.layout.list_item_empty_tag;

    final AppView.AppActionListener mAppActionListener;

    final AppView.TagActionListener mTagActionListener;

    final LongSparseArray<Boolean> mExpandedTags = new LongSparseArray<>();

    /**
     * A list of items that are currently showing in the list/grid
     */
    private final List<Object> mVisibleItems = new ArrayList<>();

    AppPageData mData;

    View.OnClickListener mOnClickListener;

    private View mParallaxView;

    AppsAdapter(AppView.AppActionListener appActionListener,
            AppView.TagActionListener tagActionListener) {
        mAppActionListener = appActionListener;
        mTagActionListener = tagActionListener;
        mVisibleItems.add(null);
        setHasStableIds(true);
    }

    public void setAppPageData(AppPageData data) {
        boolean firstRun = mVisibleItems.size() <= 1;


        // TODO: make it recycle existing loaded icons
        mData = data;
        mVisibleItems.clear();


        // parallax-view
        mVisibleItems.add(null);

        for (AppTag tag : mData.mAppTags) {
            if (firstRun) {
                mExpandedTags.put(tag.id, tag.defaultExpanded ? Boolean.TRUE : Boolean.FALSE);
            }
            boolean expanded = mExpandedTags.get(tag.id, Boolean.FALSE);
            mVisibleItems.add(tag);
            if (expanded) {

                List<AppEntry> appsInTag = getAppsInTag(tag);


                boolean empty = appsInTag == null || appsInTag.isEmpty();

                if (empty && tag.tagType == AppsContract.TagColumns.TAG_TYPE_USER) {
                    mVisibleItems.add(new EmptyTagAppsItem());
                } else if (empty && tag.tagType == AppsContract.TagColumns.TAG_TYPE_RECENT) {
                    mVisibleItems.add(new NoRecentAppsItem());
                } else {
                    mVisibleItems.addAll(appsInTag);
                }
            }
        }
        notifyDataSetChanged();
    }

    @Nullable
    public List<AppEntry> getAppsInTag(AppTag tag) {
        return mData.mAppsPerTag.get(tag.id);
    }

    @Override
    public AppsController.AbstractAppViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case VIEW_TYPE_PARALLAX_HEADER:
                return onCreateParallaxViewHolder(parent);
            case VIEW_TYPE_HEADER:
                return onCreateGroupViewHolder(parent);
            case VIEW_TYPE_NO_RECENT_APPS:
                return onCreateEmptyRecentsViewHolder(parent);
            case VIEW_TYPE_EMPTY_TAG:
                return onEmptyTagViewHolder(parent);
            case VIEW_TYPE_APP:
            case VIEW_TYPE_APP_SINGLE_ROW:
                return onCreateAppViewHolder(parent, viewType);
        }
        return null;
    }

    protected AppsController.AbstractAppViewHolder onCreateParallaxViewHolder(ViewGroup parent) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View result = layoutInflater.inflate(R.layout.page_apps_px_header, parent, false);
        mParallaxView = result;
        return new ParallaxHeaderHolder(result);
    }

    public AppHeaderHolder onCreateGroupViewHolder(ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view =
                inflater.inflate(R.layout.list_item_header_collapsible_with_options, parent, false);

        return new AppHeaderHolder(view);
    }

    public NoRecentAppsHolder onCreateEmptyRecentsViewHolder(ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.list_item_no_recent_apps, parent, false);

        return new NoRecentAppsHolder(view);
    }

    public NoRecentAppsHolder onEmptyTagViewHolder(ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.list_item_empty_tag, parent, false);

        return new NoRecentAppsHolder(view);
    }

    public AppHolder onCreateAppViewHolder(ViewGroup parent, int viewType) {
        View appView = createAppView(parent, viewType);
        return new AppHolder(appView);
    }

    public View createAppView(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        return inflater.inflate(viewType, parent, false);
    }

    @Override
    public void onBindViewHolder(AppsController.AbstractAppViewHolder holder, int position) {
        Object item = mVisibleItems.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) return VIEW_TYPE_PARALLAX_HEADER;
        if (position >= mVisibleItems.size()) return VIEW_TYPE_APP;

        Object item = mVisibleItems.get(position);

        if (item instanceof AppTag) return VIEW_TYPE_HEADER;
        if (item instanceof NoRecentAppsItem) return VIEW_TYPE_NO_RECENT_APPS;
        if (item instanceof EmptyTagAppsItem) return VIEW_TYPE_EMPTY_TAG;
        if (item instanceof AppEntry) {
            AppTag appTagWrapper = tagForPosition(position);
            if (appTagWrapper != null && appTagWrapper.columnCount == 1) {
                return VIEW_TYPE_APP_SINGLE_ROW;
            }

            return VIEW_TYPE_APP;
        }
        throw new IllegalStateException("unknown item type at position: " + position +
                " class: " + item.getClass().getSimpleName());
    }

    @Override
    public long getItemId(int position) {
        Object item = mVisibleItems.get(position);
        if (item == null) return RecyclerView.NO_ID;
        long result = getItemViewType(position);
        result = result << 32;

        if (item instanceof AppTag) return result ^ ((AppTag) item).id;
        if (item instanceof NoRecentAppsItem) return result ^ position;
        if (item instanceof EmptyTagAppsItem) return result ^ position * 2;
        if (item instanceof ResolveInfoAppEntry) {
            AppTag tag = tagForPosition(position);
            if (tag != null) {
                return result ^ ((ResolveInfoAppEntry) item).getComponentName().hashCode() ^ tag.id;
            } else {
                return result ^ ((ResolveInfoAppEntry) item).getComponentName().hashCode();

            }
        }
        return result ^ item.hashCode();
    }

    @Nullable
    AppTag tagForPosition(int position) {
        Object target = mVisibleItems.get(position);
        while (position >= 0) {
            if (target instanceof AppTag) return (AppTag) target;
            --position;
            target = mVisibleItems.get(position);
        }
        return null;
    }

    @Override
    public int getItemCount() {
        return mVisibleItems.size();
    }

    public void setOnClickListener(View.OnClickListener onClickListener) {
        mOnClickListener = onClickListener;
    }

    public void onTrimMemory(int level) {
        if (mData == null || mData.mAllApps == null) return;

        for (AppEntry tag : mData.mAllApps) {
            tag.trimMemory();
        }
    }

    void onCollapse(int position) {
        AppTag appTag = (AppTag) mVisibleItems.get(position);
        List<AppEntry> appsInTag = getAppsInTag(appTag);

        int count = appsInTag == null ? 0 : appsInTag.size();
        if (count == 0) {
            if (appTag.tagType == AppsContract.TagColumns.TAG_TYPE_USER ||
                    appTag.tagType == AppsContract.TagColumns.TAG_TYPE_RECENT) {
                count = 1;
            }
        }

        int firstPos = position + 1;
        for (int i = firstPos + count - 1; i >= firstPos; i--) {
            mVisibleItems.remove(i);
        }

        notifyItemRangeRemoved(firstPos, count);
    }

    void onExpand(int position) {
        AppTag tag = (AppTag) mVisibleItems.get(position);
        List<AppEntry> appsInTag = getAppsInTag(tag);

        int count = appsInTag == null ? 0 : appsInTag.size();
        if (count == 0 && tag.tagType == AppsContract.TagColumns.TAG_TYPE_USER) {
            mVisibleItems.add(position + 1, new EmptyTagAppsItem());
            count = 1;
        } else if (count == 0 && tag.tagType == AppsContract.TagColumns.TAG_TYPE_RECENT) {
            mVisibleItems.add(position + 1, new NoRecentAppsItem());
            count = 1;
        } else {
            mVisibleItems.addAll(position + 1, appsInTag);
        }

        notifyItemRangeInserted(position + 1, count);
    }

    public float getHeaderScrollPercentage() {
        if (mParallaxView == null) return 1f;
        if (!mParallaxView.isShown()) return 1f;
        float top = mParallaxView.getTop();
        if (top > 0) top = 0;
        return top / mParallaxView.getHeight();
    }

    static class NoRecentAppsItem implements AppEntry {

        @Override
        public ApplicationInfo getApplicationInfo() {
            return null;
        }

        @Override
        public ComponentName getComponentName() {
            return null;
        }

        @Override
        public CharSequence getLabel() {
            return null;
        }

        @Override
        public Drawable getIconIfReady() {
            return null;
        }

        @Override
        public Drawable getIcon(PackageManager packageManager) {
            return null;
        }

        @Override
        public void trimMemory() {

        }
    }

    static class EmptyTagAppsItem implements AppEntry {

        @Override
        public ApplicationInfo getApplicationInfo() {
            return null;
        }

        @Override
        public ComponentName getComponentName() {
            return null;
        }

        @Override
        public CharSequence getLabel() {
            return null;
        }

        @Override
        public Drawable getIconIfReady() {
            return null;
        }

        @Override
        public Drawable getIcon(PackageManager packageManager) {
            return null;
        }

        @Override
        public void trimMemory() {

        }
    }

    static class ParallaxHeaderHolder extends AppsController.AbstractAppViewHolder {

        public ParallaxHeaderHolder(View itemView) {
            super(itemView);
        }

        @Override
        void bind(Object object) {

        }
    }

    static class OverflowTouchDelegate extends TouchDelegate {

        final int mMinHeight;

        final private Rect mBounds;

        final private Rect mSlopBounds;

        final View mDelegateView;

        boolean mDelegateTargeted;

        private final int mSlop;

        public OverflowTouchDelegate(View delegateView) {
            super(new Rect(), delegateView);
            mBounds = new Rect();
            mSlopBounds = new Rect();
            mDelegateView = delegateView;
            mMinHeight = (int) (delegateView.getResources().getDisplayMetrics().density * 48);
            mSlop = ViewConfiguration.get(delegateView.getContext()).getScaledTouchSlop();
        }

        /**
         * Will forward touch events to the delegate view if the event is within the bounds
         * specified in the constructor.
         *
         * @param event The touch event to forward
         *
         * @return True if the event was forwarded to the delegate, false otherwise.
         */
        public boolean onTouchEvent(@NonNull MotionEvent event) {
            updateBounds();
            int x = (int) event.getX();
            int y = (int) event.getY();
            boolean sendToDelegate = false;
            boolean hit = true;
            boolean handled = false;

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    Rect bounds = mBounds;

                    if (bounds.contains(x, y)) {
                        mDelegateTargeted = true;
                        sendToDelegate = true;
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_MOVE:
                    sendToDelegate = mDelegateTargeted;
                    if (sendToDelegate) {
                        Rect slopBounds = mSlopBounds;
                        if (!slopBounds.contains(x, y)) {
                            hit = false;
                        }
                    }
                    break;
                case MotionEvent.ACTION_CANCEL:
                    sendToDelegate = mDelegateTargeted;
                    mDelegateTargeted = false;
                    break;
            }
            if (sendToDelegate) {
                final View delegateView = mDelegateView;

                if (hit) {
                    // Offset event coordinates to be inside the target view
                    event.setLocation(delegateView.getWidth() / 2, delegateView.getHeight() / 2);
                } else {
                    // Offset event coordinates to be outside the target view (in case it does
                    // something like tracking pressed state)
                    int slop = mSlop;
                    event.setLocation(-(slop * 2), -(slop * 2));
                }
                handled = delegateView.dispatchTouchEvent(event);
            }
            return handled;
        }

        void updateBounds() {
            ViewParent parent = mDelegateView.getParent();
            mBounds.set(0, 0, mDelegateView.getWidth(), mDelegateView.getHeight());
            if (mBounds.height() < mMinHeight) {
                mBounds.bottom = mBounds.top + mMinHeight;
            }
            mSlopBounds.set(mBounds);
            mSlopBounds.inset(-mSlop, -mSlop);

            parent.getChildVisibleRect(mDelegateView, mBounds, null);
        }

    }

    class NoRecentAppsHolder extends AppsController.AbstractAppViewHolder {

        public NoRecentAppsHolder(View itemView) {
            super(itemView);
        }

        @Override
        void bind(Object object) {

        }
    }

    class AppHeaderHolder extends AppsController.AbstractAppViewHolder
            implements View.OnClickListener, PopupMenu.OnMenuItemClickListener {

        AppTag mAppTag;

        final TextView mTextView;

        final View mHeaderOverflow;

        public AppHeaderHolder(View itemView) {
            super(itemView);

            TypedArray a = itemView.getContext().getTheme().obtainStyledAttributes(new int[]{
                    R.attr.colorPrimary,
                    R.attr.appsiMenuItemTint,
                    R.attr.colorAccent,
            });

            int accentColor = a.getColor(2, Color.DKGRAY);
            a.recycle();


            Resources res = itemView.getResources();
            mTextView = (TextView) itemView.findViewById(R.id.app_header_expander);
            mHeaderOverflow = itemView.findViewById(R.id.header_overflow);
            int dp48 = (int) (res.getDisplayMetrics().density * 24);
            Drawable drawable = new ExpandCollapseDrawable(res, accentColor);
            drawable.setBounds(0, 0, dp48, dp48);

            boolean isLtr = TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()) ==
                    View.LAYOUT_DIRECTION_LTR;

            if (isLtr) {
                mTextView.setCompoundDrawables(null, null, drawable, null);
            } else {
                mTextView.setCompoundDrawables(drawable, null, null, null);
            }

            mTextView.setOnClickListener(this);
            mHeaderOverflow.setOnClickListener(this);

            itemView.setTouchDelegate(new OverflowTouchDelegate(mHeaderOverflow));
        }

        @Override
        public void onClick(View v) {
            int id = v.getId();
            if (id == R.id.app_header_expander) {
                onHeaderTitleClicked();
            } else if (id == R.id.header_overflow) {
                onHeaderOverflowClicked();
            }
        }

        private void onHeaderTitleClicked() {
            // get and toggle
            boolean expanded = !mExpandedTags.get(mAppTag.id, Boolean.FALSE);

            mExpandedTags.put(mAppTag.id, expanded);

            if (expanded) {
                onExpand(getPosition());
            } else {
                onCollapse(getPosition());
            }
            setExpanded(expanded, true);
        }

        private void onHeaderOverflowClicked() {
            boolean editable =
                    mAppTag.tagType == AppsContract.TagColumns.TAG_TYPE_USER;

            boolean showingAsList = mAppTag.columnCount == 1;

            PopupMenu popupMenu = new PopupMenu(mHeaderOverflow.getContext(), mHeaderOverflow);
            MenuInflater menuInflater = popupMenu.getMenuInflater();
            Menu menu = popupMenu.getMenu();
            menuInflater.inflate(R.menu.page_apps_tag, menu);
            menu.findItem(R.id.action_sort_apps).setVisible(editable);
            menu.findItem(R.id.action_toggle_list).setChecked(showingAsList);

            popupMenu.setOnMenuItemClickListener(this);
            popupMenu.show();
        }

        void setExpanded(boolean expanded, boolean animate) {
            boolean isLtr = TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()) ==
                    View.LAYOUT_DIRECTION_LTR;

            Drawable[] drawables = mTextView.getCompoundDrawables();

            if (isLtr) {
                ((ExpandCollapseDrawable) drawables[2]).setExpanded(expanded, animate);
            } else {
                ((ExpandCollapseDrawable) drawables[0]).setExpanded(expanded, animate);
            }
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            int id = item.getItemId();
            switch (id) {
                case R.id.action_sort_apps:
                    mTagActionListener.onReorderApps(mAppTag);
                    return true;
                case R.id.action_toggle_list:
                    mTagActionListener.onToggleSingleRow(mAppTag);
                    return true;
                case R.id.action_rename_tag:
                    mTagActionListener.onEditAppTag(mAppTag);
                    return true;
            }
            return false;
        }

        @Override
        void bind(Object object) {
            mAppTag = (AppTag) object;

            String title = mAppTag.title;
            mTextView.setText(title);

            boolean expanded = mExpandedTags.get(mAppTag.id, Boolean.FALSE);

            setExpanded(expanded, false);
        }


    }

    class AppHolder extends AppsController.AbstractAppViewHolder {

        public AppHolder(View itemView) {
            super(itemView);
        }

        @Override
        void bind(Object object) {
            AppEntry item = (AppEntry) object;
            AppView appView = (AppView) itemView;
            List<TaggedApp> tags;


            if (item != null) {
                appView.setOnClickListener(mOnClickListener);
                ComponentName cn = item.getComponentName();
                tags = mData.mTagsPerComponent.get(cn);
            } else {
                // remove the listener. Even if no child is set, the view
                // may still be added.
                appView.setOnClickListener(null);
                tags = null;
            }
            appView.setAppActionListener(mAppActionListener);

            appView.bind(item, tags);

        }
    }

}
