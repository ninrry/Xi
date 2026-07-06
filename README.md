# Xi — AI 驱动的英语学习助手

<p align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" width="128" height="128" alt="Xi Logo">
</p>

<p align="center">
  <strong>AI 翻译 · 作文批改 · 边缘悬浮条</strong>
</p>

## 功能

### AI 翻译
- 支持 **20 种语言**互译：中/英/日/韩/西/法/德/意/葡/俄/阿/印/泰/越/印尼/土/荷/波/瑞/乌
- 双引擎：**AI 翻译**（OpenAI 兼容 API）或 **ML Kit 离线翻译**（Google 本地引擎）
- 五级思考深度：无 / 轻度 / 中等 / 深度 / 极深，控制 AI 推理投入
- 自动检测源语言，一键交换源/目标语言

### 作文批改
- **四种输入方式**：文本输入、相机拍照、图库选图、PDF 文件
- **三标签页结果**：逐句语法纠错 | 修改后完整作文 | 写作建议与技巧
- **四维评分**：语法 / 词汇 / 结构 / 风格（各 25 分，满分 100）
- 默认极深思考深度，支持自定义

### 边缘悬浮条
- 系统级边缘小白条，贴附屏幕右侧边缘，极窄 8x40dp 胶囊条
- **滑动呼出**：向屏幕内侧滑动打开翻译面板
- **沿边缘拖拽**：上下拖动自由调整位置
- **长按停止**：长按小白条关闭悬浮窗服务
- 支持语言选择、引擎切换、翻译与复制
- 快捷设置磁贴一键启停

### 可配置
- 兼容任意 OpenAI 格式 API（自定义 URL 与 Key）
- 模型列表自动拉取
- 连接测试（带成功/失败动画）
- ML Kit 语言包管理（下载 / 进度 / 取消）

## 技术栈

| 层 | 技术 |
|---|---|
| UI | Jetpack Compose + Material 3 + 自定义动画 |
| 架构 | MVVM + Hilt DI |
| 网络 | Retrofit + OkHttp + Gson |
| 离线翻译 | Google ML Kit Translate |
| 本地存储 | DataStore Preferences |
| 悬浮窗 | WindowManager + 前台 Service + 边缘小白条 |
| 构建 | Gradle KTS + KSP + R8 混淆 |

## 构建

```bash
export JAVA_HOME=/path/to/jdk17
export ANDROID_HOME=/path/to/android-sdk
./gradlew assembleRelease
```

> 最低 SDK 26 (Android 8.0)，目标 SDK 36，JDK 17+

## 权限

| 权限 | 用途 |
|---|---|
| `INTERNET` | AI API 网络请求 |
| `SYSTEM_ALERT_WINDOW` | 边缘悬浮条 |
| `FOREGROUND_SERVICE` | 悬浮窗保活 |
| `CAMERA` | 拍照输入作文 |
| `POST_NOTIFICATIONS` | 前台服务通知 |

## 快速开始

1. 安装 APK 后打开应用
2. 进入 **设置** → 配置 API 地址和密钥（支持 OpenAI、DeepSeek 等兼容 API）
3. 点击 **测试连接** 验证配置
4. 返回首页开始翻译或作文批改
5. 在翻译页点击 **启用悬浮窗** 获取全局翻译能力

## 版本历史

- **v1.0.1** (2026-07-06) — ML Kit 模型下载进度指示、强制分离架构打包（arm64/x86_64）、协程作用域泄漏修复、状态原子更新修复、屏幕旋转处理
- **v1.0.0** (2026-07-05) — 首个正式发布版：AI 翻译（20 语言 + 5 级思考深度）、作文批改（4 种输入 + 四维评分）、边缘小白条悬浮窗

## 下载

| 版本 | 日期 | arm64 (真机) | x86_64 (模拟器) |
|------|------|-------------|----------------|
| v1.0.1 | 2026-07-06 | [Xi_v1.0.1_arm64.apk](https://github.com/ninrry/Xi/releases/download/v1.0.1/Xi_v1.0.1_arm64.apk) | [Xi_v1.0.1_x86_64.apk](https://github.com/ninrry/Xi/releases/download/v1.0.1/Xi_v1.0.1_x86_64.apk) |

> **如何选择？** 真机（小米、三星等）下载 arm64；模拟器下载 x86_64。

## 许可证

MIT
