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

import android.app.Activity;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.appsimobile.appsii.R;
import com.appsimobile.appsii.dagger.AppInjector;
import com.appsimobile.appsii.module.home.config.HomeItemConfiguration;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.inject.Inject;

/**
 * Created by nick on 24/01/15.
 */
public class IntentEditorFragment extends Fragment implements View.OnClickListener,
        EditTitleDialog.EditTitleDialogListener, EditPackageDialog.PackageDialogListener,
        EditClassDialog.ClassDialogListener, Toolbar.OnMenuItemClickListener {

    static final int REQUEST_CODE_PICK_SHORTCUT = 100;

    static final int REQUEST_CODE_PICK_ACTIVITY = 101;

    @Inject
    HomeItemConfiguration mConfigurationHelper;

    @Inject
    SharedPreferences mSharedPreferences;

    long mCellId;

    String mTitle;

    String mAction;

    String mCategory;

    String mPackage;

    String mClassName;

    String mType;

    String mIcon;

    String mCommand;

    boolean mExecuteAsRoot;

    String[] mTypeValues;

    String[] mTypeDisplayNames;

    String[] mIconValues;

    String[] mIconDisplayNames;

    View mAdvancedHeader;

    View mCommandContainer;

    View mIconContainer;

    View mActionContainer;

    View mCategoryContainer;

    View mPackageContainer;

    View mClassContainer;

    View mTypeContainer;

    EditText mTitleView;


    TextView mTypePicker;

    TextView mIconPicker;

    TextView mActionPicker;

    TextView mCommandPicker;

    TextView mCategoryPicker;

    TextView mTargetPackagePicker;

    TextView mTargetClassPicker;

    CheckBox mExecuteAsRootCheckBox;

    boolean mTypePickerVisible;

    boolean mIconPickerVisible;

    int mShowingConfirmationForAction = -1;

    boolean mShowAdvanced;

    SwitchCompat mShowAdvancedSwitch;

    VisibilityHelper mVisibilityHelper;

    public static IntentEditorFragment createInstance(long cellId) {
        IntentEditorFragment result = new IntentEditorFragment();
        Bundle args = new Bundle();
        args.putLong("cellId", cellId);
        result.setArguments(args);
        return result;
    }

    static File createIconFile(Context context, long cellId) {
        return new File(context.getFilesDir(), "shortcut-" + cellId + ".png");
    }

    @Nullable
    static private ActivityInfo getComponentInfoByPackageNameAndClassName(PackageManager pm,
            String packageName, String className) {

        if (packageName == null || className == null) return null;

        if (className.startsWith(".")) {
            className = packageName + className;
        }
        ComponentName componentName = new ComponentName(packageName, className);
        try {
            return pm.getActivityInfo(componentName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.type_container:
                showTypeDialog();
                break;
            case R.id.icon_container:
                showIconDialog();
                break;
            case R.id.action_container:
                showActionDialog();
                break;
            case R.id.category_container:
                showCategoryDialog();
                break;
            case R.id.package_container:
                showTargetPackageDialog();
                break;
            case R.id.command_container:
                showCommandDialog();
                break;
            case R.id.class_container:
                showClassDialog();
                break;
        }
    }

    private void showTypeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        int selectedPosition = positionOfType();
        builder.setSingleChoiceItems(mTypeDisplayNames, selectedPosition,
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        onTypeSelected(which);
                        mTypePickerVisible = false;
                    }
                });
        builder.setNegativeButton(android.R.string.cancel, null);
        mTypePickerVisible = true;
        builder.show();
    }

    private void showIconDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        int selectedPosition = positionOfIcon();
        builder.setSingleChoiceItems(mIconDisplayNames, selectedPosition,
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        onIconSelected(which);
                        mIconPickerVisible = false;
                    }
                });
        builder.setNegativeButton(android.R.string.cancel, null);
        mIconPickerVisible = true;
        builder.show();
    }

    private void showActionDialog() {
        EditTitleDialog dialog = EditTitleDialog.createDialog(mAction, "action");
        dialog.setEditTitleDialogListener(this);
        dialog.show(getFragmentManager(), "text_dialog");
    }

    private void showCategoryDialog() {
        EditTitleDialog dialog = EditTitleDialog.createDialog(mCategory, "category");
        dialog.setEditTitleDialogListener(this);
        dialog.show(getFragmentManager(), "text_dialog");
    }

    private void showTargetPackageDialog() {
        EditPackageDialog dialog = EditPackageDialog.createDialog(mPackage, "package");
        dialog.setDialogListener(this);
        dialog.show(getFragmentManager(), "package_dialog");
    }

    private void showCommandDialog() {
        String shellCommandTitle = getString(R.string.shell_command);
        EditTitleDialog dialog = EditTitleDialog.
                createDialogWithCustomTitle(shellCommandTitle, mCommand, "shell");

        dialog.setEditTitleDialogListener(this);
        dialog.show(getFragmentManager(), "text_dialog");
    }

    private void showClassDialog() {
        if (TextUtils.isEmpty(mPackage)) {
            Toast.makeText(getActivity(), R.string.select_package_first, Toast.LENGTH_SHORT).show();
            return;
        }
        EditClassDialog dialog = EditClassDialog.createDialog(mPackage, mClassName, "class");
        dialog.setDialogListener(this);
        dialog.show(getFragmentManager(), "class_dialog");
    }

    int positionOfType() {
        int count = mTypeValues.length;
        for (int i = 0; i < count; i++) {
            String type = mTypeValues[i];
            if (TextUtils.equals(mType, type)) return i;
        }
        return -1;
    }

    void onTypeSelected(int which) {
        String type = mTypeValues[which];
        if (!TextUtils.equals(type, mType)) {
            mType = type;
            mTypePicker.setText(mTypeDisplayNames[which]);
            mConfigurationHelper.updateProperty(mCellId, "intentType", mType);
            updateFieldVisibilityForType();
        }
    }

    int positionOfIcon() {
        int count = mIconValues.length;
        for (int i = 0; i < count; i++) {
            String icon = mIconValues[i];
            if (TextUtils.equals(mIcon, icon)) return i;
        }
        return -1;
    }

    void onIconSelected(int which) {
        String icon = mIconValues[which];
        if (!TextUtils.equals(icon, mIcon)) {
            mIcon = icon;
            mConfigurationHelper.updateProperty(mCellId, "icon", mIcon);
            mIconPicker.setText(mIconDisplayNames[which]);
        }
    }

    void updateFieldVisibilityForType() {
        mVisibilityHelper.updateFieldVisibilityForType(mType, mShowAdvanced);
    }

    @Override
    public void onFinishEditDialog(String tag, String value) {
        switch (tag) {
            case "action":
                mAction = value;
                mConfigurationHelper.updateProperty(mCellId, "action", value);
                mActionPicker.setText(value);
                break;
            case "class":
                mClassName = value;
                mConfigurationHelper.updateProperty(mCellId, "class", value);
                mTargetClassPicker.setText(value);
                break;
            case "package":
                mPackage = value;
                mConfigurationHelper.updateProperty(mCellId, "package", value);
                mTargetPackagePicker.setText(value);
                break;
            case "category":
                mCategory = value;
                mConfigurationHelper.updateProperty(mCellId, "category", value);
                mCategoryPicker.setText(value);
                break;
            case "shell":
                mCommand = value;
                mConfigurationHelper.updateProperty(mCellId, "shell", value);
                mCommandPicker.setText(value);
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_PICK_ACTIVITY && resultCode == Activity.RESULT_OK) {
            ComponentName componentName = data.getComponent();
            final String packageName = componentName.getPackageName();
            final String activityName = componentName.getClassName();
            applyAppValues(packageName, activityName);
        } else if (requestCode == REQUEST_CODE_PICK_SHORTCUT && resultCode == Activity.RESULT_OK) {
            Intent shortcutIntent = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);
            String shortcutName = data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);
            Bitmap icon = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON);
            if (icon == null) {
                Intent.ShortcutIconResource
                        iconResource = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE);
                if (iconResource != null) {
                    try {
                        Resources remoteResources = getActivity().getPackageManager().
                                getResourcesForApplication(iconResource.packageName);
                        int resId = remoteResources.getIdentifier(iconResource.resourceName, null,
                                null);
                        icon = BitmapFactory.decodeResource(remoteResources, resId);
                    } catch (PackageManager.NameNotFoundException e) {
                        Log.wtf("IntentEditor", "error loading shortcut icon", e);
                    }
                }
            }
            applyShortcutValues(shortcutIntent, shortcutName, icon);


        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppInjector.inject(this);
        Bundle arguments = getArguments();
        mCellId = arguments.getLong("cellId");

        mTypeValues = getResources().getStringArray(R.array.intent_type_values);
        mTypeDisplayNames = getResources().getStringArray(R.array.intent_types);

        mIconValues = getResources().getStringArray(R.array.intent_icon_values);
        mIconDisplayNames = getResources().getStringArray(R.array.intent_icons);

        EditTitleDialog titleDialog =
                (EditTitleDialog) getFragmentManager().findFragmentByTag("text_dialog");
        EditPackageDialog packageDialog =
                (EditPackageDialog) getFragmentManager().findFragmentByTag("package_dialog");

        if (titleDialog != null) {
            titleDialog.setEditTitleDialogListener(this);
        }
        if (packageDialog != null) {
            packageDialog.setDialogListener(this);
        }

        if (savedInstanceState != null) {
            mTypePickerVisible = savedInstanceState.getBoolean("type_picker_visible");
            mIconPickerVisible = savedInstanceState.getBoolean("icon_picker_visible");
            mShowAdvanced = savedInstanceState.getBoolean("show_advanced");
            mShowingConfirmationForAction = savedInstanceState.getInt("confirmation_action");
        }


    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cell_intent_editor, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mVisibilityHelper = new VisibilityHelper(view);

        Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        MenuInflater menuInflater = new MenuInflater(getActivity());
        menuInflater.inflate(R.menu.intent_editor, toolbar.getMenu());
        toolbar.setOnMenuItemClickListener(this);
        toolbar.setTitle(R.string.edit_app_launcher_title);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().finish();
            }
        });

        mTitleView = (EditText) view.findViewById(R.id.title_text);
        mTypePicker = (TextView) view.findViewById(R.id.type_picker);
        mIconPicker = (TextView) view.findViewById(R.id.icon_picker);
        mActionPicker = (TextView) view.findViewById(R.id.action_picker);
        mCommandPicker = (TextView) view.findViewById(R.id.command_picker);
        mCategoryPicker = (TextView) view.findViewById(R.id.category_picker);
        mTargetClassPicker = (TextView) view.findViewById(R.id.class_picker);
        mTargetPackagePicker = (TextView) view.findViewById(R.id.package_picker);
        mShowAdvancedSwitch = (SwitchCompat) view.findViewById(R.id.advanced_switch);
        mExecuteAsRootCheckBox = (CheckBox) view.findViewById(R.id.command_as_root);

        mShowAdvancedSwitch.setChecked(mShowAdvanced);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            mTitleView.getBackground().setColorFilter(0xFF9E80, PorterDuff.Mode.SRC_ATOP);
        }

        mAdvancedHeader = view.findViewById(R.id.advanced_header);
        mCommandContainer = view.findViewById(R.id.command_container);
        mTypeContainer = view.findViewById(R.id.type_container);
        mActionContainer = view.findViewById(R.id.action_container);
        mCategoryContainer = view.findViewById(R.id.category_container);
        mPackageContainer = view.findViewById(R.id.package_container);
        mIconContainer = view.findViewById(R.id.icon_container);
        mClassContainer = view.findViewById(R.id.class_container);


        mTitle = mConfigurationHelper.getProperty(mCellId, "title", null);
        mAction = mConfigurationHelper.getProperty(mCellId, "action", null);
        mCategory = mConfigurationHelper.getProperty(mCellId, "category", null);
        mPackage = mConfigurationHelper.getProperty(mCellId, "package", null);
        mClassName = mConfigurationHelper.getProperty(mCellId, "class", null);
        mIcon = mConfigurationHelper.getProperty(mCellId, "icon", "auto");
        mType = mConfigurationHelper.getProperty(mCellId, "intentType", "app");
        mCommand = mConfigurationHelper.getProperty(mCellId, "shell", null);
        mExecuteAsRoot =
                "true".equals(mConfigurationHelper.getProperty(mCellId, "as_root", "false"));

        mTitleView.setText(mTitle);
        mTypePicker.setText(mTypeDisplayNames[positionOfType()]);
        mIconPicker.setText(mIconDisplayNames[positionOfIcon()]);
        mActionPicker.setText(mAction);
        mCategoryPicker.setText(mCategory);
        mTargetClassPicker.setText(mClassName);
        mTargetPackagePicker.setText(mPackage);
        mCommandPicker.setText(mCommand);
        mExecuteAsRootCheckBox.setChecked(mExecuteAsRoot);

        mTitleView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String newTitle = String.valueOf(s);
                if (!TextUtils.equals(mTitle, newTitle)) {
                    mTitle = newTitle;
                    mConfigurationHelper.updateProperty(mCellId, "title", newTitle);
                }
            }
        });

        mTypeContainer.setOnClickListener(this);
        mIconContainer.setOnClickListener(this);
        mActionContainer.setOnClickListener(this);
        mCategoryContainer.setOnClickListener(this);
        mPackageContainer.setOnClickListener(this);
        mClassContainer.setOnClickListener(this);
        mCommandContainer.setOnClickListener(this);

        mExecuteAsRootCheckBox.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        mExecuteAsRoot = isChecked;
                        mConfigurationHelper.updateProperty(mCellId, "as_root",
                                isChecked ? "true" : "false");
                    }
                });

        mShowAdvancedSwitch.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        mShowAdvanced = isChecked;
                        updateFieldVisibilityForType();
                    }
                });

    }

    @Override
    public void onResume() {
        super.onResume();
        showOverlayIfNeeded();

        if (mTypePickerVisible) {
            showTypeDialog();
        }
        if (mIconPickerVisible) {
            showIconDialog();
        }
        if (mShowingConfirmationForAction != -1) {
            confirmAction(mShowingConfirmationForAction);
        }
        updateFieldVisibilityForType();
    }

    private void showOverlayIfNeeded() {
        SharedPreferences preferences = mSharedPreferences;
        boolean shownInfo = preferences.getBoolean("shown_intent_editor_info", false);
        if (!shownInfo) {
            ViewStub overlay = (ViewStub) getView().findViewById(R.id.overlay);
            if (overlay != null) {
                final View view = overlay.inflate();
                view.findViewById(R.id.dismiss).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dismissOverlay(view);
                    }
                });
            }
        }
    }

    private void confirmAction(final int itemId) {
        mShowingConfirmationForAction = itemId;
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setMessage(R.string.continue_picker);
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mShowingConfirmationForAction = -1;
                onActionConfirmed(itemId);
            }
        });

        builder.show();
    }

    void dismissOverlay(View view) {
        SharedPreferences preferences = mSharedPreferences;
        preferences.edit().putBoolean("shown_intent_editor_info", true).apply();
        view.setVisibility(View.GONE);
    }

    void onActionConfirmed(int itemId) {
        if (itemId == R.id.action_pick_shortcut) {
            pickShortcut();
        } else if (itemId == R.id.action_pick_app) {
            pickActivity();
        }
    }

    private void pickShortcut() {
        Intent i = new Intent(Intent.ACTION_CREATE_SHORTCUT);
        startActivityForResult(i, REQUEST_CODE_PICK_SHORTCUT);
    }

    private void pickActivity() {
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        Intent pickIntent = new Intent(Intent.ACTION_PICK_ACTIVITY);
        pickIntent.putExtra(Intent.EXTRA_INTENT, mainIntent);
        this.startActivityForResult(pickIntent, REQUEST_CODE_PICK_ACTIVITY);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("type_picker_visible", mTypePickerVisible);
        outState.putBoolean("icon_picker_visible", mIconPickerVisible);
        outState.putBoolean("show_advanced", mShowAdvanced);
        outState.putInt("confirmation_action", mShowingConfirmationForAction);
    }

    private void applyAppValues(String packageName, String className) {
        String title = getTitleFromPackageManager(packageName, className);

        mConfigurationHelper.removeProperty(mCellId, "intent");
        mConfigurationHelper.updateProperty(mCellId, "title", title);
        mConfigurationHelper.updateProperty(mCellId, "category", Intent.CATEGORY_LAUNCHER);
        mConfigurationHelper.updateProperty(mCellId, "action", Intent.ACTION_MAIN);
        mConfigurationHelper.updateProperty(mCellId, "icon", "auto");
        mType = "app";
        mConfigurationHelper.updateProperty(mCellId, "intentType", mType);
        mConfigurationHelper.updateProperty(mCellId, "class", className);
        mConfigurationHelper.updateProperty(mCellId, "package", packageName);

        mCategory = Intent.CATEGORY_LAUNCHER;
        mCategoryPicker.setText(mCategory);

        mTitle = title;
        mTitleView.setText(title);

        mAction = Intent.ACTION_MAIN;
        mActionPicker.setText(mAction);

        mIcon = "auto";
        mIconPicker.setText(mIconDisplayNames[positionOfIcon()]);

        mClassName = className;
        mTargetClassPicker.setText(className);

        mPackage = packageName;
        mTargetPackagePicker.setText(mPackage);

        updateFieldVisibilityForType();
    }

    private void applyShortcutValues(Intent shortcutIntent, String shortcutName, Bitmap icon) {
        if (icon != null) {
            saveIcon(icon);
        } else {
            createIconFile(getActivity(), mCellId).delete();
        }

        String intent = shortcutIntent.toUri(0);
        mConfigurationHelper.updateProperty(mCellId, "intent", intent);
        mConfigurationHelper.updateProperty(mCellId, "icon", "auto");
        mType = "shortcut";
        mConfigurationHelper.updateProperty(mCellId, "intentType", mType);
        mConfigurationHelper.removeProperty(mCellId, "class");
        mConfigurationHelper.removeProperty(mCellId, "package");


        mTitle = shortcutName;
        mConfigurationHelper.updateProperty(mCellId, "title", shortcutName);
        mTitleView.setText(shortcutName);

        updateFieldVisibilityForType();
    }

    private String getTitleFromPackageManager(String packageName, String className) {
        PackageManager pm = getActivity().getPackageManager();
        ActivityInfo info = getComponentInfoByPackageNameAndClassName(pm, packageName, className);
        if (info == null) return null;
        return String.valueOf(info.loadLabel(pm));
    }

    private void saveIcon(Bitmap icon) {
        File target = createIconFile(getActivity(), mCellId);
        try {
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(target));
            try {
                icon.compress(Bitmap.CompressFormat.PNG, 100, out);
                out.flush();
            } finally {
                out.close();
            }
        } catch (IOException e) {
            Log.wtf("IntentEditor", "error saving bitmap icon", e);
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case R.id.action_pick_app:
            case R.id.action_pick_shortcut:
                confirmAction(itemId);
                return true;
        }
        return super.onOptionsItemSelected(item);

    }

    static class VisibilityHelper {

        final View mTypeContainer;

        final View mActionContainer;

        final View mCategoryContainer;

        final View mPackageContainer;

        final View mClassContainer;

        final View mCommandContainer;

        final View mAdvancedHeader;

        final View mRunAsRootView;

        VisibilityHelper(View view) {
            mTypeContainer = view.findViewById(R.id.type_container);
            mActionContainer = view.findViewById(R.id.action_container);
            mCategoryContainer = view.findViewById(R.id.category_container);
            mPackageContainer = view.findViewById(R.id.package_container);
            mClassContainer = view.findViewById(R.id.class_container);
            mCommandContainer = view.findViewById(R.id.command_container);
            mAdvancedHeader = view.findViewById(R.id.advanced_header);
            mRunAsRootView = view.findViewById(R.id.command_as_root);
        }


        void updateFieldVisibilityForType(String type, boolean showAdvanced) {

            switch (type) {
                case "shell":
                    if (showAdvanced) {
                        mTypeContainer.setVisibility(View.VISIBLE);
                        mAdvancedHeader.setVisibility(View.VISIBLE);
                    } else {
                        mTypeContainer.setVisibility(View.GONE);
                        mAdvancedHeader.setVisibility(View.GONE);
                    }
                    mRunAsRootView.setVisibility(View.VISIBLE);
                    mCommandContainer.setVisibility(View.VISIBLE);
                    mActionContainer.setVisibility(View.GONE);
                    mCategoryContainer.setVisibility(View.GONE);
                    mPackageContainer.setVisibility(View.GONE);
                    mClassContainer.setVisibility(View.GONE);
                    break;
                case "shortcut":
                    if (showAdvanced) {
                        mTypeContainer.setVisibility(View.VISIBLE);
                        mAdvancedHeader.setVisibility(View.VISIBLE);
                    } else {
                        mTypeContainer.setVisibility(View.GONE);
                        mAdvancedHeader.setVisibility(View.GONE);
                    }
                    mCommandContainer.setVisibility(View.GONE);
                    mRunAsRootView.setVisibility(View.GONE);
                    mActionContainer.setVisibility(View.GONE);
                    mCategoryContainer.setVisibility(View.GONE);
                    mPackageContainer.setVisibility(View.GONE);
                    mClassContainer.setVisibility(View.GONE);
                    break;
                default:
                    mCommandContainer.setVisibility(View.GONE);
                    mRunAsRootView.setVisibility(View.GONE);
                    if (showAdvanced) {
                        mTypeContainer.setVisibility(View.VISIBLE);
                        mActionContainer.setVisibility(View.VISIBLE);
                        mCategoryContainer.setVisibility(View.VISIBLE);
                        mPackageContainer.setVisibility(View.VISIBLE);
                        mClassContainer.setVisibility(View.VISIBLE);
                        mAdvancedHeader.setVisibility(View.VISIBLE);
                    } else {
                        mTypeContainer.setVisibility(View.GONE);
                        mActionContainer.setVisibility(View.GONE);
                        mCategoryContainer.setVisibility(View.GONE);
                        mPackageContainer.setVisibility(View.GONE);
                        mClassContainer.setVisibility(View.GONE);
                        mAdvancedHeader.setVisibility(View.GONE);
                    }
                    break;
            }


        }
    }

}