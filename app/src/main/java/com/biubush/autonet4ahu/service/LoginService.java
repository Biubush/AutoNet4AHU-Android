package com.biubush.autonet4ahu.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.biubush.autonet4ahu.core.EPortal;
import com.biubush.autonet4ahu.core.NetworkDetector;
import com.biubush.autonet4ahu.core.Notifier;
import com.biubush.autonet4ahu.model.Config;
import com.biubush.autonet4ahu.model.LoginResult;
import com.biubush.autonet4ahu.util.ConfigManager;
import com.biubush.autonet4ahu.util.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 登录服务，实现后台自动登录功能
 */
public class LoginService extends Service {
    private static final int FOREGROUND_SERVICE_ID = 1001;
    
    private ConfigManager configManager;
    private NetworkDetector networkDetector;
    private Notifier notifier;
    private ExecutorService executorService;
    private boolean isRunning = false;
    
    @Override
    public void onCreate() {
        super.onCreate();
        Logger.i("LoginService创建");
        
        // 初始化配置管理器
        configManager = new ConfigManager(this);
        
        // 初始化网络检测器
        networkDetector = new NetworkDetector(this);
        
        // 初始化线程池
        executorService = Executors.newSingleThreadExecutor();
        
        isRunning = true;
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Logger.i("LoginService启动");
        
        // 加载配置
        Config config = configManager.loadConfig();
        
        // 初始化通知器
        notifier = new Notifier(this, config.getWebhookUrls(), config.isNotifyOnSuccess());
        
        // 启动前台服务
        startForeground(FOREGROUND_SERVICE_ID, notifier.createForegroundNotification());
        
        // 如果配置了自动登录，检查网络连接状态，连接成功则执行登录操作
        if (config.isAutoLogin() && config.isComplete()) {
            // 检查网络是否连接
            if (networkDetector.isNetworkConnected()) {
                Logger.i("检测到网络已连接，执行自动登录");
                handleLogin(config);
            } else {
                Logger.w("网络未连接，等待网络连接后自动登录");
            }
        } else {
            Logger.w("自动登录未启用或配置不完整，跳过登录");
        }
        
        // 服务被杀死后自动重启
        return START_STICKY;
    }
    
    @Override
    public void onDestroy() {
        Logger.i("LoginService销毁");
        isRunning = false;
        
        // 关闭线程池
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        
        super.onDestroy();
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    /**
     * 处理登录操作
     *
     * @param config 配置信息
     */
    public void handleLogin(Config config) {
        if (!isRunning) {
            Logger.w("服务已停止，取消登录操作");
            return;
        }
        
        if (!config.isComplete()) {
            Logger.w("配置不完整，取消登录操作");
            return;
        }
        
        // 使用线程池执行登录操作，避免阻塞主线程
        executorService.execute(() -> {
            try {
                // 检查网络连接
                if (!networkDetector.isNetworkConnected()) {
                    Logger.w("网络未连接，取消登录操作");
                    return;
                }
                
                // 获取IP地址
                String ipAddress = networkDetector.getLocalIpAddress();
                if (ipAddress == null || ipAddress.isEmpty() || ipAddress.startsWith("127.")) {
                    Logger.e("获取到无效IP地址: " + (ipAddress == null || ipAddress.isEmpty() ? "空" : ipAddress) + "，取消登录操作");
                    notifier.sendLoginResultNotification(
                            new LoginResult(false, "无法获取有效的IP地址", "unknown"),
                            config.getStudentId());
                    return;
                }
                
                Logger.d("当前IP地址: " + ipAddress);
                
                // 创建ePortal实例
                EPortal ePortal = new EPortal(config.getStudentId(), config.getPassword());
                
                // 执行登录
                LoginResult result = ePortal.login();
                Logger.i("登录结果: " + result);
                
                // 发送通知
                notifier.sendLoginResultNotification(result, config.getStudentId());
                
            } catch (Exception e) {
                Logger.e("登录过程中发生异常", e);
            }
        });
    }
    
    /**
     * 检查服务是否正在运行
     */
    public static boolean isRunning() {
        // 此方法在Android高版本中已不可靠，仅作为状态标记
        return false;
    }
} 