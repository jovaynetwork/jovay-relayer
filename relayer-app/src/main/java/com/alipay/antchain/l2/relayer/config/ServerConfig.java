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

import java.net.InetAddress;
import java.net.InetSocketAddress;

import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.l2.relayer.service.AdminGrpcService;
import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class ServerConfig {

    @Value("${l2-relayer.server.admin.host:localhost}")
    private String adminHost;

    @Value("${l2-relayer.server.admin.port:7088}")
    private int adminPort;

    @Bean
    @SneakyThrows
    public Server adminGrpcServer(@Autowired AdminGrpcService adminService) {
        log.info("Starting managing server on port {}", adminPort);
        return NettyServerBuilder.forAddress(
                        new InetSocketAddress(
                                StrUtil.isEmpty(adminHost) ? InetAddress.getLoopbackAddress() : InetAddress.getByName(adminHost),
                                adminPort
                        )
                ).addService(adminService)
                .build()
                .start();
    }
}
