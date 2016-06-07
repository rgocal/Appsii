package com.appsimobile.appsii.dagger;

import android.content.ContentResolver;
import android.content.Context;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;

import com.appsimobile.appsii.dagger.agera.BroadcastObservable;
import com.appsimobile.appsii.dagger.agera.ContentProviderObservable;
import com.appsimobile.appsii.module.BaseContactInfo;
import com.appsimobile.appsii.module.PeopleQuery;
import com.appsimobile.appsii.module.people.PeopleLoaderResult;
import com.appsimobile.appsii.permissions.PermissionUtils;
import com.google.android.agera.Function;
import com.google.android.agera.Repositories;
import com.google.android.agera.Repository;
import com.google.android.agera.RepositoryConfig;
import com.google.android.agera.Result;
import com.google.android.agera.Supplier;

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
public final class PeopleModule {

    public static final String NAME_PEOPLE = "people";

    /**
     * The default sort order for this table.
     */
    private static final String DEFAULT_PEOPLE_SORT_ORDER =
            ContactsContract.Contacts.DISPLAY_NAME + " COLLATE LOCALIZED ASC";


    @Named(NAME_PEOPLE)
    @Singleton
    @Provides
    Executor providePeopleExecutor() {
        return Executors.newSingleThreadExecutor();
    }

    @Named(NAME_PEOPLE)
    @Singleton
    @Provides
    ContentProviderObservable provideContentProviderObservable(Context context) {
        return new ContentProviderObservable(context, ContactsContract.Contacts.CONTENT_URI);
    }

    @Named(NAME_PEOPLE)
    @Provides
    @Singleton
    final BroadcastObservable provideBroadcastObservable(Context context) {
        IntentFilter appActionFilter = new IntentFilter();
        appActionFilter.addAction(PermissionUtils.ACTION_PERMISSION_RESULT);

        return new BroadcastObservable(context, appActionFilter);
    }


    @Provides
    @Singleton
    final Repository<Result<PeopleLoaderResult>> providePeoplePageRepository(
            final Context context,
            @Named(NAME_PEOPLE) ContentProviderObservable providerObservable,
            @Named(NAME_PEOPLE) Executor executor,
            @Named(NAME_PEOPLE) BroadcastObservable broadcastObservable
    ) {
        return Repositories.repositoryWithInitialValue(Result.<PeopleLoaderResult>absent())
                .observe(providerObservable, broadcastObservable)
                .onUpdatesPerLoop()
                .goTo(executor)
                .attemptGetFrom(
                        new Supplier<Result<Cursor>>() {
                            @NonNull
                            @Override
                            public Result<Cursor> get() {
                                ContentResolver resolver = context.getContentResolver();

                                Uri uri = ContactsContract.Contacts.CONTENT_URI.buildUpon()
                                        .appendQueryParameter(ContactsContract.DIRECTORY_PARAM_KEY,
                                                String.valueOf(ContactsContract.Directory.DEFAULT))
                                        .build();

                                String select = "((" + ContactsContract.Contacts.DISPLAY_NAME +
                                        " NOTNULL) AND ("
                                        + ContactsContract.Contacts.DISPLAY_NAME + " != '' ))";


                                Cursor cursor = resolver.query(uri,
                                        PeopleQuery.CONTACTS_SUMMARY_PROJECTION,
                                        select,
                                        null,
                                        DEFAULT_PEOPLE_SORT_ORDER);
                                if (cursor == null) return Result.failure();
                                return Result.success(cursor);
                            }
                        })
                .orSkip()
                .thenTransform(
                        new Function<Cursor, Result<PeopleLoaderResult>>() {
                            @NonNull
                            @Override
                            public Result<PeopleLoaderResult> apply(
                                    @NonNull Cursor input) {

                                try {
                                    List<? extends BaseContactInfo> contactInfos =
                                            PeopleQuery.cursorToContactInfos(input);
                                    PeopleLoaderResult result =
                                            new PeopleLoaderResult(contactInfos);
                                    return Result.success(result);
                                } finally {
                                    input.close();
                                }
                            }
                        })
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
//
//    @Named(NAME_APPS_HISTORY)
//    @Provides
//    final public Repository<Result<List<HistoryItem>>> provideHistoryItemsRepository(
//            AppsiApplication app,
//            @Named(NAME_APPS) Executor appsExecutor) {
//
//        return Repositories.repositoryWithInitialValue(Result.<List<HistoryItem>>absent())
//                .observe(new ContentProviderObservable(app, LaunchHistoryColumns.CONTENT_URI))
//                .onUpdatesPerLoop()
//                .goTo(appsExecutor)
//                .attemptGetFrom(new HistoryItemSupplier(app))
//                .orSkip()
//                .thenTransform(new HistoryItemCursorTransform())
//                .onDeactivation(RepositoryConfig.SEND_INTERRUPT)
//                .compile();
//
//
//    }


}
