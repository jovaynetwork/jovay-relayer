ALTER TABLE chunks
    ADD COLUMN batch_version INT(8) DEFAULT -1;

SET @batch_version = (SELECT MAX(version)
                      FROM batches);
SET @batch_index = (SELECT `number`
                    FROM rollup_number_record
                    WHERE chain_type = 'LAYER_TWO'
                      AND record_type = 'NEXT_BATCH');
UPDATE chunks
SET batch_version = @batch_version
WHERE batch_index = @batch_index;
