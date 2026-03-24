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

package com.alipay.antchain.l2.relayer.commons.enums;

public enum BizTaskTypeEnum {

    BLOCK_POLLING_TASK,

    BATCH_COMMIT_TASK,

    BATCH_PROVE_TASK,

    PROOF_COMMIT_TASK,

    RELIABLE_TX_TASK,

    L1_BLOCK_POLLING_TASK,

    L1MSG_PROCESS_TASK,

    L2MSG_PROVE_TASK,

    ORACLE_GAS_FEED_TASK
}
