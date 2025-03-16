package com.tk.wallet.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;


public class CoinBalance {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String chainId;
    private String coin;
    private String apiCoin;
    private String address;
    private String contractAddress;
    private BigDecimal balance;
    private BigInteger blockHeight;
    private java.util.Date blockTime;
    private java.util.Date mtime;

    @TableField(exist = false)
    private SymbolConfig coinConfig;


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

    public String getCoin() {
        return coin;
    }

    public void setCoin(String coin) {
        this.coin = coin;
    }

    public String getApiCoin() {
        return apiCoin;
    }

    public void setApiCoin(String apiCoin) {
        this.apiCoin = apiCoin;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getContractAddress() {
        return contractAddress;
    }

    public void setContractAddress(String contractAddress) {
        this.contractAddress = contractAddress;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public BigInteger getBlockHeight() {
        return blockHeight;
    }

    public void setBlockHeight(BigInteger blockHeight) {
        this.blockHeight = blockHeight;
    }

    public Date getBlockTime() {
        return blockTime;
    }

    public void setBlockTime(Date blockTime) {
        this.blockTime = blockTime;
    }

    public Date getMtime() {
        return mtime;
    }

    public void setMtime(Date mtime) {
        this.mtime = mtime;
    }

    public SymbolConfig getCoinConfig() {
        return coinConfig;
    }

    public void setCoinConfig(SymbolConfig coinConfig) {
        this.coinConfig = coinConfig;
    }
}
