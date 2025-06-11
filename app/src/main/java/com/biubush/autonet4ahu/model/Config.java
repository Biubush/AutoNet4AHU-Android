package com.biubush.autonet4ahu.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 配置信息类，用于存储学号、密码和企业微信webhook URL
 */
public class Config {
    private String studentId;       // 学号
    private String password;        // 密码
    private List<String> webhookUrls;  // 企业微信webhook URL列表
    private boolean autoLogin;      // 是否自动登录
    private boolean notifyOnSuccess; // 登录成功时是否通知

    public Config() {
        this.webhookUrls = new ArrayList<>();
        this.autoLogin = true;
        this.notifyOnSuccess = true;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public List<String> getWebhookUrls() {
        return webhookUrls;
    }

    public void setWebhookUrls(List<String> webhookUrls) {
        this.webhookUrls = webhookUrls != null ? webhookUrls : new ArrayList<>();
    }

    public void addWebhookUrl(String url) {
        if (url != null && !url.isEmpty() && !webhookUrls.contains(url)) {
            webhookUrls.add(url);
        }
    }

    public void clearWebhookUrls() {
        webhookUrls.clear();
    }

    public boolean isAutoLogin() {
        return autoLogin;
    }

    public void setAutoLogin(boolean autoLogin) {
        this.autoLogin = autoLogin;
    }

    public boolean isNotifyOnSuccess() {
        return notifyOnSuccess;
    }

    public void setNotifyOnSuccess(boolean notifyOnSuccess) {
        this.notifyOnSuccess = notifyOnSuccess;
    }

    /**
     * 配置是否完整（至少包含学号和密码）
     */
    public boolean isComplete() {
        return studentId != null && !studentId.isEmpty() 
                && password != null && !password.isEmpty();
    }
} 