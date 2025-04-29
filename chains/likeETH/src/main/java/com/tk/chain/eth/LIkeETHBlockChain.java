package com.tk.chain.eth;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.tk.chains.BlockChain;
import com.tk.chains.event.TransactionEvent;
import com.tk.chains.exceptions.GasException;
import com.tk.wallet.common.entity.ChainScanConfig;
import com.tk.wallet.common.entity.ChainTransaction;
import com.tk.wallet.common.entity.SymbolConfig;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;
import org.web3j.protocol.core.methods.request.EthFilter;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.*;

/*
========================
chain_scan_config 配置
insert into waas.chain_scan_config(chain_id,endpoints,status,ctime,mtime,block_number,block_height,scan_time) values('BSC','',0,now(),now(),0,0,now());
endpoints 设置说明
    {
    "fields":{
        "title":"地址说明",
        "url":"节点地址"
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
            "url":"https://ethereum-goerli.publicnode.com"
        }
    ]
}
===========================
*/
public class LIkeETHBlockChain extends BlockChain<Web3j> {

    private Integer id;

    private static final String topic = "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef";

    private volatile Map<String, SymbolConfig> configHashMap = new HashMap<>();


    public boolean configIsOk() {
        return super.configIsOk() && this.id != null;
    }

    @Override
    public SymbolConfig getTokenConfig(String contractAddress) {
        return configHashMap.get(contractAddress.toLowerCase());
    }

    @SuppressWarnings("all")
    private ChainTransaction erc20LogResult(EthLog.LogResult<EthLog.LogObject> logResult, ChainClient chainClient, BigInteger blockNumber, Date blockTime) {
        EthLog.LogObject logObject = logResult.get();
        if (logObject != null && CollectionUtils.isNotEmpty(logObject.getTopics()) && logObject.getTopics().size() == 3 && logObject.getTopics().get(0).equals(topic)) {
            String hash = logObject.getTransactionHash();
            String contract = logObject.getAddress().toLowerCase();
            String from = "0x" + logObject.getTopics().get(1).substring(26).toLowerCase();
            String to = "0x" + logObject.getTopics().get(2).substring(26).toLowerCase();
            BigInteger amount = new BigInteger(logObject.getData().substring(2), 16);
            if (this.configHashMap.containsKey(contract) && addressChecker.owner(from, to, chainId, hash)) {
                SymbolConfig symbolConfig = this.configHashMap.get(contract);
                ChainTransaction chainTransaction = new ChainTransaction();
                chainTransaction.setHash(hash);
                chainTransaction.setContract(contract);
                chainTransaction.setFromAddress(from);
                chainTransaction.setToAddress(to);
                chainTransaction.setChainId(chainId);
                chainTransaction.setAmount(new BigDecimal(amount).divide(symbolConfig.precision(), 18, RoundingMode.DOWN));
                chainTransaction.setTxStatus(ChainTransaction.TX_STATUS.PENDING.name());
                chainTransaction.setBlockNum(blockNumber);
                chainTransaction.setNeedConfirmNum(this.mainCoinConfig.getConfirmCount());
                chainTransaction.setBlockTime(blockTime);
                chainTransaction.setUrlCode(chainClient.getUrl());
                chainTransaction.setTokenSymbol(symbolConfig.getTokenSymbol());
                chainTransaction.setSymbol(symbolConfig.getSymbol());
                return chainTransaction;
            }
        }
        return null;
    }

    @Override
    @SuppressWarnings("all")
    public ScanResult scan(ChainScanConfig chainScanConfig, BigInteger blockNumber, ChainClient chainClient) throws Exception {
        EthBlock.Block block = chainClient.getClient()//
                .ethGetBlockByNumber(DefaultBlockParameter.valueOf(blockNumber), true).send().getResult();
        BigInteger timestamp = block.getTimestamp();
        Date blockTime = new Date(timestamp.longValue() * 1000);
        List<EthBlock.TransactionResult> transactions = block.getTransactions();
        HashMap<String, List<ChainTransaction>> chainTransactionsMap = new HashMap<>();
        // 指定合约，hash, topic 查询
        EthFilter ethFilter = new EthFilter(DefaultBlockParameter.valueOf(blockNumber), DefaultBlockParameter.valueOf(blockNumber), Lists.newArrayList(configHashMap.keySet()));
        ethFilter.addSingleTopic(topic);
        List<EthLog.LogResult> logs = chainClient.getClient().ethGetLogs(ethFilter).send().getLogs();
        boolean gotLog = false;
        if (CollectionUtils.isNotEmpty(logs)) {
            gotLog = true;
            for (EthLog.LogResult<EthLog.LogObject> logResult : logs) {
                ChainTransaction chainTransaction = erc20LogResult(logResult, chainClient, blockNumber, blockTime);
                if (chainTransaction != null) {
                    List<ChainTransaction> chainTransactions = chainTransactionsMap.getOrDefault(chainTransaction.getContract(), new ArrayList<>());
                    chainTransactions.add(chainTransaction);
                    chainTransactionsMap.put(chainTransaction.getHash(), chainTransactions);
                }
            }
        }
        List<ChainTransaction> chainTransactions = new ArrayList<>();
        for (EthBlock.TransactionResult<EthBlock.TransactionObject> transactionResult : transactions) {
            if (transactionResult.get() == null) {
                continue;
            }
            EthBlock.TransactionObject transactionObject = transactionResult.get();
            if (transactionObject.getTo() == null) {
                continue;
            }
            // 合约代币
            if (chainTransactionsMap.containsKey(transactionObject.getHash())) {
                List<ChainTransaction> chainTransactionList = chainTransactionsMap.get(transactionObject.getHash());
                ChainTransaction chainTransaction = chainTransactionList.get(0);
                log.debug("扫描到交易 ：blockNumber={},\thash={},\tfrom={},\tto={},\ttoken={},\tamount={}", blockNumber, transactionObject.getHash(), chainTransaction.getFromAddress(), chainTransaction.getToAddress(), transactionObject.getTo(), chainTransaction.getAmount());
                if (addressChecker.owner(chainTransaction.getFromAddress(), chainTransaction.getToAddress())) { // 主动推送的交易，需要统计手续费
                    Optional<TransactionReceipt> transactionReceipt = chainClient.getClient().ethGetTransactionReceipt(chainTransaction.getHash()).send().getTransactionReceipt();
                    chainTransaction.setGas(new BigDecimal(transactionObject.getGasPrice().multiply(transactionObject.getGas())));
                    chainTransaction.setGasAddress(transactionObject.getFrom().toLowerCase());
                    updateStatusAndGas(transactionReceipt, chainTransaction);
                }
                chainTransactions.addAll(chainTransactionList);
            } else {
                ChainTransaction chainTransaction;
                if (!gotLog && transactionObject.getTo() != null && configHashMap.containsKey(transactionObject.get().getTo().toLowerCase())) {
                    chainTransaction = erc20Token(transactionObject, configHashMap.get(transactionObject.getTo().toLowerCase()));
                } else {
                    // 主币
                    chainTransaction = coin(transactionObject, this.mainCoinConfig);
                }
                if (chainTransaction != null && (addressChecker.owner(chainTransaction.getFromAddress(), chainTransaction.getToAddress(), getChainId(), transactionObject.getHash()))) {
                    log.debug("扫描到交易 ：blockNumber={},\thash={},\tfrom={},\tto={},contract={},\tamount={}", blockNumber, transactionObject.getHash(), chainTransaction.getFromAddress(), chainTransaction.getToAddress(), chainTransaction.getContract(), chainTransaction.getAmount());
                    chainTransaction.setBlockNum(blockNumber);
                    chainTransaction.setNeedConfirmNum(this.mainCoinConfig.getConfirmCount());
                    Optional<TransactionReceipt> transactionReceipt = chainClient.getClient().ethGetTransactionReceipt(chainTransaction.getHash()).send().getTransactionReceipt();
                    chainTransaction.setGas(new BigDecimal(transactionObject.getGasPrice().multiply(transactionObject.getGas())));
                    updateStatusAndGas(transactionReceipt, chainTransaction);
                    if (chainTransaction.getTxStatus().equalsIgnoreCase(ChainTransaction.TX_STATUS.SUCCESS.name())) {
                        chainTransaction.setTxStatus(ChainTransaction.TX_STATUS.PENDING.name());
                    }
                    chainTransaction.setBlockTime(blockTime);
                    chainTransactions.add(chainTransaction);
                }
            }
        }
        return new ScanResult(transactions.size(), chainTransactions, blockNumber, blockTime);
    }

    @Override
    public String blockHeight(ChainScanConfig chainScanConfig) {
        return getCurrentNumber().toString();
    }

    /**
     * 设置交易状态
     *
     * @param transactionReceipt 链上状态
     * @param chainTransaction   交易
     */
    @SuppressWarnings("all")
    private void updateStatusAndGas(Optional<TransactionReceipt> transactionReceipt, ChainTransaction chainTransaction) {
        if (transactionReceipt.isPresent()) {
            chainTransaction.setActGas(new BigDecimal(new BigInteger(transactionReceipt.get().getEffectiveGasPrice().substring(2), 16).multiply(transactionReceipt.get().getGasUsed())));
            BigDecimal gas = Convert.fromWei(chainTransaction.getGas().toString(), getConvertUnit(this.mainCoinConfig.getSymbolPrecision(), getChainId()));
            chainTransaction.setGas(gas);
            BigDecimal actGas = Convert.fromWei(chainTransaction.getActGas().toString(), getConvertUnit(this.mainCoinConfig.getSymbolPrecision(), getChainId()));
            chainTransaction.setActGas(actGas);
            if (transactionReceipt.get().isStatusOK()) {
                chainTransaction.setTxStatus(ChainTransaction.TX_STATUS.SUCCESS.name());
            } else {
                chainTransaction.setTxStatus(ChainTransaction.TX_STATUS.FAIL.name());
                chainTransaction.setFailCode(ChainTransaction.FAIL_CODE.OUT_OF_GAS.name());
            }
            chainTransaction.setNeedConfirmNum(mainCoinConfig.getConfirmCount());
        } else {
            chainTransaction.setTxStatus(ChainTransaction.TX_STATUS.FAIL.name());
            chainTransaction.setFailCode(ChainTransaction.FAIL_CODE.CHAIN_NOT_FOUND.name());
        }
    }

    /**
     * erc 20 代币
     *
     * @param transactionObject 交易信息
     * @param coinConfig        币种配置
     * @return 交易信息
     */
    @SuppressWarnings("all")
    private ChainTransaction erc20Token(Transaction transactionObject, SymbolConfig coinConfig) {
        String input = transactionObject.getInput();
        String erc20MethodId = "0xa9059cbb";
        if (StringUtils.isNotBlank(input) && StringUtils.startsWith(input, erc20MethodId)) {
            try {
                ChainTransaction chainTransaction = new ChainTransaction();
                List<TypeReference<Type>> outputParameters = new ArrayList<>();
                outputParameters.add((TypeReference) new TypeReference<Address>() {

                });
                outputParameters.add((TypeReference) new TypeReference<Uint256>() {
                });
                List<Type> typeList = FunctionReturnDecoder.decode(input.substring(erc20MethodId.length()), outputParameters);
                Address toAddress = (Address) typeList.get(0);
                BigDecimal amount = new BigDecimal(String.valueOf(typeList.get(1).getValue()));
                chainTransaction.setChainId(getChainId());
                chainTransaction.setFromAddress(transactionObject.getFrom());
                chainTransaction.setToAddress(toAddress.getValue());
                chainTransaction.setAmount(amount.divide(coinConfig.precision(), 16, RoundingMode.DOWN));
                chainTransaction.setHash(transactionObject.getHash());
                chainTransaction.setContract(transactionObject.getTo());
                chainTransaction.setGasAddress(transactionObject.getFrom());
                chainTransaction.setTokenSymbol(coinConfig.getTokenSymbol());
                chainTransaction.setSymbol(coinConfig.getSymbol());
                chainTransaction.setNonce(transactionObject.getNonce());
                return chainTransaction;
            } catch (Exception e) {
                log.warn("erc20 解析异常", e);
            }
        }
        return null;
    }

    /**
     * coin 解析
     *
     * @param transactionObject 链上状态
     * @param coinConfig        币种配置
     * @return 交易
     */
    private ChainTransaction coin(Transaction transactionObject, SymbolConfig coinConfig) {
        BigDecimal amount = Convert.fromWei(transactionObject.getValue().toString(), getConvertUnit(coinConfig.getSymbolPrecision(), ""));
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            // 忽略的代币
            return null;
        }
        String input = transactionObject.getInput();
        if (StringUtils.isNotBlank(input) && input.length() > 2) {
            return null;
        }
        ChainTransaction chainTransaction = new ChainTransaction();
        chainTransaction.setChainId(getChainId());
        chainTransaction.setHash(transactionObject.getHash());
        chainTransaction.setBlockNum(transactionObject.getBlockNumber());
        chainTransaction.setCtime(new Date());
        chainTransaction.setContract("");
        BigDecimal gas = new BigDecimal(transactionObject.getGasPrice().multiply(transactionObject.getGas()));
        gas = gas.divide(coinConfig.precision(), 18, RoundingMode.DOWN);
        chainTransaction.setGas(gas);
        chainTransaction.setActGas(BigDecimal.ZERO);
        chainTransaction.setFromAddress(transactionObject.getFrom());
        chainTransaction.setToAddress(transactionObject.getTo());
        chainTransaction.setGasAddress(transactionObject.getFrom());
        chainTransaction.setAmount(amount);
        chainTransaction.setTokenSymbol(mainCoinConfig.getTokenSymbol());
        chainTransaction.setSymbol(this.mainCoinConfig.getSymbol());
        chainTransaction.setNonce(transactionObject.getNonce());
        return chainTransaction;
    }

    public Convert.Unit getConvertUnit(Integer unit, String symbol) {
        switch (unit) {
            case 0:
                return Convert.Unit.WEI;
            case 3:
                return Convert.Unit.KWEI;
            case 6:
                return Convert.Unit.MWEI;
            case 9:
                return Convert.Unit.GWEI;
            case 12:
                return Convert.Unit.SZABO;
            case 15:
                return Convert.Unit.FINNEY;
            case 18:
                return Convert.Unit.ETHER;
            case 21:
                return Convert.Unit.KETHER;
            case 25:
                return Convert.Unit.METHER;
            case 27:
                return Convert.Unit.GETHER;
            default:
                throw new IllegalArgumentException(unit + ": 币种精度配置错误！" + symbol);
        }
    }

    @Override
    public void checkChainTransaction(ChainTransaction chainTransaction) throws RuntimeException {
        String gasConfig = chainTransaction.getGasConfig();
        if (StringUtils.isBlank(gasConfig)) {
            throw new GasException(this.getChainId(), "gasConfig error : " + gasConfig);
        }
        JSONObject jsonObject = JSON.parseObject(gasConfig);
        if (jsonObject.containsKey("limit") && jsonObject.getLong("limit") > 0) {
            if (jsonObject.containsKey("gasPrice")) {
                String value = jsonObject.getString("gasPrice");
                if (!StringUtils.isNumeric(value)) {
                    throw new GasException(this.getChainId(), "gasPrice = " + value + " 设置错误，必须是数字");
                }
                if (new BigDecimal(value).compareTo(BigDecimal.ZERO) <= 0) {
                    throw new GasException(this.getChainId(), "gasPrice = " + value + " 设置错误，必须大于0");
                }
            }
        } else {
            throw new GasException(this.getChainId(), "gasConfig limit must be set current config = " + jsonObject.toJSONString());
        }
    }

    private BigInteger getCurrentNumber() {
        ChainClient chainClient = getChainClient(null);
        if (chainClient != null) {
            try {
                return chainClient.getClient().ethBlockNumber().send().getBlockNumber();
            } catch (Exception e) {
                markClientError(chainClient);
                log.warn("ethBlockNumber", e);
                return BigInteger.ZERO;
            }
        } else {
            return BigInteger.ZERO;
        }
    }

    /**
     * 查询链上交易
     *
     * @param chainScanConfig 链配置
     * @param hash            hash
     * @param excludeUrl      排除的节点
     * @return 交易信息
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<ChainTransaction> getChainTransaction(ChainScanConfig chainScanConfig, String hash, String excludeUrl) {
        List<ChainTransaction> chainTransactions = new ArrayList<>();
        ChainClient chainClient = getChainClient(null);
        try {
            Optional<Transaction> transactionOptional = chainClient.getClient().ethGetTransactionByHash(hash).send().getTransaction();
            Optional<TransactionReceipt> transactionReceiptOptional = chainClient.getClient().ethGetTransactionReceipt(hash).send().getTransactionReceipt();
            if (transactionOptional.isPresent() && transactionReceiptOptional.isPresent()) {
                Transaction transaction = transactionOptional.get();
                ChainTransaction chainTransaction = null;
                if (configHashMap.containsKey(transaction.getTo().toLowerCase())) {
                    EthFilter ethFilter = new EthFilter(DefaultBlockParameter.valueOf(transaction.getBlockNumber()), DefaultBlockParameter.valueOf(transaction.getBlockNumber()), Lists.newArrayList(transaction.getFrom()));
                    ethFilter.addSingleTopic(topic);
                    EthLog ethLog = chainClient.getClient().ethGetLogs(ethFilter).send();
                    if (ethLog != null && ethLog.getLogs() != null) {
                        for (EthLog.LogResult<EthLog.LogObject> logResult : ethLog.getLogs()) {
                            chainTransaction = erc20LogResult(logResult, chainClient, transactionOptional.get().getBlockNumber(), chainScanConfig.getLastBlockTime());
                            if (chainTransaction != null) {
                                chainTransactions.add(chainTransaction);
                            }
                        }
                    } else {
                        chainTransaction = erc20Token(transaction, this.configHashMap.get(transaction.getTo().toLowerCase()));
                    }
                } else {
                    chainTransaction = coin(transaction, this.mainCoinConfig);
                }
                if (chainTransaction != null) {
                    chainTransaction.setGas(new BigDecimal(transaction.getGasPrice().multiply(transaction.getGas())));
                    chainTransaction.setBlockNum(transaction.getBlockNumber());
                    updateStatusAndGas(transactionReceiptOptional, chainTransaction);
                    chainTransactions.add(chainTransaction);
                }
            }
        } catch (Exception e) {
            log.warn("getChainTransaction = {}", hash, e);
            markClientError(chainClient);
        }
        return chainTransactions;
    }

    @Override
    public void confirmTransaction(ChainScanConfig chainScanConfig, ChainTransaction chainTransaction) {
        ChainClient chainClient = getChainClient(new HashSet<String>() {
            {
                add(chainTransaction.getUrlCode());
            }
        });
        Optional<TransactionReceipt> transactionReceiptOptional;
        try {
            transactionReceiptOptional = chainClient.getClient().ethGetTransactionReceipt(chainTransaction.getHash()).send().getTransactionReceipt();
        } catch (Exception e) {
            log.error("{}\tconfirmTransaction = {}", chainTransaction.getChainId(), chainTransaction.getHash(), e);
            markClientError(chainClient);
            return;
        }
        if (transactionReceiptOptional.isPresent() && transactionReceiptOptional.get().isStatusOK()) {
            blockTransactionManager.updateTxStatus(chainTransaction.getChainId(), chainTransaction.getHash(), ChainTransaction.TX_STATUS.SUCCESS.name(), null, null, chainTransaction.getId(), true);
        } else {
            log.error("chain = {}\tconfirmTransaction = {},\tnot found", getChainId(), chainTransaction.getHash());
            blockTransactionManager.updateTxStatus(chainTransaction.getChainId(), chainTransaction.getHash(), ChainTransaction.TX_STATUS.FAIL.name(), ChainTransaction.FAIL_CODE.CHAIN_NOT_FOUND.name(), null, chainTransaction.getId(), true);
        }
    }


    public String formatAddress(String address) {
        return this.isValidTronAddress(address) ? address.toLowerCase() : "";
    }

    @Override
    public void formatAddress(ChainTransaction chainTransaction) {
        if (chainTransaction == null) {
            return;
        }
        if (StringUtils.isNotBlank(chainTransaction.getFromAddress())) {
            chainTransaction.setFromAddress(chainTransaction.getFromAddress().toLowerCase());
        }
        if (StringUtils.isNotBlank(chainTransaction.getToAddress())) {
            chainTransaction.setToAddress(chainTransaction.getToAddress().toLowerCase());
        }
        if (StringUtils.isNotBlank(chainTransaction.getContract())) {
            chainTransaction.setContract(chainTransaction.getContract().toLowerCase());
        }
    }

    @Override
    public BigDecimal getTokenBalance(ChainScanConfig chainScanConfig, String address, String tokenAddress) {
        SymbolConfig coinConfig = configHashMap.get(tokenAddress.toLowerCase());
        if (coinConfig == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal erc20BalanceOrigin = getERC20BalanceOrigin(chainScanConfig, address, tokenAddress, DefaultBlockParameterName.LATEST);
        return erc20BalanceOrigin.divide(coinConfig.precision());
    }

    private BigDecimal getERC20BalanceOrigin(ChainScanConfig chainScanConfig, String address, String contractAddress, DefaultBlockParameter blockParameter) {
        ChainClient chainClient = getChainClient(null);
        String methodName = "balanceOf";
        List<Type> inputParameters = new ArrayList<>();
        List<TypeReference<?>> outputParameters = new ArrayList<>();
        Address fromAddress = new Address(address);
        inputParameters.add(fromAddress);
        TypeReference<Uint256> typeReference = new TypeReference<Uint256>() {
        };
        outputParameters.add(typeReference);
        org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(methodName, inputParameters, outputParameters);
        String data = FunctionEncoder.encode(function);
        org.web3j.protocol.core.methods.request.Transaction transaction = org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(address, contractAddress, data);
        EthCall ethCall;
        try {
            ethCall = chainClient.getClient().ethCall(transaction, blockParameter).send();
            List<Type> results = FunctionReturnDecoder.decode(ethCall.getValue(), function.getOutputParameters());
            BigInteger value = new BigInteger("0");
            if (results != null && !results.isEmpty()) {
                value = new BigInteger(String.valueOf(results.get(0).getValue()));
            }
            return new BigDecimal(value);
        } catch (Exception e) {
            log.error("getERC20Balance : " + address, e);
            return BigDecimal.ZERO;
        }
    }

    @Override
    public BigDecimal getBalance(ChainScanConfig chainScanConfig, String address) {
        BigDecimal balanceOrigin = getBalanceOrigin(chainScanConfig, address, DefaultBlockParameterName.LATEST);
        return balanceOrigin.divide(this.mainCoinConfig.precision());
    }

    @Override
    protected BigDecimal getBalance(String address, BigInteger blockNumber) {
        BigDecimal balanceOrigin = getBalanceOrigin(null, address, DefaultBlockParameter.valueOf(blockNumber));
        return balanceOrigin.divide(this.mainCoinConfig.precision(), 16, RoundingMode.DOWN);
    }

    @Override
    protected BigDecimal getTokenBalance(String address, String tokenAddress, BigInteger blockNumber) {
        SymbolConfig coinConfig = configHashMap.get(tokenAddress.toLowerCase());
        if (coinConfig == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal erc20BalanceOrigin = getERC20BalanceOrigin(null, address, tokenAddress, DefaultBlockParameter.valueOf(blockNumber));
        return erc20BalanceOrigin.divide(coinConfig.precision());
    }

    private BigDecimal getBalanceOrigin(ChainScanConfig chainScanConfig, String address, DefaultBlockParameter defaultBlockParameter) {
        ChainClient chainClient = getChainClient(null);
        try {
            // DefaultBlockParameterName.LATEST
            BigInteger balance = chainClient.getClient().ethGetBalance(address, defaultBlockParameter).send().getBalance();
            BigDecimal amount = new BigDecimal(balance.toString());
            return amount;
        } catch (IOException e) {
            markClientError(chainClient);
            return BigDecimal.ZERO;
        }
    }


    // 推送重复交易
    public void reTransfer(ChainScanConfig chainScanConfig, List<ChainTransaction> chainTransactions) {
        for (ChainTransaction transaction : chainTransactions) {
            ChainClient chainClient = getChainClient(new HashSet<String>() {
                {
                    if (StringUtils.isNotBlank(transaction.getUrlCode())) {
                        add(transaction.getUrlCode());
                    }
                }
            });
            String hexValue = "";
            if (StringUtils.isNotBlank(transaction.getChainInfo())) {
                hexValue = JSON.parseObject(transaction.getChainInfo()).getString("hexValue");
            }
            if (StringUtils.isBlank(hexValue)) {
                blockTransactionManager.updateTxStatus(transaction.getId(), ChainTransaction.TX_STATUS.FAIL.name(), null, "", chainScanConfig.getBlockHeight(), true);
                return;
            }

            //发送交易
            EthSendTransaction ethSendTransaction = null;
            log.info("start_reTransfer_0 : id={},\tBusinessId={},\tchainInfo={}", transaction.getId(), transaction.getBusinessId(), transaction.getChainInfo());
            try {
                ethSendTransaction = chainClient.getClient().ethSendRawTransaction(hexValue).sendAsync().get();
            } catch (Exception e) {
                blockTransactionManager.releaseWaitingHash(transaction.getId());
                log.error("3_ethSendRawTransaction : {}", transaction.getId(), e);
                markClientError(chainClient);
                return;
            }
            if (ethSendTransaction == null || ethSendTransaction.hasError()) {
                String errorMessage = ethSendTransaction == null ? "" : ethSendTransaction.getError().getMessage();
                if (StringUtils.equals("already known", errorMessage) || StringUtils.startsWith(errorMessage, "nonce too low: next nonce")) {
                    List<ChainTransaction> list = chainTransactionService.lambdaQuery().eq(ChainTransaction::getNonce, transaction.getNonce()).eq(ChainTransaction::getChainId, transaction.getChainId()).eq(ChainTransaction::getFromAddress, transaction.getFromAddress()).ne(ChainTransaction::getId, transaction.getId()).list();

                    long count = list.stream().filter(f -> StringUtils.equals(f.getTxStatus(), ChainTransaction.TX_STATUS.SUCCESS.name()) || (StringUtils.isNotBlank(f.getHash()) && StringUtils.equals(f.getTxStatus(), ChainTransaction.TX_STATUS.PENDING.name())) || (StringUtils.isNotBlank(f.getHash()) && StringUtils.isNotBlank(f.getFailCode()))).count();
                    if (count > 0) {
                        ChainTransaction update = new ChainTransaction();
                        update.setId(transaction.getId());
                        update.setTxStatus(ChainTransaction.TX_STATUS.FAIL.name());
                        update.setNonce(new BigInteger("0"));
                        chainTransactionService.updateById(update);
                        eventManager.emit(new TransactionEvent(chainScanConfig.getChainId(), transaction, null));
                    } else {
                        ChainTransaction update = new ChainTransaction();
                        update.setId(transaction.getId());
                        update.setTxStatus(ChainTransaction.TX_STATUS.WAIT_TO_CHAIN.name());
                        chainTransactionService.updateById(update);
                    }
                } else if (StringUtils.startsWith(errorMessage, "transaction underpriced")) {
                    ChainTransaction update = new ChainTransaction();
                    update.setId(transaction.getId());
                    update.setTxStatus(ChainTransaction.TX_STATUS.FAIL.name());
                    chainTransactionService.updateById(update);
                    transaction.setTxStatus(ChainTransaction.TX_STATUS.FAIL.name());
                    eventManager.emit(new TransactionEvent(chainScanConfig.getChainId(), transaction, null));
                } else {
                    log.error("4_ethSendRawTransaction : {},\t{}", transaction.getId(), errorMessage);
                    if (transaction.getErrorCount() < 5) {
                        blockTransactionManager.releaseWaitingHash(transaction.getId());
                    } else {
                        blockTransactionManager.updateTxStatus(transaction.getId(), ChainTransaction.TX_STATUS.FAIL.name(), null, errorMessage, chainScanConfig.getBlockHeight(), true);
                    }
                    if (StringUtils.startsWith(errorMessage, "nonce too low: next nonce")) {
                        ChainTransaction updateChainTransaction = new ChainTransaction();
                        updateChainTransaction.setId(transaction.getId());
                        updateChainTransaction.setMessage(errorMessage);
                        chainTransactionService.updateById(updateChainTransaction);
                    }
                }
            } else {
                String hash = ethSendTransaction.getTransactionHash();
                blockTransactionManager.updateHash(transaction.getId(), hash, transaction.getChainInfo());
            }
        }
    }

    @Override
    public void transfer(ChainScanConfig chainScanConfig, List<ChainTransaction> chainTransactions) {
        // 转为小写
        for (ChainTransaction chainTransaction : chainTransactions) {
            formatAddress(chainTransaction);
        }
        ChainClient chainClient = getChainClient(null);
        if (CollectionUtils.isEmpty(chainTransactions)) {
            return;
        }
        // 当前区块高度
        BigInteger blockHeight = new BigInteger(blockHeight(chainScanConfig));
        String fromAddress = chainTransactions.get(0).getFromAddress();
        BigInteger coinBalance = getBalanceOrigin(chainScanConfig, fromAddress, DefaultBlockParameterName.LATEST).toBigInteger();
        // 涉及到转账的token 余额
        HashMap<String, BigInteger> tokenBalances = new HashMap<>();
        // 涉及到转账的token 精度
        for (ChainTransaction chainTransaction : chainTransactions) {
            if (StringUtils.isNotBlank(chainTransaction.getContract())) {
                if (!tokenBalances.containsKey(chainTransaction.getContract())) {
                    BigDecimal erc20Balance = getERC20BalanceOrigin(chainScanConfig, fromAddress, chainTransaction.getContract(), DefaultBlockParameterName.LATEST);
                    tokenBalances.put(chainTransaction.getContract(), erc20Balance.toBigInteger());
                }
            }
        }
        LinkedList<ChainTransaction> chainTransactionLinkedList = new LinkedList<>(chainTransactions);
        Iterator<ChainTransaction> iterator = chainTransactionLinkedList.iterator();
        while (iterator.hasNext()) {
            ChainTransaction chainTransaction = iterator.next();
            if (StringUtils.isNotBlank(chainTransaction.getContract())) {
                SymbolConfig coinConfig = configHashMap.get(chainTransaction.getContract().toLowerCase());
                // 无法转换精度，排除本次转账
                if (coinConfig == null) {
                    log.error("合约配置错误 contract address = {},id = \t{}", chainTransaction.getContract(), chainTransaction.getId());
                    blockTransactionManager.updateTxStatus(chainTransaction.getId(), ChainTransaction.TX_STATUS.FAIL.name(), null, "合约配置错误", chainScanConfig.getBlockHeight(), true);
                    iterator.remove();
                } else {
                    // 转为链上uint256 数值（链上没有小数），金额*精度
                    BigDecimal amount = chainTransaction.getAmount().multiply(coinConfig.precision());
                    chainTransaction.setAmount(amount);
                }
            } else {
                BigDecimal amount = chainTransaction.getAmount().multiply(mainCoinConfig.precision());
                chainTransaction.setAmount(amount);
            }
        }

        // 可以发起的交易
        List<ChainTransaction> okList = new ArrayList<>();
        // 燃料不足的交易
        List<ChainTransaction> gasLowerList = new ArrayList<>();
        // 余额不足的交易
        List<ChainTransaction> balanceNotEnoughList = new ArrayList<>();
        // 旷工报价
        BigInteger gasPrice = null;
        // 经过精度转换后的交易
        for (ChainTransaction chainTransaction : chainTransactionLinkedList) {
            JSONObject gasConfig = JSON.parseObject(chainTransaction.getGasConfig());
            // strategy : float: 参考旷工报价, fixed 固定gas 不考虑旷工报价, fixed_higher：要求比旷工价格高
            String strategy = gasConfig.getString("strategy");
            BigInteger limit = gasConfig.getBigInteger("limit");
            BigInteger transferPrice;
            if (chainTransaction.getGas() != null && chainTransaction.getGas().compareTo(BigDecimal.ZERO) > 0) {
                transferPrice = chainTransaction.getGas().multiply(mainCoinConfig.precision()).toBigInteger().divide(limit);
            } else {
                // 自定义报价,不参考旷工
                if (StringUtils.startsWithIgnoreCase("fixed", strategy) && chainTransaction.getGas() != null && chainTransaction.getGas().compareTo(BigDecimal.ZERO) > 0) {
                    BigInteger gas = chainTransaction.getGas().multiply(mainCoinConfig.precision()).toBigInteger();
                    transferPrice = gas.divide(limit);
                } else if (StringUtils.startsWithIgnoreCase("fixed_higher", strategy) && chainTransaction.getGas() != null && chainTransaction.getGas().compareTo(BigDecimal.ZERO) > 0) {
                    BigInteger gas = chainTransaction.getGas().multiply(mainCoinConfig.precision()).toBigInteger();
                    // 配置的价格高于旷工价格，但是采用旷工报价
                    transferPrice = gas.divide(limit);
                    if (gasPrice == null) {
                        try {
                            gasPrice = chainClient.getClient().ethGasPrice().send().getGasPrice();
                        } catch (Exception e) {
                            markClientError(chainClient);
                            return;
                        }
                    }
                    if (transferPrice.compareTo(gasPrice) < 0) {
                        gasLowerList.add(chainTransaction);
                        continue;
                    }
                    // 按照旷工报价
                    transferPrice = gasPrice;
                } else {
                    if (gasPrice == null) {
                        if (gasConfig.containsKey("gasPrice")) {
                            gasPrice = gasConfig.getBigInteger("gasPrice");
                        } else {
                            try {
                                gasPrice = chainClient.getClient().ethGasPrice().send().getGasPrice();
                            } catch (Exception e) {
                                markClientError(chainClient);
                                return;
                            }
                        }
                    }
                    // 按照旷工报价
                    transferPrice = gasPrice;
                }
            }
            BigInteger transferGas = transferPrice.multiply(limit);
            // erc-20 转账
            if (StringUtils.isNotBlank(chainTransaction.getContract())) {
                if (coinBalance.compareTo(transferGas) < 0) {
                    gasLowerList.add(chainTransaction);
                } else {
                    BigInteger amount = tokenBalances.get(chainTransaction.getContract());
                    if (amount.compareTo(chainTransaction.getAmount().toBigInteger()) < 0) {
                        balanceNotEnoughList.add(chainTransaction);
                    } else {
                        if (CollectionUtils.isEmpty(okList)) {
                            EthGetTransactionCount transactionCount = null;
                            try {
                                transactionCount = chainClient.getClient().ethGetTransactionCount(fromAddress, DefaultBlockParameterName.LATEST).sendAsync().get();
                            } catch (Exception e) {
                                markClientError(chainClient);
                                return;
                            }
                            BigInteger nonce = transactionCount.getTransactionCount();
                            chainTransaction.setNonce(nonce);
                        } else {
                            BigInteger nonce = okList.get(okList.size() - 1).getNonce().add(new BigInteger("1"));
                            chainTransaction.setNonce(nonce);
                        }
                        chainTransaction.setCoinBalance(coinBalance);
                        chainTransaction.setTransferGas(transferGas);
                        chainTransaction.setTransferPrice(transferPrice);
                        chainTransaction.setTokenBalance(amount);
                        chainTransaction.setLimit(limit);
                        tokenBalances.put(chainTransaction.getContract(), amount.subtract(chainTransaction.getAmount().toBigInteger()));
                        coinBalance = coinBalance.subtract(transferGas);
                        okList.add(chainTransaction);
                    }
                }
            } else {// 主币
                BigInteger amount = chainTransaction.getAmount().toBigInteger();
                if (coinBalance.compareTo(transferGas.add(amount)) >= 0) {
                    if (CollectionUtils.isEmpty(okList)) {
                        EthGetTransactionCount transactionCount = null;
                        try {
                            transactionCount = chainClient.getClient().ethGetTransactionCount(fromAddress, DefaultBlockParameterName.LATEST).sendAsync().get();
                        } catch (Exception e) {
                            markClientError(chainClient);
                            return;
                        }
                        BigInteger nonce = transactionCount.getTransactionCount();
                        chainTransaction.setNonce(nonce);
                    } else {
                        BigInteger nonce = okList.get(okList.size() - 1).getNonce().add(new BigInteger("1"));
                        chainTransaction.setNonce(nonce);
                    }
                    chainTransaction.setCoinBalance(coinBalance);
                    chainTransaction.setTransferGas(transferGas);
                    chainTransaction.setTransferPrice(transferPrice);
                    chainTransaction.setLimit(limit);
                    okList.add(chainTransaction);
                    coinBalance = coinBalance.subtract(transferGas.add(amount));
                } else {
                    balanceNotEnoughList.add(chainTransaction);
                }
            }
        }
        if (CollectionUtils.isNotEmpty(okList)) {
            for (ChainTransaction transaction : okList) {
                // 签名
                String hexValue;
                try {
                    hexValue = StringUtils.isBlank(transaction.getContract()) ? coinSignMessage(transaction) : tokenSignMessage(transaction);
                } catch (Exception e) {
                    log.error("end transfer : {}", transaction.getId(), e);
                    break;
                }
                if (StringUtils.isEmpty(hexValue)) {
                    log.error("end transfer : id={},\t 签名数据为空", transaction.getId());
                    break;
                }
                if (blockTransactionManager.prepareTransfer(transaction.getId(), blockHeight, chainClient.getUrl(), transaction.getNonce())) {
                    HashMap<String, Object> info = new HashMap<>();
                    info.put("limit", transaction.getLimit());
                    info.put("transferGas", transaction.getTransferGas());
                    info.put("transferPrice", transaction.getTransferPrice());
                    info.put("coinBalance", transaction.getCoinBalance());
                    info.put("tokenBalance", transaction.getTokenBalance());
                    info.put("nonce", transaction.getNonce());
                    info.put("hexValue", hexValue);
                    String chainInfo = JSON.toJSONString(info);

                    ChainTransaction updateChainTransaction = new ChainTransaction();
                    updateChainTransaction.setId(transaction.getId());
                    updateChainTransaction.setChainInfo(chainInfo);
                    chainTransactionService.updateById(updateChainTransaction);

                    log.info("start_transfer_1 : id={},\tBusinessId={},\tchainInfo={}", transaction.getId(), transaction.getBusinessId(), chainInfo);
                    //发送交易
                    EthSendTransaction ethSendTransaction = null;
                    try {
                        ethSendTransaction = chainClient.getClient().ethSendRawTransaction(hexValue).sendAsync().get();
                    } catch (Exception e) {
                        blockTransactionManager.releaseWaitingHash(transaction.getId());
                        log.error("1_ethSendRawTransaction : {},\t{},\t{}", transaction.getId(), transaction.getBusinessId(), chainClient.getUrl(), e);
                        markClientError(chainClient);
                        break;
                    }
                    if (ethSendTransaction == null || ethSendTransaction.hasError()) {
                        log.error("2_ethSendRawTransaction : {},\t{}", transaction.getId() + ",\t" + transaction.getBusinessId(), ethSendTransaction == null ? "" : ethSendTransaction.getError().getMessage());
                        blockTransactionManager.releaseWaitingHash(transaction.getId());
                        break;
                    }
                    String hash = ethSendTransaction.getTransactionHash();
                    log.info("end transfer : id={},\t{}\tBlockHeight={},\thash={}", transaction.getId(), transaction.getBusinessId(), chainScanConfig.getBlockHeight(), hash);
                    blockTransactionManager.updateHash(transaction.getId(), hash, chainInfo);
                }
            }
        } else {
            if (CollectionUtils.isEmpty(gasLowerList) && CollectionUtils.isNotEmpty(balanceNotEnoughList)) {
                ChainTransaction chainTransaction = balanceNotEnoughList.get(0);
                blockTransactionManager.updateTxStatus(chainTransaction.getId(), ChainTransaction.TX_STATUS.FAIL.name(), ChainTransaction.FAIL_CODE.BALANCE_NOT_ENOUGH.name(), null, blockHeight, true);
            } else if (CollectionUtils.isNotEmpty(gasLowerList)) {
                ChainTransaction chainTransaction = gasLowerList.get(0);
                blockTransactionManager.updateTxStatus(chainTransaction.getId(), ChainTransaction.TX_STATUS.FAIL.name(), ChainTransaction.FAIL_CODE.GAS_NOT_ENOUGH.name(), null, blockHeight, true);
            }
        }
    }

    /**
     * 主币签名
     *
     * @return 交易签名
     */
    private String coinSignMessage(ChainTransaction transaction) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("from", transaction.getFromAddress());
        jsonObject.put("to", transaction.getToAddress());
        jsonObject.put("amount", transaction.getAmount().toBigInteger());
        jsonObject.put("nonce", transaction.getNonce());
        jsonObject.put("gasLimit", transaction.getLimit());
        jsonObject.put("gasPrice", transaction.getTransferPrice());
        jsonObject.put("contract", "");
        jsonObject.put("chainID", id);
        return sign(jsonObject);
    }

    /**
     * 代币签名
     *
     * @return
     */
    private String tokenSignMessage(ChainTransaction transaction) {
        BigInteger amount = transaction.getAmount().toBigInteger();

        List<Type> inputParameters = new ArrayList<>();
        List<TypeReference<?>> outputParameters = new ArrayList<>();
        inputParameters.add(new Address(transaction.getToAddress()));
        inputParameters.add(new Uint256(amount));

        org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function("transfer", inputParameters, outputParameters);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("from", transaction.getFromAddress());
        jsonObject.put("to", transaction.getContract());
        jsonObject.put("amount", BigInteger.ZERO);
        jsonObject.put("nonce", transaction.getNonce());
        jsonObject.put("gasLimit", transaction.getLimit());
        jsonObject.put("gasPrice", transaction.getTransferPrice());
        jsonObject.put("chainID", id);
        jsonObject.put("data", FunctionEncoder.encode(function));
        return sign(jsonObject);
    }

    @Override
    public BigDecimal gas(ChainScanConfig chainScanConfig, SymbolConfig coinConfig) {
        ChainClient chainClient = getChainClient(null);
        BigInteger gasPrice;
        JSONObject jsonObject = JSON.parseObject(coinConfig.getConfigJson());
        if (jsonObject.containsKey("gas") && jsonObject.getBigDecimal("gas").compareTo(BigDecimal.ZERO) > 0) {
            return jsonObject.getBigDecimal("gas");
        }
        if (jsonObject.containsKey("gasPrice") && jsonObject.getBigInteger("gasPrice").compareTo(new BigInteger("0")) > 0) {
            gasPrice = jsonObject.getBigInteger("gasPrice");
        } else {
            try {
                gasPrice = chainClient.getClient().ethGasPrice().send().getGasPrice();
            } catch (Exception e) {
                markClientError(chainClient);
                return BigDecimal.ZERO;
            }
        }
        return new BigDecimal(gasPrice.multiply(jsonObject.getBigInteger("limit"))).divide(mainCoinConfig.precision(), 16, RoundingMode.DOWN);
    }

    @Override
    public boolean isValidTronAddress(String address) {
        if (StringUtils.isBlank(address)) {
            return false;
        }
        if (!StringUtils.startsWith(address, "0x")) {
            return false;
        }
        String cleanInput = Numeric.cleanHexPrefix(address);
        try {
            // 16 进制数
            Numeric.toBigIntNoPrefix(cleanInput);
        } catch (Exception e) {
            return false;
        }
        return true;
    }


    @Override
    public void freshChainScanConfig(ChainScanConfig chainScanConfig) {
        super.freshChainScanConfig(chainScanConfig);
        HashMap<String, SymbolConfig> configHashMap = new HashMap<>();
        List<SymbolConfig> coinConfigs = chainScanConfig.getCoinConfigs();
        if (CollectionUtils.isNotEmpty(coinConfigs)) {
            for (SymbolConfig coinConfig : coinConfigs) {
                if (StringUtils.isNotBlank(coinConfig.getContractAddress())) {
                    configHashMap.put(this.formatAddress(coinConfig.getContractAddress()), coinConfig);
                }
            }
        }
        this.configHashMap = configHashMap;
    }

    @Override
    protected ChainClient create(JSONObject item) {
        String url = item.getString("url");
        Web3j web3j = Web3j.build(new HttpService(url));
        if (this.id == null && item.containsKey("id")) {
            this.id = item.getInteger("id");
            log.info("0_create_like_eth : chain = {},\t id = {}", this.getChainId(), id);
        }
        if (this.id == null) {
            try {
                this.id = web3j.ethChainId().send().getChainId().intValue();
            } catch (Exception e) {
                log.warn("get_chainId_error_{}", this.getChainId(), e);
            }
            log.info("1_create_like_eth : chain = {},\t id = {}", this.getChainId(), id);
        }
        return new ChainClient(url, web3j) {
            @Override
            public void close() {
                web3j.shutdown();
            }
        };
    }

    @Override
    public LastBlockInfo getLastBlockInfo() {
        BlockChain<Web3j>.ChainClient chainClient = getChainClient(null);
        try {
            EthBlock.Block block = chainClient.getClient()//
                    .ethGetBlockByNumber(DefaultBlockParameterName.LATEST, false)//
                    .send().getResult();
            LastBlockInfo lastBlockInfo = new LastBlockInfo();
            lastBlockInfo.setBlockNumber(block.getNumber());
            lastBlockInfo.setBlockTime(new Date(block.getTimestamp().longValue() * 1000));
            return lastBlockInfo;
        } catch (Exception e) {
            markClientError(chainClient);
            throw new RuntimeException(e);
        }
    }

}
