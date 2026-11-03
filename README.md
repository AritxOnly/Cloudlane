# Cloudlane

个人 Android 照片助手，包名 `com.aritxonly.cloudlane`，Android 13+。

当前为首个本地分析版本：MediaStore 真实扫描、端侧中文 OCR、旧截图复查、SHA-256 精确重复分组、保留名单、系统回收站确认、夜间充电分析。无演示数据。

**iCloud 手机端登录、上传/下载和双向删除尚未实现，当前照片不会备份到 iCloud。** 夜间任务是本地分析。完整进度见 [Features](Features/README.md)。

## 构建

需要 JDK 21、Android SDK 37、网络依赖；创建包含本机 sdk.dir 的 local.properties。

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

## 使用

授权照片访问后扫描，每批最多 80 张需要分析的照片；再次扫描继续。完整分析需完整相册权限。候选提供理由与查看原图，最多选 100 张，由 Android 系统确认移入本机回收站。保留名单持久化，可在设置恢复。
夜间分析默认关闭，开启后 00:00–07:00 充电、非计费网络、空间充足时分批执行。系统调度可能延迟。

## iCloud 只读验证

在自己的终端运行，密码和验证码交互输入，请勿发送至聊天。会话目录必须保持私有，勿提交 Git。

```sh
python3 -m venv .venv
.venv/bin/pip install -r tools/requirements.txt
.venv/bin/python tools/icloud_probe.py --session-dir /private/tmp/cloudlane-private-session
# 中国大陆账号添加 --china
```

该脚本仅验证图库读取及原图版本信息；不上传、不下载原图、不删除。尚未使用真实账号验证。高级数据保护可能需要受信任设备授权。

## UI 来源

MIUIX 0.9.4-rc01，Compose BOM 2025.08.00。Deadliner 完整玻璃源闭包及逐文件哈希见 third_party/deadliner-origin.json。保留上游许可与声明。Material3 仅用于原组件内部依赖及主题桥接；页面使用 MIUIX Scaffold、Card、Button、Switch。

导航和操作图标使用 Deadliner 同源 MIUIX-Glass Regular 资产，来源及许可位于 third_party/interface-icons。

配色遵循用户要求：MIUIX 原生 System 浅深色方案，关闭 Monet/壁纸取色；玻璃兼容层的所有 Material 色彩角色均由 MIUIX 映射，不使用 Material 默认色板。启动器底色采用 MIUIX tertiaryContainerVariant 浅色值。
