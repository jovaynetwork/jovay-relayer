ALTER TABLE reliable_transaction
    ADD INDEX `reliable_transaction-idx-chain_type-sender-nonce` (chain_type, sender_account, nonce);