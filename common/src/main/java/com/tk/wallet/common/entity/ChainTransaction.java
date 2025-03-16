package com.tk.wallet.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

@TableName("chain_transaction")
public class ChainTransaction {
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
    private String coin;

    // 币种符号
    @TableField(exist = false)
    private String apiCoin; // 交易所对接的
    //发起交易时的区块高度
    private BigInteger transferBlockNumber;

    // 推送交易失败次数
    private Integer errorCount;

//    public static final String TX_STATUS_INIT = "INIT";
//    public static final String TX_STATUS_WAIT_TO_CHAIN = "WAIT_TO_CHAIN";
//    public static final String TX_STATUS_PENDING = "PENDING";
//    public static final String TX_STATUS_FAIL = "FAIL";
//    public static final String TX_STATUS_SUCCESS = "SUCCESS";

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getChainId() {
        return chainId;
    }

    public void setChainId(String chainId) {
        this.chainId = chainId;
    }

    public String getBusinessId() {
        return businessId;
    }

    public void setBusinessId(String businessId) {
        this.businessId = businessId;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getGasAddress() {
        return gasAddress;
    }

    public void setGasAddress(String gasAddress) {
        this.gasAddress = gasAddress;
    }

    public BigDecimal getGas() {
        return gas;
    }

    public void setGas(BigDecimal gas) {
        this.gas = gas;
    }

    public BigDecimal getActGas() {
        return actGas;
    }

    public void setActGas(BigDecimal actGas) {
        this.actGas = actGas;
    }

    public String getGasConfig() {
        return gasConfig;
    }

    public void setGasConfig(String gasConfig) {
        this.gasConfig = gasConfig;
    }

    public String getChainInfo() {
        return chainInfo;
    }

    public void setChainInfo(String chainInfo) {
        this.chainInfo = chainInfo;
    }

    public String getFromAddress() {
        return fromAddress;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }

    public String getToAddress() {
        return toAddress;
    }

    public void setToAddress(String toAddress) {
        this.toAddress = toAddress;
    }

    public String getContract() {
        return contract;
    }

    public void setContract(String contract) {
        this.contract = contract;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getTxStatus() {
        return txStatus;
    }

    public void setTxStatus(String txStatus) {
        this.txStatus = txStatus;
    }

    public String getFailCode() {
        return failCode;
    }

    public void setFailCode(String failCode) {
        this.failCode = failCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Date getCtime() {
        return ctime;
    }

    public void setCtime(Date ctime) {
        this.ctime = ctime;
    }

    public Date getMtime() {
        return mtime;
    }

    public void setMtime(Date mtime) {
        this.mtime = mtime;
    }

    public Date getBlockTime() {
        return blockTime;
    }

    public void setBlockTime(Date blockTime) {
        this.blockTime = blockTime;
    }

    public int getNeedConfirmNum() {
        return needConfirmNum;
    }

    public void setNeedConfirmNum(int needConfirmNum) {
        this.needConfirmNum = needConfirmNum;
    }

    public BigInteger getBlockNum() {
        return blockNum;
    }

    public void setBlockNum(BigInteger blockNum) {
        this.blockNum = blockNum;
    }

    public int getConsumerFlag() {
        return consumerFlag;
    }

    public void setConsumerFlag(int consumerFlag) {
        this.consumerFlag = consumerFlag;
    }

    public String getUrlCode() {
        return urlCode;
    }

    public void setUrlCode(String urlCode) {
        this.urlCode = urlCode;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public int getAutoSpeedUp() {
        return autoSpeedUp;
    }

    public void setAutoSpeedUp(int autoSpeedUp) {
        this.autoSpeedUp = autoSpeedUp;
    }

    public String getCoin() {
        return coin;
    }

    public void setCoin(String coin) {
        this.coin = coin;
    }

    public String getApiCoin() {
        return apiCoin;
    }

    public void setApiCoin(String apiCoin) {
        this.apiCoin = apiCoin;
    }

    public BigInteger getTransferBlockNumber() {
        return transferBlockNumber;
    }

    public void setTransferBlockNumber(BigInteger transferBlockNumber) {
        this.transferBlockNumber = transferBlockNumber;
    }

    public Integer getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(Integer errorCount) {
        this.errorCount = errorCount;
    }

    public BigInteger getNonce() {
        return nonce;
    }

    public void setNonce(BigInteger nonce) {
        this.nonce = nonce;
    }

    public BigInteger getCoinBalance() {
        return coinBalance;
    }

    public void setCoinBalance(BigInteger coinBalance) {
        this.coinBalance = coinBalance;
    }

    public BigInteger getTokenBalance() {
        return tokenBalance;
    }

    public void setTokenBalance(BigInteger tokenBalance) {
        this.tokenBalance = tokenBalance;
    }

    public BigInteger getTransferGas() {
        return transferGas;
    }

    public void setTransferGas(BigInteger transferGas) {
        this.transferGas = transferGas;
    }

    public BigInteger getTransferPrice() {
        return transferPrice;
    }

    public void setTransferPrice(BigInteger transferPrice) {
        this.transferPrice = transferPrice;
    }

    public BigInteger getLimit() {
        return limit;
    }

    public void setLimit(BigInteger limit) {
        this.limit = limit;
    }

//    public String getMemo() {
//        return memo;
//    }
//
//    public void setMemo(String memo) {
//        this.memo = memo;
//    }

}