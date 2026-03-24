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

import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.l2.relayer.server.grpc.*;
import org.junit.Assert;
import org.junit.Test;

import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OracleCommandsTest {

    private OracleCommands oracleCommands;

    @Test
    public void testWithdrawFromVault() {
        var mockTo = "0x863df6bfa4469f3ead0be8f9f2aae51c91a907b4";
        var mockValue = 100;
        String mockTxHash = "0x05f71e1b2cb4f03e547739db15d080fd30c989eda04d37ce6264c5686e0722c9";
        oracleCommands = new OracleCommands();
        var stub = mock(AdminServiceGrpc.AdminServiceBlockingStub.class);
        when(stub.withdrawFromVault(notNull())).thenReturn(Response.newBuilder().setCode(0)
                .setWithdrawFromVaultResp(
                        WithdrawFromVaultResp.newBuilder()
                                .setTxHash(mockTxHash)
                                .build()
                )
                .build());
        ReflectUtil.setFieldValue(oracleCommands, "adminServiceBlockingStub", stub);
        Assert.assertTrue(StrUtil.contains((String) oracleCommands.withdrawFromVault(mockTo, mockValue), mockTxHash));
    }

    @Test
    public void testUpdateFixedProfit() {
        var mockFixedProfit = "100";
        String mockTxHash = "0x05f71e1b2cb4f03e547739db15d080fd30c989eda04d37ce6264c5686e0722c9";
        oracleCommands = new OracleCommands();
        var stub = mock(AdminServiceGrpc.AdminServiceBlockingStub.class);
        when(stub.updateFixedProfit(notNull())).thenReturn(Response.newBuilder().setCode(0)
                .setUpdateFixedProfitResp(
                        UpdateFixedProfitResp.newBuilder()
                                .setTxHash(mockTxHash)
                                .build()
                )
                .build());
        ReflectUtil.setFieldValue(oracleCommands, "adminServiceBlockingStub", stub);
        Assert.assertTrue(StrUtil.contains((String) oracleCommands.updateFixedProfit(mockFixedProfit), mockTxHash));
    }

    @Test
    public void testUpdateTotalProfit() {
        var mockTotalScala = "100";
        String mockTxHash = "0x05f71e1b2cb4f03e547739db15d080fd30c989eda04d37ce6264c5686e0722c9";
        oracleCommands = new OracleCommands();
        var stub = mock(AdminServiceGrpc.AdminServiceBlockingStub.class);
        when(stub.updateTotalScala(notNull())).thenReturn(Response.newBuilder().setCode(0)
                .setUpdateTotalScalaResp(
                        UpdateTotalScalaResp.newBuilder()
                                .setTxHash(mockTxHash)
                                .build()
                )
                .build());
        ReflectUtil.setFieldValue(oracleCommands, "adminServiceBlockingStub", stub);
        Assert.assertTrue(StrUtil.contains((String) oracleCommands.updateTotalScala(mockTotalScala), mockTxHash));
    }
}
