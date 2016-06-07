package com.appsimobile.appsii.dagger;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.appsimobile.appsii.dagger.agera.ContentProviderObservable;
import com.appsimobile.appsii.module.home.HomeItem;
import com.appsimobile.appsii.module.home.homepagesmanager.HomePageItem;
import com.appsimobile.appsii.module.home.provider.HomeContract;
import com.google.android.agera.Function;
import com.google.android.agera.Repositories;
import com.google.android.agera.Repository;
import com.google.android.agera.RepositoryConfig;
import com.google.android.agera.Result;
import com.google.android.agera.Supplier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Created by nmartens on 23/04/16.
 */

@Singleton
@Module
public final class HomeModule {

    public static final String NAME_HOME = "home";

    static final HomeItemComparator sComparator = new HomeItemComparator();

    /**
     * The default sort order for this table.
     */
    private static final String DEFAULT_SORT_ORDER = HomeContract.Cells.PAGE_ID + " ASC, " +
            HomeContract.Cells.ROW_POSITION + " ASC, " +
            HomeContract.Cells.POSITION + " ASC ";

    public static int longCompare(long lhs, long rhs) {
        return lhs < rhs ? -1 : (lhs == rhs ? 0 : 1);
    }

    public static int intCompare(int lhs, int rhs) {
        return lhs < rhs ? -1 : (lhs == rhs ? 0 : 1);
    }

    @Named(NAME_HOME)
    @Singleton
    @Provides
    Executor provideHomeExecutor() {
        return Executors.newSingleThreadExecutor();
    }

    @Named(NAME_HOME)
    @Singleton
    @Provides
    ContentProviderObservable provideContentProviderObservable(Context context) {
        return new ContentProviderObservable(context, HomeContract.Rows.CONTENT_URI,
                HomeContract.Cells.CONTENT_URI, HomeContract.Pages.CONTENT_URI);
    }

    // Replaces HomeLoader
    @Provides
    @Singleton
    final Repository<Result<List<HomeItem>>> provideHomeContentRepository(
            final Context context,
            @Named(NAME_HOME) ContentProviderObservable providerObservable,
            @Named(NAME_HOME) Executor executor
    ) {
        return Repositories.repositoryWithInitialValue(Result.<List<HomeItem>>absent())
                .observe(providerObservable)
                .onUpdatesPerLoop()
                .goTo(executor)
                .attemptGetFrom(
                        new Supplier<Result<Cursor>>() {
                            @NonNull
                            @Override
                            public Result<Cursor> get() {
                                ContentResolver resolver = context.getContentResolver();

                                Uri uri = HomeContract.Cells.CONTENT_URI;

                                Cursor cursor = resolver.query(uri,
                                        HomeQuery.PROJECTION,
                                        null,
                                        null,
                                        DEFAULT_SORT_ORDER);
                                if (cursor == null) return Result.failure();
                                return Result.success(cursor);
                            }
                        })
                .orSkip()
                .thenTransform(
                        new Function<Cursor, Result<List<HomeItem>>>() {


                            @NonNull
                            @Override
                            public Result<List<HomeItem>> apply(
                                    @NonNull Cursor c) {


                                List<HomeItem> result = new ArrayList<>(c.getCount());

                                while (c.moveToNext()) {
                                    HomeItem e = new HomeItem();

                                    e.mId = c.getLong(HomeQuery.ID);
                                    e.mPageId = c.getLong(HomeQuery.PAGE_ID);
                                    e.mRowId = c.getLong(HomeQuery.ROW_ID);
                                    e.mPageName = c.getString(HomeQuery.PAGE_NAME);
                                    e.mRowHeight = c.getInt(HomeQuery.ROW_HEIGHT);
                                    e.mRowPosition = c.getInt(HomeQuery.ROW_POSITION);
                                    e.mColspan = c.getInt(HomeQuery.COLSPAN);
                                    e.mPosition = c.getInt(HomeQuery.POSITION);
                                    e.mDisplayType = c.getInt(HomeQuery.TYPE);
                                    e.mEffectColor = c.getInt(HomeQuery.EFFECT_COLOR);

                                    result.add(e);
                                }
                                Collections.sort(result, sComparator);
                                return Result.success(result);

                            }
                        })
                .onDeactivation(RepositoryConfig.SEND_INTERRUPT)
                .compile();

    }

    // Replaces HomesLoader
    @Provides
    @Singleton
    final Repository<Result<List<HomePageItem>>> provideHomePagesRepository(
            final Context context,
            @Named(NAME_HOME) ContentProviderObservable providerObservable,
            @Named(NAME_HOME) Executor executor
    ) {
        return Repositories.repositoryWithInitialValue(Result.<List<HomePageItem>>absent())
                .observe(providerObservable)
                .onUpdatesPerLoop()
                .goTo(executor)
                .attemptGetFrom(
                        new Supplier<Result<Cursor>>() {
                            @NonNull
                            @Override
                            public Result<Cursor> get() {
                                ContentResolver resolver = context.getContentResolver();

                                Uri uri = HomeContract.Pages.CONTENT_URI;

                                Cursor cursor = resolver.query(uri,
                                        HomesQuery.PROJECTION,
                                        HomeContract.Pages.TYPE + "=?",
                                        new String[]{String.valueOf(HomeContract.Pages.PAGE_HOME)},
                                        HomeContract.Pages._ID + " ASC");
                                if (cursor == null) return Result.failure();
                                return Result.success(cursor);
                            }
                        })
                .orSkip()
                .thenTransform(
                        new Function<Cursor, Result<List<HomePageItem>>>() {


                            @NonNull
                            @Override
                            public Result<List<HomePageItem>> apply(
                                    @NonNull Cursor cursor) {

                                List<HomePageItem> result = new ArrayList<>();

                                try {
                                    while (cursor.moveToNext()) {
                                        long id = cursor.getLong(HomesQuery.ID);
                                        String name = cursor.getString(HomesQuery.DISPLAY_NAME);

                                        HomePageItem item = new HomePageItem();
                                        item.mId = id;
                                        item.mTitle = name;

                                        result.add(item);
                                    }
                                } finally {
                                    cursor.close();
                                }


                                return Result.success(result);

                            }
                        })
                .onDeactivation(RepositoryConfig.SEND_INTERRUPT)
                .compile();

    }

    /**
     * The query to execute when querying the home items for display.
     * Created by nick on 01/02/15.
     */
    static class HomesQuery {

        public static final String[] PROJECTION = {
                HomeContract.Pages._ID,
                HomeContract.Pages.DISPLAY_NAME,
        };

        public static final int ID = 0;

        public static final int DISPLAY_NAME = 1;

    }

    static class HomeQuery {

        public static final int ROW_ID = 0;

        public static final int ROW_HEIGHT = 1;

        public static final int ROW_POSITION = 2;

        public static final int PAGE_ID = 3;

        public static final int PAGE_NAME = 4;

        public static final int COLSPAN = 5;

        public static final int POSITION = 6;

        public static final int TYPE = 7;

        public static final int ID = 8;

        public static final int EFFECT_COLOR = 9;

        static final String[] PROJECTION = {
                HomeContract.Cells.ROW_ID,
                HomeContract.Cells.ROW_HEIGHT,
                HomeContract.Cells.ROW_POSITION,
                HomeContract.Cells.PAGE_ID,
                HomeContract.Cells.PAGE_NAME,
                HomeContract.Cells.COLSPAN,
                HomeContract.Cells.POSITION,
                HomeContract.Cells.TYPE,
                HomeContract.Cells._ID,
                HomeContract.Cells.EFFECT_COLOR,
        };


    }

    static class HomeItemComparator implements Comparator<HomeItem> {

        @Override
        public int compare(HomeItem lhs, HomeItem rhs) {
            int result = longCompare(lhs.mPageId, rhs.mPageId);
            if (result == 0) {
                result = intCompare(lhs.mRowPosition, rhs.mRowPosition);
            }
            if (result == 0) {
                result = intCompare(lhs.mPosition, rhs.mPosition);
            }
            return result;
        }
    }


}
