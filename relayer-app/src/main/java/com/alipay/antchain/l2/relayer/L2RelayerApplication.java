package com.alipay.antchain.l2.relayer;

import com.ulisesbocchio.jasyptspringboot.annotation.EnableEncryptableProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.ApplicationPidFileWriter;

@SpringBootApplication(scanBasePackages = {"com.alipay.antchain.l2.relayer"})
@MapperScan("com.alipay.antchain.l2.relayer.dal.mapper")
@EnableEncryptableProperties
public class L2RelayerApplication {

    public static void main(String[] args) {
        SpringApplicationBuilder builder = new SpringApplicationBuilder(L2RelayerApplication.class);
        builder.application().addListeners(new ApplicationPidFileWriter());
        builder.web(WebApplicationType.NONE)
                .application()
                .run(args);
    }
}
