package com.tk.wallet.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;


@Setter
@Getter
@Data
public class CoinBalance {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String chainId;
    private String coin;
    private String apiCoin;
    private String address;
    private String contractAddress;
    private BigDecimal balance;
    private BigInteger blockHeight;
    private java.util.Date blockTime;
    private java.util.Date mtime;

    @TableField(exist = false)
    private SymbolConfig coinConfig;


}
