# Jovay Sign Service Spring Boot Starter

> [English Version](README.md)

一个 Spring Boot Starter，通过 `@JovayTxSignService` 注解自动注入 `TxSignService` 实例。开箱即用地支持 **Web3j 原生签名**（Hex 私钥）和**阿里云 KMS** 签名。

## 快速开始

### 1. Maven 依赖

```xml
<dependency>
    <groupId>com.alipay.antchain.l2</groupId>
    <artifactId>jovay-sign-service-spring-boot-starter</artifactId>
    <version>${your_version}</version>
</dependency>
```

### 2. 配置

在 `application.yml` 中添加签名服务配置。每个服务通过 `jovay.sign-service` 下的唯一名称标识：

```yaml
jovay:
  sign-service:
    myService:
      type: web3j_native          # 或 aliyun_kms
      web3j-native:
        private-key: 7ff1a4c1d57e...f5ef924856
```

### 3. 通过注解注入

在任意 Spring 管理的 Bean 中，使用 `@JovayTxSignService` 注解标注字段：

```java
@Service
public class MyBlockchainService {

    @JovayTxSignService("myService")
    private TxSignService txSignService;

    public void sendTransaction() {
        var web3j = Web3j.build(new HttpService("http://localhost:8545"));
        var chainId = web3j.ethChainId().send().getChainId().longValue();
        var txManager = new RawTransactionManager(web3j, txSignService, chainId);
        // 使用 txManager 部署合约、发送交易等
    }
}
```

---

## `@JovayTxSignService` 注解参考

该注解可以应用于 Spring Bean 中的**字段**、**方法**或**参数**。

### 属性

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `value` | `String` | `""` | 服务名称 — 必须与配置中 `jovay.sign-service` 下的 key 对应 |
| `conditionalProperty` | `String[]` | `{}` | 注入条件所依赖的 Spring 属性名。为空时始终注入。多个属性使用 AND 逻辑。 |
| `conditionalPropertyHavingValue` | `String` | `""` | 条件属性的期望值。为空时，属性存在即满足条件。 |
| `conditionalPropertyMatchIfMissing` | `boolean` | `false` | 若为 `true`，当条件属性不存在时也执行注入 |

### 基本字段注入

最简用法 — 始终注入指定名称的签名服务：

```java
@Service
public class MyService {

    @JovayTxSignService("mySignService")
    private TxSignService txSignService;
}
```

### 方法注入

注解也可以放在 setter 方法上，方法必须接受恰好一个 `TxSignService` 参数：

```java
@Service
public class MyService {

    @JovayTxSignService("mySignService")
    private void setTxSignService(TxSignService txSignService) {
        // 自定义初始化逻辑
    }
}
```

### 条件注入

注入可以基于 Spring Environment 属性进行条件判断，类似于 `@ConditionalOnProperty`。当某个签名服务仅在特定配置下需要时非常有用。

**示例**：仅当 `l2-relayer.rollup.da-type` 为 `BLOBS` 时注入：

```java
@JovayTxSignService(
    value = "l1BlobPoolTxSignService",
    conditionalProperty = "l2-relayer.rollup.da-type",
    conditionalPropertyHavingValue = "BLOBS"
)
private TxSignService blobPoolTxSignService;
```

此时，如果属性值不是 `BLOBS`，`blobPoolTxSignService` 将为 `null`。

**示例**：默认注入，仅当属性明确为其他值时跳过：

```java
@JovayTxSignService(
    value = "l1BlobPoolTxSignService",
    conditionalProperty = "l2-relayer.rollup.da-type",
    conditionalPropertyHavingValue = "BLOBS",
    conditionalPropertyMatchIfMissing = true
)
private TxSignService blobPoolTxSignService;
```

该服务在以下情况下会被注入：
- `l2-relayer.rollup.da-type` 设置为 `BLOBS`，**或**
- `l2-relayer.rollup.da-type` 未出现在配置中（属性缺失）

当属性存在但值不同（如 `DAS`）时，**不会**注入。

**示例**：多条件属性（AND 逻辑）：

```java
@JovayTxSignService(
    value = "specialService",
    conditionalProperty = {"feature.enabled", "environment.type"},
    conditionalPropertyHavingValue = "true"
)
private TxSignService specialService;
```

`feature.enabled` 和 `environment.type` 都必须等于 `"true"` 才会注入。

### Relayer 中的实际用法

L2-Relayer 项目定义了三个签名服务：

```java
// L1 Legacy Pool — 始终注入（用于 EIP-1559 的 Proof 提交）
@JovayTxSignService("l1LegacyPoolTxSignService")
private TxSignService l1LegacyPoolTxSignService;

// L1 Blob Pool — 条件注入（用于 EIP-4844 的 Batch 提交，仅当 DA 类型为 BLOBS 时）
@JovayTxSignService(
    value = "l1BlobPoolTxSignService",
    conditionalProperty = "l2-relayer.rollup.da-type",
    conditionalPropertyHavingValue = "BLOBS",
    conditionalPropertyMatchIfMissing = true
)
private TxSignService l1BlobPoolTxSignService;

// L2 — 始终注入（用于 L2 交易）
@JovayTxSignService("l2TxSignService")
private TxSignService l2TxSignService;
```

---

## 签名服务类型

### Web3j 原生签名（`web3j_native`）

直接使用 Hex 编码的私钥配合 Web3j 的 `Credentials`。简单直接，适合开发环境或允许明文密钥的场景。

```yaml
jovay:
  sign-service:
    myService:
      type: web3j_native
      web3j-native:
        private-key: 7ff1a4c1d57e5f1e...f5ef924856
```

| 属性 | 说明 |
|------|------|
| `type` | 必须为 `web3j_native` |
| `web3j-native.private-key` | Hex 编码的私钥（不带 0x 前缀） |

### 阿里云 KMS 签名（`aliyun_kms`）

使用阿里云 KMS 进行安全的私钥管理。私钥永远不会离开 KMS — 所有签名操作均通过 KMS API 远程执行。

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
        private-key-version-id: v1     # 可选，不配置会自动获取
        public-key: ""                  # 可选，自动从 KMS 获取
        ca: |                           # KMS 服务端 TLS 证书
          -----BEGIN CERTIFICATE-----
          ...
          -----END CERTIFICATE-----
```

| 属性 | 必填 | 说明 |
|------|------|------|
| `type` | 是 | 必须为 `aliyun_kms` |
| `kms.endpoint` | 是 | KMS 服务 endpoint |
| `kms.access-key-id` | 是 | 阿里云 Access Key ID |
| `kms.access-key-secret` | 是 | 阿里云 Access Key Secret |
| `kms.private-key-id` | 是 | KMS 私钥 ID |
| `kms.private-key-version-id` | 否 | 私钥版本 ID（不配置会尝试自动获取，但不是所有 KMS 实例都支持） |
| `kms.public-key` | 否 | PEM 格式的公钥（不填会自动从 KMS 获取） |
| `kms.ca` | 是 | KMS 服务端的 TLS 证书（从阿里云控制台下载） |

---

## 配合 Web3j 使用

注入后，`TxSignService` 可配合 Web3j 的 `RawTransactionManager` 签名和发送交易：

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

## 模块结构

```
jovay-sign-service-spring-boot-starter/
├── src/main/java/.../signservice/
│   ├── autocofigure/
│   │   └── TxSignServiceAutoConfiguration.java   # Spring Boot 自动配置
│   ├── config/
│   │   ├── TxSignServicesProperties.java          # 根配置：jovay.sign-service.*
│   │   ├── TxSignServiceProperties.java           # 单服务配置（type, kms, web3j-native）
│   │   ├── NativeConfig.java                      # Web3j 原生密钥配置
│   │   └── KmsConfig.java                         # 阿里云 KMS 配置
│   ├── core/
│   │   ├── TxSignServiceFactory.java              # 创建并缓存 TxSignService 实例
│   │   ├── TxSignServiceType.java                 # 枚举：WEB3J_NATIVE, ALIYUN_KMS
│   │   ├── Web3jTxSignService.java                # 基于 Credentials 的 TxSignService 实现
│   │   └── KmsTxSignService.java                  # 基于 KMS 的 TxSignService 实现
│   └── inject/
│       ├── JovayTxSignService.java                # 注解定义
│       └── TxSignServiceBeanPostProcessor.java    # 处理注入的 BeanPostProcessor
└── src/main/resources/META-INF/spring/
    └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

## 工作原理

1. Spring Boot 自动配置 `TxSignServiceAutoConfiguration`，注册 `TxSignServiceBeanPostProcessor` 和 `TxSignServiceFactory`
2. `TxSignServiceBeanPostProcessor` 在 Bean 初始化期间运行，扫描所有字段和方法上的 `@JovayTxSignService` 注解
3. 对每个被注解的字段/方法，评估条件属性（如有）
4. 如果条件满足，从 `jovay.sign-service.<name>` 查找对应的服务配置，通过 `TxSignServiceFactory` 创建相应的 `TxSignService` 实例
5. 工厂会缓存实例 — 相同的服务名称始终返回同一个 `TxSignService` 对象
