package com.biubush.autonet4ahu.core;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;

import com.biubush.autonet4ahu.util.Logger;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * 网络检测类，用于监测网络状态变化
 */
public class NetworkDetector {
    private final Context context;

    /**
     * 构造函数
     *
     * @param context 应用上下文
     */
    public NetworkDetector(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * 检查是否已连接到网络
     *
     * @return 是否已连接到网络
     */
    public boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            Logger.e("无法获取ConnectivityManager");
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = cm.getActiveNetwork();
            if (network == null) {
                Logger.d("当前无活动网络");
                return false;
            }

            NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
            return capabilities != null && 
                   (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || 
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || 
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        } else {
            NetworkInfo activeNetworkInfo = cm.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
    }

    /**
     * 检查是否已连接到WiFi
     *
     * @return 是否已连接到WiFi
     */
    public boolean isWifiConnected() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            Logger.e("无法获取ConnectivityManager");
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = cm.getActiveNetwork();
            if (network == null) {
                return false;
            }

            NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
            return capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
        } else {
            NetworkInfo wifiNetworkInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            return wifiNetworkInfo != null && wifiNetworkInfo.isConnected();
        }
    }

    /**
     * 获取当前连接的WiFi名称
     *
     * @return WiFi名称，如果未连接WiFi则返回null
     */
    public String getConnectedWifiSSID() {
        if (!isWifiConnected()) {
            return null;
        }

        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null) {
            Logger.e("无法获取WifiManager");
            return null;
        }

        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo == null) {
            return null;
        }

        String ssid = wifiInfo.getSSID();
        // 移除SSID两端的双引号
        if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
            ssid = ssid.substring(1, ssid.length() - 1);
        }
        
        Logger.d("当前连接的WiFi: " + ssid);
        return ssid;
    }

    /**
     * 获取本机IP地址
     *
     * @return IP地址，如果无法获取有效IP则返回空字符串
     */
    public String getLocalIpAddress() {
        try {
            // 优先获取WiFi IP地址
            if (isWifiConnected()) {
                WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                if (wifiManager != null) {
                    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                    int ipInt = wifiInfo.getIpAddress();
                    if (ipInt != 0) {
                        String ipString = String.format("%d.%d.%d.%d",
                                (ipInt & 0xff), (ipInt >> 8 & 0xff),
                                (ipInt >> 16 & 0xff), (ipInt >> 24 & 0xff));
                        // 确认不是本地回环地址
                        if (!ipString.startsWith("127.")) {
                            Logger.d("WiFi IP地址: " + ipString);
                            return ipString;
                        } else {
                            Logger.d("WiFi IP是回环地址，继续尝试其他网络接口");
                        }
                    } else {
                        Logger.d("WiFi IP地址无效，继续尝试其他网络接口");
                    }
                }
            }

            // 如果无法获取WiFi IP，则尝试获取其他网络接口的IP
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
                    if (!address.isLoopbackAddress() && address instanceof Inet4Address) {
                        String ipString = address.getHostAddress();
                        // 再次确认不是本地回环地址
                        if (!ipString.startsWith("127.")) {
                            Logger.d("网络接口 " + networkInterface.getName() + " IP地址: " + ipString);
                            return ipString;
                        }
                    }
                }
            }

            Logger.w("无法获取有效IP地址");
            return ""; // 返回空字符串而不是127.0.0.1
        } catch (Exception e) {
            Logger.e("获取IP地址时发生异常", e);
            return ""; // 返回空字符串而不是127.0.0.1
        }
    }

    /**
     * 检查是否连接到校园网WiFi
     *
     * @return 是否连接到校园网WiFi
     */
    public boolean isConnectedToCampusWifi() {
        if (!isWifiConnected()) {
            return false;
        }
        
        String ssid = getConnectedWifiSSID();
        // 根据实际校园网WiFi名称进行匹配
        boolean isCampusWifi = ssid != null && (
                ssid.contains("AHU") || 
                ssid.contains("安徽大学") || 
                ssid.contains("安大"));
        
        Logger.d("是否连接到校园网WiFi: " + isCampusWifi);
        return isCampusWifi;
    }
} 