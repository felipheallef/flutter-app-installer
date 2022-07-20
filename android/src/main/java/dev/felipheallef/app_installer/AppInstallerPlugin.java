package dev.felipheallef.app_installer;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageInstaller;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.content.FileProvider;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.List;
import java.util.Objects;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/**
 * AppInstallerPlugin
 */
public class AppInstallerPlugin implements FlutterPlugin, ActivityAware,
        MethodChannel.MethodCallHandler, PluginRegistry.ActivityResultListener,
        PluginRegistry.NewIntentListener {

    private Context applicationContext;
    private Activity mActivity;
    private MethodChannel methodChannel;

    private static final String TAG = "AppInstallerPlugin";
    private static final String PACKAGE_INSTALLED_ACTION =
            "dev.felipheallef.app_installer.SESSION_API_PACKAGE_INSTALLED";

    /**
     * Plugin registration.
     */
    public static void registerWith(Registrar registrar) {
        AppInstallerPlugin instance = new AppInstallerPlugin();
        instance.onAttachedToEngine(registrar.context(), registrar.messenger());
        instance.onAttachedToActivity(registrar.activity());
        registrar.addActivityResultListener(instance.getActivityResultListener());
    }

    @Override
    public void onAttachedToEngine(FlutterPluginBinding binding) {
        onAttachedToEngine(binding.getApplicationContext(), binding.getBinaryMessenger());
    }

    private void onAttachedToEngine(Context applicationContext, BinaryMessenger messenger) {
        this.applicationContext = applicationContext;
        methodChannel = new MethodChannel(messenger, "flutter_app_installer");
        methodChannel.setMethodCallHandler(this);
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        this.applicationContext = null;
        methodChannel.setMethodCallHandler(null);
        methodChannel = null;
    }

    @Override
    public void onAttachedToActivity(ActivityPluginBinding binding) {
        onAttachedToActivity(binding.getActivity());
        binding.addActivityResultListener(getActivityResultListener());
        binding.addOnNewIntentListener(getNewIntentListener());
    }

    private void onAttachedToActivity(Activity activity) {
        this.mActivity = activity;
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {

    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
        onAttachedToActivity(binding);
        binding.removeActivityResultListener(getActivityResultListener());
        binding.addActivityResultListener(getActivityResultListener());
        binding.removeOnNewIntentListener(getNewIntentListener());
        binding.addOnNewIntentListener(getNewIntentListener());

    }

    @Override
    public void onDetachedFromActivity() {
        this.mActivity = null;
    }

    /**
     * 创建 ActivityResult 监听
     *
     * @return ActivityResult 监听
     */
    private PluginRegistry.ActivityResultListener getActivityResultListener() {
        return this;
    }

    private PluginRegistry.NewIntentListener getNewIntentListener() {
        return this;
    }

    @Override
    public void onMethodCall(MethodCall call, @NonNull Result result) {
        String method = call.method;
        if (method.equals("goStore")) {
            String appId = (String) call.argument("androidAppId");
            goAppStore(mActivity, appId);
            result.success(true);
        } else if (method.equals("installApk")) {
            String filePath = call.argument("apkPath");
            Boolean actionRequired = call.argument("actionRequired");

            if (!TextUtils.isEmpty(filePath)) {
                installProcess(new File(filePath), actionRequired, result);
            } else {
                result.error("installApk", "apkPath is null", null);
            }
        } else if (method.equals("installApkBytes")) {
            byte[] fileBytes = call.argument("apkBytes");
            Boolean actionRequired = call.argument("actionRequired");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                installApkBytes(fileBytes, actionRequired, result);
            }
        } else {
            result.notImplemented();
        }
    }

    /**
     * 去应用商店
     *
     * @param appId appId
     */
    private void goAppStore(Activity activity, String appId) {
        String appPackageName;
        // 获取包名
        if (appId != null) {
            appPackageName = appId;
        } else {
            appPackageName = activity.getPackageName();
        }
        // 去应用商店
        Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName));
        marketIntent.addFlags(
                Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_DOCUMENT | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        activity.startActivity(marketIntent);
    }

    // 安装apkFile
    private File apkFile;
    // 回调处理
    private Result result;

    private PackageInstaller.Session session;

    /**
     * The process of installing the app
     *
     * @param apkFile apk 文件
     * @param result  返回结果
     */
    private void installProcess(File apkFile, Boolean actionRequired, Result result) {
        this.apkFile = apkFile;
        this.result = result;
        installApk(apkFile, actionRequired, result);
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void getPackageInstaller(Boolean actionRequired) throws IOException {
        PackageInstaller packageInstaller = mActivity.getPackageManager().getPackageInstaller();

        List<PackageInstaller.SessionInfo> sessions = packageInstaller.getMySessions();

        for (int i = 0; i < sessions.size(); i++) {
            int sessionId = sessions.get(i).getSessionId();
            packageInstaller.openSession(sessionId).abandon();
            Log.d(TAG, String.format("Closed old session %d.", sessionId));
        }

        PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(
                PackageInstaller.SessionParams.MODE_FULL_INSTALL);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            params.setRequireUserAction(actionRequired ?
                    PackageInstaller.SessionParams.USER_ACTION_REQUIRED
                    : PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED);
        }

        int sessionId = packageInstaller.createSession(params);
        session = packageInstaller.openSession(sessionId);

    }

    @SuppressLint("UnspecifiedImmutableFlag")
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void installApkBytes(byte[] bytes, Boolean actionRequired, Result result) {

        this.result = result;

        try {
            getPackageInstaller(actionRequired);
            addApkToInstallSession(bytes, session);

            // Create an install status receiver.
            Context context = mActivity;
            Intent intent = new Intent(mActivity, mActivity.getClass());
            intent.setAction(PACKAGE_INSTALLED_ACTION);
            PendingIntent pendingIntent;

            mActivity.startActivity(intent);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_MUTABLE);
            } else {
                pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
            }

            IntentSender statusReceiver = pendingIntent.getIntentSender();
            // Commit the session (this will start the installation workflow).
            session.commit(statusReceiver);
        } catch (IOException e) {
            result.error("installApkBytes", "Couldn't install package", null);
        } catch (RuntimeException e) {
            if (session != null) {
                session.abandon();
            }
            result.error("installApkBytes", "Couldn't install package", null);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void addApkToInstallSession(byte[] bytes, PackageInstaller.Session session)
            throws IOException {
        // It's recommended to pass the file size to openWrite(). Otherwise installation may fail
        // if the disk is almost full.
        try (OutputStream packageInSession = session.openWrite("package", 0, bytes.length)) {
            packageInSession.write(bytes, 0, bytes.length);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void addApkToInstallSession(File file, PackageInstaller.Session session)
            throws IOException {
        // It's recommended to pass the file size to openWrite(). Otherwise installation may fail
        // if the disk is almost full.

        try (OutputStream packageInSession = session.openWrite("package", 0, -1);
             InputStream is =
                     mActivity.getContentResolver().openInputStream(Uri.fromFile(file))) {
            byte[] buffer = new byte[16384];
            int n;
            while ((n = is.read(buffer)) >= 0) {
                packageInSession.write(buffer, 0, n);
            }
        }
    }

    /**
     * Install Apk
     *
     * @param apkFile installation file
     */
    private void installApk(File apkFile, Boolean actionRequired, Result result) {
        if (apkFile != null && apkFile.exists() && apkFile.length() > 0) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                installApkV21(apkFile, actionRequired, result);
            } else {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
                mActivity.startActivity(intent);
                if (result != null) {
                    result.success(true);
                }
            }
        } else {
            if (result != null) {
                result.success(false);
            }
        }
        this.apkFile = null;
        this.result = null;
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void installApkV21(File apkFile, Boolean actionRequired, Result result) {

        try {
            getPackageInstaller(actionRequired);
            addApkToInstallSession(apkFile, session);

            // Create an install status receiver.
            Context context = mActivity;
            Intent intent = new Intent(mActivity, mActivity.getClass());
            intent.setAction(PACKAGE_INSTALLED_ACTION);
            PendingIntent pendingIntent;

            mActivity.startActivity(intent);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_MUTABLE);
            } else {
                pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
            }

            IntentSender statusReceiver = pendingIntent.getIntentSender();
            // Commit the session (this will start the installation workflow).
            session.commit(statusReceiver);
        } catch (IOException e) {
            result.error("installApkBytes", "Couldn't install package", null);
        } catch (RuntimeException e) {
            if (session != null) {
                session.abandon();
            }
            result.error("installApkBytes", "Couldn't install package", null);
        }

        this.apkFile = null;
        this.result = null;
    }

    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent intent) {
        Log.d(TAG, String.format("Request code: %d", requestCode));
        if (requestCode == 10086 && resultCode == Activity.RESULT_OK) {
            installProcess(apkFile, false, result);
            return true;
        }
        return false;
    }

    @Override
    public boolean onNewIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        Log.d(TAG, String.format("Extras: %s", extras));
        if (PACKAGE_INSTALLED_ACTION.equals(intent.getAction()) && extras != null) {
            int status = extras.getInt(PackageInstaller.EXTRA_STATUS);
            String message = extras.getString(PackageInstaller.EXTRA_STATUS_MESSAGE);
            switch (status) {
                case PackageInstaller.STATUS_PENDING_USER_ACTION:
                    // This test app isn't privileged, so the user has to confirm the install.
                    Intent confirmIntent = (Intent) extras.get(Intent.EXTRA_INTENT);
                    mActivity.startActivity(confirmIntent);
                    break;
                case PackageInstaller.STATUS_SUCCESS:
                    if (result != null) {
//                        result.success();
                    }
                    break;
                case PackageInstaller.STATUS_FAILURE:
                case PackageInstaller.STATUS_FAILURE_ABORTED:
                case PackageInstaller.STATUS_FAILURE_BLOCKED:
                case PackageInstaller.STATUS_FAILURE_CONFLICT:
                case PackageInstaller.STATUS_FAILURE_INCOMPATIBLE:
                case PackageInstaller.STATUS_FAILURE_INVALID:
                case PackageInstaller.STATUS_FAILURE_STORAGE:
                    result.error("installApkBytes", "Install failed! " + status + ", " + message, null);
                    break;
                default:
                    result.error("installApkBytes", "Unrecognized status received from installer: " + status, null);
            }
        }

        return  true;
    }

}
