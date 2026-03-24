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

package com.alipay.antchain.l2.relayer.dal.repository;

import java.util.List;
import java.util.concurrent.locks.Lock;

import com.alipay.antchain.l2.relayer.commons.models.ActiveNode;
import com.alipay.antchain.l2.relayer.commons.models.BizDistributedTask;

/**
 * Repository interface for managing distributed task scheduling and node coordination.
 * <p>This interface provides methods for coordinating distributed tasks across multiple
 * active nodes in the relayer system. It handles task distribution, node activation,
 * and status tracking to ensure proper load balancing and fault tolerance.</p>
 * <p>Key functionalities include:</p>
 * <ul>
 *   <li>Distributed lock management for task dispatch coordination</li>
 *   <li>Node registration and activation tracking</li>
 *   <li>Distributed task assignment and status management</li>
 *   <li>Batch operations for efficient task and node updates</li>
 * </ul>
 */
public interface IScheduleRepository {

    /**
     * Retrieves the distributed lock for task dispatch coordination.
     * <p>This lock ensures that only one node can dispatch tasks at a time,
     * preventing race conditions and duplicate task assignments in a distributed
     * environment.</p>
     *
     * @return the distributed lock instance for task dispatch operations
     */
    Lock getDispatchLock();

    /**
     * Activates a node in the distributed system.
     * <p>This method registers a node as active and available for task assignment.
     * It records the node's unique identifier and IP address for tracking and
     * communication purposes.</p>
     *
     * @param nodeId the unique identifier of the node to activate
     * @param nodeIp the IP address of the node
     */
    void activate(String nodeId, String nodeIp);

    /**
     * Retrieves all distributed tasks in the system.
     * <p>This method returns a complete list of all business distributed tasks,
     * regardless of their current status or assigned node.</p>
     *
     * @return a list of all business distributed tasks
     */
    List<BizDistributedTask> getAllBizDistributedTasks();

    /**
     * Retrieves all distributed tasks assigned to a specific node.
     * <p>This method filters tasks by the node identifier, returning only those
     * tasks that are currently assigned to or being processed by the specified node.</p>
     *
     * @param nodeId the unique identifier of the node
     * @return a list of business distributed tasks assigned to the specified node
     */
    List<BizDistributedTask> getBizDistributedTasksByNodeId(String nodeId);

    /**
     * Retrieves all active nodes in the distributed system.
     * <p>This method returns information about all nodes that are currently
     * registered and active in the system, including their status and metadata.</p>
     *
     * @return a list of all active nodes
     */
    List<ActiveNode> getAllActiveNodes();

    /**
     * Retrieves information about a specific active node.
     * <p>This method looks up a node by its unique identifier and returns
     * its current status and configuration details.</p>
     *
     * @param nodeId the unique identifier of the node
     * @return the active node information, or null if the node is not found
     */
    ActiveNode getActiveNodeByNodeId(String nodeId);

    /**
     * Updates the status of multiple active nodes in batch.
     * <p>This method efficiently updates the status information for multiple nodes
     * in a single operation, which is useful for periodic health checks and
     * status synchronization.</p>
     *
     * @param nodes a list of active nodes with updated status information
     */
    void updateStatusOfActiveNodes(List<ActiveNode> nodes);

    /**
     * Inserts multiple distributed tasks in batch.
     * <p>This method creates new task entries in the system efficiently by
     * performing a batch insert operation. This is typically used when
     * initializing or scaling up task distribution.</p>
     *
     * @param tasks a list of business distributed tasks to insert
     */
    void batchInsertBizDTTasks(List<BizDistributedTask> tasks);

    /**
     * Updates multiple distributed tasks in batch.
     * <p>This method efficiently updates the status, assignment, or other properties
     * of multiple tasks in a single operation. This is commonly used for task
     * reassignment, status updates, or completion tracking.</p>
     *
     * @param tasks a list of business distributed tasks with updated information
     */
    void batchUpdateBizDTTasks(List<BizDistributedTask> tasks);
}
