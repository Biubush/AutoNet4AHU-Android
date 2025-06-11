package com.biubush.autonet4ahu.model;

/**
 * 登录结果类，用于存储登录状态和消息
 */
public class LoginResult {
    private boolean success;    // 是否成功
    private String message;     // 登录结果消息
    private String ipAddress;   // IP地址
    private long timestamp;     // 登录时间戳

    public LoginResult(boolean success, String message) {
        this.success = success;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }

    public LoginResult(boolean success, String message, String ipAddress) {
        this(success, message);
        this.ipAddress = ipAddress;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "LoginResult{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
} 