# 验证记录 · 2026-11-03

## 构建与规则

环境：Android Studio JBR 21、Android SDK 37、Gradle 9.3.1；最低 Android 13，target 36。

```sh
./gradlew --no-daemon :app:assembleDebug :app:testDebugUnitTest
```

本地分析阶段构建通过。9 项 JUnit 规则测试：保护文档优先、相机照片排除、普通日期不推断过期、明确截止日期、无效日期、夜间窗口边界、完全重复保留一份、缓存信号跨日过期、旧截图年龄跨界；全部通过。

首轮编译曾因缺失离线依赖、Kotlin 小错误及旧 Gradle daemon 的写入权限失败；获取依赖、修复后使用新 daemon 完成构建，失败已解除。

## 模拟器行为

仅使用新建的 Cloudlane_Test / emulator-5580，Android 36.1 arm64。没有对真实手机执行安装或调试。

- 从 MediaStore 扫描测试生成的两张相同优惠券截图；中文 OCR 识别有效期至 2025-01-01，一份显示过期理由，另一份显示完整哈希重复。
- 选择后出现 Android 原生回收站确认；确认后候选由 2 降到 1，云端未操作。
- 点击保留后候选隐藏，设置可恢复；不后台自动清理。
- v1 索引在更新安装后升级至 v2，原有图片记录保留。重新扫描生成结构化信号，后续再扫 processed=0，仍保留日期复查理由。
- 开启夜间开关后，系统登记 NightWorker，具备 CHARGING、CONNECTIVITY、STORAGE_NOT_LOW 约束；整夜执行、手机厂商后台限制未验证。
- MIUIX-Glass 图标使用 CloudFill / Delete / Settings / Refresh，导航切页与独立扫描按钮可操作。
- 浅深色界面、初始/中间/完全滚动顶栏均已观察。中间滚动时上游折射在文字边缘较明显，效果与性能仍需实机确认；无自行替代玻璃实现。

截图中的相册数据是明确生成的模拟器测试样本，App 本身不内置假数据。实际运行截图与原型分别存放，不能把原型当作运行证据。

## 规格与源码

```sh
python3 ~/.codex/skills/deadliner-spec-dev/scripts/validate_feature_spec.py Features/PRD/photo-companion --stage ready --platform android
python3 ~/.codex/skills/deadliner-spec-dev/scripts/check_feature_index.py --workspace .
```

玻璃依赖闭包及来源哈希见 third_party/deadliner-origin.json；图标来源哈希见 third_party/interface-icons/origin.json。产品核心与导入源文件未超过 1000 有效行阈值。

## 明确未完成

iCloud 未使用真实账号认证；没有手机端登录、原图上传/下载、双向同步、删除同步。只读探针只做静态检查及 --help 检查，不能据此宣称可连接用户账号。

尚未验证：多万张相册性能、厂商后台整夜运行、权限变化所有路径、RTL/大字号/横屏全覆盖、无着色器与无 backdrop 的视觉回退。回退源码保持上游实现。

整体规格保持未完成。本轮可交付的是本地照片整理首版。

## 最终配色调整

依用户补充要求，使用 MIUIX 原生 System 色板，关闭 Monet；Scaffold 使用 surface 中性底色、Card 使用组件原生容器色、主按钮与开关使用 primary。全部玻璃兼容色从 MIUIX 映射。最终浅深色截图另行留证。

最终 APK 再次安装启动成功，AndroidRuntime 日志无崩溃。最终配色截图：[浅色](platforms/android/evidence/light.png)、[深色](platforms/android/evidence/dark.png)、[清理](platforms/android/evidence/clean.png)。规格 ready 与需求索引校验通过。
