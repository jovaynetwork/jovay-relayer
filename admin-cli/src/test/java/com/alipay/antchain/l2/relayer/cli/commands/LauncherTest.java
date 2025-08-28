package com.alipay.antchain.l2.relayer.cli.commands;

import com.alipay.antchain.l2.relayer.cli.Launcher;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.boot.builder.SpringApplicationBuilder;

public class LauncherTest {

    @Test
    public void test() {
        Mockito.mockConstruction(SpringApplicationBuilder.class, (mock, context) -> {
            Mockito.when(mock.web(ArgumentMatchers.notNull())).thenReturn(mock);
            Mockito.when(mock.properties(ArgumentMatchers.anyString())).thenReturn(mock);
            Mockito.when(mock.run(ArgumentMatchers.any())).thenReturn(null);
        });
        Launcher.main(new String[]{"--port=8888", "--host=localhost"});
    }
}
