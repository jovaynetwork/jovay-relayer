package com.alipay.antchain.l2.relayer.engine.core;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;
import jakarta.annotation.Resource;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alipay.antchain.l2.relayer.commons.enums.ActiveNodeStatusEnum;
import com.alipay.antchain.l2.relayer.commons.enums.BizTaskTypeEnum;
import com.alipay.antchain.l2.relayer.commons.models.ActiveNode;
import com.alipay.antchain.l2.relayer.commons.models.BizDistributedTask;
import com.alipay.antchain.l2.relayer.commons.models.IDistributedTask;
import com.alipay.antchain.l2.relayer.dal.repository.IScheduleRepository;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Dispatcher负责拆分区块链任务，并根据节点心跳表获取在线节点排值班表
 */
@Component
@Slf4j
public class Dispatcher {

    private final List<IDistributedTask> runningTaskList = ListUtil.toList(
            new BizDistributedTask(BizTaskTypeEnum.BLOCK_POLLING_TASK),
            new BizDistributedTask(BizTaskTypeEnum.BATCH_PROVE_TASK),
            new BizDistributedTask(BizTaskTypeEnum.BATCH_COMMIT_TASK),
            new BizDistributedTask(BizTaskTypeEnum.PROOF_COMMIT_TASK),
            new BizDistributedTask(BizTaskTypeEnum.RELIABLE_TX_TASK),
            new BizDistributedTask(BizTaskTypeEnum.L1_BLOCK_POLLING_TASK),
            new BizDistributedTask(BizTaskTypeEnum.L1MSG_PROCESS_TASK),
            new BizDistributedTask(BizTaskTypeEnum.L2MSG_PROVE_TASK),
            new BizDistributedTask(BizTaskTypeEnum.ORACLE_GAS_FEED_TASK)
    );

    @Resource
    private IScheduleRepository scheduleRepository;

    @Value("#{duty.timeSliceLength}")
    private long timeSliceLength;

    @Getter
    @Value("${l2-relayer.engine.schedule.activate.ttl:5000}")
    private long nodeTimeToLive;

    public void dispatch() {
        Lock lock = getDistributeLock();
        if (!lock.tryLock()) {
            log.debug("not my dispatch lock.");
            return;
        }

        try {
            // update status of all nodes
            List<ActiveNode> onlineNodes = getAndUpdateOnlineNode();
            if (ObjectUtil.isEmpty(onlineNodes)) {
                log.warn("none online nodes!");
                return;
            }
            log.debug("size of online node : {}", onlineNodes.size());

            log.debug("dispatch distributed tasks now.");

            // 剔除已分配过时间片的任务
            List<IDistributedTask> tasksToDispatch = filterTasksInTimeSliceOrNodeOffline(runningTaskList, onlineNodes);
            if (ObjectUtil.isEmpty(tasksToDispatch)) {
                log.debug("empty tasks to dispatch");
                return;
            }

            // 给剩余任务分配时间片
            doDispatch(onlineNodes, tasksToDispatch);
        } catch (Exception e) {
            log.error("failed to dispatch distributed task: ", e);
        } finally {
            lock.unlock();
        }
    }

    private Lock getDistributeLock() {
        return scheduleRepository.getDispatchLock();
    }

    private List<IDistributedTask> filterTasksInTimeSliceOrNodeOffline(List<IDistributedTask> allTasks, List<ActiveNode> onlineNodes) {

        Set<String> onlineNodeIdSet = onlineNodes.stream().map(ActiveNode::getNodeId).collect(Collectors.toSet());
        Map<BizTaskTypeEnum, IDistributedTask> allTasksMap = Maps.newHashMap();
        for (IDistributedTask task : allTasks) {
            task.setTimeSliceLength(timeSliceLength);
            allTasksMap.put(task.getTaskType(), task);
        }

        Map<BizTaskTypeEnum, IDistributedTask> newTaskMap = Maps.newHashMap(allTasksMap);
        List<BizDistributedTask> bizDistributedTasks = scheduleRepository.getAllBizDistributedTasks();
        for (BizDistributedTask existedTask : bizDistributedTasks) {
            existedTask.setTimeSliceLength(timeSliceLength);
            newTaskMap.remove(existedTask.getTaskType());
            if (!existedTask.ifFinish() && onlineNodeIdSet.contains(existedTask.getNodeId())) {
                allTasksMap.remove(existedTask.getTaskType());
            }
        }

        List<BizDistributedTask> newBizDistributedTasks = newTaskMap.values().stream()
                .filter(
                        task -> task instanceof BizDistributedTask
                ).map(task -> (BizDistributedTask) task)
                .peek(task -> task.setStartTime(System.currentTimeMillis()))
                .collect(Collectors.toList());
        if (!newBizDistributedTasks.isEmpty()) {
            scheduleRepository.batchInsertBizDTTasks(newBizDistributedTasks);
        }

        return Lists.newArrayList(allTasksMap.values());
    }

    private List<ActiveNode> getAndUpdateOnlineNode() {
        List<ActiveNode> nodes = scheduleRepository.getAllActiveNodes();
        List<ActiveNode> onlineNodes = Lists.newArrayList();
        for (ActiveNode node : nodes) {
            if (node.ifActive(nodeTimeToLive)) {
                node.setStatus(ActiveNodeStatusEnum.ONLINE);
                onlineNodes.add(node);
            } else {
                node.setStatus(ActiveNodeStatusEnum.OFFLINE);
            }
        }
        scheduleRepository.updateStatusOfActiveNodes(nodes);
        return onlineNodes;
    }

    private void doDispatch(List<ActiveNode> nodes, List<IDistributedTask> tasks) {
        Collections.shuffle(nodes);
        roundRobin(nodes, tasks);
        // give a better algorithm for balancing tasks
        scheduleRepository.batchUpdateBizDTTasks(
                tasks.stream()
                        .filter(
                                task -> task instanceof BizDistributedTask
                        ).map(task -> (BizDistributedTask) task)
                        .collect(Collectors.toList())
        );
        log.info("dispatch tasks : {}", tasks.stream().map(IDistributedTask::getTaskType).map(BizTaskTypeEnum::name).collect(Collectors.joining(" , ")));
    }

    private void roundRobin(List<ActiveNode> nodes, List<IDistributedTask> tasks) {
        Collections.shuffle(tasks);
        for (int i = 0; i < tasks.size(); ++i) {
            ActiveNode node = nodes.get(i % nodes.size());
            tasks.get(i).setNodeId(node.getNodeId());
            tasks.get(i).setStartTime(System.currentTimeMillis());
        }
    }
}
