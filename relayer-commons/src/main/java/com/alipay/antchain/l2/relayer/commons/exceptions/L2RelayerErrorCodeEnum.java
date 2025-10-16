package com.alipay.antchain.l2.relayer.commons.exceptions;

import lombok.Getter;

/**
 * Error code for {@code l2-relayer}
 *
 * <p>
 *     The {@code errorCode} field supposed to be hex and has two bytes.
 *     First byte represents the space code for project.
 *     Last byte represents the specific error scenarios.
 * </p>
 *
 */
@Getter
public enum L2RelayerErrorCodeEnum {

    DAL_ANCHOR_HEIGHTS_ERROR("0101", "wrong heights state"),

    DAL_RELAYER_NETWORK_ERROR("0105", "relayer net data error"),

    DAL_RELAYER_NODE_ERROR("0106", "relayer node data error"),

    DAL_DT_ACTIVE_NODE_ERROR("0107", "dt active node data error"),

    DAL_DT_TASK_ERROR("0108", "distributed task data error"),

    DAL_SYSTEM_CONFIG_ERROR("010a", "sys config data error"),

    CORE_BLOCKCHAIN_ERROR("0201", "blockchain error"),

    CORE_BLOCKCHAIN_CLIENT_INIT_ERROR("0202", "blockchain client init error"),

    CORE_PROVER_CLIENT_INIT_ERROR("0203", "prover client init error"),

    CORE_REMOTE_SERVICE_ERROR("0204", "remote service error"),

    CORE_BATCH_INVALID("0205", "invalid batch"),

    NO_NEED_TO_SPEED_UP_TX("0206", "no need to speed up tx"),

    CORE_CHUNK_INVALID("0207", "invalid chunk"),

    SPEED_UP_OVER_LIMIT("0208", "speed up over limit"),

    CURR_TX_NOT_NEXT_NONCE("0209", "curr tx is not next nonce"),

    PREVIOUS_TX_NOT_READY("020a", "previous tx not ready"),

    INVALID_ROLLUP_SPECS("020b", "invalid rollup specs"),

    L1_GAS_PRICE_TOO_HIGH("020c", "l1 gas price too high"),

    SPEEDUP_TX_ASYNC("020d", "other thread speedup same tx now"),

    SERVICE_BLOCK_POLLING_ERROR("0301", "block polling error"),

    COMMIT_L2_BATCH_ERROR("0302", "batch commit error"),

    L2_BATCH_NOT_READY("0303", "batch not ready"),

    L2_BATCH_COMMIT_FAILED("0304", "batch commit failed"),

    BATCH_PROOF_COMMIT_FAILED("0305", "batch proof commit failed"),

    BREAK_ORACLE_REQUEST("0306", "break oracle request process"),

    INIT_MAILBOX_SERVICE_ERROR("0401", "init mailbox service error"),

    MAILBOX_SERVICE_FETCH_L1MSG_ERROR("0402", "fetch l1 message error"),

    MAILBOX_SERVICE_PROCESS_L1MSG_ERROR("0403", "process l1 message error"),

    ROLLUP_SEND_TX_ERROR("1000", "send rollup tx error"),

    CALL_WITH_WARNING("1001", "can not care this exception"),

    CALL_WITH_INVALID_PERMISSION("1002", "check caller permission is relayer"),

    CALL_WITH_INVALID_PARAMETER("1003", "call with invalid parameter more info in revert reason"),

    CALL_WITH_SERIOUS_ERROR("1004", "SERIOUS MISTAKE!!! more info in revert reason"),

    ORACLE_SEND_TX_ERROR("1005", "send oracle gas feed tx error"),

    TX_NOT_FOUND_BUT_RETRY("1006", "l2 tx not found but we need to retry the query"),
    /**
     *
     */
    UNKNOWN_INTERNAL_ERROR("0001", "internal error");

    /**
     * Error code for errors happened in project {@code antchain-bridge-relayer}
     */
    private final String errorCode;

    /**
     * Every code has a short message to describe the error stuff
     */
    private final String shortMsg;

    L2RelayerErrorCodeEnum(String errorCode, String shortMsg) {
        this.errorCode = errorCode;
        this.shortMsg = shortMsg;
    }
}
