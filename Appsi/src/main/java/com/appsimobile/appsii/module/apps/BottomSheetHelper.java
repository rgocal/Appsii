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

import android.animation.Animator;
import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LayoutAnimationController;
import android.view.animation.TranslateAnimation;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.appsimobile.appsii.AnimatorAdapter;
import com.appsimobile.appsii.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nick on 09/01/15.
 */
class BottomSheetHelper {

    final Context mContext;

    View mOverlay;

    View mBottomSheet;

    View mContentView;

    ImageView mAppIconView;

    TextView mAppTitleView;

    ImageView mCloseButton;

    View mTitleDivider;

    RecyclerView mTagsRecycler;

    View mAddTagView;

    AppEntry mAppEntry;

    final AppsController mAppsController;

    final AppTagAdapter mAdapter;

    final float mCellHeight;

    int mMaxTranslationY;

    int mMinTranslationY;

    View mBottomSheetFooter;

    private final Animator.AnimatorListener mOnClosedListener = new AnimatorAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
            mOverlay.setVisibility(View.GONE);
            mBottomSheet.setVisibility(View.INVISIBLE);
        }
    };

    BottomSheetHelper(Context context, AppsController appsController) {
        mContext = context;
        mAppsController = appsController;
        mAdapter = new AppTagAdapter();

        TypedArray t = context.obtainStyledAttributes(
                new int[]{android.R.attr.listPreferredItemHeightSmall}
        );
        mCellHeight = t.getDimension(0, 0);
        t.recycle();

    }

    public void onViewCreated(View parent) {
        mBottomSheetFooter = parent.findViewById(R.id.bottom_sheet_footer);
        mOverlay = parent.findViewById(R.id.bottom_sheet_overlay);
        mContentView = parent.findViewById(R.id.bottom_sheet_content);
        mBottomSheet = parent.findViewById(R.id.bottom_sheet);
        mAppIconView = (ImageView) parent.findViewById(R.id.sheet_app_icon);
        mAppTitleView = (TextView) parent.findViewById(R.id.sheet_app_title);
        mCloseButton = (ImageView) parent.findViewById(R.id.close_sheet);
        mTitleDivider = parent.findViewById(R.id.title_divider);

        mTagsRecycler = (RecyclerView) parent.findViewById(R.id.tag_recycler_view);
        mTagsRecycler.setLayoutManager(new LinearLayoutManager(parent.getContext()));
        mTagsRecycler.setAdapter(mAdapter);
        mTagsRecycler.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {

            /**
             * The original touched y position
             */
            float mStartY;

            /**
             * The translation when we first touched the view. Used to calculate the
             * translation to apply
             */
            float mStartTy;

            /**
             * The touchslop
             */
            float mTouchSlop;

            /**
             * True if we decided to capture the event. Used to know if up/cancel
             * events must be handled.
             */
            boolean mCaptured;

            /**
             * An event is marked as ignored if the touch-slop was passed we we decided,
             * not to handle the touch in favor of a scroll
             */
            boolean mIgnoring;

            /**
             * True when we are closing because of the gesture.
             */
            boolean mClosing;

            {
                mTouchSlop = ViewConfiguration.get(mContext).getScaledTouchSlop();
            }

            @Override
            public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
                int action = e.getAction();

                float rawY = e.getRawY();

                // Reset the state on a down event
                if (action == MotionEvent.ACTION_DOWN) {
                    mCaptured = false;
                    mClosing = false;
                    mIgnoring = false;
                    mStartY = rawY;
                    mStartTy = mContentView.getTranslationY();
                }

                // calculate the delta, and when this is a move bail out when
                // the touch-slop was not reached
                float delta = Math.abs(mStartY - rawY);
                if (action == MotionEvent.ACTION_MOVE && delta <= mTouchSlop) {
                    return false;
                }

                // immediately ignore the event when it is marked as an event
                // that should not be handled
                if (action == MotionEvent.ACTION_MOVE && mIgnoring) {
                    return false;
                }

                // if the event is up or cancel return true if it was captured.
                // If we don't do this, this will make the checkboxes not
                // clickable
                if (action == MotionEvent.ACTION_UP ||
                        action == MotionEvent.ACTION_CANCEL) {
                    return mCaptured;
                }

                // Only move events will be handled below. So return false when it
                // is not a move event.
                if (action != MotionEvent.ACTION_MOVE) {
                    return false;
                }

                // is the view maximally expanded
                boolean expanded = mStartTy == mMinTranslationY;
                // is the view maximally collapsed
                boolean collapsed = mStartTy == mMaxTranslationY;

                // true if we are moving down the screen.
                // useful when combining with the recycler-view
                // ability to scroll up or down
                boolean down = rawY > mStartY;

                if (expanded && down) {
                    // if the view can scroll up, we ignore the event so the
                    // recycler will be able to scroll.
                    // This could be rewritten to start scrolling after full
                    // collapse, by clearing mIgnoring earlier on if we got
                    // into this state before
                    boolean canScrollUp = canRecyclerScrollUp(mTagsRecycler);
                    if (!canScrollUp) {
                        mClosing = true;
                        mCaptured = true;
                        return true;
                    } else {
                        mIgnoring = true;
                    }
                    return false;
                }


                if (down && !canRecyclerScrollUp(mTagsRecycler)) {
                    mCaptured = true;
                    return true;
                }

                if (collapsed && !down) {
                    mCaptured = true;
                    return true;
                }

                if (!down && !canRecyclerScrollDown(mTagsRecycler)) {
                    mCaptured = true;
                    return true;
                }

                mIgnoring = true;
                return false;
            }

            @Override
            public void onTouchEvent(RecyclerView rv, MotionEvent e) {
                int action = e.getAction();
                float newY = e.getRawY();
                float amount = mStartY - newY;
                float ty = mStartTy - amount;

                switch (action) {
                    case MotionEvent.ACTION_MOVE:

                        if (ty > mMaxTranslationY && !mClosing) {
                            ty = mMaxTranslationY;
                        }
                        if (ty < mMinTranslationY) {
                            ty = mMinTranslationY;
                        }
                        mContentView.setTranslationY(ty);
                        break;
                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_UP:
                        boolean closing = ty > mMaxTranslationY && mClosing;
                        snapToPosition(newY < mStartY, closing);
                        break;
                }
            }

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

            }
        });

        mAddTagView = parent.findViewById(R.id.add_tag);
        mAddTagView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAppsController.onAddAppToNewTag(mAppEntry);
                close();
            }
        });
        mCloseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                close();
            }
        });
        mOverlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                close();
            }
        });
        mContentView.setClickable(true);


        Animation animation = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_PARENT, 1,
                Animation.RELATIVE_TO_PARENT, 0);
        animation.setDuration(450);
        animation.setInterpolator(new DecelerateInterpolator());
        LayoutAnimationController controller = new LayoutAnimationController(animation) {

            @Override
            protected long getDelayForView(@NonNull View view) {
                int top = view.getTop();
                float pct = top / (float) mContentView.getHeight();
                return (int) (pct * 200);
            }
        };
        ((ViewGroup) mContentView).setLayoutAnimation(controller);
    }

    static boolean canRecyclerScrollUp(RecyclerView recyclerView) {
        View view = recyclerView.getChildAt(0);
        if (view == null) return false;

        boolean isFirst = recyclerView.getChildLayoutPosition(view) == 0;
        if (!isFirst) return true;

        int top = view.getTop();
        //noinspection RedundantIfStatement
        if (top < 0) return true;

        return false;
    }

    static boolean canRecyclerScrollDown(RecyclerView recyclerView) {
        View view = recyclerView.getChildAt(recyclerView.getChildCount() - 1);
        if (view == null) return false;

        int lastItemPosition = recyclerView.getAdapter().getItemCount() - 1;

        boolean isLast = recyclerView.getChildLayoutPosition(view) == lastItemPosition;
        if (!isLast) return true;

        int bottom = view.getBottom();
        //noinspection RedundantIfStatement
        if (bottom > recyclerView.getHeight()) return true;

        return false;
    }

    void snapToPosition(boolean expand, boolean closing) {
        if (expand) {
            mContentView.animate().translationY(mMinTranslationY);
        } else {
            if (closing) {
                close();
            }
            mContentView.animate().translationY(mMaxTranslationY);
        }
    }

    void close() {
        mBottomSheet.animate()
                .translationY(mBottomSheet.getHeight())
                .setListener(mOnClosedListener);
        mOverlay.animate().alpha(0);
    }

    public AppEntry getBoundAppEntry() {
        return mAppEntry;
    }

    void setAppTags(List<AppTag> allTags) {
        mAdapter.setItems(allTags);
    }

    public void updateAppliedTags(@Nullable List<TaggedApp> appliedTags) {
        mAdapter.setAppliedTags(appliedTags);
    }


    public void show(AppEntry entry, @Nullable List<TaggedApp> appliedTags) {
        setupAnimationValues();

        mAppEntry = entry;
        mAdapter.setAppliedTags(appliedTags);

        mAppIconView.setImageDrawable(entry.getIcon(mContext.getPackageManager()));
        mAppTitleView.setText(entry.getLabel());

        mBottomSheet.setVisibility(View.VISIBLE);
        mOverlay.setVisibility(View.VISIBLE);
        mOverlay.setAlpha(0);
        mOverlay.animate().alpha(1);

        mBottomSheet.setTranslationY(mBottomSheet.getHeight());
        mBottomSheet.animate().translationY(0).setListener(null);

        ((ViewGroup) mContentView).startLayoutAnimation();
    }

    private void setupAnimationValues() {
        int totalHeight = mOverlay.getHeight();
        int count = mAdapter.getItemCount();
        float density = mTagsRecycler.getResources().getDisplayMetrics().density;


        float headerHeight = 64 * density;
        int requiredHeightForAllItems = (int) ((
                mCellHeight * (count < 2 ? 2 : count)) + headerHeight);

        // this is the amount of items shown in collapsed size.
        // in case we have 2 or less tags, show two cells.
        // otherwise show 2.5 cells so the user can see the
        // view is  scrollable.
        int minDisplayHeight;
        if (count <= 2) {
            minDisplayHeight = (int) ((mCellHeight * 2) + headerHeight);
        } else {
            minDisplayHeight = (int) ((mCellHeight * 2.5f) + headerHeight);
        }

        int h = mContentView.getHeight();

        int maxHeight = Math.min(totalHeight, requiredHeightForAllItems);
        mMinTranslationY = h - maxHeight;

//        int statusBarHeight = (int) (24 * density);
        int statusBarHeight = 0;
        if (mMinTranslationY < statusBarHeight) {
            mMinTranslationY = statusBarHeight;
        }

//        int startHeight = (int) (4f * mCellHeight);

        int ty = h - minDisplayHeight;
        mMaxTranslationY = ty;

        mContentView.setTranslationY(ty);
    }


    class AppTagAdapter extends RecyclerView.Adapter<AppTagViewHolder> {

        final List<AppTag> mAppTags = new ArrayList<>();

        final List<TaggedApp> mAppliedAppTags = new ArrayList<>();

        AppTagAdapter() {
            setHasStableIds(true);
        }

        @Override
        public AppTagViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            final View view =
                    inflater.inflate(R.layout.list_item_tag_checkbox, parent, false);
            return new AppTagViewHolder(view, mAppsController);
        }

        @Override
        public void onBindViewHolder(AppTagViewHolder holder, int position) {
            AppTag tag = mAppTags.get(position);
            holder.bind(tag, mAppliedAppTags);
        }

        @Override
        public long getItemId(int position) {
            return mAppTags.get(position).id;
        }

        @Override
        public int getItemCount() {
            return mAppTags.size();
        }

        public void setAppliedTags(@Nullable List<TaggedApp> appliedAppTags) {
            mAppliedAppTags.clear();
            if (appliedAppTags != null) {
                mAppliedAppTags.addAll(appliedAppTags);
            }
            notifyDataSetChanged();
        }

        public void setItems(List<AppTag> allTags) {
            mAppTags.clear();
            for (AppTag tag : allTags) {
                if (tag.tagType == AppsContract.TagColumns.TAG_TYPE_USER) {
                    mAppTags.add(tag);
                }
            }
            notifyDataSetChanged();
        }
    }


    class AppTagViewHolder extends RecyclerView.ViewHolder
            implements CompoundButton.OnCheckedChangeListener {

        final AppsController mAppsController;

        private final TextView mLabel;

        private final CheckBox mCheckBox;

        AppTag mAppTag;

        @Nullable
        TaggedApp mTaggedApp;

        public AppTagViewHolder(View itemView, AppsController appsController) {
            super(itemView);
            mAppsController = appsController;
            mLabel = (TextView) itemView.findViewById(R.id.checkbox_label);
            mCheckBox = (CheckBox) itemView.findViewById(R.id.checkbox);
        }


        void bind(final AppTag appTag, List<TaggedApp> appliedTags) {
            mAppTag = appTag;

            final TaggedApp taggedApp = getTaggedApp(appTag, appliedTags);
            mTaggedApp = taggedApp;

            boolean checked = taggedApp != null;
            mLabel.setText(appTag.title);
            mCheckBox.setOnCheckedChangeListener(null);
            mCheckBox.setChecked(checked);
            mCheckBox.setOnCheckedChangeListener(this);
        }

        /**
         * Gets the TaggedApp for the given AppTag. Will return null if
         * the app-entry was not tagged with the given app-tag
         */
        @Nullable
        TaggedApp getTaggedApp(AppTag appTag, List<TaggedApp> appliedTags) {
            int count = appliedTags.size();
            for (int i = 0; i < count; i++) {
                TaggedApp taggedApp = appliedTags.get(i);
                if (TextUtils.equals(taggedApp.mTagName, appTag.title)) {
                    return taggedApp;
                }
            }
            return null;
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                mAppsController.onAddAppToTag(mAppTag, mAppEntry);
            } else {
                mAppsController.onRemoveAppFromTag(mTaggedApp);
            }

        }
    }


}
