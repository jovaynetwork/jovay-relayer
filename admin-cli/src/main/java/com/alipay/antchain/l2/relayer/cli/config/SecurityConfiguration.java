package com.alipay.antchain.l2.relayer.cli.config;

import io.grpc.CallCredentials;
import net.devh.boot.grpc.client.security.CallCredentialsHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class SecurityConfiguration {

    @Value("${auth.username}")
    private String username;

    @Value("${auth.password}")
    private String password;

    @Bean
    CallCredentials grpcCredentials() {
        return CallCredentialsHelper.basicAuth(this.username, this.password);
    }
}
