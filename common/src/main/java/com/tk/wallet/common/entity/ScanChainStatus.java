package com.tk.wallet.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;


@Setter
@Getter
public class ScanChainStatus {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String chainId;
    private java.util.Date blockTime;
    private Integer status;

}
