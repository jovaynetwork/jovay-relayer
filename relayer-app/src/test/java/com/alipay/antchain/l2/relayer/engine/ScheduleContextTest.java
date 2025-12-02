package com.alipay.antchain.l2.relayer.engine;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.net.Ipv4Util;
import com.alipay.antchain.l2.relayer.engine.core.ScheduleContext;
import org.junit.Assert;
import org.junit.Test;

public class ScheduleContextTest {

    @Test
    public void testNewScheduleContext() {
        ScheduleContext context1 = new ScheduleContext(ScheduleContext.NODE_ID_MODE_IP);
        Assert.assertTrue(Ipv4Util.ipv4ToLong(context1.getNodeIp()) > 0);
        Assert.assertTrue(Ipv4Util.ipv4ToLong(context1.getNodeId()) > 0);

        context1 = new ScheduleContext(ScheduleContext.NODE_ID_MODE_UUID);
        Assert.assertTrue(Ipv4Util.ipv4ToLong(context1.getNodeIp()) > 0);
        Assert.assertEquals(UUID.fromString(context1.getNodeId()).toString(), context1.getNodeId());
    }
}
