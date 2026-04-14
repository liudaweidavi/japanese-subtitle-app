# 日语字幕通 - Claude 规范

## 项目概述
Android 实时日语字幕翻译 App。捕获系统音频 → Whisper 端侧日语识别 → 百度翻译API → 悬浮窗显示中文字幕。

## 技术栈
- Kotlin + Android SDK (minSdk 29, targetSdk 34)
- whisper.cpp (JNI/NDK 端侧推理)
- 百度翻译 API
- Room 数据库
- Retrofit + OkHttp
- Kotlin Coroutines + Flow
- Jetpack DataStore

## 代码规范
- 使用 Kotlin，禁止 Java
- 使用 Kotlin Coroutines 处理异步，禁止 RxJava
- 使用 Flow 进行数据流处理
- API 调用统一封装在 `translation/` 目录
- 音频处理统一封装在 `audio/` 目录
- JNI 相关代码在 `jni/` 目录

## 安全规范
- 百度翻译 API Key 通过 DataStore 加密存储
- 不得硬编码 API Key
- 不在 logcat 中打印敏感信息

## 命名规范
- Kotlin 文件: PascalCase (MainActivity.kt)
- 变量/函数: camelCase
- 常量: UPPER_SNAKE_CASE
- XML 资源: snake_case
- 包名: 全小写

## Git 提交规范
```
<type>(<scope>): <描述>

feat(ui): 添加悬浮窗字幕显示
fix(audio): 修复音频捕获中断问题
refactor(pipeline): 重构字幕处理流水线
```

## 项目结构
```
app/src/main/java/com/subtitle/japanese/
├── ui/          # Activity + ViewModel
├── service/     # 前台 Service
├── audio/       # 音频捕获 + 缓冲
├── whisper/     # Whisper JNI 封装
├── translation/ # 翻译 API
├── overlay/     # 悬浮窗管理
├── data/        # Room DB + DataStore
├── pipeline/    # 核心编排
└── util/        # 工具类
```
