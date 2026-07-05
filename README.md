# Xi — AI 驱动的英语学习助手

<p align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" width="128" height="128" alt="Xi Logo">
</p>

<p align="center">
  <strong>AI 翻译 · 作文批改 · 浮动翻译悬浮窗</strong>
</p>

## 功能

### 🤖 AI 翻译
- 支持 **20 种语言**互译：中/英/日/韩/西/法/德/意/葡/俄/阿/印/泰/越/印尼/土/荷/波/瑞/乌
- 双引擎：**AI 翻译**（OpenAI 兼容 API）或 **ML Kit 离线翻译**（Google 本地引擎）
- 三级思考深度：轻度 / 中等 / 深度，控制 AI 推理投入
- 自动检测源语言，一键交换源/目标语言

### ✍️ 作文批改
- **四种输入方式**：文本输入、相机拍照、图库选图、PDF 文件
- **三标签页结果**：逐句语法纠错 | 修改后完整作文 | 写作建议与技巧
- **四维评分**：语法 / 词汇 / 结构 / 风格（各 25 分，满分 100）
- 支持思考深度选择与可视化评分

### 🪟 浮动翻译悬浮窗
- 系统级悬浮气泡，在任何应用上方使用
- 拖拽移动 + 点击展开翻译面板
- 支持语言选择、引擎切换、翻译与复制
- 自动贴边隐藏，触摸唤醒
- 快捷设置磁贴一键启停

### ⚙️ 可配置
- 兼容任意 OpenAI 格式 API（自定义 URL 与 Key）
- 模型列表自动拉取
- 连接测试（带成功/失败动画）
- ML Kit 语言包管理（下载 / 进度 / 取消）

## 技术栈

| 层 | 技术 |
|---|---|
| UI | Jetpack Compose + Material 3 + 动画 |
| 架构 | MVVM + Hilt DI |
| 网络 | Retrofit + OkHttp + Gson |
| 离线翻译 | Google ML Kit Translate |
| 本地存储 | DataStore Preferences |
| 悬浮窗 | WindowManager + 前台 Service |
| 构建 | Gradle KTS + KSP + R8 混淆 |

## 构建

```bash
export JAVA_HOME=/path/to/jdk17
export ANDROID_HOME=/path/to/android-sdk
./gradlew assembleDebug
```

> 最低 SDK 26 (Android 8.0)，目标 SDK 36，JDK 17+

## 权限

| 权限 | 用途 |
|---|---|
| `INTERNET` | AI API 网络请求 |
| `SYSTEM_ALERT_WINDOW` | 浮动翻译悬浮窗 |
| `FOREGROUND_SERVICE` | 悬浮窗保活 |
| `CAMERA` | 拍照输入作文 |
| `POST_NOTIFICATIONS` | 前台服务通知 |

## 快速开始

1. 安装 APK 后打开应用
2. 进入 **设置** → 配置 API 地址和密钥（支持 OpenAI、DeepSeek 等兼容 API）
3. 点击 **测试连接** 验证配置
4. 返回首页开始翻译或作文批改
5. 在翻译页点击 **启用悬浮窗** 获取全局翻译能力

## 许可证

MIT
