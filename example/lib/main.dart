import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_app_installer/flutter_app_installer.dart';
import 'package:path/path.dart';
import 'package:path_provider/path_provider.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String androidAppId = 'com.felipheallef.tasks';
  String iOSAppId = '1440249706';

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Center(
          child: Column(
            children: <Widget>[
              SizedBox(height: 80),
              TextButton.icon(
                onPressed: () {
                  AppInstaller.goStore(androidAppId, iOSAppId);
                },
                icon: Icon(Icons.store),
                label: Text('Go to store'),
              ),
              SizedBox(height: 40),
              TextButton.icon(
                onPressed: () {
                  AppInstaller.goStore(androidAppId, iOSAppId, review: true);
                },
                icon: Icon(Icons.rate_review),
                label: Text('Go to store review'),
              ),
              SizedBox(height: 40),
              TextButton.icon(
                onPressed: () async {
                  final apk = await rootBundle.load('assets/app-release.apk');
                  final bytes = apk.buffer.asUint8List();
                  AppInstaller.installApkBytes(bytes, actionRequired: false);
                },
                icon: Icon(Icons.arrow_downward),
                label: Text('Install APK from assets'),
              ),
              SizedBox(height: 40),
              TextButton.icon(
                onPressed: () async {
                  final apk = await rootBundle.load('assets/app-release.apk');
                  final appDocDir = await getApplicationDocumentsDirectory();
                  final bytes = apk.buffer.asUint8List();

                  final path = join(appDocDir.path, 'app-release.apk');

                  File(path)
                    ..createSync()
                    ..writeAsBytesSync(bytes);

                  AppInstaller.installApk(path, actionRequired: false);
                },
                icon: Icon(Icons.arrow_downward),
                label: Text('Install APK from storage'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
