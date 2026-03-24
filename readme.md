# AI 协作规范 - clj-scada (Clojure)
test333
本文档旨在指导 AI 在本项目中生成符合规范、可维护且高质量的后端代码。

## 核心规范

- **技术栈**：本项目使用 Clojure + Ring/Compojure + HugSQL。
- **代码风格**：
  - **注释**：代码注释、docstring 与提交信息使用简体中文。
  - **行数要求**： 每个函数不超过 30 行，每个文件不超过 300 行。
  - **纯函数与不可变性**：优先使用纯函数和不可变数据结构。
  - **命名**：文件名使用 `snake_case`，命名空间使用 `kebab-case`。
  - **代码复用**: 尽可能多的使用`src/clj/clj_scada/common`目录中已经提供的众多的utils文件里的函数.
- **数据库操作**：
  - 所有数据库操作应优先使用 `common_db.clj` 中提供的函数。
  - SQL 语句应定义在 `resources/sql/` 目录下的 `.sql` 文件中，由 HugSQL 管理。
  - 要尽可能使用`src/clj/clj_scada/common`目录中已经提供的众多的utils文件里的函数.
- **测试与质量**：
  - **单元测试**：对新增或修改的核心逻辑，必须在 `test` 目录下添加或更新对应的单元测试。
  - **单元测试追加**：增加单元测试时不要删除已有单元测试，需在原文件基础上追加本次的新测试，或修正已有问题测试代码。
  - **执行测试与生成 org 报告**：使用 `clj -M:test` 运行所有单元测试；测试命名空间引用 `clj-scada.test-utils` 时，会自动生成 `target/ScadaApiTestReport.org`（由 `test/clj/clj_scada/org_utils.clj` 生成）。
  - **生成覆盖率报告**：使用 cloverage CLI（无需 lein）。
    - 推荐命令：`clj -M:coverage -m cloverage.coverage -p src/clj -s test/clj -o target/coverage`
    - 依赖已在 `deps.edn` 的 `:coverage` 别名中固定为 `cloverage/cloverage 1.2.4`。
    - 输出目录默认是 `target/coverage`，HTML 报告为 `target/coverage/index.html`，如需其他格式可加 `--text/--lcov/--codecov` 等参数。
- **构建验证**：提交前，必须确保项目能成功构建：`clojure -T:build jar`（等价于 `uberjar`）。
- **云效任务提醒**：当用户给出以“XVVD-”开头的编号（如 XVVD-6733）时，需将其识别为云效“SCADA项目开发”的缺陷/任务号，并通过 yunxiao MCP 拉取任务描述、附件与评论；修复完成后在该任务评论中补充根因与修复方案。

## 常用命令速查

- **生成单元测试报告**：`clj -M:test`
- **生成覆盖率报告**：`clj -M:coverage -m cloverage.coverage -p src/clj -s test/clj -o target/coverage`

## 文档入口索引

- `docs/全貌/文本库趋势图清理文档补全.md`：针对 XVVD-6614 相关问题，列出后端负责的 `graphic/service.clj` 与 `i18n/service.clj` 清理流程、交叉索引表以及复现时观察的日志关键词。

## 文本库 ↔ 趋势图关键文件
- `src/clj/clj_scada/modules/graphic/service.clj`（`handle-component-i18n-creation`、`sync-meta-info-text-libs!` 等）：趋势控件新增时会在这里创建文本库 `text_id` 并同步 meta-info，删除趋势图时需确保相关 `cross-index` 和 `text_lib` 同步清理。
- `src/clj/clj_scada/modules/i18n/service.clj`（文本库批量增删接口）：对外暴露的 `delete-visual-text-libs!`、`sync-meta-info-text-libs!` 等函数是文本库资源管理的核心，参考文中的重现路径可验证是否传入了趋势图生成的 `text_id`。
- `src/clj/clj_scada/modules/text_and_img/service.clj`：其他模块在删除文本/图片时对文本库的清理逻辑，可作为趋势控件删除失败的横向参考。
- `src/clj/clj_scada/modules/graphic/routes.clj`：接口层在接收前端删除请求时转发 `text_ids`，阅读该文件有助于确认文本库删除是否漏传 `text_id` 或跳过同步步骤。
## 禁止项

- **禁止破坏构建**：提交的代码必须保证 `clojure -T:build jar` 命令可以成功执行。
- **禁止在业务逻辑中编写原生 SQL**：所有 SQL 都应通过 HugSQL 进行管理。

## 项目上下文

### 项目概述
clj-scada 是一个基于 Clojure 构建的 SCADA（数据采集与监控系统）系统后端。该系统管理工业自动化和控制功能，包括变量、连接、图形、配方和其他 SCADA 特定元素。

### Windows 用户管理方案
- **目标**：降低 `/api/user_admin/windows/users` 与新增/修改用户接口的响应耗时，减少 PowerShell 拉起开销。
- **方案**：
  - 查询用户列表改用 Windows API（Netapi32/JNA），仅拉取目标组成员。
  - 用户新增/修改改用 `NetUserAdd/NetUserSetInfo` 与 `NetLocalGroup*` 系列接口维护组成员关系。
  - 接口内部增加 `time-ms` 统计包裹，分阶段记录耗时，用于定位瓶颈。
  - 保留 PowerShell 仅用于未迁移的功能，避免影响既有字段获取与策略查询。
- **注意事项**：
  - Windows 运行时走真实 API；非 Windows 环境单测通过模拟与路径归一化保证稳定。
  - 打包无需额外配置，随 clj-scada 合并进 `penpot.jar`。

### 项目结构
clj-scada 应用遵循模块化架构，包含多个专业模块：
- alarm_record: 处理报警记录和消息
- computer: 管理计算机属性和设置
- file: 管理文件操作
- graphic: 处理图形显示和可视化
- project: 管理项目属性
- recipe: 处理配方定义和执行
- variable: 管理变量和变量组
- user_admin: 处理用户管理和认证
- 以及其他为特定 SCADA 功能设计的模块

### 技术架构
- **框架**: Luminus (基于 Ring, Reitit 和其他组件)
- **数据库**: SQLite，可选择连接到 WinCC/SQLServer 和 AD (Active Directory) 服务
- **依赖项**: Clojure 1.12.0, reitit 用于路由, conman/HugSQL 用于数据库操作, mount 用于状态管理
- **认证**: Windows 认证, LDAP 集成, 基于令牌的认证

### 数据库结构
应用程序使用 SQLite，包含以下表格：
- 项目管理 (project 表)
- 计算机属性 (computer 表)
- 变量和变量类型 (variable, variable_type, variable_group 表)
- 连接 (connection 表)
- 图形 (graphic_svg 表)
- 报警和消息 (报警相关表)
- 配方和配方记录
- 用户管理
- 以及其他 SCADA 特定实体

### 公共路由
应用程序为各种功能提供公共路由，包括：
- 健康检查端点 `/public/health/status` (返回状态和时间戳)
- 数据库操作 `/public/database/*`
- 项目信息 `/public/project/*`
- AD 服务器状态 `/public/ad_server/*`
- 测试端点 `/public/test/*`

### 认证和中间件
- 认证系统包括 Windows 认证、LDAP 集成和基于令牌的认证
- 使用 timbre 进行日志记录，每天滚动日志文件
- 端口 8082 上提供监控指标
- 认证中间件位于 `src/clj/clj_scada/middleware/authentication.clj`

### 最近更改
- 添加了健康检查端点 `/public/health/status`，返回简单的 200 响应，包含健康状态和时间戳
- 健康检查端点无需认证，返回包含状态和时间戳的 JSON

### 开发配置
- 开发配置存储在 `dev-config.edn` 中，包含数据库 URL、端口、nREPL 等设置
- 数据库文件：`Libs/scada.db` (主数据库) 和 `Libs/system.db` (系统数据库)
- 默认开发服务器端口：3100，nREPL 端口：7888
- 日志配置使用 timbre，在 `log/` 目录中创建滚动日志文件

### 配置文件加载优先级（deps.edn）
- **默认类路径配置**：`resources/config.edn`
- **dev/test 资源配置**：当 `-Dconf=dev-config.edn` 或 `-Dconf=test-config.edn` 指定的文件不存在时，
  自动回退到 `env/dev/resources/config.edn` 或 `env/test/resources/config.edn`
- **conf 文件覆盖**：当 `-Dconf=<文件路径>` 指向的文件存在时，其配置会覆盖上述资源配置
- **最终优先级**：`resources/config.edn` < `env/dev|test/resources/config.edn` < `-Dconf` 指定文件

### 生产打包（clj-scada uberjar）
- 推荐命令：`clojure -T:build jar`（内部复用 `uberjar`）
- `build.clj` 打包会包含 `env/prod/resources/config.edn`，并覆盖 `resources/config.edn`
- 运行 jar 未指定 `-Dconf` 时，默认使用 `env/prod/resources/config.edn`

### DynamicAction 目录解析优先级
- **优先配置**：`config.edn` 中的 `:dynamic-action-path`（支持绝对/相对路径）
- **默认相对路径**：
  - `penpot.jar` 同目录下 `../../scada/win-unpacked/resources/app/build/DynamicAction`
  - 兼容历史结构：`DynamicAction`、`TrukingSys/DynamicAction`、`backend/TrukingSys/DynamicAction`
- **安装配置兜底**：读取 `%USERPROFILE%/AppData/Local/SCADA/install_config.json`，取 `install_dir` 拼接
  `scada/win-unpacked/resources/app/build/DynamicAction`
- **启动提醒**：未配置 `:dynamic-action-path` 时启动会输出告警日志，并自动尝试上述定位
- **注意约束**：上述打包优先级仅在非 `dev` 模式生效，`dev` 模式仍沿用旧策略（配置优先 + 相对目录搜索），后续修改务必保持该约束

### 打开项目时 DynamicAction 同步规则
- **模板来源**：通过 `:dynamic-action-path` 解析到的模板目录（见上节规则）。
- **内部函数**：打开项目时必须强制同步 `DynamicAction/全局脚本/内部函数` 到项目目录，避免旧项目缺失关键字段。
- **项目/标准/动作函数**：仅按需补齐模板文件，**不删除**项目目录中用户自建脚本；已存在的脚本默认不覆盖。
- **页面脚本**：禁止在打开项目时覆盖 `DynamicAction/页面脚本`，避免误删用户脚本。

### 本地独立运行（deps.edn 项目）
- **推荐启动方式**：`clj -M:run` 或 `clj -M:dev -m clj-scada.core`
- **配置生效路径**：
  - 使用 `:dev` / `:test` 别名时会注入 `-Dconf=dev-config.edn` / `-Dconf=test-config.edn`，
    若对应文件不存在则回退到 `env/dev|test/resources/config.edn`
  - 未指定 `-Dconf` 时，仅使用类路径 `resources/config.edn`
- **独立生产包运行**：`clojure -T:build jar` → `java -jar target/uberjar/clj-scada.jar`
  默认读取 `env/prod/resources/config.edn`，如需覆盖请显式传 `-Dconf=/path/to/config.edn`

### penpot 合并打包（打入 penpot.jar）
- `penpot/backend` 使用 `:scada-merge` 别名把 `backend/clj-scada` 作为 `local/root` 依赖合并
- 资源复制顺序：`penpot/backend/resources` → `backend/clj-scada/resources` → `backend/clj-scada/env/prod/resources`
- 因最后覆盖 `env/prod/resources`，最终 `penpot.jar` 内的 `config.edn` 默认来自
  `backend/clj-scada/env/prod/resources/config.edn`
- 运行 `penpot.jar` 时仍可通过 `-Dconf=/path/to/config.edn` 覆盖 jar 内配置；
  配置优先级保持 `资源配置 < -Dconf 指定文件`

### log4jdbc SQL 日志开关
- **默认策略**：非生产环境默认启用 log4jdbc；生产环境默认关闭
- **生产开启**：在配置中设置 `:enable-log4jdbc true`（可写在 `env/prod/resources/config.edn` 或 `-Dconf` 指定文件）
- **合并包日志输出**：`penpot.jar` 使用 `penpot/backend/resources/log4j2.xml` 控制日志等级，
  如需输出 SQL，调整 `jdbc.sqlonly` / `jdbc.sqltiming` 为 `info`

### 开发工具
- Clojure MCP (多客户端协议) 可通过 deps.edn 中的 `:mcp` 别名使用
- 启动 MCP 服务器：`clojure -M:mcp`
- MCP 服务器运行在端口 7888 上，提供语言服务器功能
- 依赖项包括 slf4j-nop 以避免日志冲突和 com.bhauman/clojure-mcp
- `.clojure-mcp` 目录包含用于持久存储规划和任务信息的草稿数据
- 资源目录包含迁移、SQL 文件、公共资源和其他项目资源

### 开发工作流程
- 如果 clojure-mcp 已启动并连接，请按照更改代码的依赖顺序进行代码求值
- 修改代码后，执行 `(user/stop)` 和 `(user/start)` 或 `(user/restart)` 重新启动项目
- 重新加载文件后，使用 `(load-file "path/to/file.clj")` 来加载更改
- 使用 `(user/restart)` 可以完整重启应用程序，确保所有更改生效

### 项目打开与数据库迁移注意事项
- `/api/sys_project/is_activate` 仅返回 `runtime` 是否启动和 `activate` 状态，本接口本身不抛出版本错误。
- `"系统版本过低!请先升级!"` 来自 `db-auto-migrate`（`src/clj/clj_scada/modules/sys_project/service.clj`），在 `/api/sys_project/open` 打开项目时触发：若系统模板库 `schema_migrations` 最新 `id` 小于项目库 `ProjectDataBase/scada.db` 的最新 `id`，说明项目库版本高于当前系统版本，会直接报错并中止打开。
- 目标项目的 `ProjectDataBase/scada.db` 由 `db-auto-migrate` 在打开项目时自动执行 `migrations/migrate` 更新：默认使用 `jdbc:sqlite:<项目路径>/ProjectDataBase/scada.db`，若启用 log4jdbc 则为 `jdbc:log4jdbc:sqlite:<项目路径>/ProjectDataBase/scada.db`，按 `resources/migrations` 的 `schema_migrations` 记录补齐缺失迁移。

### Codex CLI/Cloverage 权限处理说明
- **目的**：在沙箱/CI 环境中执行覆盖率或构建时，避免写入默认 `~/.m2` 导致的权限错误。
- **处理方式**：本项目在 `deps.edn` 中已设置 `:mvn/local-repo ".lein-local-repo"`，将 Maven 本地仓库固定到项目目录下。
- **对其他开发者的影响**：该目录仅在本项目内生效，不修改用户全局配置或 `~/.m2`，不会影响其他项目或常规开发。
- **AI 执行规则**：
  - 发现 `~/.m2` 权限报错时，**不要**使用 `sudo/chmod` 修改用户目录权限。
  - 确保 `.lein-local-repo` 可写即可（目录已在 `.gitignore` 中忽略，不应提交）。
- 如需显式覆盖，可在命令行通过 `-Sdeps` 临时指定 `:mvn/local-repo`。

## 图形导入（单页面）ID重映射规范

- 适用范围：append 新增页面导入（单页面 ZIP 包）。
- 映射生成：根据 EDN 的 `graphic_meta_infos` 提取 `:component_id`，排除 `uuid/zero` 与等于当前 `page-id` 的条目，其他均生成新的随机 UUID；形成 `objects-idmap`（字符串键值）。
- 参数透传：调用 `import-page` 时在请求中增加 `:objects-idmap`，Penpot 端接收后进行对象键重写。
- Penpot 重写：后端 `v1.import-page!` 将 `(::object-id-remap cfg)` 的字符串键/值转换为 UUID，并重写页面容器 `:objects` 的键，同时同步更新 `:id/:parent-id/:frame-id/:shapes` 等内部引用。
- 数据写入：数据库写入的 EDN 中，除与 `page-id` 相同的 `component_id` 保持不变，其他非零 `component_id` 替换为新 UUID。
- 构建验证：修改完成后执行 `clojure -T:build jar`，确保构建成功。
- 代码参考：
  - 映射生成与透传：`backend/clj-scada/src/clj/clj_scada/modules/graphic/import_export_single/import_single.clj`
  - Penpot 对象键重写：`penpot/backend/src/app/binfile/v1.clj`

## 文本/图形列表存在性校验与清理规则（EDN 单页面导入）

- 适用范围：`backend/clj-scada/modules/graphic/import_export_single/import_single.clj` 的 EDN 导入流程。
- 涉及字段：
  - 文本列表：`property.use-text-list`、`property.use-text-name`
  - 图形列表：`property.graphic-list.use-graphic-list`、`property.graphic-list.graphic-list-name`
- 判定来源：
  - 查询 `resource_list` 表（`delete_flag = 0`）
    - 文本列表：`type = "TEXT"`
    - 图形列表：`type = "IMG"`
  - 名称解析优先级：`name_text_id -> i18n_text 默认语言 text_name`；若无 `name_text_id`，回退使用 `resource_list.name`（兼容历史数据）。
- 行为策略：
  - 当前项目存在与 EDN 绑定名称同名的列表：保留 EDN 原值，不做清理。
  - 当前项目不存在对应列表：将 `use-text-list` 与 `use-graphic-list` 置为 `nil`，同时清空 `use-text-name` 与 `graphic-list-name`，避免跨项目悬空引用。
- 代码入口：
  - 文本列表清理：`clear-text-list-usage`
  - 图形列表清理：`update-graphic-list-flags`
  - 属性统一处理：`cleanup-attr-after-import`
- 文件位置：`backend/clj-scada/src/clj/clj_scada/modules/graphic/import_export_single/import_single.clj`
- 构建校验：执行 `clojure -T:build jar`，确保编译通过。

## 文本/图形列表存在性校验与清理规则（EDN 单页面导入）

- 适用范围：`backend/clj-scada/modules/graphic/import_export_single/import_single.clj` 的 EDN 导入流程。
- 涉及字段：
  - 文本列表：`property.use-text-list`、`property.use-text-name`
  - 图形列表：`property.graphic-list.use-graphic-list`、`property.graphic-list.graphic-list-name`
- 判定来源：
  - 查询 `resource_list` 表（`delete_flag = 0`）
    - 文本列表：`type = "TEXT"`
    - 图形列表：`type = "IMG"`
  - 名称解析优先级：`name_text_id -> i18n_text 默认语言 text_name`；若无 `name_text_id`，回退使用 `resource_list.name`（兼容历史数据）。
- 行为策略：
  - 当前项目存在与 EDN 绑定名称同名的列表：保留 EDN 原值，不做清理。
  - 当前项目不存在对应列表：将 `use-text-list` 与 `use-graphic-list` 置为 `nil`，同时清空 `use-text-name` 与 `graphic-list-name`，避免跨项目悬空引用。
- 代码入口：
  - 文本列表清理：`clear-text-list-usage`
  - 图形列表清理：`update-graphic-list-flags`
  - 属性统一处理：`cleanup-attr-after-import`
  - 文件位置：`backend/clj-scada/src/clj/clj_scada/modules/graphic/import_export_single/import_single.clj`
- 构建校验：执行 `clojure -T:build jar`，确保编译通过。
