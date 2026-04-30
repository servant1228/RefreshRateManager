# RefreshRateManager

一个简单的 Android 刷新率管理工具，支持 Root 和 Shizuku 两种模式切换设备刷新率。

## 功能

- **Root 模式**：通过 SurfaceFlinger backdoor 强制切换刷新率，立即生效
- **Shizuku 模式**：通过 Shizuku 执行 ADB 命令切换刷新率

## 系统要求

- Android 7.0+（API 24+）
- Root 权限 或 Shizuku 环境

## 许可证

MIT License
