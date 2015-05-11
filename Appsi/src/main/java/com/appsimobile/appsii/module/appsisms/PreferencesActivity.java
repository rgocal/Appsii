///*
// * Copyright 2014 Appsi Mobile
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package com.appsimobile.appsii.module.appsisms;
//
//import android.app.AlertDialog;
//import android.app.ProgressDialog;
//import android.content.DialogInterface;
//import android.content.DialogInterface.OnMultiChoiceClickListener;
//import android.content.SharedPreferences;
//import android.content.SharedPreferences.Editor;
//import android.net.Uri;
//import android.os.AsyncTask;
//import android.os.Bundle;
//import android.preference.Preference;
//import android.preference.Preference.OnPreferenceClickListener;
//import android.preference.PreferenceActivity;
//import android.preference.PreferenceManager;
//import android.widget.Button;
//import android.widget.Toast;
//
//import com.appsimobile.appsii.R;
//import com.appsimobile.appsii.compat.PreferenceCompat;
//import com.appsimobile.appsisupport.contentprovider.ContactGroupHelper;
//
//import java.util.HashSet;
//import java.util.List;
//import java.util.Set;
//
//public class PreferencesActivity extends PreferenceActivity
//        implements OnPreferenceClickListener, SharedPreferences.OnSharedPreferenceChangeListener {
//
//    List<ContactGroupHelper.ContactGroup> mContactGroups;
//
//    boolean mGroupsLoaded;
//
//    ProgressDialog mProgressDialog;
//
//    Preference mSelectGroupsPreference;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        addPreferencesFromResource(R.xml.prefs_page_sms);
//        mProgressDialog = new ProgressDialog(this);
//        mProgressDialog.setIndeterminate(true);
//        mSelectGroupsPreference = findPreference("sms_pref_custom_contact_groups");
//        mSelectGroupsPreference.setOnPreferenceClickListener(this);
//
//        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
//        preferences.registerOnSharedPreferenceChangeListener(this);
//    }
//
//    @Override
//    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
//        if (key.equals("sms_pref_show_mark_all_read") || key.equals("sms_pref_show_compose") ||
//                key.equals("sms_pref_show_actions")) {
//            Uri actionsUri =
//                    Uri.parse("content://com.appsimobile.appsii.module.appsisms.virtual.actions");
//            getContentResolver().notifyChange(actionsUri, null);
//
//        } else if (key.equals("sms_pref_show_favorites") ||
//                key.equals("sms_pref_unlimited_favorites") ||
//                key.equals("sms_pref_favorites_count")) {
//            Uri u = Uri.parse("content://com.appsimobile.appsii.module.appsisms.virtual
// .favorites");
//            getContentResolver().notifyChange(u, null);
//
//        } else if (key.equals("sms_pref_recent_count") || key.equals("sms_pref_show_recent") ||
//                key.equals("sms_pref_recent_show_time")) {
//            Uri uri = Uri.parse("content://com.appsimobile.appsii.module.appsisms.virtual
// .recent");
//            getContentResolver().notifyChange(uri, null);
//
//        } else if (key.equals("sms_pref_show_allapps") ||
//                key.equals("sms_pref_enable_custom_contact_groups") ||
//                key.equals("sms_pref_group_contacts") ||
//                key.equals("sms_pref_selected_contact_groups")) {
//            Uri allUri = Uri.parse("content://com.appsimobile.appsii.module.appsisms.virtual
// .all");
//            getContentResolver().notifyChange(allUri, null);
//
//        }
//        if (key.equals("sms_pref_phonenumber_format")) {
//            Uri actionsUri =
//                    Uri.parse("content://com.appsimobile.appsii.module.appsisms.virtual.actions");
//            getContentResolver().notifyChange(actionsUri, null);
//            Uri u = Uri.parse("content://com.appsimobile.appsii.module.appsisms.virtual
// .favorites");
//            getContentResolver().notifyChange(u, null);
//            Uri allUri = Uri.parse("content://com.appsimobile.appsii.module.appsisms.virtual
// .all");
//            getContentResolver().notifyChange(allUri, null);
//        }
//    }
//
//    @Override
//    public boolean onPreferenceClick(Preference preference) {
//        if (!mGroupsLoaded) {
//            startContactGroupLoad();
//        } else {
//            onContactGroupsLoaded(mContactGroups);
//        }
//        return true;
//    }
//
//    private void startContactGroupLoad() {
//        mProgressDialog.show();
//        GroupsLoader l = new GroupsLoader(this);
//        l.execute();
//    }
//
//    void onContactGroupsLoaded(List<ContactGroupHelper.ContactGroup> groups) {
//        mGroupsLoaded = true;
//
//        mProgressDialog.hide();
//        mContactGroups = groups;
//        int count = groups.size();
//
//        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
//
//        Set<String> currentSelection =
//                PreferenceCompat.getStringSet(prefs, "sms_pref_selected_contact_groups", null);
//
//        String[] names = new String[count];
//        final List[] values = new List[count];
//        final boolean[] checked = new boolean[count];
//        boolean anyChecked = false;
//
//        for (int i = 0; i < count; i++) {
//            ContactGroupHelper.ContactGroup g = groups.get(i);
//            names[i] = g.title;
//            values[i] = g.ids;
//
//            boolean c = currentSelection != null && containsAny(currentSelection, g.ids);
//            if (c) {
//                anyChecked = true;
//            }
//            checked[i] = c;
//        }
//
//        AlertDialog.Builder b = new AlertDialog.Builder(this);
//
//
//        b.setMultiChoiceItems(names, checked, new OnMultiChoiceClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
//                checked[which] = isChecked;
//
//                int checkedCount = 0;
//                int checkedSize = checked.length;
//                for (int i = 0; i < checkedSize; i++) {
//                    if (checked[i]) checkedCount++;
//                }
//                Button positive = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
//                if (checkedCount == 0) {
//                    positive.setEnabled(false);
//                } else {
//                    positive.setEnabled(true);
//                }
//            }
//        });
//        b.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                int checkedCount = 0;
//                int checkedSize = checked.length;
//                for (int i = 0; i < checkedSize; i++) {
//                    if (checked[i]) checkedCount++;
//                }
//                if (checkedCount > 0) {
//                    Set<String> newValues = new HashSet<String>();
//                    int count = values.length;
//
//                    for (int i = 0; i < count; i++) {
//                        if (checked[i]) {
//                            List<String> ids = values[i];
//                            newValues.addAll(ids);
//                        }
//                    }
//                    Editor e = prefs.edit();
//                    PreferenceCompat.putStringSet(e, "sms_pref_selected_contact_groups",
// newValues);
//                    e.apply();
//                } else {
//                    Toast.makeText(PreferencesActivity.this,
//                            R.string.sms_please_select_at_least_one_group, Toast.LENGTH_SHORT)
//                            .show();
//                }
//            }
//        });
//        AlertDialog dialog = b.create();
//        dialog.show();
//
//        Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
//        if (!anyChecked) {
//            positive.setEnabled(false);
//        }
//
//
//    }
//
//    boolean containsAny(Set<String> container, List<String> items) {
//        int count = items.size();
//        for (int i = 0; i < count; i++) {
//            String item = items.get(i);
//            if (container.contains(item)) return true;
//        }
//        return false;
//    }
//
//    @Override
//    protected void onResume() {
//        super.onResume();
//    }
//
//    @Override
//    protected void onPause() {
//        super.onPause();
//    }
//
//    static class GroupsLoader extends AsyncTask<Void, Void,
// List<ContactGroupHelper.ContactGroup>> {
//
//        private final PreferencesActivity mContext;
//
//        public GroupsLoader(PreferencesActivity context) {
//            mContext = context;
//        }
//
//        @Override
//        protected List<ContactGroupHelper.ContactGroup> doInBackground(Void... params) {
//            return ContactGroupHelper.loadContactGroups(mContext);
//        }
//
//        @Override
//        protected void onPostExecute(List<ContactGroupHelper.ContactGroup> result) {
//            mContext.onContactGroupsLoaded(result);
//        }
//    }
//
//
//}
