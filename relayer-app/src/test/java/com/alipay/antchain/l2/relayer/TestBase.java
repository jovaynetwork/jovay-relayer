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

package com.alipay.antchain.l2.relayer;

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.HexUtil;
import com.alibaba.fastjson.JSON;
import com.alipay.antchain.l2.relayer.commons.l2basic.Batch;
import com.alipay.antchain.l2.relayer.commons.l2basic.BatchHeader;
import com.alipay.antchain.l2.relayer.commons.l2basic.BatchVersionEnum;
import com.alipay.antchain.l2.relayer.commons.l2basic.ChunksPayload;
import com.alipay.antchain.l2.relayer.commons.models.BatchWrapper;
import com.alipay.antchain.l2.relayer.commons.specs.IRollupSpecs;
import com.alipay.antchain.l2.relayer.commons.specs.RollupSpecs;
import com.alipay.antchain.l2.relayer.utils.MyRedisServer;
import com.alipay.antchain.l2.trace.BasicBlockTrace;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.web3j.abi.datatypes.generated.Bytes32;
import redis.embedded.RedisExecProvider;
import redis.embedded.util.Architecture;
import redis.embedded.util.OS;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@Slf4j
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = L2RelayerApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {"l2-relayer.l1-client.eth-network-fork.unknown-network-config-file=bpo/unknown.json"}
)
@DirtiesContext
public abstract class TestBase {

    public static MyRedisServer redisServer;

    // height one
    public static final BasicBlockTrace BASIC_BLOCK_TRACE1;

    public static final BasicBlockTrace BASIC_BLOCK_TRACE_11022;

    public static final BasicBlockTrace BASIC_BLOCK_TRACE_11021;

    static {
        try {
            BASIC_BLOCK_TRACE1 = BasicBlockTrace.parseFrom(HexUtil.decodeHex(FileUtil.readString("./data/block_traces/block#1", Charset.defaultCharset())));
            BASIC_BLOCK_TRACE_11022 = BasicBlockTrace.parseFrom(HexUtil.decodeHex(FileUtil.readString("./data/block_traces/block#11022", Charset.defaultCharset())));
            BASIC_BLOCK_TRACE_11021 = BasicBlockTrace.parseFrom(HexUtil.decodeHex(FileUtil.readString("./data/block_traces/block#11021", Charset.defaultCharset())));
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
    }

    // height two
    public static final BasicBlockTrace BASIC_BLOCK_TRACE2;

    static {
        try {
            BASIC_BLOCK_TRACE2 = BasicBlockTrace.parseFrom(HexUtil.decodeHex(FileUtil.readString("./data/block_traces/block#2", Charset.defaultCharset())));
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
    }

    public static final String RAW_ZERO_BATCH_HEADER = "00000000000000000000000000000000000000000000000000bac4320768bc80b363e3d087c8decdd621f65f9c335e4603bc63525ed57aaa7c000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000";

    public static final BatchHeader ZERO_BATCH_HEADER = BatchHeader.deserializeFrom(HexUtil.decodeHex(RAW_ZERO_BATCH_HEADER));

    public static final BatchWrapper ZERO_BATCH_WRAPPER;

    static {
        ZERO_BATCH_WRAPPER = new BatchWrapper();
        ZERO_BATCH_WRAPPER.setBatch(
                Batch.builder()
                        .batchHeader(ZERO_BATCH_HEADER)
                        .payload(new ChunksPayload(BatchVersionEnum.BATCH_V0, new ArrayList<>()))
                        .build()
        );
        ZERO_BATCH_WRAPPER.setPostStateRoot(Bytes32.DEFAULT.getValue());
        ZERO_BATCH_WRAPPER.setL2MsgRoot(Bytes32.DEFAULT.getValue());
        ZERO_BATCH_WRAPPER.setStartBlockNumber(BigInteger.valueOf(0));
        ZERO_BATCH_WRAPPER.setEndBlockNumber(BigInteger.valueOf(0));
    }

    public static final RollupSpecs ROLLUP_SPECS;

    static {
        ROLLUP_SPECS = JSON.parseObject(FileUtil.readBytes("specs/mainnet.json"), RollupSpecs.class);
    }

    @BeforeClass
    public static void beforeTest() throws Exception {

        // if the embedded redis can't start correctly,
        // try to use local redis server binary to start it.
        redisServer = new MyRedisServer(
                RedisExecProvider.defaultProvider()
                        .override(OS.MAC_OS_X, Architecture.x86_64, "src/test/resources/bins/redis-server")
                        .override(OS.MAC_OS_X, Architecture.x86, "src/test/resources/bins/redis-server"),
                16379
        );
        redisServer.start();
    }

    @AfterClass
    public static void after() throws Exception {
        redisServer.stop();
        Path dumpFile = Paths.get("src/test/resources/bins/dump.rdb");
        if (Files.exists(dumpFile)) {
            Files.delete(dumpFile);
            System.out.println("try to delete redis dump file");
        }
    }

    @MockitoBean
    @Getter
    public IRollupSpecs rollupSpecs;

    @Before
    public void initRollupSpecs() {
        when(rollupSpecs.getNetwork()).thenReturn(ROLLUP_SPECS.getNetwork());
        when(rollupSpecs.getFork(anyLong())).then(invocationOnMock -> {
            var curr = (long) invocationOnMock.getArguments()[0];
            return ROLLUP_SPECS.getFork(curr == 0 ? 1 : curr);
        });
    }
}
