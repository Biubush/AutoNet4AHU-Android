package com.biubush.autonet4ahu.util;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 日志工具类，用于记录应用运行日志
 */
public class Logger {
    private static final String TAG = "AutoNet4AHU";
    private static final int MAX_LOG_SIZE = 1000; // 最大保存的日志条数
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.CHINA);

    private static List<String> logBuffer = new ArrayList<>();
    private static List<OnLogListener> listeners = new ArrayList<>();
    private static Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * 记录调试信息
     */
    public static void d(String message) {
        log("DEBUG", message);
    }

    /**
     * 记录信息
     */
    public static void i(String message) {
        log("INFO", message);
    }

    /**
     * 记录警告信息
     */
    public static void w(String message) {
        log("WARN", message);
    }

    /**
     * 记录错误信息
     */
    public static void e(String message) {
        log("ERROR", message);
    }

    /**
     * 记录错误信息和异常
     */
    public static void e(String message, Throwable throwable) {
        log("ERROR", message + ": " + throwable.getMessage());
        Log.e(TAG, message, throwable);
    }

    /**
     * 记录日志
     */
    private static synchronized void log(String level, String message) {
        String timestamp = DATE_FORMAT.format(new Date());
        String logMessage = timestamp + " [" + level + "] " + message;
        
        // 输出到Android日志
        switch (level) {
            case "DEBUG":
                Log.d(TAG, message);
                break;
            case "INFO":
                Log.i(TAG, message);
                break;
            case "WARN":
                Log.w(TAG, message);
                break;
            case "ERROR":
                Log.e(TAG, message);
                break;
            default:
                Log.i(TAG, message);
                break;
        }
        
        // 添加到内存缓冲
        logBuffer.add(logMessage);
        if (logBuffer.size() > MAX_LOG_SIZE) {
            logBuffer.remove(0);
        }
        
        // 通知所有监听器
        notifyListeners(logMessage);
    }

    /**
     * 通知所有日志监听器
     */
    private static void notifyListeners(final String message) {
        mainHandler.post(() -> {
            for (OnLogListener listener : listeners) {
                listener.onNewLog(message);
            }
        });
    }

    /**
     * 获取所有日志
     */
    public static List<String> getLogs() {
        return new ArrayList<>(logBuffer);
    }

    /**
     * 清空日志
     */
    public static synchronized void clearLogs() {
        logBuffer.clear();
        i("日志已清空");
    }

    /**
     * 将日志保存到文件
     */
    public static boolean saveLogsToFile(Context context) {
        if (logBuffer.isEmpty()) {
            return false;
        }

        try {
            File logDir = new File(context.getExternalFilesDir(null), "logs");
            if (!logDir.exists()) {
                logDir.mkdirs();
            }

            String fileName = "autonet4ahu_log_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA).format(new Date()) + ".txt";
            File logFile = new File(logDir, fileName);

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile))) {
                for (String log : logBuffer) {
                    writer.write(log);
                    writer.newLine();
                }
            }
            
            i("日志已保存到文件: " + logFile.getAbsolutePath());
            return true;
        } catch (IOException e) {
            e("保存日志失败", e);
            return false;
        }
    }

    /**
     * 添加日志监听器
     */
    public static void addLogListener(OnLogListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * 移除日志监听器
     */
    public static void removeLogListener(OnLogListener listener) {
        listeners.remove(listener);
    }

    /**
     * 日志监听器接口
     */
    public interface OnLogListener {
        void onNewLog(String log);
    }
} 