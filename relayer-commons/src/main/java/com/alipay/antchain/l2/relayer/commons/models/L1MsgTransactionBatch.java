package com.alipay.antchain.l2.relayer.commons.models;

import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alipay.antchain.l2.relayer.commons.enums.InterBlockchainMessageStateEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class L1MsgTransactionBatch {

    private List<L1MsgTransactionInfo> l1MsgTransactionInfos;

    private BigInteger height;

    public List<InterBlockchainMessageDO> toInterBlockchainMessages() {
        if (ObjectUtil.isEmpty(l1MsgTransactionInfos)) {
            return ListUtil.empty();
        }
        return l1MsgTransactionInfos.stream().sorted()
                .map(x -> InterBlockchainMessageDO.fromL1MsgTx(
                        x.getSourceBlockHeight(), x.getSourceTxHash(), x.getL1MsgTransaction()
                )).peek(x -> x.setState(InterBlockchainMessageStateEnum.MSG_READY))
                .collect(Collectors.toList());
    }
}
