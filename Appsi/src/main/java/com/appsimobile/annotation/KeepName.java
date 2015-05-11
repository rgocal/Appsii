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

package com.appsimobile.annotation;

/**
 * The intention is to keep the class members annotated with this annotation
 * when proguard runs. However this does not seem to work right now
 * TODO: look into annotating proguard @keep and fix it.
 */
@java.lang.annotation.Target(
        {java.lang.annotation.ElementType.TYPE, java.lang.annotation.ElementType.FIELD,
                java.lang.annotation.ElementType.METHOD,
                java.lang.annotation.ElementType.CONSTRUCTOR})
@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.CLASS)
@java.lang.annotation.Documented
public @interface KeepName {

}