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
- 三级思考深度：低/ 中等 / 深度，控制 AI 推理投入
- 自动检测源语言，一键交换源/目标语言

### 作文批改
- **四种输入方式**：文本输入、相机拍照、图库选图、PDF 文件
- **三标签页结果**：逐句语法纠错 | 修改后完整作文 | 写作建议与技巧
- **四维评分**：语法 / 词汇 / 结构 / 风格（各 25 分，满分 100）
- 默认极深思考深度，支持自定义

### 边缘悬浮条
- 系统级边缘悬浮气泡，贴附屏幕右侧边缘，36dp 圆形图标
- **点击呼出**：点击气泡打开翻译面板
- **自由拖拽**：拖动调整位置，松手自动吸附到最近边缘
- **长按停止**：长按气泡关闭悬浮窗服务
- 支持语言选择、引擎切换、翻译与复制
- 快捷设置磁贴一键启停

### 可配置
- **AI 供应商模块**：内置小米 MiMo、OpenCode Go、自定义 API 三种供应商，一键切换
- 供应商隔离认证（MiMo 用 `api-key` header，其他用 `Authorization: Bearer`）
- 切换供应商自动重置 URL/模型/Key，专属错误提示
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
| 悬浮窗 | WindowManager + 前台 Service + 边缘悬浮气泡 |
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

- **v1.2.3** (2026-07-08) — 优化启动开场动画（图标放大淡出过渡到主界面）
- **v1.2.2** (2026-07-08) — 替换应用启动器图标为全新设计
- **v1.2.1** (2026-07-08) — UI 极简抽象化重构（AbstractIcons 细线条 Path 设计、底部导航栏简化）
- **v1.2.0** (2026-07-08) — 领域层 Gateway 接口重构、翻译请求去重、ML Kit 翻译器缓存、OverlayController 作用域修复、AppError 国际化
- **v1.1.2** (2026-07-07) — 修复 Release 构建下点击设置崩溃（ProGuard ML Kit 包名错误 + isModelDownloaded 异常未捕获）
- **v1.1.1** (2026-07-07) — 修复 ApiProvider 重复 sharedClient 导致设置界面崩溃的编译错误
- **v1.1.0** (2026-07-07) — AI 供应商模块（小米 MiMo/OpenCode Go/自定义）、85 项审查修复、流式翻译可靠性重构
- **v1.0.1** (2026-07-06) — ML Kit 模型下载进度指示、arm64-v8a 架构构建、协程作用域泄漏修复、状态原子更新修复、屏幕旋转处理
- **v1.0.0** (2026-07-05) — 首个正式发布版：AI 翻译（20 语言 + 3 级思考深度）、作文批改（4 种输入 + 四维评分）、边缘小白条悬浮窗

## 下载

| 版本 | 日期 | arm64 (真机) | x86_64 (模拟器) |
|------|------|-------------|----------------|
| v1.2.3 | 2026-07-08 | [Xi_v1.2.3_arm64.apk](https://github.com/ninrry/Xi/releases/download/v1.2.3/Xi_v1.2.3_arm64.apk) | — |
| v1.2.2 | 2026-07-08 | [Xi_v1.2.2_arm64.apk](https://github.com/ninrry/Xi/releases/download/v1.2.2/Xi_v1.2.2_arm64.apk) | — |
| v1.2.1 | 2026-07-08 | [Xi_v1.2.1_arm64.apk](https://github.com/ninrry/Xi/releases/download/v1.2.1/Xi_v1.2.1_arm64.apk) | [Xi_v1.2.1_x86_64.apk](https://github.com/ninrry/Xi/releases/download/v1.2.1/Xi_v1.2.1_x86_64.apk) |
| v1.2.0 | 2026-07-08 | [Xi_v1.2.0_arm64.apk](https://github.com/ninrry/Xi/releases/download/v1.2.0/Xi_v1.2.0_arm64.apk) | [Xi_v1.2.0_x86_64.apk](https://github.com/ninrry/Xi/releases/download/v1.2.0/Xi_v1.2.0_x86_64.apk) |
| v1.1.2 | 2026-07-07 | [Xi_v1.1.2_arm64.apk](https://github.com/ninrry/Xi/releases/download/v1.1.2/Xi_v1.1.2_arm64.apk) | — |
| v1.1.1 | 2026-07-07 | [Xi_v1.1.1_arm64.apk](https://github.com/ninrry/Xi/releases/download/v1.1.1/Xi_v1.1.1_arm64.apk) | — |
| v1.1.0 | 2026-07-07 | [Xi_v1.1.0_arm64.apk](https://github.com/ninrry/Xi/releases/download/v1.1.0/Xi_v1.1.0_arm64.apk) | — |
| v1.0.1 | 2026-07-06 | [Xi_v1.0.1_arm64.apk](https://github.com/ninrry/Xi/releases/download/v1.0.1/Xi_v1.0.1_arm64.apk) | — |

> **说明：** arm64-v8a 适用于真机（小米、三星等），x86_64 适用于模拟器（雷电、AVD）。

## 许可证

MIT
