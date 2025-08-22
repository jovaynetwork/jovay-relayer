package com.alipay.antchain.l2.relayer.query;

import com.ulisesbocchio.jasyptspringboot.annotation.EnableEncryptableProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.alipay.antchain.l2.relayer.query"})
@MapperScan("com.alipay.antchain.l2.relayer.dal.mapper")
@EnableEncryptableProperties
public class L2RelayerQueryServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(L2RelayerQueryServiceApplication.class, args);
    }
}
