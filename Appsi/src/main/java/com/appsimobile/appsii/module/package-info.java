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


/**
 * This package contains all core modules for Appsii.
 * Each of the sub-packages contains a feature and this package itself contains
 * several components that are used in multiple modules.
 * <p/>
 * The {@link com.appsimobile.appsii.PageController}'s implementation is the
 * core of each of the modules. It is modeled after the Fragment API and functions
 * somewhat similar.
 * One very important thing to note is that the implementation of the loadermanager
 * does not make ids unique across each of the instances. This allows Appsii to cache
 * the loaded data more efficiently and allows the pages to load a lot quicker.
 * The actual loaders are normal {@link android.content.Loader}s. This allows them
 * to be re-used across multiple parts of the application.
 * <p/>
 * The apps module contains the implementation of the Apps-page and the
 * AppsController class and everything else that is needed for the Apps-page.
 * <p/>
 * The same goes for the agenda, settings, sms, calls, home, people and search pages.
 * <p/>
 * Weather contains everything needed to show weather info and is completely stand-alone.
 * The Home module uses this module for everything that is weather related.
 * <p/>
 * The avatar package groups avatar related functions for the Calls and People module.
 * The Search module uses this as well.
 */
package com.appsimobile.appsii.module;