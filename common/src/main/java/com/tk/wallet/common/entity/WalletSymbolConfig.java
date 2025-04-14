package com.tk.wallet.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tk.wallet.common.fingerprint.CalcFingerprint;
import com.tk.wallet.common.fingerprint.MD5Util;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Objects;

/**
 * <p>
 * 商户币种配置表
 * </p>
 */
@TableName("wallet_symbol_config")
public class WalletSymbolConfig implements CalcFingerprint {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    //(value = "钱包id")
    private Integer walletId;

    //(value = "全局配置id")
    private Integer symbolConfigId;


    //(value = "归集地址，归集到该地址")
    private String aggAddress;


    //(value = "能量地址，从该地址发出能量")
    private String energyAddress;


    //(value = "自定义配置")
    private String configJson;

    //(value = "提现策略： 0 无需审核 1 手动审核 ")
    private Integer checkPolice;


    //(value = "币种状态 0 未使用 1 使用中")
    private Integer status;

    //(value = "归集策略： 0 自动归集 1 手动归集 ")
    private Integer aggPolice;

    //(value = "最小归集金额")
    private BigDecimal aggMinAmount;

    private BigDecimal toColdThreshold; // 转冷钱包阈值

    private BigDecimal toColdMinAmount; // 转冷钱包,最低金额

    //(value = "冷钱包地址")
    private String coldAddress;

    private String fingerprint;


    //(value = "创建时间")
    private Date ctime;

    //(value = "更新时间")
    private Date mtime;

    @Override
    public String calcFingerprint(String key) {
        return MD5Util.getMD5(coldAddress + "-" + walletId + "-" + symbolConfigId + "-" + aggAddress + "-" + energyAddress);
    }

    @Override
    public String getFingerprint() {
        return fingerprint;
    }

    @Override
    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }


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

    public Integer getSymbolConfigId() {
        return symbolConfigId;
    }

    public void setSymbolConfigId(Integer symbolConfigId) {
        this.symbolConfigId = symbolConfigId;
    }


    public String getAggAddress() {
        return aggAddress;
    }

    public void setAggAddress(String aggAddress) {
        this.aggAddress = aggAddress;
    }

    public String getEnergyAddress() {
        return energyAddress;
    }

    public void setEnergyAddress(String energyAddress) {
        this.energyAddress = energyAddress;
    }

    public String getConfigJson() {
        return configJson;
    }

    public void setConfigJson(String configJson) {
        this.configJson = configJson;
    }

    public Integer getCheckPolice() {
        return checkPolice;
    }

    public void setCheckPolice(Integer checkPolice) {
        this.checkPolice = checkPolice;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getAggPolice() {
        return aggPolice;
    }

    public void setAggPolice(Integer aggPolice) {
        this.aggPolice = aggPolice;
    }

    public BigDecimal getAggMinAmount() {
        return aggMinAmount;
    }

    public void setAggMinAmount(BigDecimal aggMinAmount) {
        this.aggMinAmount = aggMinAmount;
    }

    public BigDecimal getToColdThreshold() {
        return toColdThreshold;
    }

    public void setToColdThreshold(BigDecimal toColdThreshold) {
        this.toColdThreshold = toColdThreshold;
    }

    public BigDecimal getToColdMinAmount() {
        return toColdMinAmount;
    }

    public void setToColdMinAmount(BigDecimal toColdMinAmount) {
        this.toColdMinAmount = toColdMinAmount;
    }

    public String getColdAddress() {
        return coldAddress;
    }

    public void setColdAddress(String coldAddress) {
        this.coldAddress = coldAddress;
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

    public boolean autoUpChain() {
        return Objects.equals(checkPolice, 0);
    }

}
