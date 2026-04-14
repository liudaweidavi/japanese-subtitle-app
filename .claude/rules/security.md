# 安全规范

## API Key 管理
- 百度翻译 AppID 和密钥存储在 EncryptedSharedPreferences
- 首次使用通过设置页面由用户输入
- 不得在代码中硬编码任何密钥
- 不在 Logcat 中打印 API Key 或翻译请求签名

## 权限
- SYSTEM_ALERT_WINDOW: 悬浮窗必须权限
- RECORD_AUDIO: 音频捕获必须权限
- 所有危险权限需要运行时请求
- MediaProjection 需要用户确认弹窗

## 数据安全
- 字幕历史仅存储在本地 Room 数据库
- 不向第三方发送用户数据（百度翻译 API 除外）
- 翻译缓存仅在内存中，不持久化
