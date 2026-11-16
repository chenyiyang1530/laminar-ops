# 数据库迁移（Flyway）

## 目录

```text
backend/src/main/resources/db/migration
```

## 版本说明

```text
V1__current_schema.sql    当前完整表结构
V2__seed_core.sql         核心初始化数据：角色/用户/组织/字典
V3__sys_config_group_sort_index.sql  平台参数查询索引
V4__seed_menus_roles_config.sql      菜单、角色菜单关系、平台参数默认值
V5__sys_config_range.sql             参数范围字段
V6__log_archive.sql                  登录/操作/参数日志归档表
V7__drop_hospital_company_id.sql     删除医院历史单公司字段
```

## 现有数据库

现有数据库已经有完整结构和数据，配置为：

```yaml
spring:
  flyway:
    baseline-on-migrate: true
    baseline-version: 2
```

第一次启动会创建：

```text
flyway_schema_history
```

并把现有库标记为基线 `2`，然后只执行：

```text
V3
V4
```

不会重复执行 V1/V2。

## 全新数据库

空库启动时会按顺序执行：

```text
V1 建表
V2 核心数据
V3 索引
V4 菜单和平台参数
```

## 新增迁移

以后所有结构变更都新增文件，不要修改已经执行过的迁移：

```text
V5__add_xxx.sql
V6__add_yyy.sql
```

命名规则：

```text
V<版本号>__<描述>.sql
```

## 注意

```text
已执行过的迁移文件不要再改
生产环境执行前先备份数据库
V1 是当前结构快照，不是历史增量
现有库基线为 2，新增迁移从 3 开始
```

## 验证

```sql
select installed_rank, version, description, success
from flyway_schema_history
order by installed_rank;
```
