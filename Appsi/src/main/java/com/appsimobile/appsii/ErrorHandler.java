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
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;

import java.util.concurrent.atomic.AtomicInteger;


public class ErrorHandler {

    private static final AtomicInteger sTmpAtomicInteger = new AtomicInteger();

    private static final StringBuffer sTmpStringBuffer = new StringBuffer();

    static volatile boolean sGeneratingReport;

    public static Intent createTroubleshootingReport(Context c, StringBuffer error) {
        generateReport(c, error);
        error.insert(0, "\n\n***** Appsi error report begin *****\n");
        error.insert(0,
                "====\n\nPlease describe the issue you run into and the steps to reproduce the " +
                        "issue here\n\n=====\n\n");
        error.append("***** Appsi error report end *****\n\n");

        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setType("text/html");

        String emailAddress = c.getString(R.string.report_bug_and_troubleshoot_address);
        StringBuffer uriBuilder = new StringBuffer("mailto:").append(emailAddress).append("?");
        uriBuilder.append("subject=")
                .append(Uri.encode("Appsi Troubleshooting report"))
                .append("&");
        uriBuilder.append("body=").append(Uri.encode(error.toString()));

        Uri data = Uri.parse(uriBuilder.toString());
        intent.setData(data);

        return intent;
    }

    public static StringBuffer generateReport(Context context, StringBuffer appendTo) {
        StringBuffer result = (appendTo == null ? new StringBuffer() : appendTo);
        if (sGeneratingReport) return result;

        sGeneratingReport = true;
        try {
            PackageInfo pi;
            try {
                pi = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                result.append("Appsi version: ").append(pi.versionName).append(".\n");
                result.append("Appsi versionId: ").append(pi.versionCode).append(".\n");
            } catch (NameNotFoundException e1) {
                result.append("No version info available: ").append(e1.getMessage()).append(".\n");
            }

            result.append("Device API Level: ")
                    .append(android.os.Build.VERSION.SDK_INT)
                    .append(".\n");
            result.append("Device Model: ").append(android.os.Build.MODEL).append(".\n");
            result.append("Device Brand: ").append(android.os.Build.BRAND).append(".\n");
            result.append("Device Label: ").append(android.os.Build.ID).append(".\n");
            result.append("Device Product: ").append(android.os.Build.PRODUCT).append(".\n");
            result.append("Device: ").append(android.os.Build.DEVICE).append(".\n");

            appendInstallationDetails(context, result);

            result.append("Appsi db dump:\n");
            result.append("Appsi plugin scanner result:\n");
            return result;
        } finally {
            sGeneratingReport = false;
        }
    }

    private static StringBuffer appendInstallationDetails(Context context, StringBuffer result) {
        result.append("==============\nInstallation details according to packagemanager: ")
                .append("\n");
        appendPackageInfo(context, "Apps page", "com.appsimobile.appsii.module.appsiapps", result);
        appendPackageInfo(context, "Calendar page", "com.appsimobile.appsii.module.appsicalendar",
                result);
        appendPackageInfo(context, "Calls page", "com.appsimobile.appsii.module.appsicalls",
                result);
        appendPackageInfo(context, "Contacts page", "com.appsimobile.appsii.module.appsicontacts",
                result);
        appendPackageInfo(context, "Settings page", "com.appsimobile.appsii.module.appsisettings",
                result);
        appendPackageInfo(context, "Sms page", "com.appsimobile.appsii.module.appsisms", result);
        appendPackageInfo(context, "Powerpack", "com.appsimobile.appsipowerpack", result);
        result.append(".\n============\n");
        return result;
    }

    private static StringBuffer appendPackageInfo(Context context, String label, String packageName,
            StringBuffer appendTo) {
        boolean exists = packageExists(context, packageName, sTmpAtomicInteger, sTmpStringBuffer);
        String versionName = sTmpStringBuffer.toString();
        int code = sTmpAtomicInteger.get();
        appendTo.append(label).append(": ");
        appendTo.append(exists);
        if (exists) {
            appendTo.append(" version: ").append(versionName).append(" versionCode: ").append(code);
        }
        return appendTo.append("\n");
    }

    static boolean packageExists(Context c, String packageName, AtomicInteger versionCode,
            StringBuffer versionName) {
        try {
            PackageInfo packageInfo = c.getPackageManager().getPackageInfo(packageName, 0);
            versionName.setLength(0);
            versionName.append(packageInfo.versionName);
            versionCode.set(packageInfo.versionCode);
            return true;
        } catch (NameNotFoundException e) {
            return false;
        }
    }


}
