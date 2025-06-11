package com.biubush.autonet4ahu.core;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.biubush.autonet4ahu.R;
import com.biubush.autonet4ahu.util.Logger;

/**
 * 悬浮通知类，用于显示自定义悬浮窗通知
 */
public class FloatingNotification {

    private static final long DEFAULT_DISPLAY_DURATION = 5000; // 默认显示5秒
    
    private final Context context;
    private final WindowManager windowManager;
    private final Handler mainHandler;
    
    private View floatingView;
    private WindowManager.LayoutParams params;
    private boolean isShowing = false;
    
    /**
     * 构造函数
     *
     * @param context 应用上下文
     */
    public FloatingNotification(Context context) {
        this.context = context.getApplicationContext();
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        this.mainHandler = new Handler(Looper.getMainLooper());
        
        initWindowParams();
    }
    
    /**
     * 初始化悬浮窗参数
     */
    private void initWindowParams() {
        params = new WindowManager.LayoutParams();
        
        // 设置窗口类型
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            params.type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        
        // 设置窗口属性
        params.format = PixelFormat.TRANSLUCENT;
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | 
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | 
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        
        // 设置窗口位置和大小
        params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        params.width = WindowManager.LayoutParams.WRAP_CONTENT;
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        params.y = 100; // 距离顶部的距离
    }
    
    /**
     * 显示悬浮通知
     *
     * @param title 通知标题
     * @param message 通知内容
     * @param isSuccess 是否成功
     */
    @SuppressLint("InflateParams")
    public void show(String title, String message, boolean isSuccess) {
        show(title, message, isSuccess, DEFAULT_DISPLAY_DURATION);
    }
    
    /**
     * 显示悬浮通知
     *
     * @param title 通知标题
     * @param message 通知内容
     * @param isSuccess 是否成功
     * @param duration 显示时长（毫秒）
     */
    @SuppressLint("InflateParams")
    public void show(String title, String message, boolean isSuccess, long duration) {
        // 确保在主线程中操作UI
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post(() -> show(title, message, isSuccess, duration));
            return;
        }
        
        try {
            // 如果已经在显示通知，先移除旧的
            if (isShowing && floatingView != null) {
                hide();
            }
            
            // 初始化视图
            floatingView = LayoutInflater.from(context).inflate(R.layout.floating_notification, null);
            
            // 设置通知内容
            TextView titleView = floatingView.findViewById(R.id.notification_title);
            TextView messageView = floatingView.findViewById(R.id.notification_message);
            ImageView iconView = floatingView.findViewById(R.id.notification_icon);
            
            titleView.setText(title);
            messageView.setText(message);
            
            // 根据成功状态设置图标和背景
            if (isSuccess) {
                iconView.setImageResource(android.R.drawable.ic_dialog_info);
                floatingView.setBackgroundResource(R.drawable.floating_notification_bg_success);
            } else {
                iconView.setImageResource(android.R.drawable.ic_dialog_alert);
                floatingView.setBackgroundResource(R.drawable.floating_notification_bg_error);
            }
            
            // 添加到窗口
            windowManager.addView(floatingView, params);
            isShowing = true;
            
            // 设置自动消失
            mainHandler.postDelayed(this::hide, duration);
            
            Logger.d("悬浮通知已显示: " + title + " - " + message);
        } catch (Exception e) {
            Logger.e("显示悬浮通知失败", e);
        }
    }
    
    /**
     * 隐藏悬浮通知
     */
    public void hide() {
        // 确保在主线程中操作UI
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post(this::hide);
            return;
        }
        
        if (isShowing && floatingView != null) {
            try {
                windowManager.removeView(floatingView);
                floatingView = null;
                isShowing = false;
                Logger.d("悬浮通知已隐藏");
            } catch (Exception e) {
                Logger.e("隐藏悬浮通知失败", e);
            }
        }
    }
    
    /**
     * 显示登录成功通知
     *
     * @param message 通知内容
     */
    public void showSuccess(String message) {
        show("校园网登录成功", message, true);
    }
    
    /**
     * 显示登录失败通知
     *
     * @param message 通知内容
     */
    public void showError(String message) {
        show("校园网登录失败", message, false);
    }
} 