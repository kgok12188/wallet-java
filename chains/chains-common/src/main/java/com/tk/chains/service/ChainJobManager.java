package com.tk.chains.service;


import com.tk.wallet.common.entity.ChainScanConfig;
import com.tk.wallet.common.entity.ScanChainQueue;
import com.tk.wallet.common.mapper.ChainScanConfigMapper;
import com.tk.wallet.common.service.ChainScanConfigService;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class ChainJobManager {

    private final String taskId;

    @Autowired
    private ChainScanConfigService chainScanConfigService;
    @Autowired
    private ChainScanConfigMapper chainScanConfigMapper;

    public ChainJobManager() throws UnknownHostException {
        InetAddress addr = InetAddress.getLocalHost();
        taskId = addr.getHostName() + "-" + UUID.randomUUID().toString().replaceAll("-", "");
    }


    public String getTaskId() {
        return taskId;
    }

    /**
     * 获取当前jvm 获得分配的链
     *
     * @return
     */
    public List<ChainScanConfig> getChainScanConfigs() {
        return chainScanConfigService.getOpen(taskId);
    }

    /**
     * 熔断的链
     *
     * @return
     */
    public List<String> circuitBreaker() {
        return chainScanConfigService.circuitBreaker();
    }

    public void circuitBreaker(String chainId) {
        chainScanConfigService.saveCircuitBreaker(chainId);
    }

    public boolean hasUnFinish(String chainId) {
        return chainScanConfigService.hasUnFinish(chainId) <= 0;
    }

    public BigInteger getQueueMaxBlockNumber(ChainScanConfig chainScanConfig) {
        BigInteger blockNumber = chainScanConfigMapper.maxBlockNumber(chainScanConfig.getChainId());
        return blockNumber == null || blockNumber.compareTo(chainScanConfig.getBlockNumber()) <= 0 ? chainScanConfig.getBlockNumber() : blockNumber;
    }

    public void addScanBlockNumberToQueue(String chainId, BigInteger blockNumber) {
        chainScanConfigMapper.addScanBlockNumberToQueue(chainId, blockNumber);
    }

    public List<ScanChainQueue> getBlockNumberList(String chainId, int limit) {
        return chainScanConfigMapper.getBlockNumberList(limit, chainId);
    }

    public List<ScanChainQueue> getBlockNumberListNotEnd(String chainId, int limit) {
        return chainScanConfigMapper.getBlockNumberListNotEnd(limit, chainId);
    }

    public boolean tryUpdate(Long id) {
        Integer rows = chainScanConfigMapper.updateStatus(id);
        return rows != null && rows == 1;
    }

    public void deleteQueue(Long id) {
        chainScanConfigMapper.deleteQueue(id);
    }

    public Integer releaseBlock(Long id) {
        return chainScanConfigMapper.releaseBlock(id);
    }

    public int updateLostTime() {
        Integer rows = chainScanConfigMapper.updateLostTime(new Date(System.currentTimeMillis() - (180 * 1000)));
        return rows == null ? 0 : rows;
    }

    public void stop() {
        chainScanConfigMapper.remove(getTaskId());
    }

}
