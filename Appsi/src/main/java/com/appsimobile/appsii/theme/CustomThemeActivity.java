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

package com.appsimobile.appsii.theme;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.colorpicker.ColorPickerDialog;
import com.android.colorpicker.ColorPickerSwatch;
import com.appsimobile.appsii.ActivityUtils;
import com.appsimobile.appsii.AppsiiUtils;
import com.appsimobile.appsii.DrawableCompat;
import com.appsimobile.appsii.R;
import com.appsimobile.appsii.ThemingUtils;
import com.appsimobile.appsii.dagger.AppInjector;

import javax.inject.Inject;

/**
 * An activity that allows the user to set-up a custom theme for Appsii.
 * <p/>
 * The trick is that the {@link ThemingUtils} class will simply create multiple
 * theme wrapper around the base theme; one for each of the themable properties
 * <p/>
 * Created by nick on 04/04/15.
 */
public class CustomThemeActivity extends Activity implements View.OnClickListener,
        Toolbar.OnMenuItemClickListener {

    /**
     * The set of views the user can click to choose the base theme
     */
    View mBaseThemeView;

    /**
     * The set of views the user can click to choose the primary color
     */
    View mPrimaryColorView;

    /**
     * The set of views the user can click to choose the accent color
     */
    View mAccentColorView;

    /**
     * The text-view that displays the name of the current base theme
     */
    TextView mBaseThemeTextView;

    /**
     * The text-view that displays the name of the current primary color
     */
    TextView mPrimaryColorTextView;

    /**
     * The text-view that displays the name of the current accent color
     */
    TextView mAccentColorTextView;

    /**
     * The ImageView that displays a color for the currently selected
     * base theme
     */
    ImageView mBaseThemeColorPreview;

    /**
     * The ImageView that displays a color for the currently selected
     * primary color
     */
    ImageView mPrimaryColorPreview;

    /**
     * The ImageView that displays a color for the currently selected
     * accent color
     */
    ImageView mAccentColorPreview;

    @Inject
    SharedPreferences mPreferences;

    @Inject
    AppsiiUtils mAppsiiUtils;

    /**
     * The possible values for the base themes. As defined in a string-array
     */
    String[] mBaseThemeValues;

    /**
     * The possible titles for the base themes. As defined in a string-array
     */
    String[] mBaseThemeNames;

    /**
     * The possible values for the primary color. As defined in a string-array
     */
    String[] mPrimaryColorValues;

    /**
     * The possible titles for the primary color. As defined in a string-array
     */
    String[] mPrimaryColorNames;

    /**
     * The possible values for the accent color. As defined in a string-array
     */
    String[] mAccentColorValues;

    /**
     * The possible titles for the accent color. As defined in a string-array
     */
    String[] mAccentColorNames;


    /**
     * The current base theme (value)
     */
    String mBaseTheme;

    /**
     * A listener that is called when the base theme is changed
     */
    final ColorPickerSwatch.OnColorSelectedListener mBaseThemeListener =
            new ColorPickerSwatch.OnColorSelectedListener() {

                @Override
                public void onColorSelected(int color) {
                    onBaseThemePicked(color);
                }
            };

    /**
     * The current primary color (value)
     */
    String mPrimaryColor;

    /**
     * A listener that is called when the primary color is changed
     */
    final ColorPickerSwatch.OnColorSelectedListener mPrimaryColorListener =
            new ColorPickerSwatch.OnColorSelectedListener() {

                @Override
                public void onColorSelected(int color) {
                    onPrimaryColorPicked(color);
                }
            };

    /**
     * The current accent color (value)
     */
    String mAccentColor;

    /**
     * A listener that is called when the accent color is changed
     */
    final ColorPickerSwatch.OnColorSelectedListener mAccentColorListener =
            new ColorPickerSwatch.OnColorSelectedListener() {

                @Override
                public void onColorSelected(int color) {
                    onAccentColorPicked(color);
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppInjector.inject(this);

        ActivityUtils.setContentView(this, R.layout.activity_customize_theme);

        mBaseThemeView = findViewById(R.id.base_theme);
        mPrimaryColorView = findViewById(R.id.primary_color);
        mAccentColorView = findViewById(R.id.accent_color_container);

        mBaseThemeTextView = (TextView) findViewById(R.id.theme_value);
        mPrimaryColorTextView = (TextView) findViewById(R.id.primary_color_value);
        mAccentColorTextView = (TextView) findViewById(R.id.accent_value);

        mBaseThemeColorPreview = (ImageView) findViewById(R.id.base_theme_preview);
        mPrimaryColorPreview = (ImageView) findViewById(R.id.primary_color_preview);
        mAccentColorPreview = (ImageView) findViewById(R.id.accent_color_preview);

        Resources resources = getResources();
        mBaseThemeValues = resources.getStringArray(R.array.base_theme_values);
        mBaseThemeNames = resources.getStringArray(R.array.base_theme_names);

        mPrimaryColorValues = resources.getStringArray(R.array.primary_colors_values);
        mPrimaryColorNames = resources.getStringArray(R.array.primary_color_names);

        mAccentColorValues = resources.getStringArray(R.array.accent_colors_values);
        mAccentColorNames = resources.getStringArray(R.array.accent_color_names);


        mBaseThemeView.setOnClickListener(this);
        mPrimaryColorView.setOnClickListener(this);
        mAccentColorView.setOnClickListener(this);

        Toolbar toolbar = ActivityUtils.getToolbarPlain(this, R.id.toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onNavigateUp();
            }
        });
        MenuInflater menuInflater = new MenuInflater(this);
        menuInflater.inflate(R.menu.theme_editor, toolbar.getMenu());
        toolbar.setOnMenuItemClickListener(this);

        String baseTheme = mPreferences.getString(
                ThemingUtils.PREF_APPSII_BASE_THEME, ThemingUtils.DEFAULT_BASE_THEME);
        String primary = mPreferences.getString(
                ThemingUtils.PREF_APPSII_COLOR_PRIMARY, ThemingUtils.DEFAULT_COLOR_PRIMARY);
        String accent = mPreferences.getString(
                ThemingUtils.PREF_APPSII_COLOR_ACCENT, ThemingUtils.DEFAULT_COLOR_ACCENT);

        mBaseTheme = baseTheme;
        mPrimaryColor = primary;
        mAccentColor = accent;

        setupBaseTheme(baseTheme);
        setupPrimaryColor(primary);
        setupAccentColor(accent);

        ColorPickerDialog primaryColorPicker =
                (ColorPickerDialog) getFragmentManager().findFragmentByTag("primary_color_picker");
        if (primaryColorPicker != null) {
            primaryColorPicker.setOnColorSelectedListener(mPrimaryColorListener);
        }

        ColorPickerDialog accentColorPicker =
                (ColorPickerDialog) getFragmentManager().findFragmentByTag("accent_color_picker");
        if (accentColorPicker != null) {
            accentColorPicker.setOnColorSelectedListener(mAccentColorListener);
        }

        ColorPickerDialog baseThemePicker =
                (ColorPickerDialog) getFragmentManager().findFragmentByTag("base_theme_picker");
        if (baseThemePicker != null) {
            baseThemePicker.setOnColorSelectedListener(mBaseThemeListener);
        }

    }

    private void setupBaseTheme(String baseTheme) {
        int idx = indexOf(mBaseThemeValues, baseTheme);
        String name = mBaseThemeNames[idx];
        mBaseThemeTextView.setText(name);
        Drawable drawable = mBaseThemeColorPreview.getDrawable();
        int color;
        color = getColorForBaseTheme(baseTheme);
        DrawableCompat.setTintColorCompat(drawable, color);
    }

    private void setupPrimaryColor(String primaryColor) {
        int idx = indexOf(mPrimaryColorValues, primaryColor);
        String name = mPrimaryColorNames[idx];
        mPrimaryColorTextView.setText(name);
        Drawable drawable = mPrimaryColorPreview.getDrawable();
        int color = ThemingUtils.getPrimaryColorValue(primaryColor);
        DrawableCompat.setTintColorCompat(drawable, color);
    }

    private void setupAccentColor(String accentColor) {
        int idx = indexOf(mAccentColorValues, accentColor);
        String name = mAccentColorNames[idx];
        mAccentColorTextView.setText(name);
        Drawable drawable = mAccentColorPreview.getDrawable();
        int color = ThemingUtils.getPrimaryColorValue(accentColor);
        DrawableCompat.setTintColorCompat(drawable, color);
    }

    <T> int indexOf(T[] array, T value) {
        int N = array.length;
        for (int i = 0; i < N; i++) {
            if (value.equals(array[i])) return i;
        }
        return -1;
    }

    private int getColorForBaseTheme(String baseTheme) {
        int color;
        switch (baseTheme) {
            case "theme_dark":
                color = 0xFF333333;
                break;
            case "theme_light":
                color = 0xFFCCCCCC;
                break;
            default:
            case "theme_dark_light_ab":
                color = 0xFF666666;
                break;
            case "theme_light_dark_ab":
                color = 0xFF999999;
                break;
        }
        return color;
    }

    void onBaseThemePicked(int color) {
        String baseTheme = getBaseThemeByColor(color);
        mBaseTheme = baseTheme;
        setupBaseTheme(baseTheme);
        mPreferences.edit().
                putString(ThemingUtils.PREF_APPSII_THEME, "custom").
                putString(ThemingUtils.PREF_APPSII_BASE_THEME, baseTheme).
                apply();
        mAppsiiUtils.restartAppsi(this);
    }

    private String getBaseThemeByColor(final int color) {
        String baseTheme;
        switch (color) {
            case 0xFF333333:
                baseTheme = "theme_dark";
                break;
            case 0xFFCCCCCC:
                baseTheme = "theme_light";
                break;
            default:
            case 0xFF666666:
                baseTheme = "theme_dark_light_ab";
                break;
            case 0xFF999999:
                baseTheme = "theme_light_dark_ab";
                break;
        }
        return baseTheme;
    }

    void onPrimaryColorPicked(int color) {
        int[] primaryColors = ThemingUtils.createPrimaryColorArray(mPrimaryColorValues);
        int idx = indexOf(primaryColors, color);
        String primaryColorValue = mPrimaryColorValues[idx];
        mPrimaryColor = primaryColorValue;
        setupPrimaryColor(primaryColorValue);
        mPreferences.edit().
                putString(ThemingUtils.PREF_APPSII_THEME, "custom").
                putString(ThemingUtils.PREF_APPSII_COLOR_PRIMARY, primaryColorValue).
                apply();
        mAppsiiUtils.restartAppsi(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.base_theme:
                showBaseThemePicker();
                break;
            case R.id.primary_color:
                showPrimaryColorPicker();
                break;
            case R.id.accent_color_container:
                showAccentColorPicker();
                break;
        }
    }

    int indexOf(int[] array, int value) {
        int N = array.length;
        for (int i = 0; i < N; i++) {
            if (value == array[i]) return i;
        }
        return -1;
    }

    private void showBaseThemePicker() {
        int[] colors = {0xFFCCCCCC, 0xFF999999, 0xFF666666, 0xFF333333};

        int selectedColor = getColorForBaseTheme(mBaseTheme);
        ColorPickerDialog colorPickerDialog = ColorPickerDialog.newInstance(
                R.string.base_theme, colors, selectedColor, 4, ColorPickerDialog.SIZE_SMALL);

        colorPickerDialog.show(getFragmentManager(), "base_theme_picker");
        colorPickerDialog.setOnColorSelectedListener(mBaseThemeListener);

    }

    void onAccentColorPicked(int color) {
        int[] accentColors = ThemingUtils.createAccentColorArray(false, mAccentColorValues);
        int idx = indexOf(accentColors, color);
        String accentColorValue = mAccentColorValues[idx];
        mAccentColor = accentColorValue;
        setupAccentColor(accentColorValue);
        mPreferences.edit().
                putString(ThemingUtils.PREF_APPSII_THEME, "custom").
                putString(ThemingUtils.PREF_APPSII_COLOR_ACCENT, accentColorValue).
                apply();
        mAppsiiUtils.restartAppsi(this);
    }

    private void showPrimaryColorPicker() {
        int[] colors = ThemingUtils.createPrimaryColorArray(mPrimaryColorValues);
        int selectedColor = ThemingUtils.getPrimaryColorValue(mPrimaryColor);
        ColorPickerDialog colorPickerDialog = ColorPickerDialog.newInstance(
                R.string.primary_color, colors, selectedColor, 4, ColorPickerDialog.SIZE_SMALL);

        colorPickerDialog.show(getFragmentManager(), "primary_color_picker");
        colorPickerDialog.setOnColorSelectedListener(mPrimaryColorListener);
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        int id = menuItem.getItemId();
        switch (id) {
            case R.id.theme_teal:
                applyPreset("theme_light_dark_ab", "teal", "deep_orange");
                return true;
            case R.id.theme_teal_dark:
                applyPreset("theme_dark", "teal", "deep_orange");
                return true;
            case R.id.theme_light_green:
                applyPreset("theme_dark_light_ab", "light_green", "yellow");
                return true;
            case R.id.theme_blue_grey:
                applyPreset("theme_dark", "blue_grey", "orange");
                return true;
            case R.id.theme_orange:
                applyPreset("theme_light", "orange", "deep_purple");
                return true;
        }
        return false;
    }

    private void showAccentColorPicker() {
        int[] colors = ThemingUtils.createAccentColorArray(false, mAccentColorValues);
        int selectedColor = ThemingUtils.getAccentColorValue(false, mAccentColor);
        ColorPickerDialog colorPickerDialog = ColorPickerDialog.newInstance(
                R.string.accent_color, colors, selectedColor, 4, ColorPickerDialog.SIZE_SMALL);

        colorPickerDialog.show(getFragmentManager(), "accent_color_picker");
        colorPickerDialog.setOnColorSelectedListener(mAccentColorListener);
    }

    private void applyPreset(String baseTheme, String primaryColor, String accentColor) {
        mPreferences.edit().
                putString(ThemingUtils.PREF_APPSII_THEME, "custom").
                putString(ThemingUtils.PREF_APPSII_BASE_THEME, baseTheme).
                putString(ThemingUtils.PREF_APPSII_COLOR_PRIMARY, primaryColor).
                putString(ThemingUtils.PREF_APPSII_COLOR_ACCENT, accentColor).
                apply();
        // When the theme has been changed, Appsii needs to be restarted.
        // This is because it's context needs to be changed. And all of the
        // views are depending on these values in the theme.
        mAppsiiUtils.restartAppsi(this);
        mPrimaryColor = primaryColor;
        mAccentColor = accentColor;
        mBaseTheme = baseTheme;

        setupBaseTheme(baseTheme);
        setupPrimaryColor(primaryColor);
        setupAccentColor(accentColor);
    }


}
