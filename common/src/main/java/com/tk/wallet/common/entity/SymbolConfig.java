package com.tk.wallet.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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
public class SymbolConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    private String baseSymbol;
    private String symbol;
    private String tokenSymbol;
    private Integer confirmCount;
    private String contractAddress;
    private Integer symbolPrecision;
    private String configJson;
    private Integer status;
    private Date ctime;
    private Date mtime;

    public BigDecimal precision() {
        return BigDecimal.TEN.pow(symbolPrecision);
    }

}
