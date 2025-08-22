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