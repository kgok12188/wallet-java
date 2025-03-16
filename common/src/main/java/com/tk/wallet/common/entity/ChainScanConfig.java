package com.tk.wallet.common.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;

/**
 * CREATE TABLE `chain_scan_config` (
 * `chain_id` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '链ID,比如BTC,ETH',
 * `endpoints` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'url 等配置',
 * `status` varchar(5) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '0 关闭，1 启用',
 * `ctime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
 * `mtime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
 * `block_number` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0' COMMENT '当前扫描到的区块高度',
 * `block_height` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0' COMMENT '当前链高度',
 * `scan_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '扫描时间',
 * `task_id` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '所在机器',
 * `task_update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '任务更新时间',
 * `un_scan_block` int(11) NOT NULL DEFAULT '200' COMMENT '落后区块数量阈值',
 * UNIQUE KEY `unx_chain_id` (`chain_id`)
 * ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
 * <p>
 * ALTER TABLE chain_scan_config ADD COLUMN delay_blocks int(11) NOT NULL DEFAULT 0 COMMENT '需要延迟扫描的区块,每次扫描高度 : block_height - delay_blocks';
 */
@TableName("chain_scan_config")
public class ChainScanConfig {

    @TableId("chain_id")
    private String chainId;
    //参考样例, 根据不同链设置不同的配置
    /*
    {
    "fields":{
        "url":"节点地址",
        "title":"标题"
    },
    "value":[
            {
                "title":"地址1",
                "url":"https://rpc.ankr.com/eth_goerli"
            },
            {
                "title":"地址1",
                "url":"https://goerli.blockpi.network/v1/rpc/public"
            },
            {
                "title":"地址3",
                "url":"https://eth-goerli.api.onfinality.io/public"
            },
            {
                "title":"地址4",
                "url":"https://rpc.notadegen.com/eth/goerli"
            }
        ]
    }
    * */
    private String endpoints;

    // 0 停用，1 启用
    private String status;
    private Date ctime;
    private Date mtime;
    // 扫描到的区块高度
    private BigInteger blockNumber;
    // 当前区块高度
    private BigInteger blockHeight;

    // 扫描时间
    private Date scanTime;

    // jvm 启动的时候随机生成，多台机器部署的按照链划分扫描机器
    private String taskId;

    // 锁更新时间
    private Date taskUpdateTime;
    // 落后区块数量阈值 block_height - block_number > unScanBlock 发出告警信息，需要人工介入
    private Integer unScanBlock;

    // 需要延迟扫描的区块,每次扫描高度 : block_height - delay_blocks
    private Integer delayBlocks;
    // 出块时间，秒
    private Long blockInterval;

    private Date lastBlockTime;

    private String signUrl;

    private String addressUrl;

    @TableField(exist = false)
    private List<SymbolConfig> coinConfigs;

    private String addressSymbol; // 获取地址的coin

    // 重试间隔，等待上链的状态
    protected Integer retryInterval;

    private String multiThread; // 默认false

    @TableField("multi_thread_numbers")
    private Integer multiThreadNumbers;

    private String jsonConfig; // 自定义配置

    public boolean isSingleThread() {
        return new BigDecimal(blockHeight).subtract(new BigDecimal(blockNumber)).compareTo(new BigDecimal(unScanBlock)) <= 0;
    }

    public String getChainId() {
        return chainId;
    }

    public void setChainId(String chainId) {
        this.chainId = chainId;
    }

    public String getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(String endpoints) {
        this.endpoints = endpoints;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public BigInteger getBlockNumber() {
        return blockNumber;
    }

    public void setBlockNumber(BigInteger blockNumber) {
        this.blockNumber = blockNumber;
    }

    public BigInteger getBlockHeight() {
        return blockHeight;
    }

    public void setBlockHeight(BigInteger blockHeight) {
        this.blockHeight = blockHeight;
    }

    public Date getScanTime() {
        return scanTime;
    }

    public void setScanTime(Date scanTime) {
        this.scanTime = scanTime;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public Date getTaskUpdateTime() {
        return taskUpdateTime;
    }

    public void setTaskUpdateTime(Date taskUpdateTime) {
        this.taskUpdateTime = taskUpdateTime;
    }

    public Integer getUnScanBlock() {
        return unScanBlock;
    }

    public void setUnScanBlock(Integer unScanBlock) {
        this.unScanBlock = unScanBlock;
    }

    public Integer getDelayBlocks() {
        return delayBlocks;
    }

    public void setDelayBlocks(Integer delayBlocks) {
        this.delayBlocks = delayBlocks;
    }

    public Long getBlockInterval() {
        return blockInterval;
    }

    public void setBlockInterval(Long blockInterval) {
        this.blockInterval = blockInterval;
    }

    public Date getLastBlockTime() {
        return lastBlockTime;
    }

    public void setLastBlockTime(Date lastBlockTime) {
        this.lastBlockTime = lastBlockTime;
    }

    public String getSignUrl() {
        return signUrl;
    }

    public void setSignUrl(String signUrl) {
        this.signUrl = signUrl;
    }

    public String getAddressUrl() {
        return addressUrl;
    }

    public void setAddressUrl(String addressUrl) {
        this.addressUrl = addressUrl;
    }

    public List<SymbolConfig> getCoinConfigs() {
        return coinConfigs;
    }

    public void setCoinConfigs(List<SymbolConfig> coinConfigs) {
        this.coinConfigs = coinConfigs;
    }

    public String getAddressSymbol() {
        return addressSymbol;
    }

    public void setAddressSymbol(String addressSymbol) {
        this.addressSymbol = addressSymbol;
    }

    public Integer getRetryInterval() {
        return retryInterval;
    }

    public void setRetryInterval(Integer retryInterval) {
        this.retryInterval = retryInterval;
    }

    public String getMultiThread() {
        return multiThread;
    }

    public void setMultiThread(String multiThread) {
        this.multiThread = multiThread;
    }

    public Integer getMultiThreadNumbers() {
        return multiThreadNumbers;
    }

    public void setMultiThreadNumbers(Integer multiThreadNumbers) {
        this.multiThreadNumbers = multiThreadNumbers;
    }

    public String getJsonConfig() {
        return jsonConfig;
    }

    public void setJsonConfig(String jsonConfig) {
        this.jsonConfig = jsonConfig;
    }
}