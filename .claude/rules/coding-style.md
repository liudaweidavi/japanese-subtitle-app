# Kotlin 编码规范

## 基本规则
- 使用 Kotlin 1.9+
- 禁止使用 Java 互操作除非必要
- 优先使用 data class
- 使用 sealed class 表达有限状态
- 使用 object 表达单例

## Android 特定
- 使用 ViewBinding，禁止 findViewById
- 使用 Lifecycle-aware 组件
- 前台 Service 必须显示通知
- 权限请求使用 Activity Result API

## 异步处理
- 使用 Kotlin Coroutines (Dispatchers.IO / Dispatchers.Main)
- 使用 SharedFlow / StateFlow 进行数据流传递
- Service 中使用 lifecycleScope
- 禁止使用 Thread / Handler / AsyncTask

## JNI 规范
- JNI 函数命名: Java_包名_类名_方法名
- C++ 代码放在 src/main/jni/ 下
- 使用 CMake 构建
- native 方法集中在 WhisperJni.kt 声明
