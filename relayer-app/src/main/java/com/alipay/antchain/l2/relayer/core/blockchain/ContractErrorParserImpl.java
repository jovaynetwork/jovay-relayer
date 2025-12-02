package com.alipay.antchain.l2.relayer.core.blockchain;

import java.util.ArrayList;
import java.util.List;

import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.l2.relayer.commons.abi.AbiCustomerError;
import com.alipay.antchain.l2.relayer.commons.abi.AbiDecoder;

public class ContractErrorParserImpl implements IContractErrorParser {

    private final List<AbiDecoder> decoders = new ArrayList<>();

    public void addContractAbi(String contractName, String abi) {
        decoders.add(new AbiDecoder(contractName, abi));
    }

    @Override
    public AbiCustomerError parse(String hexError) {
        hexError = StrUtil.replace(hexError, "\"", "");
        for (var decoder : decoders) {
            var err = decoder.decodeError(hexError);
            if (err != null) {
                return err;
            }
        }
        return null;
    }
}
