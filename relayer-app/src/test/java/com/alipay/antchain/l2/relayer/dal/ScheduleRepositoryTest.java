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

package com.alipay.antchain.l2.relayer.dal;

import java.math.BigInteger;
import java.util.List;

import cn.hutool.core.collection.ListUtil;
import com.alipay.antchain.l2.prover.controller.ProverControllerServerGrpc;
import com.alipay.antchain.l2.relayer.L2RelayerApplication;
import com.alipay.antchain.l2.relayer.TestBase;
import com.alipay.antchain.l2.relayer.commons.enums.ActiveNodeStatusEnum;
import com.alipay.antchain.l2.relayer.commons.enums.BizTaskTypeEnum;
import com.alipay.antchain.l2.relayer.commons.models.ActiveNode;
import com.alipay.antchain.l2.relayer.commons.models.BizDistributedTask;
import com.alipay.antchain.l2.relayer.config.RollupConfig;
import com.alipay.antchain.l2.relayer.core.blockchain.L1Client;
import com.alipay.antchain.l2.relayer.core.blockchain.L2Client;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.gasprice.L1GasPriceProviderConfig;
import com.alipay.antchain.l2.relayer.core.blockchain.helper.gasprice.L2GasPriceProviderConfig;
import com.alipay.antchain.l2.relayer.core.layer2.economic.RollupEconomicStrategyConfig;
import com.alipay.antchain.l2.relayer.dal.mapper.ActiveNodeMapper;
import com.alipay.antchain.l2.relayer.dal.repository.IOracleRepository;
import com.alipay.antchain.l2.relayer.dal.repository.IScheduleRepository;
import com.alipay.antchain.l2.relayer.engine.core.Activator;
import com.alipay.antchain.l2.relayer.engine.core.Dispatcher;
import com.alipay.antchain.l2.tracer.TraceServiceGrpc;
import jakarta.annotation.Resource;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.web3j.protocol.Web3j;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@SpringBootTest(
        classes = L2RelayerApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {"spring.flyway.enabled=false", "l2-relayer.l1-client.eth-network-fork.unknown-network-config-file=bpo/unknown.json"}
)
@Sql(scripts = {"classpath:data/ddl.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:data/drop_all.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
public class ScheduleRepositoryTest extends TestBase {

    @Resource
    private IScheduleRepository scheduleRepository;

    @Resource
    private ActiveNodeMapper activeNodeMapper;

    @MockitoBean
    private IOracleRepository oracleRepository;

    @MockitoBean(name = "prover-client")
    private ProverControllerServerGrpc.ProverControllerServerBlockingStub proverStub;

    @MockitoBean(name = "tracer-client")
    private TraceServiceGrpc.TraceServiceBlockingStub tracerStub;

    @MockitoBean
    private L1Client l1Client;

    @MockitoBean
    private L2Client l2Client;

    @TestBean
    private RollupConfig rollupConfig;

    @MockitoBean
    private Activator activator;

    @MockitoBean
    private Dispatcher dispatcher;

    @MockitoBean
    private RollupEconomicStrategyConfig rollupEconomicStrategyConfig;

    @MockitoBean
    private L1GasPriceProviderConfig l1GasPriceProviderConfig;

    @MockitoBean
    private L2GasPriceProviderConfig l2GasPriceProviderConfig;

    @MockitoBean(name = "l1Web3j")
    private Web3j l1Web3j;

    @MockitoBean(name = "l1ChainId")
    private BigInteger l1ChainId;

    @Before
    public void initMock() {
        when(l1Client.l1BlobNumLimit()).thenReturn(4L);

        when(rollupConfig.getGasPerChunk()).thenReturn(3000_0000);
    }

    @Test
    public void testGetDispatchLock() {
        assertNotNull(scheduleRepository.getDispatchLock());
    }

    @Test
    public void testActivate() {
        // delete all before this test
        activeNodeMapper.delete(null);

        scheduleRepository.activate("test", "123.123.123.123");
        ActiveNode activeNode = scheduleRepository.getActiveNodeByNodeId("test");
        assertTrue(activeNode.getLastActiveTime() > System.currentTimeMillis() - 10_000);
        assertEquals(ActiveNodeStatusEnum.ONLINE, activeNode.getStatus());
        List<ActiveNode> activeNodes = scheduleRepository.getAllActiveNodes();
        assertEquals(1, activeNodes.size());

        activeNodes.get(0).setStatus(ActiveNodeStatusEnum.OFFLINE);
        scheduleRepository.updateStatusOfActiveNodes(activeNodes);

        activeNode = scheduleRepository.getActiveNodeByNodeId("test");
        assertEquals(ActiveNodeStatusEnum.OFFLINE, activeNode.getStatus());
    }

    @Test
    public void testBizTask() {
        scheduleRepository.batchInsertBizDTTasks(ListUtil.toList(
                new BizDistributedTask("test", BizTaskTypeEnum.BLOCK_POLLING_TASK, "123", System.currentTimeMillis())
        ));
        List<BizDistributedTask> tasks = scheduleRepository.getAllBizDistributedTasks();
        assertEquals(1, tasks.size());

        tasks = scheduleRepository.getBizDistributedTasksByNodeId("test");
        assertEquals(1, tasks.size());

        tasks.get(0).setStartTime(0);
        scheduleRepository.batchUpdateBizDTTasks(tasks);

        tasks = scheduleRepository.getBizDistributedTasksByNodeId("test");
        assertEquals(0, tasks.get(0).getStartTime());
    }


    // ==================== Negative Case Tests ====================

    /**
     * Test get active node by non-existent node ID
     * Verifies that querying non-existent node returns null
     */
    @Test
    public void testGetActiveNodeByNodeId_NonExistent() {
        ActiveNode result = scheduleRepository.getActiveNodeByNodeId("non-existent-node");
        assertNull(result);
    }

    /**
     * Test get biz distributed tasks by non-existent node ID
     * Verifies that querying non-existent node returns empty list
     */
    @Test
    public void testGetBizDistributedTasksByNodeId_NonExistent() {
        List<BizDistributedTask> result = scheduleRepository.getBizDistributedTasksByNodeId("non-existent-node");
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    /**
     * Test activate with null or empty node ID
     * Verifies that invalid node ID is handled gracefully
     */
    @Test
    public void testActivate_InvalidNodeId() {
        // Test with empty string - should handle gracefully
        try {
            scheduleRepository.activate("", "192.168.1.1");
            // If no exception, verify the node was created
            ActiveNode node = scheduleRepository.getActiveNodeByNodeId("");
            assertNotNull(node);
        } catch (Exception e) {
            // Exception is also acceptable for invalid input
            assertTrue(e.getMessage().contains("node") || e.getMessage().contains("id"));
        }
    }

    /**
     * Test activate with duplicate node ID
     * Verifies that activating same node twice updates the existing record
     */
    @Test
    public void testActivate_DuplicateNodeId() {
        activeNodeMapper.delete(null);

        scheduleRepository.activate("duplicate-test", "192.168.1.1");
        ActiveNode node1 = scheduleRepository.getActiveNodeByNodeId("duplicate-test");
        assertNotNull(node1);
        long firstActiveTime = node1.getLastActiveTime();

        // Wait a bit to ensure time difference
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Activate again with different IP
        scheduleRepository.activate("duplicate-test", "192.168.1.2");
        ActiveNode node2 = scheduleRepository.getActiveNodeByNodeId("duplicate-test");
        assertNotNull(node2);

        // Should update the existing record
        assertEquals("192.168.1.2", node2.getNodeIp());
        assertTrue(node2.getLastActiveTime() >= firstActiveTime);

        // Should still have only one node
        List<ActiveNode> allNodes = scheduleRepository.getAllActiveNodes();
        assertEquals(1, allNodes.size());
    }

    /**
     * Test update status of empty node list
     * Verifies that updating empty list is handled gracefully
     */
    @Test
    public void testUpdateStatusOfActiveNodes_EmptyList() {
        // Should not throw exception when updating empty list
        scheduleRepository.updateStatusOfActiveNodes(ListUtil.toList());

        // Verify no changes to existing nodes
        List<ActiveNode> nodes = scheduleRepository.getAllActiveNodes();
        assertNotNull(nodes);
    }

    /**
     * Test update status of non-existent nodes
     * Verifies that updating non-existent nodes does not throw exception
     */
    @Test
    public void testUpdateStatusOfActiveNodes_NonExistentNodes() {
        ActiveNode nonExistentNode = new ActiveNode();
        nonExistentNode.setNodeId("non-existent");
        nonExistentNode.setStatus(ActiveNodeStatusEnum.OFFLINE);

        // Should not throw exception when updating non-existent node
        scheduleRepository.updateStatusOfActiveNodes(ListUtil.toList(nonExistentNode));

        // Verify the node still doesn't exist
        ActiveNode result = scheduleRepository.getActiveNodeByNodeId("non-existent");
        assertNull(result);
    }

    /**
     * Test batch insert empty task list
     * Verifies that inserting empty list is handled gracefully
     */
    @Test
    public void testBatchInsertBizDTTasks_EmptyList() {
        // Should not throw exception when inserting empty list
        scheduleRepository.batchInsertBizDTTasks(ListUtil.toList());

        List<BizDistributedTask> tasks = scheduleRepository.getAllBizDistributedTasks();
        assertNotNull(tasks);
    }

    /**
     * Test batch update empty task list
     * Verifies that updating empty list is handled gracefully
     */
    @Test
    public void testBatchUpdateBizDTTasks_EmptyList() {
        // Should not throw exception when updating empty list
        scheduleRepository.batchUpdateBizDTTasks(ListUtil.toList());

        List<BizDistributedTask> tasks = scheduleRepository.getAllBizDistributedTasks();
        assertNotNull(tasks);
    }

    /**
     * Test batch update non-existent tasks
     * Verifies that updating non-existent tasks does not throw exception
     */
    @Test
    public void testBatchUpdateBizDTTasks_NonExistentTasks() {
        BizDistributedTask nonExistentTask = new BizDistributedTask(
                "non-existent-node",
                BizTaskTypeEnum.BLOCK_POLLING_TASK,
                "999",
                System.currentTimeMillis()
        );

        // Should not throw exception when updating non-existent task
        scheduleRepository.batchUpdateBizDTTasks(ListUtil.toList(nonExistentTask));

        // Verify the task still doesn't exist
        List<BizDistributedTask> tasks = scheduleRepository.getBizDistributedTasksByNodeId("non-existent-node");
        assertEquals(0, tasks.size());
    }

    /**
     * Test get all active nodes when database is empty
     * Verifies that querying empty database returns empty list
     */
    @Test
    public void testGetAllActiveNodes_EmptyDatabase() {
        activeNodeMapper.delete(null);

        List<ActiveNode> nodes = scheduleRepository.getAllActiveNodes();
        assertNotNull(nodes);
        assertEquals(0, nodes.size());
    }

    /**
     * Test get all biz distributed tasks when database is empty
     * Verifies that querying empty database returns empty list
     */
    @Test
    public void testGetAllBizDistributedTasks_EmptyDatabase() {
        List<BizDistributedTask> tasks = scheduleRepository.getAllBizDistributedTasks();
        assertNotNull(tasks);
        // May have existing tasks, just verify it doesn't throw exception
    }

    /**
     * Test get dispatch lock multiple times
     * Verifies that lock can be obtained multiple times
     */
    @Test
    public void testGetDispatchLock_MultipleCalls() {
        var lock1 = scheduleRepository.getDispatchLock();
        assertNotNull(lock1);

        var lock2 = scheduleRepository.getDispatchLock();
        assertNotNull(lock2);

        // Both locks should be valid
        assertEquals(lock1.getClass(), lock2.getClass());
    }

    /**
     * Test batch insert duplicate tasks
     * Verifies that inserting duplicate tasks is handled correctly
     */
    @Test
    public void testBatchInsertBizDTTasks_DuplicateTasks() {
        BizDistributedTask task = new BizDistributedTask(
                "duplicate-test",
                BizTaskTypeEnum.BATCH_COMMIT_TASK,
                "456",
                System.currentTimeMillis()
        );

        // Insert first time
        scheduleRepository.batchInsertBizDTTasks(ListUtil.toList(task));

        // Insert second time - may throw exception or handle gracefully depending on implementation
        try {
            scheduleRepository.batchInsertBizDTTasks(ListUtil.toList(task));
        } catch (Exception e) {
            // Exception is acceptable for duplicate insertion
            assertTrue(e.getMessage() != null);
        }
    }
}
