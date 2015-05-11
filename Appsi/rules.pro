# @Keep specifies not to shrink, optimize, or obfuscate the annotated class
# or class member as an entry point.

-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

-keep @proguard.annotation.Keep class *

-keepclassmembers class * {
    @proguard.annotation.Keep *;
}

# animations in the WindmillDrawable
-keep public class com.appsimobile.appsii.module.home.WindmillDrawable {
    public void setRotation(int);
}
# animations in the ExpandCollapseDrawable
-keep public class com.appsimobile.appsii.ExpandCollapseDrawable {
    public void setY1(float);
    public void setY2(float);
    public void setY3(float);
}
# animations in the PeopleController
-keep public class com.appsimobile.appsii.module.people.PeopleController {
    public void setToolbarY(int);
}

#needed for Google Play Services
-keep class * extends java.util.ListResourceBundle {
    protected Object[][] getContents();
}

-keep public class com.google.android.gms.common.internal.safeparcel.SafeParcelable {
    public static final *** NULL;
}

-keepnames @com.google.android.gms.common.annotation.KeepName class *
-keepclassmembernames class * {
    @com.google.android.gms.common.annotation.KeepName *;
}

-keepnames class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

-keepclassmembernames class * {
    @com.appsimobile.annotation.KeepName *;
}

# Needed for Rhino
-keep class org.mozilla.javascript.** { *; }
