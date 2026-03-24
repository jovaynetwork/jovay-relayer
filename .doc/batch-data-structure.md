# Batch Data Structure Reference

> [中文文档](batch-data-structure_CN.md)

## Overview

In the Jovay L2 Rollup system, L2 transactions are organized into a hierarchical data structure for submission to L1. The hierarchy is:

```
Batch
├── BatchHeader          (105 bytes, fixed-size metadata)
├── Payload (Chunks)     (variable-size transaction data)
│   ├── Chunk 0
│   │   ├── BlockContext 0   (40 bytes per block)
│   │   ├── BlockContext 1
│   │   ├── ...
│   │   └── L2 Transactions  (raw transaction bytes)
│   ├── Chunk 1
│   │   └── ...
│   └── ...
└── DA Data              (EIP-4844 Blob or DA Service proof)
```

A **Batch** is the top-level unit submitted to L1. Each Batch contains one or more **Chunks**, and each Chunk contains one or more **Blocks** along with their L2 transaction data.

---

## BatchHeader

The `BatchHeader` is a fixed-size 105-byte structure that uniquely identifies a Batch and links it to its parent via a hash chain.

### Field Layout

| Offset | Size (bytes) | Field | Type | Description |
|--------|-------------|-------|------|-------------|
| 0 | 1 | `version` | uint8 | Batch format version (0, 1, or 2) |
| 1 | 8 | `batchIndex` | uint64 | Sequential Batch index (0-based) |
| 9 | 32 | `l1MsgRollingHash` | bytes32 | Rolling hash of all L1 messages included up to this Batch |
| 41 | 32 | `dataHash` | bytes32 | Hash of the Batch payload data |
| 73 | 32 | `parentBatchHash` | bytes32 | Keccak256 hash of the parent BatchHeader |

**Total: 105 bytes** (1 + 8 + 32 + 32 + 32)

### Hash Calculation

The `batchHeaderHash` is the **Keccak256** hash of the serialized BatchHeader (the 105 bytes above). This hash is stored on-chain in the Rollup contract and forms a hash chain linking all Batches.

### Field Details

- **version**: Determines how the payload (Chunks) is serialized and how DA data is encoded. See [Batch Versions](#batch-versions) for details.
- **batchIndex**: Monotonically increasing index starting from 0. Batches must be submitted to L1 in sequential order.
- **l1MsgRollingHash**: A cumulative hash of all L1-to-L2 messages (deposits) included in Batches up to and including this one. Used for L1 message integrity verification.
- **dataHash**: Keccak256 hash of the serialized chunk data. For EIP-4844 mode, this is the hash of concatenated KZG versioned hashes of all blob commitments.
- **parentBatchHash**: Hash of the previous Batch's header, forming a hash chain. The genesis Batch (index 0) uses a predefined anchor hash.

---

## Chunk

A Chunk is a grouping of consecutive L2 blocks and their transactions. Each Batch contains one or more Chunks.

### Field Layout

| Field | Size | Type | Description |
|-------|------|------|-------------|
| `numBlocks` | 1 byte (V0/V1) or 4 bytes (V2) | uint8 / uint32 | Number of blocks in this Chunk |
| `blockContexts` | N × 40 bytes | BlockContext[] | Metadata for each block |
| `l2Transactions` | variable | bytes | Concatenated raw L2 transaction data |

### Serialization Format

```
┌─────────────────────────────────────────────────────┐
│ numBlocks (1 byte for V0/V1, 4 bytes for V2)       │
├─────────────────────────────────────────────────────┤
│ BlockContext[0]  (40 bytes)                         │
│ BlockContext[1]  (40 bytes)                         │
│ ...                                                 │
│ BlockContext[N-1] (40 bytes)                        │
├─────────────────────────────────────────────────────┤
│ L2 Transactions (variable length)                   │
└─────────────────────────────────────────────────────┘
```

### Constraints

| Batch Version | Max Blocks per Chunk | numBlocks Size |
|---------------|---------------------|----------------|
| BATCH_V0 | 255 | 1 byte (uint8) |
| BATCH_V1 | 255 | 1 byte (uint8) |
| BATCH_V2 | 2³² - 1 | 4 bytes (uint32) |

---

## BlockContext

Each block within a Chunk has a fixed-size 40-byte `BlockContext` that captures essential block metadata.

### Field Layout

| Offset | Size (bytes) | Field | Type | Description |
|--------|-------------|-------|------|-------------|
| 0 | 4 | `specVersion` | uint32 | Jovay Sequencer blockchain protocol spec version |
| 4 | 8 | `blockNumber` | uint64 | L2 block number |
| 12 | 8 | `timestamp` | int64 | Unix timestamp (seconds) |
| 20 | 8 | `baseFee` | uint64 | EIP-1559 base fee (in Wei) |
| 28 | 8 | `gasLimit` | uint64 | Block gas limit |
| 36 | 2 | `numTransactions` | uint16 | Number of L2 transactions in this block |
| 38 | 2 | `numL1Messages` | uint16 | Number of L1 messages (deposits) included in this block |

**Total: 40 bytes** per block (`BLOCK_CONTEXT_SIZE = 40`)

### Field Details

- **specVersion**: Identifies the Jovay Sequencer blockchain protocol spec version. Different spec versions may alter protocol parameters (e.g., block gas limit, fee model). Allows the system to handle compatible upgrades.
- **blockNumber**: The L2 chain block number. Blocks within a Chunk are consecutive.
- **timestamp**: Block production timestamp.
- **baseFee**: The EIP-1559 base fee for this block. Used for gas price calculations.
- **gasLimit**: The gas limit for this block.
- **numTransactions**: Count of L2 user transactions in this block (excludes L1 messages).
- **numL1Messages**: Count of L1-to-L2 messages (deposits) that were executed in this block.

---

## Batch Versions

The Relayer supports three Batch versions, each introducing improvements in encoding and compression.

### Version Comparison

| Feature | BATCH_V0 | BATCH_V1 | BATCH_V2 |
|---------|----------|----------|----------|
| Version Byte | 0 | 1 | 2 |
| Chunk Codec | V0 Codec | V0 Codec | V2 Codec |
| Compression | None | ZSTD | ZSTD |
| Max Blocks/Chunk | 255 | 255 | 2³² - 1 |
| numBlocks Field | 1 byte (uint8) | 1 byte (uint8) | 4 bytes (uint32) |
| Compatible DA Versions | DA_0 | DA_1, DA_2 | DA_1, DA_2 |

### BATCH_V0

The original Batch format. Chunks are serialized without compression, and the `numBlocks` field uses a single byte (max 255 blocks per Chunk). DA data is encoded in the legacy DA_0 format (raw payload without metadata prefix).

### BATCH_V1

Introduces **ZSTD compression** for the Chunk payload. The Chunk codec is unchanged from V0 (same 1-byte `numBlocks`), but the serialized chunks are compressed using Zstandard before being packed into blobs. Uses DA_1 (uncompressed) or DA_2 (compressed) encoding, which adds a 4-byte metadata prefix to the blob data.

### BATCH_V2

Extends the Chunk codec to support a **4-byte `numBlocks` field** (uint32), allowing up to 2³² - 1 blocks per Chunk. This is important for high-throughput scenarios. Compression remains ZSTD. DA encoding uses DA_1 or DA_2.

---

## DA (Data Availability) Encoding

Batch data is submitted to L1 via EIP-4844 blob transactions or through a DA Service. The DA encoding determines how the serialized Chunk data is packed into blobs.

### DA Versions

| DA Version | Value | Metadata | Compression | Compatible Batch Versions |
|------------|-------|----------|-------------|--------------------------|
| DA_0 | 0 | None | None | BATCH_V0 only |
| DA_1 | 1 | 4-byte header | None | BATCH_V1, BATCH_V2 |
| DA_2 | 2 | 4-byte header | ZSTD | BATCH_V1, BATCH_V2 |

### DA_0 Format (Legacy)

Raw serialized chunks without any metadata prefix. Used only with BATCH_V0.

```
┌─────────────────────────────────┐
│ Raw Chunks Payload              │
└─────────────────────────────────┘
```

### DA_1 / DA_2 Format

Includes a 4-byte metadata header followed by the payload.

```
┌──────────────────┬──────────────┬─────────────────────────────┐
│ batch_version    │ n_bytes      │ payload                     │
│ (1 byte, uint8)  │ (3 bytes,    │ (variable)                  │
│                  │  uint24)     │                             │
└──────────────────┴──────────────┴─────────────────────────────┘
```

| Field | Size | Description |
|-------|------|-------------|
| `batch_version` | 1 byte | Batch version (1 or 2) |
| `n_bytes` | 3 bytes | Length of the payload in bytes (uint24, max ~16 MB) |
| `payload` | variable | Serialized chunks (DA_1: uncompressed, DA_2: ZSTD-compressed) |

### Compression Strategy

When creating DA data for BATCH_V1 or BATCH_V2:
1. Serialize all chunks into the raw payload
2. Attempt ZSTD compression
3. If the compressed size < uncompressed size, use **DA_2** (compressed)
4. Otherwise, use **DA_1** (uncompressed)

---

## Chunks Payload Serialization

Multiple chunks within a Batch are serialized into a contiguous byte array using a length-prefixed format.

### Format

```
┌────────────────────────┬─────────────────────┐
│ chunk_0_len (4 bytes)  │ chunk_0_bytes       │
├────────────────────────┼─────────────────────┤
│ chunk_1_len (4 bytes)  │ chunk_1_bytes       │
├────────────────────────┼─────────────────────┤
│ ...                    │ ...                 │
└────────────────────────┴─────────────────────┘
```

Each chunk is prefixed by a 4-byte big-endian length field, followed by the serialized chunk bytes (as defined in the [Chunk](#chunk) section).

---

## EIP-4844 Blob Encoding

When submitting to Ethereum L1 via EIP-4844, the DA data (metadata + payload) is packed into blobs.

### Blob Structure

- Each blob consists of **4096 words** of 32 bytes each = **131,072 bytes** (128 KB)
- Each 32-byte word uses only **31 bytes for data** (the first byte is reserved)
- Effective capacity per blob: **31 × 4096 = 126,976 bytes** (~124 KB)

### Encoding Process

1. The first byte of the first word contains the **DA version** number
2. Data is split into 31-byte segments
3. Each segment is packed into a 32-byte word (byte 0 is set to 0 except for the DA version in the first word)
4. Words are grouped into 128 KB blobs
5. Multiple blobs may be used if data exceeds one blob's capacity

### Data Hash for Blobs

When using EIP-4844, the `dataHash` in the BatchHeader is computed as:

```
dataHash = Keccak256(versionedHash_0 ++ versionedHash_1 ++ ... ++ versionedHash_N)
```

Where each `versionedHash` is the KZG versioned hash of the corresponding blob commitment.

---

## L1 Submission Modes

Batch data can be submitted to L1 in two ways:

### 1. EIP-4844 Blob Transaction (Ethereum L1)

Used when the parent chain is Ethereum.

```
commitBatch(version, batchIndex, totalL1MessagePopped) + EIP-4844 Blobs
```

The Batch header fields and chunk data are encoded into blobs attached to the transaction.

### 2. DA Service Mode (L3 / Jovay Parent Chain)

Used when the parent chain is another Jovay chain (L3 architecture).

```
commitBatchWithDaProof(serializedBatchHeader, totalL1MessagePopped, daProof)
```

The chunk data is stored via an external DA Service, and only a DA proof is submitted on-chain.

---

## Database Schema

### `batches` Table

| Column | Type | Description |
|--------|------|-------------|
| `id` | INT(11) PK | Auto-increment primary key |
| `version` | INT(8) | Batch version (0, 1, or 2) |
| `batch_header_hash` | VARCHAR(66) BINARY | Keccak256 hash of BatchHeader (unique) |
| `batch_index` | BIGINT UNSIGNED | Sequential Batch index (unique) |
| `l1_message_popped` | INT(11) | Number of L1 messages included in this Batch |
| `total_l1_message_popped` | INT(11) | Cumulative L1 messages up to this Batch |
| `l1msg_rolling_hash` | VARCHAR(66) BINARY | Rolling hash of L1 messages |
| `data_hash` | VARCHAR(66) BINARY | Hash of the Batch data |
| `parent_batch_hash` | VARCHAR(66) BINARY | Hash of the parent Batch |
| `post_state_root` | VARCHAR(66) | Post-execution state root |
| `l2_msg_root` | VARCHAR(66) | L2 message Merkle root |
| `start_number` | VARCHAR(64) | First block number in this Batch |
| `end_number` | VARCHAR(64) | Last block number in this Batch |
| `chunk_num` | INT(11) | Number of Chunks in this Batch |
| `gmt_create` | DATETIME | Record creation time |
| `gmt_modified` | DATETIME | Record modification time |

### `chunks` Table

| Column | Type | Description |
|--------|------|-------------|
| `id` | INT(11) PK | Auto-increment primary key |
| `batch_version` | INT(8) | Batch version (-1 for legacy data) |
| `batch_index` | BIGINT UNSIGNED | Batch index this Chunk belongs to |
| `chunk_index` | INT(11) | Chunk index within the Batch |
| `chunk_hash` | VARCHAR(64) | Hash of the Chunk data |
| `num_blocks` | INT(11) | Number of blocks in this Chunk |
| `zk_cycle_sum` | BIGINT UNSIGNED | ZK proof cycle count |
| `gas_sum` | BIGINT | Cumulative gas used (-1 for legacy data) |
| `start_number` | VARCHAR(64) | First block number in this Chunk |
| `end_number` | VARCHAR(64) | Last block number in this Chunk |
| `raw_chunk` | LONGBLOB | Serialized Chunk data |
| `gmt_create` | DATETIME | Record creation time |
| `gmt_modified` | DATETIME | Record modification time |

---

## Domain Model Classes

### BatchWrapper

The `BatchWrapper` class is the primary domain model for Batches, wrapping the core `Batch` with additional metadata.

| Field | Type | Description |
|-------|------|-------------|
| `batch` | `Batch` | Core Batch (header + payload + DA data) |
| `startBlockNumber` | `BigInteger` | First L2 block number in this Batch |
| `endBlockNumber` | `BigInteger` | Last L2 block number in this Batch |
| `postStateRoot` | `byte[]` | State root after executing all transactions in this Batch |
| `l2MsgRoot` | `byte[]` | Merkle root of L2-to-L1 messages |
| `totalL1MessagePopped` | `long` | Cumulative count of L1 messages processed |
| `l1MessagePopped` | `long` | Count of L1 messages in this Batch |
| `gmtCreate` | `long` | Creation timestamp |

### ChunkWrapper

The `ChunkWrapper` class wraps a Chunk with batch-level context.

| Field | Type | Description |
|-------|------|-------------|
| `batchVersion` | `BatchVersionEnum` | Batch version this Chunk belongs to |
| `batchIndex` | `BigInteger` | Batch index |
| `chunkIndex` | `long` | Chunk index within the Batch |
| `chunk` | `Chunk` | Core Chunk (blocks + transactions) |
| `gasSum` | `long` | Cumulative gas used (-1 for legacy data) |

---

## Source File Reference

| Category | File Path |
|----------|-----------|
| Batch core | `relayer-commons/.../l2basic/Batch.java` |
| BatchHeader | `relayer-commons/.../l2basic/BatchHeader.java` |
| Batch versions | `relayer-commons/.../l2basic/BatchVersionEnum.java` |
| Chunk core | `relayer-commons/.../l2basic/Chunk.java` |
| Block context | `relayer-commons/.../l2basic/BlockContext.java` |
| Chunks payload | `relayer-commons/.../l2basic/ChunksPayload.java` |
| Chunk codec | `relayer-commons/.../l2basic/IChunkCodec.java` |
| DA versions | `relayer-commons/.../l2basic/DaVersion.java` |
| DA compressor | `relayer-commons/.../l2basic/IDaCompressor.java` |
| Blob DA data | `relayer-commons/.../l2basic/BlobsDaData.java` |
| DA data interface | `relayer-commons/.../l2basic/IDaData.java` |
| BatchWrapper | `relayer-commons/.../models/BatchWrapper.java` |
| ChunkWrapper | `relayer-commons/.../models/ChunkWrapper.java` |
| Serialization utils | `relayer-commons/.../utils/RollupUtils.java` |
| Batches entity | `relayer-dal/.../entities/BatchesEntity.java` |
| Chunks entity | `relayer-dal/.../entities/ChunksEntity.java` |
| DB migration (init) | `relayer-app/.../db/migration/V0.0.1__init-db.sql` |
| DB migration (rolling hash) | `relayer-app/.../db/migration/V0.0.3__add-l1msg-rolling-hash.sql` |
| DB migration (batch V2) | `relayer-app/.../db/migration/V0.0.7__batch-v2-update.sql` |
| DB migration (gas sum) | `relayer-app/.../db/migration/V0.0.8__add_gas_sum_for_chunk.sql` |
