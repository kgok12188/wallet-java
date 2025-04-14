package com.tk.wallet.common.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tk.wallet.common.fingerprint.CalcFingerprint;
import com.tk.wallet.common.fingerprint.MD5Util;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * <p>
 * 充值任务
 * </p>
 *
 * @author ${author}
 * @since 2022-09-29
 */
@TableName("wallet_deposit")
@Data
public class WalletDeposit implements Serializable, CalcFingerprint {

    private static final long serialVersionUID = 1L;
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    private Integer uid;
    private Integer walletId;
    private Integer source;
    private String baseSymbol;
    private String symbol;
    private String addressFrom;
    private String addressTo;
    private BigDecimal amount;
    private String txid;
    private Integer confirmations;
    private Integer status;
    private String info;
    private Date ctime;
    private Date mtime;
    private String fingerprint;

    @Override
    public String calcFingerprint(String key) {
        return MD5Util.getMD5(id + "-" + walletId + "-" + baseSymbol + "-" + symbol + "-" + addressFrom + "-" + addressTo
                + "-" + amount.stripTrailingZeros().toPlainString() + "-" + key);
    }

}
