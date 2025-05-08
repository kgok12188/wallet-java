package com.tk.wallet.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.google.common.collect.Lists;
import com.tk.wallet.common.fingerprint.CalcFingerprint;
import com.tk.wallet.common.fingerprint.MD5Util;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;


@TableName("agg_task")
@Data
public class AggTask implements CalcFingerprint<Long> {

    // 任务状态 0:等待上链; 1:上链中; 2 完成; 3:余额不足; 4 依赖上游任务 5 失败
    public static Integer STATUS_WAIT_TO_CHAIN = 0;
    public static Integer STATUS_PENDING = 1;
    public static Integer STATUS_SUCCESS = 2;
    public static Integer STATUS_NOT_INSUFFICIENT = 3;
    public static Integer STATUS_WAIT_PARENT_JOB = 4;
    public static Integer STATUS_FAIL = 5;
    // 任务类型 1 补充燃料 2 归集
    public static Integer TYPE_AGG = 1;
    public static Integer TYPE_GAS = 2;

    @TableId(type = IdType.AUTO)
    private Long id;
    private String chainId; // 链
    private Integer walletId;
    private String fromAddress;
    private String toAddress; //
    private String contractAddress; // 合约地址
    // 需要补充的gas
    // private BigDecimal gas; // 需要补充的gas
    // 主币有余额，补充差额gas
    private BigDecimal amount; // 金额
    private Integer type; // 任务类型 1 补充燃料 2 归集
    private java.util.Date ctime; // 创建时间
    private java.util.Date mtime; // 更新时间
    private Integer status; // 0:等待上链; 1:上链中; 2 完成; 3:余额不足; 4 依赖上游任务 5 失败
    private Integer retryCount; // 重试次数
    private java.util.Date runTime; // 运行时间
    // 批量归集id
    private Long batchId;
    private Integer containCoin;

    private String businessId;

    private String fingerprint;

    public static final List<Integer> notEndList = Lists.newArrayList(AggTask.STATUS_PENDING, AggTask.STATUS_WAIT_TO_CHAIN, AggTask.STATUS_WAIT_PARENT_JOB);
    public static final List<Integer> EndList = Lists.newArrayList(AggTask.STATUS_SUCCESS, AggTask.STATUS_FAIL);

    @Override
    public String calcFingerprint(String key) {
        return MD5Util.getMD5(id + "-" + chainId + "-" + businessId + "-" + fromAddress + "-" + toAddress + "-" + contractAddress + "-" + amount.stripTrailingZeros().toPlainString() + "-" + key);
    }

}
