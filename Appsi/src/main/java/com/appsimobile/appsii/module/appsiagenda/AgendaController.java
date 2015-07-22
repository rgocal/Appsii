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

package com.appsimobile.appsii.module.appsiagenda;

import android.Manifest;
import android.animation.Animator;
import android.animation.LayoutTransition;
import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.appsimobile.appsii.AnalyticsManager;
import com.appsimobile.appsii.AnimatorAdapter;
import com.appsimobile.appsii.DrawableCompat;
import com.appsimobile.appsii.ExpandCollapseDrawable;
import com.appsimobile.appsii.LoaderManager;
import com.appsimobile.appsii.PageController;
import com.appsimobile.appsii.PermissionDeniedException;
import com.appsimobile.appsii.R;
import com.appsimobile.appsii.module.PermissionHelper;
import com.appsimobile.appsii.module.SpacingItemDecoration;
import com.appsimobile.appsii.module.ToolbarScrollListener;
import com.appsimobile.appsii.permissions.PermissionUtils;
import com.appsimobile.appsii.preference.PreferencesFactory;
import com.appsimobile.util.TimeUtils;
import com.crashlytics.android.Crashlytics;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by nick on 25/05/14.
 */
public class AgendaController extends PageController
        implements View.OnClickListener,
        LayoutTransition.TransitionListener, Toolbar.OnMenuItemClickListener,
        PermissionHelper.PermissionListener {

    // TODO: use View.setOnSystemUiVisibilityChangeListener(); to detect system ui-changes

    /**
     * The key in the preferences used to store the adapter type
     */
    public static final String PREF_USE_EXPANDED_ADAPTER = "agenda_use_expanded_adapter";

    /**
     * A unique loader is for the agenda events
     */
    private static final int AGENDA_LOADER_ID = 21001;

    /**
     * A unique loader id for the event-days
     */
    private static final int AGENDA_EVENT_DAYS_LOADER_ID = 21002;

    private static final int[] ATTRS = new int[]{
            R.attr.colorToolbarTitleText,
            R.attr.appsiSidebarBackground,
    };

    /**
     * The height of the header container. This is set through a dimension.
     * This dimension is used here as well to get the height when the view
     * is being initialized.
     */
    final int mHeaderWrapperHeight;

    /**
     * A formatter that can be used to format date times using DateUtils
     */
    private final Formatter mFormatter;

    /**
     * The StringBuilder used by mFormatter. Will contain the result of the
     * formatter after formatting
     */
    private final StringBuilder mStringBuilder;

    /**
     * A temporary time object used to perform time calculations with
     */
    private final Time mTime = new Time(Time.TIMEZONE_UTC);

    /**
     * Saved in the preferences, used to determine the adapter to use
     */
    boolean mUseExpandableAdapter;

    /**
     * The recycler-view containing the agenda month-browser
     */
    RecyclerView mAgendaRecycler;

    /**
     * The recycler-view containing the agenda-items
     */
    RecyclerView mHeaderView;

    /**
     * The adapter set on the AgendaListView
     */
    AgendaAdapter mAgendaAdapter;

    /**
     * A text-view that is added to the toolbar to allow the user to toggle
     * the date-picker
     */
    TextView mToolbarTitle;

    /**
     * The toolbar with the available actions and toggle for opening the month-browser
     */
    Toolbar mToolbar;

    /**
     * The drawable added to the fake title text-view that animates in and
     * out as the view is collapsed / expanded
     */
    ExpandCollapseDrawable mTitleCompoundDrawable;

    /**
     * True when the header is expanded
     */
    boolean mHeaderExpanded;

    /**
     * The complete container, wrapping the date-picker and the divider
     */
    View mHeaderContainer;

    /**
     * The controller object for the date-picker. Tracks the state and the possible start/
     * end dates for the date-picker
     */
    DatePickerControllerImpl mDatePickerController;

    final Animator.AnimatorListener mCloseListener = new AnimatorAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
            mHeaderContainer.setVisibility(View.INVISIBLE);
            mAgendaRecycler.setBackground(null);
            updateToolbarTitleAndMonthPosition();
        }
    };

    int mAppsiBackgroundColor;

    SpacingItemDecoration mSpacingDecoration;

    WeekNumberDecoration mWeekNumberDecoration;

    List<AgendaEvent> mAgendaEvents;

    OnClickListener mOnClickListener;

    int mLastVisiblePosition;

    ViewGroup mPermissionOverlay;

    boolean mPendingPermissionError;

    /**
     * The shared-preferences we can get the configuration from
     */
    private SharedPreferences mPreferences;

    private SimpleMonthAdapter2 mSimpleMonthAdapter;

    public AgendaController(Context context, String title) {
        super(context, title);
        mStringBuilder = new StringBuilder(50);
        mFormatter = new Formatter(mStringBuilder, Locale.getDefault());
        mHeaderWrapperHeight = context.getResources().
                getDimensionPixelSize(R.dimen.agenda_header_wrapper_height);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.page_agenda, container, false);
    }

    @Override
    protected void onViewDestroyed(View view) {

    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        mAgendaRecycler = (RecyclerView) view.findViewById(R.id.agenda_recycler_view);
        mAgendaRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        mAgendaRecycler.addOnItemTouchListener(new AgendaOnItemTouchListener());
        mPermissionOverlay = (ViewGroup) view.findViewById(R.id.permission_overlay);

        // setup the header view
        mHeaderView = (RecyclerView) view.findViewById(R.id.agenda_month_recycler);

        mHeaderView.setAdapter(mSimpleMonthAdapter);
        mDatePickerController.registerOnDateChangedListener(new OnDateChangedListener() {
            @Override
            public void onDateChanged(int year, int month, int day, boolean fromTap) {
                scrollAgendaViewToDate(year, month, day, fromTap);
            }
        });

        mHeaderView.setLayoutManager(new LinearLayoutManager(getContext()));

        mToolbar = (Toolbar) view.findViewById(R.id.toolbar);
        mAgendaRecycler.addOnScrollListener(new ToolbarScrollListener(this, mToolbar) {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                View child = recyclerView.getChildAt(0);
                if (child != null) {
                    int position = recyclerView.getChildLayoutPosition(child);
                    if (position != RecyclerView.NO_POSITION) {
                        syncDayToMonth(position);
                    }
                }
            }
        });
        Menu menu = mToolbar.getMenu();
        menu.clear();
        MenuInflater menuInflater = new MenuInflater(getContext());
        menuInflater.inflate(R.menu.page_agenda, menu);

        RecyclerView.ItemAnimator animator = new AgendaItemAnimator();
        mAgendaRecycler.setItemAnimator(animator);
        mAgendaRecycler.setAdapter(mAgendaAdapter);

        MenuItem menuItemAdd = menu.findItem(R.id.action_new_calendar_event);
        MenuItem menuItemViewNormal = menu.findItem(R.id.view_type_normal);
        MenuItem menuItemViewCollapse = menu.findItem(R.id.view_type_collapsable);

        menuItemAdd.setIcon(R.drawable.ic_add_black_24dp);
        menuItemViewNormal.setIcon(R.drawable.ic_view_agenda_black_24dp);
        menuItemViewCollapse.setIcon(R.drawable.ic_view_list_black_24dp);

        updateTintColors();

        mToolbar.setOnMenuItemClickListener(this);
        mToolbarTitle = (TextView) view.findViewById(R.id.toolbar_title);

        // get the toolbar text color, and make the drawable the same color
        final TypedArray a = getContext().obtainStyledAttributes(ATTRS);
        int toolbarTextColor = a.getColor(0, Color.WHITE);
        mAppsiBackgroundColor = a.getColor(1, Color.BLUE);
        a.recycle();

        mTitleCompoundDrawable = new ExpandCollapseDrawable(getResources(), toolbarTextColor);
        mTitleCompoundDrawable.setExpanded(false /* expanded */, false /* animate */);
        int dimen = (int) (getResources().getDisplayMetrics().density * 24);
        mTitleCompoundDrawable.setBounds(0, 0, dimen, dimen);

        boolean isLtr = TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()) ==
                View.LAYOUT_DIRECTION_LTR;

        if (isLtr) {
            mToolbarTitle.setCompoundDrawables(null, null, mTitleCompoundDrawable, null);
        } else {
            mToolbarTitle.setCompoundDrawables(mTitleCompoundDrawable, null, null, null);
        }

        mToolbarTitle.setOnClickListener(this);

        mHeaderContainer = view.findViewById(R.id.header_container);

        mHeaderView.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                if (newState == RecyclerView.SCROLL_STATE_IDLE) {

                    View c0 = mHeaderView.getChildAt(0);
                    View c1 = mHeaderView.getChildAt(1);
                    boolean halfWay = c1 != null && c1.getTop() < c1.getHeight() / 2;
                    boolean neededAdjustment = false;
                    if (halfWay) {
                        int scroll = c1.getTop();
                        if (scroll > 0) {
                            mHeaderView.smoothScrollBy(0, scroll);
                            neededAdjustment = true;
                        }
                    } else {
                        int top = c0.getTop();
                        if (top < 0) {
                            mHeaderView.smoothScrollBy(0, top);
                            neededAdjustment = true;
                        }
                    }
                    if (!neededAdjustment) {
                        syncMonthToDay(mHeaderView.getChildLayoutPosition(c0));
                    }
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
            }
        });
        setupDecorations();

        if (mPendingPermissionError) {
            showPermissionError();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateToolbarTitleAndMonthPosition();
    }

    @Override
    protected void onUserVisible() {
        super.onUserVisible();
        trackPageView(AnalyticsManager.CATEGORY_AGENDA);

    }

    @Override
    protected void onUserInvisible() {
        super.onUserInvisible();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPreferences = PreferencesFactory.getPreferences(getContext());
        mUseExpandableAdapter = mPreferences.getBoolean(PREF_USE_EXPANDED_ADAPTER, false);

        mAgendaAdapter = createAgendaAdapter();
        mDatePickerController = new DatePickerControllerImpl();
        int layoutDirection = TextUtils.getLayoutDirectionFromLocale(Locale.getDefault());
        mWeekNumberDecoration =
                new WeekNumberDecoration(mDatePickerController, getContext(), layoutDirection);

        mSimpleMonthAdapter = new SimpleMonthAdapter2(getContext(), mDatePickerController);


        if (layoutDirection == View.LAYOUT_DIRECTION_LTR) {
            mSpacingDecoration = new SpacingItemDecoration(getContext(), 72, 4, 8, 4);
        } else {
            mSpacingDecoration = new SpacingItemDecoration(getContext(), 8, 4, 72, 4);
        }

        mOnClickListener = new OnClickListener();

        mWeekNumberDecoration.setAgendaAdapter(mAgendaAdapter);

        mAgendaAdapter.setOnAgendaItemClickListener(mOnClickListener);

        getLoaderManager().initLoader(AGENDA_LOADER_ID, null, new AgendaEventsLoaderManager());
        getLoaderManager().initLoader(AGENDA_EVENT_DAYS_LOADER_ID, null,
                new AgendaDaysLoaderManager());
    }

    @Override
    protected void onFirstLayout() {
        super.onFirstLayout();
        scrollToToday();
    }

    void scrollToToday() {
        View view = getView();
        if (view == null) return;
        view.post(new Runnable() {
            @Override
            public void run() {
                int today = Time.getJulianDay(System.currentTimeMillis(), 0);
                int position = mAgendaAdapter.positionOfJulianDay(today);
                if (position != -1) {
                    LinearLayoutManager lm =
                            (LinearLayoutManager) mAgendaRecycler.getLayoutManager();
                    lm.scrollToPositionWithOffset(position, mToolbar.getHeight());
                    updateToolbarTitleAndMonthPosition();
                }
            }
        });
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
//        if (mAgendaRecycler != null) {
//            mAgendaRecycler.onTrimMemory(level);
//        }
    }

    @Override
    protected void applyToolbarColor(int color) {
        mToolbar.setBackgroundColor(mPrimaryColor);
    }

    AgendaAdapter<?> createAgendaAdapter() {
        if (mUseExpandableAdapter) {
            return new ExpandableAgendaAdapter(getContext());
        } else {
            return new ColoredAgendaAdapter();
        }
    }

    void scrollAgendaViewToDate(int year, int month, int day, boolean fromTap) {
        int agendaPosition = mAgendaAdapter.positionOfDate(year, month, day);

        LinearLayoutManager lm = (LinearLayoutManager) mAgendaRecycler.getLayoutManager();
        // When the day was tapped, toolbar is visible. In this case scroll the view
        // until right below the toolbar.
        if (fromTap) {
            lm.scrollToPositionWithOffset(agendaPosition, mToolbar.getHeight());
            mAgendaAdapter.focusPosition(agendaPosition);
        } else {
            lm.scrollToPositionWithOffset(agendaPosition, 0);
        }


        if (fromTap) {
            collapseHeader();
        }
    }

    void syncDayToMonth(int firstPosition) {
        if (firstPosition != mLastVisiblePosition) {
            mLastVisiblePosition = firstPosition;
            updateToolbarTitleAndMonthPosition();
        }
    }

    private void updateTintColors() {
        Menu menu = mToolbar.getMenu();
        MenuItem menuItemViewNormal = menu.findItem(R.id.view_type_normal);
        MenuItem menuItemViewCollapse = menu.findItem(R.id.view_type_collapsable);

        if (!mUseExpandableAdapter) {
            DrawableCompat.setTintColorCompat(menuItemViewNormal.getIcon(), mAccentColor);
            DrawableCompat.setTintColorCompat(menuItemViewCollapse.getIcon(), mDefaultTintColor);
        } else {
            DrawableCompat.setTintColorCompat(menuItemViewNormal.getIcon(), mDefaultTintColor);
            DrawableCompat.setTintColorCompat(menuItemViewCollapse.getIcon(), mAccentColor);
        }

    }

    /**
     * Called when the month-view was scrolled. This syncs the list to the given day.
     */
    void syncMonthToDay(int childPosition) {
        mDatePickerController.moveSelectionToChild(childPosition);
        mSimpleMonthAdapter.setSelectedDay(mDatePickerController.mSelected);
    }

    void setupDecorations() {
        mAgendaRecycler.removeItemDecoration(mSpacingDecoration);
        mAgendaRecycler.removeItemDecoration(mWeekNumberDecoration);
        if (!mUseExpandableAdapter) {
            mAgendaRecycler.addItemDecoration(mSpacingDecoration);
            mAgendaRecycler.addItemDecoration(mWeekNumberDecoration);
        }

    }

    private void showPermissionError() {
        mPendingPermissionError = false;
        PermissionHelper permissionHelper = new PermissionHelper(
                R.string.permission_reason_calendar,
                false, this, Manifest.permission.READ_CALENDAR);

        permissionHelper.show(mPermissionOverlay);
    }

    void collapseHeader() {
        mHeaderExpanded = false;
        mAgendaRecycler.animate().translationY(0).setListener(mCloseListener);
        mTitleCompoundDrawable.setExpanded(false, true);
    }

    /**
     * Updates the position in
     */
    void updateToolbarTitleAndMonthPosition() {

        // first acquire the current year, we need that for the method that
        // updates the header text
        mTime.setToNow();
        int yearNow = mTime.year;

        // Next, get the time at the first visible position
        int firstVisiblePosition = getFirstVisiblePositionInAgendaView();
        if (mAgendaAdapter.getTimeAtPosition(mTime, firstVisiblePosition)) {

            // Now get the position of that time in the month-picker view
            int positionInMonthAdapter = mDatePickerController.getPositionOfTime(mTime);

            RecyclerView.LayoutManager lm = mHeaderView.getLayoutManager();

            // first scroll to 0 to make sure we are at the top of the view when the
            // month pager is opened
            lm.scrollToPosition(positionInMonthAdapter);

            // Next update the text of the toolbar
            mToolbarTitle.setText(getMonthAndYearString(yearNow, mTime));
        }
    }

    /**
     * Returns the position in the adapter of the item that is shown at the top of the agenda
     * view.
     */
    private int getFirstVisiblePositionInAgendaView() {
        View child = mAgendaRecycler.getChildAt(0);
        int firstVisiblePosition;
        if (child != null) {
            firstVisiblePosition = mAgendaRecycler.getChildLayoutPosition(child);
        } else {
            firstVisiblePosition = 0;
        }
        return firstVisiblePosition;
    }

    private String getMonthAndYearString(int yearNow, Time time) {
        mStringBuilder.setLength(0);

        int flags;
        // When in the same year, format as: January. Else format as Jan. 2014
        if (time.year == yearNow) {
            flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NO_MONTH_DAY;
        } else {
            flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NO_MONTH_DAY |
                    DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_ABBREV_MONTH;

        }
        long millis = time.toMillis(true);

        return DateUtils.formatDateRange(getContext(), mFormatter, millis, millis, flags,
                Time.getCurrentTimezone()).toString();
    }

    boolean onCalendarEventClicked(AgendaEvent event) {
        long id = event.id;
        long startMillis = event.startMillis;
        long endMillis = event.endMillis;

        Uri eventDetailsUri =
                ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id);
        Intent intent = new Intent(Intent.ACTION_VIEW).setData(eventDetailsUri);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_NO_HISTORY
                | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        intent.putExtra("beginTime", startMillis);
        intent.putExtra("endTime", endMillis);

        // track in google analytics
        track(AnalyticsManager.ACTION_OPEN_ITEM, AnalyticsManager.CATEGORY_AGENDA);

        getContext().startActivity(intent);
        return true;
    }

    boolean onEditCalendarEventClicked(AgendaEvent event) {
        long id = event.id;
        long startMillis = event.startMillis;
        long endMillis = event.endMillis;

        Uri eventDetailsUri =
                ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id);
        Intent intent = new Intent(Intent.ACTION_EDIT).setData(eventDetailsUri);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_NO_HISTORY
                | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        intent.putExtra("beginTime", startMillis);
        intent.putExtra("endTime", endMillis);

        track(AnalyticsManager.ACTION_EDIT_ITEM, AnalyticsManager.CATEGORY_AGENDA);

        getContext().startActivity(intent);
        return true;
    }

    boolean onDeleteCalendarEventClicked(AgendaEvent event) {
        long id = event.id;
        long startMillis = event.startMillis;
        long endMillis = event.endMillis;

        Uri eventDetailsUri =
                ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id);
        Intent intent = new Intent(Intent.ACTION_DELETE).setData(eventDetailsUri);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_NO_HISTORY
                | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        intent.putExtra("beginTime", startMillis);
        intent.putExtra("endTime", endMillis);

        track(AnalyticsManager.ACTION_DELETE_ITEM, AnalyticsManager.CATEGORY_AGENDA);

        getContext().startActivity(intent);
        return true;
    }

    void onAgendaDaysResult(AgendaDaysResult data) {
        if (data.mException != null) {
            onPermissionDenied(data.mException);
        } else {
            onAgendaDaysLoaded(data.mResult);
            if (mPermissionOverlay != null) {
                mPermissionOverlay.removeAllViews();
            }
        }
    }

    private void onPermissionDenied(PermissionDeniedException permissionDeniedException) {
        if (mPermissionOverlay == null) {
            mPendingPermissionError = true;
        } else {
            showPermissionError();
        }
    }

    void onAgendaDaysLoaded(SparseBooleanArray data) {
        mDatePickerController.setEventDays(data);
        mSimpleMonthAdapter.notifyDataSetChanged();
    }

    void onAgendaEventsResult(AgendaEventsResult data) {
        if (data.mPermissionDeniedException != null) {
            onPermissionDenied(data.mPermissionDeniedException);
        } else {
            onAgendaEventsLoaded(data.mAgendaEvents);
            if (mPermissionOverlay != null) {
                mPermissionOverlay.removeAllViews();
            }
        }
    }

    void onAgendaEventsLoaded(List<AgendaEvent> data) {
        mAgendaEvents = data;
        int oldCount = mAgendaAdapter.getItemCount();
        mAgendaAdapter.setAgendaEvents(data);
        if (oldCount <= 1) {
            scrollToToday();
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.toolbar_title) {
            // handle header click
            onHeaderClicked();
        }
    }

    private void onHeaderClicked() {
        if (!mHeaderExpanded) {
            expandHeader();
        } else {
            collapseHeader();
        }
    }

    void expandHeader() {
        mHeaderExpanded = true;
        copyAgendaPositionToDatePickerController();
        mHeaderContainer.setVisibility(View.VISIBLE);
//        mHeaderContainer.animate().translationY(0).setListener(null);
        mAgendaRecycler.setBackgroundColor(mAppsiBackgroundColor);
        mAgendaRecycler.animate().translationY(mHeaderWrapperHeight).setListener(null);
        mTitleCompoundDrawable.setExpanded(true, true);
        applyToolbarColor(mPrimaryColor);
    }

    /**
     * Updates the position in
     */
    void copyAgendaPositionToDatePickerController() {

        // first acquire the current year, we need that for the method that
        // updates the header text
        mTime.setToNow();
        int yearNow = mTime.year;

        // Next, get the time at the first visible position
        int firstVisiblePosition = getFirstVisiblePositionInAgendaView();
        if (mAgendaAdapter.getTimeAtPosition(mTime, firstVisiblePosition)) {
            int positionInMonthAdapter = mDatePickerController.getPositionOfTime(mTime);

            // Now get the position of that time in the month-picker view
            mDatePickerController.onDayOfMonthSelected(mTime.year, mTime.month, mTime.monthDay,
                    false);
            mSimpleMonthAdapter.notifyItemChanged(positionInMonthAdapter);
        }
    }

    @Override
    public void startTransition(LayoutTransition transition, ViewGroup container, View view,
            int transitionType) {

        if (transitionType == LayoutTransition.CHANGE_APPEARING) {
            mAgendaRecycler.animate().translationY(0);
        }
    }

    @Override
    public void endTransition(LayoutTransition transition, ViewGroup container, View view,
            int transitionType) {

    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        switch (itemId) {
            case R.id.action_new_calendar_event:
                Intent intent = new Intent(Intent.ACTION_INSERT)
                        .setData(CalendarContract.Events.CONTENT_URI);
                try {
                    getContext().startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Crashlytics.logException(e);
                    Toast.makeText(getContext(),
                            R.string.no_compatible_calendar_app, Toast.LENGTH_SHORT).show();
                }
                track(AnalyticsManager.ACTION_CREATE_ITEM, AnalyticsManager.CATEGORY_AGENDA);
                return true;
            case R.id.action_go_to_today:
                scrollToToday();
                return true;
            case R.id.view_type_normal:
                if (mUseExpandableAdapter) {
                    changeAdapterType(false);
                }
                return true;
            case R.id.view_type_collapsable:
                if (!mUseExpandableAdapter) {
                    changeAdapterType(true);
                }
                return true;
        }
        return false;
    }

    private void changeAdapterType(boolean useExpandableAdapter) {
        mUseExpandableAdapter = useExpandableAdapter;
        mPreferences.edit().putBoolean(PREF_USE_EXPANDED_ADAPTER, useExpandableAdapter).apply();
        onAdapterTypeChanged();
    }

    void onAdapterTypeChanged() {
        mAgendaAdapter = createAgendaAdapter();
        if (mAgendaEvents != null) {
            mAgendaAdapter.setAgendaEvents(mAgendaEvents);
        }
        mAgendaRecycler.setAdapter(mAgendaAdapter);
        mAgendaAdapter.setOnAgendaItemClickListener(mOnClickListener);

        updateTintColors();

        setupDecorations();
    }

    @Override
    public void onAccepted(PermissionHelper permissionHelper) {
        Intent intent = PermissionUtils.
                buildRequestPermissionsIntent(getContext(),
                        PermissionUtils.REQUEST_CODE_PERMISSION_READ_CALENDAR,
                        Manifest.permission.READ_CALENDAR);

        getContext().startActivity(intent);
    }

    @Override
    public void onCancelled(PermissionHelper permissionHelper, boolean dontShowAgain) {
        // this option is not available so this method won't be called
    }

    //0416 566 090 9-11 14-16

    private static class DatePickerControllerImpl implements DatePickerController {

        final MonthAdapter.CalendarDay mMinDate;

        final MonthAdapter.CalendarDay mMaxDate;

        final Time mTime = new Time(Time.TIMEZONE_UTC);

        final MonthAdapter.CalendarDay mSelected;

        private final List<OnDateChangedListener> mListeners = new ArrayList<>(3);

        private SparseBooleanArray mEventDays;

        DatePickerControllerImpl() {
            mSelected = new MonthAdapter.CalendarDay(Calendar.getInstance());


            int day = TimeUtils.getJulianDay();

            Time time = new Time(Time.TIMEZONE_UTC);
            time.setJulianDay(day);
            time.year -= 1;
            time.month = 0;
            time.monthDay = 1;
            long millis = time.normalize(true);
            mMinDate = new MonthAdapter.CalendarDay(millis);

            time.year += 3;
            millis = time.normalize(true);
            mMaxDate = new MonthAdapter.CalendarDay(millis);
        }

        @Override
        public boolean hasEventOnJulianDay(int julianDay) {
            if (mEventDays == null) return false;
            return mEventDays.get(julianDay);
        }

        @Override
        public void onYearSelected(int year) {
            mSelected.setDay(year, mSelected.month, mSelected.day);
        }

        @Override
        public void onDayOfMonthSelected(int year, int month, int day, boolean fromTap) {
            mSelected.setDay(year, month, day);
            notifySelectionChanged(fromTap);
        }

        @Override
        public void registerOnDateChangedListener(OnDateChangedListener listener) {
            mListeners.add(listener);
        }

        @Override
        public void unregisterOnDateChangedListener(OnDateChangedListener listener) {
            mListeners.remove(listener);
        }

        @Override
        public MonthAdapter.CalendarDay getSelectedDay() {
            return mSelected;
        }

        @Override
        public int getFirstDayOfWeek() {
            return Calendar.MONDAY;
        }

        @Override
        public int getMinYear() {
            return 2014;
        }

        @Override
        public int getMaxYear() {
            return 2020;
        }

        @Override
        public Calendar getMinDate() {
            Calendar c = Calendar.getInstance();
            c.set(2014, Calendar.JANUARY, 1);
            return c;
        }

        @Override
        public Calendar getMaxDate() {
            Calendar c = Calendar.getInstance();
            c.set(2020, Calendar.DECEMBER, 31);
            return c;
        }

        @Override
        public void tryVibrate() {

        }

        MonthAdapter.CalendarDay ensureMaxDate() {
            return mMaxDate;
        }

        public int getPositionOfTime(Time time) {
            MonthAdapter.CalendarDay minDate = mMinDate;
            int yearsDelta = time.year - minDate.year;
            int monthsDelta = time.month - minDate.month;
            return monthsDelta + yearsDelta * 12;
        }

        public void moveSelectionToChild(int childPosition) {
            MonthAdapter.CalendarDay minDate = mMinDate;
            mTime.set(minDate.day, minDate.month + childPosition, minDate.year);
            mTime.normalize(true);
            int max = mTime.getActualMaximum(Time.MONTH_DAY);
            int day = Math.min(mSelected.day, max);

            setSelection(mTime.year, mTime.month, day);
        }

        void setSelection(int year, int month, int monthDay) {
            onDayOfMonthSelected(year, month, monthDay, false);
        }

        private void notifySelectionChanged(boolean fromTap) {
            int N = mListeners.size();
            for (int i = 0; i < N; i++) {
                OnDateChangedListener l = mListeners.get(i);
                l.onDateChanged(mSelected.year, mSelected.month, mSelected.day, fromTap);
            }
        }

        public void setEventDays(SparseBooleanArray eventDays) {
            mEventDays = eventDays;
        }
    }

    static class WeekNumberDecoration extends RecyclerView.ItemDecoration {

        private static final int[] ATTRS = new int[]{
                R.attr.colorControlNormal,
                R.attr.colorAccent,
                R.attr.colorPrimary
        };

        final DatePickerControllerImpl mController;

        final int mFirstDayOfWeek;

        final int mTopSpacing;

        final int mDaySpacing;

        final int mWeekNumberOffset;

        final int mLeftOffset;

        final Paint mWeekNumberPaint;

        final Paint mDayNamePaint;

        final Paint mDayNumberPaint;

        final Paint mDayNamePaintToday;

        final Paint mDayNumberPaintToday;

        final int mDayNumberOffset;

        final int mDayNameOffset;

        final Rect mMeasureTextRect = new Rect();

        final Time mTime = new Time(Time.TIMEZONE_UTC);

        final Calendar mCalendarInstance =
                Calendar.getInstance(TimeZone.getTimeZone(Time.TIMEZONE_UTC), Locale.getDefault());

        final Context mContext;

        final Formatter mFormatter;

        final StringBuilder mStringBuilder;

        final int mLayoutDirection;

        final NumberFormat mNumberFormat = NumberFormat.getIntegerInstance();

        AgendaAdapter<?> mAgendaAdapter;

        WeekNumberDecoration(DatePickerControllerImpl controller, Context context,
                int layoutDirection) {
            mContext = context;
            mController = controller;
            mLayoutDirection = layoutDirection;

            // decrement by 1 to convert calendar days to time days
            mFirstDayOfWeek = controller.getFirstDayOfWeek() - 1;


            float density = context.getResources().getDisplayMetrics().density;
            mTopSpacing = (int) (density * 16);
            mLeftOffset = (int) (density * 72);
            mDaySpacing = (int) (density * 20);
            mWeekNumberOffset = (int) (density * 20);
            mDayNumberOffset = (int) (density * 20);
            mDayNameOffset = (int) (density * 36);

            final TypedArray a = context.obtainStyledAttributes(ATTRS);
            int textColor = a.getColor(0, Color.BLACK);
            int accentColor = a.getColor(1, Color.BLACK);
            int primaryColor = a.getColor(2, Color.BLACK);
            a.recycle();

            mStringBuilder = new StringBuilder();
            mFormatter = new Formatter(mStringBuilder);

            mWeekNumberPaint = new Paint();
            mWeekNumberPaint.setColor(textColor);
            mWeekNumberPaint.setTextSize(12 * density);
            mWeekNumberPaint.setAntiAlias(true);

            mDayNamePaint = new Paint();
            mDayNamePaint.setColor(textColor);
            mDayNamePaint.setTextSize(14 * density);
            mDayNamePaint.setAntiAlias(true);

            mDayNumberPaint = new Paint();
            mDayNumberPaint.setColor(textColor);
            mDayNumberPaint.setTextSize(28 * density);
            mDayNumberPaint.setAntiAlias(true);

            mDayNamePaintToday = new Paint();
            mDayNamePaintToday.setColor(accentColor);
            mDayNamePaintToday.setTextSize(14 * density);
            mDayNamePaintToday.setAntiAlias(true);

            mDayNumberPaintToday = new Paint();
            mDayNumberPaintToday.setColor(accentColor);
            mDayNumberPaintToday.setTextSize(28 * density);
            mDayNumberPaintToday.setAntiAlias(true);
        }

        public void setAgendaAdapter(AgendaAdapter<?> agendaAdapter) {
            mAgendaAdapter = agendaAdapter;
        }

        @Override
        public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
            int childCount = parent.getChildCount();
            int today = TimeUtils.getJulianDay();

            int parentWidth = parent.getWidth();

            for (int i = 0; i < childCount; i++) {
                View view = parent.getChildAt(i);
                int position = parent.getChildLayoutPosition(view);

                int top = view.getTop();
                if (mAgendaAdapter != null && mAgendaAdapter.hasWeekNumberAtPosition(position)) {

                    int startDay = mAgendaAdapter.getJulianDayAtPosition(position);
                    long startMillis = mTime.setJulianDay(startDay);
                    int daysAfterFirstDayOfWeek = mTime.weekDay - mFirstDayOfWeek;
                    if (daysAfterFirstDayOfWeek < 0) daysAfterFirstDayOfWeek += 7;

                    startMillis -= daysAfterFirstDayOfWeek * DateUtils.DAY_IN_MILLIS;

                    long endMillis = startMillis + DateUtils.DAY_IN_MILLIS * 7;

                    mStringBuilder.setLength(0);
                    DateUtils.formatDateRange(mContext, mFormatter, startMillis, endMillis,
                            DateUtils.FORMAT_SHOW_DATE, Time.TIMEZONE_UTC);
                    String weekText = mStringBuilder.toString();

                    int offset;
                    if (mLayoutDirection == View.LAYOUT_DIRECTION_LTR) {
                        offset = mLeftOffset;
                    } else {
                        mWeekNumberPaint.getTextBounds(
                                weekText, 0, weekText.length(), mMeasureTextRect);

                        offset = parentWidth - mMeasureTextRect.width() - mLeftOffset;
                    }

                    c.drawText(weekText, offset,
                            top - mWeekNumberOffset, mWeekNumberPaint);

                }
                if (needsDayNumber(position)) {
                    int day = mAgendaAdapter.getJulianDayAtPosition(position);
                    drawDayNumber(c, position, top, day == today, parentWidth);
                }
            }
        }

        boolean needsDayNumber(int position) {
            if (position == 0) return false;
            int julianDay = mAgendaAdapter.getJulianDayAtPosition(position);
            int prevPositionDay = mAgendaAdapter.getJulianDayAtPosition(position - 1);
            return prevPositionDay != julianDay;
        }

        private void drawDayNumber(Canvas c, int position, int top, boolean today,
                int parentWidth) {
            int julianDay = mAgendaAdapter.getJulianDayAtPosition(position);
            long millis = mTime.setJulianDay(julianDay);
            mCalendarInstance.setTimeInMillis(millis);

            String displayDayOfWeek = mCalendarInstance.getDisplayName(
                    Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault());

            int monthDay = mTime.monthDay;

            final int dayNameOffset;
            final int dayNumberOffset;

            String dayNumber = mNumberFormat.format(monthDay);

            if (mLayoutDirection == View.LAYOUT_DIRECTION_LTR) {
                dayNameOffset = mWeekNumberOffset;
                dayNumberOffset = mDayNumberOffset;
            } else {
                mDayNamePaint.getTextBounds(
                        displayDayOfWeek, 0, displayDayOfWeek.length(), mMeasureTextRect);
                dayNameOffset = parentWidth - mMeasureTextRect.width() - mWeekNumberOffset;

                mDayNumberPaint.getTextBounds(
                        dayNumber, 0, dayNumber.length(), mMeasureTextRect);
                dayNumberOffset = parentWidth - mMeasureTextRect.width() - mWeekNumberOffset;
            }

            c.drawText(displayDayOfWeek, dayNameOffset,
                    top + mDayNameOffset, today ? mDayNamePaintToday : mDayNamePaint);

            c.drawText(dayNumber, dayNumberOffset,
                    top + mDayNumberOffset, today ? mDayNumberPaintToday : mDayNumberPaint);
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                RecyclerView.State state) {
            int position = parent.getChildLayoutPosition(view);

            // see if this is a new day in the adapter. If so, add extra padding
            int extraOffsetForNewDay;
            if (position != 0) {
                int julianDay = mAgendaAdapter.getJulianDayAtPosition(position);
                int prevPositionDay = mAgendaAdapter.getJulianDayAtPosition(position - 1);
                boolean differentDay = prevPositionDay != julianDay;
                extraOffsetForNewDay = differentDay ? mDaySpacing : 0;
            } else {
                extraOffsetForNewDay = 0;
            }

            if (mAgendaAdapter != null && mAgendaAdapter.hasWeekNumberAtPosition(position)) {
                outRect.set(0, mTopSpacing + extraOffsetForNewDay, 0, 0);
            } else {
                outRect.set(0, extraOffsetForNewDay, 0, 0);
            }
        }
    }

    class AgendaEventsLoaderManager implements LoaderManager.LoaderCallbacks<AgendaEventsResult> {

        @Override
        public Loader<AgendaEventsResult> onCreateLoader(int id, Bundle args) {
            return new AgendaLoader(getContext(), mDatePickerController);
        }

        @Override
        public void onLoadFinished(Loader<AgendaEventsResult> loader, AgendaEventsResult data) {
            onAgendaEventsResult(data);
        }

        @Override
        public void onLoaderReset(Loader<AgendaEventsResult> loader) {
            // This is called when the last Cursor provided to onLoadFinished()
            // above is about to be closed.  We need to make sure we are no
            // longer using it.
            mAgendaAdapter.clear();
        }


    }

    class AgendaDaysLoaderManager implements LoaderManager.LoaderCallbacks<AgendaDaysResult> {

        @Override
        public Loader<AgendaDaysResult> onCreateLoader(int id, Bundle args) {
            return new AgendaDaysLoader(getContext(), mDatePickerController);
        }

        @Override
        public void onLoadFinished(Loader<AgendaDaysResult> loader, AgendaDaysResult data) {
            onAgendaDaysResult(data);
        }

        @Override
        public void onLoaderReset(Loader<AgendaDaysResult> loader) {
            // This is called when the last Cursor provided to onLoadFinished()
            // above is about to be closed.  We need to make sure we are no
            // longer using it.
        }

    }

    class OnClickListener implements ItemAgendaViewHolder.OnItemClickListener {

        @Override
        public void onItemClicked(AgendaEvent event) {
            onCalendarEventClicked(event);
        }

        @Override
        public void onItemEditClicked(AgendaEvent event) {
            onEditCalendarEventClicked(event);
        }

        @Override
        public void onItemDeleteClicked(AgendaEvent event) {
            onDeleteCalendarEventClicked(event);
        }
    }

    class AgendaOnItemTouchListener implements RecyclerView.OnItemTouchListener {

        float mInitialTouchX;

        float mInitialTouchY;

        float mLastSetValue;

        @Override
        public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
            mInitialTouchX = e.getRawX();
            mInitialTouchY = e.getRawY();
            mLastSetValue = 0;
            return mHeaderExpanded;
        }

        @Override
        public void onTouchEvent(RecyclerView rv, MotionEvent e) {
            if (mHeaderExpanded) {
                int action = e.getAction();
                if (action == MotionEvent.ACTION_MOVE) {
                    int deltaY = (int) (mInitialTouchY - e.getRawY());
                    if (deltaY <= 0) {
                        mAgendaRecycler.setTranslationY(mHeaderWrapperHeight);
                        mLastSetValue = 0;
                    } else if (deltaY > mHeaderWrapperHeight) {
                        mAgendaRecycler.setTranslationY(0);
                        mLastSetValue = deltaY;
                    } else {
                        mAgendaRecycler.setTranslationY(mHeaderWrapperHeight - deltaY);
                        mLastSetValue = deltaY;
                    }
                } else if (action == MotionEvent.ACTION_UP ||
                        action == MotionEvent.ACTION_CANCEL) {
                    int h = mHeaderWrapperHeight;
                    if (mLastSetValue < h / 4) {
                        expandHeader();
                        mToolbar.animate().translationY(0);
                    } else {
                        collapseHeader();
//                            hideToolbar();
                    }
                    mLastSetValue = 0;
                }
            }
        }

        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

        }
    }
}