/*
 * Copyright 2026 Ant Group
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alipay.antchain.l2.relayer.config;

import java.nio.charset.StandardCharsets;

import cn.hutool.core.io.FileUtil;
import com.alipay.antchain.l2.relayer.core.blockchain.ContractErrorParserImpl;
import com.alipay.antchain.l2.relayer.core.blockchain.IContractErrorParser;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.ResourcePatternResolver;

@Slf4j
@Configuration
public class ContractConfig {

    @Resource
    private ResourcePatternResolver resourcePatternResolver;

    @Bean
    @SneakyThrows
    public IContractErrorParser contractErrorParser() {
        var parser = new ContractErrorParserImpl();
        for (var resource : resourcePatternResolver.getResources("classpath:abi/level-*/*.json")) {
            var contractName = FileUtil.mainName(resource.getFilename());
            parser.addContractAbi(contractName, resource.getContentAsString(StandardCharsets.UTF_8));
            log.debug("load contract abi to abi parser: {}", contractName);
        }
        log.info("️💬 contract abi parser loaded");
        return parser;
    }
}
