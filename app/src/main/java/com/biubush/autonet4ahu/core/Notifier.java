package com.biubush.autonet4ahu.core;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.biubush.autonet4ahu.MainActivity;
import com.biubush.autonet4ahu.R;
import com.biubush.autonet4ahu.model.LoginResult;
import com.biubush.autonet4ahu.util.Logger;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 通知模块，用于发送消息通知
 */
public class Notifier {
    private static final String CHANNEL_ID = "autonet4ahu_channel";
    private static final String CHANNEL_NAME = "校园网登录通知";
    private static final int NOTIFICATION_ID = 1001;
    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();
    
    private final Context context;
    private final List<String> webhookUrls;
    private final boolean notifyOnSuccess;
    private final FloatingNotification floatingNotification;
    
    /**
     * 构造函数
     *
     * @param context 应用上下文
     * @param webhookUrls 企业微信webhook URL列表
     * @param notifyOnSuccess 是否在登录成功时发送通知
     */
    public Notifier(Context context, List<String> webhookUrls, boolean notifyOnSuccess) {
        this.context = context.getApplicationContext();
        this.webhookUrls = webhookUrls;
        this.notifyOnSuccess = notifyOnSuccess;
        this.floatingNotification = new FloatingNotification(context);
        
        // 创建通知渠道（仍然需要用于前台服务）
        createNotificationChannel();
        
        Logger.d("Notifier初始化完成，webhook数量: " + (webhookUrls != null ? webhookUrls.size() : 0) + 
                "，登录成功时通知: " + notifyOnSuccess);
    }
    
    /**
     * 发送登录结果通知
     *
     * @param loginResult 登录结果
     * @param studentId 学号
     */
    public void sendLoginResultNotification(LoginResult loginResult, String studentId) {
        // 如果登录成功但配置为不在成功时通知，则不发送通知
        if (loginResult.isSuccess() && !notifyOnSuccess) {
            Logger.d("登录成功，但配置为不在成功时通知，跳过通知");
            return;
        }
        
        // 发送悬浮窗通知
        showFloatingNotification(loginResult);
        
        // 发送企业微信通知
        if (webhookUrls != null && !webhookUrls.isEmpty()) {
            sendWebhookNotification(loginResult, studentId);
        }
    }
    
    /**
     * 显示悬浮窗通知
     *
     * @param loginResult 登录结果
     */
    private void showFloatingNotification(LoginResult loginResult) {
        try {
            // 构建通知内容
            String message = loginResult.getMessage();
            if (loginResult.getIpAddress() != null && !loginResult.getIpAddress().isEmpty()) {
                message += " IP: " + loginResult.getIpAddress();
            }
            
            // 显示悬浮通知
            if (loginResult.isSuccess()) {
                floatingNotification.showSuccess(message);
            } else {
                floatingNotification.showError(message);
            }
            
            Logger.d("悬浮窗通知已发送: " + (loginResult.isSuccess() ? "成功" : "失败") + " - " + message);
        } catch (Exception e) {
            Logger.e("发送悬浮窗通知失败", e);
        }
    }
    
    /**
     * 发送企业微信webhook通知
     *
     * @param loginResult 登录结果
     * @param studentId 学号
     */
    private void sendWebhookNotification(LoginResult loginResult, String studentId) {
        if (webhookUrls == null || webhookUrls.isEmpty()) {
            return;
        }
        
        // 使用线程池异步发送通知，避免阻塞主线程
        executorService.execute(() -> {
            try {
                // 格式化时间
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
                String timeStr = sdf.format(new Date(loginResult.getTimestamp()));
                
                // 构建消息内容
                String status = loginResult.isSuccess() ? "成功" : "失败";
                String content = "校园网登录" + status + "通知\n\n" +
                        "学号: " + studentId + "\n" +
                        "IP地址: " + loginResult.getIpAddress() + "\n" +
                        "登录结果: " + loginResult.getMessage() + "\n" +
                        "时间: " + timeStr;
                
                // 构建JSON请求
                JSONObject textObj = new JSONObject();
                textObj.put("content", content);
                
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("msgtype", "text");
                jsonObject.put("text", textObj);
                
                String jsonBody = jsonObject.toString();
                Logger.d("企业微信通知内容: " + jsonBody);
                
                // 向所有webhook URL发送请求
                for (String webhookUrl : webhookUrls) {
                    if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
                        continue;
                    }
                    
                    try {
                        URL url = new URL(webhookUrl);
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setRequestMethod("POST");
                        connection.setRequestProperty("Content-Type", "application/json");
                        connection.setDoOutput(true);
                        connection.setConnectTimeout(5000);
                        connection.setReadTimeout(5000);
                        
                        try (OutputStream os = connection.getOutputStream()) {
                            byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                            os.write(input, 0, input.length);
                        }
                        
                        int responseCode = connection.getResponseCode();
                        
                        // 读取响应
                        StringBuilder response = new StringBuilder();
                        try (BufferedReader br = new BufferedReader(
                                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                            String responseLine;
                            while ((responseLine = br.readLine()) != null) {
                                response.append(responseLine.trim());
                            }
                        }
                        
                        Logger.d("企业微信通知响应: " + responseCode + " - " + response.toString());
                        
                        if (responseCode == 200) {
                            JSONObject responseJson = new JSONObject(response.toString());
                            if (responseJson.optInt("errcode", -1) == 0) {
                                Logger.i("企业微信通知发送成功");
                            } else {
                                Logger.w("企业微信通知发送失败: " + responseJson.optString("errmsg", "未知错误"));
                            }
                        } else {
                            Logger.w("企业微信通知HTTP请求失败，状态码: " + responseCode);
                        }
                        
                    } catch (Exception e) {
                        Logger.e("发送企业微信通知失败: " + webhookUrl, e);
                    }
                }
                
            } catch (Exception e) {
                Logger.e("构建企业微信通知失败", e);
            }
        });
    }
    
    /**
     * 创建通知渠道（Android 8.0及以上需要）
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("用于显示校园网登录状态的通知");
            
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                Logger.d("通知渠道已创建: " + CHANNEL_ID);
            }
        }
    }
    
    /**
     * 创建前台服务通知
     *
     * @return 前台服务通知
     */
    public Notification createForegroundNotification() {
        // 创建点击通知时的Intent
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        // 构建通知
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("校园网自动登录")
                .setContentText("正在监听网络变化")
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true);
        
        return builder.build();
    }
} 