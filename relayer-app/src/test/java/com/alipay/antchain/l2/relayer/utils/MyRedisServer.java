package com.alipay.antchain.l2.relayer.utils;

import java.io.IOException;

import cn.hutool.core.util.StrUtil;
import cn.hutool.system.SystemUtil;
import redis.embedded.RedisExecProvider;
import redis.embedded.RedisServer;

public class MyRedisServer extends RedisServer {

    public MyRedisServer(RedisExecProvider redisExecProvider, Integer port) throws IOException {
        super(redisExecProvider, port);
    }

    @Override
    protected String redisReadyPattern() {
        if (SystemUtil.getOsInfo().isMac() && StrUtil.equalsAnyIgnoreCase(SystemUtil.getOsInfo().getArch(), "x86_64", "aarch64")) {
            return ".*Ready to accept connections tcp.*";
        }
        return super.redisReadyPattern();
    }
}
