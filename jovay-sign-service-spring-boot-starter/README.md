<div align="center">
  <img alt="am logo" src="https://mdn.alipayobjects.com/huamei_hsbbrh/afts/img/A*HhJlTLtDKywAAAAAQVAAAAgAeiOMAQ/original" width="250" >
  <h1 align="center">Jovay Sign Service</h1>
  <p align="center">
    <a href="http://makeapullrequest.com">
      <img alt="pull requests welcome badge" src="https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=flat">
    </a>
    <a href="https://www.java.com">
      <img alt="Language" src="https://img.shields.io/badge/Language-Java-blue.svg?style=flat">
    </a>
  </p>
</div>

# 使用方法

## 1.Maven依赖

在pom.xml中添加下面依赖，版本按需选择。

```xml
<dependency>
    <groupId>com.alipay.antchain.l2</groupId>
    <artifactId>jovay-sign-service-spring-boot-starter</artifactId>
    <version>${your_version}</version>
</dependency>
```

## 2.创建TxSignService

在代码中，通过注解`@JovayTxSignService("your_service")`标注成员变量，可以创建TxSignService实例并注入给成员变量。

```java
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

//or @Component or @Configuration, it has to be a bean in spring context.
@Service
class YourClass {
    @JovayTxSignService("your_service")
    private TxSignService yourService;
}
```

- 成员变量归属的类必须要作为Spring的Bean；
- 注解`@JovayTxSignService`的value必须要填入确定的值，含义为你的service的名字，需要和配置对应；
- 配置properties中，需要按照格式填入你的service的配置；

也可以注解在方法上，创建TxSignService：

```java
import com.alipay.antchain.l2.relayer.signservice.inject.JovayTxSignService;
import org.springframework.stereotype.Service;
import org.web3j.service.TxSignService;

//or @Component or @Configuration, it has to be a bean in spring context.
@Service
class YourClass {
    @JovayTxSignService("your_service")
    private void setYourService(TxSignService yourService) {
//        yourService.sign();
    }
}
```

## 3.详细配置

在你的springboot应用配置文件中，添加如下配置。

```yaml
jovay:
  sign-service:
    # 你注解里面的service name
    your_service:
      type: web3j_native
      web3j-native:
        private-key: 7ff1a4c1d57e...f5ef924856
      kms:
        endpoint: ${L1_CLIENT_LEGACY_POOL_KMS_ENDPOINT:}
        access-key-id: ${L1_CLIENT_LEGACY_POOL_KMS_ACCESS_KEY_ID:}
        access-key-secret: ${L1_CLIENT_LEGACY_POOL_KMS_ACCESS_KEY_SECRET:}
        private-key-id: ${L1_CLIENT_LEGACY_POOL_KMS_PRIVATE_KEY_ID:}
        private-key-version-id: ${L1_CLIENT_LEGACY_POOL_KMS_PRIVATE_KEY_VERSION_ID:}
        public-key: ${L1_CLIENT_LEGACY_POOL_KMS_PUBLIC_KEY:}
        ca: |
          ${L1_CLIENT_LEGACY_POOL_KMS_CA:}
```

- jovay.sign-service.your_service.type：your_service实例对应的类型，支持web3j_native和aliyun_kms两种类型
- 如果类型是web3j_native
  - jovay.sign-service.your_service.web3j-native.private-key: hex格式的私钥；
- 如果类型是aliyun_kms
  - jovay.sign-service.your_service.kms.endpoint：阿里云KMS服务的endpoint；
  - jovay.sign-service.your_service.kms.access-key-id：阿里云KMS服务的access-key-id；
  - jovay.sign-service.your_service.kms.access-key-secret：阿里云KMS服务的access-key-secret；
  - jovay.sign-service.your_service.kms.private-key-id：阿里云KMS服务的private-key-id；
  - jovay.sign-service.your_service.kms.private-key-version-id：阿里云KMS服务的private-key的version id，如果不配置会尝试自动获取，但是kms服务未必支持该接口；
  - jovay.sign-service.your_service.kms.public-key：阿里云KMS服务的public-key，这里可以不用填，会自动从阿里云KMS服务获取；
  - jovay.sign-service.your_service.kms.ca：阿里云KMS服务server的TLS证书，这里一般从网页下载即可；

## 4.用于Web3j

```java
import com.alipay.antchain.l2.relayer.signservice.inject.JovayTxSignService;
import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.gas.DynamicEIP1559GasProvider;

//or @Component or @Configuration, it has to be a bean in spring context.
@Service
class YourClass {
    @JovayTxSignService("your_service")
    private TxSignService yourService;

    public void test() {
        var web3j = Web3j.build(new HttpService("http://localhost:8545"));
        var chainId = web3j.ethChainId().send().getChainId().longValue();
        // Rollup is the contract abi class
        var rollupContract = Rollup.deploy(
                web3j,
                new RawTransactionManager(web3j, yourService, chainId),
                new DynamicEIP1559GasProvider(web3j, chainId)
        ).send();
    }
}


```