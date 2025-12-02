package com.alipay.antchain.l2.relayer.cli.commands;

import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.l2.relayer.server.grpc.AdminServiceGrpc;
import com.alipay.antchain.l2.relayer.server.grpc.GetRollupEconomicConfigReq;
import com.alipay.antchain.l2.relayer.server.grpc.UpdateRollupEconomicStrategyConfigReq;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

@Getter
@ShellCommandGroup(value = "Commands about rollup economic strategy")
@ShellComponent
@Slf4j
public class RollupEconomicCommands extends BaseCommands {

    enum ConfigKey {
        midEip1559PriceLimit,
        highEip1559PriceLimit,
        maxPendingBatchCount,
        maxPendingProofCount,
        maxBatchWaitingTime,
        maxProofWaitingTime
    }

    @Value("${grpc.client.admin.address:static://localhost:7088}")
    private String adminAddress;

    @GrpcClient("admin")
    private AdminServiceGrpc.AdminServiceBlockingStub adminServiceBlockingStub;

    @Override
    public boolean needAdminServer() {
        return true;
    }

    @ShellMethod(value = "Update the rollup economic config `midEip1559PriceLimit`.")
    Object updateMidEip1559PriceLimit(
            @ShellOption(help = "new config value (in wei)") String value
    ) {
        if (!StrUtil.isNumeric(value)) {
            return "value is not a number: " + value;
        }
        var resp = adminServiceBlockingStub.updateRollupEconomicStrategyConfig(
                UpdateRollupEconomicStrategyConfigReq.newBuilder()
                        .setConfigKey(ConfigKey.midEip1559PriceLimit.name())
                        .setConfigValue(value)
                        .build()
        );
        if (resp.getCode() != 0) {
            return "failed: " + resp.getErrorMsg();
        }
        return "success";
    }

    @ShellMethod(value = "Update the rollup economic config `highEip1559PriceLimit`.")
    Object updateHighEip1559PriceLimit(
            @ShellOption(help = "new config value (in wei)") String value
    ) {
        if (!StrUtil.isNumeric(value)) {
            return "value is not a number: " + value;
        }
        var resp = adminServiceBlockingStub.updateRollupEconomicStrategyConfig(
                UpdateRollupEconomicStrategyConfigReq.newBuilder()
                        .setConfigKey(ConfigKey.highEip1559PriceLimit.name())
                        .setConfigValue(value)
                        .build()
        );
        if (resp.getCode() != 0) {
            return "failed: " + resp.getErrorMsg();
        }
        return "success";
    }

    @ShellMethod(value = "Update the rollup economic config `maxPendingBatchCount`.")
    Object updateMaxPendingBatchCount(
            @ShellOption(help = "new config value") Integer value
    ) {
        if (value <= 0) {
            return "value must be positive: " + value;
        }
        var resp = adminServiceBlockingStub.updateRollupEconomicStrategyConfig(
                UpdateRollupEconomicStrategyConfigReq.newBuilder()
                        .setConfigKey(ConfigKey.maxPendingBatchCount.name())
                        .setConfigValue(value.toString())
                        .build()
        );
        if (resp.getCode() != 0) {
            return "failed: " + resp.getErrorMsg();
        }
        return "success";
    }

    @ShellMethod(value = "Update the rollup economic config `maxPendingProofCount`.")
    Object updateMaxPendingProofCount(
            @ShellOption(help = "new config value") Integer value
    ) {
        if (value <= 0) {
            return "value must be positive: " + value;
        }
        var resp = adminServiceBlockingStub.updateRollupEconomicStrategyConfig(
                UpdateRollupEconomicStrategyConfigReq.newBuilder()
                        .setConfigKey(ConfigKey.maxPendingProofCount.name())
                        .setConfigValue(value.toString())
                        .build()
        );
        if (resp.getCode() != 0) {
            return "failed: " + resp.getErrorMsg();
        }
        return "success";
    }

    @ShellMethod(value = "Update the rollup economic config `maxBatchWaitingTime`.")
    Object updateMaxBatchWaitingTime(
            @ShellOption(help = "new config value (in seconds)") Long value
    ) {
        if (value <= 0) {
            return "value must be positive: " + value;
        }
        var resp = adminServiceBlockingStub.updateRollupEconomicStrategyConfig(
                UpdateRollupEconomicStrategyConfigReq.newBuilder()
                        .setConfigKey(ConfigKey.maxBatchWaitingTime.name())
                        .setConfigValue(value.toString())
                        .build()
        );
        if (resp.getCode() != 0) {
            return "failed: " + resp.getErrorMsg();
        }
        return "success";
    }

    @ShellMethod(value = "Update the rollup economic config `maxProofWaitingTime`.")
    Object updateMaxProofWaitingTime(
            @ShellOption(help = "new config value (in seconds)") Long value
    ) {
        if (value <= 0) {
            return "value must be positive: " + value;
        }
        var resp = adminServiceBlockingStub.updateRollupEconomicStrategyConfig(
                UpdateRollupEconomicStrategyConfigReq.newBuilder()
                        .setConfigKey(ConfigKey.maxProofWaitingTime.name())
                        .setConfigValue(value.toString())
                        .build()
        );
        if (resp.getCode() != 0) {
            return "failed: " + resp.getErrorMsg();
        }
        return "success";
    }

    @ShellMethod(value = "Get the rollup economic config `midEip1559PriceLimit`.")
    Object getMidEip1559PriceLimit() {
        var resp = adminServiceBlockingStub.getRollupEconomicConfig(
                GetRollupEconomicConfigReq.newBuilder()
                        .setConfigKey(ConfigKey.midEip1559PriceLimit.name())
                        .build()
        );
        if (resp.getCode() != 0) {
            return "failed: " + resp.getErrorMsg();
        }
        return "midEip1559PriceLimit: " + resp.getGetRollupEconomicConfigResp().getConfigValue() + " wei";
    }

    @ShellMethod(value = "Get the rollup economic config `highEip1559PriceLimit`.")
    Object getHighEip1559PriceLimit() {
        var resp = adminServiceBlockingStub.getRollupEconomicConfig(
                GetRollupEconomicConfigReq.newBuilder()
                        .setConfigKey(ConfigKey.highEip1559PriceLimit.name())
                        .build()
        );
        if (resp.getCode() != 0) {
            return "failed: " + resp.getErrorMsg();
        }
        return "highEip1559PriceLimit: " + resp.getGetRollupEconomicConfigResp().getConfigValue() + " wei";
    }

    @ShellMethod(value = "Get the rollup economic config `maxPendingBatchCount`.")
    Object getMaxPendingBatchCount() {
        var resp = adminServiceBlockingStub.getRollupEconomicConfig(
                GetRollupEconomicConfigReq.newBuilder()
                        .setConfigKey(ConfigKey.maxPendingBatchCount.name())
                        .build()
        );
        if (resp.getCode() != 0) {
            return "failed: " + resp.getErrorMsg();
        }
        return "maxPendingBatchCount: " + resp.getGetRollupEconomicConfigResp().getConfigValue();
    }

    @ShellMethod(value = "Get the rollup economic config `maxPendingProofCount`.")
    Object getMaxPendingProofCount() {
        var resp = adminServiceBlockingStub.getRollupEconomicConfig(
                GetRollupEconomicConfigReq.newBuilder()
                        .setConfigKey(ConfigKey.maxPendingProofCount.name())
                        .build()
        );
        if (resp.getCode() != 0) {
            return "failed: " + resp.getErrorMsg();
        }
        return "maxPendingProofCount: " + resp.getGetRollupEconomicConfigResp().getConfigValue();
    }

    @ShellMethod(value = "Get the rollup economic config `maxBatchWaitingTime`.")
    Object getMaxBatchWaitingTime() {
        var resp = adminServiceBlockingStub.getRollupEconomicConfig(
                GetRollupEconomicConfigReq.newBuilder()
                        .setConfigKey(ConfigKey.maxBatchWaitingTime.name())
                        .build()
        );
        if (resp.getCode() != 0) {
            return "failed: " + resp.getErrorMsg();
        }
        return "maxBatchWaitingTime: " + resp.getGetRollupEconomicConfigResp().getConfigValue() + " seconds";
    }

    @ShellMethod(value = "Get the rollup economic config `maxProofWaitingTime`.")
    Object getMaxProofWaitingTime() {
        var resp = adminServiceBlockingStub.getRollupEconomicConfig(
                GetRollupEconomicConfigReq.newBuilder()
                        .setConfigKey(ConfigKey.maxProofWaitingTime.name())
                        .build()
        );
        if (resp.getCode() != 0) {
            return "failed: " + resp.getErrorMsg();
        }
        return "maxProofWaitingTime: " + resp.getGetRollupEconomicConfigResp().getConfigValue() + " seconds";
    }
}