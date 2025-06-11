package com.biubush.autonet4ahu.core;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.biubush.autonet4ahu.service.LoginService;
import com.biubush.autonet4ahu.model.Config;
import com.biubush.autonet4ahu.util.ConfigManager;
import com.biubush.autonet4ahu.util.Logger;

import java.util.Timer;
import java.util.TimerTask;

/**
 * 高级网络监控类，使用NetworkCallback机制和定时检查双重保障
 */
public class NetworkMonitor {
    private static final int PERIODIC_CHECK_INTERVAL = 60000; // 60秒定时检查
    
    private final Context context;
    private final ConnectivityManager connectivityManager;
    private final WifiManager wifiManager;
    private final NetworkDetector networkDetector;
    private final Handler mainHandler;
    
    private ConnectivityManager.NetworkCallback networkCallback;
    private Timer periodicCheckTimer;
    
    private String lastWifiSSID = "";
    private String lastIPAddress = "";
    private boolean isMonitoring = false;
    
    /**
     * 构造函数
     *
     * @param context 应用上下文
     */
    public NetworkMonitor(Context context) {
        this.context = context.getApplicationContext();
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        this.wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        this.networkDetector = new NetworkDetector(context);
        this.mainHandler = new Handler(Looper.getMainLooper());
        
        // 初始化记录当前状态
        updateNetworkState();
    }
    
    /**
     * 开始监控网络状态
     */
    public void startMonitoring() {
        if (isMonitoring) {
            return;
        }
        
        Logger.i("开始监控网络状态");
        isMonitoring = true;
        
        // 注册网络回调监听
        registerNetworkCallback();
        
        // 启动定时检查任务
        startPeriodicCheck();
    }
    
    /**
     * 停止监控网络状态
     */
    public void stopMonitoring() {
        if (!isMonitoring) {
            return;
        }
        
        Logger.i("停止监控网络状态");
        isMonitoring = false;
        
        // 取消网络回调监听
        unregisterNetworkCallback();
        
        // 停止定时检查任务
        stopPeriodicCheck();
    }
    
    /**
     * 注册网络回调监听
     */
    private void registerNetworkCallback() {
        if (networkCallback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (Exception e) {
                Logger.e("取消旧的网络回调失败", e);
            }
        }
        
        try {
            // 创建网络请求
            NetworkRequest.Builder builder = new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                    .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET);
            
            // 创建网络回调
            networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    super.onAvailable(network);
                    Logger.d("网络可用回调触发");
                    
                    // 使用主线程处理，确保不会有并发问题
                    mainHandler.post(() -> {
                        // 延迟一些时间让网络完全连接
                        mainHandler.postDelayed(() -> checkNetworkChange(true), 3000);
                    });
                }
                
                @Override
                public void onLost(@NonNull Network network) {
                    super.onLost(network);
                    Logger.d("网络断开回调触发");
                    
                    // 更新网络状态
                    mainHandler.post(() -> checkNetworkChange(false));
                }
                
                @Override
                public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
                    super.onCapabilitiesChanged(network, networkCapabilities);
                    Logger.d("网络能力变化回调触发");
                    
                    // 检查网络是否为WiFi
                    boolean hasWifi = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
                    
                    // 在网络能力变化时检查，可以捕获WiFi切换和IP变化
                    mainHandler.post(() -> checkNetworkChange(true));
                }
            };
            
            // 注册网络回调
            connectivityManager.registerNetworkCallback(builder.build(), networkCallback);
            Logger.d("网络回调注册成功");
        } catch (Exception e) {
            Logger.e("注册网络回调失败", e);
        }
    }
    
    /**
     * 取消网络回调监听
     */
    private void unregisterNetworkCallback() {
        if (networkCallback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
                networkCallback = null;
                Logger.d("网络回调已取消");
            } catch (Exception e) {
                Logger.e("取消网络回调失败", e);
            }
        }
    }
    
    /**
     * 启动定时检查任务
     */
    private void startPeriodicCheck() {
        if (periodicCheckTimer != null) {
            periodicCheckTimer.cancel();
        }
        
        periodicCheckTimer = new Timer();
        periodicCheckTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                // 使用主线程处理定时检查
                mainHandler.post(() -> {
                    Logger.d("执行定时网络状态检查");
                    checkNetworkChange(networkDetector.isNetworkConnected());
                });
            }
        }, PERIODIC_CHECK_INTERVAL, PERIODIC_CHECK_INTERVAL);
        
        Logger.d("定时网络检查任务已启动，间隔: " + PERIODIC_CHECK_INTERVAL + "ms");
    }
    
    /**
     * 停止定时检查任务
     */
    private void stopPeriodicCheck() {
        if (periodicCheckTimer != null) {
            periodicCheckTimer.cancel();
            periodicCheckTimer = null;
            Logger.d("定时网络检查任务已停止");
        }
    }
    
    /**
     * 检查网络变化
     *
     * @param isConnected 当前网络是否连接
     */
    private void checkNetworkChange(boolean isConnected) {
        if (isConnected) {
            // 获取最新网络状态
            String currentWifiSSID = getWifiSSID();
            String currentIPAddress = networkDetector.getLocalIpAddress();
            
            Logger.d("网络检查 - WiFi: " + currentWifiSSID + ", IP: " + currentIPAddress);
            
            boolean shouldTriggerLogin = false;
            
            // 检查WiFi是否发生变化
            if (!currentWifiSSID.equals(lastWifiSSID)) {
                Logger.i("WiFi变化检测: " + lastWifiSSID + " -> " + currentWifiSSID);
                shouldTriggerLogin = true;
            }
            
            // 检查IP是否发生变化
            if (!currentIPAddress.isEmpty() && !currentIPAddress.equals(lastIPAddress)) {
                Logger.i("IP地址变化检测: " + lastIPAddress + " -> " + currentIPAddress);
                shouldTriggerLogin = true;
            }
            
            // 更新状态记录
            lastWifiSSID = currentWifiSSID;
            lastIPAddress = currentIPAddress;
            
            // 如果需要触发登录
            if (shouldTriggerLogin) {
                // 检查自动登录设置
                triggerLoginIfEnabled();
            }
        } else {
            Logger.i("网络断开");
        }
    }
    
    /**
     * 获取当前WiFi SSID，处理了各种异常情况
     *
     * @return 当前WiFi SSID，如果无法获取则返回空字符串
     */
    private String getWifiSSID() {
        try {
            if (wifiManager == null) {
                return "";
            }
            
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo == null) {
                return "";
            }
            
            String ssid = wifiInfo.getSSID();
            
            // 处理WiFi未连接的情况
            if (ssid == null || ssid.isEmpty() || "<unknown ssid>".equals(ssid)) {
                return "";
            }
            
            // 移除SSID两端的双引号
            if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
                ssid = ssid.substring(1, ssid.length() - 1);
            }
            
            return ssid;
        } catch (Exception e) {
            Logger.e("获取WiFi SSID失败", e);
            return "";
        }
    }
    
    /**
     * 如果自动登录已启用，则触发登录流程
     */
    private void triggerLoginIfEnabled() {
        try {
            ConfigManager configManager = new ConfigManager(context);
            Config config = configManager.loadConfig();
            
            if (config.isComplete() && config.isAutoLogin()) {
                Logger.i("触发自动登录流程");
                LoginService.startService(context);
            } else {
                if (!config.isComplete()) {
                    Logger.w("配置不完整，跳过自动登录");
                } else if (!config.isAutoLogin()) {
                    Logger.w("自动登录未开启，跳过自动登录");
                }
            }
        } catch (Exception e) {
            Logger.e("触发登录流程失败", e);
        }
    }
    
    /**
     * 更新当前网络状态记录
     */
    private void updateNetworkState() {
        lastWifiSSID = getWifiSSID();
        lastIPAddress = networkDetector.getLocalIpAddress();
        
        Logger.d("初始网络状态 - WiFi: " + lastWifiSSID + ", IP: " + lastIPAddress);
    }
} 