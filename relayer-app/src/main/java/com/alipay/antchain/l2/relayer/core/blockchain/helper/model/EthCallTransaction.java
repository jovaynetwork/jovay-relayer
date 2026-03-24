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

package com.alipay.antchain.l2.relayer.core.blockchain.helper.model;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import org.apache.tuweni.bytes.Bytes;
import org.web3j.crypto.transaction.type.Transaction4844;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.utils.Numeric;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
public class EthCallTransaction extends Transaction {

    public static EthCallTransaction createEthCallTransaction(String from, Transaction4844 transaction4844) {
        return new EthCallTransaction(from, transaction4844);
    }

    private BigInteger maxFeePerBlobGas;

    private List<String> blobVersionedHashes;

    private List<String> blobs;

    private List<String> commitments;

    private List<String> proofs;

    public EthCallTransaction(String from, Transaction4844 transaction4844) {
        super(from, null, null, null, transaction4844.getTo(), null, transaction4844.getData());
        this.maxFeePerBlobGas = null;
        this.blobVersionedHashes = transaction4844.getVersionedHashes().stream().map(Bytes::toHexString).toList();
        this.blobs = transaction4844.getBlobs().orElse(new ArrayList<>()).stream().map(x -> x.getData().toHexString()).toList();
        this.commitments = transaction4844.getKzgCommitments().orElse(new ArrayList<>()).stream().map(Bytes::toHexString).toList();
        this.proofs = transaction4844.getKzgProofs().orElse(new ArrayList<>()).stream().map(Bytes::toHexString).toList();
    }

    public String getMaxFeePerBlobGas() {
        return convert(maxFeePerBlobGas);
    }

    private static String convert(BigInteger value) {
        if (value != null) {
            return Numeric.encodeQuantity(value);
        } else {
            return null; // we don't want the field to be encoded if not present
        }
    }
}
