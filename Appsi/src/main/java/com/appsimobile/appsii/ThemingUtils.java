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

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.StyleRes;
import android.util.Log;
import android.view.ContextThemeWrapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Nick Martens on 6/21/13.
 */
public class ThemingUtils {

    public static final String PREF_APPSII_THEME = "pref_appsii_theme";

    public static final String PREF_APPSII_BASE_THEME = "pref_appsii_base_theme";

    public static final String PREF_APPSII_COLOR_PRIMARY = "pref_appsii_color_primary";

    public static final String PREF_APPSII_COLOR_ACCENT = "pref_appsii_color_accent";

    public static final String DEFAULT_APPSII_THEME = "theme_material_teal";

    public static final String DEFAULT_BASE_THEME = "theme_light_dark_ab";

    public static final String DEFAULT_COLOR_PRIMARY = "teal";

    public static final String DEFAULT_COLOR_ACCENT = "orange";

    public static Context createContextThemeWrapper(Context context, SharedPreferences prefs) {

        String theme = prefs.getString(PREF_APPSII_THEME, DEFAULT_APPSII_THEME);
        String baseTheme = prefs.getString(PREF_APPSII_BASE_THEME, DEFAULT_BASE_THEME);
        String primary = prefs.getString(PREF_APPSII_COLOR_PRIMARY, DEFAULT_COLOR_PRIMARY);
        String accent = prefs.getString(PREF_APPSII_COLOR_ACCENT, DEFAULT_COLOR_ACCENT);
        return createContextThemeWrapper(context, theme, baseTheme, primary, accent);
    }

    public static Context createContextThemeWrapper(Context context,
            String presetTheme, String theme, String primary, String accent) {

        Context result = getThemeResFallback(context, presetTheme);
        if (result == null) {
            boolean isDark = "theme_dark".equals(theme) || "theme_dark_light_ab".equals(theme);

            int base = getBaseTheme(theme);

            int primaryColorTheme = getPrimaryColorTheme(primary);

            // TODO: add dark param
            int accentColorTheme = getAccentColorTheme(isDark, accent);

            result = new ContextThemeWrapper(context, base);
            result = new ContextThemeWrapper(result, accentColorTheme);
            return new ContextThemeWrapper(result, primaryColorTheme);
        }

        return result;

    }

    private static Context getThemeResFallback(Context context, String theme) {
        ContextThemeWrapper result;
        switch (theme) {
            case "theme_material_teal":
                result = new ContextThemeWrapper(
                        context, R.style.Appsi_Sidebar_Material_LightDarkActionBar);
                result = new ContextThemeWrapper(result, R.style.Palette_Accent_DeepOrange);
                return new ContextThemeWrapper(result, R.style.Palette_Primary_Teal);
            case "theme_material_teal_dark":
                result = new ContextThemeWrapper(
                        context, R.style.Appsi_Sidebar_Material_Dark);
                result = new ContextThemeWrapper(result, R.style.Palette_Accent_DeepOrange200);
                return new ContextThemeWrapper(result, R.style.Palette_Primary_Teal);
            case "theme_material_light_green":
                result = new ContextThemeWrapper(
                        context, R.style.Appsi_Sidebar_Material_DarkLightActionBar);
                result = new ContextThemeWrapper(result, R.style.Palette_Accent_Yellow);
                return new ContextThemeWrapper(result, R.style.Palette_Primary_LightGreen);
            case "theme_material_blue_grey":
                result = new ContextThemeWrapper(
                        context, R.style.Appsi_Sidebar_Material_Dark);
                result = new ContextThemeWrapper(result, R.style.Palette_Accent_Orange700);
                return new ContextThemeWrapper(result, R.style.Palette_Primary_BlueGrey);
            case "theme_material_orange":
                result = new ContextThemeWrapper(
                        context, R.style.Appsi_Sidebar_Material_Light);
                result = new ContextThemeWrapper(result, R.style.Palette_Accent_DeepPurple);
                return new ContextThemeWrapper(result, R.style.Palette_Primary_Orange);
        }
        return null;
    }

    @StyleRes
    private static int getBaseTheme(String theme) {
        switch (theme) {
            case "theme_dark":
                return R.style.Appsi_Sidebar_Material_Dark;
            case "theme_light":
                return R.style.Appsi_Sidebar_Material_Light;
            case "theme_dark_light_ab":
                return R.style.Appsi_Sidebar_Material_DarkLightActionBar;
            case "theme_light_dark_ab":
                return R.style.Appsi_Sidebar_Material_LightDarkActionBar;
        }
        return R.style.Appsi_Sidebar_Material_Light;
    }

    @StyleRes
    private static int getPrimaryColorTheme(String primary) {
        if (primary == null) {
            return R.style.Palette_Primary_Teal;
        }
        switch (primary) {
            case "red":
                return R.style.Palette_Primary_Red;
            case "pink":
                return R.style.Palette_Primary_Pink;
            case "purple":
                return R.style.Palette_Primary_Purple;

            case "deep_purple":
                return R.style.Palette_Primary_DeepPurple;
            case "indigo":
                return R.style.Palette_Primary_Indigo;
            case "blue":
                return R.style.Palette_Primary_Blue;

            case "light_blue":
                return R.style.Palette_Primary_LightBlue;
            case "cyan":
                return R.style.Palette_Primary_Cyan;
            default:
            case "teal":
                return R.style.Palette_Primary_Teal;

            case "green":
                return R.style.Palette_Primary_Green;
            case "light_green":
                return R.style.Palette_Primary_LightGreen;
            case "lime":
                return R.style.Palette_Primary_Lime;

            case "yellow":
                return R.style.Palette_Primary_Yellow;
            case "amber":
                return R.style.Palette_Primary_Amber;
            case "orange":
                return R.style.Palette_Primary_Orange;

            case "deep_orange":
                return R.style.Palette_Primary_DeepOrange;
            case "brown":
                return R.style.Palette_Primary_Brown;
            case "grey":
                return R.style.Palette_Primary_Grey;
            case "blue_grey":
                return R.style.Palette_Primary_BlueGrey;
        }
    }

    @StyleRes
    private static int getAccentColorTheme(boolean isDark, String accent) {
        if (accent == null) {
            return isDark ? R.style.Palette_Accent_Orange700 : R.style.Palette_Accent_Orange;
        }
        switch (accent) {
            case "red":
                return isDark ? R.style.Palette_Accent_Red100 : R.style.Palette_Accent_Red;
            case "pink":
                return isDark ? R.style.Palette_Accent_Pink100 : R.style.Palette_Accent_Pink;
            case "purple":
                return isDark ? R.style.Palette_Accent_Purple100 : R.style.Palette_Accent_Purple;

            case "deep_purple":
                return isDark ? R.style.Palette_Accent_DeepPurple100 :
                        R.style.Palette_Accent_DeepPurple;
            case "indigo":
                return isDark ? R.style.Palette_Accent_Indigo100 : R.style.Palette_Accent_Indigo;
            case "blue":
                return isDark ? R.style.Palette_Accent_Blue100 : R.style.Palette_Accent_Blue;

            case "light_blue":
                return isDark ? R.style.Palette_Accent_LightBlue400 :
                        R.style.Palette_Accent_LightBlue;
            case "cyan":
                return isDark ? R.style.Palette_Accent_Cyan700 : R.style.Palette_Accent_Cyan;
            case "teal":
                return isDark ? R.style.Palette_Accent_Teal400 : R.style.Palette_Accent_Teal;

            case "green":
                return isDark ? R.style.Palette_Accent_Green400 : R.style.Palette_Accent_Green;
            case "light_green":
                return isDark ? R.style.Palette_Accent_LightGreen400 :
                        R.style.Palette_Accent_LightGreen;
            case "lime":
                return isDark ? R.style.Palette_Accent_Lime400 : R.style.Palette_Accent_Lime;

            case "yellow":
                return isDark ? R.style.Palette_Accent_Yellow400 : R.style.Palette_Accent_Yellow;
            case "amber":
                return isDark ? R.style.Palette_Accent_Amber400 : R.style.Palette_Accent_Amber;
            default:
            case "orange":
                return isDark ? R.style.Palette_Accent_Orange700 : R.style.Palette_Accent_Orange;

            case "deep_orange":
                return isDark ? R.style.Palette_Accent_DeepOrange200 :
                        R.style.Palette_Accent_DeepOrange;
        }
    }

    public static Drawable createButtonDrawable(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return context.getDrawable(R.drawable.button_bg_teal);
        }

        TypedArray a = context.obtainStyledAttributes(new int[]{
                R.attr.colorPrimary,
                R.attr.colorAccent,
                R.attr.colorPrimaryDark,
        });

        int tintColor = a.getColor(0, Color.RED);
        int darkColor = a.getColor(2, Color.RED);

        a.recycle();

        return createTintedButtonDrawable(context, tintColor, darkColor);
    }

    private static Drawable createTintedButtonDrawable(Context context, int tintColor,
            int darkColor) {
        Drawable normalState = getButtonStateDrawable(context, tintColor);
        Drawable pressedState = getButtonStateDrawable(context, darkColor);
        Drawable focusedState = getButtonStateDrawable(context, darkColor);

        StateListDrawable result = new StateListDrawable();
        result.addState(new int[]{android.R.attr.state_pressed}, pressedState);
        result.addState(new int[]{android.R.attr.state_focused}, focusedState);
        result.addState(new int[]{}, normalState);

        return result;
    }

    private static Drawable getButtonStateDrawable(Context context, int color) {
        Resources res = context.getResources();
        int r = res.getDimensionPixelSize(R.dimen.control_corner_material);

        float[] roundedCorner = new float[]{r, r, r, r, r, r, r, r};
        float[] innerRoundedCorner = new float[]{0, 0, 0, 0, 0, 0, 0, 0};
        RoundRectShape shape = new RoundRectShape(roundedCorner, null, innerRoundedCorner);

        ShapeDrawable shapeDrawable = new ShapeDrawable(shape);

        int paddingVertical = res.getDimensionPixelOffset(
                R.dimen.button_padding_vertical_material);
        int paddingHorizontal = res.getDimensionPixelOffset(
                R.dimen.button_padding_horizontal_material);
        shapeDrawable.setPadding(paddingHorizontal, paddingVertical, paddingVertical,
                paddingHorizontal);
        shapeDrawable.getPaint().setColor(color);

        int insetHorizontal = res.getDimensionPixelSize(
                R.dimen.button_inset_horizontal_material);
        int insetVertical = res
                .getDimensionPixelSize(R.dimen.button_inset_vertical_material);

        InsetDrawable inset =
                new InsetDrawable(shapeDrawable, insetHorizontal, insetVertical, insetHorizontal,
                        insetVertical);
        return inset;
    }

    public static int[] createPrimaryColorArray(String[] colorNames) {
        int N = colorNames.length;
        int[] colors = new int[N];
        for (int i = 0; i < N; i++) {
            colors[i] = getPrimaryColorValue(colorNames[i]);
        }
        return colors;
    }

    public static int getPrimaryColorValue(String primary) {
        if (primary == null) {
            primary = DEFAULT_COLOR_PRIMARY;
        }
        switch (primary) {
            case "red":
                return 0xFFF44336;
            case "pink":
                return 0xFFE91E63;
            case "purple":
                return 0xFF9C27B0;

            case "deep_purple":
                return 0xFF673AB7;
            case "indigo":
                return 0xFF3F51B5;
            case "blue":
                return 0xFF2196F3;

            case "light_blue":
                return 0xFF03A9F4;
            case "cyan":
                return 0xFF00BCD4;
            default:
            case "teal":
                return 0xFF009688;

            case "green":
                return 0xFF4CAF50;
            case "light_green":
                return 0xFF8BC34A;
            case "lime":
                return 0xFFCDDC39;

            case "yellow":
                return 0xFFFFEB3B;
            case "amber":
                return 0xFFFFC107;
            case "orange":
                return 0xFFFF9800;

            case "deep_orange":
                return 0xFFFF5722;
            case "brown":
                return 0xFF795548;
            case "grey":
                return 0xFF9E9E9E;
            case "blue_grey":
                return 0xFF607D8B;
        }
    }

    public static int[] createAccentColorArray(boolean dark, String[] colorNames) {
        int N = colorNames.length;
        int[] colors = new int[N];
        for (int i = 0; i < N; i++) {
            colors[i] = getAccentColorValue(dark, colorNames[i]);
        }
        return colors;
    }

    public static int getAccentColorValue(boolean isDark, String accent) {
        if (accent == null) {
            accent = DEFAULT_COLOR_ACCENT;
        }
        switch (accent) {
            case "red":
                return isDark ? 0xFFFF1744 : 0xFFFF8A80;
            case "pink":
                return isDark ? 0xFFFF80AB : 0xFFF50057;
            case "purple":
                return isDark ? 0xFFEA80FC : 0xFFD500F9;

            case "deep_purple":
                return isDark ? 0xFFB388FF : 0xFF651FFF;
            case "indigo":
                return isDark ? 0xFF8C9EFF : 0xFF3D5AFE;
            case "blue":
                return isDark ? 0xFF82B1FF : 0xFF2979FF;

            case "light_blue":
                return isDark ? 0xFF00B0FF : 0xFF0091EA;
            case "cyan":
                return isDark ? 0xFF00E5FF : 0xFF00B8D4;
            case "teal":
                return isDark ? 0xFF1DE9B6 : 0xFF00BFA5;

            case "green":
                return isDark ? 0xFF00E676 : 0xFF00C853;
            case "light_green":
                return isDark ? 0xFF76FF03 : 0xFF64DD17;
            case "lime":
                return isDark ? 0xFFC6FF00 : 0xFFAEEA00;

            case "yellow":
                return isDark ? 0xFFFFEA00 : 0xFFFFD600;
            case "amber":
                return isDark ? 0xFFFFC400 : 0xFFFFAB00;
            default:
            case "orange":
                return isDark ? 0xFFFF6D00 : 0xFFFF9100;

            case "deep_orange":
                return isDark ? 0xFFFF6E40 : 0xFFFF3D00;
        }
    }

    public static boolean canUseFadingEdges(SharedPreferences preferences) {
        return false;
        /*
        boolean useBackground = preferences.getBoolean("pref_sidebar_custom_background", false);
        return !useBackground;
        */
    }

    static int getAlpha(int preferenceValue) {
        preferenceValue = 100 - Math.min(preferenceValue, 100);
        return (preferenceValue * 255) / 100;
    }

    static float getPercentage(int preferenceValue) {
        preferenceValue = Math.min(preferenceValue, 100);
        return (preferenceValue) / 100f;
    }

    public static Bitmap getSidebarWallpaper(Context context, SharedPreferences preferences,
            int sidebarWidth, int sidebarHeight) {
        return getSidebarWallpaperApi10(context, preferences, sidebarWidth, sidebarHeight);

    }

    @TargetApi(10)
    private static Bitmap getSidebarWallpaperApi10(Context context, SharedPreferences preferences,
            int sidebarWidth, int sidebarHeight) {
        boolean useBackground = preferences.getBoolean("pref_sidebar_custom_background", false);
        if (!useBackground) return null;

        String path = preferences.getString("pref_sidebar_background_image", null);
        if (path == null) return null;

        try {
            BitmapFactory.Options bounds = decodeBounds(createWallpaperInputStream(context, path));
            int decodeHeight = Math.min(bounds.outHeight, sidebarHeight);
            int decodeWidth = Math.min(bounds.outWidth, sidebarWidth);
            InputStream in = createWallpaperInputStream(context, path);
            try {
                BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(in, false);
                BitmapFactory.Options options = new BitmapFactory.Options();
                return decoder.decodeRegion(new Rect(0, 0, decodeWidth, decodeHeight), options);
            } finally {
                in.close();
            }
        } catch (IOException e) {
            return null;
        }

    }

    static BitmapFactory.Options decodeBounds(InputStream in) throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        try {
            // Decode image size
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(in, null, options);
            return options;
        } finally {
            in.close();
        }
    }

    public static InputStream createWallpaperInputStream(Context context, String preferenceValue)
            throws FileNotFoundException {
        File file = new File(preferenceValue);
        if (file.exists()) {
            return new FileInputStream(file);
        }

        Uri uri = Uri.parse(preferenceValue);
        ContentResolver contentResolver = context.getContentResolver();
        try {
            return contentResolver.openInputStream(uri);
        } catch (SecurityException e) {
            Log.i("ThemingUtils", "unable to open background image", e);
            FileNotFoundException fileNotFoundException = new FileNotFoundException(e.getMessage());
            fileNotFoundException.initCause(e);
            throw fileNotFoundException;
        }
    }

}
