package com.tk.wallet.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tk.wallet.common.fingerprint.CalcFingerprint;
import com.tk.wallet.common.fingerprint.MD5Util;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * <p>
 * 全局币种配置表
 * </p>
 */
// (value = "SymbolConfig对象", description = "全局币种配置表")
@Setter
@Getter
@TableName("symbol_config")
public class SymbolConfig implements Serializable, CalcFingerprint<Integer> {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    private String baseSymbol;
    private String symbol;
    private String tokenSymbol;
    private Integer confirmCount;
    private String contractAddress = "";
    private Integer symbolPrecision;
    private String configJson;
    private Integer status;
    private Date ctime;
    private Date mtime;
    private String fingerprint;

    public BigDecimal precision() {
        return BigDecimal.TEN.pow(symbolPrecision);
    }

    @Override
    public String calcFingerprint(String key) {
        return MD5Util.getMD5(id + "-" + baseSymbol + "-" + symbol + "-" + contractAddress + "-" + key);
    }

}
