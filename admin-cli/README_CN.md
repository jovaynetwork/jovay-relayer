# L2-Relayer Admin CLI 使用指南

> [English Version](README.md)

## 概述

Admin CLI 是 L2-Relayer 的命令行管理工具，提供对 Relayer 系统的运维管理、数据查询、交易操作等功能。本文档详细说明了每个命令的功能、使用方法和参数。

## 快速开始

### 启动 CLI

```bash
cd admin-cli
java -jar target/relayer-cli.jar
```

进入交互式命令行后，可以输入命令进行操作。

或者解压[relayer-cli-bin.tar.gz](target/relayer-cli-bin.tar.gz)，通过运行`bin/start.sh`启动 CLI

### 查看帮助

```bash
# 查看所有可用命令
help

# 查看特定命令的详细帮助
help <command-name>
```

### 命令格式

```bash
command-name --option1 value1 --option2 value2
```

---

## 核心功能命令 (Core Commands)

### waste-eth-account-nonce

**功能：** 浪费指定以太坊账户的 Nonce 值，用于解锁卡住的账户或处理 Nonce 间隙。

**语法：**
```bash
waste-eth-account-nonce --chainType <ChainType> --address <String> --nonce <long>
```

**参数：**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `--chainType` | ChainType | 否 | L1 | 链类型：L1 或 L2 |
| `--address` | String | 是 | - | 以太坊账户地址（0x开头） |
| `--nonce` | long | 是 | - | 要浪费的 Nonce 值 |

**示例：**
```bash
# 浪费 L1 账户的 nonce 100
waste-eth-account-nonce --chainType L1 --address 0x863df6bfa4469f3ead0be8f9f2aae51c91a907b4 --nonce 100

# 浪费 L2 账户的 nonce 50
waste-eth-account-nonce --chainType L2 --address 0x863df6bfa4469f3ead0be8f9f2aae51c91a907b4 --nonce 50
```

**使用场景：**
- 账户 Nonce 被卡住，需要跳过某个 Nonce 值
- 网络异常导致交易失败，需要重置账户状态
- 需要同步本地 Nonce 和链上实际 Nonce

---

### commit-batch-manually

**功能：** 手动提交指定的 Batch 到 L1，用于加速或恢复 Batch 提交流程。

**语法：**
```bash
commit-batch-manually --batchIndex <long>
```

**参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `--batchIndex` | long | 是 | 要提交的 Batch 索引号 |

**示例：**
```bash
# 提交 Batch 1
commit-batch-manually --batchIndex 1

# 提交 Batch 100
commit-batch-manually --batchIndex 100
```

**约束条件：**
- Batch 必须存在于数据库
- 不能重复提交已提交的 Batch
- 必须按顺序提交（如链上已提交到 Batch 5，下一个必须是 Batch 6）
- 执行前会要求用户确认

**使用场景：**

- 加速待提交的 Batch
- 恢复中断的 Batch 提交
- 手动同步 L1 状态

---

### commit-proof-manually

**功能：** 手动提交指定 Batch 的证明（TEE 或 ZK）到 L1，用于加速证明提交流程。

**语法：**

```bash
commit-proof-manually --batchIndex <long> --proofType <ProofType>
```

**参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `--batchIndex` | long | 是 | Batch 索引号 |
| `--proofType` | ProofType | 是 | 证明类型：TEE 或 ZK |

**示例：**

```bash
# 提交 Batch 1 的 TEE 证明
commit-proof-manually --batchIndex 1 --proofType TEE

# 提交 Batch 2 的 ZK 证明
commit-proof-manually --batchIndex 2 --proofType ZK
```

**约束条件：**
- 证明必须已生成
- 不能重复提交同一个 Batch 的同一种证明
- TEE 和 ZK 证明可以分别提交
- 执行前会要求用户确认

**使用场景：**

- 加速待提交的证明
- 恢复中断的证明提交
- 提高 Batch 的最终确认速度

---

### get-batch

**功能：** 获取指定 Batch 的详细信息并可选保存为 JSON 文件。

**语法：**
```bash
get-batch --batchIndex <String> [--filePath <String>]
```

**参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `--batchIndex` | String | 是 | Batch 索引号 |
| `--filePath` | String | 否 | 要保存的文件路径（可选） |

**示例：**
```bash
# 查询 Batch 1 并显示
get-batch --batchIndex 1

# 查询 Batch 1 并保存为文件
get-batch --batchIndex 1 --filePath /tmp/batch1.json
```

**返回信息：**
- Batch 版本（BatchV0/BatchV1）
- Batch 包含的区块范围
- 状态根 (State Root)
- L1/L2 消息根
- Chunks 详细信息
- Batch 大小和数据

**使用场景：**
- 验证 Batch 结构和内容
- 导出 Batch 数据用于分析
- 调试 Batch 相关问题

---

### retry-batch-tx

**功能：** 重试指定范围内的失败 Batch 交易，支持按交易类型筛选。

**语法：**
```bash
retry-batch-tx --type <TransactionTypeEnum> --fromBatchIndex <long> --toBatchIndex <long>
```

**参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `--type` | TransactionTypeEnum | 是 | 交易类型（BATCH_COMMIT_TX、BATCH_TEE_PROOF_COMMIT_TX 等） |
| `--fromBatchIndex` | long | 是 | 起始 Batch 索引（含） |
| `--toBatchIndex` | long | 是 | 结束 Batch 索引（含） |

**示例：**
```bash
# 重试 Batch 1-10 的 BATCH_COMMIT_TX
retry-batch-tx --type BATCH_COMMIT_TX --fromBatchIndex 1 --toBatchIndex 10

# 重试 Batch 50-100 的 TEE 证明提交
retry-batch-tx --type BATCH_TEE_PROOF_COMMIT_TX --fromBatchIndex 50 --toBatchIndex 100

# 重试 Batch 1-5 的 ZK 证明提交
retry-batch-tx --type BATCH_ZK_PROOF_COMMIT_TX --fromBatchIndex 1 --toBatchIndex 5
```

**使用场景：**
- 重试网络问题导致的失败交易
- 批量修复交易失败问题
- 恢复中断的交易流程

---

### query-batch-tx-info

**功能：** 查询指定 Batch 的特定类型交易信息。

**语法：**

```bash
query-batch-tx-info --type <TransactionTypeEnum> --batchIndex <long>
```

**参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `--type` | TransactionTypeEnum | 是 | 交易类型 |
| `--batchIndex` | long | 是 | Batch 索引号 |

**示例：**
```bash
# 查询 Batch 1 的 BATCH_COMMIT_TX 信息
query-batch-tx-info --type BATCH_COMMIT_TX --batchIndex 1

# 查询 Batch 5 的 TEE 证明提交信息
query-batch-tx-info --type BATCH_TEE_PROOF_COMMIT_TX --batchIndex 5
```

**返回信息：**
- 交易哈希
- 交易状态（PENDING、CONFIRMED、FAILED 等）
- 时间戳
- Gas 使用情况
- 失败原因（如有）

**使用场景：**
- 追踪特定交易的状态
- 调试交易失败原因
- 验证交易确认状态

---

### query-relayer-address

**功能：** 查询 Relayer 系统使用的所有地址信息。

**语法：**
```bash
query-relayer-address
```

**返回信息：**
- L1 Relayer 账户地址
- L2 Relayer 账户地址
- 其他相关地址

**使用场景：**
- 确认 Relayer 账户配置
- 验证地址是否正确
- 用于问题排查

---

### get-l2msg-proof

**功能：** 获取指定消息 Nonce 对应的证明。

**语法：**

```bash
get-l2msg-proof --l2MsgNonce <String>
```

**参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `--l2MsgNonce` | String | 是 | L2 消息 Nonce |

**使用场景：**
- 验证特定消息的证明
- 获取消息跨链信息

---

### init-anchor-batch

**功能：** 初始化 Relayer，设置起始 Anchor Batch。用于 Relayer 第一次启动或重新初始化。

**语法：**

```bash
init-anchor-batch --anchorBatchIndex <String> --rawAnchorBatchHeaderHex <String> --nextL2MsgNonce <String> --l2MerkleTreeBranchesHex <String>
```

**参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `--anchorBatchIndex` | String | 否 | Anchor Batch 索引 |
| `--rawAnchorBatchHeaderHex` | String | 否 | Anchor Batch Header（Hex 格式） |
| `--nextL2MsgNonce` | String | 否 | 下一个 L2 消息 Nonce |
| `--l2MerkleTreeBranchesHex` | String | 否 | L2 Merkle Tree 分支（Hex 格式） |

**初始化常用：初始化0号Batch**

```shell
docker exec -it l2-relayer-0 bash /l2-relayer/bin/init_anchor.sh -a 0000000000000000000000000000000000000000000000000000000000000000000000000000000000bac4320768bc80b363e3d087c8decdd621f65f9c335e4603bc63525ed57aaa7c0000000000000000000000000000000000000000000000000000000000000000
```

**重要提示：**

- 仅在 Relayer 首次启动时使用
- 参数必须与 L1 合约状态一致
- 错误的初始化可能导致数据不一致

**使用场景：**
- Relayer 初始化安装
- 数据库恢复
- 系统重新同步

---

### set-l1start-height

**功能：** 设置 L1 轮询的起始区块高度。在**relayer设置L1起始拉块高度**，relayer才会对L1的L1Msg进行拉块过滤，以及后续提交到L2，操作如下，参数YOUR_HEIGHT为你指定的L1起始高度，必须为第一笔发送L1Msg交易之前的高度：*如果从未发送过deposit（即L1Msg），高度从L1浏览器获取最新区块高度，如果发送过，则需要设置成第一笔deposit所在高度。*

**语法：**

```bash
set-l1start-height --startHeight <String>
```

**参数：**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `--startHeight` | String | 否 | 1 | L1 轮询起始区块高度 |

**示例：**

```bash
# 设置从区块 18000000 开始轮询
set-l1start-height --startHeight 18000000
```

**使用场景：**

- Relayer 初始化时设置起始高度

---

### query-batch-da-info

**功能：** 查询 Batch 的数据可用性（DA）相关信息。

**语法：**

```bash
query-batch-da-info --batchIndex <String>
```

**参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `--batchIndex` | String | 是 | Batch 索引号 |

**返回信息：**

- DA 提交状态
- Blob 信息
- 数据大小

**使用场景：**

- 验证 Batch DA 状态
- 调查数据可用性问题

---

### speedup-rollup-tx

**功能：** 加速指定的 Rollup 交易，通过提高 Gas 价格。

**语法：**

```bash
speedup-rollup-tx --batchIndex <long> --txType <TransactionTypeEnum> [--maxPriorityFeePerGas <String>] [--maxFeePerBlobGas <String>] [--maxFeePerGas <String>]
```

**参数：**

- batchIndex：Batch的index；
- txType：交易类型，比如BATCH_COMMIT_TX、BATCH_TEE_PROOF_COMMIT_TX，可用tab补全；
- maxPriorityFeePerGas：加速的priority price，不指定的话会走relayer的当前策略获取price；
- maxFeePerBlobGas：加速的maxFeePerBlobGas，不指定的话会走relayer的当前策略获取price；
- maxFeePerGas：加速的maxFeePerGas，不指定的话会走relayer的当前策略获取price；

⚠️如果是4844交易，maxPriorityFeePerGas和maxFeePerBlobGas必须增加至少原交易的100%，即两倍

⚠️如果是1559交易，maxPriorityFeePerGas和maxFeePerBlobGas必须增加至少原交易的10%，即1.1倍

**示例：**

```bash
speedup-rollup-tx --batchIndex 691 --txType BATCH_COMMIT_TX --maxPriorityFeePerGas 1000000000 --maxFeePerBlobGas 66251755244
```

**使用场景：**
- 加速待确认的交易
- 解决交易 Pending 过久的问题

---

### query-relayer-account-nonce

**功能：** 查询Relayer账户的下一个nonce

**语法：**

```bash
query-relayer-account-nonce --chainType <ChainType> [--accType <AccType>]
```

**参数：**

| 参数          | 类型   | 必填 | 说明                                   |
| ------------- | ------ | ---- | -------------------------------------- |
| `--chainType` | String | 否   | 要查询账户所在的链，支持L1、L2，默认L1 |
| `--accType`   | String | 是   | 要查询账户类型，支持BLOB、LEGACY       |

**示例：**

```bash
query-relayer-account-nonce --accType BLOB
```

---

### update-relayer-account-nonce-manually

**功能：** 手动更新Relayer账户的下一个nonce，这里仅支持nonce维护模式在fast的情况下进行修改。

**语法：**

```bash
update-relayer-account-nonce-manually --chainType <ChainType> --accType <AccType> --nonce <long>
```

**参数：**

| 参数          | 类型   | 必填 | 说明                                   |
| ------------- | ------ | ---- | -------------------------------------- |
| `--chainType` | String | 否   | 要查询账户所在的链，支持L1、L2，默认L1 |
| `--accType`   | String | 是   | 要查询账户类型，支持BLOB、LEGACY       |
| `--nonce`     | long   | 是   | 要更新的nonce值                        |

**示例：**

```bash
update-relayer-account-nonce-manually --accType LEGACY --nonce 100
```

---

### refetch-proof

**功能：** 重新从PC获取proof，如果proof没有提交到rollup合约，那么Relayer会重新提交这些被重新获取的proof

**语法：**

```bash
refetch-proof --proofType <ProveTypeEnum> --fromBatchIndex <String> --toBatchIndex <String>
```

**参数：**

| 参数               | 类型   | 必填 | 说明                                                         |
| ------------------ | ------ | ---- | ------------------------------------------------------------ |
| `--proofType`      | Enum   | 否   | 重新获取的proof类型，这里支持TEE_PROOF、ZK_PROOF，默认TEE_PROOF |
| `--fromBatchIndex` | String | 是   | 重新获取proof的起始batch index                               |
| `--toBatchIndex`   | String | 是   | 重新获取proof的结束batch index，包含在内                     |

**示例：**

```bash
refetch-proof --fromBatchIndex 10 --toBatchIndex 10
```

---

### rollback-to-subchain-height

**功能：** 将 Relayer 回滚到指定的子链高度。这是一个**危险操作**，会删除数据库中的数据！

**语法：**

```bash
rollback-to-subchain-height --targetBatchIndex <long> --targetBlockHeight <long> --l1MsgNonceThreshold <long>
```

**参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `--targetBatchIndex` | long | 是 | 目标 Batch 索引，batch_index >= 此值的 Batch 将被删除 |
| `--targetBlockHeight` | long | 是 | Subchain回滚高度，从这个区块开始将会在账本回滚，BLOCK_PROCESSED 将被设置为此值 - 1 |
| `--l1MsgNonceThreshold` | long | 是 | 在targetBlockHeight之前的已经提交到Subchain L1Msg的nonce，L1Msg Nonce 阈值，nonce > 此值的 L1 消息将被重置 |

**示例：**

```bash
# 回滚到 Batch 100，区块高度 50000，L1 消息 Nonce 阈值 200
rollback-to-subchain-height --targetBatchIndex 100 --targetBlockHeight 50000 --l1MsgNonceThreshold 200
```

**执行流程：**

1. 首先会提示确认是否已停止所有其他 Relayer 节点
2. 显示详细的回滚参数和影响范围
3. 要求输入 `yes` 确认
4. 要求输入 `ROLLBACK` 进行二次确认

**回滚操作内容：**

执行此命令后，系统将自动执行以下操作：

1. **rollup_number_record 表**：更新 NEXT_BATCH、NEXT_MSG_PROVE_BATCH、BATCH_COMMITTED、BLOCK_PROCESSED、NEXT_CHUNK_GAS_ACCUMULATOR、NEXT_CHUNK 等记录
2. **batches 表**：删除 batch_index >= targetBatchIndex 的所有 Batch
3. **chunks 表**：删除 batch_index > targetBatchIndex 的 Chunk，以及 batch_index = targetBatchIndex 且 chunk_index >= 目标 Chunk 的记录
4. **reliable_transaction 表**：删除相关的 BATCH_COMMIT_TX、BATCH_TEE_PROOF_COMMIT_TX、L1_MSG_TX 交易记录
5. **batch_prove_request 表**：删除 batch_index >= targetBatchIndex 的证明请求
6. **l2_merkle_tree 表**：删除 batch_index >= targetBatchIndex 的 Merkle 树
7. **inter_bc_msg 表**：
   - L1 消息：将 nonce > l1MsgNonceThreshold 的消息状态重置为 MSG_READY
   - L2 消息：删除 source_block_height >= targetBlockHeight 的消息，重置 source_block_height < targetBlockHeight 且状态不为 MSG_READY 的消息
8. **oracle_request 表**：删除 L2_BATCH_COMMIT、L2_BATCH_PROVE 类型且 request_index >= targetBatchIndex 的请求

**重要提示：**

⚠️ **此操作不可逆！** 执行前请确保：

1. **已停止所有其他 Relayer 节点**：回滚期间必须确保只有当前连接的 Relayer 节点在运行，否则可能导致数据不一致
2. **已备份数据库**：建议在执行前备份数据库
3. **确认参数正确**：仔细核对 targetBatchIndex、targetBlockHeight 和 l1MsgNonceThreshold 的值
4. **了解影响范围**：回滚会删除大量数据，请确保了解影响范围

**使用场景：**

- Subchain 链发生重组（Reorg），需要回滚 Relayer 数据以重新同步
- 测试环境需要重置到某个历史状态
- 数据异常需要回滚到正确的状态点

---

## Gas 价格配置命令 (Gas Price Configuration)

这些命令用于管理 L1 的 Gas 价格配置。

### 查询 Gas 配置

```bash
# 查询 Gas 价格增加百分比
get-eth-gas-price-increased-percentage

# 查询最大 Gas 价格限制
get-eth-max-price-limit

# 查询优先费增加百分比
get-eth-priority-fee-per-gas-increased-percentage

# 查询最小 EIP1559 优先费
get-eth-minimum-eip1559priority-price

# 查询 Base Fee 乘数
get-eth-base-fee-multiplier

# 查询 EIP4844 优先费增加百分比
get-eth-eip4844priority-fee-per-gas-increased-percentage

# 查询最小 EIP4844 优先费
get-eth-minimum-eip4844priority-price

# 查询 Blob Gas 倍数配置
get-eth-larger-fee-per-blob-gas-multiplier
get-eth-smaller-fee-per-blob-gas-multiplier

# 查询 Blob Gas 分界值
get-eth-fee-per-blob-gas-dividing-val

# 查询额外 Gas 价格
get-eth-extra-gas-price
```

### 更新 Gas 配置

所有更新命令统一使用 `--value` 参数：

```bash
# 更新 Gas 价格增加百分比（Double 类型）
update-eth-gas-price-increased-percentage --value <Double>

# 更新最大 Gas 价格限制（String 类型，单位 Wei）
update-eth-max-price-limit --value <String>

# 更新优先费增加百分比（Double 类型）
update-eth-priority-fee-per-gas-increased-percentage --value <Double>

# 更新最小 EIP1559 优先费（String 类型，单位 Wei）
update-eth-minimum-eip1559priority-price --value <String>

# 更新 Base Fee 乘数（String 类型）
update-eth-base-fee-multiplier --value <String>

# 更新 EIP4844 优先费增加百分比（Double 类型）
update-eth-eip4844priority-fee-per-gas-increased-percentage --value <Double>

# 更新最小 EIP4844 优先费（String 类型，单位 Wei）
update-eth-minimum-eip4844priority-price --value <String>

# 更新 Blob Gas 倍数配置（String 类型）
update-eth-larger-fee-per-blob-gas-multiplier --value <String>
update-eth-smaller-fee-per-blob-gas-multiplier --value <String>

# 更新 Blob Gas 分界值（String 类型，单位 Wei）
update-eth-fee-per-blob-gas-dividing-val --value <String>

# 更新额外 Gas 价格（String 类型，单位 Wei）
update-eth-extra-gas-price --value <String>
```

**使用场景：**
- 调整 Gas 价格策略以应对网络拥堵
- 优化交易成本
- 响应以太坊网络状况变化

---

## Rollup 经济策略命令 (Rollup Economic Strategy)

这些命令用于管理 Rollup 系统的经济性约束参数。

### 查询经济策略配置

```bash
# 查询最大待审核 Batch 数
get-max-pending-batch-count

# 查询最大待审核证明数
get-max-pending-proof-count

# 查询最大 Batch 等待时间（秒）
get-max-batch-waiting-time

# 查询最大证明等待时间（秒）
get-max-proof-waiting-time

# 查询中等 EIP1559 价格限制（Wei）
get-mid-eip1559price-limit

# 查询高 EIP1559 价格限制（Wei）
get-high-eip1559price-limit
```

### 更新经济策略配置

所有更新命令统一使用 `--value` 参数：

```bash
# 更新最大待审核 Batch 数（Integer 类型）
update-max-pending-batch-count --value <Integer>

# 更新最大待审核证明数（Integer 类型）
update-max-pending-proof-count --value <Integer>

# 更新最大 Batch 等待时间（Long 类型，单位：秒）
update-max-batch-waiting-time --value <Long>

# 更新最大证明等待时间（Long 类型，单位：秒）
update-max-proof-waiting-time --value <Long>

# 更新中等 EIP1559 价格限制（String 类型，单位：Wei）
update-mid-eip1559price-limit --value <String>

# 更新高 EIP1559 价格限制（String 类型，单位：Wei）
update-high-eip1559price-limit --value <String>
```

**参数说明：**

| 参数 | 说明 |
|------|------|
| `maxPendingBatchCount` | 同时待提交的最大 Batch 数，超过此数量时暂停新 Batch 提交 |
| `maxPendingProofCount` | 同时待提交的最大证明数，超过此数量时暂停证明提交 |
| `maxBatchWaitingTime` | Batch 的最大等待时间，超时后强制提交 |
| `maxProofWaitingTime` | 证明的最大等待时间，超时后强制提交 |
| `midEip1559PriceLimit` | 中等 Gas 价格限制（黄区下界），用于区分不同的 Gas 价格策略 |
| `highEip1559PriceLimit` | 高 Gas 价格限制（红区下界），超过此限制则暂停提交 |

**示例：**
```bash
# 设置最多允许 10 个待审核 Batch
update-max-pending-batch-count --value 10

# 设置 Batch 最长等待 1 小时（3600 秒）
update-max-batch-waiting-time --value 3600
```

**使用场景：**
- 优化系统吞吐量
- 控制 Gas 成本
- 应对网络拥堵
- 平衡确认速度和成本

---

## 工具函数命令 (Utility Commands)

### convert-private-key-to-pem

**功能：** 将 Hex 格式的私钥转换为 PEM 格式（X509）。

**语法：**
```bash
convert-private-key-to-pem --privateKey <String>
```

**参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `--privateKey` | String | 是 | Hex 格式的私钥（0x开头） |

**示例：**
```bash
convert-private-key-to-pem --privateKey 0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef
```

---

### convert-public-key-to-eth-address

**功能：** 将 PEM 格式的公钥转换为以太坊地址。

**语法：**
```bash
convert-public-key-to-eth-address --publicKey <String>
```

**参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `--publicKey` | String | 是 | PEM 格式的公钥 |

---

## 常见使用场景

### 场景 1：解决 Nonce 卡住问题

当账户 Nonce 被卡住，后续交易无法发送时：

```bash
# 1. 查询当前 Relayer 地址
query-relayer-address

# 2. 浪费指定的 Nonce
waste-eth-account-nonce --chainType L1 --address <relayer-address> --nonce <stuck-nonce>

# 3. 验证 Nonce 是否已解锁
# （可以通过外部工具或重新尝试发送交易来验证）
```

---

### 场景 2：手动加速 Batch 提交

当 Batch 提交缓慢需要手动加速时：

```bash
# 1. 查询待提交的 Batch
get-batch --batchIndex 100

# 2. 手动提交 Batch
commit-batch-manually --batchIndex 100

# 3. 查询提交状态
query-batch-tx-info --type BATCH_COMMIT_TX --batchIndex 100
```

---

### 场景 3：完成证明提交流程

当证明已生成需要提交到 L1 时：

```bash
# 1. 查询证明状态
query-batch-tx-info --type BATCH_TEE_PROOF_COMMIT_TX --batchIndex 50

# 2. 提交 TEE 证明
commit-proof-manually --batchIndex 50 --proofType TEE_PROOF

# 3. 如需 ZK 证明，继续提交
commit-proof-manually --batchIndex 50 --proofType ZK_PROOF
```

---

### 场景 4：批量恢复失败交易

当多个 Batch 的交易失败需要批量重试时：

```bash
# 1. 重试 Batch 1-50 的提交交易
retry-batch-tx --type BATCH_COMMIT_TX --fromBatchIndex 1 --toBatchIndex 50

# 2. 重试 Batch 1-50 的 TEE 证明提交
retry-batch-tx --type BATCH_TEE_PROOF_COMMIT_TX --fromBatchIndex 1 --toBatchIndex 50
```

---

### 场景 5：调整 Gas 价格应对网络拥堵

当网络拥堵导致交易缓慢时：

```bash
# 1. 查询当前 Gas 配置
get-eth-gas-price-increased-percentage

# 2. 提高 Gas 价格（如增加 50%，设置为 1.5）
update-eth-gas-price-increased-percentage --value 1.5

# 3. 加速待审核交易
speedup-rollup-tx --batchIndex <batch-index> --txType BATCH_COMMIT_TX --maxPriorityFeePerGas 1000000000
```

---

### 场景 6：初始化 Relayer

在新环境部署 Relayer 时：

```bash
# 1. 设置 L1 轮询起始高度
set-l1start-height --startHeight <start-block-height>

# 2. 初始化 Anchor Batch
init-anchor-batch --anchorBatchIndex <index> --rawAnchorBatchHeaderHex <hex> --nextL2MsgNonce <nonce> --l2MerkleTreeBranchesHex <branches>

# 3. 查询 Relayer 地址确认配置
query-relayer-address
```

---

## 交互式命令行功能

在 CLI 交互模式下，还支持以下内置命令：

```bash
# 显示命令历史
history

# 显示版本信息
version

# 从文件执行命令
script <filename>

# 显示帮助
help [command-name]
```

---

## 安全建议

1. **确认操作**：修改性命令会要求用户确认，请仔细检查参数
2. **备份数据**：执行重要操作前，建议备份数据库
3. **监控状态**：执行关键操作后，使用查询命令验证结果
4. **权限管理**：在生产环境中，限制 CLI 访问权限
5. **网络安全**：在安全的网络环境中运行 CLI，避免敏感信息泄露

---

## 获取帮助

在 CLI 中输入 `help <command>` 获取任何命令的详细帮助信息。

```bash
help waste-eth-account-nonce
help commit-batch-manually
help commit-proof-manually
```
