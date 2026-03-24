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
import com.alipay.antchain.l2.relayer.server.grpc.AdminServiceGrpc;
import com.alipay.antchain.l2.relayer.server.grpc.GetRollupEconomicConfigResp;
import com.alipay.antchain.l2.relayer.server.grpc.Response;
import org.junit.Assert;
import org.junit.Test;

import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RollupEconomicCommandsTest {

    private RollupEconomicCommands rollupEconomicCommands;

    // --- Update Tests ---

    @Test
    public void testUpdateMidEip1559PriceLimit() {
        rollupEconomicCommands = new RollupEconomicCommands();
        AdminServiceGrpc.AdminServiceBlockingStub stub = mock(AdminServiceGrpc.AdminServiceBlockingStub.class);
        when(stub.updateRollupEconomicStrategyConfig(notNull())).thenReturn(Response.newBuilder().setCode(0).build());
        ReflectUtil.setFieldValue(rollupEconomicCommands, "adminServiceBlockingStub", stub);

        Assert.assertEquals("success", rollupEconomicCommands.updateMidEip1559PriceLimit("1000000000"));
    }

    @Test
    public void testUpdateMidEip1559PriceLimitInvalidNumber() {
        rollupEconomicCommands = new RollupEconomicCommands();
        Assert.assertEquals("value is not a number: invalid", rollupEconomicCommands.updateMidEip1559PriceLimit("invalid"));
    }

    @Test
    public void testUpdateMidEip1559PriceLimitFailure() {
        rollupEconomicCommands = new RollupEconomicCommands();
        AdminServiceGrpc.AdminServiceBlockingStub stub = mock(AdminServiceGrpc.AdminServiceBlockingStub.class);
        when(stub.updateRollupEconomicStrategyConfig(notNull())).thenReturn(Response.newBuilder().setCode(1).setErrorMsg("test error").build());
        ReflectUtil.setFieldValue(rollupEconomicCommands, "adminServiceBlockingStub", stub);

        Assert.assertEquals("failed: test error", rollupEconomicCommands.updateMidEip1559PriceLimit("1000000000"));
    }

    @Test
    public void testUpdateHighEip1559PriceLimit() {
        rollupEconomicCommands = new RollupEconomicCommands();
        AdminServiceGrpc.AdminServiceBlockingStub stub = mock(AdminServiceGrpc.AdminServiceBlockingStub.class);
        when(stub.updateRollupEconomicStrategyConfig(notNull())).thenReturn(Response.newBuilder().setCode(0).build());
        ReflectUtil.setFieldValue(rollupEconomicCommands, "adminServiceBlockingStub", stub);

        Assert.assertEquals("success", rollupEconomicCommands.updateHighEip1559PriceLimit("2000000000"));
    }

    @Test
    public void testUpdateHighEip1559PriceLimitInvalidNumber() {
        rollupEconomicCommands = new RollupEconomicCommands();
        Assert.assertEquals("value is not a number: invalid", rollupEconomicCommands.updateHighEip1559PriceLimit("invalid"));
    }

    @Test
    public void testUpdateHighEip1559PriceLimitFailure() {
        rollupEconomicCommands = new RollupEconomicCommands();
        AdminServiceGrpc.AdminServiceBlockingStub stub = mock(AdminServiceGrpc.AdminServiceBlockingStub.class);
        when(stub.updateRollupEconomicStrategyConfig(notNull())).thenReturn(Response.newBuilder().setCode(1).setErrorMsg("test error").build());
        ReflectUtil.setFieldValue(rollupEconomicCommands, "adminServiceBlockingStub", stub);

        Assert.assertEquals("failed: test error", rollupEconomicCommands.updateHighEip1559PriceLimit("2000000000"));
    }

    @Test
    public void testUpdateMaxPendingBatchCount() {
        rollupEconomicCommands = new RollupEconomicCommands();
        AdminServiceGrpc.AdminServiceBlockingStub stub = mock(AdminServiceGrpc.AdminServiceBlockingStub.class);
        when(stub.updateRollupEconomicStrategyConfig(notNull())).thenReturn(Response.newBuilder().setCode(0).build());
        ReflectUtil.setFieldValue(rollupEconomicCommands, "adminServiceBlockingStub", stub);

        Assert.assertEquals("success", rollupEconomicCommands.updateMaxPendingBatchCount(10));
    }

    @Test
    public void testUpdateMaxPendingBatchCountInvalidValue() {
        rollupEconomicCommands = new RollupEconomicCommands();
        Assert.assertEquals("value must be positive: -5", rollupEconomicCommands.updateMaxPendingBatchCount(-5));
        Assert.assertEquals("value must be positive: 0", rollupEconomicCommands.updateMaxPendingBatchCount(0));
    }

    @Test
    public void testUpdateMaxPendingBatchCountFailure() {
        rollupEconomicCommands = new RollupEconomicCommands();
        AdminServiceGrpc.AdminServiceBlockingStub stub = mock(AdminServiceGrpc.AdminServiceBlockingStub.class);
        when(stub.updateRollupEconomicStrategyConfig(notNull())).thenReturn(Response.newBuilder().setCode(1).setErrorMsg("test error").build());
        ReflectUtil.setFieldValue(rollupEconomicCommands, "adminServiceBlockingStub", stub);

        Assert.assertEquals("failed: test error", rollupEconomicCommands.updateMaxPendingBatchCount(10));
    }

    @Test
    public void testUpdateMaxPendingProofCount() {
        rollupEconomicCommands = new RollupEconomicCommands();
        AdminServiceGrpc.AdminServiceBlockingStub stub = mock(AdminServiceGrpc.AdminServiceBlockingStub.class);
        when(stub.updateRollupEconomicStrategyConfig(notNull())).thenReturn(Response.newBuilder().setCode(0).build());
        ReflectUtil.setFieldValue(rollupEconomicCommands, "adminServiceBlockingStub", stub);

        Assert.assertEquals("success", rollupEconomicCommands.updateMaxPendingProofCount(5));
    }

    @Test
    public void testUpdateMaxPendingProofCountInvalidValue() {
        rollupEconomicCommands = new RollupEconomicCommands();
        Assert.assertEquals("value must be positive: -3", rollupEconomicCommands.updateMaxPendingProofCount(-3));
        Assert.assertEquals("value must be positive: 0", rollupEconomicCommands.updateMaxPendingProofCount(0));
    }

    @Test
    public void testUpdateMaxPendingProofCountFailure() {
        rollupEconomicCommands = new RollupEconomicCommands();
        AdminServiceGrpc.AdminServiceBlockingStub stub = mock(AdminServiceGrpc.AdminServiceBlockingStub.class);
        when(stub.updateRollupEconomicStrategyConfig(notNull())).thenReturn(Response.newBuilder().setCode(1).setErrorMsg("test error").build());
        ReflectUtil.setFieldValue(rollupEconomicCommands, "adminServiceBlockingStub", stub);

        Assert.assertEquals("failed: test error", rollupEconomicCommands.updateMaxPendingProofCount(5));
    }

    @Test
    public void testUpdateMaxBatchWaitingTime() {
        rollupEconomicCommands = new RollupEconomicCommands();
        AdminServiceGrpc.AdminServiceBlockingStub stub = mock(AdminServiceGrpc.AdminServiceBlockingStub.class);
        when(stub.updateRollupEconomicStrategyConfig(notNull())).thenReturn(Response.newBuilder().setCode(0).build());
        ReflectUtil.setFieldValue(rollupEconomicCommands, "adminServiceBlockingStub", stub);

        Assert.assertEquals("success", rollupEconomicCommands.updateMaxBatchWaitingTime(300L));
    }

    @Test
    public void testUpdateMaxBatchWaitingTimeInvalidValue() {
        rollupEconomicCommands = new RollupEconomicCommands();
        Assert.assertEquals("value must be positive: -100", rollupEconomicCommands.updateMaxBatchWaitingTime(-100L));
        Assert.assertEquals("value must be positive: 0", rollupEconomicCommands.updateMaxBatchWaitingTime(0L));
    }

    @Test
    public void testUpdateMaxBatchWaitingTimeFailure() {
        rollupEconomicCommands = new RollupEconomicCommands();
        AdminServiceGrpc.AdminServiceBlockingStub stub = mock(AdminServiceGrpc.AdminServiceBlockingStub.class);
        when(stub.updateRollupEconomicStrategyConfig(notNull())).thenReturn(Response.newBuilder().setCode(1).setErrorMsg("test error").build());
        ReflectUtil.setFieldValue(rollupEconomicCommands, "adminServiceBlockingStub", stub);

        Assert.assertEquals("failed: test error", rollupEconomicCommands.updateMaxBatchWaitingTime(300L));
    }

    @Test
    public void testUpdateMaxProofWaitingTime() {
        rollupEconomicCommands = new RollupEconomicCommands();
        AdminServiceGrpc.AdminServiceBlockingStub stub = mock(AdminServiceGrpc.AdminServiceBlockingStub.class);
        when(stub.updateRollupEconomicStrategyConfig(notNull())).thenReturn(Response.newBuilder().setCode(0).build());
        ReflectUtil.setFieldValue(rollupEconomicCommands, "adminServiceBlockingStub", stub);

        Assert.assertEquals("success", rollupEconomicCommands.updateMaxProofWaitingTime(600L));
    }

    @Test
    public void testUpdateMaxProofWaitingTimeInvalidValue() {
        rollupEconomicCommands = new RollupEconomicCommands();
        Assert.assertEquals("value must be positive: -200", rollupEconomicCommands.updateMaxProofWaitingTime(-200L));
        Assert.assertEquals("value must be positive: 0", rollupEconomicCommands.updateMaxProofWaitingTime(0L));
    }

    @Test
    public void testUpdateMaxProofWaitingTimeFailure() {
        rollupEconomicCommands = new RollupEconomicCommands();
        AdminServiceGrpc.AdminServiceBlockingStub stub = mock(AdminServiceGrpc.AdminServiceBlockingStub.class);
        when(stub.updateRollupEconomicStrategyConfig(notNull())).thenReturn(Response.newBuilder().setCode(1).setErrorMsg("test error").build());
        ReflectUtil.setFieldValue(rollupEconomicCommands, "adminServiceBlockingStub", stub);

        Assert.assertEquals("failed: test error", rollupEconomicCommands.updateMaxProofWaitingTime(600L));
    }

    // --- Get Tests ---

    @Test
    public void testGetMidEip1559PriceLimit() {
        rollupEconomicCommands = new RollupEconomicCommands();
        AdminServiceGrpc.AdminServiceBlockingStub stub = mock(AdminServiceGrpc.AdminServiceBlockingStub.class);
        Response response = Response.newBuilder()
                .setCode(0)
                .setGetRollupEconomicConfigResp(GetRollupEconomicConfigResp.newBuilder().setConfigValue("1000000000").build())
                .build();
        when(stub.getRollupEconomicConfig(notNull())).thenReturn(response);
        ReflectUtil.setFieldValue(rollupEconomicCommands, "adminServiceBlockingStub", stub);

        Assert.assertEquals("midEip1559PriceLimit: 1000000000 wei", rollupEconomicCommands.getMidEip1559PriceLimit());
    }

    @Test
    public void testGetMidEip1559PriceLimitFailure() {
        rollupEconomicCommands = new RollupEconomicCommands();
        AdminServiceGrpc.AdminServiceBlockingStub stub = mock(AdminServiceGrpc.AdminServiceBlockingStub.class);
        Response response = Response.newBuilder()
                .setCode(1)
                .setErrorMsg("test error")
                .build();
        when(stub.getRollupEconomicConfig(notNull())).thenReturn(response);
        ReflectUtil.setFieldValue(rollupEconomicCommands, "adminServiceBlockingStub", stub);

        Assert.assertEquals("failed: test error", rollupEconomicCommands.getMidEip1559PriceLimit());
    }

    @Test
    public void testGetHighEip1559PriceLimit() {
        rollupEconomicCommands = new RollupEconomicCommands();
        AdminServiceGrpc.AdminServiceBlockingStub stub = mock(AdminServiceGrpc.AdminServiceBlockingStub.class);
        Response response = Response.newBuilder()
                .setCode(0)
                .setGetRollupEconomicConfigResp(GetRollupEconomicConfigResp.newBuilder().setConfigValue("2000000000").build())
                .build();
        when(stub.getRollupEconomicConfig(notNull())).thenReturn(response);
        ReflectUtil.setFieldValue(rollupEconomicCommands, "adminServiceBlockingStub", stub);

        Assert.assertEquals("highEip1559PriceLimit: 2000000000 wei", rollupEconomicCommands.getHighEip1559PriceLimit());
    }

    @Test
    public void testGetHighEip1559PriceLimitFailure() {
        rollupEconomicCommands = new RollupEconomicCommands();
        AdminServiceGrpc.AdminServiceBlockingStub stub = mock(AdminServiceGrpc.AdminServiceBlockingStub.class);
        Response response = Response.newBuilder()
                .setCode(1)
                .setErrorMsg("test error")
                .build();
        when(stub.getRollupEconomicConfig(notNull())).thenReturn(response);
        ReflectUtil.setFieldValue(rollupEconomicCommands, "adminServiceBlockingStub", stub);

        Assert.assertEquals("failed: test error", rollupEconomicCommands.getHighEip1559PriceLimit());
    }

    @Test
    public void testGetMaxPendingBatchCount() {
        rollupEconomicCommands = new RollupEconomicCommands();
        AdminServiceGrpc.AdminServiceBlockingStub stub = mock(AdminServiceGrpc.AdminServiceBlockingStub.class);
        Response response = Response.newBuilder()
                .setCode(0)
                .setGetRollupEconomicConfigResp(GetRollupEconomicConfigResp.newBuilder().setConfigValue("10").build())
                .build();
        when(stub.getRollupEconomicConfig(notNull())).thenReturn(response);
        ReflectUtil.setFieldValue(rollupEconomicCommands, "adminServiceBlockingStub", stub);

        Assert.assertEquals("maxPendingBatchCount: 10", rollupEconomicCommands.getMaxPendingBatchCount());
    }

    @Test
    public void testGetMaxPendingBatchCountFailure() {
        rollupEconomicCommands = new RollupEconomicCommands();
        AdminServiceGrpc.AdminServiceBlockingStub stub = mock(AdminServiceGrpc.AdminServiceBlockingStub.class);
        Response response = Response.newBuilder()
                .setCode(1)
                .setErrorMsg("test error")
                .build();
        when(stub.getRollupEconomicConfig(notNull())).thenReturn(response);
        ReflectUtil.setFieldValue(rollupEconomicCommands, "adminServiceBlockingStub", stub);

        Assert.assertEquals("failed: test error", rollupEconomicCommands.getMaxPendingBatchCount());
    }

    @Test
    public void testGetMaxPendingProofCount() {
        rollupEconomicCommands = new RollupEconomicCommands();
        AdminServiceGrpc.AdminServiceBlockingStub stub = mock(AdminServiceGrpc.AdminServiceBlockingStub.class);
        Response response = Response.newBuilder()
                .setCode(0)
                .setGetRollupEconomicConfigResp(GetRollupEconomicConfigResp.newBuilder().setConfigValue("5").build())
                .build();
        when(stub.getRollupEconomicConfig(notNull())).thenReturn(response);
        ReflectUtil.setFieldValue(rollupEconomicCommands, "adminServiceBlockingStub", stub);

        Assert.assertEquals("maxPendingProofCount: 5", rollupEconomicCommands.getMaxPendingProofCount());
    }

    @Test
    public void testGetMaxPendingProofCountFailure() {
        rollupEconomicCommands = new RollupEconomicCommands();
        AdminServiceGrpc.AdminServiceBlockingStub stub = mock(AdminServiceGrpc.AdminServiceBlockingStub.class);
        Response response = Response.newBuilder()
                .setCode(1)
                .setErrorMsg("test error")
                .build();
        when(stub.getRollupEconomicConfig(notNull())).thenReturn(response);
        ReflectUtil.setFieldValue(rollupEconomicCommands, "adminServiceBlockingStub", stub);

        Assert.assertEquals("failed: test error", rollupEconomicCommands.getMaxPendingProofCount());
    }

    @Test
    public void testGetMaxBatchWaitingTime() {
        rollupEconomicCommands = new RollupEconomicCommands();
        AdminServiceGrpc.AdminServiceBlockingStub stub = mock(AdminServiceGrpc.AdminServiceBlockingStub.class);
        Response response = Response.newBuilder()
                .setCode(0)
                .setGetRollupEconomicConfigResp(GetRollupEconomicConfigResp.newBuilder().setConfigValue("300").build())
                .build();
        when(stub.getRollupEconomicConfig(notNull())).thenReturn(response);
        ReflectUtil.setFieldValue(rollupEconomicCommands, "adminServiceBlockingStub", stub);

        Assert.assertEquals("maxBatchWaitingTime: 300 seconds", rollupEconomicCommands.getMaxBatchWaitingTime());
    }

    @Test
    public void testGetMaxBatchWaitingTimeFailure() {
        rollupEconomicCommands = new RollupEconomicCommands();
        AdminServiceGrpc.AdminServiceBlockingStub stub = mock(AdminServiceGrpc.AdminServiceBlockingStub.class);
        Response response = Response.newBuilder()
                .setCode(1)
                .setErrorMsg("test error")
                .build();
        when(stub.getRollupEconomicConfig(notNull())).thenReturn(response);
        ReflectUtil.setFieldValue(rollupEconomicCommands, "adminServiceBlockingStub", stub);

        Assert.assertEquals("failed: test error", rollupEconomicCommands.getMaxBatchWaitingTime());
    }

    @Test
    public void testGetMaxProofWaitingTime() {
        rollupEconomicCommands = new RollupEconomicCommands();
        AdminServiceGrpc.AdminServiceBlockingStub stub = mock(AdminServiceGrpc.AdminServiceBlockingStub.class);
        Response response = Response.newBuilder()
                .setCode(0)
                .setGetRollupEconomicConfigResp(GetRollupEconomicConfigResp.newBuilder().setConfigValue("600").build())
                .build();
        when(stub.getRollupEconomicConfig(notNull())).thenReturn(response);
        ReflectUtil.setFieldValue(rollupEconomicCommands, "adminServiceBlockingStub", stub);

        Assert.assertEquals("maxProofWaitingTime: 600 seconds", rollupEconomicCommands.getMaxProofWaitingTime());
    }

    @Test
    public void testGetMaxProofWaitingTimeFailure() {
        rollupEconomicCommands = new RollupEconomicCommands();
        AdminServiceGrpc.AdminServiceBlockingStub stub = mock(AdminServiceGrpc.AdminServiceBlockingStub.class);
        Response response = Response.newBuilder()
                .setCode(1)
                .setErrorMsg("test error")
                .build();
        when(stub.getRollupEconomicConfig(notNull())).thenReturn(response);
        ReflectUtil.setFieldValue(rollupEconomicCommands, "adminServiceBlockingStub", stub);

        Assert.assertEquals("failed: test error", rollupEconomicCommands.getMaxProofWaitingTime());
    }
}