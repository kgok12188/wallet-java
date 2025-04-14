package com.tk.wallet.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tk.wallet.common.fingerprint.CalcFingerprint;
import com.tk.wallet.common.fingerprint.MD5Util;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Objects;

/**
 * <p>
 * 商户币种配置表
 * </p>
 */
@TableName("wallet_symbol_config")
@Data
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
        return MD5Util.getMD5(coldAddress + "-" + walletId + "-" + symbolConfigId + "-" + aggAddress + "-" + energyAddress + "-" + key);
    }

    @Override
    public String getFingerprint() {
        return fingerprint;
    }

    @Override
    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    public boolean autoUpChain() {
        return Objects.equals(checkPolice, 0);
    }

}
