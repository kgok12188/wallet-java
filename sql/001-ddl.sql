use wallet;
DROP TABLE IF EXISTS `agg_circuit_breaker`;
CREATE TABLE `agg_circuit_breaker`
(
    `chain_id` varchar(200) NOT NULL COMMENT '链ID,比如BTC,ETH',
    `ctime`    timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `chain_id` (`chain_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3 COMMENT ='归集熔断';

DROP TABLE IF EXISTS `agg_queue`;

CREATE TABLE `agg_queue`
(
    `id`          int unsigned NOT NULL AUTO_INCREMENT,
    `chain_id`    varchar(200) NOT NULL COMMENT '链ID,比如BTC,ETH',
    `wallet_id`   int          NOT NULL DEFAULT '0' COMMENT '商户id',
    `status`      int          NOT NULL DEFAULT '0' COMMENT '0 未完成, 1 进行中 2 完成',
    `ctime`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `symbol_list` varchar(1000)         DEFAULT '' COMMENT '归集币种id',
    PRIMARY KEY (`id`),
    KEY `wallet_id` (`wallet_id`, `id`),
    KEY `status` (`status`, `ctime`)
) ENGINE = InnoDB
  DEFAULT CHARSET = latin1;


DROP TABLE IF EXISTS `agg_task`;
CREATE TABLE `agg_task`
(
    `id`               int unsigned             NOT NULL AUTO_INCREMENT,
    `chain_id`         varchar(200)             NOT NULL COMMENT '链ID,比如BTC,ETH',
    `from_address`     varchar(200)             NOT NULL DEFAULT '' COMMENT '转出地址',
    `to_address`       varchar(200)             NOT NULL DEFAULT '' COMMENT '转入地址',
    `contract_address` varchar(200)             NOT NULL DEFAULT '' COMMENT '合约地址',
    `gas`              decimal(32, 16) unsigned NOT NULL COMMENT '执行交易所需的gas',
    `amount`           decimal(32, 16) unsigned NOT NULL COMMENT '转账金额',
    `type`             int unsigned             NOT NULL COMMENT '任务类型 1 补充燃料 2 归集',
    `mtime`            timestamp                NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `batch_id`         int unsigned             NOT NULL,
    `status`           int                      NOT NULL COMMENT '任务状态 0:等待上链; 1:上链中; 2 完成; 3:余额不足; 4 依赖上游任务',
    `retry_count`      int                      NOT NULL DEFAULT '0' COMMENT '重试次数',
    `run_time`         timestamp                NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
    `contain_coin`     int                      NOT NULL DEFAULT '0' COMMENT '代币转账完成后，是否转账主币 1 包括，0 不包括',
    `wallet_id`        int unsigned             NOT NULL COMMENT '商户id',
    `ctime`            datetime                 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `business_id`      varchar(200)             NOT NULL DEFAULT '',
    PRIMARY KEY (`id`),
    KEY `chain_id` (`chain_id`, `from_address`, `contract_address`),
    KEY `chain_id_2` (`chain_id`, `to_address`, `contract_address`),
    KEY `chain_id_3` (`chain_id`, `from_address`, `batch_id`),
    KEY `chain_id_4` (`chain_id`, `status`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 144
  DEFAULT CHARSET = latin1;
--

DROP TABLE IF EXISTS `agg_task_dependency`;
CREATE TABLE `agg_task_dependency`
(
    `id`             int unsigned NOT NULL AUTO_INCREMENT,
    `task_id`        int unsigned NOT NULL COMMENT '任务id',
    `parent_task_id` int unsigned NOT NULL COMMENT '依赖的父节点',
    PRIMARY KEY (`id`),
    UNIQUE KEY `task_id` (`task_id`, `parent_task_id`),
    KEY `task_id_2` (`task_id`),
    KEY `parent_task_id` (`parent_task_id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = latin1;

DROP TABLE IF EXISTS `chain_events`;
CREATE TABLE `chain_events`
(
    `id`    int unsigned NOT NULL AUTO_INCREMENT,
    `chain` varchar(200) NOT NULL COMMENT '链ID,比如BTC,ETH',
    `type`  int          NOT NULL DEFAULT '0' COMMENT '0 区块高度事件 1 交易事件',
    `info`  text COMMENT '事件内容',
    `ctime` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = latin1;

DROP TABLE IF EXISTS `chain_scan_config`;
CREATE TABLE `chain_scan_config`
(
    `chain_id`             varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '链ID,比如BTC,ETH',
    `endpoints`            text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci         NOT NULL COMMENT 'url 等配置',
    `status`               varchar(5) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci   NOT NULL COMMENT '0 关闭，1 启用',
    `ctime`                timestamp                                                     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `mtime`                timestamp                                                     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `block_number`         varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0' COMMENT '当前扫描到的区块高度',
    `block_height`         varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0' COMMENT '当前链高度',
    `scan_time`            timestamp                                                     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '扫描时间',
    `task_id`              varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '所在机器',
    `task_update_time`     timestamp                                                     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '任务更新时间',
    `un_scan_block`        int                                                           NOT NULL DEFAULT '200' COMMENT '落后区块数量阈值，block_height - block_number > un_scan_block 出现警告日志',
    `delay_blocks`         int                                                           NOT NULL DEFAULT '0' COMMENT '需要延迟扫描的区块,每次扫描高度 : block_height - delay_blocks',
    `block_interval`       int                                                           NOT NULL DEFAULT '0' COMMENT '出块时间，秒',
    `last_block_time`      timestamp                                                     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后一个区块的时间',
    `sign_url`             varchar(200) COLLATE utf8mb4_unicode_ci                                DEFAULT '' COMMENT '发起签名url',
    `address_url`          varchar(200) COLLATE utf8mb4_unicode_ci                                DEFAULT '' COMMENT '获取地址url',
    `address_symbol`       varchar(200) COLLATE utf8mb4_unicode_ci                                DEFAULT '' COMMENT '获取地址的币种',
    `multi_thread`         varchar(10) COLLATE utf8mb4_unicode_ci                                 DEFAULT 'false' COMMENT '是否多线程扫快',
    `multi_thread_numbers` int                                                                    DEFAULT '3' COMMENT '多线程扫快,并发数',
    `retry_interval`       int                                                                    DEFAULT '60' COMMENT '重试间隔,秒',
    `json_config`          text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '自定义配置',
    UNIQUE KEY `unx_chain_id` (`chain_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;


DROP TABLE IF EXISTS `chain_scan_hosts`;
CREATE TABLE `chain_scan_hosts`
(
    `task_id`     varchar(200) NOT NULL COMMENT '服务器hostname-uuid 组成jvm id',
    `update_time` timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `chain_ids`   varchar(200)          default '' COMMENT '扫描的链',
    PRIMARY KEY (`task_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `chain_transaction`;

CREATE TABLE `chain_transaction`
(
    `id`                    int unsigned                                                  NOT NULL AUTO_INCREMENT,
    `chain_id`              varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '链ID,比如BTC,ETH',
    `business_id`           varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci          DEFAULT NULL COMMENT '充值业务id',
    `hash`                  varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci          DEFAULT NULL COMMENT '链上交易hash',
    `gas_address`           varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci          DEFAULT NULL COMMENT '该笔链上交易支付gas的地址',
    `gas_config`            text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '提币配置,json格式,链不同，配置也不同',
    `chain_info`            text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT 'json格式，便于重试',
    `from_address`          varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '转出地址',
    `to_address`            varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '转入地址',
    `contract`              varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci          DEFAULT NULL COMMENT '交易合约，可以为空',
    `amount`                decimal(32, 16) unsigned                                      NOT NULL DEFAULT '0.0000000000000000' COMMENT '交易金额',
    `tx_status`             varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'INIT,WAIT_TO_CHAIN,PENDING （等待确认）,FAIL, SUCCESS',
    `fail_code`             varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci          DEFAULT NULL COMMENT '交易码',
    `message`               text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '交易信息',
    `ctime`                 timestamp                                                     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `mtime`                 timestamp                                                     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `block_time`            timestamp                                                     NULL     DEFAULT CURRENT_TIMESTAMP COMMENT '区块时间',
    `need_confirm_num`      int                                                           NOT NULL DEFAULT '0' COMMENT '需要的确认数',
    `block_num`             int                                                           NOT NULL DEFAULT '0' COMMENT '当前交易所在区块',
    `consumer_flag`         int                                                           NOT NULL DEFAULT '0' COMMENT '通知事件 1 必须通知,失败重试',
    `url_code`              varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci          DEFAULT NULL COMMENT '链ID,比如BTC,ETH',
    `priority`              int                                                           NOT NULL DEFAULT '0' COMMENT '转账优先级，相同的from_address 该值越高，交易顺序越靠前',
    `auto_speed_up`         int                                                           NOT NULL DEFAULT '0' COMMENT '加速确认 1 自动加速，0 不加速',
    `token_symbol`          varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci          DEFAULT NULL COMMENT '币种符号',
    `symbol`                varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci          DEFAULT NULL COMMENT '币种符号',
    `transfer_block_number` int                                                                    DEFAULT NULL COMMENT '发起交易时的区块高度',
    `nonce`                 int                                                                    DEFAULT '0' COMMENT '发起交易序号',
    `error_count`           int                                                                    DEFAULT '0' COMMENT '交易失败次数',
    PRIMARY KEY (`id`),
    UNIQUE KEY `business_id` (`business_id`),
    KEY `tx_status` (`tx_status`, `chain_id`, `ctime`),
    KEY `hash` (`hash`),
    KEY `chain_id_block_time` (`chain_id`, `block_time`),
    KEY `idx_from_address_nonce` (`from_address`, `nonce`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 0
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;


-- 对chain_transaction发起的交易，批量上链
DROP TABLE IF EXISTS `chain_withdraw`;

CREATE TABLE `chain_withdraw`
(
    `id`           int unsigned             NOT NULL AUTO_INCREMENT,
    `chain_id`     varchar(200)             NOT NULL COMMENT '链ID,比如BTC,ETH',
    `hash`         varchar(100) COMMENT '交易hash',
    `transfer_id`  varchar(100) COMMENT '根据链特性，没有hash的情况交易去重',
    `gas`          decimal(32, 16) unsigned NOT NULL COMMENT 'gas',
    `gas_address`  varchar(200)             NOT NULL COMMENT 'gas地址',
    `block_height` bigint(20)               NOT NULL COMMENT '区块高度',
    `block_time`   timestamp                NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '区块时间',
    `status`       varchar(200)             not null COMMENT '状态 同chain_transaction',
    `info`         text COMMENT '交易信息',
    `row_data`     text COMMENT '离线签名信息',
    `ids`          text comment 'chain_transaction的id列表,格式[1,2,3,4]',
    `mtime`        timestamp                NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uidx_hash` (`hash`),
    UNIQUE KEY `uidx_transfer_id` (`transfer_id`),
    key `idx_status` (`status`),
    KEY `idx_gas_address` (`gas_address`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 0
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci comment '对chain_transaction发起的交易，批量上链';



DROP TABLE IF EXISTS `coin_balance`;

CREATE TABLE `coin_balance`
(
    `id`               int unsigned             NOT NULL AUTO_INCREMENT,
    `chain_id`         varchar(200)             NOT NULL COMMENT '链ID,比如BTC,ETH',
    `coin`             varchar(32)              NOT NULL COMMENT '加密货币 TRX、USDT',
    `api_coin`         varchar(32)              NOT NULL DEFAULT '' COMMENT '交互币种 比如USDT-erc20, TUSDT',
    `address`          varchar(200)             NOT NULL COMMENT '地址',
    `contract_address` varchar(200)             NOT NULL DEFAULT '' COMMENT '合约地址',
    `balance`          decimal(38, 19) unsigned NOT NULL COMMENT '余额',
    `block_height`     bigint(20)               NOT NULL COMMENT '区块高度',
    `block_time`       timestamp                NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '区块时间',
    `mtime`            timestamp                NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `chain_id` (`chain_id`, `contract_address`, `address`),
    KEY `address` (`address`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 474
  DEFAULT CHARSET = latin1;

DROP TABLE IF EXISTS `coin_config`;

DROP TABLE IF EXISTS `scan_chain_queue`;
CREATE TABLE `scan_chain_queue`
(
    `id`           int unsigned NOT NULL AUTO_INCREMENT,
    `chain_id`     varchar(200) NOT NULL COMMENT '链ID,比如BTC,ETH',
    `block_number` int          NOT NULL DEFAULT '0' COMMENT '扫描的区块高度',
    `status`       int          NOT NULL DEFAULT '0' COMMENT '0 未完成, 1 进行中 2 完成',
    `ctime`        datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `chain_id` (`chain_id`, `block_number`),
    KEY `status` (`status`, `ctime`)
) ENGINE = InnoDB
  DEFAULT CHARSET = latin1;

DROP TABLE IF EXISTS `scan_chain_status`;
CREATE TABLE `scan_chain_status`
(
    `id`         int unsigned NOT NULL AUTO_INCREMENT,
    `chain_id`   varchar(200) NOT NULL COMMENT '链ID,比如BTC,ETH',
    `block_time` datetime              DEFAULT NULL COMMENT '区块时间',
    `status`     int          NOT NULL DEFAULT '0' COMMENT '0 进行中 1 完成',
    PRIMARY KEY (`id`),
    UNIQUE KEY `chain_id` (`chain_id`, `block_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = latin1;

DROP TABLE IF EXISTS `symbol_config`;
CREATE TABLE `symbol_config`
(
    `id`               int unsigned     NOT NULL AUTO_INCREMENT,
    `base_symbol`      varchar(32)      NOT NULL COMMENT '货币代号，大写字母 TRX，ETH',
    `symbol`           varchar(32)      NOT NULL COMMENT '加密货币 trx ,usdt_trc20',
    `confirm_count`    int              NOT NULL COMMENT '确认次数',
    `contract_address` varchar(200)     NOT NULL DEFAULT '' COMMENT '合约地址',
    `symbol_precision` int                       DEFAULT NULL COMMENT '币种精度',
    `status`           tinyint unsigned NOT NULL DEFAULT '0' COMMENT '币种状态 0 未使用 1 使用中',
    `config_json`      text,
    `ctime`            timestamp        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `mtime`            timestamp        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    `token_symbol`     varchar(200)              DEFAULT '' COMMENT '链上币种名称',
    PRIMARY KEY (`id`),
    unique key `uidx_base_symbol_contract` (`base_symbol`, `contract_address`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 11
  DEFAULT CHARSET = utf8mb3 COMMENT ='币种配置表';

DROP TABLE IF EXISTS `wallet_address`;
CREATE TABLE `wallet_address`
(
    `id`          int unsigned     NOT NULL AUTO_INCREMENT,
    `wallet_id`   int              NOT NULL,
    `uid`         bigint(20)                default 0,
    `base_symbol` varchar(32)      NOT NULL COMMENT '主链币代号，大写字母 TRX，ETH',
    `address`     varchar(200)     NOT NULL DEFAULT '' COMMENT '到账地址',
    `use_status`  tinyint                   DEFAULT NULL,
    `type`        tinyint unsigned NOT NULL DEFAULT '0' COMMENT '钱包类型  0 普通钱包[只有这个需要归集] 1 燃料包 2 归集钱包 3 发币钱包',
    `ctime`       timestamp        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `mtime`       timestamp        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_address` (`address`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 126
  DEFAULT CHARSET = utf8mb3 COMMENT ='钱包地址';

alter table wallet_address
    add unique index `base_symbol_address` (`base_symbol`, `address`);


DROP TABLE IF EXISTS `wallet_symbol_config`;
CREATE TABLE `wallet_symbol_config`
(
    `id`                 int unsigned             NOT NULL AUTO_INCREMENT,
    `wallet_id`          int                      NOT NULL COMMENT '钱包id',
    `symbol_config_id`   int                      NOT NULL COMMENT 'symbol_config的id',
    `agg_address`        varchar(200)             NOT NULL DEFAULT '' COMMENT '归集地址，归集到该地址',
    `withdraw_address`   varchar(200)             NOT NULL DEFAULT '' COMMENT '提现地址，从该地址提现',
    `energy_address`     varchar(200)             NOT NULL DEFAULT '' COMMENT '能量地址，从该地址发出能量',
    `config_json`        varchar(400)             NOT NULL DEFAULT '' COMMENT '自定义配置',
    `check_police`       tinyint unsigned         NOT NULL DEFAULT '0' COMMENT '提现策略： 0 无需审核 1 手动审核 2 自动策略模式1',
    `agg_police`         tinyint unsigned         NOT NULL DEFAULT '0' COMMENT '归集策略： 手动归集 自定归集模式1 ',
    `status`             tinyint unsigned         NOT NULL DEFAULT '0' COMMENT '币种状态 0 未使用 1 使用中',
    `ctime`              timestamp                NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `mtime`              timestamp                NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    `agg_min_amount`     decimal(32, 16) unsigned NOT NULL DEFAULT '0.0000000000000000' COMMENT '最小归集金额',
    `to_cold_threshold`  decimal(38, 19) unsigned NOT NULL DEFAULT '0.0000000000000000000' COMMENT '转冷阈值',
    `to_cold_min_amount` decimal(38, 19) unsigned NOT NULL DEFAULT '0.0000000000000000000' COMMENT '转冷最小值',
    `cold_address`       varchar(200)             NOT NULL DEFAULT '',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uidx_wallet_id_symbol_config_id` (`wallet_id`, `symbol_config_id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb3 COMMENT ='商户币种配置表';

DROP TABLE IF EXISTS `withdraw_to_cold_address`;
CREATE TABLE `withdraw_to_cold_address`
(
    `id`               int unsigned             NOT NULL AUTO_INCREMENT,
    `chain`            varchar(200)             NOT NULL COMMENT '链ID,比如BTC,ETH',
    `wallet_id`        int unsigned             NOT NULL,
    `from_address`     varchar(200)             NOT NULL,
    `to_address`       varchar(200)             NOT NULL,
    `contract_address` varchar(200)             NOT NULL                             DEFAULT '',
    `amount`           decimal(32, 16) unsigned NOT NULL COMMENT '转账金额',
    `status`           int                      NOT NULL                             DEFAULT '0' COMMENT '0 未完成, 1 进行中 2 完成 3 失败',
    `business_id`      varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '充值业务id',
    `hash`             varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '链上交易hash',
    `ctime`            datetime                 NOT NULL                             DEFAULT CURRENT_TIMESTAMP,
    `mtime`            datetime                 NOT NULL                             DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uidx_business_id` (`business_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = latin1;



DROP TABLE IF EXISTS `wallet_withdraw`;
CREATE TABLE `wallet_withdraw`
(
    `id`            int(11) unsigned         NOT NULL AUTO_INCREMENT,
    `trans_id`      varchar(1000)            NOT NULL COMMENT 'ex的业务id，唯一限制',
    `base_symbol`   varchar(32)              NOT NULL COMMENT '货币代号，大写字母 TRX，ETH',
    `symbol`        varchar(32)              NOT NULL COMMENT '加密货币 trx ,usdt_trc20',
    `amount`        decimal(32, 16) unsigned NOT NULL DEFAULT '0.0000000000000000' COMMENT '提现金额',
    `address_from`  varchar(200)             NOT NULL DEFAULT '' COMMENT '提现from地址',
    `address_to`    varchar(200)             NOT NULL DEFAULT '' COMMENT '提现to地址',
    `txid`          varchar(128)             NOT NULL DEFAULT '' COMMENT '区块链交易ID',
    `confirmations` int(19)                  NOT NULL DEFAULT '0' COMMENT '区块确认数',
    `status`        tinyint(3) unsigned      NOT NULL DEFAULT '0' COMMENT '订单状态:0 提现审核中 1 审核不通过 2 审核成功 3 提现中 4 提现成功 5 提现失败 6 通知ex上账成功 7 通知ex上账失败',
    `info`          varchar(200)             NOT NULL DEFAULT '' COMMENT '描述',
    `ctime`         timestamp                NOT NULL DEFAULT now() COMMENT '创建时间',
    `mtime`         timestamp                NOT NULL DEFAULT now() COMMENT '更新时间',
    `uid`           bigint(20)               NOT NULL DEFAULT '0' COMMENT '商家子用户',
    `wallet_id`     int(20)                  NOT NULL DEFAULT '0' COMMENT '商户uid',
    notice_status   tinyint(3) unsigned      NOT NULL DEFAULT '0' COMMENT '通知状态:0 未通知 1 已通知',
    PRIMARY KEY (`id`),
    UNIQUE KEY `unx_trans_id` (`trans_id`),
    KEY `idx_to_address` (`address_from`),
    KEY `idx_txid` (`txid`),
    KEY `idx_created_at` (`ctime`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8 COMMENT ='提现任务';


use wallet;

DROP TABLE IF EXISTS `wallet_user`;
CREATE TABLE `wallet_user`
(
    `id`        int(11) unsigned NOT NULL primary key AUTO_INCREMENT,
    callbackUrl varchar(200)     NOT NULL DEFAULT '',
    ctime       timestamp        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    mtime       timestamp        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8 COMMENT ='商户表';



DROP TABLE IF EXISTS `wallet_deposit`;
CREATE TABLE `wallet_deposit`
(
    `id`            INT(11) UNSIGNED         NOT NULL AUTO_INCREMENT,
    `uid`           INT(11) UNSIGNED                  default 0,
    `wallet_id`     INT(11) UNSIGNED         NOT NULL,
    `base_symbol`   VARCHAR(32)              NOT NULL COMMENT '货币代号，大写字母 TRX，ETH',
    `symbol`        VARCHAR(32)              NOT NULL COMMENT '加密货币 trx ,usdt_trc20',
    `contract`      VARCHAR(200)             NOT NULL DEFAULT '' COMMENT '合约地址',
    `address_to`    VARCHAR(200)             NOT NULL DEFAULT '' COMMENT '到账地址',
    `amount`        DECIMAL(32, 16) UNSIGNED NOT NULL COMMENT '充值金额',
    `txid`          VARCHAR(128)             NOT NULL DEFAULT '' COMMENT '区块链交易ID',
    `transfer_id`   VARCHAR(200)             NOT NULL COMMENT '扫描交易获得的全局唯一id',
    `confirmations` BIGINT(19) UNSIGNED               DEFAULT 0 COMMENT '确认数',
    `status`        TINYINT(3) UNSIGNED      NOT NULL DEFAULT 0 COMMENT '充值状态: 0 充值中，1 充值成功，2 充值失败',
    `notice_status` TINYINT(3) UNSIGNED      NOT NULL DEFAULT 0 COMMENT '通知状态:0 未通知 1 已通知',
    `info`          VARCHAR(200)             NOT NULL DEFAULT '' COMMENT '描述',
    `ctime`         TIMESTAMP                NOT NULL DEFAULT now() COMMENT '创建时间',
    `mtime`         TIMESTAMP                NOT NULL DEFAULT now() COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uid` (`transfer_id`),
    KEY `idx_to_address` (`address_to`),
    KEY `idx_txid` (`txid`),
    KEY `idx_created_at` (`ctime`)
) ENGINE = INNODB
  AUTO_INCREMENT = 5
  DEFAULT CHARSET = utf8 COMMENT ='充值任务';


alter table `agg_task`
    add column `fingerprint` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '数据指纹';
alter table `chain_transaction`
    add column `fingerprint` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '数据指纹';
alter table `wallet_address`
    add column `fingerprint` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '数据指纹';
alter table `wallet_deposit`
    add column `fingerprint` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '数据指纹';
alter table `wallet_symbol_config`
    add column `fingerprint` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '数据指纹';
alter table `wallet_withdraw`
    add column `fingerprint` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '数据指纹';
alter table `withdraw_to_cold_address`
    add column `fingerprint` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '数据指纹';
alter table `symbol_config`
    add column `fingerprint` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '数据指纹';