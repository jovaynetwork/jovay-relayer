package com.alipay.antchain.l2.relayer.core.blockchain.bpo;

import java.math.BigInteger;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EthBpoBlobConfig {

    private static final BigInteger MIN_BLOB_BASE_FEE = BigInteger.ONE;

    @JSONField(name = "name")
    private String name;

    @JSONField(name = "blob_sidecar_version")
    private int blobSidecarVersion;

    @JSONField(name = "base_fee_update_fraction")
    private BigInteger updateFraction;

    /// From web3j [#fakeExponential(BigInteger)] but with BPO `updateFraction`.
    /// @param numerator
    /// @return
    public BigInteger fakeExponential(BigInteger numerator) {
        var i = BigInteger.ONE;
        var output = BigInteger.ZERO;
        var numeratorAccum = MIN_BLOB_BASE_FEE.multiply(updateFraction);
        while (numeratorAccum.compareTo(BigInteger.ZERO) > 0) {
            output = output.add(numeratorAccum);
            numeratorAccum =
                    numeratorAccum
                            .multiply(numerator)
                            .divide(updateFraction.multiply(i));
            i = i.add(BigInteger.ONE);
        }
        return output.divide(updateFraction);
    }
}
