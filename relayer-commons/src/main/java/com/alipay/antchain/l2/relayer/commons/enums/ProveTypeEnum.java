package com.alipay.antchain.l2.relayer.commons.enums;

import com.alipay.antchain.l2.prover.common.ProverType;

public enum ProveTypeEnum {

    TEE_PROOF,

    ZK_PROOF;

    public ProverType getProverType() {
        switch (this) {
            case TEE_PROOF:
                return ProverType.TEE;
            case ZK_PROOF:
                return ProverType.ZK;
            default:
                throw new IllegalArgumentException("unexpected here");
        }
    }

    public int getRollupProofNum() {
        switch (this) {
            case TEE_PROOF:
                return 1;
            case ZK_PROOF:
                return 0;
            default:
                throw new IllegalArgumentException("unexpected here");
        }
    }
}
