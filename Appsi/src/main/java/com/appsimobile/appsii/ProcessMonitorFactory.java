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

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.UiThread;
import android.support.annotation.WorkerThread;

import com.appsimobile.appsii.annotation.VisibleForTesting;

import net.jcip.annotations.GuardedBy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by nick on 01/04/15.
 */
public class ProcessMonitorFactory {

    /**
     * The instance of the factory
     */
    @VisibleForTesting
    static ProcessMonitor sInstance;

    /**
     * Returns the singleton instance. Creating it if needed.
     */
    public static ProcessMonitor getInstance(Context context) {
        if (sInstance != null) {
            sInstance = new ProcessMonitorImpl(context);
        }
        return sInstance;
    }

    /**
     * A monitor that can be used to monitor running processes
     */
    public interface ProcessMonitor {

        /**
         * Starts monitoring for the given list of processes. The registered listener
         * will be called when a process switches to a state visible to the user.
         * <p/>
         * Process started before this monitoring starts, will not be reported
         */
        void startMonitoringProcesses(ArrayList<String> processesToReport, long updateIntervalMs);

        /**
         * Stops monitoring the registered processes
         */
        void stopMonitoringProcesses();

        /**
         * Registers a listener that will be called when a monitored package moves into
         * the foreground.
         */
        void setProcessMonitorListener(ProcessMonitorListener processMonitorListener);
    }

    /**
     * Listener that can be registered to the ProcessMonitor that will be called when a
     * monitored package moves into the foreground.
     */
    interface ProcessMonitorListener {

        /**
         * Called when a monitored packages moves into the foreground.
         */
        void onPackageRunningDetected();
    }

    static class ProcessMonitorImpl implements ProcessMonitor {

        /**
         * A message that can be sent to the background handler to
         * request an update to the running packages.
         */
        static final int MSG_UPDATE_RUNNING_PACKAGES = 1;

        /**
         * A message that can be sent to the main handler to notify
         * it about the set of running packages
         */
        static final int MSG_PACKAGES_RUNNING = 2;

        /**
         * A request sent to the main handler, that can be used to
         * schedule an update. (Will send {@link #MSG_UPDATE_RUNNING_PACKAGES}
         * to the background handler).
         */
        static final int MSG_SCHEDULE_NEXT = 3;

        /**
         * The context we are bound to
         */
        final Context mContext;

        /**
         * The activity-manager to query the running apps
         */
        final ActivityManager mActivityManager;

        /**
         * A simple wrapper around all objects that need to be handled with
         * and can be accessed across multiple threads.
         */
        final MultiThreadState mMts;

        /**
         * The set of packages that were received during the last run.
         */
        private final Set<String> mLastSeenPackageList = new HashSet<>(12);

        /**
         * True when the monitor is started
         */
        boolean mStarted;

        long mIntervalMs;

        /**
         * The listener that is registered to receive updates.
         */
        ProcessMonitorListener mProcessMonitorListener;

        /**
         * The handler that handles it's messages in a background thread.
         */
        Handler mBackgroundHandler;

        /**
         * The thread used by {@link #mBackgroundHandler}
         */
        HandlerThread mHandlerThread;


        @UiThread
        public ProcessMonitorImpl(Context context) {
            checkThread("ProcessMonitorImpl");
            mContext = context.getApplicationContext();
            mMts = new MultiThreadState();
            mActivityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        }

        static void checkThread(String methodName) {
            if (Looper.myLooper() != Looper.getMainLooper()) {
                throw new IllegalStateException(
                        methodName + "() can only be called on the main thread");
            }
        }

        @Override
        @UiThread
        public void startMonitoringProcesses(ArrayList<String> processesToReport,
                long updateIntervalMs) {
            checkThread("startMonitoringProcesses");
            mMts.setProcessesToReport(processesToReport);
            if (!mStarted) {
                startHandler();
                mStarted = true;
                postUpdateMessage();
            }
        }

        @UiThread
        public void stopMonitoringProcesses() {
            checkThread("stopMonitoringProcesses");
            mStarted = false;
            mBackgroundHandler.removeMessages(MSG_UPDATE_RUNNING_PACKAGES);
            quitHandler();
        }

        @UiThread
        public void setProcessMonitorListener(ProcessMonitorListener processMonitorListener) {
            checkThread("setProcessMonitorListener");
            mProcessMonitorListener = processMonitorListener;
        }

        @UiThread
        void quitHandler() {
            checkThread("quitHandler");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                mHandlerThread.quitSafely();
            } else {
                mHandlerThread.quit();
            }
        }

        @UiThread
        private void startHandler() {
            checkThread("startHandler");
            mHandlerThread = new HandlerThread("Process-Watcher");
            mBackgroundHandler = new Handler(mHandlerThread.getLooper(), new Handler.Callback() {
                @Override
                public boolean handleMessage(Message msg) {
                    ProcessMonitorImpl.this.updateRunningPackagesInBackground();
                    mMts.mMainHandler.sendEmptyMessage(MSG_SCHEDULE_NEXT);
                    return true;
                }
            });

        }

        @UiThread
        void postUpdateMessage() {
            checkThread("postUpdateMessage");
            if (mBackgroundHandler != null) {
                mBackgroundHandler.
                        sendEmptyMessageDelayed(MSG_UPDATE_RUNNING_PACKAGES, mIntervalMs);
            }

        }

        @WorkerThread
        void updateRunningPackagesInBackground() {
            List<RunningAppProcessInfo> processes =
                    mActivityManager.getRunningAppProcesses();

            if (processes == null) return;

            Set<String> running = null;

            int N = processes.size();
            for (int i1 = 0; i1 < N; i1++) {
                RunningAppProcessInfo process = processes.get(i1);
                if (process.importance <= RunningAppProcessInfo.IMPORTANCE_VISIBLE) {
                    int len = process.pkgList.length;
                    for (int i = 0; i < len; i++) {
                        String packageName = process.pkgList[i];
                        if (mMts.isMonitoringPackage(packageName)) {
                            if (running == null) {
                                running = new HashSet<>(1);
                            }
                            running.add(packageName);
                        }
                    }
                }
            }

            if (running != null) {
                mMts.notifyPackageNamesRunningInBackground(running);
            }
        }

        @UiThread
        void handleMessageOnMainThread(Message msg) {
            if (msg.what == MSG_PACKAGES_RUNNING) {
                // We know we get a Set<String>, so ignore the cast warning
                //noinspection unchecked
                onPackagesRunning((Set<String>) msg.obj);
            } else if (msg.what == MSG_UPDATE_RUNNING_PACKAGES) {
                postUpdateMessage();
            }
        }

        @UiThread
        void onPackagesRunning(Set<String> packageNames) {
            checkThread("onPackagesRunning");
            if (mProcessMonitorListener == null) return;

            // we only need to know new packages are now also running.
            // so we compose a list
            Set<String> temp = new HashSet<>(packageNames);
            temp.removeAll(mLastSeenPackageList);

            setLastSeenPackages(packageNames);

            if (!temp.isEmpty()) {
                mProcessMonitorListener.onPackageRunningDetected();
            }
        }

        @UiThread
        void setLastSeenPackages(Set<String> packageNames) {
            checkThread("setLastSeenPackages");
            mLastSeenPackageList.clear();
            mLastSeenPackageList.addAll(packageNames);
        }

        class MultiThreadState {

            final Handler mMainHandler;

            @GuardedBy("this")
            private final ArrayList<String> mPackageNamesToReport = new ArrayList<>(8);

            MultiThreadState() {
                mMainHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
                    @Override
                    public boolean handleMessage(Message msg) {
                        handleMessageOnMainThread(msg);
                        return true;
                    }
                });

            }

            synchronized boolean isMonitoringPackage(String packageName) {
                return mPackageNamesToReport.contains(packageName);
            }

            public synchronized void setProcessesToReport(ArrayList<String> processesToReport) {
                mPackageNamesToReport.clear();
                mPackageNamesToReport.addAll(processesToReport);
            }

            public void notifyPackageNamesRunningInBackground(Set<String> running) {
                Message message = mMainHandler.obtainMessage();
                message.what = MSG_PACKAGES_RUNNING;
                message.obj = running;
                message.sendToTarget();

            }

        }

    }

}