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

package com.alipay.antchain.l2.relayer.cli.core.beacon;

import java.math.BigInteger;
import java.util.List;

import cn.hutool.core.util.ObjectUtil;
import com.alipay.antchain.l2.relayer.commons.models.EthBlobs;

public interface IBeaconApiClient {

    EthBlobs queryBlobSidecar(BigInteger slot);

    EthBlobs queryBlobs(BigInteger slot, List<String> versionedHashes);

    default String buildGetBeaconBlobSidecarsByBlockId(String beaconUrl, String param) {
        return String.format("%s/eth/v1/beacon/blob_sidecars/%s", beaconUrl, param);
    }

    default String buildGetBlobsByBlockIdUrl(String beaconUrl, String slot, List<String> versionedHashes) {
        var versionedHashTail = ObjectUtil.isEmpty(versionedHashes) ? ""
                : "?" + versionedHashes.stream().map(x -> "versioned_hashes=" + x).reduce((x, y) -> x + "&" + y).get();
        return String.format("%s/eth/v1/beacon/blobs/%s%s", beaconUrl, slot, versionedHashTail);
    }
}
