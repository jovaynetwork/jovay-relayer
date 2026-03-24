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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.alipay.antchain.l2.relayer.commons.utils.Utils;
import lombok.Getter;
import org.apache.tuweni.bytes.Bytes;
import org.web3j.crypto.Blob;
import org.web3j.crypto.BlobUtils;
import org.web3j.crypto.Sign;
import org.web3j.crypto.transaction.type.Transaction4844;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpString;
import org.web3j.rlp.RlpType;
import org.web3j.utils.Numeric;

public class FusakaTransaction4844 extends Transaction4844 {

    @Getter
    private final byte version = 1;
    private final List<Bytes> versionedHashes;
    private final Optional<List<Blob>> blobs;
    private final Optional<List<Bytes>> kzgProofs;
    private final Optional<List<Bytes>> kzgCommitments;

    public FusakaTransaction4844(
            List<Blob> blobs,
            long chainId,
            BigInteger nonce,
            BigInteger maxPriorityFeePerGas,
            BigInteger maxFeePerGas,
            BigInteger gasLimit,
            String to,
            BigInteger value,
            String data,
            BigInteger maxFeePerBlobGas
    ) {
        super(null, null, null, chainId, nonce, maxPriorityFeePerGas, maxFeePerGas, gasLimit, to, value, data, maxFeePerBlobGas, null);
        this.blobs = Optional.ofNullable(blobs);
        assert blobs != null;
        this.kzgCommitments =
                Optional.of(
                        blobs.stream()
                                .map(BlobUtils::getCommitment)
                                .collect(Collectors.toList()));
        var proofArr = new ArrayList<Bytes>();blobs.stream().map(Utils::getCellProofs).forEach(proofArr::addAll);
        this.kzgProofs = Optional.of(proofArr);
        this.versionedHashes =
                this.kzgCommitments.get().stream()
                        .map(BlobUtils::kzgToVersionedHash)
                        .collect(Collectors.toList());
    }

    public FusakaTransaction4844(
            List<Blob> blobs,
            List<Bytes> kzgCommitments,
            List<Bytes> kzgProofs,
            long chainId,
            BigInteger nonce,
            BigInteger maxPriorityFeePerGas,
            BigInteger maxFeePerGas,
            BigInteger gasLimit,
            String to,
            BigInteger value,
            String data,
            BigInteger maxFeePerBlobGas,
            List<Bytes> versionedHashes
    ) {
        super(blobs, kzgCommitments, kzgProofs, chainId, nonce, maxPriorityFeePerGas, maxFeePerGas, gasLimit, to, value, data, maxFeePerBlobGas, versionedHashes);
        this.versionedHashes = versionedHashes;
        this.blobs = Optional.ofNullable(blobs);
        this.kzgCommitments = Optional.ofNullable(kzgCommitments);
        this.kzgProofs = Optional.ofNullable(kzgProofs);
    }

    @Override
    public List<RlpType> asRlpValues(Sign.SignatureData signatureData) {
        List<RlpType> resultTx = new ArrayList<>();

        resultTx.add(RlpString.create(getChainId()));
        resultTx.add(RlpString.create(getNonce()));
        resultTx.add(RlpString.create(getMaxPriorityFeePerGas()));
        resultTx.add(RlpString.create(getMaxFeePerGas()));
        resultTx.add(RlpString.create(getGasLimit()));

        String to = getTo();
        if (to != null && !to.isEmpty()) {
            resultTx.add(RlpString.create(Numeric.hexStringToByteArray(to)));
        } else {
            resultTx.add(RlpString.create(""));
        }

        resultTx.add(RlpString.create(getValue()));
        byte[] data = Numeric.hexStringToByteArray(getData());
        resultTx.add(RlpString.create(data));

        // access list
        resultTx.add(new RlpList());

        // Blob Transaction: max_fee_per_blob_gas and versioned_hashes
        resultTx.add(RlpString.create(getMaxFeePerBlobGas()));
        resultTx.add(new RlpList(getRlpVersionedHashes()));

        if (signatureData != null) {
            resultTx.add(RlpString.create(Sign.getRecId(signatureData, getChainId())));
            resultTx.add(
                    RlpString.create(
                            org.web3j.utils.Bytes.trimLeadingZeroes(signatureData.getR())));
            resultTx.add(
                    RlpString.create(
                            org.web3j.utils.Bytes.trimLeadingZeroes(signatureData.getS())));
        }

        List<RlpType> result = new ArrayList<>();
        result.add(new RlpList(resultTx));

        // Adding blobs, commitments, and proofs
        result.add(RlpString.create(version));
        result.add(new RlpList(getRlpBlobs()));
        result.add(new RlpList(getRlpKzgCommitments()));
        result.add(new RlpList(getRlpKzgProofs()));

        return result;
    }

    @Override
    public Optional<List<Blob>> getBlobs() {
        return blobs;
    }

    @Override
    public Optional<List<Bytes>> getKzgCommitments() {
        return kzgCommitments;
    }

    @Override
    public Optional<List<Bytes>> getKzgProofs() {
        return kzgProofs;
    }

    @Override
    public List<Bytes> getVersionedHashes() {
        return versionedHashes;
    }

    @Override
    public List<RlpType> getRlpVersionedHashes() {
        return versionedHashes.stream()
                .map(hash -> RlpString.create(hash.toArray()))
                .collect(Collectors.toList());
    }

    @Override
    public List<RlpType> getRlpKzgCommitments() {
        return kzgCommitments
                .<List<RlpType>>map(
                        bytesList ->
                                bytesList.stream()
                                        .map(bytes -> RlpString.create(bytes.toArray()))
                                        .collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    @Override
    public List<RlpType> getRlpKzgProofs() {
        return kzgProofs
                .<List<RlpType>>map(
                        bytesList ->
                                bytesList.stream()
                                        .map(bytes -> RlpString.create(bytes.toArray()))
                                        .collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    @Override
    public List<RlpType> getRlpBlobs() {
        return blobs.<List<RlpType>>map(
                        blobList ->
                                blobList.stream()
                                        .map(blob -> RlpString.create(blob.getData().toArray()))
                                        .collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }
}
