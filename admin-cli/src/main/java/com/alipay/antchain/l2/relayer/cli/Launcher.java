package com.alipay.antchain.l2.relayer.cli;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication(scanBasePackages = {"com.alipay.antchain.l2.relayer.cli"})
public class Launcher {
    public static void main(String[] args) {
        List<String> propList = new ArrayList<>();
        if (ObjectUtil.isNotEmpty(args)) {

            List<String> argsList = ListUtil.toList(args);

            String port = argsList.stream().filter(x -> StrUtil.startWith(x, "--port")).findAny().orElse("");
            int portNum = 7088;
            if (StrUtil.isNotEmpty(port)) {
                portNum = Integer.parseInt(StrUtil.split(port, "=").get(1));
            }

            String host = argsList.stream().filter(x -> StrUtil.startWith(x, "--host")).findAny().orElse("");
            String hostVal = "127.0.0.1";
            if (StrUtil.isNotEmpty(host)) {
                hostVal = StrUtil.split(host, "=").get(1);
            }

            propList.add(StrUtil.format("grpc.client.admin.address=static://{}:{}", hostVal, portNum));

            argsList = argsList.stream().filter(x -> !StrUtil.startWithAny(x, "--port", "--host")).collect(Collectors.toList());
            args = argsList.toArray(new String[0]);
        }
        new SpringApplicationBuilder(Launcher.class)
                .web(WebApplicationType.NONE)
                .properties(propList.toArray(new String[0]))
                .run(args);
    }
}
