# flutter_app_installer

A Flutter plugin for installing and/or updating applications. Supports Android & iOS. Not all methods are supported on all platforms.

This is a fork of [app_installer](https://github.com/yy1300326388/app_installer).

### iOS

* Open AppStore app page
* Open AppStore app reviews

### Android

* Open google play app page
* Install Apk File (Detect unknown source)

## Getting Started

### Open App Store

```dart
/// App Info
String androidAppId = 'com.felipheallef.tasks';
String iOSAppId = '324684580';

AppInstaller.goStore(androidAppId, iOSAppId);
```

### Open Review

```dart
AppInstaller.goStore(androidAppId, iOSAppId, review: true);
```

### Install Apk (Android-only)

- From device storage:
> ⚠️You need to allow read storage permission first, otherwise there will be a parsing error ⚠️
```dart
AppInstaller.installApk('/sdcard/apk/app-debug.apk');
```

- From assets:
```dart
final file = DefaultAssetBundle.of(context).load('assets/apk/app-debug.apk');
final bytes = file.buffer.asUint8List();
AppInstaller.installApkBytes(bytes);
```

- Without user action (Android 12 or higher):
> If set, user action will not be required when all of the following conditions are met:
> - The app being installed targets API 30 or higher and is running on Android 12 or higher:
> - The app is the installer of record of an existing version of the app (in other words, this install session is an app update) or the installer is updating itself.
> - The installer declares the UPDATE_PACKAGES_WITHOUT_USER_ACTION permission.

```dart
AppInstaller.installApk('/sdcard/apk/app-debug.apk', actionRequired: false);
```


> AndroidManifest.xml

```xml
<!-- Provider -->
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileProvider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
```

> android/app/src/main/res/xml/file_paths.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <external-path path="Android/data/packagename/" name="files_root" />
    <external-path path="." name="external_storage_root" />
</paths>

//Replace packagename with your app package name
```

## Issues and feedback

Please file [issues](https://github.com/felipheallef/flutter-app-installer/issues/new) to send feedback or report a bug. Thank you!
