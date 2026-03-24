CREATE TABLE IF NOT EXISTS `oracle_request`
(
    `id`               INT PRIMARY KEY NOT NULL AUTO_INCREMENT,
    `oracle_type`      VARCHAR(32)     NOT NULL,
    `oracle_task_type` VARCHAR(32)     NOT NULL,
    `request_index`    BIGINT UNSIGNED NOT NULL,
    `raw_data`         LONGBLOB        NOT NULL,
    `tx_state`         VARCHAR(32),
    `gmt_create`       DATETIME DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified`     DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `oracle_request-idx-task` (`oracle_type`, `oracle_task_type`, `request_index`),
    KEY `oracle_request-state` (`tx_state`)
);