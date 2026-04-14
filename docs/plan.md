# 日语字幕通 - 项目计划书

## 1. 项目概述

### 1.1 背景
移动端（尤其 iOS）缺乏实时字幕工具，日语视频观看者（动漫、日剧、学习）有明确的字幕翻译需求。系统厂商的内置字幕功能语言支持有限，且不支持跨语言翻译。

### 1.2 产品定位
一款 Android 实时日语字幕翻译工具，通过悬浮窗在其他视频 App 上方显示中文翻译字幕。

### 1.3 核心价值
- 端侧 Whisper 模型日语识别（零 API 成本）
- 百度翻译 API 实时翻译日语→中文
- 悬浮窗覆盖任何视频 App，无需切换播放器

---

## 2. 技术方案

### 2.1 技术栈

| 模块 | 技术 | 说明 |
|------|------|------|
| 语言 | Kotlin | Android 原生开发 |
| ASR | whisper.cpp (JNI/NDK) | 端侧推理，免费 |
| 翻译 | 百度翻译 API | jp→zh，免费额度 5万字/月 |
| 音频捕获 | MediaProjection API | 系统级音频截取 |
| UI | 悬浮窗 (TYPE_APPLICATION_OVERLAY) | 覆盖在其他App上方 |
| 数据库 | Room | 字幕历史本地存储 |
| 网络 | Retrofit + OkHttp | 翻译API调用 |
| 设置 | Jetpack DataStore | 持久化用户配置 |

### 2.2 最低系统要求
- Android 10 (API 29) 及以上
- ARM64 或 ARMv7 架构

### 2.3 数据流

```
视频App播放音频
       ↓
MediaProjection API 捕获系统音频 (PCM 16-bit, 16kHz mono)
       ↓
AudioBuffer 滑动窗口 (5秒窗口, 1秒重叠)
       ↓
Whisper.cpp JNI 端侧推理 (日语识别, 免费)
       ↓
百度翻译 API (日语→中文, 联网)
       ↓
悬浮窗 OverlayManager 显示中文字幕
       ↓
Room 数据库保存历史记录
```

---

## 3. 项目结构

```
com.subtitle.japanese/
├── ui/
│   ├── MainActivity.kt            # 入口, 权限流程, 开始/停止控制
│   ├── SettingsActivity.kt        # 设置页 (模型/字号/百度Key)
│   └── HistoryActivity.kt        # 字幕历史查看
├── service/
│   └── SubtitleOverlayService.kt  # 前台Service (音频+悬浮窗+Pipeline)
├── audio/
│   ├── AudioCaptureManager.kt     # MediaProjection 生命周期管理
│   └── AudioBuffer.kt             # 环形缓冲区, 滑动窗口切分
├── whisper/
│   ├── WhisperEngine.kt           # 高层封装 (模型加载/转录/释放)
│   ├── WhisperJni.kt              # JNI external fun 声明
│   ├── WhisperConfig.kt           # 模型路径/语言/线程数配置
│   └── WhisperResult.kt           # 识别结果数据类
├── translation/
│   ├── BaiduTranslator.kt         # 百度翻译API客户端
│   ├── TranslationCache.kt        # LRU缓存, 避免重复翻译
│   ├── TranslationResult.kt       # 翻译结果数据类
│   └── Md5Util.kt                 # API签名MD5工具
├── overlay/
│   ├── OverlayManager.kt          # 悬浮窗创建/更新/销毁/拖拽
│   └── OverlayView.kt             # 自定义View (半透明+白字)
├── data/
│   ├── SubtitleRepository.kt      # 数据仓库
│   ├── SubtitleEntry.kt           # Room Entity
│   ├── SubtitleDao.kt             # DAO接口
│   ├── AppDatabase.kt             # Room数据库
│   └── UserPreferences.kt         # DataStore设置持久化
├── pipeline/
│   ├── SubtitlePipeline.kt        # 核心编排 (audio→ASR→translate→display)
│   └── PipelineState.kt           # 状态机枚举
└── util/
    ├── PermissionHelper.kt        # 权限检查与请求
    └── Constants.kt               # 全局常量

src/main/jni/
├── whisper-jni.cpp                # C++ JNI桥接 (whisper.cpp → Kotlin)
└── CMakeLists.txt                 # NDK/CMake构建配置

whisper/                           # whisper.cpp 源码 (git submodule)
```

---

## 4. 实现阶段

### Phase 1: 项目骨架 + 权限 + 悬浮窗（第 1-2 周）

**目标**: 权限流程跑通，悬浮窗能显示静态文本。

| 序号 | 任务 | 状态 |
|------|------|------|
| 1.1 | 创建 Android 项目 (Kotlin, minSdk 29, targetSdk 34) | ✅ |
| 1.2 | Gradle 配置 (NDK 25+, CMake 3.22+, 依赖库) | ✅ |
| 1.3 | AndroidManifest.xml (权限声明, Service注册) | ✅ |
| 1.4 | PermissionHelper (悬浮窗/录音/通知权限) | ✅ |
| 1.5 | OverlayManager (TYPE_APPLICATION_OVERLAY 悬浮窗) | ✅ |
| 1.6 | SubtitleOverlayService (前台Service + 通知) | ✅ |
| 1.7 | MainActivity (权限流程 + 开始/停止按钮) | ✅ |
| 1.8 | 资源文件 (layout, strings, colors, themes) | ✅ |

### Phase 2: 音频捕获（第 3 周）

**目标**: 从系统捕获音频，PCM 数据流通。

| 序号 | 任务 | 状态 |
|------|------|------|
| 2.1 | AudioCaptureManager (MediaProjection 音频捕获) | ✅ |
| 2.2 | AudioBuffer (环形缓冲区, 5秒滑动窗口) | ✅ |
| 2.3 | 验证 PCM 数据流通 (44100Hz → 16kHz 重采样) | 待验证 |

### Phase 3: Whisper.cpp 集成（第 4-5 周）

**目标**: 端侧日语语音识别，输出日语文本。

| 序号 | 任务 | 状态 |
|------|------|------|
| 3.1 | whisper.cpp 作为 git submodule 引入 | 待做 |
| 3.2 | whisper-jni.cpp (JNI 桥接: init/free/transcribe) | ✅ |
| 3.3 | CMakeLists.txt (编译 whisper.cpp 为 libwhisper-jni.so) | ✅ |
| 3.4 | WhisperJni.kt (external fun 声明) | ✅ |
| 3.5 | WhisperEngine.kt (上下文管理, 模型加载, 转录) | ✅ |
| 3.6 | 下载 ggml-tiny.bin 模型到 assets/models/ | 待做 |
| 3.7 | 日语识别真机测试 | 待验证 |

### Phase 4: 翻译 + Pipeline（第 6-7 周）

**目标**: 完整数据流通，日语→中文翻译字幕显示。

| 序号 | 任务 | 状态 |
|------|------|------|
| 4.1 | BaiduTranslator (Retrofit, sign=MD5) | ✅ |
| 4.2 | TranslationCache (LRU缓存) | ✅ |
| 4.3 | Md5Util (签名工具) | ✅ |
| 4.4 | SubtitlePipeline (Kotlin Flow 编排) | ✅ |
| 4.5 | Pipeline 连接 Service + Overlay | ✅ |
| 4.6 | 端到端测试: 音频→日文→中文→悬浮窗 | 待验证 |

### Phase 5: 完善与优化（第 8-10 周）

**目标**: 设置页、历史记录、性能优化、多设备测试。

| 序号 | 任务 | 状态 |
|------|------|------|
| 5.1 | SettingsActivity (模型选择/字号/百度Key配置) | ✅ |
| 5.2 | UserPreferences (DataStore 持久化) | ✅ |
| 5.3 | HistoryActivity + Room DB (字幕历史) | ✅ |
| 5.4 | SRT 字幕文件导出 | 待做 |
| 5.5 | 性能优化 (NEON SIMD, 线程数调优) | 待做 |
| 5.6 | 多设备兼容测试 (小米/华为/OPPO/Pixel) | 待做 |
| 5.7 | 电池与内存优化 | 待做 |

---

## 5. MVP 范围

| 功能 | MVP 包含 | 后续版本 |
|------|:--------:|:--------:|
| 权限流程 (悬浮窗+录音+通知) | ✅ | |
| 系统音频捕获 (MediaProjection) | ✅ | |
| Whisper tiny 模型日语识别 | ✅ | |
| 百度翻译 jp→zh | ✅ | |
| 悬浮窗显示中文字幕 | ✅ | |
| 开始/停止控制 | ✅ | |
| 前台Service通知 | ✅ | |
| 字幕历史记录 | | ✅ |
| 设置页 (模型/字号/位置) | | ✅ |
| 可拖拽悬浮窗 | | ✅ |
| SRT 字幕导出 | | ✅ |
| 翻译缓存优化 | | ✅ |
| 麦克风模式回退 | | ✅ |
| 字幕云同步/社区 | | ✅ |

---

## 6. 成本分析

### 6.1 ASR 成本 (语音识别)

| 方案 | 1小时成本 | 1万用户/月成本 |
|------|----------|---------------|
| 阿里云实时ASR | ¥27 | ~400万 |
| 讯飞实时ASR | ¥18 | ~270万 |
| **Whisper端侧** | **¥0** | **¥0** |

### 6.2 翻译成本 (日语→中文)

| 方案 | 1小时成本 | 免费额度 |
|------|----------|---------|
| 百度翻译 | ~¥0.5 | 5万字/月 |
| DeepL | ~¥0.3 | 50万字/月 |

### 6.3 总成本

| 阶段 | 方案 | 1万用户/月成本 |
|------|------|---------------|
| 冷启动 | Whisper端侧 + 百度翻译 | ~15万 (仅翻译费) |
| 增长期 | Whisper端侧 + 端侧翻译 | ~0 |

---

## 7. 风险评估

### 7.1 高风险

| 风险 | 影响 | 应对策略 |
|------|------|---------|
| Whisper推理延迟过高 | 字幕不同步 | 使用 tiny 模型(75MB), 4线程, greedy采样, 3秒窗口 |
| MediaProjection无法捕获某些App音频 | 核心功能失效 | 回退到麦克风模式, 提示用户调大音量 |
| JNI层C++崩溃 | App闪退 | 防御性代码, null检查, 看门狗协程重启上下文 |

### 7.2 中风险

| 风险 | 影响 | 应对策略 |
|------|------|---------|
| 国产ROM限制悬浮窗权限 | 无法使用 | OEM特定设置引导, 分屏备选方案 |
| 百度API免费额度用完 | 翻译中断 | TranslationCache缓存, 后续切端侧翻译模型 |
| 电池消耗大 | 用户流失 | 静音检测自动暂停, 省电模式提示 |

### 7.3 低风险

| 风险 | 影响 | 应对策略 |
|------|------|---------|
| APK体积大 (模型+native库) | 下载转化率低 | 首次启动时下载模型, App Bundle按ABI分割 |
| 翻译质量不稳定 | 用户体验差 | 用户校对机制, 后续接入更好翻译服务 |

---

## 8. 竞品分析

| 竞品 | 平台 | 优势 | 劣势 |
|------|------|------|------|
| Chrome Live Caption | 桌面端 | 系统级, 免费 | 不支持移动端, 不翻译 |
| Android Live Caption | Android | 系统集成 | 仅部分机型, 不支持日语翻译 |
| MIUI/HyperOS 实时字幕 | 小米 | 系统级 | 语言少, 无翻译 |
| AutoCap | Android | 自动加字幕 | 非实时, 仅处理本地视频 |
| 讯飞听见 | 双端 | 识别准确 | 偏转写工具, 不是播放器 |

**差异化定位**: 系统厂商做基础字幕识别，我们做 **日语→中文实时翻译字幕**，更垂直、更精准。

---

## 9. 后续演进路线

### V1.0 (MVP)
- 日语实时识别 + 中文翻译 + 悬浮窗显示

### V2.0
- 多语言支持 (英语、韩语)
- 字幕云同步 (按视频URL匹配)
- 用户校对与评分

### V3.0
- 端侧翻译模型 (零API成本)
- 字幕社区 (类似射手网)
- iOS 版本
- SRT/ASS 字幕导出

---

## 10. 快速开始

### 10.1 环境要求
- Android Studio Hedgehog (2023.1.1) 或更新
- Android NDK 25+
- CMake 3.22+
- Android 10+ 真机或模拟器

### 10.2 构建步骤

```bash
# 1. 克隆项目
cd e:/ai/
# (项目已创建)

# 2. 添加 whisper.cpp 子模块
cd japanese-subtitle-app
git init
git submodule add https://github.com/ggerganov/whisper.cpp.git whisper

# 3. 下载日语模型
# 将 ggml-tiny.bin 放入 app/src/main/assets/models/
# 下载地址: https://huggingface.co/ggerganov/whisper.cpp/tree/main

# 4. 用 Android Studio 打开项目，同步 Gradle

# 5. 连接手机，运行
```

### 10.3 使用流程
1. 打开 App → 授予权限 (悬浮窗 + 录音 + 通知)
2. 在设置页配置百度翻译 AppID 和密钥
3. 点击「开始字幕」→ 授权屏幕录制
4. 切到任意视频 App 播放日语内容
5. 悬浮窗实时显示中文字幕
