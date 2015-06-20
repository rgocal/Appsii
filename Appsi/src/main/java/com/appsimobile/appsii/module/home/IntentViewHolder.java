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

package com.appsimobile.appsii.module.home;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.appsimobile.appsii.DrawableCompat;
import com.appsimobile.appsii.R;
import com.appsimobile.appsii.module.home.config.HomeItemConfiguration;
import com.appsimobile.appsii.module.home.config.HomeItemConfigurationHelper;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;

/**
 * Created by nick on 29/01/15.
 */
class IntentViewHolder extends BaseViewHolder implements View.OnClickListener,
        HomeItemConfigurationHelper.ConfigurationListener, PopupMenu.OnMenuItemClickListener {

    static final String INTENT_TYPE_ACTIVITY = "app";

    static final String INTENT_TYPE_SHORTCUT = "shortcut";

    static final String INTENT_TYPE_SHELL = "shell";

    static final String INTENT_TYPE_BROADCAST = "broadcast";

    static final String INTENT_TYPE_SERVICE = "service";

    final PackageManager mPackageManager;

    private final int mPrimaryColor;

    private final int mWidgetColor;

    final HomeItemConfiguration mConfigurationHelper;

    final TextView mTextView;

    final ImageView mAppImage;

    @Nullable
    String mAction;

    @Nullable
    String mCategory;

    @Nullable
    String mIntent;

    @Nullable
    String mIcon;

    @Nullable
    String mTitle;

    @Nullable
    String mPackageName;

    @Nullable
    String mClassName;

    @Nullable
    String mIntentType;

    public IntentViewHolder(HomeViewWrapper view) {
        super(view);
        mPackageManager = view.getContext().getPackageManager();

        mConfigurationHelper = HomeItemConfigurationHelper.getInstance(view.getContext());
        mAppImage = (ImageView) view.findViewById(R.id.app_image);
        mTextView = (TextView) view.findViewById(R.id.primary_text);
        mOverflow.setOnClickListener(this);

        Context context = view.getContext();
        final TypedArray a = context.obtainStyledAttributes(
                new int[]{R.attr.colorPrimary, R.attr.colorAccent,
                        R.attr.colorPrimaryDark,
                        R.attr.appsiHomeWidgetPrimaryColor,
                });

        mPrimaryColor = a.getColor(0, Color.BLACK);
        mWidgetColor = a.getColor(3, Color.BLACK);
        a.recycle();

    }


    @Override
    void updateConfiguration() {
        long cellId = mHomeItem.mId;
        mPackageName = mConfigurationHelper.getProperty(cellId, "package", null);
        mClassName = mConfigurationHelper.getProperty(cellId, "class", null);

        mAction = mConfigurationHelper.getProperty(cellId, "action", null);
        mCategory = mConfigurationHelper.getProperty(cellId, "category", null);
        mTitle = mConfigurationHelper.getProperty(cellId, "title", null);
        mIntent = mConfigurationHelper.getProperty(cellId, "intent", null);
        mIcon = mConfigurationHelper.getProperty(cellId, "icon", null);
        mIntentType =
                mConfigurationHelper.getProperty(cellId, "intentType", INTENT_TYPE_ACTIVITY);

        if (mTitle == null) {
            int titleRes = getTitleFromString(mIcon);

            if (titleRes == 0) {
                String title = getTitleFromPackageManager();
                if (title == null) {
                    mTextView.setText(R.string.untitled);
                } else {
                    mTextView.setText(title);
                }
            } else {
                mTextView.setText(titleRes);
            }
        } else {
            mTextView.setText(mTitle);
        }

        int drawableRes = getIconFromString(mIcon);
        if (drawableRes == 0) {
            Context context = getContext();

            Drawable icon;
            if (INTENT_TYPE_SHORTCUT.equals(mIntentType)) {
                icon = decodeIconFromFile(cellId);
            } else {
                icon = decodeIconFromPackageManager();
            }

            if (icon == null) {
                icon = context.getResources().getDrawable(android.R.drawable.sym_def_app_icon);
            }
            mAppImage.setImageDrawable(icon);
        } else {
            Context context = getContext();
            Drawable drawable = context.getResources().getDrawable(drawableRes);
            DrawableCompat.setTintColorCompat(drawable, mPrimaryColor);
            mAppImage.setImageDrawable(drawable);
        }
    }

    @Override
    void bind(HomeItem item, int heightPx) {
        super.bind(item, heightPx);
        updateConfiguration();
        mChildView.setOnClickListener(this);
    }

    private String getTitleFromPackageManager() {
        ComponentInfo info = getComponentInfoByPackageNameAndClassName(mPackageManager);
        if (info == null) return null;
        return String.valueOf(info.loadLabel(mPackageManager));
    }

    private Drawable decodeIconFromPackageManager() {
        if (INTENT_TYPE_SHELL.equals(mIntentType)) return null;

        PackageManager pm = mPackageManager;
        ComponentInfo info;
        if (mPackageName != null && mClassName != null) {
            info = getComponentInfoByPackageNameAndClassName(pm);
        } else {
            info = getComponentInfo(pm);
        }

        if (info != null) {
            return info.loadIcon(pm);
        }
        return null;
    }

    private Drawable decodeIconFromFile(long cellId) {
        File iconFile =
                IntentEditorFragment.createIconFile(getContext(), cellId);
        try {
            BufferedInputStream in =
                    new BufferedInputStream(new FileInputStream(iconFile));
            try {
                Bitmap bitmap = BitmapFactory.decodeStream(in);
                return new BitmapDrawable(itemView.getResources(), bitmap);
            } finally {
                in.close();
            }
        } catch (IOException e) {
            Log.wtf("HomeAdapter", "error decoding icon", e);
        }
        return null;
    }

    @Nullable
    ComponentInfo getComponentInfo(PackageManager pm) {
        if (mIntent == null) return null;
        Intent intent;
        try {
            intent = Intent.parseUri(mIntent, Intent.URI_INTENT_SCHEME);
        } catch (URISyntaxException e) {
            return null;
        }

        ComponentName componentName = intent.getComponent();
        return getComponentInfo(pm, componentName);
    }

    @Nullable
    ComponentInfo getComponentInfoByPackageNameAndClassName(PackageManager pm) {

        if (mPackageName == null || mClassName == null) return null;

        String className = mClassName;
        if (className.startsWith(".")) {
            className = mPackageName + mClassName;
        }
        ComponentName componentName = new ComponentName(mPackageName, className);
        return getComponentInfo(pm, componentName);
    }

    private ComponentInfo getComponentInfo(PackageManager pm, ComponentName componentName) {
        if (componentName == null) return null;
        try {
            switch (mIntentType) {
                case INTENT_TYPE_BROADCAST:
                    return pm.getReceiverInfo(componentName, 0);
                case INTENT_TYPE_SERVICE:
                    return pm.getServiceInfo(componentName, 0);
                default:
                    return pm.getActivityInfo(componentName, 0);
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.wtf("IntentViewHolder", "name not found", e);
            return null;
        }
    }

    @DrawableRes
    int getIconFromString(String key) {
        if (key == null) return 0;
        switch (key) {
            case "play_music":
                return R.drawable.ic_music_black_48dp;
            case "camera":
                return R.drawable.ic_photo_camera_black_48dp;
            default:
                return 0;
        }
    }

    @StringRes
    int getTitleFromString(String key) {
        if (key == null) return 0;
        switch (key) {
            case "play_music":
                return R.string.music;
            case "camera":
                return R.string.camera;
            default:
                return 0;
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.overflow) {
            showOverflowMenu(v);
        } else {
            if (INTENT_TYPE_SHELL.equals(mIntentType)) {
                String command = mConfigurationHelper.getProperty(mHomeItem.mId, "shell", null);
                boolean asRoot = "true".equals(
                        mConfigurationHelper.getProperty(mHomeItem.mId, "as_root", "false"));
                performShellCommand(command, asRoot);

                return;
            }
            Intent intent = createIntent();
            if (intent != null) {
                try {
                    switch (mIntentType) {
                        case INTENT_TYPE_BROADCAST:
                            getContext().sendBroadcast(intent);
                            break;
                        case INTENT_TYPE_SERVICE:
                            getContext().startService(intent);
                            break;
                        default:
                            getContext().startActivity(intent);
                            break;
                    }
                } catch (ActivityNotFoundException | SecurityException e) {
                    Toast.makeText(getContext(),
                            R.string.intent_error_message, Toast.LENGTH_SHORT).show();

                    Log.e("App Launcher", "Error launching intent: " + intent, e);
                }
            }
        }

    }

    private void showOverflowMenu(View v) {
        PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
        MenuInflater inflater = popupMenu.getMenuInflater();
        Menu menu = popupMenu.getMenu();
        inflater.inflate(R.menu.home_item_app, menu);
        popupMenu.setOnMenuItemClickListener(this);
        popupMenu.show();
    }

    private void performShellCommand(final String command, final boolean asRoot) {
        if (command != null) {
            AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    try {
                        if (asRoot) {
                            Process su = Runtime.getRuntime().exec("su");
                            DataOutputStream outputStream =
                                    new DataOutputStream(su.getOutputStream());

                            outputStream.writeBytes(command + "\n");
                            outputStream.flush();

                            outputStream.writeBytes("exit\n");
                            outputStream.flush();
                            su.waitFor();
                        } else {
                            Runtime.getRuntime().exec(command);
                        }
                    } catch (InterruptedException | IOException e) {
                        Log.wtf("IntentViewHolder", "error executing command", e);
                    }

                    return null;
                }
            };
            task.execute();
        }
    }

    Intent createIntent() {
        switch (mIntentType) {
            case INTENT_TYPE_ACTIVITY:
            case INTENT_TYPE_BROADCAST:
            case INTENT_TYPE_SERVICE:
                Intent intent = new Intent();
                if (!TextUtils.isEmpty(mAction)) {
                    intent.setAction(mAction);
                }
                if (!TextUtils.isEmpty(mCategory)) {
                    intent.addCategory(mCategory);
                }
                if (!TextUtils.isEmpty(mPackageName)) {
                    intent.setPackage(mCategory);
                }
                if (!TextUtils.isEmpty(mClassName) && !TextUtils.isEmpty(mPackageName)) {
                    intent.setClassName(mPackageName, mClassName);
                }
                return intent;
            case INTENT_TYPE_SHORTCUT:
                if (mIntent == null) return null;
                try {
                    return Intent.parseUri(mIntent, 0);
                } catch (URISyntaxException e) {
                    Log.wtf("HomeAdapter", "can't parse intent uri: " + mIntent);
                }
            case INTENT_TYPE_SHELL:
            default:
                return null;
        }

    }

    private Context getContext() {
        return itemView.getContext();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_cell_app_prefs) {

            Context context = getContext();
            Intent intent = new Intent(context, CellIntentEditorActivity.class);
            intent.putExtra(CellIntentEditorActivity.EXTRA_CELL_ID, mHomeItem.mId);
            context.startActivity(intent);
            return true;
        }
        return false;
    }


}
