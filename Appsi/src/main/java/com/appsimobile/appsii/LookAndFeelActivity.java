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

import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.appsimobile.appsii.icontheme.iconpack.AbstractIconPack;
import com.appsimobile.appsii.icontheme.iconpack.IconPack;
import com.appsimobile.appsii.icontheme.iconpack.IconPackFactory;
import com.appsimobile.appsii.icontheme.iconpack.IconPackScanner;
import com.appsimobile.appsii.icontheme.iconpack.VoidIconPack;
import com.appsimobile.appsii.theme.CustomThemeActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nick Martens on 6/21/13.
 */
public class LookAndFeelActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            ActivityUtils.setContentView(this, R.layout.preference_view);
            Fragment fragment = getFragmentManager().findFragmentByTag("look_and_feel_fragment");
            if (fragment == null) {
                fragment = new LookAndFeelPreferencesFragment();
                getFragmentManager().beginTransaction()
                        .add(R.id.container, fragment, "look_and_feel_fragment")
                        .commit();
            }

            ActivityUtils.setupToolbar(this, R.id.toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(null);
        } catch (RuntimeException e) {
            Log.e("LookAndFeelActivity", "error", e);
            throw e;
        }
    }


    static class IconPackItem {

        boolean mIsHeader;

        Drawable mDrawable;

        String mName;

        String mType;

        Uri mUri;
    }

    static class IconPackAdapter extends BaseAdapter {

        private static final int TYPE_PACK = 0;

        private static final int TYPE_HEADER = 1;

        private final List<IconPackItem> mIconPacks = new ArrayList<IconPackItem>();

        public void initIconPacks(Context context, List<IconPack> iconPacks) {
            mIconPacks.clear();
            int count = iconPacks.size();
            String lastType = null;
            for (int i = 0; i < count; i++) {
                IconPack iconPack = iconPacks.get(i);
                Uri uri = iconPack.getIconPackUri();
                String typeName =
                        uri == null ? "Appsi" : AbstractIconPack.getIconPackTypeIdFromUri(uri);
                if (!typeName.equals(lastType)) {
                    lastType = typeName;
                    IconPackItem item = new IconPackItem();
                    item.mIsHeader = true;
                    item.mName = typeName;
                    mIconPacks.add(item);
                }
                IconPackItem item = new IconPackItem();
                String text = iconPack.loadTitle(context);
                Drawable icon = iconPack.getIconPackIcon(context);

                item.mName = text;
                item.mType = typeName;
                item.mUri = uri;
                item.mDrawable = icon;

                mIconPacks.add(item);
            }

            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mIconPacks.size();
        }

        @Override
        public IconPackItem getItem(int position) {
            return mIconPacks.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            IconPackItem item = mIconPacks.get(position);
            if (item.mIsHeader) {
                return getViewHeader(item, convertView, parent);
            }
            boolean last = position == mIconPacks.size() - 1;
            boolean nextIsDivider = last || getItemViewType(position + 1) == TYPE_HEADER;

            return getViewIconPack(item, convertView, parent, nextIsDivider);
        }

        public View getViewHeader(IconPackItem item, View convertView, ViewGroup parent) {
//            Context context = parent.getContext();
//            if (convertView == null) {
//                LayoutInflater inflater = LayoutInflater.from(context);
//                convertView = inflater.inflate(R.layout.sidebar_section_grey, parent, false);
//            }
//            TextView textView = (TextView) convertView.findViewById(R.id.text);
//            textView.setText(item.mName);
            return convertView;
        }

        @Override
        public int getItemViewType(int position) {
            IconPackItem item = mIconPacks.get(position);
            if (item.mIsHeader) return TYPE_HEADER;
            return TYPE_PACK;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        public View getViewIconPack(IconPackItem item, View convertView, ViewGroup parent,
                boolean noDivider) {
            Context context = parent.getContext();
            if (convertView == null) {
                LayoutInflater layoutInflater = LayoutInflater.from(context);
                convertView =
                        (TextView) layoutInflater.inflate(R.layout.icon_pack_item, parent, false);
            }
            Drawable back = null;

            Drawable icon = item.mDrawable;
            int dimen = (int) context.getResources().getDimension(R.dimen.small_icon);
            int padding = (int) (8 * context.getResources().getDisplayMetrics().density);
            icon.setBounds(0, 0, dimen, dimen);
            String text = item.mName;

            ((TextView) convertView).setText(text);
            ((TextView) convertView).setCompoundDrawables(icon, null, null, null);
            convertView.setBackgroundDrawable(back);
            convertView.setPadding(padding, convertView.getPaddingTop(), padding,
                    convertView.getPaddingBottom());


            return convertView;
        }
    }

    public static class LookAndFeelPreferencesFragment extends PreferenceFragment
            implements Preference.OnPreferenceClickListener {

        Context mContext;

        Preference mThemeListPreference;

        private final IconPack mNoIconPack = new VoidIconPack();

        private Preference mIconThemePreference;

        private SharedPreferences mPreferences;

        private static void sendNotify(Context context, Uri uri) {
            context.getContentResolver().notifyChange(uri, null);
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            mContext = getActivity();

            addPreferencesFromResource(R.xml.prefs_general);

            mThemeListPreference = findPreference("pref_appsii_theme");
            mThemeListPreference.setOnPreferenceClickListener(this);

            mIconThemePreference = findPreference("pref_icon_theme");
            if (mIconThemePreference != null) {
                mIconThemePreference.setOnPreferenceClickListener(this);
            }

            mPreferences =
                    PreferenceManager.getDefaultSharedPreferences(getActivity());

            String iconThemeUriString = mPreferences.getString("pref_icon_theme", null);
            Uri uri = iconThemeUriString == null ? null : Uri.parse(iconThemeUriString);
            updateSummary(uri);
        }

        private void updateSummary(Uri uri) {
            if (mIconThemePreference == null) return;

            if (uri != null) {
                IconPack iconPack = IconPackFactory.createIconPack(uri);
                mIconThemePreference.setSummary(iconPack.loadTitle(getActivity()));
            } else {
                mIconThemePreference.setSummary(R.string.icon_pack_none);
            }

        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            if ("pref_appsii_theme".equals(preference.getKey())) {
                openThemeEditor();
            } else {
                showIconPackPicker();
            }
            return true;
        }

        public void openThemeEditor() {
            Intent intent = new Intent(getActivity(), CustomThemeActivity.class);
            startActivity(intent);
        }

        private void showIconPackPicker() {
            Context context = getActivity();
            IconPackScanner scanner = IconPackScanner.iconPackScanner(context);
            List<IconPack> iconPacks = scanner.getIconPacks();
            iconPacks.add(0, mNoIconPack);
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            final IconPackAdapter iconPackAdapter = new IconPackAdapter();
            iconPackAdapter.initIconPacks(context, iconPacks);
            builder.setAdapter(iconPackAdapter, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    IconPackItem iconPack = iconPackAdapter.getItem(which);
                    onIconPackPicked(iconPack);
                }
            });
            builder.setTitle(R.string.pref_icon_theme_name);
            AlertDialog dialog = builder.show();
            dialog.getListView().setDivider(null);
        }

        void onIconPackPicked(IconPackItem iconPack) {
            Uri uri = iconPack.mUri;
            mIconThemePreference.setSummary(iconPack.mName);
            String stringUri = uri == null ? null : uri.toString();
            SharedPreferences preferences =
                    PreferenceManager.getDefaultSharedPreferences(getActivity());
            SharedPreferences.Editor editor = preferences.edit();
            if (stringUri == null) {
                editor.remove("pref_icon_theme");
            } else {
                editor.putString("pref_icon_theme", stringUri);
            }
            editor.apply();
        }
    }
}