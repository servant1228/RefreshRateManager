# Miuix
-keep class top.yukonga.miuix.** { *; }
-dontwarn top.yukonga.miuix.**

# Shizuku
-keep class dev.rikka.shizuku.** { *; }
-dontwarn dev.rikka.shizuku.**
-keep class rikka.shizuku.** { *; }
-dontwarn rikka.shizuku.**
# Shizuku UserService
-keep class com.arcovery.refreshratemanager.service.ShellService { *; }
-keep class com.arcovery.refreshratemanager.IShellService { *; }
-keep class com.arcovery.refreshratemanager.IShellService$Stub { *; }

# Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Backdrop (liquid glass blur)
-keep class com.kyant.backdrop.** { *; }
-dontwarn com.kyant.backdrop.**

# Kotlin coroutines (for suspend functions in R8 full mode)
-keep class kotlin.coroutines.Continuation
-keepattributes Signature
