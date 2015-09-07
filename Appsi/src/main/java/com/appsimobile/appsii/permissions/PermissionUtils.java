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
 *
 */

package com.appsimobile.appsii.permissions;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.util.Log;

import com.appsimobile.appsii.BuildConfig;
import com.appsimobile.appsii.PermissionDeniedException;
import com.appsimobile.appsii.RequestPermissionActivity;
import com.appsimobile.appsii.SidebarContext;
import com.appsimobile.appsii.preference.PreferencesFactory;

/**
 * Created by nick on 10/06/15.
 */
public final class PermissionUtils {

    public static final String EXTRA_PERMISSIONS = BuildConfig.APPLICATION_ID + ".PERMISSION_NAMES";

    public static final String EXTRA_REQUEST_CODE = BuildConfig.APPLICATION_ID + ".REQ_CODE";

    public static final String ACTION_PERMISSION_RESULT =
            BuildConfig.APPLICATION_ID + ".ACTION_PERMISSION_RESULT";

    public static final String EXTRA_GRANT_RESULTS = BuildConfig.APPLICATION_ID + ".GRANT_RESULT";

    public static final int REQUEST_CODE_PERMISSION_READ_CALENDAR = 100;

    public static final int REQUEST_CODE_PERMISSION_READ_CALL_LOG = 101;

    public static final int REQUEST_CODE_PERMISSION_READ_CONTACTS = 102;

    private PermissionUtils() {
    }

    /**
     * Throws an exception if a permission is not available. Does nothing if runtime
     * permissions are not available.
     *
     * @throws PermissionDeniedException
     */
    public static void throwIfNotPermitted(Context context, String permissionName)
            throws PermissionDeniedException {
        if (!runtimePermissionsAvailable()) return;

        int result = context.checkSelfPermission(permissionName);
        if (result != PackageManager.PERMISSION_GRANTED) {
            throw new PermissionDeniedException(permissionName);
        }
    }

    /**
     * Returns true when runtime permissions are available. This means, when running on
     * Android M
     */
    public static boolean runtimePermissionsAvailable() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    /**
     * Returns true when the app holds the given permission. Always returns true on
     * devices that do not have runtime permissions (pre-M).
     */
    public static boolean holdsPermission(Context context, String permissionName) {
        if (!runtimePermissionsAvailable()) return true;
        int result = context.checkSelfPermission(permissionName);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean holdsAllPermissions(Context context, String... permissionNames) {
        if (!runtimePermissionsAvailable()) return true;
        for (String permissionName : permissionNames) {
            if (context.checkSelfPermission(permissionName) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * Removes a notification shown for a missing permission. Used in case
     * a permission that is absolutely required has been detected as granted.
     * Such as draw over other apps.
     */
    public static void cancelPermissionNotificationIfNeeded(Context context, int notificationId) {
        if (!runtimePermissionsAvailable()) return;

    }

    /**
     * Shows a notification for a missing permission. Used for permissions that are
     * absolutely required such as draw over other apps. These are checked the first
     * time the app starts.
     */
    public static void showPermissionNotification(Context context, int notificationId,
            String permissionName, @StringRes int permissionDescriptionResId) {
        if (!runtimePermissionsAvailable()) return;

        Log.w("PermissionHelper", "show permission denied: " + permissionName);
    }

    public static void requestPermission(
            Activity activity, int requestCode, String... permissions) {

        if (!runtimePermissionsAvailable()) return;

        activity.requestPermissions(permissions, requestCode);
    }

    public static void requestPermission(
            Fragment fragment, int requestCode, String... permissions) {

        if (!runtimePermissionsAvailable()) return;

        fragment.requestPermissions(permissions, requestCode);
    }

    /**
     * Returns an {@link android.content.Intent} suitable for passing to
     * {@link android.app.Activity#startActivity(Intent)}
     * which prompts the user to grant permissions to this application.
     *
     * @throws NullPointerException if {@code permissions} is {@code null} or empty.
     */
    public static Intent buildRequestPermissionsIntent(Context context, int requestCode,
            @NonNull String... permissions) {

        if (permissions.length == 0) {
            throw new NullPointerException("permission cannot be null or empty");
        }
        Intent intent = new Intent(context, RequestPermissionActivity.class);
        intent.putExtra(EXTRA_PERMISSIONS, permissions);
        intent.putExtra(EXTRA_REQUEST_CODE, requestCode);
        return intent;
    }

    public static boolean shouldShowPermissionError(SidebarContext context, String id) {
        SharedPreferences preferences = PreferencesFactory.getPreferences(context);
        String key = "show_error" + id;
        return preferences.getBoolean(key, true);
    }

    public static void setDontShowPermissionAgain(SidebarContext context, String id) {
        SharedPreferences preferences = PreferencesFactory.getPreferences(context);
        String key = "show_error" + id;
        preferences.edit().putBoolean(key, false).apply();
    }
}
