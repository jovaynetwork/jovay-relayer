# Jovay Sign Service Spring Boot Starter

> [中文文档](README_CN.md)

A Spring Boot starter that provides automatic injection of `TxSignService` instances via the `@JovayTxSignService` annotation. Supports both **Web3j native signing** (hex private key) and **Alibaba Cloud KMS** signing out of the box.

## Quick Start

### 1. Maven Dependency

```xml
<dependency>
    <groupId>com.alipay.antchain.l2</groupId>
    <artifactId>jovay-sign-service-spring-boot-starter</artifactId>
    <version>${your_version}</version>
</dependency>
```

### 2. Configuration

Add the signing service configuration to your `application.yml`. Each service is identified by a unique name under `jovay.sign-service`:

```yaml
jovay:
  sign-service:
    myService:
      type: web3j_native          # or aliyun_kms
      web3j-native:
        private-key: 7ff1a4c1d57e...f5ef924856
```

### 3. Inject via Annotation

Annotate a field in any Spring-managed bean with `@JovayTxSignService`:

```java
@Service
public class MyBlockchainService {

    @JovayTxSignService("myService")
    private TxSignService txSignService;

    public void sendTransaction() {
        var web3j = Web3j.build(new HttpService("http://localhost:8545"));
        var chainId = web3j.ethChainId().send().getChainId().longValue();
        var txManager = new RawTransactionManager(web3j, txSignService, chainId);
        // use txManager to deploy contracts, send transactions, etc.
    }
}
```

---

## `@JovayTxSignService` Annotation Reference

The annotation can be applied to **fields**, **methods**, or **parameters** in any Spring bean.

### Attributes

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| `value` | `String` | `""` | Service name — must match a key under `jovay.sign-service` in your configuration |
| `conditionalProperty` | `String[]` | `{}` | Spring property names that must match for injection to occur. If empty, always inject. Multiple properties use AND logic. |
| `conditionalPropertyHavingValue` | `String` | `""` | Expected value of the conditional property. If empty, any non-null value satisfies the condition. |
| `conditionalPropertyMatchIfMissing` | `boolean` | `false` | If `true`, inject even when the conditional property is missing from the environment |

### Basic Field Injection

The simplest usage — always inject the named signing service:

```java
@Service
public class MyService {

    @JovayTxSignService("mySignService")
    private TxSignService txSignService;
}
```

### Method Injection

The annotation can also be placed on a setter method. The method must accept exactly one `TxSignService` parameter:

```java
@Service
public class MyService {

    @JovayTxSignService("mySignService")
    private void setTxSignService(TxSignService txSignService) {
        // custom initialization logic
    }
}
```

### Conditional Injection

Injection can be made conditional on Spring Environment properties, similar to `@ConditionalOnProperty`. This is useful when a signing service is only needed under certain configurations.

**Example**: Only inject when `l2-relayer.rollup.da-type` is `BLOBS`:

```java
@JovayTxSignService(
    value = "l1BlobPoolTxSignService",
    conditionalProperty = "l2-relayer.rollup.da-type",
    conditionalPropertyHavingValue = "BLOBS"
)
private TxSignService blobPoolTxSignService;
```

In this case, `blobPoolTxSignService` will be `null` if the property is not `BLOBS`.

**Example**: Inject by default, skip only when the property explicitly has a different value:

```java
@JovayTxSignService(
    value = "l1BlobPoolTxSignService",
    conditionalProperty = "l2-relayer.rollup.da-type",
    conditionalPropertyHavingValue = "BLOBS",
    conditionalPropertyMatchIfMissing = true
)
private TxSignService blobPoolTxSignService;
```

Here, the service is injected when:
- `l2-relayer.rollup.da-type` is set to `BLOBS`, **or**
- `l2-relayer.rollup.da-type` is not present at all (missing from config)

It is **not** injected when the property exists but has a different value (e.g., `DAS`).

**Example**: Multiple conditional properties (AND logic):

```java
@JovayTxSignService(
    value = "specialService",
    conditionalProperty = {"feature.enabled", "environment.type"},
    conditionalPropertyHavingValue = "true"
)
private TxSignService specialService;
```

Both `feature.enabled` and `environment.type` must equal `"true"` for injection to occur.

### Real-World Usage in Relayer

The L2-Relayer project defines three signing services:

```java
// L1 Legacy Pool — always injected (for Proof commits via EIP-1559)
@JovayTxSignService("l1LegacyPoolTxSignService")
private TxSignService l1LegacyPoolTxSignService;

// L1 Blob Pool — conditional (for Batch commits via EIP-4844, only when DA type is BLOBS)
@JovayTxSignService(
    value = "l1BlobPoolTxSignService",
    conditionalProperty = "l2-relayer.rollup.da-type",
    conditionalPropertyHavingValue = "BLOBS",
    conditionalPropertyMatchIfMissing = true
)
private TxSignService l1BlobPoolTxSignService;

// L2 — always injected (for L2 transactions)
@JovayTxSignService("l2TxSignService")
private TxSignService l2TxSignService;
```

---

## Signing Service Types

### Web3j Native (`web3j_native`)

Uses a hex-encoded private key directly with Web3j's `Credentials`. Simple and suitable for development or environments where plaintext keys are acceptable.

```yaml
jovay:
  sign-service:
    myService:
      type: web3j_native
      web3j-native:
        private-key: 7ff1a4c1d57e5f1e...f5ef924856
```

| Property | Description |
|----------|-------------|
| `type` | Must be `web3j_native` |
| `web3j-native.private-key` | Hex-encoded private key (without 0x prefix) |

### Alibaba Cloud KMS (`aliyun_kms`)

Uses Alibaba Cloud KMS for secure private key management. The private key never leaves KMS — all signing operations are performed remotely via the KMS API.

```yaml
jovay:
  sign-service:
    myService:
      type: aliyun_kms
      kms:
        endpoint: kst-xxx.cryptoservice.kms.aliyuncs.com
        access-key-id: LTAI5t...
        access-key-secret: Gwv...
        private-key-id: key-xxx
        private-key-version-id: v1     # optional, auto-fetched if not set
        public-key: ""                  # optional, auto-fetched from KMS
        ca: |                           # KMS server TLS certificate
          -----BEGIN CERTIFICATE-----
          ...
          -----END CERTIFICATE-----
```

| Property | Required | Description |
|----------|----------|-------------|
| `type` | Yes | Must be `aliyun_kms` |
| `kms.endpoint` | Yes | KMS service endpoint |
| `kms.access-key-id` | Yes | Alibaba Cloud access key ID |
| `kms.access-key-secret` | Yes | Alibaba Cloud access key secret |
| `kms.private-key-id` | Yes | KMS private key ID |
| `kms.private-key-version-id` | No | Key version ID (auto-fetched if omitted, but not all KMS instances support this) |
| `kms.public-key` | No | Public key in PEM format (auto-fetched from KMS if omitted) |
| `kms.ca` | Yes | TLS certificate for the KMS server (download from Alibaba Cloud console) |

---

## Using with Web3j

Once injected, the `TxSignService` can be used with Web3j's `RawTransactionManager` to sign and send transactions:

```java
@Service
public class ContractService {

    @JovayTxSignService("myService")
    private TxSignService txSignService;

    public void deployContract() {
        var web3j = Web3j.build(new HttpService("http://localhost:8545"));
        var chainId = web3j.ethChainId().send().getChainId().longValue();
        var rollupContract = Rollup.deploy(
            web3j,
            new RawTransactionManager(web3j, txSignService, chainId),
            new DynamicEIP1559GasProvider(web3j, chainId)
        ).send();
    }
}
```

---

## Module Structure

```
jovay-sign-service-spring-boot-starter/
├── src/main/java/.../signservice/
│   ├── autocofigure/
│   │   └── TxSignServiceAutoConfiguration.java   # Spring Boot auto-configuration
│   ├── config/
│   │   ├── TxSignServicesProperties.java          # Root config: jovay.sign-service.*
│   │   ├── TxSignServiceProperties.java           # Per-service config (type, kms, web3j-native)
│   │   ├── NativeConfig.java                      # Web3j native key config
│   │   └── KmsConfig.java                         # Alibaba Cloud KMS config
│   ├── core/
│   │   ├── TxSignServiceFactory.java              # Creates and caches TxSignService instances
│   │   ├── TxSignServiceType.java                 # Enum: WEB3J_NATIVE, ALIYUN_KMS
│   │   ├── Web3jTxSignService.java                # TxSignService impl using Credentials
│   │   └── KmsTxSignService.java                  # TxSignService impl using KMS
│   └── inject/
│       ├── JovayTxSignService.java                # Annotation definition
│       └── TxSignServiceBeanPostProcessor.java    # BeanPostProcessor that handles injection
└── src/main/resources/META-INF/spring/
    └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

## How It Works

1. Spring Boot auto-configures `TxSignServiceAutoConfiguration`, which registers the `TxSignServiceBeanPostProcessor` and `TxSignServiceFactory`
2. The `TxSignServiceBeanPostProcessor` runs during bean initialization and scans all fields and methods for the `@JovayTxSignService` annotation
3. For each annotated field/method, it evaluates conditional properties (if any)
4. If conditions are met, it looks up the named service configuration from `jovay.sign-service.<name>` and creates the appropriate `TxSignService` instance via `TxSignServiceFactory`
5. The factory caches instances — the same service name always returns the same `TxSignService` object
