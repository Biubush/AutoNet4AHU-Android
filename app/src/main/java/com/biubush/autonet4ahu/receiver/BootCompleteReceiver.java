package com.biubush.autonet4ahu.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.biubush.autonet4ahu.service.LoginService;
import com.biubush.autonet4ahu.util.ConfigManager;
import com.biubush.autonet4ahu.util.Logger;

/**
 * 开机自启动接收器，用于在设备启动完成后自动启动服务
 */
public class BootCompleteReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Logger.i("设备启动完成，准备启动登录服务");
            
            // 检查配置是否完整
            ConfigManager configManager = new ConfigManager(context);
            if (configManager.isConfigComplete()) {
                // 启动登录服务
                startLoginService(context);
            } else {
                Logger.w("配置不完整，跳过自动登录");
            }
        }
    }
    
    /**
     * 启动登录服务
     *
     * @param context 上下文
     */
    private void startLoginService(Context context) {
        try {
            Intent serviceIntent = new Intent(context, LoginService.class);
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
                Logger.d("通过startForegroundService启动登录服务");
            } else {
                context.startService(serviceIntent);
                Logger.d("通过startService启动登录服务");
            }
        } catch (Exception e) {
            Logger.e("启动登录服务失败", e);
        }
    }
} 