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

package com.appsimobile.appsii.module.home;

import android.annotation.SuppressLint;
import android.app.LoaderManager;
import android.appwidget.AppWidgetHost;
import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.android.colorpicker.ColorPickerDialog;
import com.android.colorpicker.ColorPickerSwatch;
import com.appsimobile.appsii.BuildConfig;
import com.appsimobile.appsii.R;
import com.appsimobile.appsii.ThemingUtils;
import com.appsimobile.appsii.dagger.AppInjector;
import com.appsimobile.appsii.module.home.config.HomeItemConfiguration;
import com.appsimobile.appsii.module.home.provider.HomeContract;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

/**
 * Editor for the home screen
 * Created by nick on 24/01/15.
 */
public class HomeEditorActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<List<HomeItem>>, OnWrapperClickedListener {


    /**
     * An extra that must be provided to identify the page to edit
     */
    public static final String EXTRA_PAGE_ID = BuildConfig.APPLICATION_ID + ".extra_page_id";

    public static final String EXTRA_PAGE_TITLE = BuildConfig.APPLICATION_ID + ".extra_page_title";

    private static final Interpolator sQuinticInterpolator = new Interpolator() {
        public float getInterpolation(float t) {
            t -= 1.0f;
            return t * t * t * t * t + 1.0f;
        }
    };

    /**
     * The handler thread to which the operations to be performed on the db are executed
     * The {@link #mSaveHandler} uses this thread
     */
    final HandlerThread mHandlerThread;

    /**
     * A rect to recycle when calculating cell positions
     */
    final Rect mPositionRect = new Rect();

    /**
     * The handler used to post position updates to after the content has been loaded.
     * This is executed with a minor delay to ensure the structural change has been
     * completely executed and the proper locations can be calculated
     */
    final Handler mHandler;

    /**
     * The recycler-view showing the items
     */
    RecyclerView mRecyclerView;

    /**
     * The home adapter
     */
    HomeAdapter mHomeAdapter;

    /**
     * Layout manager for the editor
     */
    GridLayoutManager mGridLayoutManager;

    /**
     * The decorator used to draw the highlights
     */
    SingleSelectionDecoration mSingleSelectionDecoration;

    /**
     * The action-mode, if it is active.
     */
    ActionMode mActionMode;

    /**
     * The initialized action-mode callback
     */
    ActionModeCallbackImpl mActionModeCallback;

    /**
     * The handler used to save the operations to the database. Runs on a different
     * thread
     */
    Handler mSaveHandler;

    /**
     * The callback handling messages on the save-handler.
     */
    Handler.Callback mCallback;

    /**
     * The page-id to which the editor is bound
     */
    long mPageId;

    /**
     * The page-id to which the editor is bound
     */
    String mPageTitle;

    /**
     * View (button) to increase the span
     */
    View mIncreaseSpanView;

    /**
     * View (button) to decrease the span
     */
    View mDecreaseSpanView;

    /**
     * View (button) to move the cell left
     */
    View mMoveLeftView;

    /**
     * View (button) to move the cell right
     */
    View mMoveRightView;

    /**
     * View (button) to move the row up
     */
    View mMoveUpView;

    /**
     * View (button) to move the row down
     */
    View mMoveDownView;

    /**
     * View (button) to decrease the row-height
     */
    View mSmallerView;

    /**
     * View (button) to increase the row-height
     */
    View mLargerView;

    /**
     * The editor instance of the HomeAdapter.
     */
    HomeAdapter.HomeAdapterEditor mHomeAdapterEditor;

    boolean mApplyingNewData;

    ViewGroup mRootView;

    @Inject
    SharedPreferences mSharedPreferences;

    public HomeEditorActivity() {
        mHandlerThread = new HandlerThread("db-thread");
        mHandlerThread.start();

        mHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                return doHandleMessage(msg);
            }
        });
    }

    private static void ensureInvisible(View view) {
        if (view.getVisibility() != View.INVISIBLE) {
            view.setVisibility(View.INVISIBLE);
        }
    }

    private static void centerView(View view, Rect positionRect) {
        view.setVisibility(View.INVISIBLE);
        int x = positionRect.left + (positionRect.width() - view.getWidth()) / 2;
        int y = positionRect.top + (positionRect.height() - view.getHeight()) / 2;
        view.setTranslationX(x);
        view.setTranslationY(y);
    }

    /**
     * Handles a message sent to the update positions handler
     */
    boolean doHandleMessage(Message msg) {
        if (msg.what == 0) {
            updateEditViewPosition();
            return true;
        }
        return false;
    }

    /**
     * Updates the positions of the edit overlay views, and hides/shows them as needed
     */
    void updateEditViewPosition() {
        if (mApplyingNewData) return;

        int position = mSingleSelectionDecoration.mSelectedPosition;

        // if the view is not visible, hide all controls
        if (!getRecyclerViewChildBounds(position, mPositionRect)) {
            mMoveLeftView.setVisibility(View.GONE);
            mMoveRightView.setVisibility(View.GONE);
            mMoveUpView.setVisibility(View.GONE);
            mMoveDownView.setVisibility(View.GONE);
            mIncreaseSpanView.setVisibility(View.GONE);
            mDecreaseSpanView.setVisibility(View.GONE);
            mLargerView.setVisibility(View.GONE);
            mSmallerView.setVisibility(View.GONE);
            return;
        }

        // if the view can move left, show and update the controls positions
        if (mHomeAdapterEditor.canMoveLeft(position)) {
            int x = mPositionRect.left - mMoveLeftView.getWidth();
            int y = mPositionRect.top + (mPositionRect.height() - mMoveLeftView.getHeight()) / 2;

            animateToXy(mMoveLeftView, x, y);
        } else {
            ensureInvisible(mMoveLeftView);
        }

        // if the view can move right, show and update the controls positions
        if (mHomeAdapterEditor.canMoveRight(position)) {
            int x = mPositionRect.right;
            int y = mPositionRect.top + (mPositionRect.height() - mMoveRightView.getHeight()) / 2;

            animateToXy(mMoveRightView, x, y);
        } else {
            ensureInvisible(mMoveRightView);
        }

        // if the view's row can move up, show and update the controls positions
        if (mHomeAdapterEditor.canMoveUp(position)) {
            int x = mPositionRect.left + (mPositionRect.width() - mMoveUpView.getWidth()) / 2;
            int y = mPositionRect.top - mMoveUpView.getHeight();

            animateToXy(mMoveUpView, x, y);
        } else {
            ensureInvisible(mMoveUpView);
        }

        // if the view's row can move down, show and update the controls positions
        if (mHomeAdapterEditor.canMoveDown(position)) {
            int x = mPositionRect.left + (mPositionRect.width() - mMoveDownView.getWidth()) / 2;
            int y = mPositionRect.bottom;

            animateToXy(mMoveDownView, x, y);
        } else {
            ensureInvisible(mMoveDownView);
        }

        // we need these so we can properly position the actions
        final boolean canDecreaseHeight = mHomeAdapterEditor.canDecreaseHeight(position);
        final boolean canIncreaseHeight = mHomeAdapterEditor.canIncreaseHeight(position);
        final boolean canIncreaseSpan = mHomeAdapterEditor.canIncreaseSpan(position);
        final boolean canDecreaseSpan = mHomeAdapterEditor.canDecreaseSpan(position);

        // if the view's row's height can be increased, show and update the controls positions
        if (canIncreaseHeight) {

            // when the height can also be decreased, position this item left at 25%
            // otherwise position it at 50%
            float fx = canDecreaseHeight ? .25f : .5f;
            // when the span can be changed, position this item top at 25%
            // otherwise position it at 50%
            float fy = (!canIncreaseSpan && !canDecreaseSpan) ? .5f : .25f;

            int x = (int) (mPositionRect.left +
                    (mPositionRect.width() * fx - mLargerView.getWidth() / 2));

            int y = (int) (mPositionRect.top +
                    (mPositionRect.height() * fy - mLargerView.getHeight() / 2));

            animateToXy(mLargerView, x, y);

        } else {
            ensureInvisible(mLargerView);
        }

        // if the view's row's height can be decreased, show and update the controls positions
        if (canDecreaseHeight) {
            float fx = canIncreaseHeight ? .75f : .5f;
            float fy = (!canIncreaseSpan && !canDecreaseSpan) ? .5f : .25f;

            int x = (int) (mPositionRect.left +
                    (mPositionRect.width() * fx - mSmallerView.getWidth() / 2));

            int y = (int) (mPositionRect.top +
                    (mPositionRect.height() * fy - mSmallerView.getHeight() / 2));

            animateToXy(mSmallerView, x, y);
        } else {
            ensureInvisible(mSmallerView);
        }


        // if the view's width can be increased, show and update the controls positions
        if (canIncreaseSpan) {
            float fx = canDecreaseSpan ? .75f : .5f;
            float fy = (!canIncreaseHeight && !canDecreaseHeight) ? .5f : .75f;


            int x = (int) (mPositionRect.left +
                    (mPositionRect.width() * fx - mIncreaseSpanView.getWidth() / 2));

            int y = (int) (mPositionRect.top +
                    (mPositionRect.height() * fy - mIncreaseSpanView.getHeight() / 2));

            animateToXy(mIncreaseSpanView, x, y);
        } else {
            ensureInvisible(mIncreaseSpanView);
        }

        // if the view's width can be decreased, show and update the controls positions
        if (canDecreaseSpan) {
            float fx = canIncreaseSpan ? .25f : .5f;
            float fy = (!canIncreaseHeight && !canDecreaseHeight) ? .5f : .75f;

            int x = (int) (mPositionRect.left +
                    (mPositionRect.width() * fx - mDecreaseSpanView.getWidth() / 2));

            int y = (int) (mPositionRect.top +
                    (mPositionRect.height() * fy - mDecreaseSpanView.getHeight() / 2));

            animateToXy(mDecreaseSpanView, x, y);
        } else {
            ensureInvisible(mDecreaseSpanView);
        }
    }

    /**
     * Fills the provided bounds with the position of the view relative to the recycler-view
     * Returns false if the view does not exist or is not visible. Otherwise true is returned
     */
    boolean getRecyclerViewChildBounds(int position, Rect bounds) {
        View child = getRecyclerViewChildAt(position);
        if (child == null) return false;

        bounds.set(0, 0, child.getWidth(), child.getHeight());
        mRecyclerView.offsetDescendantRectToMyCoords(child, bounds);
        return true;
    }

    private void animateToXy(View view, int targetX, int targetY) {

        view.setVisibility(View.VISIBLE);
        view.animate().
                setDuration(150).
                translationX(targetX).
                translationY(targetY);

    }

    View getRecyclerViewChildAt(int position) {
        View view = mRecyclerView.getChildAt(0);
        if (view == null) return null;

        int firstVisiblePosition = mRecyclerView.getChildLayoutPosition(view);
        if (firstVisiblePosition > position) return null;

        int childPosition = position - firstVisiblePosition;
        return mRecyclerView.getChildAt(childPosition);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppInjector.inject(this);

        Context context = ThemingUtils.createContextThemeWrapper(this, mSharedPreferences);

        setContentView(R.layout.activity_home_editor);

        mMoveLeftView = findViewById(R.id.move_left);
        mMoveRightView = findViewById(R.id.move_right);
        mMoveUpView = findViewById(R.id.move_up);
        mMoveDownView = findViewById(R.id.move_down);
        mSmallerView = findViewById(R.id.decrease_height);
        mLargerView = findViewById(R.id.increase_height);
        mDecreaseSpanView = findViewById(R.id.decrease_span);
        mIncreaseSpanView = findViewById(R.id.increase_span);

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler);
        mRootView = (ViewGroup) findViewById(R.id.home_editor_root);

        mRecyclerView.getItemAnimator().setChangeDuration(250);
        mRecyclerView.getItemAnimator().setMoveDuration(250);
        mRecyclerView.getItemAnimator().setRemoveDuration(250);
        mRecyclerView.getItemAnimator().setAddDuration(250);

        mMoveLeftView.setVisibility(View.INVISIBLE);
        mMoveRightView.setVisibility(View.INVISIBLE);
        mMoveUpView.setVisibility(View.INVISIBLE);
        mMoveDownView.setVisibility(View.INVISIBLE);
        mSmallerView.setVisibility(View.INVISIBLE);
        mLargerView.setVisibility(View.INVISIBLE);
        mDecreaseSpanView.setVisibility(View.INVISIBLE);
        mIncreaseSpanView.setVisibility(View.INVISIBLE);

        final Intent intent = getIntent();
        mPageId = intent.getLongExtra(EXTRA_PAGE_ID, -1);
        mPageTitle = intent.getStringExtra(EXTRA_PAGE_TITLE);
        getSupportActionBar().setTitle(mPageTitle);

        mHomeAdapter = new HomeAdapter(context,
                new InterceptClicksViewWrapperFactory(this), mPageId, false);


        TypedArray a = context.obtainStyledAttributes(new int[]{R.attr.appsiSidebarBackground});
        getWindow().setBackgroundDrawable(a.getDrawable(0));
        a.recycle();

        mHomeAdapterEditor = mHomeAdapter.getEditor();

        HomeAdapter.HomeSpanSizeLookup spanSizeLookup = mHomeAdapter.getHomeSpanSizeLookup();

        mGridLayoutManager = new GridLayoutManager(this, 12);
        mGridLayoutManager.setSpanSizeLookup(spanSizeLookup);

        mRecyclerView.setLayoutManager(mGridLayoutManager);
        mRecyclerView.setAdapter(mHomeAdapter);
        mSingleSelectionDecoration = new SingleSelectionDecoration(context);
        mRecyclerView.addItemDecoration(new GridLayoutDecoration(context));
        mRecyclerView.addItemDecoration(mSingleSelectionDecoration);

        mRecyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (mActionMode != null) {
                    scrollEditViews(dy);
                }
            }

        });

        getLoaderManager().initLoader(0, null, this);
        mCallback = new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                @SuppressWarnings("unchecked") ArrayList<ContentProviderOperation> ops =
                        (ArrayList<ContentProviderOperation>) msg.obj;

                applyContentProviderOperationsAsync(ops);
                return false;
            }
        };
        mSaveHandler = new Handler(mHandlerThread.getLooper(), mCallback);

        if (savedInstanceState != null) {
            int selection = savedInstanceState.getInt("selection");
            mSingleSelectionDecoration.setSelectedPosition(selection);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // terminate the looper to stop leaking this thread
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mSaveHandler.getLooper().quitSafely();
        } else {
            mSaveHandler.getLooper().quit();
        }
    }

    /**
     * Scrolls the edit-views the same amount the recycler-view was scrolled.
     * This makes it look synchronous.
     */
    void scrollEditViews(int dy) {
        scrollEditView(mLargerView, dy);
        scrollEditView(mSmallerView, dy);
        scrollEditView(mMoveUpView, dy);
        scrollEditView(mMoveDownView, dy);
        scrollEditView(mMoveLeftView, dy);
        scrollEditView(mMoveRightView, dy);
        scrollEditView(mDecreaseSpanView, dy);
        scrollEditView(mIncreaseSpanView, dy);
    }

    void applyContentProviderOperationsAsync(ArrayList<ContentProviderOperation> ops) {
        try {
            Log.wtf("HomeEditorActivity", "applying batch...");
            getContentResolver().applyBatch(HomeContract.AUTHORITY, ops);
        } catch (RemoteException | OperationApplicationException e) {
            Log.wtf("HomeEditorActivity", "error applying batch", e);
        }
    }

    /**
     * Updates the scroll, of a single child
     */
    private void scrollEditView(View view, int dy) {
        view.setTranslationY(view.getTranslationY() - dy);
    }

    @Override
    public Loader<List<HomeItem>> onCreateLoader(int id, Bundle args) {
        return new HomeLoader(this);
    }

    @Override
    public void onLoadFinished(Loader<List<HomeItem>> loader, List<HomeItem> data) {
        mApplyingNewData = true;
        int lastPosition = mSingleSelectionDecoration.mSelectedPosition;
        long lastId;
        boolean restored;
        if (lastPosition == -1) {
            lastId = -1;
            restored = false;
        } else {
            lastId = mHomeAdapter.getItemId(lastPosition);
            // we have just been restored and want to
            // restore the selection.
            restored = mHomeAdapter.getItemCount() == 0;
        }

        mHomeAdapter.setHomeItems(data);

        int newPosition = mHomeAdapter.getPositionOfId(lastId);
        if (!restored) {
            mSingleSelectionDecoration.setSelectedPosition(newPosition);
        }
        if (newPosition == -1) {
            if (mActionMode != null) {
                mActionMode.finish();
            }
        }
        postUpdatePositions();
        mApplyingNewData = false;
    }

    void postUpdatePositions() {
        Message message = mHandler.obtainMessage(0);
        mHandler.sendMessageDelayed(message, 10);
    }

    @Override
    public void onLoaderReset(Loader<List<HomeItem>> loader) {

    }

    @Override
    protected void onPause() {
        super.onPause();
        mHandler.removeMessages(0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mActionMode == null && mSingleSelectionDecoration.mSelectedPosition != -1) {
            startEditActionMode(mSingleSelectionDecoration.mSelectedPosition);
            ColorPickerDialog dialog =
                    (ColorPickerDialog) getFragmentManager().findFragmentByTag("color_picker");
            if (dialog != null) {
                dialog.setOnColorSelectedListener(mActionModeCallback);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("selection", mSingleSelectionDecoration.mSelectedPosition);
    }

    private void startEditActionMode(int position) {
        mActionModeCallback = new ActionModeCallbackImpl(mHomeAdapterEditor);
        mActionMode = startSupportActionMode(mActionModeCallback);
        mSingleSelectionDecoration.setSelectedPosition(position);
        prepositionEditViews();
    }

    void prepositionEditViews() {
        mRootView.setLayoutTransition(null);

        int position = mSingleSelectionDecoration.mSelectedPosition;

        if (getRecyclerViewChildBounds(position, mPositionRect)) {
            centerView(mMoveLeftView, mPositionRect);
            centerView(mMoveRightView, mPositionRect);
            centerView(mMoveUpView, mPositionRect);
            centerView(mMoveDownView, mPositionRect);
            centerView(mIncreaseSpanView, mPositionRect);
            centerView(mDecreaseSpanView, mPositionRect);
            centerView(mLargerView, mPositionRect);
            centerView(mSmallerView, mPositionRect);
        }

    }

    @Override
    public void onWrapperClicked(HomeViewWrapper wrapper) {
        int position = mRecyclerView.getChildLayoutPosition(wrapper);

        if (mActionMode != null) {
            mSingleSelectionDecoration.setSelectedPosition(-1);
            mActionMode.finish();
        } else if (position != mSingleSelectionDecoration.mSelectedPosition) {
            startEditActionMode(position);
        }
        mRecyclerView.invalidate();
    }

    private void performSwitchPositions(int positionFrom, int positionTo) {
        mActionModeCallback.confirmSwitchItemPositions(positionFrom, positionTo);
    }

    class InterceptClicksViewWrapperFactory extends HomeAdapter.DefaultViewWrapperFactory
            implements View.OnClickListener {

        final OnWrapperClickedListener mListener;

        InterceptClicksViewWrapperFactory(OnWrapperClickedListener listener) {
            mListener = listener;
        }

        public HomeViewWrapper wrapView(Context context, View child, int heightPx) {
            HomeViewWrapper wrapper = super.wrapView(context, child, heightPx);
            View blocker = new View(context);
            blocker.setTag(wrapper);
            wrapper.addView(blocker, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            blocker.setOnClickListener(this);

            return wrapper;
        }

        @Override
        public void onClick(View v) {
            HomeViewWrapper wrapper = (HomeViewWrapper) v.getTag();
            mListener.onWrapperClicked(wrapper);
        }
    }

    class SingleSelectionDecoration extends RecyclerView.ItemDecoration {

        final Paint mPaint;

        final Paint mSelectionOutlinePaint;

        final Paint mSelectionPaint;

        final Rect mRect = new Rect();

        final RectF mRectf = new RectF();

        final float mCornerRadius;

        private final int[] ATTRS = new int[]{
                R.attr.colorAccent,
                R.attr.appsiSidebarBackground,
                R.attr.appsiTextShadow,
        };

        int mSelectedPosition = -1;

        SingleSelectionDecoration(Context context) {
            mCornerRadius = context.getResources().getDisplayMetrics().density * 1;
            float strokeWidth = context.getResources().getDisplayMetrics().density * 2;

            final TypedArray a = context.obtainStyledAttributes(ATTRS);
            int accentColor = a.getColor(0, Color.WHITE);
            int textShadow = a.getColor(2, Color.WHITE);
            a.recycle();


            mPaint = new Paint();
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(textShadow);

            mSelectionOutlinePaint = new Paint();
            mSelectionOutlinePaint.setStyle(Paint.Style.STROKE);
            mSelectionOutlinePaint.setStrokeWidth(strokeWidth);
            mSelectionOutlinePaint.setColor(accentColor);

            mSelectionPaint = new Paint();
            mSelectionPaint.setStyle(Paint.Style.FILL);
            mSelectionPaint.setStrokeWidth(strokeWidth);
            mSelectionPaint.setColor(accentColor);
            mSelectionPaint.setAlpha(0x10);

        }

        public void setSelectedPosition(int selectedPosition) {
            mSelectedPosition = selectedPosition;
            postUpdatePositions();
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

                if (position != mSelectedPosition) {
//                    c.drawRoundRect(mRectf, mCornerRadius, mCornerRadius, mPaint);
                } else {
                    c.drawRoundRect(mRectf, mCornerRadius, mCornerRadius, mSelectionOutlinePaint);
                    c.drawRoundRect(mRectf, mCornerRadius, mCornerRadius, mSelectionPaint);
                }
            }
        }
    }

    public class ActionModeCallbackImpl implements ActionMode.Callback, View.OnClickListener,
            PopupMenu.OnMenuItemClickListener, ColorPickerSwatch.OnColorSelectedListener {

        final ArrayList<ContentProviderOperation> mContentProviderOperations = new ArrayList<>();
        final HomeAdapter.HomeAdapterEditor mEditor;
        @Inject
        AppWidgetHost mAppWidgetHost;
        View mInsertView;

        View mDeleteView;

        View mEditView;

        View mColorizeView;

        View mNoColorView;

        @Inject
        HomeItemConfiguration mHomeItemConfiguration;

        public ActionModeCallbackImpl(HomeAdapter.HomeAdapterEditor editor) {
            mEditor = editor;
            AppInjector.inject(this);
        }

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            LayoutInflater layoutInflater = LayoutInflater.from(HomeEditorActivity.this);

            // suppress this warning. We can't set this properly
            @SuppressLint("InflateParams")
            View view = layoutInflater.inflate(R.layout.include_home_editor_actions, null);

            actionMode.setCustomView(view);

            mInsertView = view.findViewById(R.id.add);
            mDeleteView = view.findViewById(R.id.remove);
//            mMoveView = view.findViewById(R.id.move);
            mEditView = view.findViewById(R.id.edit);
            mColorizeView = view.findViewById(R.id.colorize);
            mNoColorView = view.findViewById(R.id.no_color);

            mInsertView.setOnClickListener(this);
            mDeleteView.setOnClickListener(this);
            mMoveLeftView.setOnClickListener(this);
            mMoveRightView.setOnClickListener(this);
            mMoveUpView.setOnClickListener(this);
            mMoveDownView.setOnClickListener(this);
            mSmallerView.setOnClickListener(this);
            mLargerView.setOnClickListener(this);
            mDecreaseSpanView.setOnClickListener(this);
            mIncreaseSpanView.setOnClickListener(this);
//            mMoveView.setOnClickListener(this);
            mEditView.setOnClickListener(this);
            mColorizeView.setOnClickListener(this);
            mNoColorView.setOnClickListener(this);

            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            mActionMode = null;
            mSingleSelectionDecoration.setSelectedPosition(-1);
            mRecyclerView.invalidate();

            mMoveLeftView.setVisibility(View.GONE);
            mMoveRightView.setVisibility(View.GONE);
            mMoveUpView.setVisibility(View.GONE);
            mMoveDownView.setVisibility(View.GONE);
            mSmallerView.setVisibility(View.GONE);
            mLargerView.setVisibility(View.GONE);
            mDecreaseSpanView.setVisibility(View.GONE);
            mIncreaseSpanView.setVisibility(View.GONE);
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            int position = mSingleSelectionDecoration.mSelectedPosition;
            int itemId = item.getItemId();

            ArrayList<ContentProviderOperation> ops = mContentProviderOperations;
            ops.clear();

            switch (itemId) {
                case R.id.action_remove_cell:
                    if (mHomeAdapter.getItemCount() > 1) {
                        mEditor.removeCellAtPosition(position, ops);
                    } else {
                        Toast.makeText(HomeEditorActivity.this,
                                R.string.need_at_least_one_cell, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case R.id.action_remove_row:
                    if (mEditor.hasAtLeastTwoRows()) {
                        mEditor.removeRowAtItemPosition(position, ops);
                    } else {
                        Toast.makeText(HomeEditorActivity.this,
                                R.string.need_at_least_one_cell, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case R.id.action_add_cell_left:
                    mEditor.insertCellLeftOfPosition(position, ops);
                    break;
                case R.id.action_add_cell_right:
                    mEditor.insertCellRightOfPosition(position, ops);
                    break;
                case R.id.action_add_row_above:
                    mEditor.insertRowAbovePosition(position, ops);
                    break;
                case R.id.action_add_row_below:
                    mEditor.insertRowBelowPosition(position, ops);
                    break;
                case R.id.action_set_item_none:
                    doChangeType(position, HomeContract.Cells.DISPLAY_TYPE_UNSET, ops);
                    break;
                case R.id.action_set_item_bluetooth:
                    doChangeType(position, HomeContract.Cells.DISPLAY_TYPE_BLUETOOTH_TOGGLE, ops);
                    break;
                case R.id.action_set_item_clock:
                    doChangeType(position, HomeContract.Cells.DISPLAY_TYPE_CLOCK, ops);
                    break;
                case R.id.action_set_item_app:
                    doChangeType(position, HomeContract.Cells.DISPLAY_TYPE_INTENT, ops);
                    break;
                case R.id.action_set_item_image:
                    doChangeType(position, HomeContract.Cells.DISPLAY_TYPE_PROFILE_IMAGE, ops);
                    break;
                case R.id.action_set_item_sun:
                    doChangeType(position, HomeContract.Cells.DISPLAY_TYPE_WEATHER_SUNRISE, ops);
                    break;
                case R.id.action_set_item_temperature:
                    doChangeType(position, HomeContract.Cells.DISPLAY_TYPE_WEATHER_TEMP, ops);
                    break;
                case R.id.action_set_item_wind:
                    doChangeType(position, HomeContract.Cells.DISPLAY_TYPE_WEATHER_WIND, ops);
                    break;
                case R.id.action_set_app_widget:
                    doChangeType(position, HomeContract.Cells.DISPLAY_TYPE_APP_WIDGET, ops);
                    break;

                default:
                    return false;
            }
            applyContentProviderOperationsAsync(ops);
            return true;
        }

        private void doChangeType(int position, int type, ArrayList<ContentProviderOperation> ops) {
            HomeItem homeItem = mHomeAdapter.getItemAt(position);

            // When this cell is an app-widget right now, clean up the id,
            // and release it.
            if (homeItem.mDisplayType == HomeContract.Cells.DISPLAY_TYPE_APP_WIDGET) {
                HomeItemConfiguration configurationHelper = mHomeItemConfiguration;

                // get the current id.
                String widgetId = configurationHelper.
                        getProperty(homeItem.mId, "app_widget_id", null);
                if (widgetId != null) {
                    int id = Integer.parseInt(widgetId);
                    mAppWidgetHost.deleteAppWidgetId(id);
                }
                // now remove it from the database and from the app-widget host
                configurationHelper.removeAllProperties(homeItem.mId);
            }

            mEditor.changeCellType(position, type, ops);
        }

        public void confirmSwitchItemPositions(int positionFrom, int positionTo) {
            if (positionFrom != positionTo) {
                mContentProviderOperations.clear();
                mEditor.switchItemPositions(positionFrom, positionTo, mContentProviderOperations);
                applyContentProviderOperationsAsync(mContentProviderOperations);
            }
            mActionModeCallback.unblock();

        }

        public void unblock() {
            enableActions(true);
        }

        void enableActions(boolean enable) {
            int visibility = enable ? View.VISIBLE : View.GONE;
            mEditView.setVisibility(visibility);
            mInsertView.setVisibility(visibility);
            mDeleteView.setVisibility(visibility);
            mMoveLeftView.setVisibility(visibility);
            mMoveRightView.setVisibility(visibility);
            mMoveUpView.setVisibility(visibility);
            mMoveDownView.setVisibility(visibility);
            mSmallerView.setVisibility(visibility);
            mLargerView.setVisibility(visibility);
            mDecreaseSpanView.setVisibility(visibility);
            mIncreaseSpanView.setVisibility(visibility);
//            mMoveView.setVisibility(visibility);
        }

        @Override
        public void onClick(View v) {
            // TODO: why does this happen? is there an orientation change involved?
            if (mSingleSelectionDecoration.mSelectedPosition == -1) {
                if (mActionMode != null) {
                    mActionMode.finish();
                }
                return;
            }
            int id = v.getId();
            switch (id) {
                case R.id.colorize:
                    onColorizeViewPressed(v);
                    break;
                case R.id.no_color:
                    onNoColorPressed(v);
                    break;
                case R.id.add:
                    onInsertViewPressed(v);
                    break;
                case R.id.remove:
                    onRemovePressed(v);
                    break;
                case R.id.move_left:
                    onMoveLeftPressed();
                    break;
                case R.id.move_right:
                    onMoveRightPressed();
                    break;
                case R.id.move_up:
                    onMoveUpPressed();
                    break;
                case R.id.move_down:
                    onMoveDownPressed();
                    break;
                case R.id.decrease_height:
                    onDecreaseHeightPressed();
                    break;
                case R.id.increase_height:
                    onIncreaseHeightPressed();
                    break;
                case R.id.decrease_span:
                    onDecreaseSpanPressed();
                    break;
                case R.id.increase_span:
                    onIncreaseSpanPressed();
                    break;
                case R.id.edit:
                    onEditPressed(v);
                    break;
            }
        }


        private void onIncreaseSpanPressed() {
            int selection = mSingleSelectionDecoration.mSelectedPosition;

            mContentProviderOperations.clear();
            mEditor.increaseSpan(selection, mContentProviderOperations);
            applyOperations(mContentProviderOperations);
        }


        private void onDecreaseSpanPressed() {
            int selection = mSingleSelectionDecoration.mSelectedPosition;

            mContentProviderOperations.clear();
            mEditor.decreaseSpan(selection, mContentProviderOperations);
            applyOperations(mContentProviderOperations);

        }

        private void onEditPressed(View v) {
            PopupMenu popupMenu = new PopupMenu(HomeEditorActivity.this, v);

            MenuInflater menuInflater = popupMenu.getMenuInflater();
            Menu menu = popupMenu.getMenu();
            menuInflater.inflate(R.menu.home_editor_edit_options, menu);

            popupMenu.setOnMenuItemClickListener(this);

            popupMenu.show();

        }

        private void onIncreaseHeightPressed() {
            int selection = mSingleSelectionDecoration.mSelectedPosition;
            mContentProviderOperations.clear();
            mEditor.increaseHeight(selection, mContentProviderOperations);
            applyContentProviderOperationsAsync(mContentProviderOperations);
        }


        private void onDecreaseHeightPressed() {
            int selection = mSingleSelectionDecoration.mSelectedPosition;
            mContentProviderOperations.clear();
            mEditor.decreaseHeight(selection, mContentProviderOperations);
            applyContentProviderOperationsAsync(mContentProviderOperations);
        }

        private void onMoveDownPressed() {
            int selection = mSingleSelectionDecoration.mSelectedPosition;
            HomeItem homeItem = mEditor.getItemAt(selection);

            mContentProviderOperations.clear();
            mEditor.moveRowDown(homeItem.mRowPosition, mContentProviderOperations);
            applyContentProviderOperationsAsync(mContentProviderOperations);


            updateSelection(homeItem);
        }

        private void onMoveUpPressed() {
            int selection = mSingleSelectionDecoration.mSelectedPosition;
            HomeItem homeItem = mEditor.getItemAt(selection);

            mContentProviderOperations.clear();
            mEditor.moveRowUp(homeItem.mRowPosition, mContentProviderOperations);
            applyContentProviderOperationsAsync(mContentProviderOperations);

            updateSelection(homeItem);
        }

        void updateSelection(HomeItem homeItem) {
//            mSingleSelectionDecoration.setSelectedPosition(mEditor.getItemPosition(homeItem.mId));
        }

        private void onMoveRightPressed() {
            int selection = mSingleSelectionDecoration.mSelectedPosition;
            HomeItem homeItem = mEditor.getItemAt(selection);

            mContentProviderOperations.clear();
            mEditor.moveItemRight(selection, mContentProviderOperations);
            applyContentProviderOperationsAsync(mContentProviderOperations);

            updateSelection(homeItem);
        }

        private void onMoveLeftPressed() {
            int selection = mSingleSelectionDecoration.mSelectedPosition;
            HomeItem homeItem = mEditor.getItemAt(selection);

            mContentProviderOperations.clear();
            mEditor.moveItemLeft(selection, mContentProviderOperations);
            applyContentProviderOperationsAsync(mContentProviderOperations);

            updateSelection(homeItem);
        }

        private void onRemovePressed(View v) {
            PopupMenu popupMenu = new PopupMenu(HomeEditorActivity.this, v);

            MenuInflater menuInflater = popupMenu.getMenuInflater();
            Menu menu = popupMenu.getMenu();
            menuInflater.inflate(R.menu.home_editor_remove_options, menu);

            popupMenu.setOnMenuItemClickListener(this);

            popupMenu.show();
        }

        private void onInsertViewPressed(View v) {
            int selection = mSingleSelectionDecoration.mSelectedPosition;

            PopupMenu popupMenu = new PopupMenu(HomeEditorActivity.this, v);

            MenuInflater menuInflater = popupMenu.getMenuInflater();
            Menu menu = popupMenu.getMenu();
            boolean canAddCells = mEditor.canAddCellsInItemRow(selection);
            menuInflater.inflate(R.menu.home_editor_add_options, menu);

            if (!canAddCells) {
                menu.findItem(R.id.action_add_cell_left).setEnabled(false);
                menu.findItem(R.id.action_add_cell_right).setEnabled(false);
            }

            popupMenu.setOnMenuItemClickListener(this);

            popupMenu.show();

        }

        void onNoColorPressed(View v) {
            onColorSelected(Color.TRANSPARENT);
        }

        private void onColorizeViewPressed(View v) {
            int selection = mSingleSelectionDecoration.mSelectedPosition;
            if (selection == -1) return;

            HomeItem homeItem = mHomeAdapter.getItemAt(selection);

            ColorPickerDialog colorPickerDialog =
                    ColorPickerDialog.newInstance(R.string.color_picker_default_title,
                            new int[]{
                                    // grey tints
                                    0xFFCCCCCC, 0xFF333333, 0xFF999999, 0xFF666666,
                                    // 500 values from palette
                                    0xFF607D8B, 0xFF000000, 0xFFF44336, 0xFFE91E63,
                                    0xFF9C27B0, 0xFF673AB7, 0xFF3F51B5, 0xFF2196F3,
                                    0xFF03A9F4, 0xFF00BCD4, 0xFF009688, 0xFF4CAF50,
                                    0xFF8BC34A, 0xFFCDDC39, 0xFFFFEB3B, 0xFFFFC107,
                                    0xFFFF9800, 0xFFFF5722, 0xFF795548, 0xFF9E9E9E,
                                    // 100 values from palette
                                    0xFFFFCDD2, 0xFFF8BBD0, 0xFFE1BEE7, 0xFFD1C4E9,
                                    0xFFC5CAE9, 0xFFBBDEFB, 0xFFB3E5FC, 0xFFB2EBF2,
                                    0xFFB2DFDB, 0xFFC8E6C9, 0xFFDCEDC8, 0xFFF0F4C3,
                                    0xFFFFF9C4, 0xFFFFECB3, 0xFFFFE0B2, 0xFFFFCCBC,
                                    0xFFD7CCC8,
                                    // 900 values from palette
                                    0xFFB71C1C, 0xFF880E4F, 0xFF4A148C, 0xFF311B92,
                                    0xFF1A237E, 0xFF0D47A1, 0xFF01579B, 0xFF006064,
                                    0xFF004D40, 0xFF1B5E20, 0xFF33691E, 0xFF827717,
                                    0xFFF57F17, 0xFFFF6F00, 0xFFE65100, 0xFFBF360C,
                                    0xFF3E2723

                            },
                            homeItem.mEffectColor,
                            4,
                            ColorPickerDialog.SIZE_SMALL);

            colorPickerDialog.setOnColorSelectedListener(this);
            colorPickerDialog.show(getFragmentManager(), "color_picker");
        }


        private void applyOperations(ArrayList<ContentProviderOperation> ops) {
            if (!ops.isEmpty()) {
                mSaveHandler.obtainMessage(0, ops).sendToTarget();
            }
        }

        @Override
        public void onColorSelected(int color) {
            int position = mSingleSelectionDecoration.mSelectedPosition;
            HomeItem item = mHomeAdapter.getItemAt(position);
            ArrayList<ContentProviderOperation> ops = new ArrayList<>();
            ops.add(ContentProviderOperation.newUpdate(mEditor.homeItemUri(item)).
                    withValue(HomeContract.Cells.EFFECT_COLOR, color).
                    build());

            applyOperations(ops);
        }
    }

}
