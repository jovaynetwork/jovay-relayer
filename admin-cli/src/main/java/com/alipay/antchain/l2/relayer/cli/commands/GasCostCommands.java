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

import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.l2.relayer.server.grpc.AdminServiceGrpc;
import com.alipay.antchain.l2.relayer.server.grpc.GetGasPriceConfigReq;
import com.alipay.antchain.l2.relayer.server.grpc.UpdateGasPriceConfigReq;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

@Getter
@ShellCommandGroup(value = "Commands about blockchain gas costs")
@ShellComponent
@Slf4j
public class GasCostCommands extends BaseCommands {

    private static final String L1_CHAIN_TYPE = "ethereum";

    @Value("${grpc.client.admin.address:static://localhost:7088}")
    private String adminAddress;

    @GrpcClient("admin")
    private AdminServiceGrpc.AdminServiceBlockingStub adminServiceBlockingStub;

    @Override
    public boolean needAdminServer() {
        return true;
    }

    @ShellMethod(value = "Update the gas price config `gasPriceIncreasedPercentage` for L1 client. ")
    Object updateEthGasPriceIncreasedPercentage(
            @ShellOption(help = "new config value") Double value
    ) {
        var resp = adminServiceBlockingStub.updateGasPriceConfig(
                UpdateGasPriceConfigReq.newBuilder()
                        .setChainType(L1_CHAIN_TYPE)
                        .setConfigKey("gasPriceIncreasedPercentage")
                        .setConfigValue(value.toString())
                        .build()
        );
        if (resp.getCode() != 0) {
            return "failed: " + resp.getErrorMsg();
        }
        return "success";
    }

    @ShellMethod("Update the gas price config `largerFeePerBlobGasMultiplier` for L1 client.")
    Object updateEthFeePerBlobGasDividingVal(
            @ShellOption(help = "new config value") String value
    ) {
        if (!StrUtil.isNumeric(value)) {
            return "value is not a number: " + value;
        }
        var resp = adminServiceBlockingStub.updateGasPriceConfig(
                UpdateGasPriceConfigReq.newBuilder()
                        .setChainType(L1_CHAIN_TYPE)
                        .setConfigKey("feePerBlobGasDividingVal")
                        .setConfigValue(value)
                        .build()
        );
        if (resp.getCode() != 0) {
            return "failed: " + resp.getErrorMsg();
        }
        return "success";
    }

    @ShellMethod("Update the gas price config `largerFeePerBlobGasMultiplier` for L1 client.")
    Object updateEthLargerFeePerBlobGasMultiplier(
            @ShellOption(help = "new config value") Double value
    ) {
        var resp = adminServiceBlockingStub.updateGasPriceConfig(
                UpdateGasPriceConfigReq.newBuilder()
                        .setChainType(L1_CHAIN_TYPE)
                        .setConfigKey("largerFeePerBlobGasMultiplier")
                        .setConfigValue(value.toString())
                        .build()
        );
        if (resp.getCode() != 0) {
            return "failed: " + resp.getErrorMsg();
        }
        return "success";
    }

    @ShellMethod("Update the gas price config `smallerFeePerBlobGasMultiplier` for L1 client.")
    Object updateEthSmallerFeePerBlobGasMultiplier(
            @ShellOption(help = "new config value") Double value
    ) {
        var resp = adminServiceBlockingStub.updateGasPriceConfig(
                UpdateGasPriceConfigReq.newBuilder()
                        .setChainType(L1_CHAIN_TYPE)
                        .setConfigKey("smallerFeePerBlobGasMultiplier")
                        .setConfigValue(value.toString())
                        .build()
        );
        if (resp.getCode() != 0) {
            return "failed: " + resp.getErrorMsg();
        }
        return "success";
    }

    @ShellMethod("Update the gas price config `baseFeeMultiplier` for L1 client.")
    Object updateEthBaseFeeMultiplier(
            @ShellOption(help = "new config value") Integer value
    ) {
        var resp = adminServiceBlockingStub.updateGasPriceConfig(
                UpdateGasPriceConfigReq.newBuilder()
                        .setChainType(L1_CHAIN_TYPE)
                        .setConfigKey("baseFeeMultiplier")
                        .setConfigValue(value.toString())
                        .build()
        );
        if (resp.getCode() != 0) {
            return "failed: " + resp.getErrorMsg();
        }
        return "success";
    }

    @ShellMethod("Update the gas price config `priorityFeePerGasIncreasedPercentage` for L1 client.")
    Object updateEthPriorityFeePerGasIncreasedPercentage(
            @ShellOption(help = "new config value") Double value
    ) {
        var resp = adminServiceBlockingStub.updateGasPriceConfig(
                UpdateGasPriceConfigReq.newBuilder()
                        .setChainType(L1_CHAIN_TYPE)
                        .setConfigKey("priorityFeePerGasIncreasedPercentage")
                        .setConfigValue(value.toString())
                        .build()
        );
        if (resp.getCode() != 0) {
            return "failed: " + resp.getErrorMsg();
        }
        return "success";
    }

    @ShellMethod("Update the gas price config `eip4844PriorityFeePerGasIncreasedPercentage` for L1 client.")
    Object updateEthEip4844PriorityFeePerGasIncreasedPercentage(
            @ShellOption(help = "new config value") Double value
    ) {
        var resp = adminServiceBlockingStub.updateGasPriceConfig(
                UpdateGasPriceConfigReq.newBuilder()
                        .setChainType(L1_CHAIN_TYPE)
                        .setConfigKey("eip4844PriorityFeePerGasIncreasedPercentage")
                        .setConfigValue(value.toString())
                        .build()
        );
        if (resp.getCode() != 0) {
            return "failed: " + resp.getErrorMsg();
        }
        return "success";
    }

    @ShellMethod("Update the gas price config `maxPriceLimit` for L1 client.")
    Object updateEthMaxPriceLimit(
            @ShellOption(help = "new config value") String value
    ) {
        if (!StrUtil.isNumeric(value)) {
            return "value is not a number: " + value;
        }
        var resp = adminServiceBlockingStub.updateGasPriceConfig(
                UpdateGasPriceConfigReq.newBuilder()
                        .setChainType(L1_CHAIN_TYPE)
                        .setConfigKey("maxPriceLimit")
                        .setConfigValue(value)
                        .build()
        );
        if (resp.getCode() != 0) {
            return "failed: " + resp.getErrorMsg();
        }
        return "success";
    }

    @ShellMethod("Update the gas price config `extraGasPrice` for L1 client.")
    Object updateEthExtraGasPrice(
            @ShellOption(help = "new config value") String value
    ) {
        if (!StrUtil.isNumeric(value)) {
            return "value is not a number: " + value;
        }
        var resp = adminServiceBlockingStub.updateGasPriceConfig(
                UpdateGasPriceConfigReq.newBuilder()
                        .setChainType(L1_CHAIN_TYPE)
                        .setConfigKey("extraGasPrice")
                        .setConfigValue(value)
                        .build()
        );
        if (resp.getCode() != 0) {
            return "failed: " + resp.getErrorMsg();
        }
        return "success";
    }

    @ShellMethod("Update the gas price config `minimumEip4844PriorityPrice` for L1 client.")
    Object updateEthMinimumEip4844PriorityPrice(
            @ShellOption(help = "new config value") String value
    ) {
        if (!StrUtil.isNumeric(value)) {
            return "value is not a number: " + value;
        }
        var resp = adminServiceBlockingStub.updateGasPriceConfig(
                UpdateGasPriceConfigReq.newBuilder()
                        .setChainType(L1_CHAIN_TYPE)
                        .setConfigKey("minimumEip4844PriorityPrice")
                        .setConfigValue(value)
                        .build()
        );
        if (resp.getCode() != 0) {
            return "failed: " + resp.getErrorMsg();
        }
        return "success";
    }

    @ShellMethod("Update the gas price config `minimumEip1559PriorityPrice` for L1 client.")
    Object updateEthMinimumEip1559PriorityPrice(
            @ShellOption(help = "new config value") String value
    ) {
        if (!StrUtil.isNumeric(value)) {
            return "value is not a number: " + value;
        }
        var resp = adminServiceBlockingStub.updateGasPriceConfig(
                UpdateGasPriceConfigReq.newBuilder()
                        .setChainType(L1_CHAIN_TYPE)
                        .setConfigKey("minimumEip1559PriorityPrice")
                        .setConfigValue(value)
                        .build()
        );
        if (resp.getCode() != 0) {
            return "failed: " + resp.getErrorMsg();
        }
        return "success";
    }

    @ShellMethod(value = "Get the gas price config `gasPriceIncreasedPercentage` for L1 client.")
    Object getEthGasPriceIncreasedPercentage() {
        var resp = adminServiceBlockingStub.getGasPriceConfig(
                GetGasPriceConfigReq.newBuilder()
                        .setChainType(L1_CHAIN_TYPE)
                        .setConfigKey("gasPriceIncreasedPercentage")
                        .build()
        );
        if (resp.getCode() != 0) {
            return "failed: " + resp.getErrorMsg();
        }
        return "gasPriceIncreasedPercentage: " + resp.getGetGasPriceConfigResp().getConfigValue() + "%";
    }

    @ShellMethod(value = "Get the gas price config `feePerBlobGasDividingVal` for L1 client.")
    Object getEthFeePerBlobGasDividingVal() {
        var resp = adminServiceBlockingStub.getGasPriceConfig(
                GetGasPriceConfigReq.newBuilder()
                        .setChainType(L1_CHAIN_TYPE)
                        .setConfigKey("feePerBlobGasDividingVal")
                        .build()
        );
        if (resp.getCode() != 0) {
            return "failed: " + resp.getErrorMsg();
        }
        return "feePerBlobGasDividingVal: " + resp.getGetGasPriceConfigResp().getConfigValue();
    }

    @ShellMethod(value = "Get the gas price config `largerFeePerBlobGasMultiplier` for L1 client.")
    Object getEthLargerFeePerBlobGasMultiplier() {
        var resp = adminServiceBlockingStub.getGasPriceConfig(
                GetGasPriceConfigReq.newBuilder()
                        .setChainType(L1_CHAIN_TYPE)
                        .setConfigKey("largerFeePerBlobGasMultiplier")
                        .build()
        );
        if (resp.getCode() != 0) {
            return "failed: " + resp.getErrorMsg();
        }
        return "largerFeePerBlobGasMultiplier: " + resp.getGetGasPriceConfigResp().getConfigValue();
    }

    @ShellMethod(value = "Get the gas price config `smallerFeePerBlobGasMultiplier` for L1 client.")
    Object getEthSmallerFeePerBlobGasMultiplier() {
        var resp = adminServiceBlockingStub.getGasPriceConfig(
                GetGasPriceConfigReq.newBuilder()
                        .setChainType(L1_CHAIN_TYPE)
                        .setConfigKey("smallerFeePerBlobGasMultiplier")
                        .build()
        );
        if (resp.getCode() != 0) {
            return "failed: " + resp.getErrorMsg();
        }
        return "smallerFeePerBlobGasMultiplier: " + resp.getGetGasPriceConfigResp().getConfigValue();
    }

    @ShellMethod(value = "Get the gas price config `baseFeeMultiplier` for L1 client.")
    Object getEthBaseFeeMultiplier() {
        var resp = adminServiceBlockingStub.getGasPriceConfig(
                GetGasPriceConfigReq.newBuilder()
                        .setChainType(L1_CHAIN_TYPE)
                        .setConfigKey("baseFeeMultiplier")
                        .build()
        );
        if (resp.getCode() != 0) {
            return "failed: " + resp.getErrorMsg();
        }
        return "baseFeeMultiplier: " + resp.getGetGasPriceConfigResp().getConfigValue();
    }

    @ShellMethod(value = "Get the gas price config `priorityFeePerGasIncreasedPercentage` for L1 client.")
    Object getEthPriorityFeePerGasIncreasedPercentage() {
        var resp = adminServiceBlockingStub.getGasPriceConfig(
                GetGasPriceConfigReq.newBuilder()
                        .setChainType(L1_CHAIN_TYPE)
                        .setConfigKey("priorityFeePerGasIncreasedPercentage")
                        .build()
        );
        if (resp.getCode() != 0) {
            return "failed: " + resp.getErrorMsg();
        }
        return "priorityFeePerGasIncreasedPercentage: " + resp.getGetGasPriceConfigResp().getConfigValue() + "%";
    }

    @ShellMethod(value = "Get the gas price config `eip4844PriorityFeePerGasIncreasedPercentage` for L1 client.")
    Object getEthEip4844PriorityFeePerGasIncreasedPercentage() {
        var resp = adminServiceBlockingStub.getGasPriceConfig(
                GetGasPriceConfigReq.newBuilder()
                        .setChainType(L1_CHAIN_TYPE)
                        .setConfigKey("eip4844PriorityFeePerGasIncreasedPercentage")
                        .build()
        );
        if (resp.getCode() != 0) {
            return "failed: " + resp.getErrorMsg();
        }
        return "eip4844PriorityFeePerGasIncreasedPercentage: " + resp.getGetGasPriceConfigResp().getConfigValue() + "%";
    }

    @ShellMethod(value = "Get the gas price config `maxPriceLimit` for L1 client.")
    Object getEthMaxPriceLimit() {
        var resp = adminServiceBlockingStub.getGasPriceConfig(
                GetGasPriceConfigReq.newBuilder()
                        .setChainType(L1_CHAIN_TYPE)
                        .setConfigKey("maxPriceLimit")
                        .build()
        );
        if (resp.getCode() != 0) {
            return "failed: " + resp.getErrorMsg();
        }
        return "maxPriceLimit: " + resp.getGetGasPriceConfigResp().getConfigValue() + " wei";
    }

    @ShellMethod(value = "Get the gas price config `extraGasPrice` for L1 client.")
    Object getEthExtraGasPrice() {
        var resp = adminServiceBlockingStub.getGasPriceConfig(
                GetGasPriceConfigReq.newBuilder()
                        .setChainType(L1_CHAIN_TYPE)
                        .setConfigKey("extraGasPrice")
                        .build()
        );
        if (resp.getCode() != 0) {
            return "failed: " + resp.getErrorMsg();
        }
        return "extraGasPrice: " + resp.getGetGasPriceConfigResp().getConfigValue() + " wei";
    }

    @ShellMethod(value = "Get the gas price config `minimumEip4844PriorityPrice` for L1 client.")
    Object getEthMinimumEip4844PriorityPrice() {
        var resp = adminServiceBlockingStub.getGasPriceConfig(
                GetGasPriceConfigReq.newBuilder()
                        .setChainType(L1_CHAIN_TYPE)
                        .setConfigKey("minimumEip4844PriorityPrice")
                        .build()
        );
        if (resp.getCode() != 0) {
            return "failed: " + resp.getErrorMsg();
        }
        return "minimumEip4844PriorityPrice: " + resp.getGetGasPriceConfigResp().getConfigValue() + " wei";
    }

    @ShellMethod(value = "Get the gas price config `minimumEip1559PriorityPrice` for L1 client.")
    Object getEthMinimumEip1559PriorityPrice() {
        var resp = adminServiceBlockingStub.getGasPriceConfig(
                GetGasPriceConfigReq.newBuilder()
                        .setChainType(L1_CHAIN_TYPE)
                        .setConfigKey("minimumEip1559PriorityPrice")
                        .build()
        );
        if (resp.getCode() != 0) {
            return "failed: " + resp.getErrorMsg();
        }
        return "minimumEip1559PriorityPrice: " + resp.getGetGasPriceConfigResp().getConfigValue() + " wei";
    }
}