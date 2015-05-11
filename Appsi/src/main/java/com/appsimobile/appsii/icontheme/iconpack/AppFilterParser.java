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

package com.appsimobile.appsii.icontheme.iconpack;

import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.util.AttributeSet;
import android.util.Xml;

import com.appsimobile.appsii.compat.MapCompat;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Nick Martens on 9/25/13.
 */
public class AppFilterParser {


    public static AppFilterData parse(String packageName, Resources resources)
            throws IOException, XmlPullParserException {
        int id = resources.getIdentifier("appfilter", "xml", packageName);
        if (id == 0) return null;

        XmlResourceParser parser = resources.getXml(id);
        if (parser == null) return null;

        try {
            int eventType = parser.next();
            boolean inResources = false;

            while (!inResources) {
                if (eventType == XmlPullParser.START_TAG) {
                    String name = parser.getName();
                    if (name.equals("resources")) {
                        return parseResources(parser, resources);
                    }
                }

                eventType = parser.next();
                if (eventType == XmlPullParser.END_DOCUMENT) {
                    return null;
                }
            }
        } finally {
            parser.close();
        }

        return null;
    }

    private static AppFilterData parseResources(XmlResourceParser parser, Resources resources)
            throws IOException, XmlPullParserException {
        AppFilterData result = new AppFilterData(resources);
        int event = parser.next();
        String name = parser.getName();
        Pattern pattern = Pattern.compile("ComponentInfo\\{" +
                "(" +
                "[a-zA-Z0-9._]+" + // group 1, package name
                ")/(" +
                "[a-zA-Z0-9._]+" + // group 2, class name
                ")\\}");
        while (!(event == XmlPullParser.END_TAG && "resources".equals(name))) {
            if (event == XmlPullParser.START_TAG) {
                if ("iconback".equals(name)) {
                    int count = parser.getAttributeCount();
                    result.mIconBack = new String[count];
                    for (int i = 0; i < count; i++) {
                        result.mIconBack[i] = parser.getAttributeValue(i);
                    }
                } else if ("iconmask".equals(name)) {
                    int count = parser.getAttributeCount();
                    result.mIconMask = new String[count];
                    for (int i = 0; i < count; i++) {
                        result.mIconMask[i] = parser.getAttributeValue(i);
                    }
                } else if ("iconupon".equals(name)) {
                    int count = parser.getAttributeCount();
                    result.mIconUpon = new String[count];
                    for (int i = 0; i < count; i++) {
                        result.mIconUpon[i] = parser.getAttributeValue(i);
                    }
                } else if ("scale".equals(name)) {
                    int count = parser.getAttributeCount();
                    for (int i = 0; i < count; i++) {
                        String attrName = parser.getAttributeName(i);
                        if ("factor".equals(attrName)) {
                            try {
                                result.mScaleFactor = Float.parseFloat(parser.getAttributeValue(i));
                                break;
                            } catch (NumberFormatException e) {
                                // input was invalid, ignore and continue
                            }
                        }
                    }
                } else if ("item".equals(name)) {
                    AttributeSet set = Xml.asAttributeSet(parser);
                    String component = set.getAttributeValue(null, "component");
                    String drawable = set.getAttributeValue(null, "drawable");
                    if (drawable != null && component != null) {
                        Matcher matcher = pattern.matcher(component);
                        if (matcher.matches()) {
                            String className = matcher.group(2);
                            result.mIconNameMappings.put(className, drawable);
                        }
                    }
                }
            }
            event = parser.next();
            name = parser.getName();
        }
        return result;
    }

    public static class AppFilterData {

        String[] mIconBack;

        String[] mIconMask;

        String[] mIconUpon;

        float mScaleFactor;

        Map<String, String> mIconNameMappings = MapCompat.createMap();

        Resources mResources;

        public AppFilterData(Resources resources) {
            mResources = resources;
        }
    }
}
