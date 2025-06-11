package com.biubush.autonet4ahu.core;

import com.biubush.autonet4ahu.model.LoginResult;
import com.biubush.autonet4ahu.util.Logger;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 校园网ePortal登录类
 */
public class EPortal {
    private static final String BASE_URL = "http://172.16.253.3:801/eportal/";
    private static final String LOGIN_URL = BASE_URL + "?c=Portal&a=login&callback=dr1003&login_method=1&jsVersion=3.3.2&v=1117";
    private static final String CAMPUS_CHECK_URL = "http://172.16.253.3/a79.htm";
    private static final String USER_AGENT = "Mozilla/5.0 (Linux; Android 13; AutoNet4AHU) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/95.0.4638.74 Mobile Safari/537.36";
    
    private final String studentId;
    private final String password;
    private String ipAddress;
    private NetworkDetector networkDetector;

    /**
     * 构造函数
     *
     * @param studentId 学号
     * @param password 密码
     */
    public EPortal(String studentId, String password) {
        this.studentId = studentId;
        this.password = password;
        this.ipAddress = "";
        Logger.d("EPortal初始化完成，学号：" + studentId);
    }

    /**
     * 执行登录操作
     *
     * @return 登录结果
     */
    public LoginResult login() {
        Logger.i("开始执行校园网登录...");
        
        // 检查是否已连接到校园网
        if (!isConnectedToCampusNetwork()) {
            Logger.w("未连接到校园网环境");
            return new LoginResult(false, "未连接到校园网环境", ipAddress);
        }
        
        try {
            // 在登录前更新IP地址
            updateIpAddress();
            
            if (ipAddress.isEmpty()) {
                Logger.e("无法获取有效的IP地址");
                return new LoginResult(false, "无法获取有效的IP地址", "unknown");
            }
            
            // 构建登录参数
            Map<String, String> params = new HashMap<>();
            params.put("c", "Portal");
            params.put("a", "login");
            params.put("callback", "dr1003");
            params.put("login_method", "1");
            params.put("user_account", studentId);
            params.put("user_password", password);
            params.put("wlan_user_ip", ipAddress);
            params.put("wlan_user_ipv6", "");
            params.put("wlan_user_mac", "000000000000");
            params.put("wlan_ac_ip", "");
            params.put("wlan_ac_name", "");
            params.put("jsVersion", "3.3.2");
            params.put("v", "1117");
            
            // 构建URL参数字符串
            StringBuilder urlParams = new StringBuilder();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (urlParams.length() > 0) {
                    urlParams.append("&");
                }
                urlParams.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
                urlParams.append("=");
                urlParams.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
            }
            
            // 创建完整的URL
            URL url = new URL(LOGIN_URL + "&" + urlParams.toString());
            Logger.d("登录URL: " + url);
            
            // 创建连接
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setRequestProperty("Accept", "*/*");
            connection.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9");
            connection.setRequestProperty("Referer", "http://172.16.253.3/");
            connection.setRequestProperty("Cache-Control", "no-cache");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            
            // 获取响应
            int responseCode = connection.getResponseCode();
            Logger.d("HTTP响应码: " + responseCode);
            
            if (responseCode == 200) {
                // 读取响应内容
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
                String inputLine;
                StringBuilder response = new StringBuilder();
                
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                
                String responseText = response.toString();
                Logger.d("登录响应: " + responseText);
                
                // 解析JSON响应
                // 格式通常为: dr1003({...})
                Pattern pattern = Pattern.compile("dr1003\\((.*)\\)");
                Matcher matcher = pattern.matcher(responseText);
                
                if (matcher.find()) {
                    String jsonStr = matcher.group(1);
                    JSONObject result = new JSONObject(jsonStr);
                    
                    if ("1".equals(result.optString("result"))) {
                        Logger.i("登录成功");
                        return new LoginResult(true, "登录成功", ipAddress);
                    } else {
                        String msg = result.optString("msg", "登录失败，未知原因");
                        Logger.w("登录失败: " + msg);
                        return new LoginResult(false, msg, ipAddress);
                    }
                } else {
                    Logger.e("无法解析登录响应");
                    return new LoginResult(false, "无法解析登录响应", ipAddress);
                }
            } else {
                Logger.e("HTTP请求失败，状态码: " + responseCode);
                return new LoginResult(false, "HTTP请求失败，状态码: " + responseCode, ipAddress);
            }
        } catch (Exception e) {
            Logger.e("登录过程中发生异常", e);
            return new LoginResult(false, "登录过程中发生异常: " + e.getMessage(), ipAddress);
        }
    }

    /**
     * 检查是否已连接到校园网
     *
     * @return 是否已连接到校园网
     */
    public boolean isConnectedToCampusNetwork() {
        try {
            URL url = new URL(CAMPUS_CHECK_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            
            int responseCode = connection.getResponseCode();
            Logger.d("校园网检测响应码: " + responseCode);
            
            return responseCode == 200;
        } catch (Exception e) {
            Logger.d("校园网连接检测失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 更新IP地址
     */
    public void updateIpAddress() {
        // 使用NetworkDetector获取有效IP地址
        if (networkDetector == null && android.app.Application.getProcessName() != null) {
            try {
                android.app.Application app = (android.app.Application) Class.forName("android.app.ActivityThread")
                        .getMethod("currentApplication").invoke(null);
                if (app != null) {
                    networkDetector = new NetworkDetector(app);
                }
            } catch (Exception e) {
                Logger.e("获取应用上下文失败", e);
            }
        }
        
        // 如果能获取到NetworkDetector，则使用它来获取IP地址
        if (networkDetector != null) {
            this.ipAddress = networkDetector.getLocalIpAddress();
            Logger.d("通过NetworkDetector更新IP地址: " + ipAddress);
        } else {
            // 如果获取不到NetworkDetector，使用备用方法获取IP
            this.ipAddress = getLocalIpAddressFallback();
            Logger.d("通过备用方法更新IP地址: " + ipAddress);
        }
    }
    
    /**
     * 备用方法获取本机IP地址
     * 
     * @return IP地址
     */
    private String getLocalIpAddressFallback() {
        try {
            // 尝试获取非回环地址的IP
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                // 跳过禁用的网络接口
                if (!networkInterface.isUp() || networkInterface.isLoopback() || networkInterface.isVirtual()) {
                    continue;
                }
                
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    // 只接受IPv4地址，不接受回环地址
                    if (!address.isLoopbackAddress() && address instanceof java.net.Inet4Address) {
                        String ipAddress = address.getHostAddress();
                        Logger.d("备用方法获取IP地址: " + ipAddress + " (接口: " + networkInterface.getName() + ")");
                        return ipAddress;
                    }
                }
            }
            
            Logger.w("备用方法无法获取有效的IP地址");
            return "";
        } catch (Exception e) {
            Logger.e("备用方法获取IP地址失败", e);
            return "";
        }
    }
    
    /**
     * 获取当前IP地址
     */
    public String getIpAddress() {
        if (ipAddress == null || ipAddress.isEmpty() || "127.0.0.1".equals(ipAddress)) {
            updateIpAddress();
        }
        return ipAddress;
    }
} 