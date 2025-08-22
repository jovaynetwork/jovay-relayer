package com.alipay.antchain.l2.relayer.dal.repository;

import java.util.List;
import java.util.concurrent.locks.Lock;

import com.alipay.antchain.l2.relayer.commons.models.ActiveNode;
import com.alipay.antchain.l2.relayer.commons.models.BizDistributedTask;

public interface IScheduleRepository {

    Lock getDispatchLock();

    void activate(String nodeId, String nodeIp);

    List<BizDistributedTask> getAllBizDistributedTasks();

    List<BizDistributedTask> getBizDistributedTasksByNodeId(String nodeId);

    List<ActiveNode> getAllActiveNodes();

    ActiveNode getActiveNodeByNodeId(String nodeId);

    void updateStatusOfActiveNodes(List<ActiveNode> nodes);

    void batchInsertBizDTTasks(List<BizDistributedTask> tasks);

    void batchUpdateBizDTTasks(List<BizDistributedTask> tasks);
}
