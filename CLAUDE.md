# CLAUDE.md — Project Guide for AI Agents

This file provides the essential context an AI coding agent needs to work effectively in the L2-Relayer codebase.

## What Is This Project?

**Jovay Relayer** is the core middleware for the Jovay L2 Rollup system. It sits between Layer 1 (Ethereum) and Layer 2 (Jovay), driving the full Rollup lifecycle:

1. Polls L2 blocks from the Tracer Service
2. Aggregates blocks into Chunks and Batches
3. Submits Batches to L1 via EIP-4844 Blob transactions
4. Coordinates with Prover Controller to obtain TEE/ZK proofs
5. Commits proofs to L1 via EIP-1559 transactions
6. Relays cross-chain messages (L1 ↔ L2)

## Tech Stack

- **Language**: Java 21
- **Framework**: Spring Boot 3.5
- **Build**: Maven (multi-module)
- **Database**: MySQL 8.0+ (MyBatis-Plus ORM, Flyway migrations)
- **Cache/Lock**: Redis (distributed locking, nonce cache)
- **Blockchain**: Web3j (Ethereum JSON-RPC client)
- **gRPC**: Prover Controller communication
- **CLI**: Spring Shell (Admin CLI)

## Module Structure

```
L2-Relayer/
├── relayer-commons/     # Shared: models, enums, ABI wrappers, RollupSpecs, utilities
├── relayer-dal/         # Data access: MyBatis-Plus entities & mappers
├── relayer-app/         # Core application (main Relayer process)
├── query-service/       # REST API service for external queries
├── admin-cli/           # Spring Shell CLI for runtime operations
├── jovay-sign-service-spring-boot-starter/  # Tx signing (Web3j / KMS)
└── docker/              # Dockerfiles & compose.yaml
```

## Package Layout (relayer-app)

```
com.alipay.antchain.l2.relayer
├── config/          # Spring config classes (RollupConfig, ParentChainConfig, etc.)
├── engine/          # Distributed task scheduling
│   ├── core/        #   Dispatcher (leader election), Duty (task execution), ScheduleContext
│   ├── checker/     #   IDistributedTaskChecker implementations
│   └── executor/    #   Task executors (BlockPolling, BatchCommit, ProofCommit, etc.)
├── service/         # Business services
│   ├── IRollupService / RollupServiceImpl          # Rollup pipeline
│   ├── IReliableTxService / ReliableTxServiceImpl  # Transaction lifecycle
│   ├── IOracleService / OracleServiceImpl          # Gas oracle
│   └── IMailboxService / MailboxServiceImpl         # Cross-chain messaging
├── core/            # Domain logic
│   ├── layer2/      #   RollupAggregator, economic strategy, GrowingBatchChunksMemCache
│   ├── blockchain/  #   L1Client, L2Client, NonceManager, TxManager, GasPriceProvider
│   └── prover/      #   ProverControllerClient (gRPC)
├── dal/repository/  # Repository interfaces & implementations
├── metrics/         # OpenTelemetry metrics
└── utils/           # Utility classes (ConvertUtil, etc.)
```

## Key Classes to Know

| Class | Where | What It Does |
|-------|-------|-------------|
| `RollupAggregator` | `core/layer2/` | Block → Chunk → Batch pipeline, compression, DA version |
| `L1Client` | `core/blockchain/` | Builds & sends EIP-4844/1559 txs, queries Rollup contract |
| `ReliableTxServiceImpl` | `service/` | Tx lifecycle: confirmation, speed-up, resend, retry |
| `Dispatcher` | `engine/core/` | Leader election (Redis lock), round-robin task assignment |
| `Duty` | `engine/core/` | Polls task table, dispatches to executor thread pools |
| `ProverControllerClient` | `core/prover/` | gRPC client for proof request/retrieval |
| `RollupSpecs` | `relayer-commons` | Fork-based protocol versioning (Batch versions, DA versions) |
| `BatchHeader` | `relayer-commons` | 105-byte fixed header structure |
| `Batch` / `Chunk` / `BlockContext` | `relayer-commons` | Core data structures for Rollup |
| `BatchVersionEnum` | `relayer-commons` | BATCH_V0 / V1 / V2 definitions |
| `DaVersion` | `relayer-commons` | DA_0 / DA_1 / DA_2 encoding formats |
| `BlobsDaData` | `relayer-commons` | EIP-4844 Blob encoding logic |
| `RollupEconomicStrategy` | `core/layer2/economic/` | Green/Yellow/Red zone gas price strategy |

## Data Model Summary

- **Batch**: Top-level L1 submission unit. Contains BatchHeader (105 bytes) + Chunks + DA data.
- **Chunk**: Group of consecutive L2 blocks + their transactions.
- **BlockContext**: 40-byte per-block metadata (specVersion, blockNumber, timestamp, baseFee, gasLimit, numTx, numL1Msg).
- **ReliableTransaction**: Tracks every L1/L2 tx through states: TX_PENDING → TX_PACKAGED → TX_SUCCESS / TX_FAILED / BIZ_SUCCESS.

## Database

- ORM: **MyBatis-Plus** (entities in `relayer-dal/.../entities/`)
- Migrations: **Flyway** (SQL files in `relayer-app/src/main/resources/db/migration/`)
- Key tables: `batches`, `chunks`, `reliable_transaction`, `inter_bc_msg`, `rollup_number_record`, `active_node`, `distributed_task`, `oracle_request`, `batch_prove_request`, `l2_merkle_tree`

## Build & Run

```bash
# Build (requires JDK 21+, Maven 3.8+)
mvn clean package -DskipTests

# Run via Docker Compose (recommended)
docker build -f docker/Dockerfile-Relayer -t jovay-relayer .
docker build -f docker/Dockerfile-QS -t jovay-query-service .
cd docker && docker compose up -d

# Run standalone
cd relayer-app/target && tar -xzf l2-relayer.tar.gz && cd l2-relayer && ./bin/start.sh
```

## Configuration

- **Docker Compose**: Environment variables in `docker/compose.yaml`
- **Standalone**: `relayer-app/src/main/resources/application-prod.yml`
- **Dynamic (runtime)**: Via Admin CLI commands (gas price, economic strategy, nonce, etc.)

Key config areas:
- Chain connections (L1/L2 RPC URLs)
- Contract addresses (Rollup, Mailbox, L1GasOracle)
- Private keys (Blob key, Legacy key, L2 key) — or KMS config
- Prover Controller gRPC endpoint
- Gas price strategy parameters
- Economic strategy thresholds

## Testing

```bash
# Unit tests
mvn test

# Single module
mvn test -pl relayer-commons
mvn test -pl relayer-app
```

## Documentation Index

| Document | Path | Description |
|----------|------|-------------|
| README (EN) | `README.md` | Project overview, architecture, features |
| README (CN) | `README_CN.md` | Chinese version |
| Configuration Guide (EN) | `.doc/configuration-guide.md` | Full deployment & config reference |
| Configuration Guide (CN) | `.doc/configuration-guide_CN.md` | Chinese version |
| Batch Data Structure (EN) | `.doc/batch-data-structure.md` | Batch/Chunk field layouts, versions, DA encoding |
| Batch Data Structure (CN) | `.doc/batch-data-structure_CN.md` | Chinese version |
| Reliable Transaction (EN) | `.doc/reliable-transaction.md` | Tx lifecycle, speed-up, gas strategy |
| Reliable Transaction (CN) | `.doc/reliable-transaction_CN.md` | Chinese version |
| Admin CLI Guide (EN) | `admin-cli/README.md` | CLI command reference |
| Admin CLI Guide (CN) | `admin-cli/README_CN.md` | Chinese version |

## Code Conventions

- Java source uses standard Spring Boot conventions
- Package base: `com.alipay.antchain.l2.relayer`
- Entity classes: `*Entity.java` in `relayer-dal`
- Domain models: `*DO.java` or plain classes in `relayer-commons`
- Service pattern: interface `I*Service` + implementation `*ServiceImpl`
- Task executors extend `BaseScheduleTaskExecutor`
- Config classes annotated with `@ConfigurationProperties`
- All documents maintain English and Chinese versions (EN as primary)

## Common Tasks for Agents

### Adding a new Admin CLI command
1. Create a new method in the appropriate `*Commands.java` class under `admin-cli/src/.../commands/`
2. Annotate with `@ShellMethod` and `@ShellOption` for parameters
3. Update both `admin-cli/README.md` (EN) and `admin-cli/README_CN.md` (CN)

### Adding a new distributed task
1. Create executor class extending `BaseScheduleTaskExecutor` in `relayer-app/.../engine/executor/`
2. Register the task type in `DistributedTaskTypeEnum`
3. Add checker if needed in `engine/checker/`

### Adding a new transaction type
1. Add enum value to `TransactionTypeEnum` in `relayer-commons`
2. Handle the new type in `ReliableTxServiceImpl`
3. Add cost checker if economic strategy applies

### Database schema changes
1. Create a new Flyway migration SQL file: `V{version}__{description}.sql` in `relayer-app/src/main/resources/db/migration/`
2. Update the corresponding entity in `relayer-dal/.../entities/`
3. Update repository methods if needed

### Modifying gas price logic
- Gas price providers: `relayer-app/.../blockchain/helper/EthereumGasPriceProvider.java`, `ApiGasPriceProvider.java`
- Speed-up logic: `L1Client.java` (`isOkToSpeedUpEip1559Tx`, `calcSpeedUpMaxBlobFeePerGas`)
- Economic strategy: `relayer-app/.../layer2/economic/` (cost checkers + strategy config)

## Important Notes

- Relayer uses **two separate signing keys** for L1: Blob Key (EIP-4844, Batch commits) and Legacy Key (EIP-1559, Proof commits). They must be different addresses.
- Nonce management has two modes: NORMAL (query chain each time) and FAST (Redis-cached, higher throughput).
- The economic strategy (Green/Yellow/Red zones) can be toggled off via config.
- Rollup Specs defines protocol forks by Batch index — similar to Ethereum hard forks.
- Batch versions (V0/V1/V2) control chunk codec and compression; DA versions (DA_0/DA_1/DA_2) control blob encoding.
- The `BLOCK_CONTEXT_SIZE` is 40 bytes; `BatchHeader` is 105 bytes (version:1 + batchIndex:8 + l1MsgRollingHash:32 + dataHash:32 + parentBatchHash:32).
