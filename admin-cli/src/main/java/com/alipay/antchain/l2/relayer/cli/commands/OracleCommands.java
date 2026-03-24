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

import com.alipay.antchain.l2.relayer.server.grpc.AdminServiceGrpc;
import com.alipay.antchain.l2.relayer.server.grpc.UpdateFixedProfitReq;
import com.alipay.antchain.l2.relayer.server.grpc.UpdateTotalScalaReq;
import com.alipay.antchain.l2.relayer.server.grpc.WithdrawFromVaultReq;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

@Getter
@ShellCommandGroup(value = "Commands about oracle functions")
@ShellComponent
@Slf4j
public class OracleCommands extends BaseCommands {

    @Value("${grpc.client.admin.address:static://localhost:7088}")
    private String adminAddress;

    @GrpcClient("admin")
    private AdminServiceGrpc.AdminServiceBlockingStub adminServiceBlockingStub;

    @Override
    public boolean needAdminServer() {
        return true;
    }

    @ShellMethod(value = "admin send tx to withdraw L2's ETH to L1's relayer account from L2 Coinbase.")
    Object withdrawFromVault(
            @ShellOption(help = "vault receive address") String to,
            @ShellOption(help = "specific withdraw amount") int amount
    ) {
        try {
            var response = adminServiceBlockingStub.withdrawFromVault(
                    WithdrawFromVaultReq.newBuilder()
                            .setTo(to)
                            .setAmount(amount)
                            .build()
            );
            if (response.getCode() != 0) {
                return "failed: " + response.getErrorMsg();
            }
            return response.getWithdrawFromVaultResp().getTxHash();
        } catch (Throwable t) {
            throw new RuntimeException("unexpected error please input stacktrace to check the detail", t);
        }
    }

    @ShellMethod(value = "admin send tx to set L2's fixed profit to L1OracleContract.")
    Object updateFixedProfit(
            @ShellOption(help = "L2's L1OracleContract's new fixed profit.") String profit
    ) {
        try {
            var response = adminServiceBlockingStub.updateFixedProfit(
                    UpdateFixedProfitReq.newBuilder()
                            .setProfit(profit)
                            .build()
            );
            if (response.getCode() != 0) {
                return "failed: " + response.getErrorMsg();
            }
            return response.getUpdateFixedProfitResp().getTxHash();
        } catch (Throwable t) {
            throw new RuntimeException("unexpected error please input stacktrace to check the detail", t);
        }
    }

    @ShellMethod(value = "admin send tx to set L2's total scala to L1OracleContract.")
    Object updateTotalScala(
            @ShellOption(help = "L2's L1OracleContract's new total scala.") String totalScala
    ) {
        try {
            var response = adminServiceBlockingStub.updateTotalScala(
                    UpdateTotalScalaReq.newBuilder()
                            .setTotalScala(totalScala)
                            .build()
            );
            if (response.getCode() != 0) {
                return "failed: " + response.getErrorMsg();
            }
            return response.getUpdateTotalScalaResp().getTxHash();
        } catch (Throwable t) {
            throw new RuntimeException("unexpected error please input stacktrace to check the detail", t);
        }
    }
}
