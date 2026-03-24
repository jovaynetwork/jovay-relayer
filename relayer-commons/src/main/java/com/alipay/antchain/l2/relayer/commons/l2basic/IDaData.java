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

/**
 * Data Availability (DA) data interface representing the DA layer data structure.
 * <p>
 * This interface defines the contract for DA data objects that encapsulate
 * batch data submitted to L1 for data availability. It includes version information,
 * data length, hash, and methods to convert to batch payload.
 * </p>
 */
public interface IDaData {

    /**
     * Get the DA version.
     * <p>
     * This indicates the version of the data availability format being used.
     * </p>
     *
     * @return the DA version
     */
    DaVersion getDaVersion();

    /**
     * Get the batch version.
     * <p>
     * This indicates the version of the batch format being used.
     * </p>
     *
     * @return the batch version enum
     */
    BatchVersionEnum getBatchVersion();

    /**
     * Get the length of the data.
     *
     * @return the data length in bytes
     */
    int getDataLen();

    /**
     * Calculate and return the hash of the data.
     * <p>
     * This computes a cryptographic hash of the DA data for integrity verification.
     * </p>
     *
     * @return the data hash bytes
     */
    byte[] dataHash();

    /**
     * Convert this DA data to a batch payload.
     * <p>
     * This deserializes or transforms the DA data into a batch payload object
     * for further processing.
     * </p>
     *
     * @return the batch payload
     */
    IBatchPayload toBatchPayload();
}
