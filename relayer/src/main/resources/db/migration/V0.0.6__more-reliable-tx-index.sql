DELIMITER //
CREATE PROCEDURE CreateReliableTxChainTypeAndStateIndexIfNotExists()
BEGIN
    DECLARE indexExists INT;
    SELECT COUNT(1) INTO indexExists
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'reliable_transaction'
      AND INDEX_NAME = 'reliable_transaction-idx-chain_type-state';

    IF indexExists = 0 THEN
        ALTER TABLE reliable_transaction ADD INDEX `reliable_transaction-idx-chain_type-state` (chain_type, state);
    END IF;
END //
DELIMITER ;

CALL CreateReliableTxChainTypeAndStateIndexIfNotExists();
DROP PROCEDURE IF EXISTS CreateReliableTxChainTypeAndStateIndexIfNotExists;