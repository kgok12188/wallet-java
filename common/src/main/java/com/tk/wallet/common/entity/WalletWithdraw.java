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
 * 提现任务
 * </p>
 */
@Setter
@Getter
@TableName("wallet_withdraw")
public class WalletWithdraw implements Serializable, CalcFingerprint<Long> {

    // 订单状态:0 提现审核中 1 审核不通过 2 审核成功 3 提现中 4 提现成功 5 提现失败
    public enum Status {
        INIT,
        AUDIT_FAIL,
        AUDIT_SUCCESS,
        WITHDRAWING,
        SUCCESS,
        FAIL
    }

    private static final long serialVersionUID = 1L;
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Integer walletId;
    private Long uid = 0L;
    private String transId;
    private String baseSymbol;
    private String symbol;
    private BigDecimal amount;
    private String addressFrom;
    private String addressTo;
    private String txid;
    private Integer confirmations;
    // 订单状态:0 提现审核中 1 审核不通过 2 审核成功 3 提现中 4 提现成功 5 提现失败
    private Integer status;
    private Integer noticeStatus; // 0 未通知 1 已通知
    private String info;
    private Date ctime;
    private Date mtime;
    private String fingerprint;

    @Override
    public String calcFingerprint(String key) {
        return MD5Util.getMD5(id + "-" + walletId + "-" + uid + "-" + baseSymbol + "-" + symbol + "-" + addressFrom + "-" + addressTo + "-" + transId + "-" + key);
    }

}
