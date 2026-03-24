# L2-Relayer Admin CLI Guide

> [中文文档](README_CN.md)

## Overview

Admin CLI is the command-line management tool for L2-Relayer, providing operations management, data queries, transaction operations, and runtime configuration tuning. This document describes every available command with its syntax, parameters, and usage examples.

## Quick Start

### Launching the CLI

```bash
cd admin-cli
java -jar target/relayer-cli.jar
```

This opens an interactive shell where you can enter commands directly.

Alternatively, extract [relayer-cli-bin.tar.gz](target/relayer-cli-bin.tar.gz) and run `bin/start.sh` to launch the CLI.

### Getting Help

```bash
# List all available commands
help

# Show detailed help for a specific command
help <command-name>
```

### Command Format

```bash
command-name --option1 value1 --option2 value2
```

---

## Core Commands

### waste-eth-account-nonce

**Purpose:** Burn a specific Nonce on an Ethereum account, used to unlock stuck accounts or resolve Nonce gaps.

**Syntax:**
```bash
waste-eth-account-nonce --chainType <ChainType> --address <String> --nonce <long>
```

**Parameters:**

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `--chainType` | ChainType | No | L1 | Chain type: L1 or L2 |
| `--address` | String | Yes | - | Ethereum account address (0x-prefixed) |
| `--nonce` | long | Yes | - | The Nonce value to burn |

**Examples:**
```bash
# Burn nonce 100 on L1
waste-eth-account-nonce --chainType L1 --address 0x863df6bfa4469f3ead0be8f9f2aae51c91a907b4 --nonce 100

# Burn nonce 50 on L2
waste-eth-account-nonce --chainType L2 --address 0x863df6bfa4469f3ead0be8f9f2aae51c91a907b4 --nonce 50
```

**Use Cases:**
- Account Nonce is stuck and a specific Nonce value needs to be skipped
- Network anomaly caused transaction failure — need to reset account state
- Sync local Nonce with the on-chain actual Nonce

---

### commit-batch-manually

**Purpose:** Manually submit a specific Batch to L1, used to accelerate or recover the Batch submission process.

**Syntax:**
```bash
commit-batch-manually --batchIndex <long>
```

**Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `--batchIndex` | long | Yes | The Batch index to submit |

**Examples:**
```bash
commit-batch-manually --batchIndex 1
commit-batch-manually --batchIndex 100
```

**Constraints:**
- The Batch must exist in the database
- Cannot re-submit an already submitted Batch
- Must be submitted in order (e.g., if L1 has committed up to Batch 5, the next must be Batch 6)
- Requires user confirmation before execution

**Use Cases:**
- Accelerate a pending Batch
- Recover an interrupted Batch submission
- Manually sync L1 state

---

### commit-proof-manually

**Purpose:** Manually submit a proof (TEE or ZK) for a specific Batch to L1.

**Syntax:**
```bash
commit-proof-manually --batchIndex <long> --proofType <ProofType>
```

**Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `--batchIndex` | long | Yes | Batch index |
| `--proofType` | ProofType | Yes | Proof type: TEE or ZK |

**Examples:**
```bash
# Submit TEE proof for Batch 1
commit-proof-manually --batchIndex 1 --proofType TEE

# Submit ZK proof for Batch 2
commit-proof-manually --batchIndex 2 --proofType ZK
```

**Constraints:**
- The proof must already be generated
- Cannot re-submit the same proof type for the same Batch
- TEE and ZK proofs can be submitted separately
- Requires user confirmation before execution

**Use Cases:**
- Accelerate pending proofs
- Recover interrupted proof submissions
- Speed up Batch finalization

---

### get-batch

**Purpose:** Retrieve detailed information about a specific Batch, optionally saving it as a JSON file.

**Syntax:**
```bash
get-batch --batchIndex <String> [--filePath <String>]
```

**Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `--batchIndex` | String | Yes | Batch index |
| `--filePath` | String | No | File path to save output (optional) |

**Examples:**
```bash
# Query and display Batch 1
get-batch --batchIndex 1

# Query Batch 1 and save to file
get-batch --batchIndex 1 --filePath /tmp/batch1.json
```

**Returns:**
- Batch version (BatchV0/BatchV1)
- Block range included in the Batch
- State Root
- L1/L2 message roots
- Chunks detail
- Batch size and data

**Use Cases:**
- Verify Batch structure and content
- Export Batch data for analysis
- Debug Batch-related issues

---

### retry-batch-tx

**Purpose:** Retry failed Batch transactions within a specified range, filtered by transaction type.

**Syntax:**
```bash
retry-batch-tx --type <TransactionTypeEnum> --fromBatchIndex <long> --toBatchIndex <long>
```

**Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `--type` | TransactionTypeEnum | Yes | Transaction type (BATCH_COMMIT_TX, BATCH_TEE_PROOF_COMMIT_TX, etc.) |
| `--fromBatchIndex` | long | Yes | Start Batch index (inclusive) |
| `--toBatchIndex` | long | Yes | End Batch index (inclusive) |

**Examples:**
```bash
# Retry BATCH_COMMIT_TX for Batches 1-10
retry-batch-tx --type BATCH_COMMIT_TX --fromBatchIndex 1 --toBatchIndex 10

# Retry TEE proof commits for Batches 50-100
retry-batch-tx --type BATCH_TEE_PROOF_COMMIT_TX --fromBatchIndex 50 --toBatchIndex 100

# Retry ZK proof commits for Batches 1-5
retry-batch-tx --type BATCH_ZK_PROOF_COMMIT_TX --fromBatchIndex 1 --toBatchIndex 5
```

**Use Cases:**
- Retry transactions that failed due to network issues
- Batch-fix transaction failures
- Recover interrupted transaction flows

---

### query-batch-tx-info

**Purpose:** Query transaction information of a specific type for a given Batch.

**Syntax:**
```bash
query-batch-tx-info --type <TransactionTypeEnum> --batchIndex <long>
```

**Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `--type` | TransactionTypeEnum | Yes | Transaction type |
| `--batchIndex` | long | Yes | Batch index |

**Examples:**
```bash
# Query BATCH_COMMIT_TX info for Batch 1
query-batch-tx-info --type BATCH_COMMIT_TX --batchIndex 1

# Query TEE proof commit info for Batch 5
query-batch-tx-info --type BATCH_TEE_PROOF_COMMIT_TX --batchIndex 5
```

**Returns:**
- Transaction hash
- Transaction status (PENDING, CONFIRMED, FAILED, etc.)
- Timestamp
- Gas usage
- Failure reason (if any)

**Use Cases:**
- Track a specific transaction's status
- Debug transaction failures
- Verify transaction confirmation

---

### query-relayer-address

**Purpose:** Query all addresses used by the Relayer system.

**Syntax:**
```bash
query-relayer-address
```

**Returns:**
- L1 Relayer account addresses
- L2 Relayer account addresses
- Other related addresses

**Use Cases:**
- Confirm Relayer account configuration
- Verify addresses are correct
- Aid troubleshooting

---

### get-l2msg-proof

**Purpose:** Retrieve the proof for a given L2 message Nonce.

**Syntax:**
```bash
get-l2msg-proof --l2MsgNonce <String>
```

**Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `--l2MsgNonce` | String | Yes | L2 message Nonce |

**Use Cases:**
- Verify proof for a specific message
- Retrieve cross-chain message information

---

### init-anchor-batch

**Purpose:** Initialize the Relayer by setting the starting Anchor Batch. Used on first startup or re-initialization.

**Syntax:**
```bash
init-anchor-batch --anchorBatchIndex <String> --rawAnchorBatchHeaderHex <String> --nextL2MsgNonce <String> --l2MerkleTreeBranchesHex <String>
```

**Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `--anchorBatchIndex` | String | No | Anchor Batch index |
| `--rawAnchorBatchHeaderHex` | String | No | Anchor Batch Header (hex format) |
| `--nextL2MsgNonce` | String | No | Next L2 message Nonce |
| `--l2MerkleTreeBranchesHex` | String | No | L2 Merkle Tree branches (hex format) |

**Common initialization: init Batch 0**

```shell
docker exec -it l2-relayer-0 bash /l2-relayer/bin/init_anchor.sh -a 0000000000000000000000000000000000000000000000000000000000000000000000000000000000bac4320768bc80b363e3d087c8decdd621f65f9c335e4603bc63525ed57aaa7c0000000000000000000000000000000000000000000000000000000000000000
```

**Important:**
- Only use during first-time Relayer startup
- Parameters must be consistent with L1 contract state
- Incorrect initialization may cause data inconsistency

**Use Cases:**
- Initial Relayer installation
- Database recovery
- System re-synchronization

---

### set-l1start-height

**Purpose:** Set the L1 polling start block height. Relayer polls L1 for L1Msg events starting from this height, then submits them to L2. The height must be before the first L1Msg (deposit) transaction. If no deposits have ever been sent, use the latest L1 block height from a block explorer. If deposits have been sent, set it to the block height of the first deposit.

**Syntax:**
```bash
set-l1start-height --startHeight <String>
```

**Parameters:**

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `--startHeight` | String | No | 1 | L1 polling start block height |

**Examples:**
```bash
# Start polling from block 18000000
set-l1start-height --startHeight 18000000
```

**Use Cases:**
- Setting the start height during Relayer initialization

---

### query-batch-da-info

**Purpose:** Query Data Availability (DA) information for a Batch.

**Syntax:**
```bash
query-batch-da-info --batchIndex <String>
```

**Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `--batchIndex` | String | Yes | Batch index |

**Returns:**
- DA submission status
- Blob information
- Data size

**Use Cases:**
- Verify Batch DA status
- Investigate data availability issues

---

### speedup-rollup-tx

**Purpose:** Speed up a specific Rollup transaction by increasing its Gas price.

**Syntax:**
```bash
speedup-rollup-tx --batchIndex <long> --txType <TransactionTypeEnum> [--maxPriorityFeePerGas <String>] [--maxFeePerBlobGas <String>] [--maxFeePerGas <String>]
```

**Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `--batchIndex` | long | Yes | Batch index |
| `--txType` | TransactionTypeEnum | Yes | Transaction type (e.g., BATCH_COMMIT_TX, BATCH_TEE_PROOF_COMMIT_TX). Tab-completable. |
| `--maxPriorityFeePerGas` | String | No | Priority fee for speed-up. If omitted, uses Relayer's current strategy. |
| `--maxFeePerBlobGas` | String | No | maxFeePerBlobGas for speed-up. If omitted, uses Relayer's current strategy. |
| `--maxFeePerGas` | String | No | maxFeePerGas for speed-up. If omitted, uses Relayer's current strategy. |

> ⚠️ For EIP-4844 transactions, `maxPriorityFeePerGas` and `maxFeePerBlobGas` must increase by at least 100% (2x) of the original.
>
> ⚠️ For EIP-1559 transactions, `maxPriorityFeePerGas` must increase by at least 10% (1.1x) of the original.

**Examples:**
```bash
speedup-rollup-tx --batchIndex 691 --txType BATCH_COMMIT_TX --maxPriorityFeePerGas 1000000000 --maxFeePerBlobGas 66251755244
```

**Use Cases:**
- Accelerate pending transactions
- Resolve transactions stuck in pending state for too long

---

### query-relayer-account-nonce

**Purpose:** Query the next Nonce for a Relayer account.

**Syntax:**
```bash
query-relayer-account-nonce --chainType <ChainType> [--accType <AccType>]
```

**Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `--chainType` | ChainType | No | Chain to query: L1 or L2 (default: L1) |
| `--accType` | AccType | Yes | Account type: BLOB or LEGACY |

**Examples:**
```bash
query-relayer-account-nonce --accType BLOB
```

---

### update-relayer-account-nonce-manually

**Purpose:** Manually update the next Nonce for a Relayer account. Only supported when the Nonce management policy is set to `FAST`.

**Syntax:**
```bash
update-relayer-account-nonce-manually --chainType <ChainType> --accType <AccType> --nonce <long>
```

**Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `--chainType` | ChainType | No | Chain: L1 or L2 (default: L1) |
| `--accType` | AccType | Yes | Account type: BLOB or LEGACY |
| `--nonce` | long | Yes | Nonce value to set |

**Examples:**
```bash
update-relayer-account-nonce-manually --accType LEGACY --nonce 100
```

---

### refetch-proof

**Purpose:** Re-fetch proofs from the Prover Controller. If the proofs have not yet been submitted to the Rollup contract, Relayer will re-submit the re-fetched proofs.

**Syntax:**
```bash
refetch-proof --proofType <ProveTypeEnum> --fromBatchIndex <String> --toBatchIndex <String>
```

**Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `--proofType` | ProveTypeEnum | No | Proof type: TEE_PROOF or ZK_PROOF (default: TEE_PROOF) |
| `--fromBatchIndex` | String | Yes | Start Batch index |
| `--toBatchIndex` | String | Yes | End Batch index (inclusive) |

**Examples:**
```bash
refetch-proof --fromBatchIndex 10 --toBatchIndex 10
```

---

### rollback-to-subchain-height

**Purpose:** Roll back the Relayer to a specified subchain height. This is a **dangerous operation** that deletes data from the database!

**Syntax:**
```bash
rollback-to-subchain-height --targetBatchIndex <long> --targetBlockHeight <long> --l1MsgNonceThreshold <long>
```

**Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `--targetBatchIndex` | long | Yes | Target Batch index — Batches with batch_index >= this value will be deleted |
| `--targetBlockHeight` | long | Yes | Subchain rollback height — block processing will restart from here, BLOCK_PROCESSED is set to this value - 1 |
| `--l1MsgNonceThreshold` | long | Yes | L1Msg Nonce threshold — messages with nonce > this value will be reset |

**Examples:**
```bash
# Rollback to Batch 100, block height 50000, L1 message Nonce threshold 200
rollback-to-subchain-height --targetBatchIndex 100 --targetBlockHeight 50000 --l1MsgNonceThreshold 200
```

**Execution Flow:**

1. Prompts to confirm all other Relayer nodes have been stopped
2. Displays detailed rollback parameters and impact scope
3. Requires typing `yes` to confirm
4. Requires typing `ROLLBACK` as a second confirmation

**Rollback Operations:**

After execution, the system automatically performs these operations:

1. **rollup_number_record table**: Updates NEXT_BATCH, NEXT_MSG_PROVE_BATCH, BATCH_COMMITTED, BLOCK_PROCESSED, NEXT_CHUNK_GAS_ACCUMULATOR, NEXT_CHUNK, etc.
2. **batches table**: Deletes all Batches with batch_index >= targetBatchIndex
3. **chunks table**: Deletes Chunks with batch_index > targetBatchIndex, and Chunks with batch_index = targetBatchIndex where chunk_index >= the target Chunk
4. **reliable_transaction table**: Deletes related BATCH_COMMIT_TX, BATCH_TEE_PROOF_COMMIT_TX, L1_MSG_TX transaction records
5. **batch_prove_request table**: Deletes proof requests with batch_index >= targetBatchIndex
6. **l2_merkle_tree table**: Deletes Merkle trees with batch_index >= targetBatchIndex
7. **inter_bc_msg table**:
   - L1 messages: Resets messages with nonce > l1MsgNonceThreshold to MSG_READY status
   - L2 messages: Deletes messages with source_block_height >= targetBlockHeight, resets messages with source_block_height < targetBlockHeight and status != MSG_READY
8. **oracle_request table**: Deletes L2_BATCH_COMMIT and L2_BATCH_PROVE requests with request_index >= targetBatchIndex

**Important:**

⚠️ **This operation is irreversible!** Before executing, ensure:

1. **All other Relayer nodes are stopped**: Only the currently connected node should be running during rollback to prevent data inconsistency
2. **Database is backed up**: Back up the database before executing
3. **Parameters are correct**: Double-check targetBatchIndex, targetBlockHeight, and l1MsgNonceThreshold values
4. **Impact scope is understood**: Rollback deletes significant data — make sure you understand the impact

**Use Cases:**
- Subchain reorganization (Reorg) — need to rollback Relayer data to re-sync
- Test environment needs to reset to a historical state
- Data anomaly requires rollback to a correct state point

---

## Gas Price Configuration Commands

These commands manage L1 Gas price configuration.

### Query Gas Configuration

```bash
# Query Gas price increase percentage
get-eth-gas-price-increased-percentage

# Query max Gas price limit
get-eth-max-price-limit

# Query priority fee increase percentage
get-eth-priority-fee-per-gas-increased-percentage

# Query minimum EIP-1559 priority price
get-eth-minimum-eip1559priority-price

# Query Base Fee multiplier
get-eth-base-fee-multiplier

# Query EIP-4844 priority fee increase percentage
get-eth-eip4844priority-fee-per-gas-increased-percentage

# Query minimum EIP-4844 priority price
get-eth-minimum-eip4844priority-price

# Query Blob Gas multiplier configs
get-eth-larger-fee-per-blob-gas-multiplier
get-eth-smaller-fee-per-blob-gas-multiplier

# Query Blob Gas dividing value
get-eth-fee-per-blob-gas-dividing-val

# Query extra Gas price
get-eth-extra-gas-price
```

### Update Gas Configuration

All update commands use the `--value` parameter:

```bash
# Update Gas price increase percentage (Double)
update-eth-gas-price-increased-percentage --value <Double>

# Update max Gas price limit (String, in Wei)
update-eth-max-price-limit --value <String>

# Update priority fee increase percentage (Double)
update-eth-priority-fee-per-gas-increased-percentage --value <Double>

# Update minimum EIP-1559 priority price (String, in Wei)
update-eth-minimum-eip1559priority-price --value <String>

# Update Base Fee multiplier (String)
update-eth-base-fee-multiplier --value <String>

# Update EIP-4844 priority fee increase percentage (Double)
update-eth-eip4844priority-fee-per-gas-increased-percentage --value <Double>

# Update minimum EIP-4844 priority price (String, in Wei)
update-eth-minimum-eip4844priority-price --value <String>

# Update Blob Gas multiplier configs (String)
update-eth-larger-fee-per-blob-gas-multiplier --value <String>
update-eth-smaller-fee-per-blob-gas-multiplier --value <String>

# Update Blob Gas dividing value (String, in Wei)
update-eth-fee-per-blob-gas-dividing-val --value <String>

# Update extra Gas price (String, in Wei)
update-eth-extra-gas-price --value <String>
```

**Use Cases:**
- Adjust Gas price strategy to handle network congestion
- Optimize transaction costs
- Respond to Ethereum network conditions

---

## Rollup Economic Strategy Commands

These commands manage the Rollup system's economic constraint parameters.

### Query Economic Strategy

```bash
# Query max pending Batch count
get-max-pending-batch-count

# Query max pending Proof count
get-max-pending-proof-count

# Query max Batch waiting time (seconds)
get-max-batch-waiting-time

# Query max Proof waiting time (seconds)
get-max-proof-waiting-time

# Query mid EIP-1559 price limit (Wei)
get-mid-eip1559price-limit

# Query high EIP-1559 price limit (Wei)
get-high-eip1559price-limit
```

### Update Economic Strategy

All update commands use the `--value` parameter:

```bash
# Update max pending Batch count (Integer)
update-max-pending-batch-count --value <Integer>

# Update max pending Proof count (Integer)
update-max-pending-proof-count --value <Integer>

# Update max Batch waiting time (Long, in seconds)
update-max-batch-waiting-time --value <Long>

# Update max Proof waiting time (Long, in seconds)
update-max-proof-waiting-time --value <Long>

# Update mid EIP-1559 price limit (String, in Wei)
update-mid-eip1559price-limit --value <String>

# Update high EIP-1559 price limit (String, in Wei)
update-high-eip1559price-limit --value <String>
```

**Parameter Reference:**

| Parameter | Description |
|-----------|-------------|
| `maxPendingBatchCount` | Max concurrent pending Batches — exceeding this pauses new Batch submissions |
| `maxPendingProofCount` | Max concurrent pending Proofs — exceeding this pauses proof submissions |
| `maxBatchWaitingTime` | Max Batch wait time before forced submission |
| `maxProofWaitingTime` | Max Proof wait time before forced submission |
| `midEip1559PriceLimit` | Mid Gas price boundary (Yellow zone lower bound) |
| `highEip1559PriceLimit` | High Gas price boundary (Red zone lower bound) |

**Examples:**
```bash
# Allow up to 10 pending Batches
update-max-pending-batch-count --value 10

# Set max Batch waiting time to 1 hour (3600 seconds)
update-max-batch-waiting-time --value 3600
```

**Use Cases:**
- Optimize system throughput
- Control Gas costs
- Handle network congestion
- Balance confirmation speed and costs

---

## Utility Commands

### convert-private-key-to-pem

**Purpose:** Convert a hex-encoded private key to PEM format (X509).

**Syntax:**
```bash
convert-private-key-to-pem --privateKey <String>
```

**Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `--privateKey` | String | Yes | Hex-encoded private key (0x-prefixed) |

**Examples:**
```bash
convert-private-key-to-pem --privateKey 0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef
```

---

### convert-public-key-to-eth-address

**Purpose:** Convert a PEM-encoded public key to an Ethereum address.

**Syntax:**
```bash
convert-public-key-to-eth-address --publicKey <String>
```

**Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `--publicKey` | String | Yes | PEM-encoded public key |

---

## Common Scenarios

### Scenario 1: Resolving a Stuck Nonce

When an account Nonce is stuck and subsequent transactions cannot be sent:

```bash
# 1. Query current Relayer addresses
query-relayer-address

# 2. Burn the stuck Nonce
waste-eth-account-nonce --chainType L1 --address <relayer-address> --nonce <stuck-nonce>

# 3. Verify the Nonce is unblocked
# (verify via external tools or by retrying the transaction)
```

---

### Scenario 2: Manually Accelerating Batch Submission

When Batch submission is slow and needs manual acceleration:

```bash
# 1. Query the pending Batch
get-batch --batchIndex 100

# 2. Manually submit the Batch
commit-batch-manually --batchIndex 100

# 3. Check submission status
query-batch-tx-info --type BATCH_COMMIT_TX --batchIndex 100
```

---

### Scenario 3: Completing Proof Submission

When proofs are generated and need to be submitted to L1:

```bash
# 1. Check proof status
query-batch-tx-info --type BATCH_TEE_PROOF_COMMIT_TX --batchIndex 50

# 2. Submit TEE proof
commit-proof-manually --batchIndex 50 --proofType TEE_PROOF

# 3. If ZK proof is also needed
commit-proof-manually --batchIndex 50 --proofType ZK_PROOF
```

---

### Scenario 4: Batch Recovery of Failed Transactions

When multiple Batch transactions have failed and need batch retry:

```bash
# 1. Retry commit transactions for Batches 1-50
retry-batch-tx --type BATCH_COMMIT_TX --fromBatchIndex 1 --toBatchIndex 50

# 2. Retry TEE proof commits for Batches 1-50
retry-batch-tx --type BATCH_TEE_PROOF_COMMIT_TX --fromBatchIndex 1 --toBatchIndex 50
```

---

### Scenario 5: Adjusting Gas Prices During Network Congestion

When network congestion causes slow transactions:

```bash
# 1. Check current Gas config
get-eth-gas-price-increased-percentage

# 2. Increase Gas price (e.g., set to 1.5 for 50% increase)
update-eth-gas-price-increased-percentage --value 1.5

# 3. Speed up a pending transaction
speedup-rollup-tx --batchIndex <batch-index> --txType BATCH_COMMIT_TX --maxPriorityFeePerGas 1000000000
```

---

### Scenario 6: Initializing Relayer

When deploying Relayer in a new environment:

```bash
# 1. Set L1 polling start height
set-l1start-height --startHeight <start-block-height>

# 2. Initialize Anchor Batch
init-anchor-batch --anchorBatchIndex <index> --rawAnchorBatchHeaderHex <hex> --nextL2MsgNonce <nonce> --l2MerkleTreeBranchesHex <branches>

# 3. Verify Relayer address configuration
query-relayer-address
```

---

## Interactive Shell Features

The CLI interactive mode supports these built-in commands:

```bash
# Show command history
history

# Show version info
version

# Execute commands from a file
script <filename>

# Show help
help [command-name]
```

---

## Security Recommendations

1. **Confirm operations**: Mutating commands require user confirmation — check parameters carefully
2. **Back up data**: Back up the database before critical operations
3. **Monitor state**: After critical operations, use query commands to verify results
4. **Access control**: In production, restrict CLI access
5. **Network security**: Run CLI in a secure network environment to prevent sensitive information leaks

---

## Getting Help

Type `help <command>` in the CLI to get detailed help for any command.

```bash
help waste-eth-account-nonce
help commit-batch-manually
help commit-proof-manually
```
