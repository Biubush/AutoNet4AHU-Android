package com.biubush.autonet4ahu;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.biubush.autonet4ahu.util.Logger;

import java.io.File;
import java.util.List;

/**
 * 日志活动类，用于显示和管理应用日志
 */
public class LogActivity extends AppCompatActivity implements Logger.OnLogListener {
    private TextView logTextView;
    private StringBuilder logBuilder = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);
        
        // 初始化视图
        logTextView = findViewById(R.id.log_text_view);
        Button backButton = findViewById(R.id.back_button);
        Button clearLogsButton = findViewById(R.id.clear_logs_button);
        Button saveLogsButton = findViewById(R.id.save_logs_button);
        
        // 设置按钮点击监听器
        backButton.setOnClickListener(v -> finish());
        
        clearLogsButton.setOnClickListener(v -> {
            Logger.clearLogs();
            logBuilder.setLength(0);
            logTextView.setText("");
            Toast.makeText(this, R.string.msg_logs_cleared, Toast.LENGTH_SHORT).show();
        });
        
        saveLogsButton.setOnClickListener(v -> {
            if (Logger.saveLogsToFile(this)) {
                File logDir = new File(getExternalFilesDir(null), "logs");
                Toast.makeText(this, getString(R.string.msg_logs_saved, logDir.getAbsolutePath()), 
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, R.string.msg_logs_save_failed, Toast.LENGTH_SHORT).show();
            }
        });
        
        // 加载日志
        loadLogs();
        
        // 注册日志监听器
        Logger.addLogListener(this);
        
        // 记录日志
        Logger.i("日志界面已打开");
    }
    
    @Override
    protected void onDestroy() {
        // 移除日志监听器
        Logger.removeLogListener(this);
        Logger.i("日志界面已关闭");
        super.onDestroy();
    }
    
    /**
     * 加载日志
     */
    private void loadLogs() {
        List<String> logs = Logger.getLogs();
        logBuilder.setLength(0);
        
        for (String log : logs) {
            logBuilder.append(log).append("\n");
        }
        
        logTextView.setText(logBuilder.toString());
        
        // 滚动到底部
        scrollToBottom();
    }
    
    /**
     * 滚动到底部
     */
    private void scrollToBottom() {
        logTextView.post(() -> {
            View parent = (View) logTextView.getParent();
            while (!(parent instanceof androidx.core.widget.NestedScrollView) && 
                   !(parent instanceof android.widget.ScrollView) && 
                   parent != null) {
                parent = (View) parent.getParent();
            }
            
            if (parent != null) {
                parent.scrollTo(0, logTextView.getHeight());
            }
        });
    }
    
    @Override
    public void onNewLog(String log) {
        runOnUiThread(() -> {
            logBuilder.append(log).append("\n");
            logTextView.setText(logBuilder.toString());
            scrollToBottom();
        });
    }
} 