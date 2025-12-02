package com.alipay.antchain.l2.relayer.commons.abi;

import com.esaulpaugh.headlong.abi.ContractError;
import com.esaulpaugh.headlong.abi.Tuple;
import org.web3j.utils.Numeric;

public record AbiCustomerError(String contractName, ContractError<Tuple> error, Tuple parameters) {

    public String getErrorName() {
        return error.getName();
    }

    public String getReason() {
        return error.getName() + ":" + toString(parameters);
    }

    public String toString(Tuple tuple) {
        StringBuilder sb = new StringBuilder("[");
        if (!tuple.isEmpty()) {
            for (Object o : tuple) {
                parse(sb, o);
                sb.append(", ");
            }
            sb.setLength(sb.length() - 2);
        }
        sb.append("]");
        return sb.toString();
    }

    private void parse(StringBuilder sb, Object element) {
        if (element instanceof byte[]) {
            sb.append(Numeric.toHexString((byte[]) element));
        } else if (element instanceof Tuple) {
            sb.append(toString((Tuple) element));
        } else {
            String str = element.toString();
            sb.append(element instanceof String
                    ? '"' + str + '"'
                    : "_".equals(str)
                    ? "\\_"
                    : element);
        }
    }
}
