package com.alipay.antchain.l2.relayer.commons.abi;

import java.util.Map;
import java.util.stream.Collectors;

import com.esaulpaugh.headlong.abi.ABIJSON;
import com.esaulpaugh.headlong.abi.ContractError;
import com.esaulpaugh.headlong.abi.Tuple;
import com.esaulpaugh.headlong.util.Strings;
import lombok.extern.slf4j.Slf4j;
import org.web3j.utils.Numeric;

@Slf4j
public class AbiDecoder {

    private final String contractName;

    private final Map<String, ContractError<Tuple>> errorMap;

    public AbiDecoder(String contractName, String abi) {
        this.contractName = contractName;
        this.errorMap = ABIJSON.parseErrors(abi).stream()
                .collect(Collectors.toMap(e -> e.function().selectorHex(), e -> e));
    }

    public AbiCustomerError decodeError(String code) {
        try {
            code = Numeric.cleanHexPrefix(code);
            if (code.length() < 8) {
                return null;
            }
            var error = errorMap.get(code.substring(0, 8));
            if (error == null) {
                return null;
            }
            var function = error.function();
            var tuple = function.decodeCall(Strings.decode(code));
            return new AbiCustomerError(contractName, error, tuple);
        } catch (Throwable t) {
            log.warn("decodeError failed for {}", code, t);
            return null;
        }
    }
}
