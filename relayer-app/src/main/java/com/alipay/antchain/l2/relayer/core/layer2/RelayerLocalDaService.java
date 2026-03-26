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

package com.alipay.antchain.l2.relayer.core.layer2;

import com.alipay.antchain.l2.relayer.commons.l2basic.da.DaProof;
import com.alipay.antchain.l2.relayer.commons.l2basic.da.IDaService;
import com.alipay.antchain.l2.relayer.commons.models.BatchWrapper;
import com.alipay.antchain.l2.relayer.dal.repository.IRollupRepository;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class RelayerLocalDaService implements IDaService {

    private IRollupRepository rollupRepository;

    @Override
    public void uploadBatch(BatchWrapper batchWrapper) {
        // save batch meta to storage
        rollupRepository.saveBatch(batchWrapper);
    }

    @Override
    public DaProof endorseBatch(BatchWrapper batchWrapper) {
        // just return empty proof
        return new DaProof(new byte[0]);
    }
}
