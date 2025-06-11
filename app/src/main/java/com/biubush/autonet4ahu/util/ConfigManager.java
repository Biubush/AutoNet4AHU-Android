package com.biubush.autonet4ahu.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.biubush.autonet4ahu.model.Config;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 配置管理工具类，用于保存和加载配置
 */
public class ConfigManager {
    private static final String PREF_NAME = "autonet4ahu_config";
    private static final String KEY_STUDENT_ID = "student_id";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_WEBHOOK_URLS = "webhook_urls";
    private static final String KEY_AUTO_LOGIN = "auto_login";
    private static final String KEY_NOTIFY_ON_SUCCESS = "notify_on_success";

    private final Context context;
    private final SharedPreferences preferences;

    /**
     * 构造函数
     *
     * @param context 应用上下文
     */
    public ConfigManager(Context context) {
        this.context = context.getApplicationContext();
        this.preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        
        Logger.d("ConfigManager初始化完成");
    }

    /**
     * 保存配置
     *
     * @param config 配置对象
     * @return 是否保存成功
     */
    public boolean saveConfig(Config config) {
        if (config == null) {
            Logger.e("保存配置失败：配置对象为空");
            return false;
        }

        try {
            Logger.d("开始保存配置");
            
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(KEY_STUDENT_ID, config.getStudentId());
            editor.putString(KEY_PASSWORD, config.getPassword());
            editor.putBoolean(KEY_AUTO_LOGIN, config.isAutoLogin());
            editor.putBoolean(KEY_NOTIFY_ON_SUCCESS, config.isNotifyOnSuccess());
            
            // 将webhook URLs列表转换为JSON字符串
            JSONArray webhookArray = new JSONArray();
            for (String url : config.getWebhookUrls()) {
                webhookArray.put(url);
            }
            editor.putString(KEY_WEBHOOK_URLS, webhookArray.toString());
            
            boolean success = editor.commit();
            Logger.i("配置保存" + (success ? "成功" : "失败"));
            return success;
        } catch (Exception e) {
            Logger.e("保存配置时发生异常", e);
            return false;
        }
    }

    /**
     * 加载配置
     *
     * @return 配置对象
     */
    public Config loadConfig() {
        Logger.d("开始加载配置");
        
        Config config = new Config();
        config.setStudentId(preferences.getString(KEY_STUDENT_ID, ""));
        config.setPassword(preferences.getString(KEY_PASSWORD, ""));
        config.setAutoLogin(preferences.getBoolean(KEY_AUTO_LOGIN, true));
        config.setNotifyOnSuccess(preferences.getBoolean(KEY_NOTIFY_ON_SUCCESS, true));
        
        // 解析webhook URLs
        try {
            String webhookJson = preferences.getString(KEY_WEBHOOK_URLS, "[]");
            JSONArray webhookArray = new JSONArray(webhookJson);
            List<String> webhookUrls = new ArrayList<>();
            
            for (int i = 0; i < webhookArray.length(); i++) {
                webhookUrls.add(webhookArray.getString(i));
            }
            
            config.setWebhookUrls(webhookUrls);
        } catch (JSONException e) {
            Logger.e("解析webhook URLs时发生异常", e);
            config.setWebhookUrls(new ArrayList<>());
        }
        
        Logger.i("配置加载完成，学号：" + maskString(config.getStudentId()) + 
                "，密码长度：" + (config.getPassword() == null ? 0 : config.getPassword().length()) + 
                "，webhook数量：" + config.getWebhookUrls().size());
        
        return config;
    }

    /**
     * 清除配置
     */
    public void clearConfig() {
        Logger.d("正在清除配置");
        preferences.edit().clear().apply();
        Logger.i("配置已清除");
    }

    /**
     * 检查配置是否完整
     *
     * @return 配置是否完整
     */
    public boolean isConfigComplete() {
        Config config = loadConfig();
        return config.isComplete();
    }

    /**
     * 掩盖字符串，用于日志输出，保护敏感信息
     * 例如：12345678 -> 123****8
     */
    private String maskString(String str) {
        if (str == null || str.length() <= 5) {
            return "***";
        }
        
        int length = str.length();
        return str.substring(0, 3) + "****" + str.substring(length - 1);
    }
} 