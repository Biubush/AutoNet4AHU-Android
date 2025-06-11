# AutoNet4AHU-Android

![GitHub license](https://img.shields.io/github/license/Biubush/AutoNet4AHU-Android)
![Android Version](https://img.shields.io/badge/Android-8.0%2B-green)

安徽大学校园网自动登录Android客户端，支持网络状态变化时自动重连，可推送登录结果消息。

## 目录

- [背景介绍](#背景介绍)
- [功能特点](#功能特点)
- [下载安装](#下载安装)
- [使用说明](#使用说明)
- [实现原理](#实现原理)
- [项目结构](#项目结构)
- [兼容性与适用范围](#兼容性与适用范围)
- [常见问题](#常见问题)
- [贡献指南](#贡献指南)
- [版本历史](#版本历史)
- [许可证](#许可证)
- [鸣谢](#鸣谢)

## 背景介绍

作为安徽大学的学生，每次切换网络、重启设备或网络中断后，都需要重新登录校园网，这个过程十分繁琐。为了解决这个问题，我最初开发了windows版本的[AutoNet4AHU](https://github.com/Biubush/AutoNet4AHU)工具。

随着使用场景的扩展，我意识到移动设备也需要类似的自动化解决方案，因此将其移植并扩展到了Android平台，开发了这个更加完善的AutoNet4AHU-Android客户端。

本应用实现了校园网认证的全自动化处理，当网络状态发生变化时（如WiFi切换、IP变更），会自动检测并执行登录，还可以将登录结果通过企业微信webhook推送到您的消息群组，让您随时了解登录状态。

## 功能特点

- **全自动登录**：监测网络变化，自动完成校园网认证
- **开机自启动**：设备启动后自动在后台运行
- **多种通知方式**：
  - 悬浮窗通知：直观显示登录结果
  - 企业微信推送：远程接收登录状态消息
- **完整日志系统**：记录所有操作，便于排查问题
- **低能耗设计**：采用事件驱动机制，最小化资源占用
- **安全可靠**：本地存储账号信息，不连接任何第三方服务器

## 下载安装

### 方式一：直接下载APK

从[Releases](https://github.com/Biubush/AutoNet4AHU-Android/releases)页面下载最新版本的APK文件，安装到您的Android设备上。

### 方式二：从源码构建

1. 克隆仓库到本地
```bash
git clone https://github.com/Biubush/AutoNet4AHU-Android.git
```

2. 使用Android Studio打开项目
3. 构建并安装到您的设备上

## 使用说明

### 初始设置

1. 首次打开应用时，需要授予必要的权限（网络访问、开机自启动、通知权限等）
2. 在主界面输入您的校园网账号（学号）和密码
3. 如需接收企业微信通知，请填入webhook URL
4. 选择是否启用"自动登录"和"登录成功时通知"功能
5. 点击"保存配置"按钮

### 功能说明

- **保存配置**：保存您的账号信息和设置
- **立即登录**：手动触发一次登录操作
- **查看日志**：打开日志页面，查看应用运行记录
- **关于**：查看应用版本和相关信息

### 自动登录机制

应用将在以下情况自动尝试登录校园网：
- 设备开机启动后
- WiFi网络连接或断开时
- 网络状态发生变化时
- IP地址变更时

## 实现原理

### 网络监控机制

应用采用了三重网络监控机制，确保能可靠地捕获所有网络变化事件：

1. **NetworkCallback监听**：使用Android系统的NetworkCallback API监听网络能力和状态变化
2. **BroadcastReceiver接收**：注册广播接收器，接收系统网络变化广播
3. **定时检查**：定期检查网络状态，作为兜底方案

### 登录流程

1. 检测网络连接状态
2. 获取本机IP地址
3. 构建登录请求参数
4. 发送HTTP请求到校园网认证服务器
5. 解析响应结果
6. 根据设置发送通知

### 通知系统

- **悬浮窗通知**：使用WindowManager创建系统级悬浮窗口，显示登录结果
- **企业微信通知**：通过webhook API向企业微信群组推送消息

### 服务保活

应用使用前台服务机制保持后台运行，通过适配Android各版本的后台限制策略，确保服务的持续可用性。

## 项目结构

```
com.biubush.autonet4ahu
├── MainActivity.java            # 主界面
├── LogActivity.java             # 日志界面
├── core                         # 核心功能模块
│   ├── EPortal.java             # 校园网登录实现
│   ├── NetworkDetector.java     # 网络状态检测
│   ├── NetworkMonitor.java      # 网络监控服务
│   ├── Notifier.java            # 通知系统
│   └── FloatingNotification.java # 悬浮窗通知
├── model                        # 数据模型
│   ├── Config.java              # 配置信息模型
│   └── LoginResult.java         # 登录结果模型
├── service                      # 服务组件
│   └── LoginService.java        # 登录后台服务
├── receiver                     # 广播接收器
│   ├── NetworkChangeReceiver.java # 网络变化接收器
│   └── BootCompleteReceiver.java # 开机启动接收器
└── util                         # 工具类
    ├── ConfigManager.java       # 配置管理工具
    ├── Logger.java              # 日志工具
    └── PermissionUtil.java      # 权限管理工具
```

## 兼容性与适用范围

- **Android系统要求**：Android 8.0 (API 26) 及以上版本
- **适用校区**：已在安徽大学磬苑校区和金寨路校区测试通过
- **账号类型**：目前已确认研究生网络账号有效可行，其他类型账号请自行测试
- **设备兼容性**：已在红米K40机器上运行测试通过

> **注意**：由于各品牌设备对后台应用的限制策略不同，可能需要在系统设置中为本应用添加自启动权限和后台运行权限。

## 常见问题

### Q: 应用无法自动登录

可能的原因及解决方案：
1. 检查是否已开启"自动登录"开关
2. 确认账号密码是否正确
3. 检查系统设置中是否允许应用自启动和后台运行
4. 查看应用日志，了解具体错误原因

### Q: 无法收到企业微信通知

可能的原因及解决方案：
1. 确认webhook URL是否正确
2. 检查网络连接是否正常
3. 确认"登录成功时通知"开关已开启
4. 查看应用日志，了解推送失败原因

### Q: 设备重启后应用没有自动启动

可能的原因及解决方案：
1. 确认已授予"开机自启动"权限
2. 在系统设置中允许应用自启动
3. 部分设备需要在系统安全中心/电池管理中设置应用为"受保护应用"

## 贡献指南

非常欢迎您为AutoNet4AHU-Android项目贡献代码或提出建议！

### 提交问题或建议

如果您发现了bug或有新功能建议，请在GitHub上[创建Issue](https://github.com/Biubush/AutoNet4AHU-Android/issues/new)，并尽可能详细地描述问题或建议。

### 提交代码

1. Fork本仓库
2. 创建您的特性分支 (`git checkout -b feature/amazing-feature`)
3. 提交您的更改 (`git commit -m 'Add some amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 创建Pull Request

### 代码规范

- 遵循Java/Android代码规范
- 添加必要的注释
- 编写单元测试（如果适用）
- 确保代码可在Android 8.0及以上版本正常运行

## 版本历史

请查看[Releases页面](https://github.com/Biubush/AutoNet4AHU-Android/releases)获取完整的版本历史。

## 许可证

本项目采用MIT许可证 - 详见[LICENSE](LICENSE)文件

## 鸣谢

- 感谢[AutoNet4AHU](https://github.com/Biubush/AutoNet4AHU)项目提供的基础实现
- 感谢所有为此项目提供反馈和建议的用户
- 感谢Android开源社区提供的宝贵资源

---

**免责声明**：本项目仅供学习和研究使用，请遵守学校网络使用规定。开发者不对因使用本应用造成的任何问题负责。

如有任何问题或建议，欢迎在GitHub上提交Issue或直接联系我。

© 2025 [biubush](https://github.com/Biubush) 