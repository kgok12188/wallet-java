package com.tk.wallet.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * <p>
 * 全局币种配置表
 * </p>
 *
 * @author ${author}
 * @since 2022-09-30
 */
// (value = "SymbolConfig对象", description = "全局币种配置表")
public class SymbolConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    //(value = "主链")
    private String baseSymbol;

    //(value = "apiName  TUSDT,EUSDT")
    private String symbol;

    //(value = "链上的币种名称")
    private String tokenSymbol;

    //(value = "确认次数限制")
    private Integer confirmCount;

    //(value = "合约地址：主币没有")
    private String contractAddress;

    //(value = "币种精度")
    private Integer symbolPrecision;


    //(value = "自定义配置")
    private String configJson;


    //(value = "币种状态 0 未使用 1 使用中")
    private Integer status;

    //(value = "创建时间")
    private Date ctime;

    //(value = "更新时间")
    private Date mtime;


    public BigDecimal precision() {
        String decimal = "1";
        for (int i = 0; i < symbolPrecision; i++) {
            decimal = decimal + "0";
        }
        return new BigDecimal(decimal);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getBaseSymbol() {
        return baseSymbol;
    }

    public void setBaseSymbol(String baseSymbol) {
        this.baseSymbol = baseSymbol;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getTokenSymbol() {
        return tokenSymbol;
    }

    public void setTokenSymbol(String tokenSymbol) {
        this.tokenSymbol = tokenSymbol;
    }

    public Integer getConfirmCount() {
        return confirmCount;
    }

    public void setConfirmCount(Integer confirmCount) {
        this.confirmCount = confirmCount;
    }

    public String getContractAddress() {
        return contractAddress;
    }

    public void setContractAddress(String contractAddress) {
        this.contractAddress = contractAddress;
    }

    public Integer getSymbolPrecision() {
        return symbolPrecision;
    }

    public void setSymbolPrecision(Integer symbolPrecision) {
        this.symbolPrecision = symbolPrecision;
    }

    public String getConfigJson() {
        return configJson;
    }

    public void setConfigJson(String configJson) {
        this.configJson = configJson;
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

    public Date getMtime() {
        return mtime;
    }

    public void setMtime(Date mtime) {
        this.mtime = mtime;
    }


}
