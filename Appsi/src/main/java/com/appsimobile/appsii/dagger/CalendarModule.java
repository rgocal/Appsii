package com.appsimobile.appsii.dagger;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.support.annotation.NonNull;
import android.text.format.Time;
import android.util.SparseBooleanArray;

import com.appsimobile.appsii.AppsiApplication;
import com.appsimobile.appsii.dagger.agera.BroadcastObservable;
import com.appsimobile.appsii.dagger.agera.ContentProviderObservable;
import com.appsimobile.appsii.module.appsiagenda.AgendaDaysResult;
import com.appsimobile.appsii.module.appsiagenda.AgendaEvent;
import com.appsimobile.appsii.module.appsiagenda.AgendaEventsResult;
import com.appsimobile.appsii.permissions.PermissionUtils;
import com.google.android.agera.Function;
import com.google.android.agera.Repositories;
import com.google.android.agera.Repository;
import com.google.android.agera.RepositoryConfig;
import com.google.android.agera.Result;
import com.google.android.agera.Supplier;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
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
public class CalendarModule {

    public static final String NAME_CALENDAR = "name_calendar";
    public static final String NAME_UTC = "name_utc";

    private static final String WHERE_CALENDARS_SELECTED =
            CalendarContract.Calendars.VISIBLE + "=?";

    private static final String[] WHERE_CALENDARS_ARGS = {
            "1"
    };

    /**
     * The default sort order for this table.
     */
    private static final String DEFAULT_SORT_ORDER = "begin ASC";


    @Named(NAME_UTC)
    @Provides
    final Calendar provideUtcCalendar() {
        return Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    }

    @Named(NAME_CALENDAR)
    @Provides
    @Singleton
    final ContentProviderObservable provideTablesObservable(Context context) {
        return new ContentProviderObservable(context, CalendarContract.CONTENT_URI);
    }


    @Named(NAME_CALENDAR)
    @Provides
    @Singleton
    final BroadcastObservable provideBroadcastObservable(Context context) {
        IntentFilter appActionFilter = new IntentFilter();
        appActionFilter.addAction(Intent.ACTION_DATE_CHANGED);
        appActionFilter.addAction(PermissionUtils.ACTION_PERMISSION_RESULT);

        return new BroadcastObservable(context, appActionFilter);
    }


    @Named(NAME_CALENDAR)
    @Singleton
    @Provides
    Executor provideCalendarExecutor() {
        return Executors.newSingleThreadExecutor();
    }


    @Provides
    final public Repository<Result<AgendaEventsResult>> provideAgendaEventsRepository(
            final AppsiApplication app,
            @Named(NAME_CALENDAR) ContentProviderObservable tablesObservable,
            @Named(NAME_CALENDAR) BroadcastObservable broadcastObservable,
            @Named(NAME_CALENDAR) Executor executor
            ) {

        return Repositories.repositoryWithInitialValue(Result.<AgendaEventsResult>absent())
                .observe(tablesObservable, broadcastObservable)
                .onUpdatesPer(250)
                .goTo(executor)
                .getFrom(new StartAndEndTimeSupplier())
                .attemptTransform(new Function<Result<long[]>, Result<Uri>>() {
                    @NonNull
                    @Override
                    public Result<Uri> apply(@NonNull Result<long[]> input) {
                        if (input.isAbsent()) return Result.absent();

                        long start = input.get()[0];
                        long end = input.get()[1];
                        int startDay = Time.getJulianDay(start, 0);
                        int endDay = Time.getJulianDay(end, 0);
                        Uri uri = CalendarContract.Instances.CONTENT_BY_DAY_URI.
                                buildUpon().
                                appendPath(String.valueOf(startDay)).
                                appendPath(String.valueOf(endDay)).build();

                        return Result.success(uri);
                    }
                })
                .orSkip()
                .attemptTransform(new Function<Uri, Result<Cursor>>() {
                    @NonNull
                    @Override
                    public Result<Cursor> apply(@NonNull Uri uri) {
                        ContentResolver contentResolver = app.getContentResolver();
                        Cursor cursor = contentResolver
                                .query(uri, CalendarQuery.projection, WHERE_CALENDARS_SELECTED,
                                        WHERE_CALENDARS_ARGS, DEFAULT_SORT_ORDER);

                        if (cursor == null) return Result.failure();
                        return Result.success(cursor);
                    }
                })
                .orSkip()
                .thenTransform(new Function<Cursor, Result<AgendaEventsResult>>() {
                    @NonNull
                    @Override
                    public Result<AgendaEventsResult> apply(@NonNull Cursor c) {

                        List<AgendaEvent> result = new ArrayList<>(c.getCount());

                        c.moveToPosition(-1);

                        if (c.getCount() > 0) {

                            while (c.moveToNext()) {
                                AgendaEvent e = new AgendaEvent();

                                e.allDay = c.getInt(CalendarQuery.ALL_DAY) == 1;
                                e.calendarName = c.getString(CalendarQuery.CALENDAR_DISPLAY_NAME);

                                e.startDay = c.getInt(CalendarQuery.START_DAY);
                                e.startMillis = c.getLong(CalendarQuery.BEGIN);
                                e.calendarId = c.getLong(CalendarQuery.CALENDAR_ID);

                                e.endDay = c.getInt(CalendarQuery.END_DAY);
                                e.endMillis = c.getLong(CalendarQuery.END);

                                int loadColorFrom = CalendarQuery.DISPLAY_COLOR;

                                e.color = 0xff000000 | c.getInt(loadColorFrom);
                                e.id = c.getLong(CalendarQuery.EVENT_ID);
                                e.title = c.getString(CalendarQuery.TITLE);
                                result.add(e);
                            }
                        }

                        AgendaEventsResult res = new AgendaEventsResult(result);
                        return Result.success(res);
                    }
                })
                .onDeactivation(RepositoryConfig.SEND_INTERRUPT)
                .compile();
    }


    @Provides
    final public Repository<Result<AgendaDaysResult>> provideAgendaDayRepository(
            final AppsiApplication app,
            @Named(NAME_CALENDAR) ContentProviderObservable tablesObservable,
            @Named(NAME_CALENDAR) BroadcastObservable broadcastObservable,
            @Named(NAME_CALENDAR) Executor executor
            ) {

        return Repositories.repositoryWithInitialValue(Result.<AgendaDaysResult>absent())
                .observe(tablesObservable, broadcastObservable)
                .onUpdatesPer(250)
                .goTo(executor)
                .getFrom(new StartAndEndTimeSupplier())
                .attemptTransform(new Function<Result<long[]>, Result<Uri>>() {
                    @NonNull
                    @Override
                    public Result<Uri> apply(@NonNull Result<long[]> input) {
                        if (input.isAbsent()) return Result.absent();

                        long start = input.get()[0];
                        long end = input.get()[1];
                        int startDay = Time.getJulianDay(start, 0);
                        int endDay = Time.getJulianDay(end, 0);

                        Uri uri = CalendarContract.EventDays.CONTENT_URI.
                                buildUpon().
                                appendPath(String.valueOf(startDay)).
                                appendPath(String.valueOf(endDay)).build();

                        return Result.success(uri);
                    }
                })
                .orSkip()
                .attemptTransform(new Function<Uri, Result<Cursor>>() {
                    @NonNull
                    @Override
                    public Result<Cursor> apply(@NonNull Uri uri) {

                        final String EVENT_DAYS_SELECTION = CalendarContract.Events.VISIBLE + "=1";

                        ContentResolver contentResolver = app.getContentResolver();
                        Cursor cursor = contentResolver
                                .query(uri,
                                        new String[]{
                                                CalendarContract.EventDays.STARTDAY,
                                                CalendarContract.EventDays.ENDDAY
                                        },
                                        EVENT_DAYS_SELECTION, null, null);

                        if (cursor == null) return Result.failure();
                        return Result.success(cursor);
                    }
                })
                .orSkip()
                .thenTransform(new Function<Cursor, Result<AgendaDaysResult>>() {
                    @NonNull
                    @Override
                    public Result<AgendaDaysResult> apply(@NonNull Cursor c) {
                        int count = c.getCount();

                        AgendaDaysResult res;

                        if (count > 0) {
                            SparseBooleanArray result = new SparseBooleanArray(count);

                            while (c.moveToNext()) {
                                int startDay = c.getInt(0);
                                int endDay = c.getInt(1);
                                for (int i = startDay; i <= endDay; i++) {
                                    result.put(i, true);
                                }
                            }
                            res = new AgendaDaysResult(result);
                        } else {
                            res = new AgendaDaysResult((SparseBooleanArray) null);
                        }
                        return Result.success(res);

                    }
                })
                .onDeactivation(RepositoryConfig.SEND_INTERRUPT)
                .compile();
    }



    static class StartAndEndTimeSupplier implements Supplier<Result<long[]>> {

        public StartAndEndTimeSupplier() {
        }

        @NonNull
        @Override
        public Result<long[]> get() {
            Calendar calendar = Calendar.getInstance();

            calendar.add(Calendar.YEAR, -1);
            long start = calendar.getTimeInMillis();

            calendar.add(Calendar.YEAR, 4);
            long end = calendar.getTimeInMillis();

            return Result.success(new long[] {start, end});
        }
    }


    static class CalendarQuery {


        public static final int DISPLAY_COLOR = 0;

        public static final int ALL_DAY = 1;

        public static final int BEGIN = 2;

        public static final int END = 3;

        public static final int START_DAY = 4;

        public static final int END_DAY = 5;

        public static final int CALENDAR_ID = 6;

        public static final int TITLE = 7;

        public static final int EVENT_TIMEZONE = 8;

        public static final int EVENT_ID = 9;

        public static final int CALENDAR_DISPLAY_NAME = 10;

        static final String[] projection = {
                CalendarContract.Instances.DISPLAY_COLOR,
                CalendarContract.Instances.ALL_DAY,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,
                CalendarContract.Instances.START_DAY,
                CalendarContract.Instances.END_DAY,
                CalendarContract.Instances.CALENDAR_ID,
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.EVENT_TIMEZONE,
                CalendarContract.Instances.EVENT_ID,
                CalendarContract.Instances.CALENDAR_DISPLAY_NAME,
        };


    }
}
