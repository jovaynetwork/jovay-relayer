package com.alipay.antchain.l2.relayer.core.blockchain.bpo;

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.map.MapUtil;
import com.alibaba.fastjson.JSON;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;

@Slf4j
public class EthBlobForkConfig {

    private Map<BigInteger, EthBpoBlobConfig> configs;

    public EthBlobForkConfig(BigInteger l1ChainId, Resource unknownEthNetworkForkBlobConfigFile) {
        initConfigsFromResources(l1ChainId, unknownEthNetworkForkBlobConfigFile);
    }

    @SneakyThrows
    private void initConfigsFromResources(BigInteger chainId, Resource unknownEthNetworkForkBlobConfigFile) {
        this.configs = new ConcurrentHashMap<>();
        String raw;
        if (chainId.equals(BigInteger.ONE)) {
            log.info("loading Ethereum blob config: mainnet");
            raw = ResourceUtil.readStr("bpo/mainnet.json", Charset.defaultCharset());
        } else if (chainId.equals(BigInteger.valueOf(11155111))) {
            log.info("loading Ethereum blob config: sepolia testnet");
            raw = ResourceUtil.readStr("bpo/sepolia.json", Charset.defaultCharset());
        } else {
            log.info("loading Ethereum blob config: unknown net");
            raw = unknownEthNetworkForkBlobConfigFile.getContentAsString(Charset.defaultCharset());
        }
        JSON.parseObject(raw).getInnerMap().forEach((k, v) -> {
            log.info("loading Ethereum blob config entry: ({} => {})", k, v);
            this.configs.put(new BigInteger(k), JSON.parseObject(v.toString(), EthBpoBlobConfig.class));
        });
        this.configs = MapUtil.sort(configs, Comparator.reverseOrder());
    }

    public EthBpoBlobConfig getCurrConfig(long currTimestamp) {
        for (var entry : configs.entrySet()) {
            if (entry.getKey().longValue() <= currTimestamp) {
                return entry.getValue();
            }
        }
        throw new RuntimeException("No BPO config found");
    }

    public EthBpoBlobConfig getCurrConfig() {
        return getCurrConfig(System.currentTimeMillis());
    }
}
