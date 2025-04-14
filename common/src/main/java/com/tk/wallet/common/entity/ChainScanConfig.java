package com.tk.wallet.common.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;

@Setter
@Getter
@TableName("chain_scan_config")
@Data
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

}