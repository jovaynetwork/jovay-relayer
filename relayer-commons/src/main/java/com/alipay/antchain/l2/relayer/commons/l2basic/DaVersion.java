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

package com.alipay.antchain.l2.relayer.commons.l2basic;

import cn.hutool.core.lang.Assert;
import lombok.AllArgsConstructor;

/**
 * Caution that value of {@code DaVersion} never greater than or equal to number 115.
 * According to the {@link ethereum.ckzg4844.CKZG4844JNI#BLS_MODULUS}.
 */
@AllArgsConstructor
public enum DaVersion {

    /**
     * <p>Version zero means these below points.</p>
     * <ol>
     *     <li>The 0.7.0 original way to deal with blobs, and only serialized chunks inside.</li>
     *     <li>Only batch version zero goes through with DA zero</li>
     * </ol>
     */
    DA_0((byte) 0),

    /**
     * <p>
     *     Version one requires new layouts for blobs. And has no compression for payload.
     *     If batch version ge 1 and no need for payload compression, use this da version.
     * </p>
     * <ol>
     *     <li>From 0.8.0, batch version which greater than or equal to one goes through with DA one or two, it depends on
     *     if compression is efficient. </li>
     * </ol>
     *
     * <pre>
     * Field               Bytes       Type         Index   Comments
     * batch_version       1           uint8        0       The version of the batch
     * n_bytes[1:3]        3           uint24       1       Value denoting the number of payload bytes
     * payload             N           bytes        4       Uncompressed payload bytes
     * </pre>
     */
    DA_1((byte) 1),

    /**
     * <p>
     *     Version two requires new layouts for blobs. And has compression for payload.
     *     If batch version ge 1 and need for payload compression, use this da version.
     * </p>
     * <ol>
     *     <li>From 0.8.0, batch version which greater than or equal to one goes through with DA one or two, it depends on
     *     if compression is efficient. </li>
     * </ol>
     *
     * <pre>
     * Field               Bytes       Type         Index   Comments
     * batch_version       1           uint8        0       The version of the batch
     * n_bytes[1:3]        3           uint24       1       Value denoting the number of payload bytes
     * payload             N           bytes        4       Zstd-encoded payload bytes
     * </pre>
     */
    DA_2((byte) 2);

    public static DaVersion from(byte value) {
        Assert.isTrue(value < 115, "Can't be greater than the first byte of BLS modulus");
        for (DaVersion version : DaVersion.values()) {
            if (version.value == value) {
                return version;
            }
        }
        throw new IllegalArgumentException("invalid da version: " + value);
    }

    private final byte value;

    public byte toByte() {
        return value;
    }

    public boolean isCompressed() {
        return this.value > 1;
    }
}
