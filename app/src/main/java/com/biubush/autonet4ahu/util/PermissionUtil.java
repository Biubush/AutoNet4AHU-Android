package com.biubush.autonet4ahu.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 权限管理工具类，处理Android 13所需的权限
 */
public class PermissionUtil {
    
    // 网络权限
    public static final String PERMISSION_INTERNET = Manifest.permission.INTERNET;
    public static final String PERMISSION_ACCESS_NETWORK_STATE = Manifest.permission.ACCESS_NETWORK_STATE;
    public static final String PERMISSION_ACCESS_WIFI_STATE = Manifest.permission.ACCESS_WIFI_STATE;
    public static final String PERMISSION_CHANGE_WIFI_STATE = Manifest.permission.CHANGE_WIFI_STATE;
    public static final String PERMISSION_CHANGE_NETWORK_STATE = Manifest.permission.CHANGE_NETWORK_STATE;
    
    // 位置权限（在某些设备上获取WiFi信息需要）
    public static final String PERMISSION_ACCESS_FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    public static final String PERMISSION_ACCESS_COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    
    // Android 13 通知权限
    public static final String PERMISSION_POST_NOTIFICATIONS = "android.permission.POST_NOTIFICATIONS";
    
    // 存储权限（用于日志保存）
    public static final String PERMISSION_WRITE_EXTERNAL_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE;
    
    // 前台服务权限（用于保持登录服务运行）
    public static final String PERMISSION_FOREGROUND_SERVICE = Manifest.permission.FOREGROUND_SERVICE;
    
    // 开机自启动和网络变化接收相关权限
    public static final String PERMISSION_RECEIVE_BOOT_COMPLETED = Manifest.permission.RECEIVE_BOOT_COMPLETED;

    /**
     * 检查并请求必要权限
     *
     * @param activity 活动
     * @param requestCode 请求码
     * @return 如果所有权限都已授予，返回true；否则返回false
     */
    public static boolean checkAndRequestPermissions(Activity activity, int requestCode) {
        List<String> permissions = new ArrayList<>();
        
        // 基本网络权限
        permissions.add(PERMISSION_INTERNET);
        permissions.add(PERMISSION_ACCESS_NETWORK_STATE);
        permissions.add(PERMISSION_ACCESS_WIFI_STATE);
        permissions.add(PERMISSION_CHANGE_WIFI_STATE);
        permissions.add(PERMISSION_CHANGE_NETWORK_STATE);
        
        // 位置权限（获取WiFi信息）
        permissions.add(PERMISSION_ACCESS_FINE_LOCATION);
        permissions.add(PERMISSION_ACCESS_COARSE_LOCATION);
        
        // 前台服务权限（Android 9+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            permissions.add(PERMISSION_FOREGROUND_SERVICE);
        }
        
        // 开机自启动权限
        permissions.add(PERMISSION_RECEIVE_BOOT_COMPLETED);
        
        // Android 13通知权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(PERMISSION_POST_NOTIFICATIONS);
        }
        
        // 筛选出未授权的权限
        List<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }
        
        Logger.d("需要请求的权限：" + permissionsToRequest);
        
        // 如果有未授权的权限，则请求它们
        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(activity, 
                    permissionsToRequest.toArray(new String[0]), 
                    requestCode);
            return false;
        }
        
        Logger.i("所有必要的权限已授予");
        return true;
    }

    /**
     * 处理权限请求结果
     *
     * @param permissions 权限列表
     * @param grantResults 授权结果
     * @return 返回被拒绝的权限列表，如果全部授权则返回空列表
     */
    @NonNull
    public static List<String> handlePermissionResult(@NonNull String[] permissions, @NonNull int[] grantResults) {
        List<String> deniedPermissions = new ArrayList<>();
        
        for (int i = 0; i < permissions.length; i++) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                deniedPermissions.add(permissions[i]);
                Logger.w("权限被拒绝: " + permissions[i]);
            } else {
                Logger.d("权限已授予: " + permissions[i]);
            }
        }
        
        return deniedPermissions;
    }

    /**
     * 检查是否应该显示权限请求说明
     *
     * @param activity 活动
     * @param permission 权限
     * @return 如果应该显示权限请求说明，返回true；否则返回false
     */
    public static boolean shouldShowRequestPermissionRationale(Activity activity, String permission) {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission);
    }

    /**
     * 打开应用权限设置页面
     *
     * @param context 上下文
     */
    public static void openAppSettings(Context context) {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package", context.getPackageName(), null));
        context.startActivity(intent);
    }
} 