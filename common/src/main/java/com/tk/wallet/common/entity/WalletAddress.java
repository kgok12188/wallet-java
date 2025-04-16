package com.tk.wallet.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tk.wallet.common.fingerprint.CalcFingerprint;
import com.tk.wallet.common.fingerprint.MD5Util;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * <p>
 * 钱包地址
 * </p>
 *
 * @author ${author}
 * @since 2022-09-29
 */
@Data
@TableName("wallet_address")
public class WalletAddress implements Serializable, CalcFingerprint<Integer> {

    private static final long serialVersionUID = 1L;
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    private Integer walletId;
    // "主链币代号，大写字母 TRX，ETH"
    private String baseSymbol;
    private String address;
    // (value = "状态:      0 未使用，1 使用中")
    private Integer useStatus;
    //(value = "创建时间")
    private Date ctime;
    //(value = "更新时间")
    private Date mtime;
    private Long uid;

    private String fingerprint;

    @Override
    public String calcFingerprint(String key) {
        return MD5Util.getMD5(id + "-" + walletId + "-" + baseSymbol + "-" + address + "-" + key);
    }

}
