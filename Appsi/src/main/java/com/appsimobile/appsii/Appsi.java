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

package com.appsimobile.appsii;

import android.animation.Animator;
import android.annotation.TargetApi;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.ScrollerCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.VelocityTracker;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.DecelerateInterpolator;
import android.widget.RelativeLayout;

import com.appsimobile.appsii.Sidebar.SidebarListener;
import com.appsimobile.appsii.SidebarHotspot.SwipeListener;
import com.appsimobile.appsii.hotspotmanager.ManageHotspotsActivity;
import com.appsimobile.appsii.icontheme.iconpack.ActiveIconPackInfo;
import com.appsimobile.appsii.module.home.config.HomeItemConfigurationHelper;
import com.appsimobile.appsii.module.home.provider.HomeContract;
import com.appsimobile.appsii.plugins.IconCache;
import com.appsimobile.appsii.preference.PreferenceHelper;
import com.appsimobile.appsii.preference.PreferencesFactory;
import com.appsimobile.appsii.tinting.AppsiLayoutInflater;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Appsii's main service.
 * <p/>
 * To see the status: adb shell dumpsys activity services com.appsimobile.appsii
 */
public class Appsi extends Service
        implements OnSharedPreferenceChangeListener, HotspotHelperListener, SidebarListener,
        PopupLayer.PopupLayerListener, Sidebar.OnCancelCloseListener {

    /**
     * Notification Ids
     */
    public static final int ONGOING_NOTIFICATION_ID = 11;

    /**
     * Will be sent to Appsi to exit the main service
     */
    public static final String ACTION_STOP_APPSI =
            BuildConfig.APPLICATION_ID + ".ACTION_STOP_APPSI";

    /**
     * Will be sent to Appsi to restart the main service. For example after a theme change
     */
    public static final String ACTION_RESTART_APPSI =
            BuildConfig.APPLICATION_ID + ".ACTION_RESTART_APPSI";

    /**
     * Broadcast to send to Appsi to resume all hotspots
     */
    public static final String ACTION_UNSUSPEND = BuildConfig.APPLICATION_ID + ".UNSUSPEND_APPSI";

    /**
     * Sent to Appsi to close the sidebar
     */
    public static final String ACTION_CLOSE_SIDEBAR =
            BuildConfig.APPLICATION_ID + ".action_close_sidebar";

    /**
     * Sent to Appsi to close the sidebar
     */
    public static final String ACTION_TRY_PAGE = BuildConfig.APPLICATION_ID + ".action_try";

    /**
     * Broadcast to get the status from Appsi
     */
    public static final String ACTION_ORDERED_BROADCAST_RUNNING =
            "com.appsimobile.appsii.ACTION_RUNNING";

    // ORDERED BROADCASTS

    public static final int RESULT_RUNNING_STATUS_SUSPENDED = 3;

    public static final int RESULT_RUNNING_STATUS_ENABLED = 4;

    public static final int RESULT_RUNNING_STATUS_DISABLED = 5;

    public static final String ACTION_LOCAL_OPEN_SIDEBAR_FROM_SHORTCUT =
            "com.appsimobile.appsii.ACTION_LOCAL_OPEN_SIDEBAR_FROM_SHORTCUT";

    /**
     * FLAG for appsi to indicate it should open from the left
     */
    public static final int OPEN_FLAG_LEFT = 2;

    /**
     * FLAG for appsi to indicate it should open from the right
     */
    public static final int OPEN_FLAG_RIGHT = 4;

    /**
     * FLAG for appsi to indicate it should open from the right
     */
    public static final int OPEN_FLAG_LIKE_NOTIFICATION_BAR = 8;

    static final int MESSAGE_CLOSE_SIDEBAR = 101;

    // RECEIVERS

    /**
     * True if Appsi was suspended by the user
     */
    static volatile boolean mSuspended = false;

    /**
     * Used in the binder to determine if Appsi was already created.
     * This will be used to determine it's status
     */
    static volatile boolean mCreated = false;

    private final AccelerateInterpolator mOutInterpolator = new AccelerateInterpolator();

    // CONFIGURATION OPTIONS

    private final DecelerateInterpolator mInInterpolator = new DecelerateInterpolator();

    /**
     * Receives and handles the following LOCAL broadcasts:
     * {@link #ACTION_LOCAL_OPEN_SIDEBAR_FROM_SHORTCUT}
     */
    LocalReceiver mLocalActionsReceiver;

    /**
     * The percentage of screen space the sidebar should be wide
     */
    int mSidebarPercentage;

    /**
     * A reference to the action sidebar view
     */
    Sidebar mSidebar;

    // VIEWS

    /**
     * The layer we add views to. This layer automatically handles things like
     * showing and hiding the screen dimming
     */
    PopupLayer mPopupLayer;

    private final Animator.AnimatorListener mCloseListener = new AnimatorAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
            mPopupLayer.removePopupChild(mSidebar);
            mSidebar.setTranslationX(0);
        }
    };

    // RUNTIME STATE

    /**
     * A handler used to post certain events to. And for content observers
     */
    Handler mHandler;

    /**
     * Handles changes in the hotspots. Will remove, add and update hotspots as
     * needed.
     */
    private final ContentObserver mHotspotsContentObserver = new ContentObserver(mHandler) {
        @Override
        public void onChange(boolean selfChange) {
            onChange(selfChange, null);
        }

        // Implement the onChange(boolean, Uri) method to take advantage of the new Uri argument.
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            // simple reload the hotspots
            reloadHotspots();
        }

    };

    /**
     * Used to query the keyguard status. When the key-guard is showing Appsi will
     * not be shown.
     */
    KeyguardManager mKeyguardManager;

    /**
     * The window manager. This is a system service used to show views on top of
     * other apps.
     */
    WindowManager mWindowManager;

    /**
     * The total number of plugins currently added.
     */
    int mConfiguredPluginCount;

    /**
     * The last known configuration appsi was shown in
     */
    Configuration mLastConfiguration;

    /**
     * A view that is dragged across the screen that looks like the notification
     * opening handle.
     */
    NotificationLikeOpener mNotificationLikeOpener;

    AnimationListener mScrollAnimationListener;

    /**
     * receives and handles several broadcasts and updates Appsi accordingly. This
     * mostly involves things like removing an open sidebar when the screen is turned
     * of, closing system dialogs when requested, day-dreams etc.
     * {@link android.content.Intent#ACTION_EXTERNAL_APPLICATIONS_AVAILABLE}
     * {@link android.content.Intent#ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE}
     * {@link android.content.Intent#ACTION_USER_PRESENT}
     * {@link android.content.Intent#ACTION_DREAMING_STARTED}
     * {@link android.content.Intent#ACTION_SCREEN_ON}
     * {@link android.content.Intent#ACTION_CLOSE_SYSTEM_DIALOGS}
     * {@link android.content.Intent#ACTION_SCREEN_OFF}
     */
    private ExternalEventsListener mExternalEventsReceiver;

    /**
     * Handles the following LOCAL broadcasts:
     * {@link #ACTION_ORDERED_BROADCAST_RUNNING}
     * {@link #ACTION_STOP_APPSI}
     */
    private AppsiStatusBroadcastReceiver mIsRunningBroadcastReceiver;

    /**
     * Handles plugin installation events
     */
    private BroadcastReceiver mAppStatusReceiver;

    /**
     * The shared preferences for Appsi
     */
    private SharedPreferences mPrefs;

    /**
     * Handles things like showing/hiding the hotspots and flashing the hotspots.
     * In it's early days, Appsi had two operating modes, one for a hotspot per
     * plugin and one for a single hotspot. This was removed later when Appsi
     * evolved.
     */
    private AbstractHotspotHelper mHotspotHelper;

    /**
     * True while the sidebar is visible.
     */
    private boolean mSidebarVisible;

    /**
     * The hotspot configurations that have been found in the
     * appsi database.
     */
    private List<HotspotItem> mHotspotItems;

    private LoaderManagerImpl mLoaderManager;

    public Appsi() {

    }

    @Override
    public void onCreate() {
        super.onCreate();


        SharedPreferences prefs = PreferencesFactory.getPreferences(this);
        Context context = ThemingUtils.createContextThemeWrapper(this, prefs);

        LayoutInflater layoutInflater = LayoutInflater.from(context);
        if (layoutInflater.getFactory() == null) {
            layoutInflater.setFactory(new AppsiLayoutInflater.FactoryImpl());
        }

        mLoaderManager = new LoaderManagerImpl("Appsi", this, false);
        mNotificationLikeOpener = new NotificationLikeOpener(this);
        mLastConfiguration = new Configuration(getResources().getConfiguration());

        mSidebar = (Sidebar) layoutInflater.inflate(R.layout.sidebar, null);
        mSidebar.setLoaderManager(mLoaderManager);
        mSidebar.setOnCancelCloseListener(this);

        mSidebar.setSidebarListener(this);
        mPopupLayer = (PopupLayer) layoutInflater.inflate(R.layout.popup_layer, null);
        mPopupLayer.setPopuplayerListener(this);

        mPrefs = prefs;

        PreferenceHelper preferenceHelper = PreferenceHelper.getInstance(this);

        mSidebarPercentage = preferenceHelper.getSidebarWidth();

        int value = preferenceHelper.getSidebarDimLevel();
        updateDefaultDimColor(ThemingUtils.getPercentage(value));

        prefs.registerOnSharedPreferenceChangeListener(this);

        mHotspotHelper = new HotspotHelperImpl(this, this, mPopupLayer);

        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        mKeyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);

        mExternalEventsReceiver = new ExternalEventsListener();
        mExternalEventsReceiver.register(this);

        mIsRunningBroadcastReceiver = new AppsiStatusBroadcastReceiver();
        mIsRunningBroadcastReceiver.register(this);

        mAppStatusReceiver = AppStatusReceiver.register(this);

        mLocalActionsReceiver = new LocalReceiver();
        mLocalActionsReceiver.register(this);

        mHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                if (msg.what == MESSAGE_CLOSE_SIDEBAR) {
                    onCloseSidebar();
                }
                return false;
            }
        });

        // listen for changes in the hotspots
        getContentResolver().registerContentObserver(HomeContract.Hotspots.CONTENT_URI, true,
                mHotspotsContentObserver);

        // Now that appsi has been initialized, scan the hotspot config
        reloadHotspots();
        mCreated = true;
    }

    private void updateDefaultDimColor(float alpha) {
        mPopupLayer.setDefaultDimAlpha(alpha);
    }

    @Override
    public void onCloseSidebar() {
        // remove any possible messages that are still scheduled to close Appsii
        mHandler.removeMessages(MESSAGE_CLOSE_SIDEBAR);

        if (mSidebarVisible) {
            mSidebarVisible = false;

            boolean left = mSidebar.getIsLeft();

            int sidebarWidth = getSidebarWidth();

            mSidebar.animate().
                    translationX(left ? -sidebarWidth : sidebarWidth).
                    setListener(mCloseListener).
                    setInterpolator(mOutInterpolator).
                    setDuration(150);
            mNotificationLikeOpener.mRemoveAfterScrollEnd = false;
            mSidebar.onSidebarClosing();
        }
        if (mLoaderManager.isStarted()) {
            mLoaderManager.doStop();
        }
    }

    /**
     * Start an AsyncTask that will load the hotspot configurations.
     * This will call {@link #onHotspotsLoaded(java.util.List)} when finished
     */
    void reloadHotspots() {
        HotspotLoader loader = new HotspotLoader(this);
        loader.execute();
    }

    private int getSidebarWidth() {
        int w = mWindowManager.getDefaultDisplay().getWidth();
        int h = mWindowManager.getDefaultDisplay().getHeight();
        int sw = Math.min(w, h);
        return (sw * mSidebarPercentage) / 100;
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        startAppsiService();
        // force initialize the home config to make everything a bit smoother
        // and this makes sure we don't have to wait for it when opening the
        // sidebar for the first time.
        HomeItemConfigurationHelper.getInstance(this);

        String action = intent == null ? null : intent.getAction();
        if (ACTION_TRY_PAGE.equals(action)) {

            HotspotPageEntry entry = intent.getParcelableExtra("entry");
            HotspotItem hotspotItem = intent.getParcelableExtra("hotspot");
            if (entry != null && hotspotItem != null) {
                List<HotspotPageEntry> entries = new ArrayList<>(1);
                entries.add(entry);
                openSidebar(hotspotItem, entries, 0, true);
            }
        }

        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mPrefs.unregisterOnSharedPreferenceChangeListener(this);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mLocalActionsReceiver);

        unregisterReceiver(mExternalEventsReceiver);
        unregisterReceiver(mAppStatusReceiver);
        unregisterReceiver(mIsRunningBroadcastReceiver);

        getContentResolver().unregisterContentObserver(mHotspotsContentObserver);

        mPopupLayer.setPopuplayerListener(null);
        mSidebar.setSidebarListener(null);

        mSidebar = null;
        mPopupLayer = null;

        mSuspended = false;
        mHotspotHelper.removeHotspots();
        mHotspotHelper.onDestroy();

        Intent i = new Intent(AppsiiUtils.ACTION_APPSI_STATUS_CHANGED);
        sendBroadcast(i);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        int diff = mLastConfiguration.diff(newConfig);

        // handle layout-direction change

        boolean directionChanged = (diff & ActivityInfo.CONFIG_LAYOUT_DIRECTION) ==
                ActivityInfo.CONFIG_LAYOUT_DIRECTION;
        boolean localeChanged = (diff & ActivityInfo.CONFIG_LOCALE) ==
                ActivityInfo.CONFIG_LOCALE;
        boolean orientationChanged = newConfig.orientation != mLastConfiguration.orientation;

        mLastConfiguration = new Configuration(newConfig);

        if (directionChanged || localeChanged) {
            Log.i("Appsi", "restarting because of direction or locale change");
            AppsiiUtils.restartAppsi(this);
            return;
        }

        if (orientationChanged) {
            mHotspotHelper.onOrientationChanged();
        }
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (mSidebar != null) {
            mSidebar.onTrimMemory(level);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startAppsiService() {
        updateNotificationStatus();

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent i = new Intent(AppsiiUtils.ACTION_STARTED);
                sendBroadcast(i);
            }
        }, 500);
    }

    public SidebarHotspot.SwipeListener openSidebar(HotspotItem conf,
            List<HotspotPageEntry> entries, int flags, boolean animate) {

        LockableAsyncQueryHandler.lock();
        mSidebar.setSidebarOpening(true);

//        if (mFirstOpen) {
//            if (!mLoaderManager.isStarted()) {
//                mLoaderManager.doStart();
//            }
//            mFirstOpen = false;
//        }

        boolean fullscreen = mHotspotHelper.getTopOffset() == 0;

        mSidebar.setInFullScreenMode(fullscreen);

        mNotificationLikeOpener.mRemoveAfterScrollEnd = false;
        mScrollAnimationListener = null;
        mSidebar.animate().cancel();

        // users reported this may happen
        if (mSidebarVisible) {
            mPopupLayer.forceClose();
            mSidebarVisible = false;
        }

        int sidebarWidth = getSidebarWidth();

        mSidebarVisible = true;

        if (conf == null) {
            conf = getDefaultConfiguration();
        }

        SwipeListener result = null;
        boolean openLikeNotificationBar = (flags & OPEN_FLAG_LIKE_NOTIFICATION_BAR) != 0;
        mSidebar.setTag(conf);
        boolean left = conf.mLeft;

        boolean forceLeft = (flags & OPEN_FLAG_LEFT) != 0;
        boolean forceRight = (flags & OPEN_FLAG_RIGHT) != 0;

        if (forceLeft) {
            left = true;
        }
        if (forceRight) {
            left = false;
        }
        mSidebar.setIsLeft(left);

        View v = mSidebar.findViewById(R.id.sidebar_content);

        RelativeLayout.LayoutParams sidebarLayoutParams =
                (RelativeLayout.LayoutParams) v.getLayoutParams();
        sidebarLayoutParams.width = sidebarWidth;

        View container = mSidebar.findViewById(R.id.sidebar_container);
        RelativeLayout.LayoutParams slp =
                (RelativeLayout.LayoutParams) container.getLayoutParams();

        if (left) {
            slp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 0);
            slp.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
            sidebarLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 0);
            sidebarLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
        } else {
            slp.addRule(RelativeLayout.ALIGN_PARENT_LEFT, 0);
            slp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
            sidebarLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, 0);
            sidebarLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
        }

        if (animate && !openLikeNotificationBar) {

            mNotificationLikeOpener.cancel();

            if (left) {
                mSidebar.setTranslationX(-sidebarWidth);
                mSidebar.animate().
                        setInterpolator(mInInterpolator).setDuration(400).
                        setListener(null).translationX(0);
                mSidebar.findViewById(R.id.sidebar_left_shadow).setVisibility(View.GONE);
                mSidebar.findViewById(R.id.sidebar_right_shadow).setVisibility(View.VISIBLE);
            } else {
                mSidebar.setTranslationX(sidebarWidth);
                mSidebar.animate().setDuration(400).
                        setListener(null).
                        setInterpolator(mInInterpolator).translationX(0);
                mSidebar.findViewById(R.id.sidebar_left_shadow).setVisibility(View.VISIBLE);
                mSidebar.findViewById(R.id.sidebar_right_shadow).setVisibility(View.GONE);
            }
            updateDimColor(mSidebar, left);
            mPopupLayer.addPopupChild(mSidebar);
            mSidebar.updateAdapterData(entries);

            mScrollAnimationListener = null;
        } else if (openLikeNotificationBar) {
            result = mNotificationLikeOpener;

            if (left) {
                mSidebar.findViewById(R.id.sidebar_right_shadow).setVisibility(View.VISIBLE);
                mSidebar.findViewById(R.id.sidebar_left_shadow).setVisibility(View.GONE);
            } else {
                mSidebar.findViewById(R.id.sidebar_right_shadow).setVisibility(View.GONE);
                mSidebar.findViewById(R.id.sidebar_left_shadow).setVisibility(View.VISIBLE);
            }

            int w = mWindowManager.getDefaultDisplay().getWidth();
            int h = mWindowManager.getDefaultDisplay().getHeight();
            int sw = Math.min(w, h);


            mNotificationLikeOpener.setTargetView(mSidebar, sidebarWidth, sw, left);
            mSidebar.updateAdapterData(entries);
            mPopupLayer.addPopupChild(mSidebar);
        } else {
            mSidebar.setTranslationX(0);
            mPopupLayer.addPopupChild(mSidebar);
        }

        return result;
    }

    protected void updateNotificationStatus() {
        if (mSuspended) {
            startForeground(ONGOING_NOTIFICATION_ID, createSuspendedNotification());
        } else {
            stopForeground(false);
            startForeground(ONGOING_NOTIFICATION_ID, createOperatingNormallyNotification());
            if (!mLoaderManager.isStarted()) {
                mLoaderManager.doStart();
            }
        }
    }

    private HotspotItem getDefaultConfiguration() {
        return mHotspotItems.isEmpty() ? null : mHotspotItems.get(0);
    }

    float updateDimColor(View scrollView, boolean left) {
        if (scrollView != null) {
            int mTargetWidth = scrollView.getWidth();
            float factor;
            int scroll = (int) -scrollView.getTranslationX();

            float openedPercentage;
            if (left) {
                scroll = (int) (scroll + (0 * getResources().getDisplayMetrics().density));
                factor = scroll / (float) mTargetWidth;
                openedPercentage = 1 - factor;
            } else {
                scroll = (int) (scroll - (0 * getResources().getDisplayMetrics().density));
                factor = (mTargetWidth - scroll) / (float) mTargetWidth;
                openedPercentage = 1 - (factor - 1);
            }
            mPopupLayer.setDimLayerAlpha(1);
            return openedPercentage;
        }
        return 0;
    }

    Notification createSuspendedNotification() {
        Intent resultIntent = new Intent(ACTION_UNSUSPEND);
        PendingIntent bc = PendingIntent.getBroadcast(this, 0, resultIntent, 0);

        return createNotification(bc, R.string.notification_suspended,
                R.drawable.appsi_notification_icon_alert).build();
    }

    Notification createOperatingNormallyNotification() {

        Intent resultIntent = new Intent(this, MainActivity.class);
        PendingIntent resultPendingIntent = createPendingIntentWithBackstack(resultIntent);

        boolean minimalNotification = mPrefs.getBoolean("pref_minimal_notification", false);


        int desc = R.string.notification_description;

        int drawable = R.drawable.ic_notification;
        NotificationCompat.Builder b = createNotification(resultPendingIntent, desc, drawable);

        // When the user requested a minimal notification, do not add additional actions
        // to it. Simply show the running notification.
        if (!minimalNotification) {

            // When the normal notification must be shown, build a large text notification
            // and add the actions to it.
            NotificationCompat.BigTextStyle largeNotification =
                    new NotificationCompat.BigTextStyle();
            largeNotification.setBigContentTitle(getString(R.string.application_name));
            largeNotification.setSummaryText(getString(R.string.notification_description));
            b.setStyle(largeNotification);

            // hotspots configuration action
            Intent hotspotIntent = new Intent(this, ManageHotspotsActivity.class);
            PendingIntent hsPendingIntent = createPendingIntentWithBackstack(hotspotIntent);
            b.addAction(R.drawable.ic_action_panels,
                    getString(R.string.hotspots_and_pages), hsPendingIntent);

            b.setPriority(NotificationCompat.PRIORITY_MIN);
        } else {
            b.setWhen(0);
            b.setPriority(NotificationCompat.PRIORITY_MIN);
        }
        // don't show on wearables, there is no use for that
        b.setLocalOnly(true);

        return b.build();
    }

    /**
     * Creates a simple notification without any additional actions
     */
    NotificationCompat.Builder createNotification(PendingIntent action, int descriptionResourceId,
            int imageResourceId) {
        NotificationCompat.Builder b = new NotificationCompat.Builder(this);
        b.setSmallIcon(imageResourceId);
        b.setContentTitle(getString(R.string.application_name));
        b.setContentText(getString(descriptionResourceId));

        b.setContentIntent(action);
        // don't show on wearables, there is no use for that
        b.setLocalOnly(true);
        return b;
    }

    /**
     * Creates a pending intent that can be added to an action in the notification
     */
    PendingIntent createPendingIntentWithBackstack(Intent resultIntent) {
        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(MainActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        return stackBuilder.getPendingIntent(
                0,
                PendingIntent.FLAG_UPDATE_CURRENT
        );

    }

    String levelToString(int level) {
        switch (level) {
            case TRIM_MEMORY_BACKGROUND:
                return "TRIM_MEMORY_BACKGROUND";
            case TRIM_MEMORY_COMPLETE:
                return "TRIM_MEMORY_COMPLETE";
            case TRIM_MEMORY_MODERATE:
                return "TRIM_MEMORY_MODERATE";
            case TRIM_MEMORY_UI_HIDDEN:
                return "TRIM_MEMORY_UI_HIDDEN";
            case TRIM_MEMORY_RUNNING_CRITICAL:
                return "TRIM_MEMORY_RUNNING_CRITICAL";
            case TRIM_MEMORY_RUNNING_LOW:
                return "TRIM_MEMORY_RUNNING_LOW";
            case TRIM_MEMORY_RUNNING_MODERATE:
                return "TRIM_MEMORY_RUNNING_LOW";
        }
        return String.valueOf(level);
    }

    void restartAppsiService() {
        stopAppsiService();
        Intent intent = new Intent(this, Appsi.class);
        startService(intent);
    }

    void stopAppsiService() {

        onSuspend();
        mCreated = true;
        mHotspotHelper.removeHotspots();
        stopForeground(true);
        mLoaderManager.doDestroy();
        stopSelf();
        removeHotspots();
    }

    void onSuspend() {
        removeHotspots();
        mPopupLayer.onSuspend();
        Intent i = new Intent(AppsiiUtils.ACTION_SUSPEND);
        sendBroadcast(i);
    }

    void removeHotspots() {
        mHotspotHelper.removeHotspots();
    }

    /**
     * Updates Appsi's status to or from suspended.
     */
    public void setSuspended(boolean suspended) {
        if (mSuspended != suspended) {
            mSuspended = suspended;
            updateNotificationStatus();
            if (suspended) {
                onSuspend();
            } else {
                onUnsuspend();
            }
        }
    }

    void onUnsuspend() {
        addHotspotsIfUnlocked();
        Intent i = new Intent(AppsiiUtils.ACTION_UNSUSPEND);
        sendBroadcast(i);
    }

    void addHotspotsIfUnlocked() {
        if (!isKeyguardLocked() && !mSuspended) {
            addHotspots();
        }
    }

    boolean isKeyguardLocked() {
        return mKeyguardManager.isKeyguardLocked();
    }

    void addHotspots() {
        if (!mSuspended) {
            mHotspotHelper.addHotspots();
        }
    }

    /**
     * Called when loading the hotspots completes. This will remove currently
     * visible hotspots, and add new ones in case the screen is not locked
     */
    public void onHotspotsLoaded(List<HotspotItem> configurations) {
        mHotspotItems = configurations;
        mConfiguredPluginCount = configurations.size();
        updateNotificationStatus();
        mHotspotHelper.onHotspotsLoaded(configurations);
        removeHotspots();
        addHotspotsIfUnlocked();
    }

    void openSidebarFromShortcut(Intent intent) {
//        final Uri uri = Uri.parse(intent.getStringExtra(ShortcutActivity.EXTRA_DATASET_URI));
//        final boolean left =
//                intent.getBooleanExtra(ShortcutActivity.EXTRA_ACTION_TO_PERFORM, false);
//        mHandler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                openSidebar(null, uri, left ? OPEN_FLAG_LEFT : OPEN_FLAG_RIGHT, true);
//            }
//        }, 1);
//
    }

    void onOpenCompleted() {
        if (!mLoaderManager.isStarted()) {
            mLoaderManager.doStart();
        }
        LockableAsyncQueryHandler.unlock();
        mSidebar.setSidebarOpening(false);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        PreferenceHelper preferenceHelper = PreferenceHelper.getInstance(this);
        switch (key) {
            case "pref_minimal_notification":
                onNotificationOptionsChanged();
                break;
            case "pref_sidebar_dimming_level":
                int value = preferenceHelper.getSidebarDimLevel();
                updateDefaultDimColor(ThemingUtils.getPercentage(value));
                break;
            case "pref_icon_theme":
                // TODO: add this again
                String iconPackStringUri = sharedPreferences.getString("pref_icon_theme", null);
                Uri iconPackUri = iconPackStringUri == null ? null : Uri.parse(iconPackStringUri);
                ActiveIconPackInfo.getInstance(this).setActiveIconPackUri(iconPackUri);
                IconCache.getInstance(this).clearAllIcons();
                break;
            case "pref_hotspot_width":
                removeHotspots();
                addHotspotsIfUnlocked();
                break;
            case "pref_hide_persistent_notification_with_hack":
                onNotificationOptionsChanged();
                break;
            case "pref_sidebar_size":
                mSidebarPercentage = preferenceHelper.getSidebarWidth();
                break;
            case "pref_sidebar_haptic_feedback":
                boolean mVibrate = preferenceHelper.getHotspotsHapticFeedbackEnabled();
                mHotspotHelper.setVibrate(mVibrate);
                break;
        }
    }

    private void onNotificationOptionsChanged() {
        updateNotificationStatus();
    }

    @Override
    public void onPopupLayerForceClosed() {
        mSidebarVisible = false;
    }

    @Override
    public void opPopupLayerHidden() {
        addHotspotsIfUnlocked();
    }

    @Override
    public void opPopupLayerShown() {
        removeHotspots();
    }

    @Override
    public SwipeListener openSidebar(HotspotItem conf, List<HotspotPageEntry> entries,
            int flags) {
        return openSidebar(conf, entries, flags, true /* animate */);
    }

    @Override
    public boolean canShowSidebar(HotspotItem configuration) {
        return true;
    }

    @Override
    public void cancelVisualHints() {
    }

    @Override
    public void startActivity(Intent intent) {
        onCloseSidebar();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        super.startActivity(intent);
    }

    @Override
    public void startActivity(Intent intent, Bundle options) {
        onCloseSidebar();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        super.startActivity(intent, options);
    }

    @Override
    public void startIntentSender(IntentSender intent, Intent fillInIntent, int flagsMask,
            int flagsValues, int extraFlags) throws IntentSender.SendIntentException {
        closeAfterAppWidgetAction();
        super.startIntentSender(intent, fillInIntent, flagsMask, flagsValues, extraFlags);
    }

    private void closeAfterAppWidgetAction() {
        mSidebar.showCloseOverlay(new CloseCallbackImpl());
    }

    @Override
    public void startIntentSender(IntentSender intent, Intent fillInIntent, int flagsMask,
            int flagsValues, int extraFlags, Bundle options)
            throws IntentSender.SendIntentException {
        closeAfterAppWidgetAction();
        super.startIntentSender(intent, fillInIntent, flagsMask, flagsValues, extraFlags, options);
    }

    /**
     * There seems to be a problem unregistering receivers sometimes.
     * This is definitely a bug in Appsi. For now work around it by
     * catching the exception to make sure Appsi won't crash.
     */
    @Override
    public void unregisterReceiver(BroadcastReceiver receiver) {
        try {
            super.unregisterReceiver(receiver);
        } catch (Exception e) {
            Log.wtf("Appsi", "error unregistering receiver", e);
        }
    }

    @Override
    public void onCloseCancelled() {
        mHandler.removeMessages(MESSAGE_CLOSE_SIDEBAR);
    }

    static class HotspotLoader extends AsyncTask<Void, Void, List<HotspotItem>> {

        private final WeakReference<Appsi> mContext;

        public HotspotLoader(Appsi c) {
            mContext = new WeakReference<>(c);
        }

        @Override
        protected List<HotspotItem> doInBackground(Void... params) {
            Context context = mContext.get();
            if (context == null) return null;
            return com.appsimobile.appsii.module.HotspotLoader.loadHotspots(context);
        }

        @Override
        protected void onPostExecute(List<HotspotItem> result) {
            Appsi appsi = mContext.get();
            if (appsi != null) {
                appsi.onHotspotsLoaded(result);
            }
        }
    }

    class NotificationLikeOpener implements SidebarHotspot.SwipeListener {

        final int mVelocityTreshold;

        final ScrollerCompat mScrollerCompat;

        int mTargetWidth;

        int mScreenWidth;

        boolean mLeft;

        long mLastLocationUpdate;

        boolean mRemoveAfterScrollEnd;

//        Handler mHandler;

        WeakReference<View> mTargetView;

        private final Runnable mUpdatePositionRunnable = new Runnable() {
            @Override
            public void run() {
                boolean running = mScrollerCompat.computeScrollOffset();
                int x = mScrollerCompat.getCurrX();
                View targetView = mTargetView.get();

                mScrollerCompat.getFinalX();

                if (targetView != null) {
                    targetView.setTranslationX(-x);
                    updateDimColor();
                    if (running) {
                        targetView.postOnAnimation(this);
                    } else {
                        targetView.setTranslationX(-mScrollerCompat.getFinalX());
                        if (mRemoveAfterScrollEnd) {
                            mPopupLayer.removePopupChild(mSidebar);
                        } else {
                            onScrollEnded(targetView);
                        }
                    }
                }
            }
        };

        public NotificationLikeOpener(Context context) {
//            mHandler = new Handler();
            mVelocityTreshold = (int) (context.getResources().getDisplayMetrics().density * 125);
            mScrollerCompat = ScrollerCompat.create(context, new DecelerateInterpolator());

        }

        @Override
        public void setSwipeLocation(SidebarHotspot hotspot, int localX, int localY, int screenX,
                int screenY) {
            mRemoveAfterScrollEnd = false;

            // position the targetview according to the drag
            View targetView = mTargetView.get();
            int delta = mTargetWidth - mScreenWidth;
            if (targetView != null) {
                int xValue =
                        localX > mTargetWidth ? mTargetWidth : localX;
                if (mLeft) {
                    targetView.setTranslationX(-(mScreenWidth - xValue + delta));
                } else {
                    int x = -xValue - mTargetWidth;
                    if (x > 0) {
                        x = 0;
                    }
                    targetView.setTranslationX(-x);
                }
            }
            updateDimColor();
        }

        float updateDimColor() {
            View view = mTargetView.get();
            if (view != null) {
                return Appsi.this.updateDimColor(view, mLeft);

            }
            return 0;
        }

        @Override
        public void onSwipeEnd(SidebarHotspot hotspot, int screenX, int screenY, boolean cancelled,
                VelocityTracker velocityTracker) {
            // snap to target or close
            // fade out drag handle
            View targetView = mTargetView.get();
            velocityTracker.computeCurrentVelocity(1000);
            float xVelocity = velocityTracker.getXVelocity();
            float pct = updateDimColor();

            boolean isOpened = false;
            if (targetView != null) {
                if (mLeft) {
                    int threshold = pct >= .2f ? -mVelocityTreshold : mVelocityTreshold;
                    if (xVelocity > threshold) {
                        mScrollerCompat.startScroll((int) -targetView.getTranslationX(), 0,
                                (int) targetView.getTranslationX(), 0);
                        isOpened = true;
                        // open
                    } else {
                        mScrollerCompat.startScroll((int) -targetView.getTranslationX(), 0,
                                (int) (mTargetWidth + targetView.getTranslationX()), 0, 100);
                        // close
                    }

                } else {
                    int threshold = pct >= .2f ? mVelocityTreshold : -mVelocityTreshold;
                    if (xVelocity < threshold) {
                        mScrollerCompat.startScroll((int) -targetView.getTranslationX(), 0,
                                (int) targetView.getTranslationX(), 0);
                        isOpened = true;
                        // open
                    } else {
                        mScrollerCompat.startScroll((int) -targetView.getTranslationX(), 0,
                                (int) (-mTargetWidth + targetView.getTranslationX()), 0, 100);
                        // close
                    }
                }


                mRemoveAfterScrollEnd = !isOpened;
                targetView.postOnAnimation(mUpdatePositionRunnable);
            }
        }

        void onScrollEnded(View targetView) {
            if (targetView.getTranslationX() == 0) {
                onOpenCompleted();
            }
        }

        public void setTargetView(View v, int targetWidth, int minScreenWidth, boolean left) {
            mTargetView = new WeakReference<>(v);
            mTargetWidth = targetWidth;
            mScreenWidth = minScreenWidth;
            mLeft = left;
            mLastLocationUpdate = System.currentTimeMillis();
            if (left) {
                v.setTranslationX(-targetWidth);
            } else {
                v.setTranslationX(targetWidth);
            }
            updateDimColor();
            mLastLocationUpdate = System.currentTimeMillis();
        }

        public void cancel() {
            mScrollerCompat.abortAnimation();
        }


    }

    class LocalReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_LOCAL_OPEN_SIDEBAR_FROM_SHORTCUT.equals(action)) {
                openSidebarFromShortcut(intent);
            }
        }

        void register(Context context) {
            IntentFilter reloadFilter = new IntentFilter(ACTION_LOCAL_OPEN_SIDEBAR_FROM_SHORTCUT);
            LocalBroadcastManager mgr = LocalBroadcastManager.getInstance(context);
            mgr.registerReceiver(mLocalActionsReceiver, reloadFilter);

        }
    }

    class AppsiStatusBroadcastReceiver extends BroadcastReceiver {

        public void register(Context appsi) {
            IntentFilter isRunningIntentFilter = new IntentFilter();
            isRunningIntentFilter.addAction(ACTION_STOP_APPSI);
            isRunningIntentFilter.addAction(ACTION_RESTART_APPSI);
            isRunningIntentFilter.addAction(ACTION_ORDERED_BROADCAST_RUNNING);
            appsi.registerReceiver(this, isRunningIntentFilter);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_ORDERED_BROADCAST_RUNNING.equals(action)) {
                if (mSuspended) {
                    setResultCode(RESULT_RUNNING_STATUS_SUSPENDED);
                } else {
                    setResultCode(RESULT_RUNNING_STATUS_ENABLED);
                }
            } else if (ACTION_RESTART_APPSI.equals(action)) {
                restartAppsiService();
            } else if (ACTION_STOP_APPSI.equals(action)) {
                stopAppsiService();
            }
        }
    }

    class ExternalEventsListener extends BroadcastReceiver {

        void register(Context context) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(Intent.ACTION_USER_PRESENT);
            intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
            intentFilter.addAction(Intent.ACTION_SCREEN_ON);
            intentFilter.addAction(ACTION_CLOSE_SIDEBAR);
            intentFilter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
            addApi8Features(intentFilter);

            addApi17Features(intentFilter);
            context.registerReceiver(this, intentFilter);
        }

        @TargetApi(Build.VERSION_CODES.FROYO)
        private void addApi8Features(IntentFilter appActionFilter) {
            appActionFilter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE);
            appActionFilter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE);
        }

        private void addApi17Features(IntentFilter intentFilter) {
            intentFilter.addAction(Intent.ACTION_DREAMING_STARTED);
            intentFilter.addAction(Intent.ACTION_DREAMING_STOPPED);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case ACTION_CLOSE_SIDEBAR:
                    onCloseSidebar();
                    addHotspotsIfUnlocked();
                    break;
                case Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE:
                    AppStatusReceiver.broadcastRefreshAllApps(context);
                    break;
                case Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE:
                    AppStatusReceiver.broadcastRefreshAllApps(context);
                    break;
                case Intent.ACTION_USER_PRESENT:
                    // show our hotspot
                    addHotspots();
                    break;
                case Intent.ACTION_DREAMING_STARTED:
                    removeHotspots();
                    break;
                case Intent.ACTION_DREAMING_STOPPED:
                    addHotspotsIfUnlocked();
                    break;
                case Intent.ACTION_SCREEN_ON:
                    // show our hotspot in case !mKeyguardManager.isKeyguardLocked()
                    // clear previous animations because they can interfere
                    mSidebar.clearAnimation();
                    View v = mSidebar.findViewById(R.id.sidebar_container);
                    Animation a = v.getAnimation();
                    if (a != null) {
                        a.setAnimationListener(null);
                    }
                    v.clearAnimation();
                    updateNotificationStatus();

                    addHotspotsIfUnlocked();
                    break;
                case Intent.ACTION_CLOSE_SYSTEM_DIALOGS:
                    onCloseSidebar();
                    break;
                case Intent.ACTION_SCREEN_OFF:
                    onCloseSidebar();
                    // remove our hotspot
                    removeHotspots();
                    stopForeground(true);
                    break;
            }
        }
    }

    class CloseCallbackImpl implements CloseCallback {

        @Override
        public void close() {
            onCloseSidebar();
        }

        @Override
        public void closeDelayed(int delayMillis) {
            mHandler.sendEmptyMessageDelayed(MESSAGE_CLOSE_SIDEBAR, delayMillis);
        }
    }

}
