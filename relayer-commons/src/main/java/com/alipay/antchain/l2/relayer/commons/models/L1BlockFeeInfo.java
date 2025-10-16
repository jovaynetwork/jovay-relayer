package com.alipay.antchain.l2.relayer.commons.models;

import lombok.*;
import org.web3j.utils.Numeric;

import java.math.BigInteger;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class L1BlockFeeInfo {
    private String number;

    private String baseFeePerGas;

    private String gasUsed;

    private String gasLimit;

    private String blobGasUsed;

    private String excessBlobGas;

    public BigInteger getNumber() {
        return Numeric.decodeQuantity(number);
    }

    public String getNumberRaw() {
        return number;
    }

}
