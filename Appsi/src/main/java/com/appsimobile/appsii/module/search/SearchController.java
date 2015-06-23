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

package com.appsimobile.appsii.module.search;

import android.content.ComponentName;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.appsimobile.appsii.AnalyticsManager;
import com.appsimobile.appsii.BuildConfig;
import com.appsimobile.appsii.LoaderManager;
import com.appsimobile.appsii.PageController;
import com.appsimobile.appsii.R;
import com.appsimobile.appsii.compat.LauncherAppsCompat;
import com.appsimobile.appsii.compat.UserHandleCompat;
import com.appsimobile.appsii.module.BaseContactInfo;
import com.appsimobile.appsii.module.PeopleQuery;
import com.appsimobile.appsii.module.apps.AppEntry;
import com.appsimobile.appsii.module.apps.AppHistoryUtils;
import com.appsimobile.appsii.module.people.ContactView;
import com.appsimobile.appsii.preference.PreferenceHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nick on 25/05/14.
 */
public class SearchController extends PageController
        implements AdapterView.OnItemClickListener, OnAppClickedListener, OnPersonClickedListener,
        ContactView.PeopleActionListener {

    static final int APPS_SEARCH_LOADER = 334001;

    static final int PEOPLE_SEARCH_LOADER = 334002;

    static final int RECENT_SEARCHES_LOADER = 334003;

    /**
     * The recycler-view containing the agenda-items
     */
    RecyclerView mRecyclerView;

    /**
     * The view where the user can enter the search query
     */
    EditText mSearchView;

    /**
     * The speech recognized used for speech search
     */
    SpeechRecognizer mSpeechRecognizer;

    /**
     * The query the user entered
     */
    String mQuery;

    /**
     * The search adapter. This shows the search results
     */
    SearchAdapter mSearchAdapter;

    /**
     * Loader callbacks for the people loader
     */
    PeopleCallbacks mPeopleCallbacks;

    /**
     * Loader callbacks for the apps loader
     */
    AppsCallbacks mAppsCallbacks;

    /**
     * Loader callbacks for the recent searches loader
     */
    RecentSearchedCallbacks mRecentSearchedCallbacks;

    /**
     * The shared-preferences we can get the configuration from
     */
    SharedPreferences mPreferences;

    RecyclerView mSearchRecyclerView;

    View mSearchDivider;

    GridLayoutManager mGridLayoutManager;

    SearchSuggestionsAdapter mSearchSuggestionsAdapter;

    View mSuggestionsAnchor;

    public SearchController(Context context, String title) {
        super(context, title);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.page_search, container, false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSpeechRecognizer.destroy();
    }

    @Override
    protected void onViewDestroyed(View view) {

    }

//    @Override
//    public int getFlags() {
//        return AbstractSidebarPagerAdapter.FLAG_NO_DECORATIONS;
//    }

    @Override
    protected void onViewCreated(View view, Bundle savedInstanceState) {
        mRecyclerView = (RecyclerView) view.findViewById(R.id.recycler);
        mSearchView = (EditText) view.findViewById(R.id.search_view);
        mSuggestionsAnchor = view.findViewById(R.id.search_wrapper);
        mSearchRecyclerView = (RecyclerView) view.findViewById(R.id.search_recycler);
        mSearchDivider = view.findViewById(R.id.search_divider);

        mSearchRecyclerView.setLayoutManager(new WrapContentLinearLayoutManager(getContext()));
        mSearchRecyclerView.setAdapter(mSearchSuggestionsAdapter);

        mRecyclerView.setLayoutManager(new WrapContentLinearLayoutManager(getContext()));
        mRecyclerView.setAdapter(mSearchAdapter);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
//        mRecyclerView.setOnScrollListener(new ToolbarScrollListener(this, mToolbar));

        Resources res = getResources();
        float density = res.getDisplayMetrics().density;

        // calculate the width of the sidebar
        int contentWidth = getContentWidth();
        if (contentWidth == 0) {
            PreferenceHelper preferenceHelper = PreferenceHelper.getInstance(getContext());
            float pct = preferenceHelper.getSidebarWidth() / 100f;
            contentWidth = (int) (res.getConfiguration().smallestScreenWidthDp * density * pct);
        }

        int dp104 = (int) (density * 104);
        int cellCount = contentWidth / dp104;
        if (cellCount < 3) cellCount = 3;
        if (cellCount > 3) cellCount--;

        mGridLayoutManager = new GridLayoutManager(getContext(), cellCount);
        mSearchAdapter.setSpanCount(cellCount);
        mRecyclerView.setLayoutManager(mGridLayoutManager);
        mGridLayoutManager.setSpanSizeLookup(mSearchAdapter.mSpanSizeLookup);

        mSearchView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                onSearchTextChanged(s);
            }
        });
        mSearchView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                onSearchFocusChanged(hasFocus);
            }
        });

        mSearchSuggestionsAdapter.setOnItemClickListener(this);

        Button speechButton;
        /*
        speechButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
//                intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,5);
                mSpeechRecognizer.startListening(intent);
                Log.i("111111", "11111111");
            }
        });
        */
    }

    void onSearchTextChanged(CharSequence charSequence) {
        mQuery = String.valueOf(charSequence);
        if (mQuery.length() > 0) {
            getLoaderManager().restartLoader(PEOPLE_SEARCH_LOADER, null, mPeopleCallbacks);
            getLoaderManager().restartLoader(APPS_SEARCH_LOADER, null, mAppsCallbacks);
            hideSearchSuggestions();
        } else {
            mQuery = null;
            mSearchAdapter.clear();
            showSearchSuggestions();
            getLoaderManager().destroyLoader(PEOPLE_SEARCH_LOADER);
            getLoaderManager().destroyLoader(APPS_SEARCH_LOADER);
        }
    }

    void onSearchFocusChanged(boolean hasFocus) {
        if (hasFocus) {
            showSearchSuggestions();
        }
    }

    private void hideSearchSuggestions() {
        mSearchRecyclerView.setVisibility(View.GONE);
        mSearchDivider.setVisibility(View.GONE);
    }

    private void showSearchSuggestions() {
        mSearchRecyclerView.setVisibility(View.VISIBLE);
        mSearchDivider.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onUserVisible() {
        super.onUserVisible();
        trackPageView(AnalyticsManager.CATEGORY_SEARCH);
        focusSearchView();
    }

    @Override
    protected void onUserInvisible() {
        super.onUserInvisible();
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(
                Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mSearchView.getWindowToken(), 0);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(getContext());
        mSpeechRecognizer.setRecognitionListener(new RecognitionListenerImpl());

        mSearchAdapter = new SearchAdapter(this, this, this);

        mPeopleCallbacks = new PeopleCallbacks();
        mAppsCallbacks = new AppsCallbacks();
        mRecentSearchedCallbacks = new RecentSearchedCallbacks();

        mSearchSuggestionsAdapter = new SearchSuggestionsAdapter();

        getLoaderManager().initLoader(RECENT_SEARCHES_LOADER, null, mRecentSearchedCallbacks);

    }

    @Override
    protected void applyToolbarColor(int color) {

    }

    public void setDeferLoads(boolean deferLoads) {
        super.setDeferLoads(deferLoads);
        if (!deferLoads) {
            focusSearchView();
        }
    }

    void focusSearchView() {
        if (mSearchView != null && isUserVisible()) {
            mSearchView.requestFocus();
            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(mSearchView, InputMethodManager.SHOW_IMPLICIT);

        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        hideSearchSuggestions();
        SearchSuggestion searchSuggestion = mSearchSuggestionsAdapter.getItem(position);
        SearchSuggestionUtils.getInstance(getContext()).saveQuery(searchSuggestion.query);
        mSearchView.setText(searchSuggestion.query);
    }

    @Override
    public void onAppClicked(AppEntry app) {
        LauncherAppsCompat launcherApps = LauncherAppsCompat.getInstance(getContext());
        launcherApps.startActivityForProfile(app.getComponentName(),
                UserHandleCompat.myUserHandle(),
                null,
                null);
        // track a launch of the app in the app history
        ComponentName componentName = app.getComponentName();
        AppHistoryUtils.trackAppLaunch(getContext(), componentName);

        // track in google analytics
        track(AnalyticsManager.ACTION_OPEN_ITEM, AnalyticsManager.CATEGORY_APPS,
                app.getComponentName().flattenToShortString());

        SearchSuggestionUtils.getInstance(getContext()).saveQuery(mQuery);
    }

    @Override
    public void onPersonClicked(BaseContactInfo entry) {
        Uri uri = entry.mContactLookupUri;
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        track(AnalyticsManager.ACTION_OPEN_ITEM, AnalyticsManager.CATEGORY_SEARCH);
        getContext().startActivity(intent);
        SearchSuggestionUtils.getInstance(getContext()).saveQuery(mQuery);
    }

    @Override
    public void onEditSelected(BaseContactInfo entry) {
        Uri uri = entry.mContactLookupUri;
        Intent intent = new Intent(Intent.ACTION_EDIT, uri);
        track(AnalyticsManager.ACTION_EDIT_ITEM, AnalyticsManager.CATEGORY_SEARCH);
        getContext().startActivity(intent);
        SearchSuggestionUtils.getInstance(getContext()).saveQuery(mQuery);
    }

    private static class RecognitionListenerImpl implements RecognitionListener {

        RecognitionListenerImpl() {
        }

        @Override
        public void onReadyForSpeech(Bundle params) {

        }

        @Override
        public void onBeginningOfSpeech() {

        }

        @Override
        public void onRmsChanged(float rmsdB) {

        }

        @Override
        public void onBufferReceived(byte[] buffer) {

        }

        @Override
        public void onEndOfSpeech() {

        }

        @Override
        public void onError(int error) {

        }

        @Override
        public void onResults(Bundle results) {
            // implement result here
            List<String> voice = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        }

        @Override
        public void onPartialResults(Bundle partialResults) {
            // implement partial results here
        }

        @Override
        public void onEvent(int eventType, Bundle params) {

        }
    }

    static class SearchSuggestionViewHolder extends RecyclerView.ViewHolder {

        final TextView mTitleView;

        final AdapterView.OnItemClickListener mOnItemClickListener;

        public SearchSuggestionViewHolder(View itemView, final AdapterView.OnItemClickListener l) {
            super(itemView);
            mOnItemClickListener = l;
            mTitleView = (TextView) itemView.findViewById(R.id.recent_item_title);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mOnItemClickListener.onItemClick(null, null, getPosition(), getItemId());
                }
            });
        }

        public void bind(SearchSuggestion item) {
            mTitleView.setText(item.query);
        }
    }

    static class SearchSuggestionsAdapter extends RecyclerView.Adapter<SearchSuggestionViewHolder> {

        final List<SearchSuggestion> mSearchSuggestions = new ArrayList<>();

        AdapterView.OnItemClickListener mOnItemClickListener;

        SearchSuggestionsAdapter() {
            setHasStableIds(true);
        }

        @Override
        public SearchSuggestionViewHolder onCreateViewHolder(ViewGroup parent,
                int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            View view = inflater.inflate(R.layout.list_item_recent_search, parent, false);
            return new SearchSuggestionViewHolder(view, mOnItemClickListener);
        }

        @Override
        public void onBindViewHolder(SearchSuggestionViewHolder holder, int position) {
            SearchSuggestion item = getItem(position);
            holder.bind(item);
        }

        public SearchSuggestion getItem(int position) {
            return mSearchSuggestions.get(position);
        }

        @Override
        public long getItemId(int position) {
            return mSearchSuggestions.get(position).id;
        }

        @Override
        public int getItemCount() {
            return mSearchSuggestions.size();
        }

        public void setItems(List<SearchSuggestion> suggestions) {
            mSearchSuggestions.clear();
            mSearchSuggestions.addAll(suggestions);
            notifyDataSetChanged();
        }

        public void setOnItemClickListener(AdapterView.OnItemClickListener listener) {
            mOnItemClickListener = listener;
        }
    }

    /**
     * {@link android.support.v7.widget.LinearLayoutManager} which wraps its content. Note that
     * this class will always
     * wrap the content regardless of {@link android.support.v7.widget.RecyclerView} layout
     * parameters.
     * <p/>
     * Now it's impossible to run add/remove animations with child views which have arbitrary
     * dimensions (height for
     * VERTICAL orientation and width for HORIZONTAL). However if child views have fixed dimensions
     * {@link #setChildSize(int)} method might be used to let the layout manager know how big
     * they are going to be.
     * If animations are not used at all then a normal measuring procedure will run and child
     * views will be measured during
     * the measure pass.
     */
    public static class WrapContentLinearLayoutManager
            extends android.support.v7.widget.LinearLayoutManager {

        private static final int CHILD_WIDTH = 0;

        private static final int CHILD_HEIGHT = 1;

        private static final int DEFAULT_CHILD_SIZE = 100;

        private final int[] childDimensions = new int[2];

        private final RecyclerView view;

        private int childSize = DEFAULT_CHILD_SIZE;

        private boolean hasChildSize;

        private int overScrollMode = ViewCompat.OVER_SCROLL_ALWAYS;

        @SuppressWarnings("UnusedDeclaration")
        public WrapContentLinearLayoutManager(Context context) {
            super(context);
            this.view = null;
        }

        @SuppressWarnings("UnusedDeclaration")
        public WrapContentLinearLayoutManager(Context context, int orientation,
                boolean reverseLayout) {
            super(context, orientation, reverseLayout);
            this.view = null;
        }

        @SuppressWarnings("UnusedDeclaration")
        public WrapContentLinearLayoutManager(RecyclerView view) {
            super(view.getContext());
            this.view = view;
            this.overScrollMode = ViewCompat.getOverScrollMode(view);
        }

        @SuppressWarnings("UnusedDeclaration")
        public WrapContentLinearLayoutManager(RecyclerView view, int orientation,
                boolean reverseLayout) {
            super(view.getContext(), orientation, reverseLayout);
            this.view = view;
            this.overScrollMode = ViewCompat.getOverScrollMode(view);
        }

        public void setOverScrollMode(int overScrollMode) {
            if (overScrollMode < ViewCompat.OVER_SCROLL_ALWAYS ||
                    overScrollMode > ViewCompat.OVER_SCROLL_NEVER) {
                throw new IllegalArgumentException("Unknown overscroll mode: " + overScrollMode);
            }
            if (this.view == null) throw new IllegalStateException("view == null");
            this.overScrollMode = overScrollMode;
            ViewCompat.setOverScrollMode(view, overScrollMode);
        }

        @Override
        public void onMeasure(RecyclerView.Recycler recycler, RecyclerView.State state,
                int widthSpec, int heightSpec) {
            final int widthMode = View.MeasureSpec.getMode(widthSpec);
            final int heightMode = View.MeasureSpec.getMode(heightSpec);

            final int widthSize = View.MeasureSpec.getSize(widthSpec);
            final int heightSize = View.MeasureSpec.getSize(heightSpec);

            final boolean exactWidth = widthMode == View.MeasureSpec.EXACTLY;
            final boolean exactHeight = heightMode == View.MeasureSpec.EXACTLY;

            final int unspecified = makeUnspecifiedSpec();

            if (exactWidth && exactHeight) {
                // in case of exact calculations for both dimensions let's use default
                // "onMeasure" implementation
                super.onMeasure(recycler, state, widthSpec, heightSpec);
                return;
            }

            final boolean vertical = getOrientation() == VERTICAL;

            initChildDimensions(widthSize, heightSize, vertical);

            int width = 0;
            int height = 0;

            // it's possible to get scrap views in recycler which are bound to old (invalid)
            // adapter entities. This
            // happens because their invalidation happens after "onMeasure" method. As a
            // workaround let's clear the
            // recycler now (it should not cause any performance issues while scrolling as
            // "onMeasure" is never
            // called whiles scrolling)
            recycler.clear();

            final int stateItemCount = state.getItemCount();
            final int adapterItemCount = getItemCount();
            // adapter always contains actual data while state might contain old data (f.e. data
            // before the animation is
            // done). As we want to measure the view with actual data we must use data from the
            // adapter and not from  the
            // state
            for (int i = 0; i < adapterItemCount; i++) {
                if (vertical) {
                    if (!hasChildSize) {
                        if (i < stateItemCount) {
                            // we should not exceed state count, otherwise we'll get
                            // IndexOutOfBoundsException. For such items
                            // we will use previously calculated dimensions
                            measureChild(recycler, i, widthSpec, unspecified, childDimensions);
                        } else {
                            logMeasureWarning(i);
                        }
                    }
                    height += childDimensions[CHILD_HEIGHT];
                    if (i == 0) {
                        width = childDimensions[CHILD_WIDTH];
                    }
                    if (height >= heightSize) {
                        break;
                    }
                } else {
                    if (!hasChildSize) {
                        if (i < stateItemCount) {
                            // we should not exceed state count, otherwise we'll get
                            // IndexOutOfBoundsException. For such items
                            // we will use previously calculated dimensions
                            measureChild(recycler, i, unspecified, heightSpec, childDimensions);
                        } else {
                            logMeasureWarning(i);
                        }
                    }
                    width += childDimensions[CHILD_WIDTH];
                    if (i == 0) {
                        height = childDimensions[CHILD_HEIGHT];
                    }
                    if (width >= widthSize) {
                        break;
                    }
                }
            }

            final boolean fit =
                    (vertical && height < heightSize) || (!vertical && width < widthSize);
            if (fit) {
                // we really should wrap the contents of the view, let's do it

                if (exactWidth) {
                    width = widthSize;
                } else {
                    width += getPaddingLeft() + getPaddingRight();
                }

                if (exactHeight) {
                    height = heightSize;
                } else {
                    height += getPaddingTop() + getPaddingBottom();
                }

                setMeasuredDimension(width, height);
            } else {
                // if calculated height/width exceeds requested height/width let's use default
                // "onMeasure" implementation
                super.onMeasure(recycler, state, widthSpec, heightSpec);
            }

            if (view != null && overScrollMode == ViewCompat.OVER_SCROLL_IF_CONTENT_SCROLLS) {
                ViewCompat.setOverScrollMode(view,
                        fit ? ViewCompat.OVER_SCROLL_NEVER : ViewCompat.OVER_SCROLL_ALWAYS);
            }
        }

        public static int makeUnspecifiedSpec() {
            return View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        }

        private void initChildDimensions(int width, int height, boolean vertical) {
            if (childDimensions[CHILD_WIDTH] != 0 || childDimensions[CHILD_HEIGHT] != 0) {
                // already initialized, skipping
                return;
            }
            if (vertical) {
                childDimensions[CHILD_WIDTH] = width;
                childDimensions[CHILD_HEIGHT] = childSize;
            } else {
                childDimensions[CHILD_WIDTH] = childSize;
                childDimensions[CHILD_HEIGHT] = height;
            }
        }

        private void measureChild(RecyclerView.Recycler recycler, int position, int widthSpec,
                int heightSpec, int[] dimensions) {
            final View child = recycler.getViewForPosition(position);

            final RecyclerView.LayoutParams p = (RecyclerView.LayoutParams) child.getLayoutParams();

            final int hPadding = getPaddingLeft() + getPaddingRight();
            final int vPadding = getPaddingTop() + getPaddingBottom();

            final int hMargin = p.leftMargin + p.rightMargin;
            final int vMargin = p.topMargin + p.bottomMargin;

            final int hDecoration = getRightDecorationWidth(child) + getLeftDecorationWidth(child);
            final int vDecoration =
                    getTopDecorationHeight(child) + getBottomDecorationHeight(child);

            final int childWidthSpec =
                    getChildMeasureSpec(widthSpec, hPadding + hMargin + hDecoration, p.width,
                            canScrollHorizontally());
            final int childHeightSpec =
                    getChildMeasureSpec(heightSpec, vPadding + vMargin + vDecoration, p.height,
                            canScrollVertically());

            child.measure(childWidthSpec, childHeightSpec);

            dimensions[CHILD_WIDTH] =
                    getDecoratedMeasuredWidth(child) + p.leftMargin + p.rightMargin;
            dimensions[CHILD_HEIGHT] =
                    getDecoratedMeasuredHeight(child) + p.bottomMargin + p.topMargin;

            recycler.recycleView(child);
        }

        private void logMeasureWarning(int child) {
            if (BuildConfig.DEBUG) {
                Log.w("LinearLayoutManager", "Can't measure child #" + child +
                        ", previously used dimensions will be reused." +
                        "To remove this message either use #setChildSize() method or don't run " +
                        "RecyclerView animations");
            }
        }

        @Override
        public void setOrientation(int orientation) {
            // might be called before the constructor of this class is called
            //noinspection ConstantConditions
            if (childDimensions != null) {
                if (getOrientation() != orientation) {
                    childDimensions[CHILD_WIDTH] = 0;
                    childDimensions[CHILD_HEIGHT] = 0;
                }
            }
            super.setOrientation(orientation);
        }

        public void clearChildSize() {
            hasChildSize = false;
            setChildSize(DEFAULT_CHILD_SIZE);
        }

        public void setChildSize(int childSize) {
            hasChildSize = true;
            if (this.childSize != childSize) {
                this.childSize = childSize;
                requestLayout();
            }
        }
    }

    class PeopleCallbacks implements LoaderManager.LoaderCallbacks<Cursor> {

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            Uri baseUri;
            if (mQuery != null) {
                baseUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_FILTER_URI,
                        Uri.encode(mQuery));
            } else {
                baseUri = ContactsContract.Contacts.CONTENT_URI.buildUpon()
                        .appendQueryParameter(ContactsContract.DIRECTORY_PARAM_KEY,
                                String.valueOf(ContactsContract.Directory.DEFAULT)).build();
            }


            // Now create and return a CursorLoader that will take care of
            // creating a Cursor for the data being displayed.
            String select = "((" + ContactsContract.Contacts.DISPLAY_NAME + " NOTNULL) AND ("
                    + ContactsContract.Contacts.DISPLAY_NAME + " != '' ))";
            return new CursorLoader(getContext(), baseUri,
                    PeopleQuery.CONTACTS_SUMMARY_PROJECTION, select, null,
                    ContactsContract.Contacts.DISPLAY_NAME + " COLLATE LOCALIZED ASC");
        }

        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            List<? extends BaseContactInfo> contactInfos = PeopleQuery.cursorToContactInfos(data);
            mSearchAdapter.setPeopleInfos(contactInfos);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {

        }
    }

    class AppsCallbacks implements LoaderManager.LoaderCallbacks<List<AppEntry>> {

        @Override
        public Loader<List<AppEntry>> onCreateLoader(int id, Bundle args) {
            return new AppSearchLoader(getContext(), mQuery);

        }

        public void onLoadFinished(Loader<List<AppEntry>> loader, List<AppEntry> apps) {
            mSearchAdapter.setApps(apps);
        }

        @Override
        public void onLoaderReset(Loader<List<AppEntry>> loader) {

        }
    }

    class RecentSearchedCallbacks implements LoaderManager.LoaderCallbacks<List<SearchSuggestion>> {

        @Override
        public Loader<List<SearchSuggestion>> onCreateLoader(int id, Bundle args) {
            return new RecentSearchLoader(getContext());

        }

        public void onLoadFinished(Loader<List<SearchSuggestion>> loader,
                List<SearchSuggestion> suggestions) {
            mSearchSuggestionsAdapter.setItems(suggestions);
        }

        @Override
        public void onLoaderReset(Loader<List<SearchSuggestion>> loader) {

        }
    }
}