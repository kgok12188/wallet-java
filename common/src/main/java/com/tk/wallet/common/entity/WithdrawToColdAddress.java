package com.tk.wallet.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.util.Date;

/*
*
-- 转冷交易
CREATE TABLE `withdraw_to_cold_address` (
        `id` int(20) unsigned NOT NULL AUTO_INCREMENT,
        `chain` varchar(200) NOT NULL COMMENT '链ID,比如BTC,ETH',
        `from_address` varchar(200) NOT NULL,
        `to_address` varchar(200) NOT NULL,
        contract_address varchar(200) NOT NULL default '',
        `amount` decimal(32,16) unsigned NOT NULL COMMENT '转账金额',
        status int(20) NOT NULL default 0 COMMENT '0 未完成, 1 进行中 2 完成 3 失败' ,
        `business_id` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '充值业务id',
        `hash` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '链上交易hash',
        `fingerprint` varchar(200)  NOT NULL default '';
        ctime DATETIME NOT NULL DEFAULT now(),
        mtime DATETIME NOT NULL DEFAULT now(),
        PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=latin1;
*
* */
@TableName("withdraw_to_cold_address")
public class WithdrawToColdAddress {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    private Integer walletId;
    private String chain;// 链
    private String fromAddress; // from地址
    private String toAddress;// to地址
    private String contractAddress; // 合约地址
    private BigDecimal amount; // 金额
    private Integer status; // 0 初始化, 1 进行中 2 完成 3 失败
    private String businessId;// 业务id
    private String hash; // hash
    private String fingerprint;
    private Date ctime;
    private Date mtime;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getWalletId() {
        return walletId;
    }

    public void setWalletId(Integer walletId) {
        this.walletId = walletId;
    }

    public String getChain() {
        return chain;
    }

    public void setChain(String chain) {
        this.chain = chain;
    }

    public String getFromAddress() {
        return fromAddress;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }

    public String getToAddress() {
        return toAddress;
    }

    public void setToAddress(String toAddress) {
        this.toAddress = toAddress;
    }

    public String getContractAddress() {
        return contractAddress;
    }

    public void setContractAddress(String contractAddress) {
        this.contractAddress = contractAddress;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getBusinessId() {
        return businessId;
    }

    public void setBusinessId(String businessId) {
        this.businessId = businessId;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    public Date getCtime() {
        return ctime;
    }

    public void setCtime(Date ctime) {
        this.ctime = ctime;
    }

    public Date getMtime() {
        return mtime;
    }

    public void setMtime(Date mtime) {
        this.mtime = mtime;
    }
}
