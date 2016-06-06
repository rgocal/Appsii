package com.appsimobile.appsii.dagger;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.util.ArrayMap;
import android.support.v4.util.SimpleArrayMap;
import android.util.LongSparseArray;

import com.appsimobile.appsii.AppsiApplication;
import com.appsimobile.appsii.compat.LauncherActivityInfoCompat;
import com.appsimobile.appsii.compat.LauncherAppsCompat;
import com.appsimobile.appsii.compat.UserHandleCompat;
import com.appsimobile.appsii.dagger.agera.BroadcastObservable;
import com.appsimobile.appsii.dagger.agera.ContentProviderObservable;
import com.appsimobile.appsii.module.apps.AppEntry;
import com.appsimobile.appsii.module.apps.AppHistoryQuery;
import com.appsimobile.appsii.module.apps.AppPageData;
import com.appsimobile.appsii.module.apps.AppQuery;
import com.appsimobile.appsii.module.apps.AppTag;
import com.appsimobile.appsii.module.apps.AppTagQuery;
import com.appsimobile.appsii.module.apps.AppsContract;
import com.appsimobile.appsii.module.apps.AppsContract.LaunchHistoryColumns;
import com.appsimobile.appsii.module.apps.HistoryItem;
import com.appsimobile.appsii.module.apps.ResolveInfoAppEntry;
import com.appsimobile.appsii.module.apps.ShortcutNameComparator;
import com.appsimobile.appsii.module.apps.TaggedApp;
import com.google.android.agera.Function;
import com.google.android.agera.Merger;
import com.google.android.agera.Repositories;
import com.google.android.agera.Repository;
import com.google.android.agera.RepositoryConfig;
import com.google.android.agera.Result;
import com.google.android.agera.Supplier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Created by nmartens on 23/04/16.
 */
@Singleton
@Module
public class AppsModule {

    public static final String NAME_APPS = "apps";

    public static final String NAME_APPS_HISTORY = "apps_history";

    public static final String NAME_APPS_TAGS = "apps_tags";

    /**
     * Helper method to add items to a list in a long sparse array that maps keys to lists.
     */
    static <V> void addItemToLongSparseArray(LongSparseArray<List<V>> map, long key, V item) {
        List<V> list = map.get(key);
        if (list == null) {
            list = new ArrayList<>();
            map.put(key, list);
        }
        list.add(item);
    }

    /**
     * Helper method to add items to a list in a map that maps keys to lists.
     */
    static <K, V> void addItemToMapList(SimpleArrayMap<K, List<V>> map, K key, V item) {
        List<V> list = map.get(key);
        if (list == null) {
            list = new ArrayList<>();
            map.put(key, list);
        }
        list.add(item);
    }

    @Named(NAME_APPS)
    @Singleton
    @Provides
    Executor provideAppsExecutor() {
        return Executors.newSingleThreadExecutor();
    }

    @Named(NAME_APPS)
    @Provides
    @Singleton
    final BroadcastObservable providePackagesChangedObserver(Context context) {
        IntentFilter appActionFilter = new IntentFilter();
        appActionFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        appActionFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        appActionFilter.addDataScheme("package");

        return new BroadcastObservable(context, appActionFilter);
    }

    @Named(NAME_APPS)
    @Provides
    @Singleton
    final ContentProviderObservable provideTablesObserver(Context context) {
        return new ContentProviderObservable(context, AppsContract.TaggedAppColumns.CONTENT_URI);
    }

    @Provides
    @Singleton
    final ShortcutNameComparator provideShortcutNameComparator(
            Context context,
            @Named(NAME_APPS) Map<ComponentName, CharSequence> cacheMap) {
        return new ShortcutNameComparator(context, cacheMap);
    }

    @Named(NAME_APPS)
    @Provides
    @Singleton
    final Map<ComponentName, CharSequence> provideCacheMap(Context context) {
        return new ArrayMap<>();
    }

    @Named(NAME_APPS)
    @Provides
    @Singleton
    final SimpleArrayMap<ComponentName, ResolveInfoAppEntry> provideResolveInfoCache() {
        return new SimpleArrayMap<>();
    }

    @Named(NAME_APPS)
    @Provides
    @Singleton
    final Repository<Result<AppPageData>> provideAppPageRepository(
            @Named(NAME_APPS) BroadcastObservable packageObservable,
            @Named(NAME_APPS) ContentProviderObservable providerObservable,
            @Named(NAME_APPS) Executor appsExecutor,
            @Named(NAME_APPS_HISTORY) Repository<Result<List<HistoryItem>>> historyRepo,
            @Named(NAME_APPS_TAGS) Repository<Result<List<AppTag>>> appTagRepo,
            AppsiApplication app
    ) {
        return Repositories.repositoryWithInitialValue(Result.<AppPageData>absent())
                .observe(packageObservable, providerObservable, historyRepo, appTagRepo)
                .onUpdatesPerLoop()
                .goTo(appsExecutor)
                .attemptGetFrom(new AppPageDataBuilderSupplier())
                .orSkip()
                .attemptMergeIn(new LauncherAppsProvider(app),
                        new Merger<AppPageData.Builder, Result<ArrayList<ResolveInfoAppEntry>>,
                                Result<AppPageData.Builder>>() {

                            @NonNull
                            @Override
                            public Result<AppPageData.Builder> merge(
                                    @NonNull AppPageData.Builder builder,
                                    @NonNull Result<ArrayList<ResolveInfoAppEntry>> listResult) {
                                if (listResult.isAbsent()) return Result.absent();
                                return Result.success(builder.allApps(listResult.get()));
                            }
                        })
                .orSkip()
                .attemptMergeIn(appTagRepo,
                        new Merger<AppPageData.Builder, Result<List<AppTag>>,
                                Result<AppPageData.Builder>>() {
                            @NonNull
                            @Override
                            public Result<AppPageData.Builder> merge(
                                    @NonNull AppPageData.Builder builder,
                                    @NonNull Result<List<AppTag>> listResult) {
                                if (listResult.isAbsent()) return Result.absent();
                                return Result.success(builder.tags(listResult.get()));
                            }

                        })
                .orSkip()
                .attemptMergeIn(historyRepo,
                        new Merger<AppPageData.Builder, Result<List<HistoryItem>>,
                                Result<AppPageData.Builder>>() {

                            @NonNull
                            @Override
                            public Result<AppPageData.Builder> merge(
                                    @NonNull AppPageData.Builder builder,
                                    @NonNull Result<List<HistoryItem>> listResult) {
                                if (listResult.isAbsent()) return Result.absent();
                                return Result.success(builder.recentApps(listResult.get()));
                            }
                        })
                .orSkip()
                .thenMergeIn(new TaggedAppCursorSupplier(app), new TaggedAppsIntoDataMerger(app))
                .onDeactivation(RepositoryConfig.SEND_INTERRUPT)
                .compile();
    }


//
//    private static class AppTagSupplier implements Supplier<Result<List<AppTag>>> {
//
//        @NonNull
//        @Override
//        public Result<List<AppTag>> get() {
//            List<AppTag> tags = AppTagUtils.loadTagsBlocking(mContext);
//
//
//
//            return Result.success(tags);
//        }
//    }

    @Named(NAME_APPS_HISTORY)
    @Provides
    final public Repository<Result<List<HistoryItem>>> provideHistoryItemsRepository(
            AppsiApplication app,
            @Named(NAME_APPS) Executor appsExecutor) {

        return Repositories.repositoryWithInitialValue(Result.<List<HistoryItem>>absent())
                .observe(new ContentProviderObservable(app, LaunchHistoryColumns.CONTENT_URI))
                .onUpdatesPerLoop()
                .goTo(appsExecutor)
                .attemptGetFrom(new HistoryItemSupplier(app))
                .orSkip()
                .thenTransform(new HistoryItemCursorTransform())
                .onDeactivation(RepositoryConfig.SEND_INTERRUPT)
                .compile();


    }


    @Named(NAME_APPS_TAGS)
    @Provides
    final public Repository<Result<List<AppTag>>> provideAppTagsRepository(
            AppsiApplication app,
            @Named(NAME_APPS) Executor appsExecutor) {

        return Repositories.repositoryWithInitialValue(Result.<List<AppTag>>absent())
                .observe(new ContentProviderObservable(app, AppsContract.TagColumns.CONTENT_URI))
                .onUpdatesPerLoop()
                .goTo(appsExecutor)
                .attemptGetFrom(new AppTagCursorSupplier(app))
                .orSkip()
                .thenTransform(new AppTagCursorTransform())
                .onDeactivation(RepositoryConfig.SEND_INTERRUPT)
                .compile();
    }

    static class LauncherAppsProvider
            implements Supplier<Result<ArrayList<ResolveInfoAppEntry>>> {

        @Inject
        Context mContext;

        @Inject
        ShortcutNameComparator mShortcutNameComparator;

        @Named(NAME_APPS)
        @Inject
        Map<ComponentName, CharSequence> mCacheMap;

        @Named(NAME_APPS)
        @Inject
        SimpleArrayMap<ComponentName, ResolveInfoAppEntry> mResolveInfoCache;

        public LauncherAppsProvider(AppsiApplication app) {
            app.getComponent().inject(this);
        }

        @NonNull
        @Override
        public Result<ArrayList<ResolveInfoAppEntry>> get() {

            ArrayList<ResolveInfoAppEntry> result = new ArrayList<>();

            LauncherAppsCompat lap = LauncherAppsCompat.getInstance(mContext);
            List<LauncherActivityInfoCompat> apps =
                    lap.getActivityList(null, UserHandleCompat.myUserHandle());

            // Fail if we don't have any apps
            if (apps == null || apps.isEmpty()) {
                return Result.success(result);
            }
            // Sort the applications by name
            Collections.sort(apps, mShortcutNameComparator);

            // Create the AppEntries
            int N = apps.size();
            for (int i = 0; i < N; i++) {
                LauncherActivityInfoCompat app = apps.get(i);

                ResolveInfoAppEntry entry = mResolveInfoCache.get(app.getComponentName());

                if (entry == null) {
                    CharSequence label;
                    ComponentName componentName = app.getComponentName();

                    if (mCacheMap.containsKey(componentName)) {
                        label = mCacheMap.get(componentName);
                    } else {
                        label = app.getLabel();
                        mCacheMap.put(componentName, label);
                    }

                    entry = new ResolveInfoAppEntry(app, label);
                } else {
                    // if it already exists, update it with the entry and label.
                    // this makes sure the icon is displayed correctly after update
                    entry.update(app, app.getLabel());
                }

                result.add(entry);
            }


            return Result.success(result);
        }
    }

    private static class AppPageDataBuilderSupplier
            implements Supplier<Result<AppPageData.Builder>> {

        @NonNull
        @Override
        public Result<AppPageData.Builder> get() {
            return Result.success(new AppPageData.Builder());
        }
    }

    public static class HistoryItemSupplier implements Supplier<Result<Cursor>> {

        @Inject
        Context mContext;

        public HistoryItemSupplier(AppsiApplication app) {
            app.getComponent().inject(this);
        }

        @NonNull
        @Override
        public Result<Cursor> get() {
            Cursor cursor =
                    mContext.getContentResolver().query(LaunchHistoryColumns.CONTENT_URI,
                            AppHistoryQuery.PROJECTION,
                            null,
                            null,
                            LaunchHistoryColumns.LAUNCH_COUNT + " DESC, " +
                                    LaunchHistoryColumns.LAST_LAUNCHED + " DESC "
                    );
            if (cursor == null) {
                return Result.failure();
            }
            return Result.success(cursor);
        }
    }

    private static class HistoryItemCursorTransform
            implements Function<Cursor, Result<List<HistoryItem>>> {

        @NonNull
        @Override
        public Result<List<HistoryItem>> apply(@NonNull Cursor cursor) {

            List<HistoryItem> result = new ArrayList<>();

            while (cursor.moveToNext()) {
                int count = cursor.getInt(AppHistoryQuery.LAUNCH_COUNT);
                String flattenedComponentName = cursor.getString(AppHistoryQuery.COMPONENT_NAME);
                long lastLaunched = cursor.getLong(AppHistoryQuery.LAST_LAUNCHED);

                ComponentName cn = ComponentName.unflattenFromString(flattenedComponentName);
                HistoryItem item = new HistoryItem();
                item.componentName = cn;
                item.launchCount = count;
                item.lastLaunched = lastLaunched;

                result.add(item);
                if (result.size() >= 9) break;
            }
            cursor.close();

            return Result.success(result);
        }
    }

    public static class TaggedAppCursorSupplier implements Supplier<Result<Cursor>> {

        @Inject
        Context mContext;

        public TaggedAppCursorSupplier(AppsiApplication app) {
            app.getComponent().inject(this);
        }

        @NonNull
        @Override
        public Result<Cursor> get() {
            ContentResolver res = mContext.getContentResolver();

            Cursor result = res.query(AppsContract.TaggedAppColumns.CONTENT_URI,
                    AppQuery.PROJECTION,
                    AppQuery.WHERE_NOT_DELETED,
                    null,
                    AppQuery.ORDER);
            if (result == null) return Result.failure();

            return Result.success(result);
        }
    }

    private static class AppTagCursorTransform implements Function<Cursor, Result<List<AppTag>>> {

        @NonNull
        @Override
        public Result<List<AppTag>> apply(@NonNull Cursor cursor) {
            int count = cursor.getCount();
            List<AppTag> result = new ArrayList<>(count);

            while (cursor.moveToNext()) {
                long id = cursor.getLong(AppTagQuery._ID);
                boolean defaultExpanded = cursor.getInt(AppTagQuery.DEFAULT_EXPANDED) == 1;
                String name = cursor.getString(AppTagQuery.NAME);
                int position = cursor.getInt(AppTagQuery.POSITION);
                int columnCount = cursor.getInt(AppTagQuery.COLUMN_COUNT);
                int tagType = cursor.getInt(AppTagQuery.TAG_TYPE);
                boolean visible = cursor.getInt(AppTagQuery.VISIBLE) == 1;
                AppTag tag = new AppTag(id, name, position, defaultExpanded,
                        visible, columnCount, tagType);
                result.add(tag);
            }

            cursor.close();

            return Result.success(result);
        }
    }

    static class AppTagCursorSupplier implements Supplier<Result<Cursor>> {

        @Inject
        Context mContext;

        public AppTagCursorSupplier(AppsiApplication app) {
            app.getComponent().inject(this);
        }

        @NonNull
        @Override
        public Result<Cursor> get() {
            Uri uri = AppsContract.TagColumns.CONTENT_URI;
            ContentResolver contentResolver = mContext.getContentResolver();
            Cursor cursor =
                    contentResolver
                            .query(uri, AppTagQuery.PROJECTION, null, null, AppTagQuery.ORDER);

            if (cursor == null) return Result.failure();
            return Result.success(cursor);

        }
    }

    public static class TaggedAppsIntoDataMerger
            implements Merger<AppPageData.Builder, Result<Cursor>, Result<AppPageData>> {

        @Named(NAME_APPS)
        @Inject
        SimpleArrayMap<ComponentName, ResolveInfoAppEntry> mCache;

        @Inject
        Context mContext;


        public TaggedAppsIntoDataMerger(AppsiApplication app) {
            app.getComponent().inject(this);

        }

        @NonNull
        @Override
        public Result<AppPageData> merge(@NonNull AppPageData.Builder builder,
                @NonNull Result<Cursor> cursorResult) {

            Cursor cursor = cursorResult.get();

            List<ResolveInfoAppEntry> allApps = builder.getAllApps();

            int appsSize = allApps.size();
            SimpleArrayMap<ComponentName, AppEntry> entriesByComponent =
                    new SimpleArrayMap<>(appsSize);
            for (int i = 0; i < appsSize; i++) {
                AppEntry app = allApps.get(i);
                entriesByComponent.put(app.getComponentName(), app);
            }

            AppPageData result = builder.buid();

            while (cursor.moveToNext()) {
                String shortComponentName = cursor.getString(AppQuery.COMPONENT_NAME);
                ComponentName componentName = ComponentName.unflattenFromString(shortComponentName);


                // find the app entry from all apps. If it does not exists, the component
                // was changed or uninstalled. In that case, ignore it.
                AppEntry appEntry = entriesByComponent.get(componentName);
                if (appEntry == null) continue;

                // now create the tagged-app object. This holds the details of the
                // tagged instance
                TaggedApp taggedApp = new TaggedApp();
                long tagId = cursor.getLong(AppQuery.TAG_ID);
                String tagName = cursor.getString(AppQuery.TAG_NAME);

                taggedApp.mComponentName = componentName;
                taggedApp.mId = cursor.getLong(AppQuery._ID);
                taggedApp.mTagName = tagName;
                taggedApp.mTagId = tagId;
                taggedApp.mAppEntry = appEntry;

                addItemToLongSparseArray(result.mAppsPerTag, tagId, appEntry);
                addItemToMapList(result.mTagsPerComponent, componentName, taggedApp);

            }

            cursor.close();


            mCache.clear();

            int N = allApps.size();
            for (int i = 0; i < N; i++) {
                ResolveInfoAppEntry app = allApps.get(i);
                mCache.put(app.getComponentName(), app);
            }

            return Result.success(result);

        }
    }
}
