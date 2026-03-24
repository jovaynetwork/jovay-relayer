# Relayer 配置操作文档

> [English Version](configuration-guide.md)

本文档介绍 Jovay L2 Relayer 的完整配置体系，包括 Docker Compose 部署配置和 Relayer 进程内部配置。

---

## 目录

- [1. 部署架构概览](#1-部署架构概览)
- [2. Docker Compose 部署配置](#2-docker-compose-部署配置)
  - [2.1 服务组成](#21-服务组成)
  - [2.2 启动与管理](#22-启动与管理)
  - [2.3 Relayer 服务环境变量（必填项）](#23-relayer-服务环境变量必填项)
  - [2.4 Relayer 服务环境变量（可选项）](#24-relayer-服务环境变量可选项)
  - [2.5 Query Service 环境变量](#25-query-service-环境变量)
  - [2.6 MySQL 服务配置](#26-mysql-服务配置)
  - [2.7 Redis 服务配置](#27-redis-服务配置)
  - [2.8 使用 env_file 管理变量](#28-使用-env_file-管理变量)
- [3. Relayer 进程配置详解](#3-relayer-进程配置详解)
  - [3.1 Spring 基础配置](#31-spring-基础配置)
  - [3.2 Rollup 核心配置](#32-rollup-核心配置)
  - [3.3 经济策略配置（择时提交）](#33-经济策略配置择时提交)
  - [3.4 L1 客户端配置](#34-l1-客户端配置)
  - [3.5 L2 客户端配置](#35-l2-客户端配置)
  - [3.6 Tracer 客户端配置](#36-tracer-客户端配置)
  - [3.7 任务配置](#37-任务配置)
  - [3.8 告警配置](#38-告警配置)
  - [3.9 监控配置](#39-监控配置)
  - [3.10 引擎配置](#310-引擎配置)
  - [3.11 gRPC 客户端配置](#311-grpc-客户端配置)
  - [3.12 签名服务配置](#312-签名服务配置)
  - [3.13 Jasypt 加密配置](#313-jasypt-加密配置)
- [4. 动态配置（Admin CLI）](#4-动态配置admin-cli)
  - [4.1 启动 CLI](#41-启动-cli)
  - [4.2 Gas 价格动态调整](#42-gas-价格动态调整)
  - [4.3 经济策略动态调整](#43-经济策略动态调整)
- [5. 配置变更记录](#5-配置变更记录)
- [6. 附录](#6-附录)
  - [6.1 Rollup Specs 配置示例](#61-rollup-specs-配置示例)
  - [6.2 BPO 文件示例](#62-bpo-文件示例)
  - [6.3 注意事项](#63-注意事项)

---

## 1. 部署架构概览

Relayer 通过 Docker Compose 进行部署，一个完整的 Relayer 实例由以下四个服务组成：

```
┌─────────────────────────────────────────────────────────┐
│                   Docker Compose Stack                   │
│                                                         │
│  ┌──────────────┐     ┌──────────────────────────────┐  │
│  │  l2-relayer   │────▶│  l2-relayer-mysql (MySQL 8.4) │  │
│  │  (核心服务)   │     └──────────────────────────────┘  │
│  │  Port: 7088   │     ┌──────────────────────────────┐  │
│  │              │────▶│  l2-relayer-redis (Redis 6.2)  │  │
│  └──────────────┘     └──────────────────────────────┘  │
│         ▲                                               │
│         │ depends_on (healthy)                           │
│  ┌──────────────┐                                       │
│  │ query-service │                                       │
│  │  Port: 8080   │                                       │
│  └──────────────┘                                       │
└─────────────────────────────────────────────────────────┘
```

- **l2-relayer**：核心 Rollup 服务，负责 L2 Block 拉取、Chunk/Batch 组装、L1 交易提交、Proof 获取与提交
- **l2-relayer-mysql**：MySQL 8.4 数据库，存储 Batch、Chunk、交易、消息等持久化数据
- **l2-relayer-redis**：Redis 6.2，用于分布式锁和缓存
- **query-service**：数据查询服务，提供 REST API 供外部查询 Relayer 状态

---

## 2. Docker Compose 部署配置

### 2.1 服务组成

配置模板位于仓库 `docker/compose.yaml`，包含四个服务。多实例部署时通过 `CONTAINER_INDEX` 环境变量区分：

| 服务 | 容器名 | 镜像 | 默认端口映射 |
|------|--------|------|-------------|
| `l2-relayer` | `l2-relayer-${CONTAINER_INDEX:-0}` | `l2-relayer:${DOCKER_TAG}` | `7088:7088`（Admin CLI）, `25005:25005`（Debug） |
| `l2-relayer-mysql` | `l2-relayer-mysql-${CONTAINER_INDEX:-0}` | `mysql:8.4.2` | `13306:3306` |
| `l2-relayer-redis` | `l2-relayer-redis-${CONTAINER_INDEX:-0}` | `redis:6.2` | 无 |
| `query-service` | `query-service-${CONTAINER_INDEX:-0}` | `l2-relayer-query-service:${DOCKER_TAG}` | `8080:8080`, `25006:25006`（Debug） |

### 2.2 启动与管理

```bash
# 启动全部服务
docker compose -f compose.yaml up -d

# 仅重启 Relayer（修改配置后）
docker compose -f compose.yaml up -d l2-relayer

# 查看日志
docker compose -f compose.yaml logs -f l2-relayer

# 停止全部服务（优雅停止，Relayer 有 5 分钟的 stop_grace_period）
docker compose -f compose.yaml down
```

### 2.3 Relayer 服务环境变量（必填项）

以下标记 🌟 的变量为必填项，容器启动时 `entrypoint.sh` 会校验这些变量是否存在。

#### 数据库配置

| 变量 | 说明 | 示例值 |
|------|------|--------|
| 🌟 `MYSQL_HOST` | MySQL 的 host 或 IP | `l2-relayer-mysql` |
| 🌟 `MYSQL_PORT` | MySQL 的端口 | `3306` |
| 🌟 `MYSQL_USER_NAME` | MySQL 用户名 | `root` |
| 🌟 `MYSQL_USER_PASSWORD` | MySQL 用户密码 | - |
| `MYSQL_DB_NAME` | 数据库名称 | 默认 `l2relayer` |

#### Redis 配置

| 变量 | 说明 | 示例值 |
|------|------|--------|
| 🌟 `REDIS_URL` | Redis 的 host 或 IP | `l2-relayer-redis` |
| 🌟 `REDIS_PORT` | Redis 端口 | `6379` |
| 🌟 `REDIS_USER_PASSWORD` | Redis 密码，支持 `user:pwd` 或 `pwd` 格式 | - |
| `REDIS_DATABASE` | Redis 数据库编号 | 默认 `0` |

#### L1 链配置

| 变量 | 说明 | 注意事项 |
|------|------|---------|
| 🌟 `L1_RPC_URL` (compose中为`L1_RPC_URL_INPUT`) | L1 链的 RPC 地址 | - |
| 🌟 `L1_ROLLUP_CONTRACT` (compose中为`L1_ROLLUP_CONTRACT_INPUT`) | L1 Rollup 合约地址 | 需提前部署 |
| 🌟 `L1_MAILBOX_CONTRACT` | L1 Mailbox 合约地址 | 需提前部署 |

#### L2 链配置

| 变量 | 说明 | 注意事项 |
|------|------|---------|
| 🌟 `L2_RPC_URL` (compose中为`L2_RPC_URL_INPUT`) | L2 链的 RPC 地址 | - |

#### Tracer 配置

| 变量 | 说明 |
|------|------|
| 🌟 `TRACER_IP` | Tracer Service 的 IP 地址 |
| 🌟 `TRACER_PORT` | Tracer Service 的 gRPC 端口 |

#### Prover Controller 配置

| 变量 | 说明 | 示例值 |
|------|------|--------|
| 🌟 `PROVER_CONTROLLER_ENDPOINTS` | PC 端点地址，多个以逗号分隔 | `192.168.1.18:54101,192.168.1.18:54102` |

> 也可以分别设置 `PROVER_CONTROLLER_IP` 和 `PROVER_CONTROLLER_PORT`，entrypoint 脚本会自动拼接。

#### 签名服务配置

Relayer 需要三组签名服务分别用于：L1 Blob 交易（提交 Batch）、L1 Legacy 交易（提交 Proof）、L2 交易。

**⚠️ 重要：L1 Blob Pool 和 L1 Legacy Pool 的私钥不可以相同，也不可以与其他应用复用！**

| 变量 | 说明 |
|------|------|
| 🌟 `L1_LEGACY_POOL_TX_SIGN_SERVICE_TYPE` | Legacy Pool 签名类型：`web3j_native` 或 `aliyun_kms` |
| 🌟 `L1_BLOB_POOL_TX_SIGN_SERVICE_TYPE` | Blob Pool 签名类型：`web3j_native` 或 `aliyun_kms` |
| 🌟 `L2_TX_SIGN_SERVICE_TYPE` | L2 签名类型：`web3j_native` 或 `aliyun_kms` |

**如果使用 `web3j_native`（明文私钥）：**

| 变量 | 说明 |
|------|------|
| 🌟 `L1_CLIENT_LEGACY_POOL_TX_PRIVATE_KEY` | L1 Legacy Pool 私钥（Hex，无 0x 前缀） |
| 🌟 `L1_CLIENT_BLOB_POOL_TX_PRIVATE_KEY` | L1 Blob Pool 私钥（Hex，无 0x 前缀） |
| 🌟 `L2_CLIENT_PRIVATE_KEY` | L2 私钥（Hex，无 0x 前缀） |

**如果使用 `aliyun_kms`（阿里云 KMS，生产推荐）：**

对每个签名服务（`LEGACY_POOL` / `BLOB_POOL` / `L2`），需配置以下变量（以 `L1_CLIENT_LEGACY_POOL_KMS_` 为前缀的示例）：

| 变量 | 说明 |
|------|------|
| 🌟 `..._KMS_ENDPOINT` | KMS 服务端点 |
| 🌟 `..._KMS_ACCESS_KEY_ID` | KMS Access Key ID |
| 🌟 `..._KMS_ACCESS_KEY_SECRET` | KMS Access Key Secret |
| 🌟 `..._KMS_PRIVATE_KEY_ID` | KMS 中的私钥标识 |
| 🌟 `..._KMS_PRIVATE_KEY_VERSION_ID` | 私钥版本标识 |
| `..._KMS_PUBLIC_KEY` | 对应公钥 |
| 🌟 `..._KMS_CA` | KMS 服务的 TLS 证书 |

#### Rollup Specs 配置

| 变量 | 说明 | 可选值 |
|------|------|--------|
| 🌟 `ROLLUP_SPECS_NETWORK` | 网络类型 | `mainnet`, `testnet`, `PRIVATE_NET` |
| `ROLLUP_SPECS_PRIVATE_NET_FILE` | 私有网络 Specs 文件路径（`PRIVATE_NET` 时必填） | 容器内路径 |

#### Proof 类型配置

| 变量 | 说明 | 可选值 |
|------|------|--------|
| 🌟 `BATCH_PROVE_REQ_TYPES` | 处理的 Proof 类型 | `ALL`（默认）, `TEE_ONLY`, `ZK_ONLY` |

### 2.4 Relayer 服务环境变量（可选项）

#### 监控与 Tracing

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `OTEL_SERVICE_NAME` | OpenTelemetry 服务名 | `l2-relayer-${CONTAINER_INDEX:-0}` |
| `OTEL_EXPORTER_OTLP_TRACES_ENDPOINT` | OTLP Traces 端点 | 不填则不启用 |
| `ACC_BALANCE_MONITOR_SWITCH` | 开启账户余额监控 | `false` |
| `ACC_BALANCE_MONITOR_INTERVAL` | 余额检查间隔（ms） | `10000` |
| `ACC_BALANCE_MONITOR_BALANCE_THRESHOLD` | 余额告警阈值（ETH） | `3.0` |
| `SELF_REPORT_URL` | 自定义监控上报 URL | 空 |
| `SELF_REPORT_SWITCH` | 自定义监控上报开关 | `false` |

#### L1 客户端调优

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `L1_CONSISTENCY_LEVEL` | L1 区块共识水位 | `FINALIZED`（支持 `LATEST`, `SAFE`） |
| `L1_MAX_POLLING_BLOCK_SIZE` | L1 每轮最大拉块数 | `32` |
| `L1_CLIENT_GAS_PRICE_POLICY` | Gas Price 策略 | `FROM_API`（动态）或 `STATIC` |
| `L1_CLIENT_STATIC_GAS_PRICE` | 静态 Gas Price（Wei） | `4100000000`（4.1 Gwei） |
| `L1_CLIENT_GAS_LIMIT_POLICY` | Gas Limit 策略 | `STATIC` 或 `ESTIMATE` |
| `L1_CLIENT_STATIC_GAS_LIMIT` | 静态 Gas Limit | `7200000` |
| `L1_CLIENT_EXTRA_GAS` | Gas 估算额外缓冲 | `400000` |
| `L1_CLIENT_HTTP_CLIENT_PROTOCOLS` | HTTP 协议版本 | `HTTP_1_1` |
| `L1_CLIENT_HTTP_CLIENT_WRITE_TIMEOUT` | HTTP 写超时（秒） | `60` |
| `L1_CLIENT_HTTP_CLIENT_READ_TIMEOUT` | HTTP 读超时（秒） | `60` |
| `L1_BLOB_POOL_TX_TRAFFIC_LIMIT` | Blob Pool 最大并发交易数 | `16`（-1 无限制） |
| `L1_LEGACY_POOL_TX_TRAFFIC_LIMIT` | Legacy Pool 最大并发交易数 | `-1`（无限制） |
| `ETH_NETWORK_FORK_UNKNOWN_NETWORK_CONFIG_FILE` | 非 mainnet/sepolia 网络的 BPO 配置文件 | `null` |

#### L1 交易加速

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `L1_CLIENT_TX_SPEEDUP_BLOB_FEE_LIMIT` | 加速时最大 Blob Fee（Wei） | `1000000000000`（1000 Gwei） |
| `L1_CLIENT_TX_SPEEDUP_PRIORITY_FEE_LIMIT` | 加速时最大 Priority Fee（Wei） | `100000000000`（100 Gwei） |
| `L1_CLIENT_TX_SPEEDUP_BLOB_PRICE_BUMP` | Blob Price Bump 倍数 | `1` |
| `L1_CLIENT_FORCE_TX_SPEEDUP_TIME_LIMIT` | 强制加速等待时间（ms） | `900000`（15 分钟） |

#### L2 客户端

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `L2_COINBASE_PROXY_CONTRACT` | L2 Coinbase Proxy 合约地址 | `0x7100000000000000000000000000000000000000` |
| `L2_GAS_ORACLE_CONTRACT` | L2 Gas Oracle 合约地址 | `0x8100000000000000000000000000000000000000` |
| `L2_CLIENT_EXTRA_GAS` | L2 Gas 估算额外缓冲 | `500000` |
| `L2_CONSISTENCY_LEVEL` | L2 区块共识水位 | `FINALIZED` |

#### Rollup 核心参数

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `ROLLUP_DA_TYPE` | DA 类型 | `BLOBS`（支持 `DAS`） |
| `ROLLUP_DA_CONFIG_SERVICE_TYPE` | DA 服务类型 | `LOCAL` |
| `ROLLUP_CONFIG_PARENT_CHAIN_TYPE` | Parent Chain 类型 | `ETHEREUM`（支持 `JOVAY`） |
| `ROLLUP_CONFIG_GAS_PER_CHUNK_RECOMMENDED` | Chunk 推荐 Gas 上限 | `23000000`（23M） |
| `ROLLUP_CONFIG_MAX_CHUNKS_MEMORY_USED` | Growing Batch Chunks 最大内存 | `1073741824`（1GB） |

#### 经济策略（择时提交）

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `ROLLUP_ECONOMIC_STRATEGY_CONF_SWITCH` | 择时提交开关 | `true` |
| `ROLLUP_ECONOMIC_STRATEGY_CONF_DEFAULT_MID_EIP1559_PRICE_LIMIT` | Gas Price 黄区起始（Wei） | `3000000000`（3 Gwei） |
| `ROLLUP_ECONOMIC_STRATEGY_CONF_DEFAULT_HIGH_EIP1559_PRICE_LIMIT` | Gas Price 红区起始（Wei） | `8000000000`（8 Gwei） |
| `ROLLUP_ECONOMIC_STRATEGY_CONF_DEFAULT_MAX_PENDING_BATCH_COUNT` | 最大待提交 Batch 数 | `12` |
| `ROLLUP_ECONOMIC_STRATEGY_CONF_DEFAULT_MAX_PENDING_PROOF_COUNT` | 最大待提交 Proof 数 | `12` |
| `ROLLUP_ECONOMIC_STRATEGY_CONF_DEFAULT_MAX_BATCH_WAITING_TIME` | Batch 最长等待时间（秒） | `43200`（12 小时） |
| `ROLLUP_ECONOMIC_STRATEGY_CONF_DEFAULT_MAX_PROOF_WAITING_TIME` | Proof 最长等待时间（秒） | `43200`（12 小时） |

> 择时提交将 Gas Price 分为绿/黄/红三个区间：绿区（< mid）正常提交，黄区（mid ~ high）根据待提交数量判断，红区（> high）除非超时否则暂停提交。

#### 告警配置

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `ROLLUP_ALARM_SWITCH` | 告警总开关 | `true` |
| `ROLLUP_ALARM_BATCH_DELAYED_THRESHOLD` | Batch 难产告警阈值（ms） | `10800000`（3 小时） |
| `ROLLUP_ALARM_CHUNK_DELAYED_THRESHOLD` | Chunk 难产告警阈值（ms） | `3660000`（1 小时 1 分） |
| `ROLLUP_ALARM_TEE_PROOF_DELAYED_THRESHOLD` | TEE Proof 超时告警阈值（ms） | `300000`（5 分钟） |
| `ROLLUP_ALARM_ZK_PROOF_DELAYED_THRESHOLD` | ZK Proof 超时告警阈值（ms） | `1800000`（30 分钟） |
| `ROLLUP_ALARM_L2_BLOCK_DELAYED_THRESHOLD` | L2 区块更新超时告警（ms） | `300000`（5 分钟） |
| `ROLLUP_ALARM_TX_OVER_PENDING_THRESHOLD` | 交易 Pending 超时告警（ms） | `1800000`（30 分钟） |
| `ROLLUP_ALARM_MAX_GAP_BETWEEN_BATCH_AND_PROOF_COMMIT` | Batch 与 Proof 提交最大间距 | `20` |
| `ROLLUP_ALARM_CIRCUIT_BREAKER_THRESHOLD` | 熔断告警阈值（ms） | `18000000`（5 小时） |
| `ROLLUP_ALARM_MAX_BLOCK_GAP_BETWEEN_TRACER_AND_SEQ` | Tracer 与 Sequencer 最大区块差 | `10` |

#### 缓存配置

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `L2_BLOCK_TRACE_CACHE_FOR_CURR_CHUNK_CAPACITY` | 当前 Chunk Block Trace 缓存容量 | `1300` |
| `L2_BLOCK_TRACE_CACHE_TTL` | Block Trace 缓存 TTL（ms） | `4200000`（70 分钟） |
| `L2_CHUNK_CACHE_TTL` | Chunk 缓存 TTL（ms） | `4200000`（70 分钟） |
| `L2_BATCH_ETH_BLOBS_CACHE_TTL` | Batch Blobs 缓存 TTL（ms） | `300000`（5 分钟） |
| `L2_BLOCK_POLLING_GROWING_BATCH_CHUNKS_MEM_CACHE_ASYNC_CORE_SIZE` | 异步缓存填充线程数 | `4` |

#### 任务配置

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `BATCH_COMMIT_WINDOWS_LENGTH` | Batch 并发提交窗口大小 | `12` |
| `L2_BLOCK_POLLING_GET_BLOCK_TIMEOUT` | Block Trace 请求超时（秒） | `10` |
| `PROOF_COMMIT_ROLLUP_QUERY_LEVEL` | Proof 提交时 Rollup 查询水位 | `LATEST` |
| `PROOF_COMMIT_ROLLUP_QUERY_HEIGHT_BACKOFF` | Rollup 查询高度回退 | `0` |
| `RELIABLE_TX_PARENT_CHAIN_TX_MISSED_TOLERANT_TIME` | Parent Chain 交易丢失容忍时间（秒） | `5` |
| `RELIABLE_TX_SUBCHAIN_TX_MISSED_TOLERANT_TIME` | Subchain 交易丢失容忍时间（秒） | `5` |
| `RELIABLE_TX_TX_TIMEOUT_LIMIT` | 交易超时时间（秒） | `600`（10 分钟） |
| `RELIABLE_TX_PROCESS_BATCH_SIZE` | 每批次处理交易数 | `10` |
| `RELIABLE_TX_RETRY_LIMIT` | 失败交易最大重试次数 | `1` |
| `ORACLE_GAS_FEED_ORACLE_REQ_NUMBER_PER_ROUND_LIMIT` | 每轮 Oracle 最大请求数 | `10` |
| `ORACLE_GAS_FEED_ORACLE_BASE_FEE_UPDATE_THRESHOLD` | Base Fee 更新阈值 | `0` |
| `ORACLE_GAS_FEED_ORACLE_BASE_PROOF` | 基准 Proof 类型 | `TEE_PROOF` |

#### Tracer 重试配置

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `TRACER_CLIENT_REQ_RETRY_MAX_ATTEMPTS` | 最大重试次数 | `10` |
| `TRACER_CLIENT_REQ_RETRY_BACKOFF_DELAY` | 基础重试间隔（ms） | `5` |
| `TRACER_CLIENT_REQ_RETRY_BACKOFF_MULTIPLIER` | 间隔乘数（指数退避） | `10` |
| `TRACER_CLIENT_REQ_RETRY_BACKOFF_MAX_DELAY` | 最大重试间隔（ms） | `500` |

#### 引擎配置

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `DUTY_PERIOD` | Duty 调度周期（ms） | `100` |
| `ENGINE_SHUTDOWN_AWAIT_TERMINATE_TIME` | 退出时等待线程完成时间（ms） | `60000`（1 分钟） |
| `ENGINE_SHUTDOWN_BATCH_COMMIT_AWAIT_TERMINATE_TIME` | Batch Commit 退出等待时间（ms） | `-1`（无限等待） |
| `ENGINE_SHUTDOWN_PROOF_COMMIT_AWAIT_TERMINATE_TIME` | Proof Commit 退出等待时间（ms） | `-1` |
| `ENGINE_SHUTDOWN_RELIABLE_TX_AWAIT_TERMINATE_TIME` | Reliable TX 退出等待时间（ms） | `-1` |

#### 端口与调试

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `ADMIN_EXPOSE_PORT` | Admin CLI 映射端口 | `7088` |
| `DEBUG_PORT` | JVM Debug 端口 | `25005` |
| `DEBUG_MODE` | JVM Debug 开关 | `off`（设为 `on` 开启） |
| `JASYPT_PASSWD` | Jasypt 加密密钥 | 空 |
| `CONTAINER_INDEX` | 容器实例编号（多实例部署） | `0` |

### 2.5 Query Service 环境变量

Query Service 的配置较为简单，只需要数据库连接信息：

| 变量 | 说明 | 必填 |
|------|------|------|
| 🌟 `MYSQL_HOST` | MySQL host | 是 |
| 🌟 `MYSQL_PORT` | MySQL 端口 | 是 |
| 🌟 `MYSQL_USER_NAME` | MySQL 用户名 | 是 |
| 🌟 `MYSQL_USER_PASSWORD` | MySQL 密码 | 是 |
| `MYSQL_DB_NAME` | 数据库名 | 默认 `l2relayer` |
| `JASYPT_PASSWD` | Jasypt 密钥 | 否 |
| `DEBUG_MODE` | Debug 开关 | 默认 `off` |
| `QS_EXPOSE_PORT` | Query Service 映射端口 | 默认 `8080` |

### 2.6 MySQL 服务配置

```yaml
l2-relayer-mysql:
  environment:
    - MYSQL_ROOT_PASSWORD=YOUR_PWD        # MySQL root 密码
    - TZ=Asia/Shanghai                     # 时区
  command: >-
    --max_allowed_packet=32505856          # 最大包大小 (~31MB)
    --default-time_zone=+8:00             # 默认时区 UTC+8
    --disable-log-bin                      # 禁用 binlog（单机模式）
  mem_limit: 1024m                         # 内存限制，可按需调整
  volumes:
    - ./mysql/data:/var/lib/mysql           # 数据持久化
    - ./mysql/conf:/etc/mysql/conf.d        # 自定义 MySQL 配置
```

> ⚠️ 注意：默认 `mem_limit` 为 1024M，生产环境建议根据数据量调整或移除限制。

### 2.7 Redis 服务配置

```yaml
l2-relayer-redis:
  command: >-
    --requirepass 'YOUR_PWD'               # Redis 密码
    --maxmemory 1024MB                     # 最大内存
    --maxmemory-policy volatile-lru        # 过期策略
```

### 2.8 使用 env_file 管理变量

对于多实例部署或变量较多的场景，推荐将环境变量提取到独立的 `.env` 文件中：

**compose.yaml 示例：**

```yaml
services:
  l2-relayer-0:
    image: l2-relayer:${DOCKER_TAG}
    env_file: ./relayer.env
    # ...
  l2-relayer-1:
    image: l2-relayer:${DOCKER_TAG}
    env_file: ./relayer.env
    # ...
```

**relayer.env 示例：**

```env
MYSQL_HOST=l2-relayer-mysql
MYSQL_PORT=3306
MYSQL_USER_NAME=root
MYSQL_USER_PASSWORD=YOUR_PWD
REDIS_URL=l2-relayer-redis
REDIS_PORT=6379
REDIS_USER_PASSWORD=YOUR_PWD
L1_RPC_URL=https://eth-mainnet.example.com
L1_ROLLUP_CONTRACT=0x...
L1_MAILBOX_CONTRACT=0x...
L2_RPC_URL=https://jovay-rpc.example.com
TRACER_IP=192.168.1.10
TRACER_PORT=5000
PROVER_CONTROLLER_ENDPOINTS=192.168.1.18:54101
BATCH_PROVE_REQ_TYPES=ALL
ROLLUP_SPECS_NETWORK=mainnet
L1_LEGACY_POOL_TX_SIGN_SERVICE_TYPE=web3j_native
L1_CLIENT_LEGACY_POOL_TX_PRIVATE_KEY=your_private_key_hex
L1_BLOB_POOL_TX_SIGN_SERVICE_TYPE=web3j_native
L1_CLIENT_BLOB_POOL_TX_PRIVATE_KEY=your_private_key_hex
L2_TX_SIGN_SERVICE_TYPE=web3j_native
L2_CLIENT_PRIVATE_KEY=your_private_key_hex
```

> ⚠️ 如果使用 Jasypt 加密密码，将密码值替换为 `ENC(密文)`，并设置 `JASYPT_PASSWD` 为对应密钥。

---

## 3. Relayer 进程配置详解

Relayer 进程配置位于 `relayer-app/src/main/resources/application-prod.yml`，所有配置项均支持通过环境变量覆盖，格式为 `${ENV_VAR:default_value}`。

### 3.1 Spring 基础配置

```yaml
spring:
  application:
    name: l2-relayer
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://${MYSQL_HOST}:${MYSQL_PORT}/${MYSQL_DB_NAME}?serverTimezone=Asia/Shanghai&rewriteBatchedStatements=true&createDatabaseIfNotExist=true
    password: ${MYSQL_USER_PASSWORD}
    username: ${MYSQL_USER_NAME}
  data:
    redis:
      host: ${REDIS_URL}
      port: ${REDIS_PORT}
      password: ${REDIS_USER_PASSWORD}
      database: ${REDIS_DATABASE:0}
```

数据库使用 MySQL，启用了 `rewriteBatchedStatements` 优化批量插入性能，`createDatabaseIfNotExist` 自动创建数据库。数据库 Schema 通过 Flyway 自动迁移管理。

### 3.2 Rollup 核心配置

```yaml
l2-relayer:
  rollup:
    da-type: ${ROLLUP_DA_TYPE:BLOBS}              # BLOBS 或 DAS
    da-config:
      service:
        type: ${ROLLUP_DA_CONFIG_SERVICE_TYPE:LOCAL}
    config:
      parent-chain-type: ${ROLLUP_CONFIG_PARENT_CHAIN_TYPE:ETHEREUM}
      gas-per-chunk-recommended: ${ROLLUP_CONFIG_GAS_PER_CHUNK_RECOMMENDED:23000000}
      max-chunks-memory-used: ${ROLLUP_CONFIG_MAX_CHUNKS_MEMORY_USED:1073741824}
    specs:
      network: ${ROLLUP_SPECS_NETWORK:mainnet}
      private-net:
        specs-file: ${ROLLUP_SPECS_PRIVATE_NET_FILE:null}
```

- **`da-type`**：数据可用性类型。以太坊作为 Parent Chain 时使用 `BLOBS`（EIP-4844），Jovay 作为 Parent Chain（L3 场景）时使用 `DAS`
- **`parent-chain-type`**：Parent Chain 类型，`ETHEREUM` 或 `JOVAY`
- **`gas-per-chunk-recommended`**：Chunk 切分时的 Gas 上限阈值，影响 ZK Prover 的证明效率
- **`max-chunks-memory-used`**：Growing Batch 内序列化 Chunks 的内存上限，防止 OOM

### 3.3 经济策略配置（择时提交）

择时提交策略根据当前 L1 Gas Price 动态决定是否提交交易，以降低 L1 成本：

```
Gas Price 区间：
  ├── 绿区 [0, mid)     → 正常提交
  ├── 黄区 [mid, high)  → 根据待提交数量和等待时间判断
  └── 红区 [high, ∞)    → 暂停提交，除非超过最大等待时间
```

| 配置项 | 说明 |
|--------|------|
| `switch` | 总开关，默认开启 |
| `default-mid-eip1559-price-limit` | 黄区下界（Wei），默认 3 Gwei |
| `default-high-eip1559-price-limit` | 红区下界（Wei），默认 8 Gwei |
| `default-max-pending-batch-count` | 黄区内触发提交的待提交 Batch 数阈值 |
| `default-max-pending-proof-count` | 黄区内触发提交的待提交 Proof 数阈值 |
| `default-max-batch-waiting-time` | 超时强制提交 Batch 的时间（秒） |
| `default-max-proof-waiting-time` | 超时强制提交 Proof 的时间（秒） |

### 3.4 L1 客户端配置

#### 连接与合约

```yaml
l2-relayer:
  l1-client:
    rpc-url: ${L1_RPC_URL}                          # L1 RPC 端点（必填）
    rollup-contract: ${L1_ROLLUP_CONTRACT}           # Rollup 合约地址（必填）
    mailbox-contract: ${L1_MAILBOX_CONTRACT}         # Mailbox 合约地址（必填）
    nonce-policy: FAST                                # Nonce 管理：FAST 优化模式
```

#### Gas 策略

| 配置项 | 环境变量 | 说明 | 默认值 |
|--------|----------|------|--------|
| `gas-price-policy` | `L1_CLIENT_GAS_PRICE_POLICY` | Gas Price 获取策略 | `FROM_API` |
| `static-gas-price` | `L1_CLIENT_STATIC_GAS_PRICE` | STATIC 模式固定 Gas Price（Wei） | `4100000000` |
| `gas-limit-policy` | `L1_CLIENT_GAS_LIMIT_POLICY` | Gas Limit 获取策略 | `STATIC` |
| `static-gas-limit` | `L1_CLIENT_STATIC_GAS_LIMIT` | STATIC 模式固定 Gas Limit | `7200000` |
| `extra-gas` | `L1_CLIENT_EXTRA_GAS` | ESTIMATE 模式额外 Gas 缓冲 | `400000` |

#### 交易流量控制

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `blob-pool-tx-traffic-limit` | Blob Pool 最大未打包交易数 | `16` |
| `legacy-pool-tx-traffic-limit` | Legacy Pool 最大未打包交易数 | `-1`（不限制） |

#### HTTP 客户端

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `protocols` | HTTP 协议版本 | `HTTP_1_1` |
| `write-timeout` | 写超时（秒） | `60` |
| `read-timeout` | 读超时（秒） | `60` |

### 3.5 L2 客户端配置

```yaml
l2-relayer:
  l2-client:
    rpc-url: ${L2_RPC_URL}
    coinbase-contract: ${L2_COINBASE_PROXY_CONTRACT:0x7100000000000000000000000000000000000000}
    gas-oracle-contract: ${L2_GAS_ORACLE_CONTRACT:0x8100000000000000000000000000000000000000}
    extra-gas: ${L2_CLIENT_EXTRA_GAS:500000}
```

### 3.6 Tracer 客户端配置

Tracer 客户端使用指数退避重试策略：

```yaml
l2-relayer:
  tracer-client:
    req-retry:
      max-attempts: 10        # 最大重试 10 次
      backoff-delay: 5         # 基础间隔 5ms
      backoff-multiplier: 10   # 每次乘 10
      backoff-max-delay: 500   # 最大间隔 500ms
```

重试序列示例：5ms → 50ms → 500ms → 500ms → ...

### 3.7 任务配置

Relayer 内部通过分布式任务引擎调度以下核心任务：

| 任务 | 配置前缀 | 说明 |
|------|----------|------|
| BlockPolling | `tasks.block-polling` | L1/L2 区块轮询 |
| BatchCommit | `tasks.batch-commit` | Batch 提交到 L1 |
| BatchProve | `tasks.batch-prove` | 向 PC 请求 Proof |
| ProofCommit | `tasks.proof-commit` | Proof 提交到 L1 |
| ReliableTx | `tasks.reliable-tx` | 可靠交易确认 |
| OracleGasFeed | `tasks.oracle-gas-feed` | L1 Gas 价格同步到 L2 |

### 3.8 告警配置

告警系统监控 Rollup 流程的关键指标，当指标超过阈值时输出告警日志：

```yaml
l2-relayer:
  alarm:
    rollup:
      switch: true
      batch-delayed-threshold: 10800000      # Batch 3 小时不产出则告警
      chunk-delayed-threshold: 3660000       # Chunk 1 小时 1 分不产出则告警
      tee-proof-delayed-threshold: 300000    # TEE Proof 5 分钟未生成则告警
      zk-proof-delayed-threshold: 1800000    # ZK Proof 30 分钟未生成则告警
      l2-block-delayed-threshold: 300000     # L2 区块 5 分钟未更新则告警
      tx-over-pending-threshold: 1800000     # 交易 30 分钟未打包则告警
      circuit-breaker-threshold: 18000000    # 5 小时未恢复则熔断告警
```

### 3.9 监控配置

```yaml
l2-relayer:
  metrics:
    self-report:
      url: ${SELF_REPORT_URL:}
      switch: false
    blockchain:
      acc-balance-monitor:
        switch: false
        l1-interval: 10000        # 每 10 秒检查一次余额
        l1-balance-threshold: 3.0  # 低于 3 ETH 告警
```

### 3.10 引擎配置

```yaml
l2-relayer:
  engine:
    shutdown:
      await-terminate-time: 60000   # 默认退出等待 1 分钟
      batch-commit:
        await-terminate-time: -1    # Batch Commit 无限等待直到完成
      proof-commit:
        await-terminate-time: -1
      reliable-tx:
        await-terminate-time: -1
    schedule:
      duty:
        period: 100                  # Duty 调度周期 100ms
```

> Relayer 使用 `stop_grace_period: 5m` 提供充足的优雅关闭时间，确保正在处理的 L1 交易能完成确认。

### 3.11 gRPC 客户端配置

```yaml
grpc:
  client:
    prover-client:
      address: "static://${PROVER_CONTROLLER_ENDPOINTS}"
      negotiation-type: plaintext
    tracer-client:
      address: "static://${TRACER_IP}:${TRACER_PORT:5000}"
      negotiation-type: plaintext
```

### 3.12 签名服务配置

Relayer 支持两种签名方式：

| 方式 | 适用场景 | 说明 |
|------|----------|------|
| `web3j_native` | 开发/测试 | 直接使用明文私钥签名 |
| `kms` | 生产环境 | 通过阿里云 KMS 签名，更安全 |

配置路径结构：

```yaml
jovay:
  sign-service:
    l1LegacyPoolTxSignService:    # L1 非 Blob 交易（如 Proof 提交）
      type: web3j_native | kms
      web3j-native:
        private-key: ...
      kms:
        endpoint: ...
        access-key-id: ...
        # ...

    l1BlobPoolTxSignService:      # L1 Blob 交易（Batch 提交）
      type: web3j_native | kms
      # 同上结构

    l2TxSignService:              # L2 交易
      type: web3j_native | kms
      # 同上结构
```

### 3.13 Jasypt 加密配置

```yaml
jasypt:
  encryptor:
    password: ${JASYPT_PASSWD:}
```

Jasypt 用于加密配置文件中的敏感值。加密后的值格式为 `ENC(密文)`，运行时使用 `JASYPT_PASSWD` 解密。

---

## 4. 动态配置（Admin CLI）

Relayer 支持通过 [Admin CLI](../admin-cli/README_CN.md) 在运行时动态修改部分配置，无需重启服务。

### 4.1 启动 CLI

**方式一：进入容器交互式 CLI**

```bash
docker exec -it l2-relayer-0 /l2-relayer/bin/relayer-cli/bin/start.sh
```

**方式二：直接执行命令（非交互式）**

```bash
docker exec -it l2-relayer-0 java -jar bin/relayer-cli/lib/relayer-cli.jar <command> [options]
```

完整命令列表请参阅 [Admin CLI 使用指南](../admin-cli/README_CN.md)。

### 4.2 Gas 价格动态调整

支持运行时查询和更新的 Gas 配置：

| 查询命令 | 更新命令 | 说明 |
|----------|----------|------|
| `get-eth-gas-price-increased-percentage` | `update-eth-gas-price-increased-percentage --value <Double>` | Gas Price 增加百分比 |
| `get-eth-max-price-limit` | `update-eth-max-price-limit --value <String>` | 最大 Gas Price 限制（Wei） |
| `get-eth-priority-fee-per-gas-increased-percentage` | `update-eth-priority-fee-per-gas-increased-percentage --value <Double>` | Priority Fee 增加百分比 |
| `get-eth-minimum-eip1559priority-price` | `update-eth-minimum-eip1559priority-price --value <String>` | 最小 EIP-1559 Priority Price（Wei） |
| `get-eth-base-fee-multiplier` | `update-eth-base-fee-multiplier --value <String>` | Base Fee 乘数 |
| `get-eth-eip4844priority-fee-per-gas-increased-percentage` | `update-eth-eip4844priority-fee-per-gas-increased-percentage --value <Double>` | EIP-4844 Priority Fee 增加百分比 |
| `get-eth-minimum-eip4844priority-price` | `update-eth-minimum-eip4844priority-price --value <String>` | 最小 EIP-4844 Priority Price（Wei） |
| `get-eth-extra-gas-price` | `update-eth-extra-gas-price --value <String>` | 额外 Gas Price（Wei） |
| `get-eth-larger-fee-per-blob-gas-multiplier` | `update-eth-larger-fee-per-blob-gas-multiplier --value <String>` | Blob Gas 大倍数 |
| `get-eth-smaller-fee-per-blob-gas-multiplier` | `update-eth-smaller-fee-per-blob-gas-multiplier --value <String>` | Blob Gas 小倍数 |
| `get-eth-fee-per-blob-gas-dividing-val` | `update-eth-fee-per-blob-gas-dividing-val --value <String>` | Blob Gas 分界值（Wei） |

### 4.3 经济策略动态调整

| 查询命令 | 更新命令 | 说明 |
|----------|----------|------|
| `get-max-pending-batch-count` | `update-max-pending-batch-count --value <Integer>` | 最大待提交 Batch 数 |
| `get-max-pending-proof-count` | `update-max-pending-proof-count --value <Integer>` | 最大待提交 Proof 数 |
| `get-max-batch-waiting-time` | `update-max-batch-waiting-time --value <Long>` | Batch 最长等待时间（秒） |
| `get-max-proof-waiting-time` | `update-max-proof-waiting-time --value <Long>` | Proof 最长等待时间（秒） |
| `get-mid-eip1559price-limit` | `update-mid-eip1559price-limit --value <String>` | 黄区下界（Wei） |
| `get-high-eip1559price-limit` | `update-high-eip1559price-limit --value <String>` | 红区下界（Wei） |

**示例：调整择时提交策略应对 Gas 高峰**

```bash
# 查看当前策略
docker exec -it l2-relayer-0 java -jar bin/relayer-cli/lib/relayer-cli.jar get-mid-eip1559price-limit
docker exec -it l2-relayer-0 java -jar bin/relayer-cli/lib/relayer-cli.jar get-high-eip1559price-limit

# 放宽阈值：黄区上调到 5 Gwei，红区上调到 15 Gwei
docker exec -it l2-relayer-0 java -jar bin/relayer-cli/lib/relayer-cli.jar update-mid-eip1559price-limit --value 5000000000
docker exec -it l2-relayer-0 java -jar bin/relayer-cli/lib/relayer-cli.jar update-high-eip1559price-limit --value 15000000000

# 增加最大等待时间到 24 小时
docker exec -it l2-relayer-0 java -jar bin/relayer-cli/lib/relayer-cli.jar update-max-batch-waiting-time --value 86400
```

---

## 5. 配置变更记录

### v0.7.0（Base）

初始版本，包含所有基础配置项。详见第 2.3 和 2.4 节。

### v0.8.0

**新增：**
- 签名服务升级：`L1_LEGACY_POOL_TX_SIGN_SERVICE_TYPE`、`L1_BLOB_POOL_TX_SIGN_SERVICE_TYPE`、`L2_TX_SIGN_SERVICE_TYPE` 及对应 KMS 配置
- Rollup Specs：`ROLLUP_SPECS_NETWORK`、`ROLLUP_SPECS_PRIVATE_NET_FILE`
- Oracle Gas Feed：`ORACLE_GAS_FEED_*` 系列配置
- L1 交易手续费精细化：`L1_CLIENT_EIP4844_PRIORITY_FEE_PER_GAS_INCREASED_PERCENTAGE`、`L1_CLIENT_MINIMUM_EIP1559_PRIORITY_PRICE`、`L1_CLIENT_MINIMUM_EIP4844_PRIORITY_PRICE`
- L2 合约：`L2_COINBASE_PROXY_CONTRACT`、`L2_GAS_ORACLE_CONTRACT`

**删除：**
- `PROOF_COMMIT_WINDOWS_LENGTH`

### v0.8.1

**新增：**
- `L1_CLIENT_BLOB_SIDECAR_VERSION`：Blob 版本号（sepolia 为 1，以太坊主网默认 0）

### v0.9.0

**新增：**
- 择时提交全部配置：`ROLLUP_ECONOMIC_STRATEGY_CONF_*` 系列
- 缓存 TTL 配置：`L2_BLOCK_TRACE_CACHE_TTL`、`L2_CHUNK_CACHE_TTL`、`L2_BATCH_ETH_BLOBS_CACHE_TTL`
- 异步缓存线程：`L2_BLOCK_POLLING_GROWING_BATCH_CHUNKS_MEM_CACHE_ASYNC_CORE_SIZE`
- 熔断告警：`ROLLUP_ALARM_CIRCUIT_BREAKER_THRESHOLD`
- Oracle 基准 Proof：`ORACLE_GAS_FEED_ORACLE_BASE_PROOF`
- 非标网络 BPO：`ETH_NETWORK_FORK_UNKNOWN_NETWORK_CONFIG_FILE`
- 交易超时：`RELIABLE_TX_TX_TIMEOUT_LIMIT`
- 动态配置（[Admin CLI](../admin-cli/README_CN.md)）支持

**删除：**
- `L1_CLIENT_BLOB_SIDECAR_VERSION`

### v0.10.0

**新增：**
- `L2_BLOCK_TRACE_CACHE_FOR_CURR_CHUNK_CAPACITY`：当前 Chunk Block Trace 缓存容量
- `ROLLUP_CONFIG_GAS_PER_CHUNK_RECOMMENDED`：Chunk Gas 推荐值
- `ROLLUP_ALARM_TEE_PROOF_DELAYED_THRESHOLD`：TEE Proof 告警阈值（拆分自通用阈值）
- `ROLLUP_ALARM_ZK_PROOF_DELAYED_THRESHOLD`：ZK Proof 告警阈值

**删除：**
- `ROLLUP_CONFIG_ZK_VERIFICATION_START_BATCH`
- `TRACER_CLIENT_MAX_BLOCK_STABLE_GAP`
- `ROLLUP_ALARM_PROOF_DELAYED_THRESHOLD`（拆分为 TEE/ZK 独立阈值）

### v0.11.0

**新增：**
- `ROLLUP_CONFIG_MAX_CHUNKS_MEMORY_USED`：Chunks 内存上限
- 优雅关闭配置：`ENGINE_SHUTDOWN_*` 系列
- HTTP 超时：`L1_CLIENT_HTTP_CLIENT_WRITE_TIMEOUT`、`L1_CLIENT_HTTP_CLIENT_READ_TIMEOUT`

**修改：**
- `ROLLUP_ALARM_CHUNK_DELAYED_THRESHOLD`：默认值改为 3660000ms（1 小时 1 分钟）

### v0.12.0

**新增：**
- `ROLLUP_CONFIG_PARENT_CHAIN_TYPE`：Parent Chain 类型（`ETHEREUM` / `JOVAY`）
- `ROLLUP_DA_TYPE`：DA 类型（`BLOBS` / `DAS`）
- `RELIABLE_TX_PARENT_CHAIN_TX_MISSED_TOLERANT_TIME`：Parent Chain 交易丢失容忍时间
- `RELIABLE_TX_SUBCHAIN_TX_MISSED_TOLERANT_TIME`：Subchain 交易丢失容忍时间

---

## 6. 附录

### 6.1 Rollup Specs 配置示例

Rollup Specs 定义了 Rollup 协议的分叉规则和 Batch 版本号，通过时间戳触发升级。

**mainnet：**

```json
{
  "network": "mainnet",
  "layer2_chain_id": "5734951",
  "layer1_chain_id": "1",
  "forks": {
    "1": {
      "batch_version": 1
    },
    "4919284474000": {
      "batch_version": 2
    }
  }
}
```

**testnet：**

```json
{
  "network": "testnet",
  "layer2_chain_id": "2019775",
  "layer1_chain_id": "11155111",
  "forks": {
    "1": {
      "batch_version": 0
    },
    "1758327170000": {
      "batch_version": 1
    },
    "4919284474000": {
      "batch_version": 2
    }
  }
}
```

**private_net：**

- `network` 填 `private_net`
- `layer2_chain_id` 填对应环境 Jovay 的 Chain ID
- `layer1_chain_id` 填对应环境 L1 的 Chain ID
- 配置文件通过 volume 挂载到容器内，设置 `ROLLUP_SPECS_PRIVATE_NET_FILE` 指向文件路径

```yaml
volumes:
  - ./private-specs.json:/l2-relayer/config/private-specs.json
environment:
  - ROLLUP_SPECS_PRIVATE_NET_FILE=/l2-relayer/config/private-specs.json
```

### 6.2 BPO 文件示例

BPO（Blob Price Oracle）配置定义以太坊网络硬分叉参数，非 mainnet/sepolia 网络需要通过 `ETH_NETWORK_FORK_UNKNOWN_NETWORK_CONFIG_FILE` 指定：

```json
{
  "1746612311000": {
    "name": "Prague",
    "blob_sidecar_version": 0,
    "base_fee_update_fraction": 5007716
  },
  "1764798551000": {
    "name": "Osaka",
    "blob_sidecar_version": 1,
    "base_fee_update_fraction": 5007716
  }
}
```

挂载方式：

```yaml
volumes:
  - ./unknown-bpo.json:/l2-relayer/config/unknown-bpo.json
environment:
  - ETH_NETWORK_FORK_UNKNOWN_NETWORK_CONFIG_FILE=/l2-relayer/config/unknown-bpo.json
```

### 6.3 注意事项

1. **私钥安全**：L1 Blob Pool 和 L1 Legacy Pool 的私钥**必须不同**，且不可与其他应用复用。确保对应账户有充足的 L1 ETH。
2. **内存限制**：默认 compose 中 MySQL `mem_limit` 为 1024M，生产环境可按需调整或移除。
3. **Jasypt 加密**：生产环境推荐使用 Jasypt 加密敏感配置值，使用 `ENC(密文)` 格式，通过 `JASYPT_PASSWD` 解密。
4. **优雅关闭**：Relayer 的 `stop_grace_period` 为 5 分钟，`ENGINE_SHUTDOWN_*_AWAIT_TERMINATE_TIME` 默认为 -1（无限等待），确保正在处理的 L1 交易能完成确认再退出。
5. **多实例部署**：通过 `CONTAINER_INDEX` 区分实例，使用 `env_file` 共享配置。多实例共享同一 MySQL 和 Redis，通过 Redis 分布式锁协调任务执行。
6. **配置生效**：静态配置修改后需重启 Relayer 容器（`docker compose up -d l2-relayer`），动态配置通过 [Admin CLI](../admin-cli/README_CN.md) 修改后立即生效。
