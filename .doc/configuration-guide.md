# Relayer Configuration Guide

> [中文版](configuration-guide_CN.md)

This document provides a complete reference for configuring the Jovay L2 Relayer, covering both Docker Compose deployment configuration and Relayer process-level settings.

---

## Table of Contents

- [1. Deployment Overview](#1-deployment-overview)
- [2. Docker Compose Deployment](#2-docker-compose-deployment)
  - [2.1 Service Composition](#21-service-composition)
  - [2.2 Startup & Management](#22-startup--management)
  - [2.3 Relayer Environment Variables (Required)](#23-relayer-environment-variables-required)
  - [2.4 Relayer Environment Variables (Optional)](#24-relayer-environment-variables-optional)
  - [2.5 Query Service Environment Variables](#25-query-service-environment-variables)
  - [2.6 MySQL Service Configuration](#26-mysql-service-configuration)
  - [2.7 Redis Service Configuration](#27-redis-service-configuration)
  - [2.8 Using env_file for Variable Management](#28-using-env_file-for-variable-management)
- [3. Relayer Process Configuration](#3-relayer-process-configuration)
  - [3.1 Spring Boot Basics](#31-spring-boot-basics)
  - [3.2 Rollup Core Configuration](#32-rollup-core-configuration)
  - [3.3 Economic Timing Strategy](#33-economic-timing-strategy)
  - [3.4 L1 Client Configuration](#34-l1-client-configuration)
  - [3.5 L2 Client Configuration](#35-l2-client-configuration)
  - [3.6 Tracer Client Configuration](#36-tracer-client-configuration)
  - [3.7 Task Configuration](#37-task-configuration)
  - [3.8 Alarm Configuration](#38-alarm-configuration)
  - [3.9 Metrics & Monitoring](#39-metrics--monitoring)
  - [3.10 Engine Configuration](#310-engine-configuration)
  - [3.11 gRPC Client Configuration](#311-grpc-client-configuration)
  - [3.12 Signing Service Configuration](#312-signing-service-configuration)
  - [3.13 Jasypt Encryption](#313-jasypt-encryption)
- [4. Dynamic Configuration (Admin CLI)](#4-dynamic-configuration-admin-cli)
  - [4.1 Launching the CLI](#41-launching-the-cli)
  - [4.2 Gas Price Tuning](#42-gas-price-tuning)
  - [4.3 Economic Strategy Tuning](#43-economic-strategy-tuning)
- [5. Configuration Changelog](#5-configuration-changelog)
- [6. Appendix](#6-appendix)

---

## 1. Deployment Overview

Relayer is deployed via Docker Compose. A complete instance consists of four services:

```
┌─────────────────────────────────────────────────────────┐
│                   Docker Compose Stack                   │
│                                                         │
│  ┌──────────────┐     ┌──────────────────────────────┐  │
│  │  l2-relayer   │────▶│  l2-relayer-mysql (MySQL 8.4) │  │
│  │ (Core Service)│     └──────────────────────────────┘  │
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

- **l2-relayer**: Core Rollup service — L2 block polling, Chunk/Batch assembly, L1 transaction submission, Proof retrieval and commitment
- **l2-relayer-mysql**: MySQL 8.4 database for persisting Batches, Chunks, transactions, and cross-chain messages
- **l2-relayer-redis**: Redis 6.2 for distributed locking and caching
- **query-service**: REST API service for external Relayer state queries

---

## 2. Docker Compose Deployment

### 2.1 Service Composition

The configuration template is located at `docker/compose.yaml`. For multi-instance deployments, use the `CONTAINER_INDEX` environment variable to differentiate instances:

| Service | Container Name | Image | Default Port Mapping |
|---------|---------------|-------|---------------------|
| `l2-relayer` | `l2-relayer-${CONTAINER_INDEX:-0}` | `l2-relayer:${DOCKER_TAG}` | `7088:7088` (Admin CLI), `25005:25005` (Debug) |
| `l2-relayer-mysql` | `l2-relayer-mysql-${CONTAINER_INDEX:-0}` | `mysql:8.4.2` | `13306:3306` |
| `l2-relayer-redis` | `l2-relayer-redis-${CONTAINER_INDEX:-0}` | `redis:6.2` | None |
| `query-service` | `query-service-${CONTAINER_INDEX:-0}` | `l2-relayer-query-service:${DOCKER_TAG}` | `8080:8080`, `25006:25006` (Debug) |

### 2.2 Startup & Management

```bash
# Start all services
docker compose -f compose.yaml up -d

# Restart only Relayer (after config changes)
docker compose -f compose.yaml up -d l2-relayer

# View logs
docker compose -f compose.yaml logs -f l2-relayer

# Stop all services (graceful — Relayer has a 5-minute stop_grace_period)
docker compose -f compose.yaml down
```

### 2.3 Relayer Environment Variables (Required)

Variables marked with 🌟 are required. The container's `entrypoint.sh` validates their presence on startup.

#### Database

| Variable | Description | Example |
|----------|-------------|---------|
| 🌟 `MYSQL_HOST` | MySQL host or IP | `l2-relayer-mysql` |
| 🌟 `MYSQL_PORT` | MySQL port | `3306` |
| 🌟 `MYSQL_USER_NAME` | MySQL username | `root` |
| 🌟 `MYSQL_USER_PASSWORD` | MySQL password | - |
| `MYSQL_DB_NAME` | Database name | Default: `l2relayer` |

#### Redis

| Variable | Description | Example |
|----------|-------------|---------|
| 🌟 `REDIS_URL` | Redis host or IP | `l2-relayer-redis` |
| 🌟 `REDIS_PORT` | Redis port | `6379` |
| 🌟 `REDIS_USER_PASSWORD` | Redis password, supports `user:pwd` or `pwd` format | - |
| `REDIS_DATABASE` | Redis database index | Default: `0` |

#### L1 Chain

| Variable | Description | Notes |
|----------|-------------|-------|
| 🌟 `L1_RPC_URL` (`L1_RPC_URL_INPUT` in compose) | L1 chain RPC endpoint | - |
| 🌟 `L1_ROLLUP_CONTRACT` (`L1_ROLLUP_CONTRACT_INPUT` in compose) | L1 Rollup contract address | Must be pre-deployed |
| 🌟 `L1_MAILBOX_CONTRACT` | L1 Mailbox contract address | Must be pre-deployed |

#### L2 Chain

| Variable | Description | Notes |
|----------|-------------|-------|
| 🌟 `L2_RPC_URL` (`L2_RPC_URL_INPUT` in compose) | L2 chain RPC endpoint | - |

#### Tracer

| Variable | Description |
|----------|-------------|
| 🌟 `TRACER_IP` | Tracer Service IP address |
| 🌟 `TRACER_PORT` | Tracer Service gRPC port |

#### Prover Controller

| Variable | Description | Example |
|----------|-------------|---------|
| 🌟 `PROVER_CONTROLLER_ENDPOINTS` | PC endpoint(s), comma-separated for multiple | `192.168.1.18:54101,192.168.1.18:54102` |

> Alternatively, set `PROVER_CONTROLLER_IP` and `PROVER_CONTROLLER_PORT` separately — the entrypoint script concatenates them automatically.

#### Signing Service

Relayer requires three separate signing services for: L1 Blob transactions (Batch submission), L1 Legacy transactions (Proof submission), and L2 transactions.

**⚠️ Important: L1 Blob Pool and L1 Legacy Pool private keys MUST be different and MUST NOT be shared with other applications!**

| Variable | Description |
|----------|-------------|
| 🌟 `L1_LEGACY_POOL_TX_SIGN_SERVICE_TYPE` | Legacy Pool signing type: `web3j_native` or `aliyun_kms` |
| 🌟 `L1_BLOB_POOL_TX_SIGN_SERVICE_TYPE` | Blob Pool signing type: `web3j_native` or `aliyun_kms` |
| 🌟 `L2_TX_SIGN_SERVICE_TYPE` | L2 signing type: `web3j_native` or `aliyun_kms` |

**If using `web3j_native` (plaintext private key):**

| Variable | Description |
|----------|-------------|
| 🌟 `L1_CLIENT_LEGACY_POOL_TX_PRIVATE_KEY` | L1 Legacy Pool private key (hex, without 0x prefix) |
| 🌟 `L1_CLIENT_BLOB_POOL_TX_PRIVATE_KEY` | L1 Blob Pool private key (hex, without 0x prefix) |
| 🌟 `L2_CLIENT_PRIVATE_KEY` | L2 private key (hex, without 0x prefix) |

**If using `aliyun_kms` (Alibaba Cloud KMS — recommended for production):**

For each signing service (`LEGACY_POOL` / `BLOB_POOL` / `L2`), configure the following (shown with `L1_CLIENT_LEGACY_POOL_KMS_` prefix as example):

| Variable | Description |
|----------|-------------|
| 🌟 `..._KMS_ENDPOINT` | KMS service endpoint |
| 🌟 `..._KMS_ACCESS_KEY_ID` | KMS Access Key ID |
| 🌟 `..._KMS_ACCESS_KEY_SECRET` | KMS Access Key Secret |
| 🌟 `..._KMS_PRIVATE_KEY_ID` | Private key identifier in KMS |
| 🌟 `..._KMS_PRIVATE_KEY_VERSION_ID` | Private key version identifier |
| `..._KMS_PUBLIC_KEY` | Corresponding public key |
| 🌟 `..._KMS_CA` | TLS certificate for KMS service |

#### Rollup Specs

| Variable | Description | Values |
|----------|-------------|--------|
| 🌟 `ROLLUP_SPECS_NETWORK` | Network type | `mainnet`, `testnet`, `PRIVATE_NET` |
| `ROLLUP_SPECS_PRIVATE_NET_FILE` | Private network specs file path (required when `PRIVATE_NET`) | Container-internal path |

#### Proof Type

| Variable | Description | Values |
|----------|-------------|--------|
| 🌟 `BATCH_PROVE_REQ_TYPES` | Proof types to process | `ALL` (default), `TEE_ONLY`, `ZK_ONLY` |

### 2.4 Relayer Environment Variables (Optional)

#### Monitoring & Tracing

| Variable | Description | Default |
|----------|-------------|---------|
| `OTEL_SERVICE_NAME` | OpenTelemetry service name | `l2-relayer-${CONTAINER_INDEX:-0}` |
| `OTEL_EXPORTER_OTLP_TRACES_ENDPOINT` | OTLP Traces endpoint | Empty (disabled) |
| `ACC_BALANCE_MONITOR_SWITCH` | Enable account balance monitoring | `false` |
| `ACC_BALANCE_MONITOR_INTERVAL` | Balance check interval (ms) | `10000` |
| `ACC_BALANCE_MONITOR_BALANCE_THRESHOLD` | Balance alarm threshold (ETH) | `3.0` |
| `SELF_REPORT_URL` | Custom metrics reporting URL | Empty |
| `SELF_REPORT_SWITCH` | Custom metrics switch | `false` |

#### L1 Client Tuning

| Variable | Description | Default |
|----------|-------------|---------|
| `L1_CONSISTENCY_LEVEL` | L1 block consistency level | `FINALIZED` (also: `LATEST`, `SAFE`) |
| `L1_MAX_POLLING_BLOCK_SIZE` | Max blocks per L1 polling round | `32` |
| `L1_CLIENT_GAS_PRICE_POLICY` | Gas Price strategy | `FROM_API` (dynamic) or `STATIC` |
| `L1_CLIENT_STATIC_GAS_PRICE` | Static Gas Price in Wei | `4100000000` (4.1 Gwei) |
| `L1_CLIENT_GAS_LIMIT_POLICY` | Gas Limit strategy | `STATIC` or `ESTIMATE` |
| `L1_CLIENT_STATIC_GAS_LIMIT` | Static Gas Limit | `7200000` |
| `L1_CLIENT_EXTRA_GAS` | Extra gas buffer for ESTIMATE mode | `400000` |
| `L1_CLIENT_HTTP_CLIENT_PROTOCOLS` | HTTP protocol version | `HTTP_1_1` |
| `L1_CLIENT_HTTP_CLIENT_WRITE_TIMEOUT` | HTTP write timeout (seconds) | `60` |
| `L1_CLIENT_HTTP_CLIENT_READ_TIMEOUT` | HTTP read timeout (seconds) | `60` |
| `L1_BLOB_POOL_TX_TRAFFIC_LIMIT` | Max concurrent Blob Pool transactions | `16` (-1 = unlimited) |
| `L1_LEGACY_POOL_TX_TRAFFIC_LIMIT` | Max concurrent Legacy Pool transactions | `-1` (unlimited) |
| `ETH_NETWORK_FORK_UNKNOWN_NETWORK_CONFIG_FILE` | BPO config file for non-mainnet/sepolia networks | `null` |

#### L1 Transaction Speed-Up

| Variable | Description | Default |
|----------|-------------|---------|
| `L1_CLIENT_TX_SPEEDUP_BLOB_FEE_LIMIT` | Max Blob Fee for speed-up (Wei) | `1000000000000` (1000 Gwei) |
| `L1_CLIENT_TX_SPEEDUP_PRIORITY_FEE_LIMIT` | Max Priority Fee for speed-up (Wei) | `100000000000` (100 Gwei) |
| `L1_CLIENT_TX_SPEEDUP_BLOB_PRICE_BUMP` | Blob Price Bump multiplier | `1` |
| `L1_CLIENT_FORCE_TX_SPEEDUP_TIME_LIMIT` | Force speed-up wait time (ms) | `900000` (15 min) |

#### L2 Client

| Variable | Description | Default |
|----------|-------------|---------|
| `L2_COINBASE_PROXY_CONTRACT` | L2 Coinbase Proxy contract | `0x7100000000000000000000000000000000000000` |
| `L2_GAS_ORACLE_CONTRACT` | L2 Gas Oracle contract | `0x8100000000000000000000000000000000000000` |
| `L2_CLIENT_EXTRA_GAS` | L2 extra gas buffer | `500000` |
| `L2_CONSISTENCY_LEVEL` | L2 block consistency level | `FINALIZED` |

#### Rollup Core Parameters

| Variable | Description | Default |
|----------|-------------|---------|
| `ROLLUP_DA_TYPE` | Data Availability type | `BLOBS` (also: `DAS`) |
| `ROLLUP_DA_CONFIG_SERVICE_TYPE` | DA service type | `LOCAL` |
| `ROLLUP_CONFIG_PARENT_CHAIN_TYPE` | Parent Chain type | `ETHEREUM` (also: `JOVAY`) |
| `ROLLUP_CONFIG_GAS_PER_CHUNK_RECOMMENDED` | Recommended gas limit per Chunk | `23000000` (23M) |
| `ROLLUP_CONFIG_MAX_CHUNKS_MEMORY_USED` | Max memory for serialized chunks in growing Batch | `1073741824` (1GB) |

#### Economic Timing Strategy

| Variable | Description | Default |
|----------|-------------|---------|
| `ROLLUP_ECONOMIC_STRATEGY_CONF_SWITCH` | Enable economic timing strategy | `true` |
| `ROLLUP_ECONOMIC_STRATEGY_CONF_DEFAULT_MID_EIP1559_PRICE_LIMIT` | Yellow zone lower bound (Wei) | `3000000000` (3 Gwei) |
| `ROLLUP_ECONOMIC_STRATEGY_CONF_DEFAULT_HIGH_EIP1559_PRICE_LIMIT` | Red zone lower bound (Wei) | `8000000000` (8 Gwei) |
| `ROLLUP_ECONOMIC_STRATEGY_CONF_DEFAULT_MAX_PENDING_BATCH_COUNT` | Max pending Batch count threshold | `12` |
| `ROLLUP_ECONOMIC_STRATEGY_CONF_DEFAULT_MAX_PENDING_PROOF_COUNT` | Max pending Proof count threshold | `12` |
| `ROLLUP_ECONOMIC_STRATEGY_CONF_DEFAULT_MAX_BATCH_WAITING_TIME` | Max Batch waiting time (seconds) | `43200` (12 hours) |
| `ROLLUP_ECONOMIC_STRATEGY_CONF_DEFAULT_MAX_PROOF_WAITING_TIME` | Max Proof waiting time (seconds) | `43200` (12 hours) |

> The strategy classifies Gas Prices into Green/Yellow/Red zones: Green (< mid) submits immediately, Yellow (mid ~ high) submits based on pending count, Red (> high) pauses unless max waiting time is exceeded.

#### Alarm Configuration

| Variable | Description | Default |
|----------|-------------|---------|
| `ROLLUP_ALARM_SWITCH` | Alarm master switch | `true` |
| `ROLLUP_ALARM_BATCH_DELAYED_THRESHOLD` | Batch stall alarm (ms) | `10800000` (3 hours) |
| `ROLLUP_ALARM_CHUNK_DELAYED_THRESHOLD` | Chunk stall alarm (ms) | `3660000` (1h 1min) |
| `ROLLUP_ALARM_TEE_PROOF_DELAYED_THRESHOLD` | TEE Proof timeout alarm (ms) | `300000` (5 min) |
| `ROLLUP_ALARM_ZK_PROOF_DELAYED_THRESHOLD` | ZK Proof timeout alarm (ms) | `1800000` (30 min) |
| `ROLLUP_ALARM_L2_BLOCK_DELAYED_THRESHOLD` | L2 block update timeout alarm (ms) | `300000` (5 min) |
| `ROLLUP_ALARM_TX_OVER_PENDING_THRESHOLD` | Tx pending timeout alarm (ms) | `1800000` (30 min) |
| `ROLLUP_ALARM_MAX_GAP_BETWEEN_BATCH_AND_PROOF_COMMIT` | Max gap between Batch and Proof commit | `20` |
| `ROLLUP_ALARM_CIRCUIT_BREAKER_THRESHOLD` | Circuit breaker alarm (ms) | `18000000` (5 hours) |
| `ROLLUP_ALARM_MAX_BLOCK_GAP_BETWEEN_TRACER_AND_SEQ` | Max block gap between Tracer and Sequencer | `10` |

#### Cache Configuration

| Variable | Description | Default |
|----------|-------------|---------|
| `L2_BLOCK_TRACE_CACHE_FOR_CURR_CHUNK_CAPACITY` | Current Chunk Block Trace cache capacity | `1300` |
| `L2_BLOCK_TRACE_CACHE_TTL` | Block Trace cache TTL (ms) | `4200000` (70 min) |
| `L2_CHUNK_CACHE_TTL` | Chunk cache TTL (ms) | `4200000` (70 min) |
| `L2_BATCH_ETH_BLOBS_CACHE_TTL` | Batch Blobs cache TTL (ms) | `300000` (5 min) |
| `L2_BLOCK_POLLING_GROWING_BATCH_CHUNKS_MEM_CACHE_ASYNC_CORE_SIZE` | Async cache fill thread count | `4` |

#### Task Configuration

| Variable | Description | Default |
|----------|-------------|---------|
| `BATCH_COMMIT_WINDOWS_LENGTH` | Batch concurrent commit window size | `12` |
| `L2_BLOCK_POLLING_GET_BLOCK_TIMEOUT` | Block Trace request timeout (seconds) | `10` |
| `PROOF_COMMIT_ROLLUP_QUERY_LEVEL` | Rollup query level for Proof commit | `LATEST` |
| `PROOF_COMMIT_ROLLUP_QUERY_HEIGHT_BACKOFF` | Rollup query height backoff | `0` |
| `RELIABLE_TX_PARENT_CHAIN_TX_MISSED_TOLERANT_TIME` | Parent Chain tx miss tolerance (seconds) | `5` |
| `RELIABLE_TX_SUBCHAIN_TX_MISSED_TOLERANT_TIME` | Subchain tx miss tolerance (seconds) | `5` |
| `RELIABLE_TX_TX_TIMEOUT_LIMIT` | Transaction timeout (seconds) | `600` (10 min) |
| `RELIABLE_TX_PROCESS_BATCH_SIZE` | Transactions per processing batch | `10` |
| `RELIABLE_TX_RETRY_LIMIT` | Max retries for failed transactions | `1` |
| `ORACLE_GAS_FEED_ORACLE_REQ_NUMBER_PER_ROUND_LIMIT` | Max Oracle requests per round | `10` |
| `ORACLE_GAS_FEED_ORACLE_BASE_FEE_UPDATE_THRESHOLD` | Base Fee update threshold | `0` |
| `ORACLE_GAS_FEED_ORACLE_BASE_PROOF` | Base Proof type for Oracle | `TEE_PROOF` |

#### Tracer Retry Configuration

| Variable | Description | Default |
|----------|-------------|---------|
| `TRACER_CLIENT_REQ_RETRY_MAX_ATTEMPTS` | Max retry attempts | `10` |
| `TRACER_CLIENT_REQ_RETRY_BACKOFF_DELAY` | Base retry interval (ms) | `5` |
| `TRACER_CLIENT_REQ_RETRY_BACKOFF_MULTIPLIER` | Backoff multiplier (exponential) | `10` |
| `TRACER_CLIENT_REQ_RETRY_BACKOFF_MAX_DELAY` | Max retry interval (ms) | `500` |

#### Engine Configuration

| Variable | Description | Default |
|----------|-------------|---------|
| `DUTY_PERIOD` | Duty scheduling period (ms) | `100` |
| `ENGINE_SHUTDOWN_AWAIT_TERMINATE_TIME` | Shutdown thread wait time (ms) | `60000` (1 min) |
| `ENGINE_SHUTDOWN_BATCH_COMMIT_AWAIT_TERMINATE_TIME` | Batch Commit shutdown wait (ms) | `-1` (unlimited) |
| `ENGINE_SHUTDOWN_PROOF_COMMIT_AWAIT_TERMINATE_TIME` | Proof Commit shutdown wait (ms) | `-1` |
| `ENGINE_SHUTDOWN_RELIABLE_TX_AWAIT_TERMINATE_TIME` | Reliable TX shutdown wait (ms) | `-1` |

#### Ports & Debug

| Variable | Description | Default |
|----------|-------------|---------|
| `ADMIN_EXPOSE_PORT` | Admin CLI mapped port | `7088` |
| `DEBUG_PORT` | JVM Debug port | `25005` |
| `DEBUG_MODE` | JVM Debug switch | `off` (set `on` to enable) |
| `JASYPT_PASSWD` | Jasypt encryption password | Empty |
| `CONTAINER_INDEX` | Container instance index (multi-instance) | `0` |

### 2.5 Query Service Environment Variables

Query Service only requires database connection information:

| Variable | Description | Required |
|----------|-------------|----------|
| 🌟 `MYSQL_HOST` | MySQL host | Yes |
| 🌟 `MYSQL_PORT` | MySQL port | Yes |
| 🌟 `MYSQL_USER_NAME` | MySQL username | Yes |
| 🌟 `MYSQL_USER_PASSWORD` | MySQL password | Yes |
| `MYSQL_DB_NAME` | Database name | Default: `l2relayer` |
| `JASYPT_PASSWD` | Jasypt password | No |
| `DEBUG_MODE` | Debug switch | Default: `off` |
| `QS_EXPOSE_PORT` | Query Service mapped port | Default: `8080` |

### 2.6 MySQL Service Configuration

```yaml
l2-relayer-mysql:
  environment:
    - MYSQL_ROOT_PASSWORD=YOUR_PWD        # MySQL root password
    - TZ=Asia/Shanghai                     # Timezone
  command: >-
    --max_allowed_packet=32505856          # Max packet size (~31MB)
    --default-time_zone=+8:00             # Default timezone UTC+8
    --disable-log-bin                      # Disable binlog (standalone mode)
  mem_limit: 1024m                         # Memory limit — adjust for production
  volumes:
    - ./mysql/data:/var/lib/mysql           # Data persistence
    - ./mysql/conf:/etc/mysql/conf.d        # Custom MySQL configuration
```

> ⚠️ The default `mem_limit` is 1024M. For production environments, increase or remove this limit based on data volume.

### 2.7 Redis Service Configuration

```yaml
l2-relayer-redis:
  command: >-
    --requirepass 'YOUR_PWD'               # Redis password
    --maxmemory 1024MB                     # Max memory
    --maxmemory-policy volatile-lru        # Eviction policy
```

### 2.8 Using env_file for Variable Management

For multi-instance deployments or when managing many variables, extract environment variables into a separate `.env` file:

**compose.yaml example:**

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

**relayer.env example:**

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

> ⚠️ If using Jasypt encryption, replace password values with `ENC(ciphertext)` and set `JASYPT_PASSWD` to the corresponding key.

---

## 3. Relayer Process Configuration

The Relayer process configuration is located at `relayer-app/src/main/resources/application-prod.yml`. All settings support environment variable overrides using the `${ENV_VAR:default_value}` syntax.

### 3.1 Spring Boot Basics

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

The database uses MySQL with `rewriteBatchedStatements` enabled for batch insert optimization and `createDatabaseIfNotExist` for automatic database creation. Schema migrations are managed automatically via Flyway.

### 3.2 Rollup Core Configuration

```yaml
l2-relayer:
  rollup:
    da-type: ${ROLLUP_DA_TYPE:BLOBS}
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

- **`da-type`**: Data Availability type. Use `BLOBS` (EIP-4844) when Ethereum is the Parent Chain; use `DAS` when another Jovay instance is the Parent Chain (L3 scenario).
- **`parent-chain-type`**: `ETHEREUM` or `JOVAY`.
- **`gas-per-chunk-recommended`**: Gas threshold for Chunk splitting, affecting ZK Prover proof efficiency.
- **`max-chunks-memory-used`**: Memory cap for serialized Chunks in a growing Batch, preventing OOM.

### 3.3 Economic Timing Strategy

The economic timing strategy dynamically decides whether to submit transactions based on current L1 Gas Prices, reducing L1 costs:

```
Gas Price Zones:
  ├── Green [0, mid)     → Submit immediately
  ├── Yellow [mid, high) → Submit based on pending count and wait time
  └── Red [high, ∞)      → Pause unless max waiting time is exceeded
```

| Setting | Description |
|---------|-------------|
| `switch` | Master switch, enabled by default |
| `default-mid-eip1559-price-limit` | Yellow zone lower bound (Wei), default 3 Gwei |
| `default-high-eip1559-price-limit` | Red zone lower bound (Wei), default 8 Gwei |
| `default-max-pending-batch-count` | Pending Batch count threshold for Yellow zone submission |
| `default-max-pending-proof-count` | Pending Proof count threshold for Yellow zone submission |
| `default-max-batch-waiting-time` | Timeout to force Batch submission (seconds) |
| `default-max-proof-waiting-time` | Timeout to force Proof submission (seconds) |

### 3.4 L1 Client Configuration

#### Connection & Contracts

```yaml
l2-relayer:
  l1-client:
    rpc-url: ${L1_RPC_URL}
    rollup-contract: ${L1_ROLLUP_CONTRACT}
    mailbox-contract: ${L1_MAILBOX_CONTRACT}
    nonce-policy: FAST
```

#### Gas Strategy

| Setting | Env Variable | Description | Default |
|---------|-------------|-------------|---------|
| `gas-price-policy` | `L1_CLIENT_GAS_PRICE_POLICY` | Gas Price retrieval strategy | `FROM_API` |
| `static-gas-price` | `L1_CLIENT_STATIC_GAS_PRICE` | Fixed Gas Price for STATIC mode (Wei) | `4100000000` |
| `gas-limit-policy` | `L1_CLIENT_GAS_LIMIT_POLICY` | Gas Limit retrieval strategy | `STATIC` |
| `static-gas-limit` | `L1_CLIENT_STATIC_GAS_LIMIT` | Fixed Gas Limit for STATIC mode | `7200000` |
| `extra-gas` | `L1_CLIENT_EXTRA_GAS` | Extra gas buffer for ESTIMATE mode | `400000` |

#### Transaction Traffic Control

| Setting | Description | Default |
|---------|-------------|---------|
| `blob-pool-tx-traffic-limit` | Max unconfirmed Blob Pool transactions | `16` |
| `legacy-pool-tx-traffic-limit` | Max unconfirmed Legacy Pool transactions | `-1` (unlimited) |

#### HTTP Client

| Setting | Description | Default |
|---------|-------------|---------|
| `protocols` | HTTP protocol version | `HTTP_1_1` |
| `write-timeout` | Write timeout (seconds) | `60` |
| `read-timeout` | Read timeout (seconds) | `60` |

### 3.5 L2 Client Configuration

```yaml
l2-relayer:
  l2-client:
    rpc-url: ${L2_RPC_URL}
    coinbase-contract: ${L2_COINBASE_PROXY_CONTRACT:0x7100000000000000000000000000000000000000}
    gas-oracle-contract: ${L2_GAS_ORACLE_CONTRACT:0x8100000000000000000000000000000000000000}
    extra-gas: ${L2_CLIENT_EXTRA_GAS:500000}
```

### 3.6 Tracer Client Configuration

The Tracer client uses an exponential backoff retry strategy:

```yaml
l2-relayer:
  tracer-client:
    req-retry:
      max-attempts: 10
      backoff-delay: 5
      backoff-multiplier: 10
      backoff-max-delay: 500
```

Retry sequence example: 5ms → 50ms → 500ms → 500ms → ...

### 3.7 Task Configuration

Relayer's distributed task engine schedules these core tasks:

| Task | Config Prefix | Description |
|------|--------------|-------------|
| BlockPolling | `tasks.block-polling` | L1/L2 block polling |
| BatchCommit | `tasks.batch-commit` | Batch submission to L1 |
| BatchProve | `tasks.batch-prove` | Proof request to PC |
| ProofCommit | `tasks.proof-commit` | Proof submission to L1 |
| ReliableTx | `tasks.reliable-tx` | Reliable transaction confirmation |
| OracleGasFeed | `tasks.oracle-gas-feed` | L1 Gas price sync to L2 |

### 3.8 Alarm Configuration

The alarm system monitors critical Rollup metrics and logs alerts when thresholds are exceeded:

```yaml
l2-relayer:
  alarm:
    rollup:
      switch: true
      batch-delayed-threshold: 10800000      # 3 hours
      chunk-delayed-threshold: 3660000       # 1 hour 1 minute
      tee-proof-delayed-threshold: 300000    # 5 minutes
      zk-proof-delayed-threshold: 1800000    # 30 minutes
      l2-block-delayed-threshold: 300000     # 5 minutes
      tx-over-pending-threshold: 1800000     # 30 minutes
      circuit-breaker-threshold: 18000000    # 5 hours
```

### 3.9 Metrics & Monitoring

```yaml
l2-relayer:
  metrics:
    self-report:
      url: ${SELF_REPORT_URL:}
      switch: false
    blockchain:
      acc-balance-monitor:
        switch: false
        l1-interval: 10000
        l1-balance-threshold: 3.0
```

### 3.10 Engine Configuration

```yaml
l2-relayer:
  engine:
    shutdown:
      await-terminate-time: 60000
      batch-commit:
        await-terminate-time: -1
      proof-commit:
        await-terminate-time: -1
      reliable-tx:
        await-terminate-time: -1
    schedule:
      duty:
        period: 100
```

> Relayer uses `stop_grace_period: 5m` in Docker Compose, providing ample time for graceful shutdown so that in-flight L1 transactions can reach finality.

### 3.11 gRPC Client Configuration

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

### 3.12 Signing Service Configuration

Relayer supports two signing methods:

| Method | Use Case | Description |
|--------|----------|-------------|
| `web3j_native` | Dev / Test | Direct signing with plaintext private key |
| `kms` | Production | Signing via Alibaba Cloud KMS — more secure |

Configuration structure:

```yaml
jovay:
  sign-service:
    l1LegacyPoolTxSignService:    # L1 non-Blob tx (e.g., Proof submission)
      type: web3j_native | kms
      web3j-native:
        private-key: ...
      kms:
        endpoint: ...
        access-key-id: ...
        # ...

    l1BlobPoolTxSignService:      # L1 Blob tx (Batch submission)
      type: web3j_native | kms
      # same structure as above

    l2TxSignService:              # L2 transactions
      type: web3j_native | kms
      # same structure as above
```

### 3.13 Jasypt Encryption

```yaml
jasypt:
  encryptor:
    password: ${JASYPT_PASSWD:}
```

Jasypt is used to encrypt sensitive values in configuration. Encrypted values use the format `ENC(ciphertext)` and are decrypted at runtime using the `JASYPT_PASSWD` key.

---

## 4. Dynamic Configuration (Admin CLI)

Relayer supports runtime modification of certain configuration parameters via the [Admin CLI](../admin-cli/README.md) without restarting the service.

### 4.1 Launching the CLI

**Option 1: Interactive CLI inside container**

```bash
docker exec -it l2-relayer-0 /l2-relayer/bin/relayer-cli/bin/start.sh
```

**Option 2: Direct command execution (non-interactive)**

```bash
docker exec -it l2-relayer-0 java -jar bin/relayer-cli/lib/relayer-cli.jar <command> [options]
```

For the full list of Admin CLI commands and usage, see the [Admin CLI README](../admin-cli/README.md).

### 4.2 Gas Price Tuning

Runtime-configurable gas parameters:

| Query Command | Update Command | Description |
|---------------|---------------|-------------|
| `get-eth-gas-price-increased-percentage` | `update-eth-gas-price-increased-percentage --value <Double>` | Gas Price increase percentage |
| `get-eth-max-price-limit` | `update-eth-max-price-limit --value <String>` | Max Gas Price limit (Wei) |
| `get-eth-priority-fee-per-gas-increased-percentage` | `update-eth-priority-fee-per-gas-increased-percentage --value <Double>` | Priority Fee increase percentage |
| `get-eth-minimum-eip1559priority-price` | `update-eth-minimum-eip1559priority-price --value <String>` | Min EIP-1559 Priority Price (Wei) |
| `get-eth-base-fee-multiplier` | `update-eth-base-fee-multiplier --value <String>` | Base Fee multiplier |
| `get-eth-eip4844priority-fee-per-gas-increased-percentage` | `update-eth-eip4844priority-fee-per-gas-increased-percentage --value <Double>` | EIP-4844 Priority Fee increase % |
| `get-eth-minimum-eip4844priority-price` | `update-eth-minimum-eip4844priority-price --value <String>` | Min EIP-4844 Priority Price (Wei) |
| `get-eth-extra-gas-price` | `update-eth-extra-gas-price --value <String>` | Extra Gas Price (Wei) |
| `get-eth-larger-fee-per-blob-gas-multiplier` | `update-eth-larger-fee-per-blob-gas-multiplier --value <String>` | Blob Gas large multiplier |
| `get-eth-smaller-fee-per-blob-gas-multiplier` | `update-eth-smaller-fee-per-blob-gas-multiplier --value <String>` | Blob Gas small multiplier |
| `get-eth-fee-per-blob-gas-dividing-val` | `update-eth-fee-per-blob-gas-dividing-val --value <String>` | Blob Gas dividing value (Wei) |

### 4.3 Economic Strategy Tuning

| Query Command | Update Command | Description |
|---------------|---------------|-------------|
| `get-max-pending-batch-count` | `update-max-pending-batch-count --value <Integer>` | Max pending Batch count |
| `get-max-pending-proof-count` | `update-max-pending-proof-count --value <Integer>` | Max pending Proof count |
| `get-max-batch-waiting-time` | `update-max-batch-waiting-time --value <Long>` | Max Batch waiting time (seconds) |
| `get-max-proof-waiting-time` | `update-max-proof-waiting-time --value <Long>` | Max Proof waiting time (seconds) |
| `get-mid-eip1559price-limit` | `update-mid-eip1559price-limit --value <String>` | Yellow zone lower bound (Wei) |
| `get-high-eip1559price-limit` | `update-high-eip1559price-limit --value <String>` | Red zone lower bound (Wei) |

**Example: Adjusting the timing strategy during a gas spike**

```bash
# Check current thresholds
docker exec -it l2-relayer-0 java -jar bin/relayer-cli/lib/relayer-cli.jar get-mid-eip1559price-limit
docker exec -it l2-relayer-0 java -jar bin/relayer-cli/lib/relayer-cli.jar get-high-eip1559price-limit

# Relax thresholds: Yellow to 5 Gwei, Red to 15 Gwei
docker exec -it l2-relayer-0 java -jar bin/relayer-cli/lib/relayer-cli.jar update-mid-eip1559price-limit --value 5000000000
docker exec -it l2-relayer-0 java -jar bin/relayer-cli/lib/relayer-cli.jar update-high-eip1559price-limit --value 15000000000

# Extend max Batch wait to 24 hours
docker exec -it l2-relayer-0 java -jar bin/relayer-cli/lib/relayer-cli.jar update-max-batch-waiting-time --value 86400
```

---

## 5. Configuration Changelog

### v0.7.0 (Base)

Initial release with all baseline configuration items. See Sections 2.3 and 2.4.

### v0.8.0

**Added:**
- Signing service upgrade: `L1_LEGACY_POOL_TX_SIGN_SERVICE_TYPE`, `L1_BLOB_POOL_TX_SIGN_SERVICE_TYPE`, `L2_TX_SIGN_SERVICE_TYPE` and corresponding KMS configs
- Rollup Specs: `ROLLUP_SPECS_NETWORK`, `ROLLUP_SPECS_PRIVATE_NET_FILE`
- Oracle Gas Feed: `ORACLE_GAS_FEED_*` series
- L1 tx fee refinement: `L1_CLIENT_EIP4844_PRIORITY_FEE_PER_GAS_INCREASED_PERCENTAGE`, `L1_CLIENT_MINIMUM_EIP1559_PRIORITY_PRICE`, `L1_CLIENT_MINIMUM_EIP4844_PRIORITY_PRICE`
- L2 contracts: `L2_COINBASE_PROXY_CONTRACT`, `L2_GAS_ORACLE_CONTRACT`

**Removed:**
- `PROOF_COMMIT_WINDOWS_LENGTH`

### v0.8.1

**Added:**
- `L1_CLIENT_BLOB_SIDECAR_VERSION`: Blob version (sepolia = 1, Ethereum mainnet default = 0)

### v0.9.0

**Added:**
- Full economic timing configs: `ROLLUP_ECONOMIC_STRATEGY_CONF_*` series
- Cache TTL configs: `L2_BLOCK_TRACE_CACHE_TTL`, `L2_CHUNK_CACHE_TTL`, `L2_BATCH_ETH_BLOBS_CACHE_TTL`
- Async cache threads: `L2_BLOCK_POLLING_GROWING_BATCH_CHUNKS_MEM_CACHE_ASYNC_CORE_SIZE`
- Circuit breaker alarm: `ROLLUP_ALARM_CIRCUIT_BREAKER_THRESHOLD`
- Oracle base proof: `ORACLE_GAS_FEED_ORACLE_BASE_PROOF`
- Non-standard network BPO: `ETH_NETWORK_FORK_UNKNOWN_NETWORK_CONFIG_FILE`
- Tx timeout: `RELIABLE_TX_TX_TIMEOUT_LIMIT`
- Dynamic configuration via [Admin CLI](../admin-cli/README.md)

**Removed:**
- `L1_CLIENT_BLOB_SIDECAR_VERSION`

### v0.10.0

**Added:**
- `L2_BLOCK_TRACE_CACHE_FOR_CURR_CHUNK_CAPACITY`: Current Chunk Block Trace cache capacity
- `ROLLUP_CONFIG_GAS_PER_CHUNK_RECOMMENDED`: Chunk gas recommendation
- `ROLLUP_ALARM_TEE_PROOF_DELAYED_THRESHOLD`: TEE Proof alarm (split from generic threshold)
- `ROLLUP_ALARM_ZK_PROOF_DELAYED_THRESHOLD`: ZK Proof alarm

**Removed:**
- `ROLLUP_CONFIG_ZK_VERIFICATION_START_BATCH`
- `TRACER_CLIENT_MAX_BLOCK_STABLE_GAP`
- `ROLLUP_ALARM_PROOF_DELAYED_THRESHOLD` (split into TEE/ZK individual thresholds)

### v0.11.0

**Added:**
- `ROLLUP_CONFIG_MAX_CHUNKS_MEMORY_USED`: Chunks memory cap
- Graceful shutdown configs: `ENGINE_SHUTDOWN_*` series
- HTTP timeouts: `L1_CLIENT_HTTP_CLIENT_WRITE_TIMEOUT`, `L1_CLIENT_HTTP_CLIENT_READ_TIMEOUT`

**Changed:**
- `ROLLUP_ALARM_CHUNK_DELAYED_THRESHOLD`: Default changed to 3660000ms (1 hour 1 minute)

### v0.12.0

**Added:**
- `ROLLUP_CONFIG_PARENT_CHAIN_TYPE`: Parent Chain type (`ETHEREUM` / `JOVAY`)
- `ROLLUP_DA_TYPE`: DA type (`BLOBS` / `DAS`)
- `RELIABLE_TX_PARENT_CHAIN_TX_MISSED_TOLERANT_TIME`: Parent Chain tx miss tolerance
- `RELIABLE_TX_SUBCHAIN_TX_MISSED_TOLERANT_TIME`: Subchain tx miss tolerance

---

## 6. Appendix

### 6.1 Rollup Specs Examples

Rollup Specs define protocol fork rules and Batch version numbers, triggered by timestamps.

**mainnet:**

```json
{
  "network": "mainnet",
  "layer2_chain_id": "5734951",
  "layer1_chain_id": "1",
  "forks": {
    "1": { "batch_version": 1 },
    "4919284474000": { "batch_version": 2 }
  }
}
```

**testnet:**

```json
{
  "network": "testnet",
  "layer2_chain_id": "2019775",
  "layer1_chain_id": "11155111",
  "forks": {
    "1": { "batch_version": 0 },
    "1758327170000": { "batch_version": 1 },
    "4919284474000": { "batch_version": 2 }
  }
}
```

**private_net:**

- Set `network` to `private_net`
- Set `layer2_chain_id` to your Jovay Chain ID
- Set `layer1_chain_id` to your L1 Chain ID
- Mount the config file into the container and set `ROLLUP_SPECS_PRIVATE_NET_FILE`:

```yaml
volumes:
  - ./private-specs.json:/l2-relayer/config/private-specs.json
environment:
  - ROLLUP_SPECS_PRIVATE_NET_FILE=/l2-relayer/config/private-specs.json
```

### 6.2 BPO File Examples

BPO (Blob Price Oracle) configuration defines Ethereum hard fork parameters. Non-mainnet/sepolia networks require this via `ETH_NETWORK_FORK_UNKNOWN_NETWORK_CONFIG_FILE`:

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

Mount:

```yaml
volumes:
  - ./unknown-bpo.json:/l2-relayer/config/unknown-bpo.json
environment:
  - ETH_NETWORK_FORK_UNKNOWN_NETWORK_CONFIG_FILE=/l2-relayer/config/unknown-bpo.json
```

### 6.3 Important Notes

1. **Private Key Security**: L1 Blob Pool and L1 Legacy Pool private keys **must be different** and must not be shared with other applications. Ensure the corresponding accounts have sufficient L1 ETH.
2. **Memory Limits**: The default MySQL `mem_limit` in compose is 1024M. Adjust or remove for production.
3. **Jasypt Encryption**: For production, encrypt sensitive config values using `ENC(ciphertext)` format with `JASYPT_PASSWD`.
4. **Graceful Shutdown**: Relayer's `stop_grace_period` is 5 minutes. `ENGINE_SHUTDOWN_*_AWAIT_TERMINATE_TIME` defaults to -1 (unlimited wait), ensuring in-flight L1 transactions can reach finality before shutdown.
5. **Multi-Instance Deployment**: Use `CONTAINER_INDEX` to differentiate instances and `env_file` to share configuration. Multiple instances share the same MySQL and Redis, coordinated via Redis distributed locks.
6. **Configuration Activation**: Static config changes require a Relayer container restart (`docker compose up -d l2-relayer`). Dynamic config changes via [Admin CLI](../admin-cli/README.md) take effect immediately.
