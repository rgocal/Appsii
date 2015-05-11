# Appsii
This is the git-repository for Appsii.

Appsii is an open-source sidebar application for Android licensed under the APL-v2 license.

It is available in the [Google play store](https://play.google.com/store/apps/details?id=com.appsimobile.appsii) for free.

With Appsii you can access any resource on your device, no matter what you are doing.

# Development status
I consider Appsii's code very healty and not polluted. However, it could use a lot more tests as some parts are rather complex and not well covered. This is a known 'issue' I would like to resolve in the near future. 

# Development

This app is developed by Nick Martens (nick.martens1980@gmail.com). 

All development takes place in the master branch. Releases tags will be added for each version.

# Contributors

If you fixed a bug, or built a feature please send me a pull request. Code style is quite strict and is very close to the Android contributors code style.

Before sending me a pull request make sure your code is compatible with all supported API levels or at least fails gracefully. Meaning that certain features will only available at certain API levels and do not interfere with older API versions.

Also make sure all UI elements fully support right-to-left text as well as left-to-right.

# Building
Appsii should build just fine. To build a release version a file named release.properties is needed with the proper keys in it to sign the application. For the keys, take a look at the build.gradle file in the Appsi folder.

