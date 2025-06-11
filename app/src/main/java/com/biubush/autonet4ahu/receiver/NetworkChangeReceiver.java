package com.biubush.autonet4ahu.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiManager;
import android.os.Build;

import com.biubush.autonet4ahu.core.NetworkDetector;
import com.biubush.autonet4ahu.model.Config;
import com.biubush.autonet4ahu.service.LoginService;
import com.biubush.autonet4ahu.util.ConfigManager;
import com.biubush.autonet4ahu.util.Logger;

/**
 * 网络变化接收器，用于监听网络状态变化
 */
public class NetworkChangeReceiver extends BroadcastReceiver {
    private static boolean lastNetworkState = false;
    private static String lastWifiSSID = "";
    private static String lastIPAddress = "";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction()) || 
            WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction()) ||
            WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent.getAction())) {
            
            Logger.d("接收到网络变化广播: " + intent.getAction());
            
            NetworkDetector networkDetector = new NetworkDetector(context);
            
            // 检查网络状态
            boolean isConnected = networkDetector.isNetworkConnected();
            Logger.i("网络状态: " + (isConnected ? "已连接" : "未连接"));
            
            if (isConnected) {
                // 获取当前WiFi名称
                String currentWifiSSID = networkDetector.getConnectedWifiSSID();
                Logger.d("当前WiFi: " + (currentWifiSSID != null ? currentWifiSSID : "未连接WiFi"));
                
                // 获取当前IP地址
                String currentIPAddress = networkDetector.getLocalIpAddress();
                Logger.d("当前IP地址: " + (currentIPAddress.isEmpty() ? "无效" : currentIPAddress));
                
                // 检查是否需要触发登录流程
                boolean shouldTriggerLogin = false;
                
                // 情况1: 网络从断开变为连接
                if (!lastNetworkState) {
                    Logger.i("网络从断开变为连接，需要触发登录");
                    shouldTriggerLogin = true;
                } 
                // 情况2: WiFi网络发生切换
                else if (currentWifiSSID != null && !currentWifiSSID.equals(lastWifiSSID)) {
                    Logger.i("WiFi网络切换: " + lastWifiSSID + " -> " + currentWifiSSID + "，需要触发登录");
                    shouldTriggerLogin = true;
                } 
                // 情况3: IP地址发生变动
                else if (!currentIPAddress.isEmpty() && !currentIPAddress.equals(lastIPAddress)) {
                    Logger.i("IP地址变动: " + lastIPAddress + " -> " + currentIPAddress + "，需要触发登录");
                    shouldTriggerLogin = true;
                }
                
                // 更新状态记录
                lastWifiSSID = currentWifiSSID != null ? currentWifiSSID : "";
                lastIPAddress = currentIPAddress;
                
                // 如果需要触发登录，检查配置并启动登录服务
                if (shouldTriggerLogin) {
                    // 检查配置是否完整且自动登录开关已打开
                    ConfigManager configManager = new ConfigManager(context);
                    Config config = configManager.loadConfig();
                    
                    if (config.isComplete() && config.isAutoLogin()) {
                        // 启动登录服务
                        startLoginService(context);
                    } else {
                        if (!config.isComplete()) {
                            Logger.w("配置不完整，跳过自动登录");
                        } else if (!config.isAutoLogin()) {
                            Logger.w("自动登录未开启，跳过自动登录");
                        }
                    }
                }
            }
            
            // 更新上次网络状态
            lastNetworkState = isConnected;
        } else if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Logger.i("设备启动完成，准备启动登录服务");
            
            // 启动时初始化网络状态
            NetworkDetector networkDetector = new NetworkDetector(context);
            lastNetworkState = networkDetector.isNetworkConnected();
            lastWifiSSID = networkDetector.getConnectedWifiSSID() != null ? networkDetector.getConnectedWifiSSID() : "";
            lastIPAddress = networkDetector.getLocalIpAddress();
            
            Logger.d("初始化网络状态: " + (lastNetworkState ? "已连接" : "未连接") + 
                    ", WiFi: " + lastWifiSSID + ", IP: " + lastIPAddress);
            
            // 检查配置是否完整且自动登录开关已打开
            ConfigManager configManager = new ConfigManager(context);
            Config config = configManager.loadConfig();
            
            if (config.isComplete() && config.isAutoLogin()) {
                // 启动登录服务
                startLoginService(context);
            } else {
                if (!config.isComplete()) {
                    Logger.w("配置不完整，跳过自动登录");
                } else if (!config.isAutoLogin()) {
                    Logger.w("自动登录未开启，跳过自动登录");
                }
            }
        }
    }
    
    /**
     * 检查网络是否连接
     *
     * @param context 上下文
     * @return 是否已连接到网络
     */
    private boolean isNetworkConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return false;
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = cm.getActiveNetwork();
            if (network == null) {
                return false;
            }
            
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
            return capabilities != null && 
                   (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || 
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || 
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        } else {
            return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
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
    
    /**
     * 重置网络状态记录
     * 当应用启动时调用此方法，确保网络状态检测正确
     */
    public static void resetNetworkState(Context context) {
        lastNetworkState = false;
        
        // 重置时也初始化WiFi名称和IP地址记录
        NetworkDetector networkDetector = new NetworkDetector(context);
        lastWifiSSID = networkDetector.getConnectedWifiSSID() != null ? networkDetector.getConnectedWifiSSID() : "";
        lastIPAddress = networkDetector.getLocalIpAddress();
        
        Logger.d("NetworkChangeReceiver网络状态已重置，WiFi: " + lastWifiSSID + ", IP: " + lastIPAddress);
    }
} 