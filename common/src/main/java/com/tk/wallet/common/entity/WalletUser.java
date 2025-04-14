package com.tk.wallet.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * <p>
 * 钱包账号
 * </p>
 */

@TableName("wallet_user") // 商户
@Data
public class WalletUser implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    //(value = "通知上账地址")
    private String callbackUrl;

    //(value = "创建时间")
    private Date ctime;

    //(value = "更新时间")
    private Date mtime;


}
