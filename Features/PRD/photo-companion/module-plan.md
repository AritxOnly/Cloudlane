# 模块计划

app 单模块：domain 为无平台依赖规则；data 为 MediaStore、SQLite 索引、OCR；NightWorker 为持久化调度；UI 为 MIUIX 与本地 Deadliner 组件分叉。新项目无可复用业务实体，Photo/候选直接对应索引，不增加空仓储层。
玻璃组件以 third_party/deadliner-origin.json 的源文件哈希为准，完整保留，Material3 仅作为原组件内部兼容依赖；产品界面使用 MIUIX。
KMP 暂不适用：用户当前仅开发 Android，新工程无 DeadlinerCore 集成，避免耦合任务管理业务。


## 模块拆分

app/domain：分类与时间规则；app/data：MediaStore、SQLite、OCR；NightWorker：调度；MainActivity/LibraryModel：UI与状态。

## 实体复用与新增理由

新工程无业务模型；Photo 直接表示索引结果。玻璃组件完整迁移并记录哈希，未复制 Deadliner 任务页面。

## 平台映射

仅 Android；KMP/iOS/HarmonyOS 当前不适用。

## 文件拆分策略

业务文件按规则、索引、扫描和调度拆分；上游组件维持源文件边界。

## 风险点

玻璃依赖使用 Material3 内部主题桥接，产品组件使用 MIUIX；OCR 失败需可重试。
