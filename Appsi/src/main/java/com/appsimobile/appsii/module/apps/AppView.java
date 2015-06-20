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

package com.appsimobile.appsii.module.apps;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.appsimobile.appsii.InterceptingTouchDelegate;
import com.appsimobile.appsii.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an app in the list of all apps or a specific folder
 * Created by nick on 03/06/14.
 */
public class AppView extends FrameLayout implements View.OnClickListener,
        PopupMenu.OnMenuItemClickListener, AppTagUtils.AppTagListener {

    /**
     * The id of the menu action to show app info
     */
    public static final int ACTION_APP_INFO = 0;

    /**
     * The id of the menu action to uninstall the app
     */
    public static final int ACTION_APP_UNINSTALL = 1;

    /**
     * The id of the menu action to launch the app in halo
     */
    public static final int ACTION_APP_HALO = 3;

    /**
     * The id of the menu action to show the tags where the user
     * can add or remove the app from tags
     */
    public static final int ACTION_APP_TAGS = 4;

    /**
     * The id of the menu action in the tags sub-menu, to add a new
     * app tag
     */
    public static final int ACTION_NEW_TAG = Integer.MIN_VALUE;

    /**
     * The list of TaggedApp entries, these are the currently
     * applied AppTags to the current AppEntry
     */
    private final List<TaggedApp> mAppliedTags = new ArrayList<>();

    InterceptingTouchDelegate mTouchDelegate;

    boolean mDispatchToDelegate;

    /**
     * The app entry this App-View is bound to
     */
    private AppEntry mAppEntry;

    /**
     * The image view to display the app's icon
     */
    private ImageView mImage;

    /**
     * The text view containing the title of the app
     */
    private TextView mText;

    /**
     * The overflow menu button
     */
    private View mOverflow;

    /**
     * A async-task used to load the app icon for the current app
     * entry. When non-null, the task may be in progress and can
     * be cancelled.
     */
    private AppIconLoaderTask mActiveAppIconLoaderTask;

    /**
     * All AppTags that are currently present in the system
     */
    private List<AppTag> mAppTags;

    /**
     * A listener that can perform actions on the tag
     */
    private AppActionListener mAppActionListener;

    public AppView(Context context) {
        super(context);
    }

    public AppView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AppView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }


    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mImage = (ImageView) findViewById(R.id.image);
        mText = (TextView) findViewById(R.id.primary_text);
        mOverflow = findViewById(R.id.overflow);
        mOverflow.setOnClickListener(this);

        mAppTags = AppTagUtils.getInstance(getContext()).registerAppTagListener(this);
        setWillNotDraw(false);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }

    /**
     * Binds the App-View to the given app-entry. A list of TaggedApps,
     * is also provided. Each taggedApp represents a tag that is
     * applied to the app-entry.
     */
    public void bind(AppEntry app, List<TaggedApp> tags) {
        mAppEntry = app;
        if (app == null) {
            mImage.setImageBitmap(null);
            mText.setVisibility(GONE);
            mOverflow.setVisibility(GONE);
            return;
        }
        // update the list of tags
        mAppliedTags.clear();
        if (tags != null) {
            mAppliedTags.addAll(tags);
        }


        mText.setVisibility(VISIBLE);
        mOverflow.setVisibility(VISIBLE);

        mText.setText(app.getLabel());

        // when an icon is currently being loaded,
        // cancel the load.
        if (mActiveAppIconLoaderTask != null) {
            mActiveAppIconLoaderTask.cancel(true);
        }

        // if an icon is already loaded in the
        Drawable existing = app.getIconIfReady();
        if (existing == null) {
            mImage.setAlpha(0f);
            mActiveAppIconLoaderTask =
                    new AppIconLoaderTaskImpl(getContext(), app, getContext().getPackageManager());
            mActiveAppIconLoaderTask.execute();
        } else {
            mImage.setAlpha(1f);
            mImage.setImageDrawable(existing);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mActiveAppIconLoaderTask != null) {
            mActiveAppIconLoaderTask.cancel(true);
        }
    }

    @Override
    public void onClick(View v) {
        PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
        popupMenu.getMenu().add(0, ACTION_APP_INFO, 0, R.string.apps_action_app_info);
        if (mAppActionListener != null) {
            popupMenu.getMenu().add(0, ACTION_APP_TAGS, 0, R.string.action_tags);
        }

        popupMenu.getMenu().add(0, ACTION_APP_HALO, 0, R.string.apps_action_halo);
        popupMenu.getMenu().add(0, ACTION_APP_UNINSTALL, 0, R.string.apps_action_uninstall);

        popupMenu.setOnMenuItemClickListener(this);
        popupMenu.show();
    }


    @Override
    public boolean onMenuItemClick(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case ACTION_APP_INFO:
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + mAppEntry.getApplicationInfo().packageName));
                getContext().startActivity(intent);
                return true;
            case ACTION_APP_HALO:
                Context context = getContext();
                PackageManager pm = context.getPackageManager();
                Intent appIntent =
                        pm.getLaunchIntentForPackage(mAppEntry.getApplicationInfo().packageName);
                // 0x2000 is the flag to open in halo
                appIntent.addFlags(0x2000);
                context.startActivity(appIntent);
                return true;
            case ACTION_APP_UNINSTALL:
                Intent uninstall = new Intent(Intent.ACTION_UNINSTALL_PACKAGE);
                uninstall.setData(
                        Uri.parse("package:" + mAppEntry.getApplicationInfo().packageName));
                getContext().startActivity(uninstall);
                return true;
            case ACTION_APP_TAGS:
                mAppActionListener.onEditAppliedTags(mAppEntry, mAppTags, mAppliedTags);
                return true;
        }
        return false;
    }
//
//    private boolean handleTagItemClick(MenuItem item) {
//        if (item.getItemId() == ACTION_NEW_TAG) {
//            mAppActionListener.onAddAppToNewTag(mAppEntry);
//        } else {
//            AppTag tag = mAppTags.get(item.getItemId());
//            if (!item.isChecked()) {
//                mAppActionListener.onAddAppToTag(tag, mAppEntry);
//            } else {
//                TaggedApp taggedApp = getTaggedApp(tag);
//                mAppActionListener.onRemoveAppFromTag(taggedApp);
//
//            }
//        }
//        return true;
//    }

    public AppEntry getAppEntry() {
        return mAppEntry;
    }

    @Override
    public void onTagsChanged(List<AppTag> appTags) {
        mAppTags = appTags;
    }

    void onIconLoaded(Drawable drawable) {
        if (drawable == null) {
            drawable = getContext().getResources().getDrawable(
                    android.R.drawable.sym_def_app_icon);
        }
        mImage.setImageDrawable(drawable);
        mImage.animate().alpha(1);
    }

    public void setAppActionListener(AppActionListener appActionListener) {
        mAppActionListener = appActionListener;
    }

    public interface AppActionListener {

        void onEditAppliedTags(AppEntry entry, List<AppTag> allTags, List<TaggedApp> appliedTags);

    }

    public interface TagActionListener {

        void onEditAppTag(AppTag entry);

        void onReorderApps(AppTag entry);

        void onToggleSingleRow(AppTag entry);
    }

    class AppIconLoaderTaskImpl extends AppIconLoaderTask {


        AppIconLoaderTaskImpl(Context context, AppEntry appEntry, PackageManager packageManager) {
            super(context, appEntry, packageManager);
        }

        @Override
        protected void onPostExecute(Drawable drawable) {
            onIconLoaded(drawable);
        }
    }


}
