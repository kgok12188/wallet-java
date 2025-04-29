package com.tk.wallet.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tk.wallet.common.fingerprint.CalcFingerprint;
import com.tk.wallet.common.fingerprint.MD5Util;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

@TableName("chain_transaction")
@Data
public class ChainTransaction implements CalcFingerprint<Long> {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    // 链id,BTC,ETH 等
    private String chainId;
    // 业务id,提交充值
    private String businessId;
    // 链上交易hash
    private String hash;
    // 手续费支付地址
    private String gasAddress;
    // 交易手续费, 需要调用 com.chainup.wallet.api.chain.support.BlockChain.gas 方法，得到的结果保存到 gas 字段
    private BigDecimal gas;
    // 实际手续费
    private BigDecimal actGas;
    //提币配置, 发起提现需要将 waas.coin_config的config_json 填充到这个字段
    private String gasConfig;
    // json格式，便于重试
    private String chainInfo;

    private String fromAddress;

    private String toAddress;
    // 合约地址
    private String contract;
    // 交易金额
    private BigDecimal amount;
    //INIT,WAIT_TO_CHAIN,PENDING （等待确认）,FAIL, SUCCESS
    private String txStatus;
    //
    private String failCode;
    //
    private String message;
    //
    private Date ctime;
    //
    private Date mtime;
    // 区块
    private Date blockTime;
    // 确认数
    private int needConfirmNum;
    // 交易所在区块
    private BigInteger blockNum;
    // 通知事件是否执行
    private int consumerFlag;

    private String urlCode;

    private int priority;
    // 1 可以自动加速确认交易
    private int autoSpeedUp;
    // 币种符号
    private String symbol;//BEP20-USDT,ERC20-USDT
    private String tokenSymbol; // USDT,USDC

    //发起交易时的区块高度
    private BigInteger transferBlockNumber;

    // 推送交易失败次数
    private Integer errorCount;


    private String fingerprint;

    @Override
    public String calcFingerprint(String key) {
        return MD5Util.getMD5(id + "-" + chainId + "-" + (StringUtils.isBlank(businessId) ? "" : businessId) + "-" + fromAddress + "-" + toAddress + "-" + (StringUtils.isBlank(contract) ? "" : contract) + "-" + amount.stripTrailingZeros().toPlainString() + "-" + key);
    }

    private BigInteger nonce;
    @TableField(exist = false)
    private BigInteger coinBalance;
    @TableField(exist = false)
    private BigInteger tokenBalance;
    @TableField(exist = false)
    private BigInteger transferGas;
    @TableField(exist = false)
    private BigInteger transferPrice;
    @TableField(exist = false)
    private BigInteger limit;

    // private String memo;

    public enum TX_STATUS {
        INIT, WAITING_HASH, WAIT_TO_CHAIN, PENDING, FAIL, SUCCESS
    }

    public enum FAIL_CODE {
        CHAIN_NOT_FOUND,  // 链上没有找到交易
        OUT_OF_GAS,      // 链上交易失败
        BALANCE_NOT_ENOUGH, // 发起交易校验，余额不足
        GAS_NOT_ENOUGH     // 发起交易交易，燃料不足
    }

    public boolean success() {
        return StringUtils.equals(txStatus, TX_STATUS.SUCCESS.name());
    }

    public boolean pending() {
        return StringUtils.equals(txStatus, TX_STATUS.PENDING.name());
    }

    public boolean fail() {
        return StringUtils.equals(txStatus, TX_STATUS.FAIL.name());
    }

    public boolean init() {
        return StringUtils.equals(txStatus, TX_STATUS.INIT.name());
    }

    public boolean waitingHash() {
        return StringUtils.equals(txStatus, TX_STATUS.WAITING_HASH.name());
    }

}