package com.tk.wallet.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;

import java.util.Date;
import java.util.Objects;

/*
*
drop table if exists agg_queue;
CREATE TABLE `agg_queue` (
    `id` int(20) unsigned NOT NULL AUTO_INCREMENT,
    `chain_id` varchar(200) NOT NULL COMMENT '链ID,比如BTC,ETH',
    `wallet_id` int(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 0 COMMENT '扫描的区块高度',
    status int(20) NOT NULL default 0 COMMENT '0 未完成, 1 进行中 2 完成' ,
    ctime DATETIME NOT NULL DEFAULT now(),
    PRIMARY KEY (`id`),
    key(wallet_id,id),
    key(status,ctime)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=latin1;
*
* */
public class AggQueue {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String chainId;
    private Integer walletId;
    private Integer status;
    private java.util.Date ctime;
    private String symbolList; // symbol_config 的 id 列表，逗号隔开

    public boolean isPending() {
        return Objects.equals(status, 1);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getChainId() {
        return chainId;
    }

    public void setChainId(String chainId) {
        this.chainId = chainId;
    }

    public Integer getWalletId() {
        return walletId;
    }

    public void setWalletId(Integer walletId) {
        this.walletId = walletId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Date getCtime() {
        return ctime;
    }

    public void setCtime(Date ctime) {
        this.ctime = ctime;
    }

    public String getSymbolList() {
        return symbolList;
    }

    public void setSymbolList(String symbolList) {
        this.symbolList = symbolList;
    }
}
