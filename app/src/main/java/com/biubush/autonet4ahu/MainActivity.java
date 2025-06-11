package com.biubush.autonet4ahu;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.biubush.autonet4ahu.core.EPortal;
import com.biubush.autonet4ahu.core.NetworkDetector;
import com.biubush.autonet4ahu.core.Notifier;
import com.biubush.autonet4ahu.model.Config;
import com.biubush.autonet4ahu.model.LoginResult;
import com.biubush.autonet4ahu.receiver.NetworkChangeReceiver;
import com.biubush.autonet4ahu.service.LoginService;
import com.biubush.autonet4ahu.util.ConfigManager;
import com.biubush.autonet4ahu.util.Logger;
import com.biubush.autonet4ahu.util.PermissionUtil;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 主活动类，实现登录配置界面功能
 */
public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 1001;
    
    private TextInputEditText studentIdInput;
    private TextInputEditText passwordInput;
    private TextInputEditText webhookInput;
    private SwitchMaterial autoLoginSwitch;
    private SwitchMaterial notifyOnSuccessSwitch;
    private Button saveButton;
    private Button loginButton;
    private Button viewLogsButton;
    private Button aboutButton;
    
    private ConfigManager configManager;
    private NetworkDetector networkDetector;
    private ExecutorService executorService;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        // 设置边缘到边缘显示
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        
        // 初始化视图
        initViews();
        
        // 初始化工具类
        configManager = new ConfigManager(this);
        networkDetector = new NetworkDetector(this);
        executorService = Executors.newSingleThreadExecutor();
        
        // 重置网络状态记录
        NetworkChangeReceiver.resetNetworkState(this);
        
        // 加载配置
        loadConfig();
        
        // 检查权限
        checkPermissions();
        
        // 记录日志
        Logger.i("应用已启动");
    }
    
    @Override
    protected void onDestroy() {
        // 关闭线程池
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        
        super.onDestroy();
    }
    
    /**
     * 初始化视图
     */
    private void initViews() {
        studentIdInput = findViewById(R.id.student_id_input);
        passwordInput = findViewById(R.id.password_input);
        webhookInput = findViewById(R.id.webhook_input);
        autoLoginSwitch = findViewById(R.id.auto_login_switch);
        notifyOnSuccessSwitch = findViewById(R.id.notify_on_success_switch);
        saveButton = findViewById(R.id.save_button);
        loginButton = findViewById(R.id.login_button);
        viewLogsButton = findViewById(R.id.view_logs_button);
        aboutButton = findViewById(R.id.about_button);
        
        // 设置按钮点击监听器
        saveButton.setOnClickListener(v -> saveConfig());
        loginButton.setOnClickListener(v -> performLogin());
        viewLogsButton.setOnClickListener(v -> openLogActivity());
        aboutButton.setOnClickListener(v -> showAboutDialog());
    }
    
    /**
     * 加载配置
     */
    private void loadConfig() {
        Config config = configManager.loadConfig();
        
        studentIdInput.setText(config.getStudentId());
        passwordInput.setText(config.getPassword());
        
        // 如果有webhook URL，则显示第一个
        List<String> webhookUrls = config.getWebhookUrls();
        if (webhookUrls != null && !webhookUrls.isEmpty()) {
            webhookInput.setText(webhookUrls.get(0));
        }
        
        autoLoginSwitch.setChecked(config.isAutoLogin());
        notifyOnSuccessSwitch.setChecked(config.isNotifyOnSuccess());
        
        Logger.d("配置已加载");
    }
    
    /**
     * 保存配置
     */
    private void saveConfig() {
        String studentId = studentIdInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String webhook = webhookInput.getText().toString().trim();
        boolean autoLogin = autoLoginSwitch.isChecked();
        boolean notifyOnSuccess = notifyOnSuccessSwitch.isChecked();
        
        Config config = new Config();
        config.setStudentId(studentId);
        config.setPassword(password);
        config.setAutoLogin(autoLogin);
        config.setNotifyOnSuccess(notifyOnSuccess);
        
        // 添加webhook URL
        if (!TextUtils.isEmpty(webhook)) {
            List<String> webhookUrls = new ArrayList<>();
            webhookUrls.add(webhook);
            config.setWebhookUrls(webhookUrls);
        }
        
        // 保存配置
        if (configManager.saveConfig(config)) {
            Toast.makeText(this, R.string.msg_config_saved, Toast.LENGTH_SHORT).show();
            Logger.i("配置已保存");
            
            // 如果启用了自动登录，则启动登录服务
            if (autoLogin && config.isComplete()) {
                startLoginService();
            }
        } else {
            Toast.makeText(this, R.string.msg_config_save_failed, Toast.LENGTH_SHORT).show();
            Logger.e("配置保存失败");
        }
    }
    
    /**
     * 执行登录操作
     */
    private void performLogin() {
        // 检查网络连接
        if (!networkDetector.isNetworkConnected()) {
            Toast.makeText(this, R.string.msg_network_unavailable, Toast.LENGTH_SHORT).show();
            Logger.w("网络不可用，取消登录");
            return;
        }
        
        // 获取配置
        String studentId = studentIdInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        
        // 检查配置是否完整
        if (TextUtils.isEmpty(studentId) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, R.string.msg_incomplete_config, Toast.LENGTH_SHORT).show();
            Logger.w("配置不完整，取消登录");
            return;
        }
        
        // 显示登录中提示
        Toast.makeText(this, R.string.msg_login_started, Toast.LENGTH_SHORT).show();
        Logger.i("开始手动登录");
        
        // 使用线程池执行登录操作
        executorService.execute(() -> {
            try {
                // 获取IP地址
                String ipAddress = networkDetector.getLocalIpAddress();
                if (ipAddress == null || ipAddress.isEmpty() || ipAddress.startsWith("127.")) {
                    Logger.e("获取到无效IP地址: " + (ipAddress == null || ipAddress.isEmpty() ? "空" : ipAddress));
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, 
                                getString(R.string.msg_login_failed, "无法获取有效的IP地址"), 
                                Toast.LENGTH_LONG).show();
                    });
                    return;
                }
                
                // 创建ePortal实例
                EPortal ePortal = new EPortal(studentId, password);
                
                // 执行登录
                LoginResult result = ePortal.login();
                Logger.i("登录结果: " + result);
                
                // 在UI线程显示结果
                runOnUiThread(() -> {
                    if (result.isSuccess()) {
                        Toast.makeText(MainActivity.this, R.string.msg_login_success, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, getString(R.string.msg_login_failed, result.getMessage()), Toast.LENGTH_LONG).show();
                    }
                });
                
                // 只在登录成功时发送通知
                if (result.isSuccess()) {
                    String webhook = webhookInput.getText().toString().trim();
                    List<String> webhookUrls = new ArrayList<>();
                    if (!TextUtils.isEmpty(webhook)) {
                        webhookUrls.add(webhook);
                    }
                    
                    Notifier notifier = new Notifier(MainActivity.this, webhookUrls, notifyOnSuccessSwitch.isChecked());
                    notifier.sendLoginResultNotification(result, studentId);
                }
                
            } catch (Exception e) {
                Logger.e("登录过程中发生异常", e);
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, getString(R.string.msg_login_failed, e.getMessage()), Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    
    /**
     * 启动登录服务
     */
    private void startLoginService() {
        try {
            // 使用新的静态方法启动服务
            LoginService.startService(this);
        } catch (Exception e) {
            Logger.e("启动登录服务失败", e);
        }
    }
    
    /**
     * 打开日志活动
     */
    private void openLogActivity() {
        Intent intent = new Intent(this, LogActivity.class);
        startActivity(intent);
    }
    
    /**
     * 显示关于对话框
     */
    private void showAboutDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.about_title)
                .setMessage(R.string.about_content)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
    
    /**
     * 检查权限
     */
    private void checkPermissions() {
        // 检查是否有悬浮窗权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.app_name)
                    .setMessage("需要授予悬浮窗权限以显示通知")
                    .setPositiveButton("去授权", (dialog, which) -> {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                    })
                    .setNegativeButton("取消", null)
                    .show();
        }
        
        // 检查其他权限
        if (!PermissionUtil.checkAndRequestPermissions(this, PERMISSION_REQUEST_CODE)) {
            Logger.i("正在请求权限");
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            List<String> deniedPermissions = PermissionUtil.handlePermissionResult(permissions, grantResults);
            
            if (!deniedPermissions.isEmpty()) {
                Logger.w("部分权限被拒绝: " + deniedPermissions);
                
                // 检查是否应该显示权限请求说明
                boolean shouldShowRationale = false;
                for (String permission : deniedPermissions) {
                    if (PermissionUtil.shouldShowRequestPermissionRationale(this, permission)) {
                        shouldShowRationale = true;
                        break;
                    }
                }
                
                if (shouldShowRationale) {
                    // 显示权限请求说明
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.app_name)
                            .setMessage(R.string.permission_rationale)
                            .setPositiveButton(R.string.btn_grant_permission, (dialog, which) -> {
                                PermissionUtil.checkAndRequestPermissions(this, PERMISSION_REQUEST_CODE);
                            })
                            .setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                                Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_LONG).show();
                            })
                            .show();
                } else {
                    // 用户选择了"不再询问"，引导用户去设置页面手动授权
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.app_name)
                            .setMessage(R.string.permission_denied)
                            .setPositiveButton(R.string.btn_grant_permission, (dialog, which) -> {
                                PermissionUtil.openAppSettings(this);
                            })
                            .setNegativeButton(android.R.string.cancel, null)
                            .show();
                }
            } else {
                Logger.i("所有权限已授予");
                
                // 检查配置是否完整，如果完整且启用了自动登录，则启动登录服务
                Config config = configManager.loadConfig();
                if (config.isComplete() && config.isAutoLogin()) {
                    startLoginService();
                }
            }
        }
    }
}