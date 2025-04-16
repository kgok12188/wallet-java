package com.tk.wallet.common.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tk.wallet.common.fingerprint.CalcFingerprint;
import com.tk.wallet.common.fingerprint.MD5Util;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

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
public class WalletDeposit implements Serializable, CalcFingerprint<Integer> {


    public enum STATUS {
        INIT,
        SUCCESS,
        FAIL
    }

    public enum NotifyStatus {
        INIT,
        SUCCESS,
        FAIL
    }

    private static final long serialVersionUID = 1L;
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    private Integer uid;
    private Integer walletId;
    private String baseSymbol;
    private String symbol;
    private String contract;
    private String addressTo; // 到账地址
    private BigDecimal amount;
    private String txid;
    private Integer confirmations;
    private Integer status; // 充值状态: 0 充值中，1 充值成功，2 充值失败
    private Integer noticeStatus; // 通知状态:0 未通知 1 通知成功 2 通知失败（会一直重试通知）
    private String info;
    private String transferId; // chain_transaction的表id
    private Date ctime;
    private Date mtime;
    private String fingerprint;

    @Override
    public String calcFingerprint(String key) {
        return MD5Util.getMD5("wallet_deposit" + transferId + "-" + id + "-" + walletId + "-" + baseSymbol + "-" + (StringUtils.isBlank(contract) ? "" : contract) + "-" + addressTo
                + "-" + amount.stripTrailingZeros().toPlainString() + "-" + key);
    }

}
