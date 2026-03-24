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
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Comparator;
import java.util.List;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alipay.antchain.l2.relayer.commons.models.EthBlobs;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.web3j.crypto.Blob;
import org.web3j.utils.Numeric;

@Slf4j
public class BeaconApiClient implements IBeaconApiClient {

    private final HttpClient httpClient;

    private final String beaconRpcUrl;

    public BeaconApiClient(String beaconRpcUrl) {
        this.httpClient = HttpClient.newBuilder().build();
        this.beaconRpcUrl = beaconRpcUrl;
    }

    @Override
    @SneakyThrows
    public EthBlobs queryBlobSidecar(BigInteger slot) {
        var resp = httpClient.send(
                HttpRequest.newBuilder()
                        .GET()
                        .uri(URI.create(buildGetBeaconBlobSidecarsByBlockId(beaconRpcUrl, slot.toString())))
                        .header("accept", "application/json")
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
        switch (resp.statusCode()) {
            case 200 -> {
                var body = JSON.parseObject(resp.body(), ObjectAndMetaData.class);
                if (ObjectUtil.isNull(body.getData())) {
                    throw new RuntimeException(StrUtil.format("unexpected http resp body: {}", resp.body()));
                }
                if (!body.getFinalized()) {
                    log.info("note that block is not finalized, slot: {}", slot);
                }
                var rawArr = JSON.parseArray(body.getData(), RawBlob.class);
                if (ObjectUtil.isEmpty(rawArr)) {
                    throw new RuntimeException(StrUtil.format("none blobs found, slot: {}", slot));
                }
                rawArr.sort(Comparator.comparingInt(RawBlob::getIndex));
                return new EthBlobs(rawArr.stream().map(x -> Numeric.hexStringToByteArray(x.getBlob())).map(Blob::new).toList());
            }
            case 404 -> {
                log.warn("block not found, slot: {}, msg: {}", slot, resp.body());
                return null;
            }
            default ->
                    throw new RuntimeException(StrUtil.format("get beacon block request failed, slot: {}, msg: {}", slot, resp.body()));
        }
    }

    @Override
    @SneakyThrows
    public EthBlobs queryBlobs(BigInteger slot, List<String> versionedHashes) {
        var resp = httpClient.send(
                HttpRequest.newBuilder()
                        .GET()
                        .uri(URI.create(buildGetBlobsByBlockIdUrl(beaconRpcUrl, slot.toString(), versionedHashes)))
                        .header("accept", "application/json")
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
        switch (resp.statusCode()) {
            case 200 -> {
                var body = JSON.parseObject(resp.body(), ObjectAndMetaData.class);
                if (ObjectUtil.isNull(body.getData())) {
                    throw new RuntimeException(StrUtil.format("unexpected http resp body: {}", resp.body()));
                }
                if (!body.getFinalized()) {
                    log.info("note that block is not finalized, slot: {}", slot);
                }
                var rawArr = JSON.parseArray(body.getData(), String.class);
                if (ObjectUtil.isEmpty(rawArr)) {
                    throw new RuntimeException(StrUtil.format("none blobs found, slot: {}", slot));
                }
                return new EthBlobs(rawArr.stream().map(Numeric::hexStringToByteArray).map(Blob::new).toList());
            }
            case 404 -> {
                log.warn("block not found, slot: {}, msg: {}", slot, resp.body());
                return null;
            }
            default ->
                    throw new RuntimeException(StrUtil.format("get beacon block request failed, slot: {}, msg: {}", slot, resp.body()));
        }
    }
}
