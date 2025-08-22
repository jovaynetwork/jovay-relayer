package com.alipay.antchain.l2.relayer.cli.commands;

import java.io.IOException;
import java.net.Socket;
import java.util.List;

import cn.hutool.core.util.StrUtil;
import org.springframework.shell.Availability;
import org.springframework.shell.standard.ShellMethodAvailability;

public abstract class BaseCommands {

    public abstract String getAdminAddress();

    public abstract boolean needAdminServer();

    @ShellMethodAvailability
    public Availability baseAvailability() {
        if (needAdminServer()) {
            List<String> addrArr = StrUtil.split(StrUtil.split(getAdminAddress(), "//").get(1), ":");

            if (!checkServerStatus(addrArr.get(0), Integer.parseInt(addrArr.get(1)))) {
                return Availability.unavailable(
                        StrUtil.format("admin server {} is unreachable", getAdminAddress())
                );
            }
        }

        return Availability.available();
    }

    private boolean checkServerStatus(String host, int port) {
        try {
            Socket socket = new Socket(host, port);
            socket.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
