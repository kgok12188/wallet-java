package com.tk.chain.likebtc;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.neemre.btcdcli4j.core.domain.Output;
import com.tk.chains.BlockChain;
import com.tk.chains.exceptions.GasException;
import com.tk.wallet.common.entity.ChainScanConfig;
import com.tk.wallet.common.entity.ChainTransaction;
import com.tk.wallet.common.entity.ChainWithdraw;
import com.tk.wallet.common.entity.SymbolConfig;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

public class LIkeBTCBlockChain extends BlockChain<LIkeBTCBlockChain.LikeBTCClient> {

    private static final RestTemplate restTemplate = new RestTemplate();


    @Override
    public void checkChainTransaction(ChainTransaction chainTransaction) {
        String gasConfig = chainTransaction.getGasConfig();
        if (StringUtils.isBlank(gasConfig)) {
            throw new GasException(this.getChainId(), "gasConfig error : " + gasConfig);
        }
        JSONObject jsonObject = JSON.parseObject(gasConfig);
        if (!jsonObject.containsKey("feekb")) {
            throw new GasException(this.getChainId(), "feekb 必须设置 : " + gasConfig);
        }
        String value = jsonObject.getString("feekb");
        if (!StringUtils.isNumeric(value)) {
            throw new GasException(this.getChainId(), "feekb = " + value + " 设置错误，必须是数字");
        }
        if (new BigDecimal(value).compareTo(BigDecimal.ZERO) <= 0) {
            throw new GasException(this.getChainId(), "feekb = " + value + " 设置错误，必须大于0");
        }
        BigDecimal gas = jsonObject.getBigDecimal("gas");
        if (gas == null) {
            throw new GasException(this.getChainId(), "gas = " + value + " 必须设置");
        }
        if (gas.compareTo(BigDecimal.ZERO) <= 0) {
            throw new GasException(this.getChainId(), "gas = " + value + " 设置错误，必须大于0");
        }
    }

    @Override
    public ScanResult scan(ChainScanConfig chainScanConfig, BigInteger blockNumber, ChainClient chainClient) throws Exception {
        return chainClient.getClient().doScan(chainScanConfig, blockNumber);
    }

    @Override
    public String blockHeight(ChainScanConfig chainScanConfig) {
        ChainClient chainClient = getChainClient(null);
        try {
            Integer currentNumber = chainClient.getClient().getBlockCount();
            if (currentNumber != null && currentNumber > 0) {
                return currentNumber.toString();
            }
        } catch (Exception e) {
            log.error("blockHeight : {},\t{}", chainClient.getUrl(), e.getMessage());
            markClientError(chainClient);
        }
        return "0";
    }

    @Override
    public List<ChainTransaction> getChainTransaction(ChainScanConfig chainScanConfig, String hash, String excludeUrl) {
        ChainClient chainClient = getChainClient(null);
        LikeBTCClient likeBTCClient = chainClient.getClient();
        return likeBTCClient.getChainTransaction(chainScanConfig, hash);
    }

    @Override
    public void confirmTransaction(ChainScanConfig chainScanConfig, ChainTransaction chainTransaction) {
        ChainClient chainClient = getChainClient(new HashSet<String>() {
            {
                add(chainTransaction.getUrlCode());
            }
        });
        try {
            List<ChainTransaction> chainTransactions = chainClient.getClient().getChainTransaction(chainScanConfig, chainTransaction.getHash());
            if (CollectionUtils.isNotEmpty(chainTransactions)) {
                blockTransactionManager.updateTxStatus(getChainId(), chainTransaction.getHash(), ChainTransaction.TX_STATUS.SUCCESS.name(), null, null, chainTransaction.getId(), true);
            } else {
                blockTransactionManager.updateTxStatus(getChainId(), chainTransaction.getHash(), ChainTransaction.TX_STATUS.FAIL.name(), ChainTransaction.FAIL_CODE.CHAIN_NOT_FOUND.name(), null, chainTransaction.getId(), true);
            }
        } catch (Exception e) {
            if ("400 Bad Request".equals(e.getMessage())) {
                blockTransactionManager.updateTxStatus(getChainId(), chainTransaction.getHash(), ChainTransaction.TX_STATUS.FAIL.name(), ChainTransaction.FAIL_CODE.CHAIN_NOT_FOUND.name(), null, chainTransaction.getId(), true);
            } else {
                markClientError(chainClient);
            }
        }
    }

    @Override
    public BigDecimal getTokenBalance(ChainScanConfig chainScanConfig, String address, String tokenAddress) {
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getBalance(ChainScanConfig chainScanConfig, String address) {
        ChainClient chainClient = getChainClient(null);
        return chainClient.getClient().getBalance(address);
    }

    @Override
    protected BigDecimal getBalance(String address, BigInteger blockNumber) {
        return getBalance(chainScanConfigService.getByChainId(getChainId()), address);
    }

    @Override
    protected BigDecimal getTokenBalance(String address, String tokenAddress, BigInteger blockNumber) {
        return null;
    }

    @Override
    public BigDecimal gas(ChainScanConfig chainScanConfig, SymbolConfig coinConfig) {
        return JSON.parseObject(coinConfig.getConfigJson()).getBigDecimal("gas");
    }

    @Override
    protected ChainClient create(JSONObject endpoint) {
        String type = endpoint.getString("type");
        if (StringUtils.equalsIgnoreCase("nowNodes", type)) {
            String url = endpoint.getString("url");
            NowNodesLikeBTCClient likeBTCClient = new NowNodesLikeBTCClient(url, endpoint.getString("api-key"), endpoint.getString("sendTxUrl"));
            BlockChain<LikeBTCClient>.ChainClient chainClient = new ChainClient(url, likeBTCClient) {
                @Override
                public void close() {
                    likeBTCClient.close();
                }
            };
            likeBTCClient.setChainClient(chainClient);
            return chainClient;
        } else {
            return null;
        }
    }

    @Override
    public LastBlockInfo getLastBlockInfo() {
        return getChainClient(null).getClient().getLastBlockInfo();
    }

    @Override
    public void transfer(ChainScanConfig chainScanConfig, List<ChainTransaction> chainTransactions) {
        ChainClient chainClient = getChainClient(null);
        String fromAddress = chainTransactions.get(0).getFromAddress();

        List<Output> outputs;
        try {
            outputs = chainClient.getClient().listUnspent(fromAddress);
        } catch (Exception e) {
            log.error("transfer\t\t{}", e.getMessage());
            return;
        }
        String changeAddress = addressChecker.getChangeAddress(getChainId(), fromAddress);
        BigDecimal maxGas = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;
        Long feekb = null;
        for (ChainTransaction chainTransaction : chainTransactions) {
            BigDecimal transferGas = JSON.parseObject(mainCoinConfig.getConfigJson()).getBigDecimal("gas");
            maxGas = maxGas.compareTo(transferGas) >= 0 ? maxGas : transferGas;
            totalAmount = totalAmount.add(chainTransaction.getAmount());
            Long feekbItem = JSON.parseObject(chainTransaction.getGasConfig()).getLong("feekb");
            if (feekb == null) {
                feekb = feekbItem;
            } else {
                feekb = (feekbItem != null && feekbItem > feekb) ? feekbItem : feekb;
            }
        }
        if (feekb == null) {
            feekb = JSON.parseObject(mainCoinConfig.getConfigJson()).getLong("feekb");
        }
        if (maxGas.compareTo(BigDecimal.ZERO) <= 0) {
            log.error("手续费未配置");
            return;
        }
        if (feekb == null) {
            log.error("feekb未配置");
            return;
        }
        BigDecimal count = chainTransactions.size() > 1 ? new BigDecimal("" + chainTransactions.size()).divide(new BigDecimal("2")) : BigDecimal.ONE;
        count = count.compareTo(new BigDecimal("4")) >= 0 ? new BigDecimal("4") : count;
        BigDecimal totalGas = maxGas.multiply(count);

        List<Output> selectedOutputs = new ArrayList<>();
        BigDecimal selectedAmount = BigDecimal.ZERO;
        boolean enoughBalance = false;
        for (Output output : outputs) {
            selectedAmount = selectedAmount.add(output.getAmount());
            selectedOutputs.add(output);
            if (selectedAmount.compareTo(totalGas.add(totalAmount)) >= 0) {
                enoughBalance = true;
                break;
            }
        }
        if (enoughBalance) {
            List<Map<String, Object>> vin = new ArrayList<>();
            String transferId = "";
            for (Output output : selectedOutputs) {
                if (StringUtils.isBlank(transferId)) {
                    transferId = output.getTxId();
                }
                HashMap<String, Object> vItem = new HashMap<>();
                vItem.put("txid", output.getTxId());
                vItem.put("vout", output.getVOut());
                vItem.put("address", fromAddress);
                vItem.put("amount", output.getAmount());
                vin.add(vItem);
            }
            List<Map<String, Object>> vout = new ArrayList<>();
            for (ChainTransaction chainTransaction : chainTransactions) {
                HashMap<String, Object> outItem = new HashMap<>();
                outItem.put("address", chainTransaction.getToAddress());
                outItem.put("amount", chainTransaction.getAmount());
                vout.add(outItem);
            }
            if (selectedAmount.compareTo(totalGas.add(totalAmount)) > 0) {
                HashMap<String, Object> outItem = new HashMap<>();
                outItem.put("address", changeAddress);
                outItem.put("amount", selectedAmount.subtract(totalAmount).subtract(totalGas));
                vout.add(outItem);
            }

            HashMap<String, Object> signReq = new HashMap<>();
            signReq.put("vin", vin);
            signReq.put("vout", vout);
            signReq.put("change", changeAddress);
            signReq.put("feekb", feekb);
            signReq.put("coin", mainCoinConfig.getSymbol());
            String rowData = super.sign(signReq);
            if (log.isDebugEnabled()) {
                log.debug("rawTransaction : {}", rowData);
            }
            // 存储在交易快照中
            signReq.put("gas", totalGas);
            signReq.put("totalOut", selectedAmount);
            boolean flag = true;
            for (ChainTransaction chainTransaction : chainTransactions) {
                flag = flag && blockTransactionManager.prepareTransfer(chainTransaction.getId(), chainScanConfig.getBlockHeight(), chainClient.getUrl());
            }
            String hash = "";
            if (flag) {
                String chainInfo = JSON.toJSONString(signReq);
                ChainWithdraw chainWithdraw = new ChainWithdraw();
                chainWithdraw.setStatus(ChainTransaction.TX_STATUS.WAITING_HASH.name());
                chainWithdraw.setGasAddress(fromAddress);
                chainWithdraw.setTransferId(transferId);
                chainWithdraw.setRowData(rowData);
                chainWithdraw.setIds(JSON.toJSONString(chainTransactions.stream().map(ChainTransaction::getId).collect(Collectors.toList())));
                chainWithdrawService.save(chainWithdraw);
                try {
                    hash = chainClient.getClient().sendRawTransaction(rowData);
                } catch (Exception e) {
                    blockTransactionManager.networkError(chainWithdraw);
                    markClientError(chainClient);
                    log.error("sendRawTransaction {}", StringUtils.join(chainTransactions.stream().map(ChainTransaction::getId).collect(Collectors.toList())), e);
                    return;
                }
                if (StringUtils.isNotBlank(hash)) {
                    log.info("transfer {},\t\t{}", hash, chainInfo);
                    for (ChainTransaction chainTransaction : chainTransactions) {
                        blockTransactionManager.updateHash(chainTransaction.getId(), hash, chainInfo);
                    }
                } else {
                    for (ChainTransaction chainTransaction : chainTransactions) {
                        blockTransactionManager.releaseWaitingHash(chainTransaction.getId());
                    }
                }
            } else {
                for (ChainTransaction chainTransaction : chainTransactions) {
                    blockTransactionManager.releaseWaitingHash(chainTransaction.getId());
                }
            }
        } else {
            ChainTransaction chainTransaction = chainTransactions.get(0);
            blockTransactionManager.updateTxStatus(chainTransaction.getId(), ChainTransaction.TX_STATUS.FAIL.name(), ChainTransaction.FAIL_CODE.BALANCE_NOT_ENOUGH.name(), null, chainScanConfig.getBlockHeight(), true);
        }
    }

    protected abstract class LikeBTCClient {
        protected HttpHeaders headers = new HttpHeaders();
        protected String url;
        protected String sendTxUrl;
        protected ChainClient chainClient;

        public LikeBTCClient(String url, String apiKey, String sendTxUrl) {
            this.url = url;
            headers.add("api-key", apiKey);
            headers.add("x-api-key", apiKey);
            headers.add("Accept", "*/*");
            headers.add("User-Agent", "PostmanRuntime/7.29.2");
            headers.add("Connection", "keep-alive");
            headers.add("Content-Type", "application/json");
            this.sendTxUrl = sendTxUrl;
        }

        public void setChainClient(ChainClient chainClient) {
            this.chainClient = chainClient;
        }

        public abstract String getBlockHash(Integer blockNumber);

        public void close() {

        }

        public BigDecimal getBalance(String address) {
            List<Output> outputs = this.listUnspent(address);
            BigDecimal amount = BigDecimal.ZERO;
            for (Output output : outputs) {
                amount = amount.add(output.getAmount());
            }
            return amount;
        }

        public abstract ScanResult doScan(ChainScanConfig chainScanConfig, BigInteger blockNumber) throws Exception;

        public Integer getBlockCount() {
            HashMap<String, Object> params = new HashMap<>();
            params.put("jsonrpc", "2.0");
            params.put("method", "getblockcount");
            params.put("params", new ArrayList<>(0));
            params.put("id", UUID.randomUUID().toString().replaceAll("-", ""));
            ResponseEntity<String> response = restTemplate.postForEntity(this.url, new HttpEntity<>(params, headers), String.class);
            return JSON.parseObject(response.getBody()).getInteger("result");
        }

        public abstract List<ChainTransaction> getChainTransaction(ChainScanConfig chainScanConfig, String hash);

        public abstract List<Output> listUnspent(String address);

        public String sendRawTransaction(String hex) {
            HashMap<String, Object> request = new HashMap<>();
            request.put("jsonrpc", "2.0");
            request.put("method", "sendrawtransaction");
            ArrayList<Object> params = new ArrayList<>();
            params.add(hex);
            request.put("params", params);
            request.put("id", UUID.randomUUID().toString().replaceAll("-", ""));
            ResponseEntity<String> response = restTemplate.postForEntity(sendTxUrl, new HttpEntity<>(request, headers), String.class);
            return JSON.parseObject(response.getBody()).getString("result");
        }

        public abstract LastBlockInfo getLastBlockInfo();

    }

    //  {"type":"array","value":[{"type":"nowNodes","title":"私有节点","sendTxUrl":"https://doge.nownodes.io","api-key":"4e2966c3-bc74-40a8-883e-2edb4550c557","url":"https://dogebook.nownodes.io"}]}
    protected class NowNodesLikeBTCClient extends LikeBTCClient {
        private String utxoPath = "api/v2/utxo";
        private String blockPath = "api/v2/block";
        private String txPath = "api/v2/tx";

        public NowNodesLikeBTCClient(String url, String apiKey, String sendTxUrl) {
            super(url, apiKey, sendTxUrl);
        }

        public JSONObject getBlock(String hash, int page) {
            String getUrl = String.format("%s/%s/%s?page=%s", url, blockPath, hash, page);
            ResponseEntity<String> exchange = restTemplate.exchange(getUrl, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            return JSON.parseObject(exchange.getBody());
        }


        @Override
        public String getBlockHash(Integer blockNumber) {
            HashMap<String, Object> request = new HashMap<>();
            request.put("jsonrpc", "2.0");
            request.put("method", "getblockhash");
            ArrayList<Object> params = new ArrayList<>();
            params.add(blockNumber);
            request.put("params", params);
            request.put("id", UUID.randomUUID().toString().replaceAll("-", ""));
            ResponseEntity<String> response = restTemplate.postForEntity(this.sendTxUrl, new HttpEntity<>(request, headers), String.class);
            return JSON.parseObject(response.getBody()).getString("result");
        }

        @Override
        public ScanResult doScan(ChainScanConfig chainScanConfig, BigInteger blockNumber) {
            String blockHash = getBlockHash(blockNumber.intValue());
            int pageIndex = 1;
            int totalPage = Integer.MAX_VALUE;
            int txsCount = 0;
            Date blockTime = null;
            List<ScanResultTx> txList = new ArrayList<>();
            while (pageIndex <= totalPage) {
                JSONObject block = getBlock(blockHash, pageIndex);
                if (blockTime == null) {
                    blockTime = new Date(block.getLongValue("time") * 1000);
                }
                List<ChainTransaction> transactions = new ArrayList<>();
                totalPage = block.getIntValue("totalPages");
                JSONArray txs = block.getJSONArray("txs");
                txsCount = txs.size() + txsCount;
                for (int i = 0; i < txs.size(); i++) {
                    JSONObject tx = txs.getJSONObject(i);
                    String hash = tx.getString("txid");
                    String fromAddress = "";
                    JSONArray vout = tx.getJSONArray("vout");
                    String transferId = "";
                    HashMap<String, BigDecimal> toAddressAmount = new HashMap<>();
                    BigDecimal totalOut = BigDecimal.ZERO;
                    for (int j = 0; j < vout.size(); j++) {
                        JSONObject txOut = vout.getJSONObject(j);
                        Boolean isAddress = txOut.getBoolean("isAddress");
                        if (isAddress != null && isAddress) {
                            String toAddress = txOut.getJSONArray("addresses").getString(0);
                            BigDecimal amount = txOut.getBigDecimal("value").divide(mainCoinConfig.precision(), 16, RoundingMode.DOWN).stripTrailingZeros();
                            BigDecimal value = toAddressAmount.get(toAddress);
                            if (value == null) {
                                value = BigDecimal.ZERO;
                            }
                            value = value.add(amount);
                            toAddressAmount.put(toAddress, value);
                            totalOut = totalOut.add(amount);
                        }
                    }
                    JSONArray vinList = tx.getJSONArray("vin");
                    for (int k = 0; k < vinList.size(); k++) {
                        JSONObject item = vinList.getJSONObject(k);
                        if (item.containsKey("isAddress") && item.getBoolean("isAddress")) {
                            fromAddress = item.getJSONArray("addresses").get(0).toString();
                            break;
                        }
                        if (StringUtils.isBlank(transferId)) {
                            transferId = item.getString("txid");
                        }
                    }
                    BigDecimal actGas = BigDecimal.ZERO;
                    for (Map.Entry<String, BigDecimal> kv : toAddressAmount.entrySet()) {
                        String toAddress = kv.getKey();
                        BigDecimal amount = kv.getValue();
                        BigDecimal gas = BigDecimal.ZERO;
                        if (addressChecker.owner(fromAddress, toAddress, getChainId(), hash)) {
                            ChainTransaction chainTransaction = new ChainTransaction();
                            chainTransaction.setChainId(getChainId());
                            chainTransaction.setHash(hash);
                            chainTransaction.setBlockNum(new BigInteger(blockNumber.toString()));
                            chainTransaction.setCtime(new Date());
                            chainTransaction.setMtime(new Date());
                            String txStatus = (chainScanConfig.getBlockHeight().subtract(new BigInteger(blockNumber.toString()))).compareTo(new BigInteger(mainCoinConfig.getConfirmCount().toString()).multiply(new BigInteger("2"))) > 0 ? ChainTransaction.TX_STATUS.SUCCESS.name() : ChainTransaction.TX_STATUS.PENDING.name();
                            chainTransaction.setTxStatus(txStatus);
                            chainTransaction.setContract("");
                            chainTransaction.setFromAddress(fromAddress);
                            chainTransaction.setToAddress(toAddress);
                            chainTransaction.setTxStatus(ChainTransaction.TX_STATUS.PENDING.name());
                            chainTransaction.setAmount(amount);
                            chainTransaction.setTokenSymbol(mainCoinConfig.getTokenSymbol());
                            chainTransaction.setSymbol(mainCoinConfig.getSymbol());
                            chainTransaction.setNeedConfirmNum(mainCoinConfig.getConfirmCount());
                            chainTransaction.setUrlCode(chainClient.getUrl());
                            chainTransaction.setBlockTime(blockTime);
                            ChainWithdraw chainWithdraw = chainWithdrawService.lambdaQuery().eq(ChainWithdraw::getChainId, chainId).eq(ChainWithdraw::getHash, hash).last("limit 1").one();
                            if (chainWithdraw == null) {
                                chainWithdrawService.lambdaQuery().eq(ChainWithdraw::getChainId, chainId).eq(ChainWithdraw::getTransferId, transferId).last("limit 1").one();
                            }
                            if (chainWithdraw != null) {
                                JSONObject outConfig = JSON.parseObject(chainWithdraw.getInfo());
                                actGas = outConfig.getBigDecimal("totalOut").subtract(totalOut);
                            }
                            transactions.add(chainTransaction);
                        }
                    }
                    if (CollectionUtils.isNotEmpty(transactions)) {
                        txList.add(new ScanResultTx(hash, transferId, fromAddress, actGas, transactions, transactions.get(0).getTxStatus()));
                    }
                }
                pageIndex++;
            }
            return new ScanResult(txList.size(), txList, blockNumber, blockTime);
        }

        @Override
        public Integer getBlockCount() {
            HashMap<String, Object> request = new HashMap<>();
            request.put("jsonrpc", "2.0");
            request.put("method", "getblockcount");
            request.put("params", new ArrayList<>(0));
            request.put("id", UUID.randomUUID().toString().replaceAll("-", ""));
            ResponseEntity<String> response = restTemplate.postForEntity(sendTxUrl, new HttpEntity<>(request, headers), String.class);
            return JSON.parseObject(response.getBody()).getInteger("result");
        }

        @Override
        public List<ChainTransaction> getChainTransaction(ChainScanConfig chainScanConfig, String hash) {
            JSONObject transaction = getTransaction(hash);
            Integer confirmations = transaction.getInteger("confirmations");
            confirmations = confirmations == null ? 0 : confirmations;
            JSONArray vout = transaction.getJSONArray("vout");
            JSONArray vinList = transaction.getJSONArray("vin");
            String fromAddress = "";
            BigDecimal inputAmount = BigDecimal.ZERO;
            BigDecimal outputAmount = BigDecimal.ZERO;
            for (int i = 0; i < vinList.size(); i++) {
                JSONObject item = vinList.getJSONObject(i);
                if (item.getBoolean("isAddress")) {
                    fromAddress = item.getJSONArray("addresses").get(0).toString();
                    BigDecimal value = item.getBigDecimal("value").divide(mainCoinConfig.precision(), 16, RoundingMode.DOWN);
                    inputAmount = inputAmount.add(value);
                }
            }
            List<ChainTransaction> chainTransactions = new ArrayList<>();
            String txStatus = confirmations >= mainCoinConfig.getConfirmCount() ? ChainTransaction.TX_STATUS.SUCCESS.name() : ChainTransaction.TX_STATUS.PENDING.name();
            for (int i = 0; i < vout.size(); i++) {
                JSONObject out = vout.getJSONObject(i);
                if (out.getBoolean("isAddress")) {
                    ChainTransaction chainTransaction = new ChainTransaction();
                    chainTransaction.setChainId(getChainId());
                    chainTransaction.setToAddress(out.getJSONArray("addresses").get(0).toString());
                    chainTransaction.setFromAddress(fromAddress);
                    chainTransaction.setAmount(out.getBigDecimal("value").divide(mainCoinConfig.precision(), 16, RoundingMode.DOWN));
                    chainTransaction.setHash(hash);
                    chainTransaction.setTxStatus(txStatus);
                    chainTransaction.setNeedConfirmNum(mainCoinConfig.getConfirmCount());
                    chainTransaction.setTokenSymbol(mainCoinConfig.getTokenSymbol());
                    chainTransaction.setSymbol(mainCoinConfig.getSymbol());
                    chainTransaction.setUrlCode(url);
                    chainTransactions.add(chainTransaction);
                    outputAmount = outputAmount.add(chainTransaction.getAmount());
                }
            }
            return chainTransactions;
        }

        private JSONObject getTransaction(String hash) {
            String getUrl = String.format("%s/%s/%s", url, txPath, hash);
            ResponseEntity<String> exchange = restTemplate.exchange(getUrl, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            return JSON.parseObject(exchange.getBody());
        }

        public List<Output> listUnspent(String address) {
            List<Output> outputs = new ArrayList<>();
            String getUrl = String.format("%s/%s/%s?confirmed=true", url, utxoPath, address);
            ResponseEntity<String> exchange = restTemplate.exchange(getUrl, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            JSONArray jsonArray = JSON.parseArray(exchange.getBody());
            for (int i = 0; i < jsonArray.size(); i++) {
                Output output = new Output();
                JSONObject item = jsonArray.getJSONObject(i);
                output.setVOut(item.getIntValue("vout"));
                output.setTxId(item.getString("txid"));
                output.setAmount(item.getBigDecimal("value").divide(mainCoinConfig.precision()));
                output.setAddress(address);
                outputs.add(output);
            }
            return outputs;
        }

        @Override
        public LastBlockInfo getLastBlockInfo() {
            HashMap<String, Object> request = new HashMap<>();
            request.put("jsonrpc", "2.0");
            request.put("method", "getblockchaininfo");
            request.put("params", new ArrayList<>(0));
            request.put("id", UUID.randomUUID().toString().replaceAll("-", ""));
            ResponseEntity<String> response = restTemplate.postForEntity(sendTxUrl, new HttpEntity<>(request, headers), String.class);
            JSONObject result = JSON.parseObject(response.getBody()).getJSONObject("result");
            Long blocks = result.getLong("blocks");
            Long time = result.getLong("time");
            LastBlockInfo lastBlockInfo = new LastBlockInfo();
            lastBlockInfo.setBlockNumber(new BigInteger(blocks.toString()));
            lastBlockInfo.setBlockTime(new Date(time * 1000));
            return lastBlockInfo;
        }
    }

}
