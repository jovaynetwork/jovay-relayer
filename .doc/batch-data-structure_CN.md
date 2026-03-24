# Batch 数据结构参考文档

> [English Version](batch-data-structure.md)

## 概述

在 Jovay L2 Rollup 系统中，L2 交易被组织成层级数据结构提交到 L1。整体结构如下：

```
Batch
├── BatchHeader          (105 字节，固定大小元数据)
├── Payload (Chunks)     (可变大小交易数据)
│   ├── Chunk 0
│   │   ├── BlockContext 0   (每个区块 40 字节)
│   │   ├── BlockContext 1
│   │   ├── ...
│   │   └── L2 Transactions  (原始交易字节)
│   ├── Chunk 1
│   │   └── ...
│   └── ...
└── DA Data              (EIP-4844 Blob 或 DA Service 证明)
```

**Batch** 是提交到 L1 的顶级单元。每个 Batch 包含一个或多个 **Chunk**，每个 Chunk 包含一个或多个 **Block** 及其 L2 交易数据。

---

## BatchHeader

`BatchHeader` 是一个固定大小的 105 字节结构，唯一标识一个 Batch，并通过哈希链将其与父 Batch 连接。

### 字段布局

| 偏移量 | 大小（字节） | 字段 | 类型 | 说明 |
|--------|-------------|------|------|------|
| 0 | 1 | `version` | uint8 | Batch 格式版本（0、1 或 2） |
| 1 | 8 | `batchIndex` | uint64 | Batch 顺序索引（从 0 开始） |
| 9 | 32 | `l1MsgRollingHash` | bytes32 | 截至本 Batch 所有已包含 L1 消息的滚动哈希 |
| 41 | 32 | `dataHash` | bytes32 | Batch 有效载荷数据的哈希 |
| 73 | 32 | `parentBatchHash` | bytes32 | 父 BatchHeader 的 Keccak256 哈希 |

**总计：105 字节**（1 + 8 + 32 + 32 + 32）

### 哈希计算

`batchHeaderHash` 是序列化 BatchHeader（上述 105 字节）的 **Keccak256** 哈希。此哈希存储在链上的 Rollup 合约中，形成连接所有 Batch 的哈希链。

### 字段详细说明

- **version**：决定有效载荷（Chunks）的序列化方式和 DA 数据的编码方式。详见[Batch 版本](#batch-版本)。
- **batchIndex**：从 0 开始的单调递增索引。Batch 必须按顺序提交到 L1。
- **l1MsgRollingHash**：截至并包括本 Batch 在内，所有已包含的 L1 到 L2 消息（存款）的累积哈希。用于 L1 消息完整性验证。
- **dataHash**：序列化 Chunk 数据的 Keccak256 哈希。在 EIP-4844 模式下，这是所有 Blob Commitment 的 KZG Versioned Hash 拼接后的哈希。
- **parentBatchHash**：前一个 Batch Header 的哈希，形成哈希链。创世 Batch（索引 0）使用预定义的锚点哈希。

---

## Chunk

Chunk 是连续 L2 区块及其交易的分组。每个 Batch 包含一个或多个 Chunk。

### 字段布局

| 字段 | 大小 | 类型 | 说明 |
|------|------|------|------|
| `numBlocks` | 1 字节 (V0/V1) 或 4 字节 (V2) | uint8 / uint32 | 本 Chunk 中的区块数量 |
| `blockContexts` | N × 40 字节 | BlockContext[] | 每个区块的元数据 |
| `l2Transactions` | 可变 | bytes | 拼接的原始 L2 交易数据 |

### 序列化格式

```
┌─────────────────────────────────────────────────────┐
│ numBlocks（V0/V1: 1 字节，V2: 4 字节）              │
├─────────────────────────────────────────────────────┤
│ BlockContext[0]  (40 字节)                          │
│ BlockContext[1]  (40 字节)                          │
│ ...                                                 │
│ BlockContext[N-1] (40 字节)                         │
├─────────────────────────────────────────────────────┤
│ L2 Transactions（可变长度）                          │
└─────────────────────────────────────────────────────┘
```

### 约束条件

| Batch 版本 | 每 Chunk 最大区块数 | numBlocks 字段大小 |
|-----------|--------------------|--------------------|
| BATCH_V0 | 255 | 1 字节（uint8） |
| BATCH_V1 | 255 | 1 字节（uint8） |
| BATCH_V2 | 2³² - 1 | 4 字节（uint32） |

---

## BlockContext

Chunk 中的每个区块都有一个固定大小的 40 字节 `BlockContext`，记录区块的关键元数据。

### 字段布局

| 偏移量 | 大小（字节） | 字段 | 类型 | 说明 |
|--------|-------------|------|------|------|
| 0 | 4 | `specVersion` | uint32 | Jovay Sequencer 区块链协议 Spec 版本 |
| 4 | 8 | `blockNumber` | uint64 | L2 区块号 |
| 12 | 8 | `timestamp` | int64 | Unix 时间戳（秒） |
| 20 | 8 | `baseFee` | uint64 | EIP-1559 基础费（单位：Wei） |
| 28 | 8 | `gasLimit` | uint64 | 区块 Gas 上限 |
| 36 | 2 | `numTransactions` | uint16 | 本区块中 L2 交易数量 |
| 38 | 2 | `numL1Messages` | uint16 | 本区块中包含的 L1 消息（存款）数量 |

**总计：40 字节**（`BLOCK_CONTEXT_SIZE = 40`）

### 字段详细说明

- **specVersion**：标识 Jovay Sequencer 区块链协议的 Spec 版本号。不同的 Spec 版本可能改变协议参数（如区块 Gas 上限、费用模型），使系统能够处理兼容性升级。
- **blockNumber**：L2 链的区块号。Chunk 内的区块是连续的。
- **timestamp**：区块生产时间戳。
- **baseFee**：本区块的 EIP-1559 基础费，用于 Gas 价格计算。
- **gasLimit**：本区块的 Gas 上限。
- **numTransactions**：本区块中的 L2 用户交易数量（不包括 L1 消息）。
- **numL1Messages**：本区块中执行的 L1 到 L2 消息（存款）数量。

---

## Batch 版本

Relayer 支持三个 Batch 版本，每个版本在编码和压缩方面引入改进。

### 版本对比

| 特性 | BATCH_V0 | BATCH_V1 | BATCH_V2 |
|------|----------|----------|----------|
| 版本字节 | 0 | 1 | 2 |
| Chunk 编解码器 | V0 编解码器 | V0 编解码器 | V2 编解码器 |
| 压缩 | 无 | ZSTD | ZSTD |
| 每 Chunk 最大区块数 | 255 | 255 | 2³² - 1 |
| numBlocks 字段 | 1 字节（uint8） | 1 字节（uint8） | 4 字节（uint32） |
| 兼容的 DA 版本 | DA_0 | DA_1, DA_2 | DA_1, DA_2 |

### BATCH_V0

原始 Batch 格式。Chunk 在不压缩的情况下序列化，`numBlocks` 字段使用单字节（每 Chunk 最多 255 个区块）。DA 数据采用传统的 DA_0 格式编码（无元数据前缀的原始载荷）。

### BATCH_V1

引入 **ZSTD 压缩**。Chunk 编解码器与 V0 相同（1 字节 `numBlocks`），但序列化后的 Chunk 数据在打包到 Blob 前会用 Zstandard 进行压缩。使用 DA_1（未压缩）或 DA_2（压缩）编码，在 Blob 数据前添加 4 字节元数据前缀。

### BATCH_V2

扩展 Chunk 编解码器，支持 **4 字节 `numBlocks` 字段**（uint32），允许每 Chunk 最多包含 2³² - 1 个区块。这对高吞吐量场景至关重要。压缩仍使用 ZSTD，DA 编码使用 DA_1 或 DA_2。

---

## DA（数据可用性）编码

Batch 数据通过 EIP-4844 Blob 交易或 DA Service 提交到 L1。DA 编码决定了序列化的 Chunk 数据如何打包到 Blob 中。

### DA 版本

| DA 版本 | 值 | 元数据 | 压缩 | 兼容的 Batch 版本 |
|---------|------|--------|------|-------------------|
| DA_0 | 0 | 无 | 无 | 仅 BATCH_V0 |
| DA_1 | 1 | 4 字节头 | 无 | BATCH_V1, BATCH_V2 |
| DA_2 | 2 | 4 字节头 | ZSTD | BATCH_V1, BATCH_V2 |

### DA_0 格式（传统）

不包含任何元数据前缀的原始序列化 Chunk 数据。仅用于 BATCH_V0。

```
┌─────────────────────────────────┐
│ 原始 Chunks 载荷                │
└─────────────────────────────────┘
```

### DA_1 / DA_2 格式

包含 4 字节元数据头和载荷。

```
┌──────────────────┬──────────────┬─────────────────────────────┐
│ batch_version    │ n_bytes      │ payload                     │
│ (1 字节, uint8)  │ (3 字节,     │ (可变长度)                   │
│                  │  uint24)     │                             │
└──────────────────┴──────────────┴─────────────────────────────┘
```

| 字段 | 大小 | 说明 |
|------|------|------|
| `batch_version` | 1 字节 | Batch 版本（1 或 2） |
| `n_bytes` | 3 字节 | 载荷长度（uint24，最大约 16 MB） |
| `payload` | 可变 | 序列化的 Chunks（DA_1：未压缩，DA_2：ZSTD 压缩） |

### 压缩策略

为 BATCH_V1 或 BATCH_V2 创建 DA 数据时：
1. 将所有 Chunk 序列化为原始载荷
2. 尝试 ZSTD 压缩
3. 如果压缩后大小 < 未压缩大小，使用 **DA_2**（压缩）
4. 否则，使用 **DA_1**（未压缩）

---

## Chunks 载荷序列化

Batch 中的多个 Chunk 使用长度前缀格式序列化为连续的字节数组。

### 格式

```
┌────────────────────────┬─────────────────────┐
│ chunk_0_len (4 字节)   │ chunk_0_bytes       │
├────────────────────────┼─────────────────────┤
│ chunk_1_len (4 字节)   │ chunk_1_bytes       │
├────────────────────────┼─────────────────────┤
│ ...                    │ ...                 │
└────────────────────────┴─────────────────────┘
```

每个 Chunk 前面有一个 4 字节大端序长度字段，后面紧跟序列化的 Chunk 字节（按照 [Chunk](#chunk) 部分定义的格式）。

---

## EIP-4844 Blob 编码

通过 EIP-4844 提交到以太坊 L1 时，DA 数据（元数据 + 载荷）被打包到 Blob 中。

### Blob 结构

- 每个 Blob 由 **4096 个 Word**（每个 32 字节）组成 = **131,072 字节**（128 KB）
- 每个 32 字节 Word 仅使用 **31 字节存储数据**（第一个字节保留）
- 每个 Blob 的有效容量：**31 × 4096 = 126,976 字节**（约 124 KB）

### 编码流程

1. 第一个 Word 的第一个字节包含 **DA 版本**号
2. 数据被分割为 31 字节的段
3. 每个段被打包到一个 32 字节 Word 中（字节 0 设为 0，第一个 Word 中存放 DA 版本除外）
4. Word 被组合成 128 KB 的 Blob
5. 如果数据超过一个 Blob 的容量，则使用多个 Blob

### Blob 的数据哈希

使用 EIP-4844 时，BatchHeader 中的 `dataHash` 计算方式为：

```
dataHash = Keccak256(versionedHash_0 ++ versionedHash_1 ++ ... ++ versionedHash_N)
```

其中每个 `versionedHash` 是对应 Blob Commitment 的 KZG Versioned Hash。

---

## L1 提交模式

Batch 数据可以通过两种方式提交到 L1：

### 1. EIP-4844 Blob 交易（以太坊 L1）

当父链为以太坊时使用。

```
commitBatch(version, batchIndex, totalL1MessagePopped) + EIP-4844 Blobs
```

Batch Header 字段和 Chunk 数据被编码到交易附带的 Blob 中。

### 2. DA Service 模式（L3 / Jovay 父链）

当父链是另一条 Jovay 链（L3 架构）时使用。

```
commitBatchWithDaProof(serializedBatchHeader, totalL1MessagePopped, daProof)
```

Chunk 数据通过外部 DA Service 存储，链上仅提交 DA 证明。

---

## 数据库表结构

### `batches` 表

| 列名 | 类型 | 说明 |
|------|------|------|
| `id` | INT(11) PK | 自增主键 |
| `version` | INT(8) | Batch 版本（0、1 或 2） |
| `batch_header_hash` | VARCHAR(66) BINARY | BatchHeader 的 Keccak256 哈希（唯一） |
| `batch_index` | BIGINT UNSIGNED | Batch 顺序索引（唯一） |
| `l1_message_popped` | INT(11) | 本 Batch 包含的 L1 消息数量 |
| `total_l1_message_popped` | INT(11) | 截至本 Batch 的累计 L1 消息数量 |
| `l1msg_rolling_hash` | VARCHAR(66) BINARY | L1 消息滚动哈希 |
| `data_hash` | VARCHAR(66) BINARY | Batch 数据哈希 |
| `parent_batch_hash` | VARCHAR(66) BINARY | 父 Batch 哈希 |
| `post_state_root` | VARCHAR(66) | 执行后状态根 |
| `l2_msg_root` | VARCHAR(66) | L2 消息 Merkle 根 |
| `start_number` | VARCHAR(64) | 本 Batch 的起始区块号 |
| `end_number` | VARCHAR(64) | 本 Batch 的结束区块号 |
| `chunk_num` | INT(11) | 本 Batch 的 Chunk 数量 |
| `gmt_create` | DATETIME | 记录创建时间 |
| `gmt_modified` | DATETIME | 记录修改时间 |

### `chunks` 表

| 列名 | 类型 | 说明 |
|------|------|------|
| `id` | INT(11) PK | 自增主键 |
| `batch_version` | INT(8) | Batch 版本（历史数据为 -1） |
| `batch_index` | BIGINT UNSIGNED | 所属 Batch 索引 |
| `chunk_index` | INT(11) | Chunk 在 Batch 内的索引 |
| `chunk_hash` | VARCHAR(64) | Chunk 数据的哈希 |
| `num_blocks` | INT(11) | 本 Chunk 的区块数量 |
| `zk_cycle_sum` | BIGINT UNSIGNED | ZK 证明循环计数 |
| `gas_sum` | BIGINT | 累计 Gas 使用量（历史数据为 -1） |
| `start_number` | VARCHAR(64) | 本 Chunk 的起始区块号 |
| `end_number` | VARCHAR(64) | 本 Chunk 的结束区块号 |
| `raw_chunk` | LONGBLOB | 序列化的 Chunk 数据 |
| `gmt_create` | DATETIME | 记录创建时间 |
| `gmt_modified` | DATETIME | 记录修改时间 |

---

## 领域模型类

### BatchWrapper

`BatchWrapper` 类是 Batch 的主要领域模型，在核心 `Batch` 的基础上封装了额外的元数据。

| 字段 | 类型 | 说明 |
|------|------|------|
| `batch` | `Batch` | 核心 Batch（Header + Payload + DA Data） |
| `startBlockNumber` | `BigInteger` | 本 Batch 的起始 L2 区块号 |
| `endBlockNumber` | `BigInteger` | 本 Batch 的结束 L2 区块号 |
| `postStateRoot` | `byte[]` | 执行本 Batch 所有交易后的状态根 |
| `l2MsgRoot` | `byte[]` | L2 到 L1 消息的 Merkle 根 |
| `totalL1MessagePopped` | `long` | 已处理的 L1 消息累计数量 |
| `l1MessagePopped` | `long` | 本 Batch 中的 L1 消息数量 |
| `gmtCreate` | `long` | 创建时间戳 |

### ChunkWrapper

`ChunkWrapper` 类在 Chunk 基础上封装了 Batch 级别的上下文信息。

| 字段 | 类型 | 说明 |
|------|------|------|
| `batchVersion` | `BatchVersionEnum` | 所属 Batch 的版本 |
| `batchIndex` | `BigInteger` | Batch 索引 |
| `chunkIndex` | `long` | Chunk 在 Batch 内的索引 |
| `chunk` | `Chunk` | 核心 Chunk（区块 + 交易） |
| `gasSum` | `long` | 累计 Gas 使用量（历史数据为 -1） |

---

## 源代码文件参考

| 分类 | 文件路径 |
|------|----------|
| Batch 核心 | `relayer-commons/.../l2basic/Batch.java` |
| BatchHeader | `relayer-commons/.../l2basic/BatchHeader.java` |
| Batch 版本 | `relayer-commons/.../l2basic/BatchVersionEnum.java` |
| Chunk 核心 | `relayer-commons/.../l2basic/Chunk.java` |
| Block 上下文 | `relayer-commons/.../l2basic/BlockContext.java` |
| Chunks 载荷 | `relayer-commons/.../l2basic/ChunksPayload.java` |
| Chunk 编解码器 | `relayer-commons/.../l2basic/IChunkCodec.java` |
| DA 版本 | `relayer-commons/.../l2basic/DaVersion.java` |
| DA 压缩器 | `relayer-commons/.../l2basic/IDaCompressor.java` |
| Blob DA 数据 | `relayer-commons/.../l2basic/BlobsDaData.java` |
| DA 数据接口 | `relayer-commons/.../l2basic/IDaData.java` |
| BatchWrapper | `relayer-commons/.../models/BatchWrapper.java` |
| ChunkWrapper | `relayer-commons/.../models/ChunkWrapper.java` |
| 序列化工具 | `relayer-commons/.../utils/RollupUtils.java` |
| Batches 实体 | `relayer-dal/.../entities/BatchesEntity.java` |
| Chunks 实体 | `relayer-dal/.../entities/ChunksEntity.java` |
| 数据库迁移（初始化） | `relayer-app/.../db/migration/V0.0.1__init-db.sql` |
| 数据库迁移（滚动哈希） | `relayer-app/.../db/migration/V0.0.3__add-l1msg-rolling-hash.sql` |
| 数据库迁移（Batch V2） | `relayer-app/.../db/migration/V0.0.7__batch-v2-update.sql` |
| 数据库迁移（Gas 累计） | `relayer-app/.../db/migration/V0.0.8__add_gas_sum_for_chunk.sql` |
