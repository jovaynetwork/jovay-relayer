CREATE TABLE IF NOT EXISTS `system_config`
(
    `id`           INT(11) NOT NULL AUTO_INCREMENT,
    `conf_key`     VARCHAR(128)   DEFAULT NULL,
    `conf_value`   VARCHAR(15000) DEFAULT NULL,
    `gmt_create`   DATETIME       DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` DATETIME       DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `conf_key` (`conf_key`)
);

CREATE TABLE IF NOT EXISTS `active_node`
(
    `id`               INT(11) PRIMARY KEY NOT NULL AUTO_INCREMENT,
    `node_id`          VARCHAR(64) DEFAULT NULL,
    `node_ip`          VARCHAR(64) DEFAULT NULL,
    `last_active_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    `status`           VARCHAR(16)         NOT NULL,
    `gmt_create`       DATETIME    DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified`     DATETIME    DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_node` (`node_id`)
);

CREATE TABLE IF NOT EXISTS `batches`
(
    `id`                      INT(11) PRIMARY KEY NOT NULL AUTO_INCREMENT,
    `version`                 INT(8)              NOT NULL,
    `batch_header_hash`       VARCHAR(66)         NOT NULL,
    `batch_index`             BIGINT UNSIGNED     NOT NULL,
    `l1_message_popped`       INT(11)             NOT NULL,
    `total_l1_message_popped` INT(11)             NOT NULL,
    `data_hash`               VARCHAR(66)         NOT NULL,
    `parent_batch_hash`       VARCHAR(66)         NOT NULL,
    `post_state_root`         VARCHAR(66)         NOT NULL,
    `l2_msg_root`             VARCHAR(66)         NOT NULL,
    `l1msg_rolling_hash`      VARCHAR(66)         NOT NULL,
    `start_number`            VARCHAR(64)         NOT NULL,
    `end_number`              VARCHAR(64)         NOT NULL,
    `chunk_num`               INT(11)             NOT NULL,
    `gmt_create`              DATETIME DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified`            DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `batches-idx-hdr-hash` (`batch_header_hash`),
    UNIQUE KEY `batches-idx-batch-index` (`batch_index`)
);

CREATE TABLE IF NOT EXISTS `batch_prove_request`
(
    `id`           INT(11) PRIMARY KEY NOT NULL AUTO_INCREMENT,
    `batch_index`  BIGINT UNSIGNED     NOT NULL,
    `prove_type`   VARCHAR(32)         NOT NULL,
    `proof`        LONGBLOB,
    `state`        VARCHAR(32)         NOT NULL,
    `gmt_create`   DATETIME DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY `prove-req-idx-state` (`state`),
    KEY `prove-req-idx-bidx-type` (`batch_index`, `prove_type`)
);

CREATE TABLE IF NOT EXISTS `biz_task`
(
    `id`              INT(11) PRIMARY KEY NOT NULL AUTO_INCREMENT,
    `node_id`         VARCHAR(64)         NOT NULL,
    `task_type`       VARCHAR(32)         NOT NULL,
    `start_timestamp` DATETIME(3)         NOT NULL,
    `gmt_create`      DATETIME DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified`    DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `biz_task_idx-node-id-task-type` (`node_id`, `task_type`)
);

CREATE TABLE IF NOT EXISTS `chunks`
(
    `id`           INT(11) PRIMARY KEY NOT NULL AUTO_INCREMENT,
    `batch_index`  BIGINT UNSIGNED     NOT NULL,
    `chunk_index`  INT(11)             NOT NULL,
    `chunk_hash`   VARCHAR(64)         NOT NULL,
    `num_blocks`   INT(11)             NOT NULL,
    `zk_cycle_sum` BIGINT UNSIGNED     NOT NULL,
    `start_number` VARCHAR(64)         NOT NULL,
    `end_number`   VARCHAR(64)         NOT NULL,
    `raw_chunk`    LONGBLOB,
    `gmt_create`   DATETIME DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `chunks-idx-bidx-cidx` (`batch_index`, `chunk_index`),
    KEY `chunks-chunk_hash` (`chunk_hash`)
);

CREATE TABLE IF NOT EXISTS `reliable_transaction`
(
    `id`                  INT(11) PRIMARY KEY NOT NULL AUTO_INCREMENT,
    `chain_type`          VARCHAR(32)         NOT NULL,
    `transaction_type`    VARCHAR(32)         NOT NULL,
    `batch_index`         BIGINT UNSIGNED     NOT NULL,
    `sender_account`      VARCHAR(66)         NOT NULL,
    `nonce`               BIGINT UNSIGNED     NOT NULL,
    `original_tx_hash`    VARCHAR(66)         NOT NULL,
    `latest_tx_hash`      VARCHAR(66)         NOT NULL,
    `raw_tx`              LONGBLOB            NOT NULL,
    `latest_tx_send_time` DATETIME(3)         NOT NULL,
    `state`               VARCHAR(32)         NOT NULL,
    `retry_count`         INT(11)      DEFAULT 0,
    `revert_reason`       varchar(255) DEFAULT NULL,
    `gmt_create`          DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified`        DATETIME     DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `reliable_transaction-idx-tx-per-batch` (`chain_type`, `transaction_type`, `batch_index`),
    UNIQUE KEY `reliable_transaction-idx-original_tx_hash` (`original_tx_hash`),
    UNIQUE KEY `reliable_transaction-idx-latest_tx_hash` (`latest_tx_hash`),
    KEY `reliable_transaction-idx-state` (`state`)
);

CREATE TABLE IF NOT EXISTS `rollup_number_record`
(
    `id`           INT(11) PRIMARY KEY NOT NULL AUTO_INCREMENT,
    `chain_type`   VARCHAR(32)         NOT NULL,
    `record_type`  VARCHAR(32)         NOT NULL,
    `number`       VARCHAR(64)         NOT NULL,
    `gmt_create`   DATETIME DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `rollup_number_record-idx-1` (`chain_type`, `record_type`)
);

CREATE TABLE IF NOT EXISTS `inter_bc_msg`
(
    `id`                  INT PRIMARY KEY NOT NULL AUTO_INCREMENT,
    `type`                VARCHAR(32)     NOT NULL,
    `batch_index`         BIGINT UNSIGNED,
    `msg_hash`            VARCHAR(66)     NOT NULL,
    `source_block_height` VARCHAR(64)     NOT NULL,
    `source_tx_hash`      VARCHAR(66)     NOT NULL,
    `sender`              VARCHAR(66)     NOT NULL,
    `receiver`            VARCHAR(66)     NOT NULL,
    `nonce`               BIGINT UNSIGNED NOT NULL,
    `raw_message`         LONGBLOB        NOT NULL,
    `proof`               LONGBLOB,
    `state`               VARCHAR(32)     NOT NULL,
    `gmt_create`          DATETIME DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified`        DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `inter_bc_msg-idx-1` (`msg_hash`),
    UNIQUE KEY `inter_bc_msg-idx-2` (`type`, `nonce`),
    KEY `inter_bc_msg-idx-3` (`type`, `state`),
    KEY `inter_bc_msg-idx-4` (`source_tx_hash`),
    KEY `inter_bc_msg-idx-5` (`type`, `batch_index`)
);

CREATE TABLE IF NOT EXISTS `l2_merkle_tree`
(
    `id`             INT PRIMARY KEY NOT NULL AUTO_INCREMENT,
    `branch`         LONGBLOB        NOT NULL,
    `batch_index`    BIGINT UNSIGNED NOT NULL,
    `next_msg_nonce` BIGINT UNSIGNED NOT NULL,
    `gmt_create`     DATETIME DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified`   DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `l2_merkle_tree-idx-1` (`batch_index`)
);


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