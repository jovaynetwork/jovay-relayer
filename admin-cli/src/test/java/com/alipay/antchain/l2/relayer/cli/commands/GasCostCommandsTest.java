package com.alipay.antchain.l2.relayer.cli.commands;

import cn.hutool.core.util.ReflectUtil;
import com.alipay.antchain.l2.relayer.server.grpc.AdminServiceGrpc;
import com.alipay.antchain.l2.relayer.server.grpc.GetGasPriceConfigResp;
import com.alipay.antchain.l2.relayer.server.grpc.Response;
import org.junit.Assert;
import org.junit.Test;

import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GasCostCommandsTest {

    private GasCostCommands gasCostCommands;

    @Test
    public void testUpdateEthGasPriceIncreasedPercentage() {
        gasCostCommands = new GasCostCommands();
        AdminServiceGrpc.AdminServiceBlockingStub stub = mock(AdminServiceGrpc.AdminServiceBlockingStub.class);
        when(stub.updateGasPriceConfig(notNull())).thenReturn(Response.newBuilder().setCode(0).build());
        ReflectUtil.setFieldValue(gasCostCommands, "adminServiceBlockingStub", stub);

        Assert.assertEquals("success", gasCostCommands.updateEthGasPriceIncreasedPercentage(1.5));
    }

    @Test
    public void testUpdateEthGasPriceIncreasedPercentageFailure() {
        gasCostCommands = new GasCostCommands();
        AdminServiceGrpc.AdminServiceBlockingStub stub = mock(AdminServiceGrpc.AdminServiceBlockingStub.class);
        when(stub.updateGasPriceConfig(notNull())).thenReturn(Response.newBuilder().setCode(1).setErrorMsg("test error").build());
        ReflectUtil.setFieldValue(gasCostCommands, "adminServiceBlockingStub", stub);

        Assert.assertEquals("failed: test error", gasCostCommands.updateEthGasPriceIncreasedPercentage(1.5));
    }

    @Test
    public void testUpdateEthFeePerBlobGasDividingVal() {
        gasCostCommands = new GasCostCommands();
        AdminServiceGrpc.AdminServiceBlockingStub stub = mock(AdminServiceGrpc.AdminServiceBlockingStub.class);
        when(stub.updateGasPriceConfig(notNull())).thenReturn(Response.newBuilder().setCode(0).build());
        ReflectUtil.setFieldValue(gasCostCommands, "adminServiceBlockingStub", stub);

        Assert.assertEquals("success", gasCostCommands.updateEthFeePerBlobGasDividingVal("100"));
    }

    @Test
    public void testUpdateEthFeePerBlobGasDividingValInvalidNumber() {
        gasCostCommands = new GasCostCommands();

        Assert.assertEquals("value is not a number: abc", gasCostCommands.updateEthFeePerBlobGasDividingVal("abc"));
    }

    @Test
    public void testUpdateEthLargerFeePerBlobGasMultiplier() {
        gasCostCommands = new GasCostCommands();
        AdminServiceGrpc.AdminServiceBlockingStub stub = mock(AdminServiceGrpc.AdminServiceBlockingStub.class);
        when(stub.updateGasPriceConfig(notNull())).thenReturn(Response.newBuilder().setCode(0).build());
        ReflectUtil.setFieldValue(gasCostCommands, "adminServiceBlockingStub", stub);

        Assert.assertEquals("success", gasCostCommands.updateEthLargerFeePerBlobGasMultiplier(2.0));
    }

    @Test
    public void testUpdateEthSmallerFeePerBlobGasMultiplier() {
        gasCostCommands = new GasCostCommands();
        AdminServiceGrpc.AdminServiceBlockingStub stub = mock(AdminServiceGrpc.AdminServiceBlockingStub.class);
        when(stub.updateGasPriceConfig(notNull())).thenReturn(Response.newBuilder().setCode(0).build());
        ReflectUtil.setFieldValue(gasCostCommands, "adminServiceBlockingStub", stub);

        Assert.assertEquals("success", gasCostCommands.updateEthSmallerFeePerBlobGasMultiplier(0.8));
    }

    @Test
    public void testUpdateEthBaseFeeMultiplier() {
        gasCostCommands = new GasCostCommands();
        AdminServiceGrpc.AdminServiceBlockingStub stub = mock(AdminServiceGrpc.AdminServiceBlockingStub.class);
        when(stub.updateGasPriceConfig(notNull())).thenReturn(Response.newBuilder().setCode(0).build());
        ReflectUtil.setFieldValue(gasCostCommands, "adminServiceBlockingStub", stub);

        Assert.assertEquals("success", gasCostCommands.updateEthBaseFeeMultiplier(2));
    }

    @Test
    public void testUpdateEthPriorityFeePerGasIncreasedPercentage() {
        gasCostCommands = new GasCostCommands();
        AdminServiceGrpc.AdminServiceBlockingStub stub = mock(AdminServiceGrpc.AdminServiceBlockingStub.class);
        when(stub.updateGasPriceConfig(notNull())).thenReturn(Response.newBuilder().setCode(0).build());
        ReflectUtil.setFieldValue(gasCostCommands, "adminServiceBlockingStub", stub);

        Assert.assertEquals("success", gasCostCommands.updateEthPriorityFeePerGasIncreasedPercentage(1.2));
    }

    @Test
    public void testUpdateEthEip4844PriorityFeePerGasIncreasedPercentage() {
        gasCostCommands = new GasCostCommands();
        AdminServiceGrpc.AdminServiceBlockingStub stub = mock(AdminServiceGrpc.AdminServiceBlockingStub.class);
        when(stub.updateGasPriceConfig(notNull())).thenReturn(Response.newBuilder().setCode(0).build());
        ReflectUtil.setFieldValue(gasCostCommands, "adminServiceBlockingStub", stub);

        Assert.assertEquals("success", gasCostCommands.updateEthEip4844PriorityFeePerGasIncreasedPercentage(1.3));
    }

    @Test
    public void testUpdateEthMaxPriceLimit() {
        gasCostCommands = new GasCostCommands();
        AdminServiceGrpc.AdminServiceBlockingStub stub = mock(AdminServiceGrpc.AdminServiceBlockingStub.class);
        when(stub.updateGasPriceConfig(notNull())).thenReturn(Response.newBuilder().setCode(0).build());
        ReflectUtil.setFieldValue(gasCostCommands, "adminServiceBlockingStub", stub);

        Assert.assertEquals("success", gasCostCommands.updateEthMaxPriceLimit("1000000"));
    }

    @Test
    public void testUpdateEthMaxPriceLimitInvalidNumber() {
        gasCostCommands = new GasCostCommands();

        Assert.assertEquals("value is not a number: invalid", gasCostCommands.updateEthMaxPriceLimit("invalid"));
    }

    @Test
    public void testUpdateEthExtraGasPrice() {
        gasCostCommands = new GasCostCommands();
        AdminServiceGrpc.AdminServiceBlockingStub stub = mock(AdminServiceGrpc.AdminServiceBlockingStub.class);
        when(stub.updateGasPriceConfig(notNull())).thenReturn(Response.newBuilder().setCode(0).build());
        ReflectUtil.setFieldValue(gasCostCommands, "adminServiceBlockingStub", stub);

        Assert.assertEquals("success", gasCostCommands.updateEthExtraGasPrice("500000"));
    }

    @Test
    public void testUpdateEthExtraGasPriceInvalidNumber() {
        gasCostCommands = new GasCostCommands();

        Assert.assertEquals("value is not a number: xyz", gasCostCommands.updateEthExtraGasPrice("xyz"));
    }

    @Test
    public void testUpdateEthMinimumEip4844PriorityPrice() {
        gasCostCommands = new GasCostCommands();
        AdminServiceGrpc.AdminServiceBlockingStub stub = mock(AdminServiceGrpc.AdminServiceBlockingStub.class);
        when(stub.updateGasPriceConfig(notNull())).thenReturn(Response.newBuilder().setCode(0).build());
        ReflectUtil.setFieldValue(gasCostCommands, "adminServiceBlockingStub", stub);

        Assert.assertEquals("success", gasCostCommands.updateEthMinimumEip4844PriorityPrice("1000"));
    }

    @Test
    public void testUpdateEthMinimumEip4844PriorityPriceInvalidNumber() {
        gasCostCommands = new GasCostCommands();

        Assert.assertEquals("value is not a number: notanumber", gasCostCommands.updateEthMinimumEip4844PriorityPrice("notanumber"));
    }

    @Test
    public void testUpdateEthMinimumEip1559PriorityPrice() {
        gasCostCommands = new GasCostCommands();
        AdminServiceGrpc.AdminServiceBlockingStub stub = mock(AdminServiceGrpc.AdminServiceBlockingStub.class);
        when(stub.updateGasPriceConfig(notNull())).thenReturn(Response.newBuilder().setCode(0).build());
        ReflectUtil.setFieldValue(gasCostCommands, "adminServiceBlockingStub", stub);

        Assert.assertEquals("success", gasCostCommands.updateEthMinimumEip1559PriorityPrice("2000"));
    }

    @Test
    public void testUpdateEthMinimumEip1559PriorityPriceInvalidNumber() {
        gasCostCommands = new GasCostCommands();

        Assert.assertEquals("value is not a number: 123abc", gasCostCommands.updateEthMinimumEip1559PriorityPrice("123abc"));
    }

    @Test
    public void testGetEthGasPriceIncreasedPercentage() {
        gasCostCommands = new GasCostCommands();
        AdminServiceGrpc.AdminServiceBlockingStub stub = mock(AdminServiceGrpc.AdminServiceBlockingStub.class);
        Response response = Response.newBuilder()
                .setCode(0)
                .setGetGasPriceConfigResp(GetGasPriceConfigResp.newBuilder().setConfigValue("1.5").build())
                .build();
        when(stub.getGasPriceConfig(notNull())).thenReturn(response);
        ReflectUtil.setFieldValue(gasCostCommands, "adminServiceBlockingStub", stub);

        Assert.assertEquals("gasPriceIncreasedPercentage: 1.5%", gasCostCommands.getEthGasPriceIncreasedPercentage());
    }

    @Test
    public void testGetEthGasPriceIncreasedPercentageFailure() {
        gasCostCommands = new GasCostCommands();
        AdminServiceGrpc.AdminServiceBlockingStub stub = mock(AdminServiceGrpc.AdminServiceBlockingStub.class);
        Response response = Response.newBuilder()
                .setCode(1)
                .setErrorMsg("test error")
                .build();
        when(stub.getGasPriceConfig(notNull())).thenReturn(response);
        ReflectUtil.setFieldValue(gasCostCommands, "adminServiceBlockingStub", stub);

        Assert.assertEquals("failed: test error", gasCostCommands.getEthGasPriceIncreasedPercentage());
    }

    @Test
    public void testGetEthFeePerBlobGasDividingVal() {
        gasCostCommands = new GasCostCommands();
        AdminServiceGrpc.AdminServiceBlockingStub stub = mock(AdminServiceGrpc.AdminServiceBlockingStub.class);
        Response response = Response.newBuilder()
                .setCode(0)
                .setGetGasPriceConfigResp(GetGasPriceConfigResp.newBuilder().setConfigValue("100").build())
                .build();
        when(stub.getGasPriceConfig(notNull())).thenReturn(response);
        ReflectUtil.setFieldValue(gasCostCommands, "adminServiceBlockingStub", stub);

        Assert.assertEquals("feePerBlobGasDividingVal: 100", gasCostCommands.getEthFeePerBlobGasDividingVal());
    }

    @Test
    public void testGetEthFeePerBlobGasDividingValFailure() {
        gasCostCommands = new GasCostCommands();
        AdminServiceGrpc.AdminServiceBlockingStub stub = mock(AdminServiceGrpc.AdminServiceBlockingStub.class);
        Response response = Response.newBuilder()
                .setCode(1)
                .setErrorMsg("test error")
                .build();
        when(stub.getGasPriceConfig(notNull())).thenReturn(response);
        ReflectUtil.setFieldValue(gasCostCommands, "adminServiceBlockingStub", stub);

        Assert.assertEquals("failed: test error", gasCostCommands.getEthFeePerBlobGasDividingVal());
    }

    @Test
    public void testGetEthLargerFeePerBlobGasMultiplier() {
        gasCostCommands = new GasCostCommands();
        AdminServiceGrpc.AdminServiceBlockingStub stub = mock(AdminServiceGrpc.AdminServiceBlockingStub.class);
        Response response = Response.newBuilder()
                .setCode(0)
                .setGetGasPriceConfigResp(GetGasPriceConfigResp.newBuilder().setConfigValue("2.0").build())
                .build();
        when(stub.getGasPriceConfig(notNull())).thenReturn(response);
        ReflectUtil.setFieldValue(gasCostCommands, "adminServiceBlockingStub", stub);

        Assert.assertEquals("largerFeePerBlobGasMultiplier: 2.0", gasCostCommands.getEthLargerFeePerBlobGasMultiplier());
    }

    @Test
    public void testGetEthLargerFeePerBlobGasMultiplierFailure() {
        gasCostCommands = new GasCostCommands();
        AdminServiceGrpc.AdminServiceBlockingStub stub = mock(AdminServiceGrpc.AdminServiceBlockingStub.class);
        Response response = Response.newBuilder()
                .setCode(1)
                .setErrorMsg("test error")
                .build();
        when(stub.getGasPriceConfig(notNull())).thenReturn(response);
        ReflectUtil.setFieldValue(gasCostCommands, "adminServiceBlockingStub", stub);

        Assert.assertEquals("failed: test error", gasCostCommands.getEthLargerFeePerBlobGasMultiplier());
    }

    @Test
    public void testGetEthSmallerFeePerBlobGasMultiplier() {
        gasCostCommands = new GasCostCommands();
        AdminServiceGrpc.AdminServiceBlockingStub stub = mock(AdminServiceGrpc.AdminServiceBlockingStub.class);
        Response response = Response.newBuilder()
                .setCode(0)
                .setGetGasPriceConfigResp(GetGasPriceConfigResp.newBuilder().setConfigValue("0.8").build())
                .build();
        when(stub.getGasPriceConfig(notNull())).thenReturn(response);
        ReflectUtil.setFieldValue(gasCostCommands, "adminServiceBlockingStub", stub);

        Assert.assertEquals("smallerFeePerBlobGasMultiplier: 0.8", gasCostCommands.getEthSmallerFeePerBlobGasMultiplier());
    }

    @Test
    public void testGetEthSmallerFeePerBlobGasMultiplierFailure() {
        gasCostCommands = new GasCostCommands();
        AdminServiceGrpc.AdminServiceBlockingStub stub = mock(AdminServiceGrpc.AdminServiceBlockingStub.class);
        Response response = Response.newBuilder()
                .setCode(1)
                .setErrorMsg("test error")
                .build();
        when(stub.getGasPriceConfig(notNull())).thenReturn(response);
        ReflectUtil.setFieldValue(gasCostCommands, "adminServiceBlockingStub", stub);

        Assert.assertEquals("failed: test error", gasCostCommands.getEthSmallerFeePerBlobGasMultiplier());
    }

    @Test
    public void testGetEthBaseFeeMultiplier() {
        gasCostCommands = new GasCostCommands();
        AdminServiceGrpc.AdminServiceBlockingStub stub = mock(AdminServiceGrpc.AdminServiceBlockingStub.class);
        Response response = Response.newBuilder()
                .setCode(0)
                .setGetGasPriceConfigResp(GetGasPriceConfigResp.newBuilder().setConfigValue("2").build())
                .build();
        when(stub.getGasPriceConfig(notNull())).thenReturn(response);
        ReflectUtil.setFieldValue(gasCostCommands, "adminServiceBlockingStub", stub);

        Assert.assertEquals("baseFeeMultiplier: 2", gasCostCommands.getEthBaseFeeMultiplier());
    }

    @Test
    public void testGetEthBaseFeeMultiplierFailure() {
        gasCostCommands = new GasCostCommands();
        AdminServiceGrpc.AdminServiceBlockingStub stub = mock(AdminServiceGrpc.AdminServiceBlockingStub.class);
        Response response = Response.newBuilder()
                .setCode(1)
                .setErrorMsg("test error")
                .build();
        when(stub.getGasPriceConfig(notNull())).thenReturn(response);
        ReflectUtil.setFieldValue(gasCostCommands, "adminServiceBlockingStub", stub);

        Assert.assertEquals("failed: test error", gasCostCommands.getEthBaseFeeMultiplier());
    }

    @Test
    public void testGetEthPriorityFeePerGasIncreasedPercentage() {
        gasCostCommands = new GasCostCommands();
        AdminServiceGrpc.AdminServiceBlockingStub stub = mock(AdminServiceGrpc.AdminServiceBlockingStub.class);
        Response response = Response.newBuilder()
                .setCode(0)
                .setGetGasPriceConfigResp(GetGasPriceConfigResp.newBuilder().setConfigValue("1.2").build())
                .build();
        when(stub.getGasPriceConfig(notNull())).thenReturn(response);
        ReflectUtil.setFieldValue(gasCostCommands, "adminServiceBlockingStub", stub);

        Assert.assertEquals("priorityFeePerGasIncreasedPercentage: 1.2%", gasCostCommands.getEthPriorityFeePerGasIncreasedPercentage());
    }

    @Test
    public void testGetEthPriorityFeePerGasIncreasedPercentageFailure() {
        gasCostCommands = new GasCostCommands();
        AdminServiceGrpc.AdminServiceBlockingStub stub = mock(AdminServiceGrpc.AdminServiceBlockingStub.class);
        Response response = Response.newBuilder()
                .setCode(1)
                .setErrorMsg("test error")
                .build();
        when(stub.getGasPriceConfig(notNull())).thenReturn(response);
        ReflectUtil.setFieldValue(gasCostCommands, "adminServiceBlockingStub", stub);

        Assert.assertEquals("failed: test error", gasCostCommands.getEthPriorityFeePerGasIncreasedPercentage());
    }

    @Test
    public void testGetEthEip4844PriorityFeePerGasIncreasedPercentage() {
        gasCostCommands = new GasCostCommands();
        AdminServiceGrpc.AdminServiceBlockingStub stub = mock(AdminServiceGrpc.AdminServiceBlockingStub.class);
        Response response = Response.newBuilder()
                .setCode(0)
                .setGetGasPriceConfigResp(GetGasPriceConfigResp.newBuilder().setConfigValue("1.3").build())
                .build();
        when(stub.getGasPriceConfig(notNull())).thenReturn(response);
        ReflectUtil.setFieldValue(gasCostCommands, "adminServiceBlockingStub", stub);

        Assert.assertEquals("eip4844PriorityFeePerGasIncreasedPercentage: 1.3%", gasCostCommands.getEthEip4844PriorityFeePerGasIncreasedPercentage());
    }

    @Test
    public void testGetEthEip4844PriorityFeePerGasIncreasedPercentageFailure() {
        gasCostCommands = new GasCostCommands();
        AdminServiceGrpc.AdminServiceBlockingStub stub = mock(AdminServiceGrpc.AdminServiceBlockingStub.class);
        Response response = Response.newBuilder()
                .setCode(1)
                .setErrorMsg("test error")
                .build();
        when(stub.getGasPriceConfig(notNull())).thenReturn(response);
        ReflectUtil.setFieldValue(gasCostCommands, "adminServiceBlockingStub", stub);

        Assert.assertEquals("failed: test error", gasCostCommands.getEthEip4844PriorityFeePerGasIncreasedPercentage());
    }

    @Test
    public void testGetEthMaxPriceLimit() {
        gasCostCommands = new GasCostCommands();
        AdminServiceGrpc.AdminServiceBlockingStub stub = mock(AdminServiceGrpc.AdminServiceBlockingStub.class);
        Response response = Response.newBuilder()
                .setCode(0)
                .setGetGasPriceConfigResp(GetGasPriceConfigResp.newBuilder().setConfigValue("1000000").build())
                .build();
        when(stub.getGasPriceConfig(notNull())).thenReturn(response);
        ReflectUtil.setFieldValue(gasCostCommands, "adminServiceBlockingStub", stub);

        Assert.assertEquals("maxPriceLimit: 1000000 wei", gasCostCommands.getEthMaxPriceLimit());
    }

    @Test
    public void testGetEthMaxPriceLimitFailure() {
        gasCostCommands = new GasCostCommands();
        AdminServiceGrpc.AdminServiceBlockingStub stub = mock(AdminServiceGrpc.AdminServiceBlockingStub.class);
        Response response = Response.newBuilder()
                .setCode(1)
                .setErrorMsg("test error")
                .build();
        when(stub.getGasPriceConfig(notNull())).thenReturn(response);
        ReflectUtil.setFieldValue(gasCostCommands, "adminServiceBlockingStub", stub);

        Assert.assertEquals("failed: test error", gasCostCommands.getEthMaxPriceLimit());
    }

    @Test
    public void testGetEthExtraGasPrice() {
        gasCostCommands = new GasCostCommands();
        AdminServiceGrpc.AdminServiceBlockingStub stub = mock(AdminServiceGrpc.AdminServiceBlockingStub.class);
        Response response = Response.newBuilder()
                .setCode(0)
                .setGetGasPriceConfigResp(GetGasPriceConfigResp.newBuilder().setConfigValue("500000").build())
                .build();
        when(stub.getGasPriceConfig(notNull())).thenReturn(response);
        ReflectUtil.setFieldValue(gasCostCommands, "adminServiceBlockingStub", stub);

        Assert.assertEquals("extraGasPrice: 500000 wei", gasCostCommands.getEthExtraGasPrice());
    }

    @Test
    public void testGetEthExtraGasPriceFailure() {
        gasCostCommands = new GasCostCommands();
        AdminServiceGrpc.AdminServiceBlockingStub stub = mock(AdminServiceGrpc.AdminServiceBlockingStub.class);
        Response response = Response.newBuilder()
                .setCode(1)
                .setErrorMsg("test error")
                .build();
        when(stub.getGasPriceConfig(notNull())).thenReturn(response);
        ReflectUtil.setFieldValue(gasCostCommands, "adminServiceBlockingStub", stub);

        Assert.assertEquals("failed: test error", gasCostCommands.getEthExtraGasPrice());
    }

    @Test
    public void testGetEthMinimumEip4844PriorityPrice() {
        gasCostCommands = new GasCostCommands();
        AdminServiceGrpc.AdminServiceBlockingStub stub = mock(AdminServiceGrpc.AdminServiceBlockingStub.class);
        Response response = Response.newBuilder()
                .setCode(0)
                .setGetGasPriceConfigResp(GetGasPriceConfigResp.newBuilder().setConfigValue("1000").build())
                .build();
        when(stub.getGasPriceConfig(notNull())).thenReturn(response);
        ReflectUtil.setFieldValue(gasCostCommands, "adminServiceBlockingStub", stub);

        Assert.assertEquals("minimumEip4844PriorityPrice: 1000 wei", gasCostCommands.getEthMinimumEip4844PriorityPrice());
    }

    @Test
    public void testGetEthMinimumEip4844PriorityPriceFailure() {
        gasCostCommands = new GasCostCommands();
        AdminServiceGrpc.AdminServiceBlockingStub stub = mock(AdminServiceGrpc.AdminServiceBlockingStub.class);
        Response response = Response.newBuilder()
                .setCode(1)
                .setErrorMsg("test error")
                .build();
        when(stub.getGasPriceConfig(notNull())).thenReturn(response);
        ReflectUtil.setFieldValue(gasCostCommands, "adminServiceBlockingStub", stub);

        Assert.assertEquals("failed: test error", gasCostCommands.getEthMinimumEip4844PriorityPrice());
    }

    @Test
    public void testGetEthMinimumEip1559PriorityPrice() {
        gasCostCommands = new GasCostCommands();
        AdminServiceGrpc.AdminServiceBlockingStub stub = mock(AdminServiceGrpc.AdminServiceBlockingStub.class);
        Response response = Response.newBuilder()
                .setCode(0)
                .setGetGasPriceConfigResp(GetGasPriceConfigResp.newBuilder().setConfigValue("2000").build())
                .build();
        when(stub.getGasPriceConfig(notNull())).thenReturn(response);
        ReflectUtil.setFieldValue(gasCostCommands, "adminServiceBlockingStub", stub);

        Assert.assertEquals("minimumEip1559PriorityPrice: 2000 wei", gasCostCommands.getEthMinimumEip1559PriorityPrice());
    }

    @Test
    public void testGetEthMinimumEip1559PriorityPriceFailure() {
        gasCostCommands = new GasCostCommands();
        AdminServiceGrpc.AdminServiceBlockingStub stub = mock(AdminServiceGrpc.AdminServiceBlockingStub.class);
        Response response = Response.newBuilder()
                .setCode(1)
                .setErrorMsg("test error")
                .build();
        when(stub.getGasPriceConfig(notNull())).thenReturn(response);
        ReflectUtil.setFieldValue(gasCostCommands, "adminServiceBlockingStub", stub);

        Assert.assertEquals("failed: test error", gasCostCommands.getEthMinimumEip1559PriorityPrice());
    }
}