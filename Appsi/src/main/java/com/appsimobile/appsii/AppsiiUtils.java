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

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.net.Uri;

/**
 * Created by nick on 04/04/15.
 */
public final class AppsiiUtils {

    public static final String PARAM_APPSI_ICON_HEIGHT = "icon_height";

    public static final String PARAM_APPSI_ICON_WIDTH = "icon_width";

    /**
     * Action for the appsi service, will unsuspend appsi if it is suspended.
     * Can also be sent in a broadcast by Appsi, indicating is has been unsuspended
     */
    public static final String ACTION_UNSUSPEND = BuildConfig.APPLICATION_ID + ".ACTION_UNSUSPEND";

    /**
     * Broadcast action, sent by Appsi when it is suspended
     */
    public static final String ACTION_SUSPEND = BuildConfig.APPLICATION_ID + ".ACTION_SUSPEND";

    /**
     * Broadcast action, sent by Appsi after the foreground service has been initiated
     */
    public static final String ACTION_STARTED = BuildConfig.APPLICATION_ID + ".ACTION_STARTED";

    public static final String ACTION_APPSI_STATUS_CHANGED =
            BuildConfig.APPLICATION_ID + ".ACTION_APPSI_STATUS_CHANGED";

    private AppsiiUtils() {
    }


    private static AppsiiAccess fromContext(Context context) {
        if (context instanceof Appsi) {
            return new AppsiiAccessImpl((Appsi) context);
        }
        if (context instanceof ContextWrapper) {
            return fromContext(((ContextWrapper) context).getBaseContext());
        }
        return null;
    }

    /**
     * Closes Appsi. This needs a compatible context; a context that is hosted
     * in the Appsi service.
     */
    public static void closeSidebar(Context context) {
        AppsiiAccess a = fromContext(context);
        if (a != null) {
            a.closeSidebar();
        }
    }

    public static Intent createTryOpenIntent(Context context, int pageType) {
        HotspotItem hotspotItem = new HotspotItem();
        hotspotItem.mName = "Try";
        hotspotItem.mLeft = true;
        hotspotItem.mId = -1L;

        HotspotPageEntry entry = new HotspotPageEntry();
        entry.mHotspotId = -1L;
        entry.mPageType = pageType;
        entry.mPageName = "Sample";
        entry.mEnabled = true;
        entry.mPosition = 0;

        Intent intent = new Intent(context, Appsi.class);
        intent.putExtra("hotspot", hotspotItem);
        intent.putExtra("entry", entry);
        intent.setAction(Appsi.ACTION_TRY_PAGE);
        return intent;
    }

    public static void restartAppsi(Context context) {
        Intent intent = new Intent(Appsi.ACTION_RESTART_APPSI);
        context.sendBroadcast(intent);
    }

    public static int[] getIconDimensionsFromQuery(Uri uri, int[] in) {
        if (in == null) in = new int[2];
        String wParam = uri.getQueryParameter(PARAM_APPSI_ICON_WIDTH);
        String hParam = uri.getQueryParameter(PARAM_APPSI_ICON_HEIGHT);
        in[0] = Integer.parseInt(wParam);
        in[1] = Integer.parseInt(hParam);
        return in;
    }

    public static void stopAppsi(Context context) {
        Intent stop = new Intent(Appsi.ACTION_STOP_APPSI);
        context.sendBroadcast(stop);
    }

    public static void startAppsi(Context context) {
        Intent startServiceIntent = new Intent(context, Appsi.class);
        context.startService(startServiceIntent);
    }

    public interface AppsiiAccess {

        void closeSidebar();
    }

    static class AppsiiAccessImpl implements AppsiiAccess {

        final Appsi mAppsi;

        public AppsiiAccessImpl(Appsi appsi) {
            mAppsi = appsi;
        }

        @Override
        public void closeSidebar() {
            mAppsi.onCloseSidebar();
        }
    }
}
