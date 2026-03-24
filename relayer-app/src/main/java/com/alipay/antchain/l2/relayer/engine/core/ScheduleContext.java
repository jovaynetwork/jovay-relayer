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

package com.alipay.antchain.l2.relayer.engine.core;

import java.net.InetAddress;
import java.net.SocketException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import cn.hutool.core.net.NetUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 定时任务框架上下文
 */
@Getter
@Slf4j
public class ScheduleContext {

    public final static String NODE_ID_MODE_IP = "IP";

    public final static String NODE_ID_MODE_UUID = "UUID";

    private final String nodeIp;

    private final String nodeId;

    public ScheduleContext(String mode) {
        Set<InetAddress> inetAddresses = NetUtil.localAddressList(
                networkInterface -> {
                    try {
                        return !networkInterface.isLoopback()
                               && !StrUtil.containsAny(networkInterface.getName(), "docker")
                               && !StrUtil.startWith(networkInterface.getName(), "br-")
                               && !StrUtil.startWith(networkInterface.getName(), "bridge");
                    } catch (SocketException e) {
                        throw new RuntimeException(e);
                    }
                },
                inetAddress -> !inetAddress.isLoopbackAddress() && inetAddress.getAddress().length == 4
        );
        InetAddress localAddress;
        if (ObjectUtil.isEmpty(inetAddresses)) {
            log.warn("none inet addresses satisfy the requirements and just use one of the localhost");
            localAddress = NetUtil.getLocalhost();
        } else {
            log.debug("all inet addresses satisfied is [ {} ]",
                    inetAddresses.stream().map(InetAddress::getHostAddress).collect(Collectors.joining(",")));
            localAddress = inetAddresses.iterator().next();
        }

        if (ObjectUtil.isNull(localAddress)) {
            throw new RuntimeException("null local ip");
        }
        this.nodeIp = localAddress.getHostAddress();

        if (StrUtil.equalsIgnoreCase(mode, NODE_ID_MODE_IP)) {
            this.nodeId = this.nodeIp;
        } else {
            this.nodeId = UUID.randomUUID().toString();
        }

        log.info("relayer node id for distribute tasks is {}", nodeId);
    }
}
