package com.alipay.antchain.l2.relayer.dal.entities;

import java.math.BigInteger;
import java.util.Arrays;

import com.alibaba.fastjson.annotation.JSONField;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.web3j.abi.datatypes.generated.Bytes32;

@Getter
@Setter
@TableName("l2_merkle_tree")
@NoArgsConstructor
@AllArgsConstructor
public class L2MerkleTreeEntity extends BaseEntity {

    @TableField("branch")
    private byte[] branches;

    @TableField("batch_index")
    private BigInteger batchIndex;

    @TableField("next_msg_nonce")
    private BigInteger nextMsgNonce;

    @JSONField(serialize = false, deserialize = false)
    public Bytes32[] toBranches() {
        Bytes32[] result = new Bytes32[branches.length / 32];
        for (int i = 0; i < branches.length / 32; i++) {
            result[i] = new Bytes32(Arrays.copyOfRange(branches, i * 32, (i + 1) * 32));
        }
        return result;
    }
}
