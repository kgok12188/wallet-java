package com.tk.wallet.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

/*
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
* */
@TableName("chain_withdraw")
@Data
public class ChainWithdraw {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String chainId;
    private String hash;
    private String transferId;
    private BigDecimal gas;
    private String gasAddress;
    private BigInteger blockHeight = BigInteger.ZERO;
    private Date blockTime;
    private String status;
    private String info;
    private String rowData;
    private String ids;
    private Date mtime;
}
