CREATE TABLE IF NOT EXISTS `inter_bc_msg`
(
    `id`                  INT PRIMARY KEY NOT NULL AUTO_INCREMENT,
    `type`                VARCHAR(32)     NOT NULL,
    `batch_index`         BIGINT UNSIGNED,
    `msg_hash`            VARCHAR(66),
    `source_block_height` VARCHAR(64),
    `source_tx_hash`      VARCHAR(66),
    `sender`              VARCHAR(66),
    `receiver`            VARCHAR(66),
    `nonce`               BIGINT UNSIGNED NOT NULL,
    `raw_message`         LONGBLOB,
    `proof`               LONGBLOB,
    `state`               VARCHAR(32),
    `gmt_create`          DATETIME DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified`        DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `inter_bc_msg-idx-1` (`msg_hash`),
    UNIQUE KEY `inter_bc_msg-idx-2` (`type`, `nonce`),
    KEY `inter_bc_msg-idx-3` (`type`, `state`),
    KEY `inter_bc_msg-idx-4` (`source_tx_hash`),
    KEY `inter_bc_msg-idx-5` (`type`, `batch_index`)
);

CREATE TABLE IF NOT EXISTS `batches`
(
    `id`                      INT(11) PRIMARY KEY NOT NULL AUTO_INCREMENT,
    `version`                 INT(8),
    `batch_header_hash`       VARCHAR(66),
    `batch_index`             BIGINT UNSIGNED,
    `l1_message_popped`       INT(11),
    `total_l1_message_popped` INT(11),
    `data_hash`               VARCHAR(66),
    `parent_batch_hash`       VARCHAR(66),
    `post_state_root`         VARCHAR(66),
    `l2_msg_root`             VARCHAR(66),
    `l1msg_rolling_hash`      VARCHAR(66),
    `start_number`            VARCHAR(64),
    `end_number`              VARCHAR(64),
    `chunk_num`               INT(11),
    `gmt_create`              DATETIME DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified`            DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `batches-idx-hdr-hash` (`batch_header_hash`),
    UNIQUE KEY `batches-idx-batch-index` (`batch_index`)
);