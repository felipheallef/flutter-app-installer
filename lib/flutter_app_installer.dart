import 'dart:async';
import 'dart:io';
import 'dart:typed_data';

import 'package:flutter/services.dart';

class AppInstaller {
  static const MethodChannel _channel =
      const MethodChannel('flutter_app_installer');

  ///去应用商店
  ///[androidAppId] Android package name
  ///[iOSAppId] iOS Bundle Id
  ///[review] iOS App Store evaluation
  static Future<void> goStore(String androidAppId, String iOSAppId,
      {bool review = false}) async {
    _channel.invokeMethod('goStore', {
      'androidAppId': androidAppId,
      'iOSAppId': iOSAppId,
      'review': review,
    });
  }

  ///安装 Apk
  ///[apkPath] Apk file path
  static Future<void> installApk(String apkPath,
      {bool actionRequired = true}) async {
    if (Platform.isAndroid) {
      _channel.invokeMethod('installApk', {
        'apkPath': apkPath,
        'actionRequired': actionRequired,
      });
    }
  }

  static Future<void> installApkBytes(Uint8List bytes,
      {bool actionRequired = true}) async {
    if (Platform.isAndroid) {
      _channel.invokeMethod('installApkBytes', {
        'apkBytes': bytes,
        'actionRequired': actionRequired,
      });
    }
  }
}
