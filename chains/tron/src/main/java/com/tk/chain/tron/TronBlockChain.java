package com.tk.chain.tron;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.tk.chains.BlockChain;
import com.tk.chains.exceptions.GasException;
import com.tk.wallet.common.entity.ChainScanConfig;
import com.tk.wallet.common.entity.ChainTransaction;
import com.tk.wallet.common.entity.SymbolConfig;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.bitcoinj.core.Base58;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.tron.trident.abi.FunctionEncoder;
import org.tron.trident.abi.TypeDecoder;
import org.tron.trident.abi.TypeReference;
import org.tron.trident.abi.datatypes.Address;
import org.tron.trident.abi.datatypes.Type;
import org.tron.trident.abi.datatypes.generated.Uint256;
import org.tron.trident.core.ApiWrapper;
import org.tron.trident.core.contract.Contract;
import org.tron.trident.core.contract.Trc20Contract;
import org.tron.trident.core.exceptions.IllegalException;
import org.tron.trident.proto.Chain;
import org.tron.trident.proto.Response;
import org.tron.trident.utils.Base58Check;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Service("TRX")
public class TronBlockChain extends BlockChain<TronBlockChain.TrxChainClient> {

    private volatile Map<String, SymbolConfig> configHashMap = new HashMap<>();


    @Override
    public SymbolConfig getTokenConfig(String contractAddress) {
        return configHashMap.get(contractAddress);
    }

    @Override
    public void checkChainTransaction(ChainTransaction chainTransaction) {
        String gasConfig = chainTransaction.getGasConfig();
        if (StringUtils.isBlank(gasConfig)) {
            throw new GasException(this.getChainId(), "gasConfig error : " + gasConfig);
        }
        JSONObject jsonObject = JSON.parseObject(gasConfig);
        if (!jsonObject.containsKey("feeLimit")) {
            throw new GasException(this.getChainId(), "feeLimit 必须设置: " + gasConfig);
        }
        String feeLimit = jsonObject.getString("feeLimit");
        if (!StringUtils.isNumeric(feeLimit)) {
            throw new GasException(this.getChainId(), "feeLimit 必须是数字: " + gasConfig);
        }
        if (new BigDecimal(feeLimit).compareTo(BigDecimal.ZERO) <= 0) {
            throw new GasException(this.getChainId(), "feeLimit 必须大于0: " + gasConfig);
        }
    }

    /**
     * @param chainScanConfig
     * @param chainTransactions 按照 fromAddress 分组后的交易
     */
    @Override
    public void transfer(ChainScanConfig chainScanConfig, List<ChainTransaction> chainTransactions) {
        BlockChain<TrxChainClient>.ChainClient chainClient = getChainClient(null);
        if (CollectionUtils.isNotEmpty(chainTransactions)) {
            BigDecimal coinBalance = null; // 主币余额
            HashMap<String, BigDecimal> tokenBalances = new HashMap<>(); // trc-20 合约代币余额
            for (ChainTransaction chainTransaction : chainTransactions) {
                JSONObject transaction = null;
                if (StringUtils.isBlank(chainTransaction.getContract())) {
                    if (coinBalance == null) {
                        coinBalance = getBalance(chainScanConfig, chainTransaction.getFromAddress());
                    }
                    if (coinBalance.compareTo(chainTransaction.getAmount()) < 0) {
                        blockTransactionManager.updateTxStatus(chainTransaction.getId(), ChainTransaction.TX_STATUS.FAIL.name(), ChainTransaction.FAIL_CODE.BALANCE_NOT_ENOUGH.name(), "余额不足", chainScanConfig.getBlockHeight(), true);
                        return;
                    }
                    coinBalance = coinBalance.subtract(chainTransaction.getAmount());

                    JSONObject createdTransfer = chainClient.getClient().buildCoinDto(chainTransaction, mainCoinConfig);
                    if (createdTransfer == null) {
                        log.error("创建TRX交易返回为空 {},\t{}", chainTransaction.getId(), chainTransaction.getBusinessId());
                        blockTransactionManager.updateTxStatus(chainTransaction.getId(), ChainTransaction.TX_STATUS.FAIL.name(), null, "创建TRX交易返回为空", chainScanConfig.getBlockHeight(), true);
                        return;
                    }
                    if (createdTransfer.getString("Error") != null) {
                        String error = createdTransfer.getString("Error");
                        log.error("创建TRX交易失败 TronService transfer error transferId={} ,error={}", chainTransaction.getId(), error);
                        blockTransactionManager.updateTxStatus(chainTransaction.getId(), ChainTransaction.TX_STATUS.FAIL.name(), null, "创建TRX交易失败", chainScanConfig.getBlockHeight(), true);
                        return;
                    }
                    transaction = createdTransfer;
                } else {
                    SymbolConfig coinConfig = configHashMap.get(chainTransaction.getContract());
                    if (!tokenBalances.containsKey(chainTransaction.getContract())) {
                        BigDecimal tokenBalance = getTokenBalance(chainScanConfig, chainTransaction.getFromAddress(), chainTransaction.getContract());
                        tokenBalances.put(chainTransaction.getContract(), tokenBalance);
                    }
                    BigDecimal tokenBalance = tokenBalances.get(chainTransaction.getContract());
                    if (tokenBalance.compareTo(chainTransaction.getAmount()) < 0) {
                        blockTransactionManager.updateTxStatus(chainTransaction.getId(), ChainTransaction.TX_STATUS.FAIL.name(), ChainTransaction.FAIL_CODE.BALANCE_NOT_ENOUGH.name(), "余额不足", chainScanConfig.getBlockHeight(), true);
                        return;
                    }
                    tokenBalance = tokenBalance.subtract(chainTransaction.getAmount());
                    tokenBalances.put(chainTransaction.getContract(), tokenBalance);
                    JSONObject createdTransfer = chainClient.getClient().buildTokenDto(chainTransaction, coinConfig);
                    if (createdTransfer == null) {
                        log.error("创建TRC20_USDT交易返回为空 {},\t{}", chainTransaction.getId(), chainTransaction.getBusinessId());
                        blockTransactionManager.updateTxStatus(chainTransaction.getId(), ChainTransaction.TX_STATUS.FAIL.name(), null, "创建TRC20_USDT交易返回为空", chainScanConfig.getBlockHeight(), true);
                        return;
                    }
                    /**金额不足、地址未激活、地址错误等都会在这里报出来*/
                    JSONObject resObj = createdTransfer.getJSONObject("result");
                    if (resObj == null || !"true".equals(resObj.getString("result"))) {
                        String error = "";
                        if (resObj != null) {
                            error = resObj.getString("message");
                            log.error("transfer error transferId={} ,error={}", chainTransaction.getBusinessId(), error);
                        }
                        log.error("创建TRC20_USDT交易失败 {},\t{}", chainTransaction.getId(), chainTransaction.getBusinessId());
                        blockTransactionManager.updateTxStatus(chainTransaction.getId(), ChainTransaction.TX_STATUS.FAIL.name(), error, "创建TRC20_USDT交易失败", chainScanConfig.getBlockHeight(), true);
                        return;
                    }
                    if (createdTransfer.getJSONObject("transaction") == null) {
                        log.error("创建交易返回transaction为空 {},\t{}", chainTransaction.getId(), chainTransaction.getBusinessId());
                        blockTransactionManager.updateTxStatus(chainTransaction.getId(), ChainTransaction.TX_STATUS.FAIL.name(), null, "创建交易返回transaction为空", chainScanConfig.getBlockHeight(), true);
                        return;
                    }
                    transaction = createdTransfer.getJSONObject("transaction");
                }
                if (transaction != null) {
                    log.info("transaction = {}", JSON.toJSONString(transaction));
                    String needSignData = transaction.getString("raw_data_hex");
                    if (StringUtils.isBlank(needSignData)) {
                        log.error("交易中raw_data_hex为空 {},\t{}", chainTransaction.getId(), chainTransaction.getBusinessId());
                        blockTransactionManager.updateTxStatus(chainTransaction.getId(), ChainTransaction.TX_STATUS.FAIL.name(), null, "交易中raw_data_hex为空", chainScanConfig.getBlockHeight(), true);
                        return;
                    }
                    String hash = transaction.getString("txID");
                    /**签名*/
                    Map<String, String> signMap = new HashMap<>();
                    signMap.put("from", chainTransaction.getFromAddress());
                    signMap.put("raw_data_hex", needSignData);
                    String signature = sign(signMap);
                    if (StringUtils.isBlank(signature)) {
                        log.error("交易签名返回为空 {},\t{}", chainTransaction.getId(), chainTransaction.getBusinessId());
                        blockTransactionManager.updateTxStatus(chainTransaction.getId(), ChainTransaction.TX_STATUS.FAIL.name(), null, "交易签名返回为空", chainScanConfig.getBlockHeight(), true);
                        return;
                    }
                    transaction.put("signature", signature.replaceAll("\"", ""));
                    /**
                     * 1、更新状态为等待上链
                     * 2、广播数据（如果http广播数据出错,那么释放交易，等待下一次再次发起交易）
                     * 3、更新本地hash
                     */
                    if (blockTransactionManager.prepareTransfer(chainTransaction.getId(), chainScanConfig.getBlockHeight(), chainClient.getUrl())) {
                        log.info("broadcastTransaction : {},\t{}", chainTransaction.getId(), hash);
                        try {
                            Map<String, Object> map = chainClient.getClient().broadcastTransaction(transaction);
                            if (MapUtils.getBoolean(map, "result", false)) {
                                blockTransactionManager.updateHash(chainTransaction.getId(), hash, "");
                            } else {
                                String code = MapUtils.getString(map, "code", "");
                                String errorMsg = code + ":" + MapUtils.getString(map, "message", "");
                                if (StringUtils.isBlank(errorMsg)) {
                                    errorMsg = MapUtils.getString(map, "Error");
                                    log.error("broadcastTransaction : {},\t{},\t{}", chainTransaction.getId(), hash, errorMsg);
                                }
                                blockTransactionManager.updateTxStatus(chainTransaction.getId(), ChainTransaction.TX_STATUS.FAIL.name(), null, errorMsg, chainScanConfig.getBlockHeight(), true);
                            }
                        } catch (Exception e) {
                            // 释放交易，等待下一次提交
                            blockTransactionManager.releaseWaitingHash(chainTransaction.getId());
                            log.error("广播失败", e);
                            markClientError(chainClient);
                            return;
                        }
                    }
                }
            }
        }
    }

    @Override
    @SuppressWarnings("all")
    public ScanResult scan(ChainScanConfig chainScanConfig, BigInteger blockNumber, BlockChain<TrxChainClient>.ChainClient chainClient) throws Exception {
        Response.BlockExtention block = chainClient.getClient().getBlock(blockNumber.longValue());
        List<Response.TransactionExtention> blockTransactions = block.getTransactionsList();
        long timestamp = block.getBlockHeader().getRawData().getTimestamp();
        List<ScanResultTx> txList = new ArrayList<>();
        for (Response.TransactionExtention transaction : blockTransactions) {
            List<ChainTransaction> chainTransactions = new ArrayList<>();
            String hash = ApiWrapper.toHex(transaction.getTxid());
            Chain.Transaction.Contract contract = transaction.getTransaction().getRawData().getContract(0);
            ByteString value = contract.getParameter().getValue();
            Chain.Transaction.Contract.ContractType type = contract.getType();
            if (type == Chain.Transaction.Contract.ContractType.UNRECOGNIZED) {
                continue;
            }
            long confirmations = chainScanConfig.getBlockHeight().subtract(blockNumber).longValue();
            String txStatus = confirmations >= mainCoinConfig.getConfirmCount() ? ChainTransaction.TX_STATUS.SUCCESS.name() : ChainTransaction.TX_STATUS.PENDING.name();
            ChainTransaction chainTransaction = null;
            if (contract.getType().getNumber() == 1) { // coin
                org.tron.trident.proto.Contract.TransferContract transferContract = org.tron.trident.proto.Contract.TransferContract.parseFrom(value.toByteArray());
                long amount = transferContract.getAmount();
                String toAddress = Base58Check.bytesToBase58(transferContract.getToAddress().toByteArray());
                String ownerAddress = Base58Check.bytesToBase58(transferContract.getOwnerAddress().toByteArray());
                if (addressChecker.owner(ownerAddress, toAddress, getChainId(), hash)) {
                    log.info("tron 查询到trx有充值交易：区块高度：{},充值地址：{},txId:{}", blockNumber, toAddress, hash);
                    BigDecimal amountDecimal = new BigDecimal(amount);
                    chainTransaction = new ChainTransaction();
                    chainTransaction.setAmount(amountDecimal.divide(mainCoinConfig.precision(), 16, RoundingMode.DOWN));
                    chainTransaction.setFromAddress(ownerAddress);
                    chainTransaction.setToAddress(toAddress);
                    chainTransaction.setTokenSymbol(mainCoinConfig.getTokenSymbol());
                    chainTransaction.setSymbol(mainCoinConfig.getSymbol());
                }
            } else if (contract.getType().getNumber() == 31) { // trc-20
                org.tron.trident.proto.Contract.TriggerSmartContract triggerSmartContract = org.tron.trident.proto.Contract.TriggerSmartContract.parseFrom(value);
                String fromAddress = Base58Check.bytesToBase58(Hex.decode(ApiWrapper.toHex(triggerSmartContract.getOwnerAddress())));
                String contractAddress = Base58Check.bytesToBase58(Hex.decode(ApiWrapper.toHex(triggerSmartContract.getContractAddress())));
                SymbolConfig coinConfig = configHashMap.get(contractAddress);
                if (coinConfig != null) {
                    ByteString data = triggerSmartContract.getData();
                    String s = ApiWrapper.toHex(data.toByteArray());

                    if (s.length() >= 72) {
                        String methodId = s.substring(0, 8);
                        if ("a9059cbb".equals(methodId)) { //a9059cbb是transfer(address,uint256)的函数选择器
                            // Address toAddress = TypeDecoder.decodeStaticStruct(s.substring(8, 72), 0, TypeReference.create(Address.class));
                            Address toAddress = (Address) TypeDecoder.instantiateType("address", s.substring(32, 72));
                            String checkAddress = toAddress.getValue();
                            if (StringUtils.isNotBlank(checkAddress) && addressChecker.owner(fromAddress, checkAddress, getChainId(), hash)) {
                                log.info("tron 查询到有trc20充值交易：区块高度：{},充值地址：{},txId:{}", blockNumber, checkAddress, hash);
                                BigDecimal tokenAmount = new BigDecimal(new BigInteger(s.substring(72), 16));
                                chainTransaction = new ChainTransaction();
                                chainTransaction.setFromAddress(fromAddress);
                                chainTransaction.setToAddress(checkAddress);
                                chainTransaction.setAmount(tokenAmount.divide(coinConfig.precision(), 16, RoundingMode.DOWN));
                                chainTransaction.setContract(contractAddress);
                                chainTransaction.setTokenSymbol(coinConfig.getTokenSymbol());
                                chainTransaction.setSymbol(coinConfig.getSymbol());
                            }
                        }
                    }
                }
            }
            BigDecimal actFee = BigDecimal.ZERO;
            if (chainTransaction != null) {
                chainTransaction.setHash(hash);
                chainTransaction.setTxStatus(txStatus);
                chainTransaction.setChainId(getChainId());
                chainTransaction.setBlockNum(blockNumber);
                chainTransaction.setNeedConfirmNum(mainCoinConfig.getConfirmCount());
                chainTransaction.setUrlCode(chainClient.getUrl());
                Response.TransactionInfo transactionInfo = chainClient.getClient().getTransactionInfoById(hash);
                long fee = transactionInfo.getFee();
                actFee = new BigDecimal(fee).divide(mainCoinConfig.precision(), 16, RoundingMode.DOWN);
                Chain.Transaction.Result.contractResult contractRet = transaction.getTransaction().getRet(0).getContractRet();
                if (Chain.Transaction.Result.contractResult.SUCCESS != contractRet) {
                    chainTransaction.setTxStatus(ChainTransaction.TX_STATUS.FAIL.name());
                    chainTransaction.setMessage(contractRet.name());
                }
                chainTransactions.add(chainTransaction);
            }
            if (CollectionUtils.isNotEmpty(chainTransactions)) {
                txList.add(new ScanResultTx(hash, null, chainTransactions.get(0).getFromAddress(), actFee, chainTransactions, chainTransaction.getTxStatus()));
            }
        }
        return new ScanResult(txList.size(), txList, blockNumber, new Date(timestamp));
    }

    @Override
    public String blockHeight(ChainScanConfig chainScanConfig) {
        BlockChain<TrxChainClient>.ChainClient chainClient = getChainClient(null);
        try {
            return chainClient.getClient().getCurrentBlockNum().toString();
        } catch (IllegalException e) {
            log.error("blockHeight {}", e.getMessage());
            markClientError(chainClient);
            return "0";
        }
    }

    @Override
    public List<ChainTransaction> getChainTransaction(ChainScanConfig chainScanConfig, String hash, String excludeUrl) {
        BlockChain<TrxChainClient>.ChainClient chainClient = getChainClient(new HashSet<String>() {
            {
                if (StringUtils.isNotBlank(excludeUrl)) {
                    add(excludeUrl);
                }
            }
        });
        Chain.Transaction transaction;
        try {
            transaction = chainClient.getClient().getTransactionById(hash);
        } catch (IllegalException e) {
            markClientError(chainClient);
            throw new RuntimeException(e);
        }
        List<ChainTransaction> chainTransactions = new ArrayList<>();
        String result = transaction.getRet(0).getContractRet().toString();
        if (StringUtils.equalsIgnoreCase("SUCCESS", result)) {
            Chain.Transaction.Contract contract = transaction.getRawData().getContract(0);
            ByteString value = contract.getParameter().getValue();
            Chain.Transaction.Contract.ContractType type = contract.getType();
            ChainTransaction chainTransaction = null;
            if (type.getNumber() == 1) {
                org.tron.trident.proto.Contract.TransferContract transferContract = null;
                try {
                    transferContract = org.tron.trident.proto.Contract.TransferContract.parseFrom(value.toByteArray());
                    long amount = transferContract.getAmount();
                    ByteString toAddress = transferContract.getToAddress();
                    ByteString ownerAddress = transferContract.getOwnerAddress();
                    BigDecimal amountDecimal = new BigDecimal(amount);
                    chainTransaction = new ChainTransaction();
                    chainTransaction.setFromAddress(Base58Check.bytesToBase58(ownerAddress.toByteArray()));
                    chainTransaction.setToAddress(Base58Check.bytesToBase58(toAddress.toByteArray()));
                    chainTransaction.setAmount(amountDecimal.divide(mainCoinConfig.precision(), 16, RoundingMode.DOWN));
                    chainTransaction.setTokenSymbol(mainCoinConfig.getTokenSymbol());
                    chainTransaction.setSymbol(mainCoinConfig.getSymbol());
                } catch (InvalidProtocolBufferException e) {
                    log.error("1 TransferContract.parseFrom", e);
                    throw new RuntimeException(e);
                }
            } else if (type.getNumber() == 31) {
                org.tron.trident.proto.Contract.TriggerSmartContract triggerSmartContract = null;
                try {
                    triggerSmartContract = org.tron.trident.proto.Contract.TriggerSmartContract.parseFrom(value);
                    ByteString data = triggerSmartContract.getData();
                    ByteString contractAddress = triggerSmartContract.getContractAddress();
                    ByteString ownerAddress = triggerSmartContract.getOwnerAddress();
                    //解析余额
                    String s = ApiWrapper.toHex(data.toByteArray());
                    TypeReference<Address> addressTypeReference = TypeReference.create(Address.class);
                    Address toAddress = TypeDecoder.decodeStaticStruct(s.substring(8, 72), 0, addressTypeReference);
                    BigDecimal amountDecimal = new BigDecimal(new BigInteger(s.substring(72), 16));
                    //解析to地址
                    String s1 = Base58Check.bytesToBase58(Hex.decode(ApiWrapper.toHex(ownerAddress)));
                    String s2 = Base58Check.bytesToBase58(Hex.decode(ApiWrapper.toHex(contractAddress)));
                    SymbolConfig coinConfig = configHashMap.get(s2);
                    if (coinConfig != null) {
                        chainTransaction = new ChainTransaction();
                        chainTransaction.setFromAddress(s1);
                        chainTransaction.setToAddress(toAddress.toString());
                        chainTransaction.setContract(s2);
                        chainTransaction.setAmount(amountDecimal.divide(coinConfig.precision(), 16, RoundingMode.DOWN));
                        chainTransaction.setTokenSymbol(coinConfig.getTokenSymbol());
                        chainTransaction.setSymbol(coinConfig.getSymbol());
                    }
                } catch (InvalidProtocolBufferException e) {
                    log.error("1 TransferContract.parseFrom", e);
                    throw new RuntimeException(e);
                }
            }
            if (chainTransaction != null) {
                chainTransaction.setHash(hash);
                chainTransaction.setChainId(getChainId());
                chainTransaction.setNeedConfirmNum(mainCoinConfig.getConfirmCount());
                chainTransaction.setUrlCode(chainClient.getUrl());
                chainTransaction.setTxStatus(ChainTransaction.TX_STATUS.PENDING.name());
                try {
                 //   Response.TransactionInfo transactionInfo = chainClient.getClient().getTransactionInfoById(hash);
//                    long fee = transactionInfo.getFee();
//                    BigDecimal feeBig = new BigDecimal(fee);
                    chainTransaction.setBlockNum(chainScanConfig.getBlockHeight());
                } catch (Exception e) {
                    log.info("查询交易能量消耗失败,需要继续查询：txId={}，error", hash, e);
                    throw new RuntimeException(e);
                }
                chainTransactions.add(chainTransaction);
            }
        } else {
            ChainTransaction chainTransaction = new ChainTransaction();
            chainTransaction.setChainId(getChainId());
            chainTransaction.setHash(hash);
            chainTransaction.setUrlCode(chainClient.getUrl());
            chainTransaction.setTxStatus(ChainTransaction.TX_STATUS.FAIL.name());
            chainTransaction.setMessage(result);
            chainTransactions.add(chainTransaction);
        }
        return chainTransactions;
    }

    /**
     * 满足确认数之后，再次查询链上数据，是否存在，并且交易是否成功
     *
     * @param chainScanConfig
     * @param chainTransaction
     */
    @Override
    public void confirmTransaction(ChainScanConfig chainScanConfig, ChainTransaction chainTransaction) {
        List<ChainTransaction> chainTransactions = getChainTransaction(chainScanConfig, chainTransaction.getHash(), chainTransaction.getUrlCode());
        if (CollectionUtils.isNotEmpty(chainTransactions) && (StringUtils.equals(chainTransactions.get(0).getTxStatus(), ChainTransaction.TX_STATUS.SUCCESS.name()) || StringUtils.equals(chainTransactions.get(0).getTxStatus(), ChainTransaction.TX_STATUS.PENDING.name()))) {
            blockTransactionManager.updateTxStatus(chainTransaction.getChainId(), chainTransaction.getHash(), ChainTransaction.TX_STATUS.SUCCESS.name(), null, null, chainTransaction.getId(), true);
        } else {
            log.error("chain = {}\tconfirmTransaction = {},\tnot found or error", getChainId(), chainTransaction.getHash());
            blockTransactionManager.updateTxStatus(chainTransaction.getChainId(), chainTransaction.getHash(), ChainTransaction.TX_STATUS.FAIL.name(), null, null, chainTransaction.getId(), true);
        }
    }

    @Override
    public BigDecimal getTokenBalance(ChainScanConfig chainScanConfig, String address, String tokenAddress) {
        SymbolConfig coinConfig = configHashMap.get(tokenAddress);
        if (coinConfig == null) {
            return BigDecimal.ZERO;
        }
        return getChainClient(null).getClient().getTokenBalance(address, tokenAddress).divide(coinConfig.precision());
    }

    @Override
    public BigDecimal getBalance(ChainScanConfig chainScanConfig, String address) {
        return new BigDecimal(getChainClient(null).getClient().getAccountBalance(address)).divide(mainCoinConfig.precision());
    }

    @Override
    protected BigDecimal getBalance(String address, BigInteger blockNumber) {
        return getBalance(chainScanConfigService.getByChainId(this.getChainId()), address);
    }

    @Override
    protected BigDecimal getTokenBalance(String address, String tokenAddress, BigInteger blockNumber) {
        return getTokenBalance(chainScanConfigService.getByChainId(this.getChainId()), address, tokenAddress);
    }

    @Override
    public BigDecimal gas(ChainScanConfig chainScanConfig, SymbolConfig coinConfig) {
        String configJson = coinConfig.getConfigJson();
        return JSON.parseObject(configJson).getBigDecimal("feeLimit").divide(new BigDecimal("1000000"));
    }

    // endpoint 配置
    @Override
    protected ChainClient create(JSONObject item) {
        TrxChainClient trxChainClient = new TrxChainClient(item.getString("url"), // grpcEndpoint
                item.getString("grpcEndpointSolidity"), item.getString("privateKey"), item.getString("apiKey"), item.getString("triggerSmartContract"), item.getString("broadcastTransaction"), item.getString("createTransaction"));
        BlockChain<TrxChainClient>.ChainClient chainClient = new ChainClient(item.getString("url"), trxChainClient) {
            @Override
            public void close() throws IOException {
                trxChainClient.close();
            }
        };
        trxChainClient.setChainClient(chainClient);
        return chainClient;
    }

    @Override
    public LastBlockInfo getLastBlockInfo() throws Exception {
        Chain.Block block = getChainClient(null).getClient().getBlock();
        LastBlockInfo lastBlockInfo = new LastBlockInfo();
        lastBlockInfo.setBlockTime(new Date(block.getBlockHeader().getRawData().getTimestamp()));
        lastBlockInfo.setBlockNumber(new BigInteger(block.getBlockHeader().getRawData().getNumber() + ""));
        return lastBlockInfo;
    }

    class TrxChainClient {
        private final ApiWrapper apiWrapper;
        @Setter
        @Getter
        private ChainClient chainClient;
        private final String triggerSmartContractUrl;
        private final String broadcastTransactionUrl;
        private final String createTransactionUrl;
        private final HttpHeaders httpHeaders = new HttpHeaders();
        private final RestTemplate restTemplate = new RestTemplate();

        public TrxChainClient(String grpcEndpoint, String grpcEndpointSolidity, String privateKey, String apiKey, String triggerSmartContract, String broadcastTransaction, String createTransaction) {
            apiWrapper = StringUtils.isNotBlank(apiKey) ? new ApiWrapper(grpcEndpoint, grpcEndpointSolidity, privateKey, apiKey) : new ApiWrapper(grpcEndpoint, grpcEndpointSolidity, privateKey);
            this.triggerSmartContractUrl = triggerSmartContract;
            this.broadcastTransactionUrl = broadcastTransaction;
            this.createTransactionUrl = createTransaction;
            httpHeaders.add("Content-Type", "application/json;charset=UTF-8");
            httpHeaders.add("TRON-PRO-API-KEY", apiKey);
        }

        public Long getCurrentBlockNum() throws IllegalException {
            Chain.Block nowBlock = apiWrapper.getNowBlock();
            return nowBlock.getBlockHeader().getRawData().getNumber();
        }

        public Response.BlockExtention getBlock(long dealBlockNum) throws IllegalException {
            Response.BlockListExtention blockByLimitNext = apiWrapper.getBlockByLimitNext(dealBlockNum, dealBlockNum + 1);
            Response.BlockExtention block = blockByLimitNext.getBlock(0);
            return block;
        }

        public Chain.Block getBlock() throws IllegalException {
            return apiWrapper.getNowBlock();
        }

        public Response.TransactionInfo getTransactionInfoById(String txID) throws Exception {
            return apiWrapper.getTransactionInfoById(txID);
        }

        public void close() {
            apiWrapper.close();
        }

        public Chain.Transaction getTransactionById(String txid) throws IllegalException {
            return apiWrapper.getTransactionById(txid);
        }


        public long getAccountBalance(String address) {
            return apiWrapper.getAccountBalance(address);
        }

        public BigDecimal getTokenBalance(String address, String contractAddress) {
            Contract contract = apiWrapper.getContract(contractAddress);
            Trc20Contract token = new Trc20Contract(contract, address, apiWrapper);
            BigInteger balance = token.balanceOf(address);
            return new BigDecimal(balance.toString());
        }


        public Map<String, Object> broadcastTransaction(JSONObject transaction) {
            return restTemplate.postForEntity(broadcastTransactionUrl, new HttpEntity<>(transaction, httpHeaders), JSONObject.class).getBody();
        }

        public JSONObject buildTokenDto(ChainTransaction chainTransaction, SymbolConfig coinConfig) {
            TokenDto tokenDto = new TokenDto();
            tokenDto.setOwner_address(chainTransaction.getFromAddress());
            tokenDto.setContract_address(chainTransaction.getContract());
            tokenDto.setFunction_selector("transfer(address,uint256)");
            tokenDto.setVisible(true);
            Integer symbolPrecision = coinConfig.getSymbolPrecision();
            BigDecimal pow = BigDecimal.TEN.pow(symbolPrecision);
            BigDecimal amount = chainTransaction.getAmount();
            BigDecimal feeLimit = JSON.parseObject(chainTransaction.getGasConfig()).getBigDecimal("feeLimit");
            if (feeLimit == null) {
                feeLimit = JSON.parseObject(coinConfig.getConfigJson()).getBigDecimal("feeLimit");
            }
            tokenDto.setFee_limit(feeLimit.toBigInteger().longValue());
            /**Uint256 256位的无符号整数*/
            BigInteger priceBig = amount.multiply(pow).toBigInteger();
            List<Type> types = Arrays.asList(new Address(chainTransaction.getToAddress()), new Uint256(priceBig));
            String parameter = FunctionEncoder.encodeConstructor(types);
            tokenDto.setParameter(parameter);
            return restTemplate.postForEntity(triggerSmartContractUrl, new HttpEntity<>(tokenDto, httpHeaders), JSONObject.class).getBody();
        }

        public JSONObject buildCoinDto(ChainTransaction chainTransaction, SymbolConfig coinConfig) {
            BigDecimal amount = chainTransaction.getAmount();
            Integer symbolPrecision = coinConfig.getSymbolPrecision();
            BigDecimal pow = BigDecimal.TEN.pow(symbolPrecision);
            amount = amount.multiply(pow);//处理精度
            CoinDto coinDto = new CoinDto();
            coinDto.setOwner_address(chainTransaction.getFromAddress());
            coinDto.setTo_address(chainTransaction.getToAddress());
            coinDto.setAmount(amount.longValue());
            coinDto.setVisible(true);
            return restTemplate.postForEntity(createTransactionUrl, new HttpEntity<>(coinDto, httpHeaders), JSONObject.class).getBody();
        }
    }

    @Override
    public void freshChainScanConfig(ChainScanConfig chainScanConfig) {
        super.freshChainScanConfig(chainScanConfig);
        HashMap<String, SymbolConfig> configHashMap = new HashMap<>();
        List<SymbolConfig> coinConfigs = chainScanConfig.getCoinConfigs();
        if (CollectionUtils.isNotEmpty(coinConfigs)) {
            for (SymbolConfig coinConfig : coinConfigs) {
                if (StringUtils.isNotBlank(coinConfig.getContractAddress())) {
                    configHashMap.put(coinConfig.getContractAddress(), coinConfig);
                }
            }
        }
        this.configHashMap = configHashMap;
    }

    public boolean isValidTronAddress(String address) {
        try {
            byte[] addressBytes = Base58.decode(address);
            if (addressBytes.length != 25) {
                return false;
            }

            byte[] versionAndHash = Arrays.copyOfRange(addressBytes, 0, 21);
            byte[] checksum = Arrays.copyOfRange(addressBytes, 21, 25);

            byte[] versionHashChecksum = Arrays.copyOfRange(Sha256(Sha256(versionAndHash)), 0, 4);

            return Arrays.equals(checksum, versionHashChecksum);
        } catch (Exception e) {
            return false;
        }
    }

    private static byte[] Sha256(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(data);
    }

}
