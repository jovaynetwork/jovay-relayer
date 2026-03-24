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

package com.alipay.antchain.l2.relayer.cli.commands;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import lombok.SneakyThrows;
import org.junit.*;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class DaCommandsTest {

    private DaCommands daCommands;
    private Path tempOutputDir;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.wireMockConfig().dynamicPort().dynamicHttpsPort());

    @Before
    @SneakyThrows
    public void setUp() {
        daCommands = new DaCommands();
        tempOutputDir = Files.createTempDirectory("da-test-");
    }

    @After
    @SneakyThrows
    public void tearDown() {
        // Clean up temp directory
        if (Files.exists(tempOutputDir)) {
            Files.walk(tempOutputDir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (Exception e) {
                            // Ignore
                        }
                    });
        }
    }

    @Test
    @SneakyThrows
    public void testFetchBatchPayloadFromDaSuccess() {
        stubFor(WireMock.get(urlPathTemplate("/eth/v1/beacon/blobs/{block_id}"))
                .willReturn(ok()
                        .withBody(StrUtil.format("""
                                {
                                    "execution_optimistic": false,
                                    "finalized": true,
                                    "data": [
                                        "{}"
                                    ]
                                }""", FileUtil.readString("data/blob", Charset.defaultCharset())))));

        stubFor(WireMock.post(anyUrl())
                .withRequestBody(matchingJsonPath("$.[?(@.method == 'eth_getTransactionReceipt')]"))
                .willReturn(ok().withBody(StrUtil.format(FileUtil.readString("data/batch-commit-tx-http-resp.json", Charset.defaultCharset())))));

        var result = (String) daCommands.fetchBatchPayloadFromDa(
                "http://localhost:" + wireMockRule.port(),
                "http://localhost:" + wireMockRule.port(),
                "0x1234567890abcdef",
                "1000",
                new ArrayList<>(),
                tempOutputDir.toString()
        );

        System.out.println(tempOutputDir.toAbsolutePath());

        Assert.assertNotNull(result);
        Assert.assertTrue(result.contains("success"));
        Assert.assertTrue(Files.isDirectory(Paths.get(tempOutputDir.toString(), "batch_29275"))
                          && Files.exists(Paths.get(tempOutputDir.toString(), "batch_29275")));
        Assert.assertTrue(Files.isDirectory(Paths.get(tempOutputDir.toString(), "batch_29275", "chunk_42"))
                          && Files.exists(Paths.get(tempOutputDir.toString(), "batch_29275", "chunk_42")));
        Assert.assertEquals(
                43,
                JSON.parseObject(Files.readString(Paths.get(tempOutputDir.toString(), "batch_29275", "batch_meta.json"))).getJSONArray("chunk_info").size()
        );
    }
}
