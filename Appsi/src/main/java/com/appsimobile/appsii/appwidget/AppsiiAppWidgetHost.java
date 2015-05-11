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

package com.appsimobile.appsii.appwidget;

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.os.TransactionTooLargeException;
import android.support.annotation.NonNull;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.WeakHashMap;

/**
 * An extended host, that allows registering listeners for starting and stopping
 * the widgets. It also returns a special host-view needed to correctly dispatch
 * the events when using a recycler-view that wants to scroll the app-widgets,
 * before responding to the scroll by itself.
 * <p/>
 * The startListening implementation is cherry-picked from the AOSP launcher3 app.
 */
public class AppsiiAppWidgetHost extends AppWidgetHost {

    final WeakHashMap<HostStatusListener, Void> mListeners = new WeakHashMap<>();

    private final ArrayList<Runnable> mProviderChangeListeners = new ArrayList<Runnable>();

    boolean mStartedListening;

    public AppsiiAppWidgetHost(Context context, int hostId) {
        super(context, hostId);
    }

    @Override
    public void startListening() {
        try {
            super.startListening();
            mStartedListening = true;
            notifyStartListening();
        } catch (Exception e) {
            if (e.getCause() instanceof TransactionTooLargeException) {
                // We're willing to let this slide. The exception is being caused by the list of
                // RemoteViews which is being passed back. The startListening relationship will
                // have been established by this point, and we will end up populating the
                // widgets upon bind anyway. See issue 14255011 for more context.
            } else {
                throw new RuntimeException(e);
            }
        }
    }

    private void notifyStartListening() {
        for (HostStatusListener l : mListeners.keySet()) {
            if (l != null) {
                l.onStartedListening();
            }
        }
    }

    @Override
    public void stopListening() {
        super.stopListening();
        mStartedListening = false;

        notifyStopListening();
        clearViews();
        notifyViewsCleared();
    }

    private void notifyStopListening() {
        for (HostStatusListener l : mListeners.keySet()) {
            if (l != null) {
                l.onStoppedListening();
            }
        }

    }

    private void notifyViewsCleared() {
        for (HostStatusListener l : mListeners.keySet()) {
            if (l != null) {
                l.onViewsCleared();
            }
        }

    }

    @Override
    protected AppWidgetHostView onCreateView(Context context, int appWidgetId,
            AppWidgetProviderInfo appWidget) {
        return new AppsiAppWidgetHostView(context);
    }

    protected void onProvidersChanged() {
        // Once we get the message that widget packages are updated, we need to rebind items
        // in AppsCustomize accordingly.
//        mLauncher.bindPackagesUpdated(LauncherModel.getSortedWidgetsAndShortcuts(mLauncher));

        for (Runnable callback : mProviderChangeListeners) {
            callback.run();
        }
    }

    public boolean isListening() {
        return mStartedListening;
    }

    public void addProviderChangeListener(Runnable callback) {
        mProviderChangeListeners.add(callback);
    }

    public void removeProviderChangeListener(Runnable callback) {
        mProviderChangeListeners.remove(callback);
    }

    public void registerHostStatusListener(HostStatusListener l) {
        mListeners.put(l, null);
    }

    public void unregisterHostStatusListener(HostStatusListener l) {
        mListeners.remove(l);
    }

    public interface HostStatusListener {

        void onStartedListening();

        void onStoppedListening();

        void onViewsCleared();
    }

    public interface CapturedEventQueue {

        boolean dispatchTouchEvent(MotionEvent e);

        void release();
    }

    /**
     * A very special implementation of the AppWidgetHostView that allows Appsii
     * to re-route it's motion-event input. Because the recycler-view somehow
     * posts the events to the children even if it's been captured I needed some
     * way to post modified events to the view, and have it ignore the normal
     * events dispatched by the system.
     */
    public static class AppsiAppWidgetHostView extends AppWidgetHostView {

        boolean mEventTouchQueueCaptured;

        AppWidgetTouchEventQueue mAppWidgetTouchEventQueue;

        public AppsiAppWidgetHostView(Context context) {
            super(context);
        }

        /**
         * This is called when a component (HomeController) wants to override the
         * event queue. This will make the view ignore normal touch events and
         * respond only to our own stream of events.
         */
        public CapturedEventQueue captureEventQueue() {
            mEventTouchQueueCaptured = true;
            if (mAppWidgetTouchEventQueue == null) {
                mAppWidgetTouchEventQueue = new AppWidgetTouchEventQueue();
            }
            return mAppWidgetTouchEventQueue;
        }

        @Override
        public boolean dispatchTouchEvent(@NonNull MotionEvent ev) {
            // When the event queue is captured, return false and
            // do not dispatch
            return !mEventTouchQueueCaptured && super.dispatchTouchEvent(ev);
        }

        /**
         * Used to dispatch events when the queue has been captured.
         */
        boolean innerDispatchTouchEvent(MotionEvent e) {
            // When the event queue is captured, dispatch them to
            // the component tree.
            return mEventTouchQueueCaptured && super.dispatchTouchEvent(e);
        }

        class AppWidgetTouchEventQueue implements CapturedEventQueue {

            @Override
            public boolean dispatchTouchEvent(MotionEvent e) {
                return innerDispatchTouchEvent(e);
            }

            @Override
            public void release() {
                mEventTouchQueueCaptured = false;
            }
        }

    }

}
