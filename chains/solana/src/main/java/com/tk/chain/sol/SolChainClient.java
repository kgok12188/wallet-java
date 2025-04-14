package com.tk.chain.sol;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tk.chain.sol.core.Account;
import com.tk.chain.sol.core.PublicKey;
import com.tk.chain.sol.core.RpcSendTransactionConfig;
import com.tk.chain.sol.model.*;
import com.tk.chain.sol.programs.SystemProgram;
import com.tk.chain.sol.programs.TokenProgram;
import com.tk.chain.sol.utils.SolConstant;
import com.tk.chain.thirdPart.*;
import com.tk.chains.BlockChain;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


@Slf4j
@Data
public class SolChainClient {

    private static final Logger logger = LoggerFactory.getLogger(SolChainClient.class);
    protected final static ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();
    private String url;

    public SolChainClient(String url) {
        this.client = buildWebClient("TonNodeJsConnectionPool", url, 50);
        this.url = url;
    }

    private WebClient client;


    private String getMainCoinName() {
        return "SOL";
    }

    public WebClient buildWebClient(String poolName, String baseUrl, int maxConnections) {
        HttpClient httpClient = buildHttpClient(poolName, maxConnections);

        return org.springframework.web.reactive.function.client.WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(ClientCodecConfigurer::defaultCodecs)
                        .build()).build();
    }

    public HttpClient buildHttpClient(String poolName, int maxConnections) {
        if (maxConnections < 100)
            maxConnections = 100;

        ConnectionProvider provider = ConnectionProvider.fixed(poolName, maxConnections);


        return HttpClient.create(provider);
    }


    @SneakyThrows
    public <T> ResponseEntity<T> exchange(JsonRpcRequest request, ParameterizedTypeReference<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("User-Agent", "Java/11");
        HttpEntity<String> requestEntity = new HttpEntity<>(objectMapper.writeValueAsString(request), headers);
        return restTemplate.exchange(url, HttpMethod.POST, requestEntity, responseType);
    }

    @SneakyThrows
    public BlockHeight getHeight() {
        ResponseEntity<JsonRpcResponseGenerics<SolEpochInfo>> response = exchange(new JsonRpcRequest<>(SolConstant.getEpochInfo, new ArrayList<>()), new ParameterizedTypeReference<JsonRpcResponseGenerics<SolEpochInfo>>() {
        });
        return BlockHeight.builder().blockHeight(response.getBody().getResult().getAbsoluteSlot().longValue() - 120).build();
    }

    @SneakyThrows
    public BlockChain.LastBlockInfo getLastBlockInfo() {
        ResponseEntity<JsonRpcResponseGenerics<SolEpochInfo>> response = exchange(new JsonRpcRequest<>(SolConstant.getEpochInfo, new ArrayList<>()), new ParameterizedTypeReference<JsonRpcResponseGenerics<SolEpochInfo>>() {
        });
        BlockChain.LastBlockInfo lastBlockInfo = new BlockChain.LastBlockInfo();
        lastBlockInfo.setBlockNumber(Objects.requireNonNull(response.getBody().getResult()).getAbsoluteSlot());
        lastBlockInfo.setBlockTime(new Date(getBlock(Objects.requireNonNull(response.getBody().getResult()).getAbsoluteSlot().longValue()).getBlockTime()));
        return lastBlockInfo;
    }

    public String submitTransaction(SubmitTransaction transaction, String signature) {
        return sendTransaction(transaction.getContractAddress(), transaction.getFromAddress(), transaction.getToAddress(), new BigDecimal(transaction.getAmount()), transaction.getPrecision(), transaction.getRecentBlockHash(), signature);
    }

    public String getRawMessage(SubmitTransaction transaction) {
        return getRawMessage(transaction.getContractAddress(), transaction.getFromAddress(), transaction.getToAddress(), new BigDecimal(transaction.getAmount()), transaction.getPrecision(), transaction.getRecentBlockHash());
    }

    @SneakyThrows
    public String sendTransaction(String contractAddress, String fromAddress, String toAddress, BigDecimal amount, Integer precision, String recentBlockHash, String signature) {
        long toAmount = amount.longValue();
        PublicKey toAddressPublicKey = new PublicKey(toAddress);
        PublicKey fromAddressPublicKey = new PublicKey(fromAddress);
        com.tk.chain.sol.core.Transaction transaction = new com.tk.chain.sol.core.Transaction();
        if (StringUtils.isEmpty(contractAddress) || getMainCoinName().equalsIgnoreCase(contractAddress)) {
            transaction.addInstruction(SystemProgram.transfer(fromAddressPublicKey, toAddressPublicKey, toAmount));
        } else {
            PublicKey mintAddress = new PublicKey(contractAddress);
            SolTokenAccounts fromTokenAddressAccounts = getTokenAccountsByOwner(fromAddress, contractAddress);
            if (Objects.isNull(fromTokenAddressAccounts) || CollectionUtils.isEmpty(fromTokenAddressAccounts.getValue())) {
                throw new RuntimeException("from token address is null!");
            }
            PublicKey fromTokenAddress = new PublicKey(fromTokenAddressAccounts.getValue().get(0).getPubkey());

            SolTokenAccounts tokenAccountsByOwner = getTokenAccountsByOwner(toAddress, contractAddress);
            PublicKey toTokenAddress;
            if (Objects.isNull(tokenAccountsByOwner) || CollectionUtils.isEmpty(tokenAccountsByOwner.getValue())) {
                toTokenAddress = findProgramAddress(toAddress, contractAddress);
                transaction.addInstruction(SystemProgram.createAccountForkJsSdk(fromAddressPublicKey, toTokenAddress, toAddressPublicKey, mintAddress));
            } else {
                toTokenAddress = new PublicKey(tokenAccountsByOwner.getValue().get(0).getPubkey());
            }

            transaction.addInstruction(TokenProgram.transfer(fromTokenAddress, // from token address
                    toTokenAddress, // to token address
                    toAmount, // amount
                    fromAddressPublicKey, // from address
                    mintAddress));
        }

        transaction.setRecentBlockHash(recentBlockHash);
        transaction.serializeOnly(Collections.singletonList(new Account(fromAddress)));
        transaction.setSignature(signature);
        byte[] serializedTransaction = transaction.serialize();
        String base64Trx = Base64.getEncoder().encodeToString(serializedTransaction);

        List<Object> list = new ArrayList<>();
        list.add(base64Trx);
        list.add(new RpcSendTransactionConfig());
        log.info("send sol transaction ,base64Trx:{}", base64Trx);
        ResponseEntity<JsonRpcResponseGenerics<String>> response = exchange(new JsonRpcRequest<>(SolConstant.sendTransaction, list), new ParameterizedTypeReference<JsonRpcResponseGenerics<String>>() {
        });
        if (Objects.nonNull(response.getBody().getError())) {
            log.error("send sol transaction error: {}", response.getBody().getError());
            throw new RuntimeException(response.getBody().getError().toString());
        }
        return response.getBody().getResult();
    }


    public String getRawMessage(String contractAddress, String fromAddress, String toAddress, BigDecimal amount, Integer precision, String recentBlockHash) {
        long toAmount = amount.longValue();
        PublicKey toAddressPublicKey = new PublicKey(toAddress);
        PublicKey fromAddressPublicKey = new PublicKey(fromAddress);
        com.tk.chain.sol.core.Transaction transaction = new com.tk.chain.sol.core.Transaction();
        if (StringUtils.isEmpty(contractAddress) || getMainCoinName().equalsIgnoreCase(contractAddress)) {
            transaction.addInstruction(SystemProgram.transfer(fromAddressPublicKey, toAddressPublicKey, toAmount));
        } else {
            PublicKey mintAddress = new PublicKey(contractAddress);
            SolTokenAccounts fromTokenAddressAccounts = getTokenAccountsByOwner(fromAddress, contractAddress);
            if (Objects.isNull(fromTokenAddressAccounts) || CollectionUtils.isEmpty(fromTokenAddressAccounts.getValue())) {
                throw new RuntimeException("from token address is null!");
            }
            PublicKey fromTokenAddress = new PublicKey(fromTokenAddressAccounts.getValue().get(0).getPubkey());

            SolTokenAccounts tokenAccountsByOwner = getTokenAccountsByOwner(toAddress, contractAddress);
            PublicKey toTokenAddress;
            if (Objects.isNull(tokenAccountsByOwner) || CollectionUtils.isEmpty(tokenAccountsByOwner.getValue())) {
                toTokenAddress = findProgramAddress(toAddress, contractAddress);
                transaction.addInstruction(SystemProgram.createAccountForkJsSdk(fromAddressPublicKey, toTokenAddress, toAddressPublicKey, mintAddress));
            } else {
                toTokenAddress = new PublicKey(tokenAccountsByOwner.getValue().get(0).getPubkey());
            }

            transaction.addInstruction(TokenProgram.transfer(fromTokenAddress, // from token address
                    toTokenAddress, // to token address
                    toAmount, // amount
                    fromAddressPublicKey, // from address
                    mintAddress));
        }

        transaction.setRecentBlockHash(recentBlockHash);

        return transaction.getRawMessage(Collections.singletonList(new Account(fromAddress)));
    }


    @SneakyThrows
    public PublicKey findProgramAddress(String mainAddress, String mintAddress) {
        PublicKey programId = new PublicKey("TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA");
        List<byte[]> seeds = new ArrayList<>();
        seeds.add(new PublicKey(mainAddress).toByteArray());
        seeds.add(programId.toByteArray());
        seeds.add(new PublicKey(mintAddress).toByteArray());
        PublicKey.ProgramDerivedAddress newAccount = PublicKey.findProgramAddress(seeds, new PublicKey("ATokenGPvbdGVxr1b2hvZbsiqW5xWH25efTNsLJA8knL"));
        return newAccount.getAddress();
    }


    @SneakyThrows
    public SolTokenAccounts getTokenAccountsByOwner(String ownerAddress, String contractAddress) {
        Map<String, String> encoding = Maps.newHashMap();
        encoding.put(SolConstant.encoding, SolConstant.jsonParsed);
        Map<String, String> mint = Maps.newHashMap();
        mint.put(SolConstant.mint, contractAddress);
        List<Object> list = new ArrayList<>();
        list.add(ownerAddress);
        list.add(mint);
        list.add(encoding);
        ResponseEntity<JsonRpcResponseGenerics<SolTokenAccounts>> response = exchange(new JsonRpcRequest<>(SolConstant.getTokenAccountsByOwner, list), new ParameterizedTypeReference<JsonRpcResponseGenerics<SolTokenAccounts>>() {
        });
        return response.getBody().getResult();
    }

    @SneakyThrows
    public Block getBlock(Long height) {
        List<Object> list = new ArrayList<>();
        list.add(height);
        Map<String, String> map = Maps.newHashMap();
        map.put(SolConstant.transactionDetails, SolConstant.signatures);
        list.add(map);

        ResponseEntity<JsonRpcResponseGenerics<SolanaConfirmedBlock>> jsonRpcResponse = exchange(new JsonRpcRequest<>(SolConstant.getBlock, list), new ParameterizedTypeReference<JsonRpcResponseGenerics<SolanaConfirmedBlock>>() {
        });


        if (Objects.nonNull(jsonRpcResponse.getBody().getError())) {
            String errMsg = jsonRpcResponse.getBody().getError().toString();
            throw new RuntimeException(errMsg);
        }
        SolanaConfirmedBlock result = jsonRpcResponse.getBody().getResult();
        return Block.builder().blockHash(result.getBlockhash()).blockHeight(height).transactionIds(result.getSignatures()).blockTime(result.getBlockTime().longValue() * 1000).parentHash(result.getPreviousBlockhash()).build();
    }

    public static final String USER_AGENT = "User-Agent";

    public Mono<BlockTx> getBlockTx(Long height, String userAgent) {
        List<Object> list = new ArrayList<>();
        list.add(height);
        Map<String, Object> map = Maps.newHashMap();
        map.put(SolConstant.maxSupportedTransactionVersion, 0);
        list.add(map);
        return client
                .post()
                .header(USER_AGENT, userAgent)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(new JsonRpcRequest<>(SolConstant.getBlock, list)), JsonRpcRequest.class)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<JsonRpcResponseGenerics<SolanaConfirmedBlock>>() {
                })
                .map(jsonRpcResponse -> {
                            if (Objects.nonNull(jsonRpcResponse.getError())) {
                                String errMsg = jsonRpcResponse.getError().toString();
                                throw new RuntimeException(errMsg);
                            }
                            SolanaConfirmedBlock result = jsonRpcResponse.getResult();
                            return BlockTx.builder()
                                    .blockHash(result.getBlockhash())
                                    .blockHeight(height)
                                    .blockTime(result.getBlockTime().longValue() * 1000)
                                    .parentHash(result.getPreviousBlockhash())
                                    .txs(result.getTransactions()
                                            .parallelStream()
//                                            .filter(tx -> tx.getVersion() instanceof String && tx.getVersion().toString().equals("legacy"))
                                            .map(tx -> buildSolTx(tx.getMeta(), tx.getTransaction(), height))
                                            .collect(Collectors.toList()))
                                    .build();
                        }
                );
    }

    @SneakyThrows
    public BlockTx getBlockTx(Long height) {

        if (logger.isDebugEnabled()) {
            logger.info("getBlockTx: {}", height);
        }
        List<Object> list = new ArrayList<>();
        list.add(height);
        Map<String, Object> map = Maps.newHashMap();
        map.put(SolConstant.maxSupportedTransactionVersion, 0);
        list.add(map);

        ResponseEntity<JsonRpcResponseGenerics<SolanaConfirmedBlock>> jsonRpcResponse = exchange(new JsonRpcRequest<>(SolConstant.getBlock, list), new ParameterizedTypeReference<JsonRpcResponseGenerics<SolanaConfirmedBlock>>() {
        });

        if (Objects.nonNull(jsonRpcResponse.getBody().getError())) {
            String errMsg = jsonRpcResponse.getBody().getError().toString();
            if (errMsg.contains("skip") || errMsg.contains("miss")) {
                return BlockTx.builder().blockHash(height.toString()).blockHeight(height).txs(Lists.newArrayList()).build();
            }
            throw new RuntimeException(errMsg);
        }
        SolanaConfirmedBlock result = jsonRpcResponse.getBody().getResult();
        return BlockTx.builder().blockHash(result.getBlockhash()).blockHeight(height).blockTime(result.getBlockTime().longValue() * 1000).parentHash(result.getPreviousBlockhash()).txs(result.getTransactions().parallelStream().map(tx -> buildSolTx(tx.getMeta(), tx.getTransaction(), height)).collect(Collectors.toList())).build();
    }


    @SneakyThrows
    public Transaction getTx(String hash) {
        List<Object> list = new ArrayList<>();
        list.add(hash);
        Map<String, Object> map = Maps.newHashMap();
        map.put(SolConstant.maxSupportedTransactionVersion, 0);
        list.add(map);

        ResponseEntity<JsonRpcResponseGenerics<SolConfirmedTransaction>> jsonRpcResponse = exchange(new JsonRpcRequest<>(SolConstant.getTransaction, list), new ParameterizedTypeReference<JsonRpcResponseGenerics<SolConfirmedTransaction>>() {
        });
        if (Objects.nonNull(jsonRpcResponse.getBody().getError())) {
            throw new RuntimeException(jsonRpcResponse.getBody().getError().toString());
        }
        return buildSolTx(jsonRpcResponse.getBody().getResult().getMeta(), jsonRpcResponse.getBody().getResult().getTransaction(), jsonRpcResponse.getBody().getResult().getSlot().longValue());
    }

    @SneakyThrows
    public AddressBalance getBalance(String address, String contractAddress) {
        if (!StringUtils.hasLength(contractAddress)) {
            List<Object> list = new ArrayList<>();
            list.add(address);
            ResponseEntity<JsonRpcResponseGenerics<SolBalance>> jsonRpcResponse = exchange(new JsonRpcRequest<>(SolConstant.getBalance, list), new ParameterizedTypeReference<JsonRpcResponseGenerics<SolBalance>>() {
            });
            return AddressBalance.builder().address(address).balance(Objects.requireNonNull(jsonRpcResponse.getBody()).getResult().getValue()).symbol(getMainCoinName()).build();
        } else {
            List<Object> list = new ArrayList<>();
            list.add(address);
            Map<String, String> encoding = Maps.newHashMap();
            encoding.put(SolConstant.encoding, SolConstant.jsonParsed);
            Map<String, String> mint = Maps.newHashMap();
            mint.put(SolConstant.mint, contractAddress);
            list.add(mint);
            list.add(encoding);
            ResponseEntity<JsonRpcResponseGenerics<SolTokenAccounts>> jsonRpcResponse = exchange(new JsonRpcRequest<>(SolConstant.getTokenAccountsByOwner, list), new ParameterizedTypeReference<JsonRpcResponseGenerics<SolTokenAccounts>>() {
            });
            if (CollectionUtils.isEmpty(jsonRpcResponse.getBody().getResult().getValue())) {
                return AddressBalance.builder().address(address).balance(BigDecimal.ZERO).symbol(getMainCoinName()).build();
            }
            return AddressBalance.builder().address(address).balance(new BigDecimal(jsonRpcResponse.getBody().getResult().getValue().get(0).getAccount().getData().getParsed().getInfo().getTokenAmount().getAmount())).symbol(getMainCoinName()).build();
        }
    }


    protected Transaction buildSolTx(SolConfirmedTransaction.MetaModel meta, SolConfirmedTransaction.TransactionModel transaction, Long blockHeight) {
        SolConfirmedTransaction.TransactionModel.MessageModel message = transaction.getMessage();
        PaymentStatusEnum paymentStatus = Objects.nonNull(meta.getErr()) || Objects.nonNull(meta.getStatus().getErr()) ? PaymentStatusEnum.FAIL : PaymentStatusEnum.CONFIRMED;
        String hash = transaction.getSignatures().get(0);
        String from = message.getAccountKeys().get(message.getHeader().getNumReadonlySignedAccounts());

        AtomicInteger index = new AtomicInteger(0);

        if (meta.getPostBalances().size() != transaction.getMessage().getAccountKeys().size()) {
            return Transaction.builder().blockHeight(blockHeight).status(paymentStatus.getCode()).txHash(hash).actions(Lists.newArrayList()).build();
        }

        ArrayList<Transaction.Action> actions = Lists.newArrayList();

        for (int i = 0; i < meta.getPostBalances().size(); i++) {
            if (meta.getPostBalances().get(i).compareTo(meta.getPreBalances().get(i)) > 0) {
                actions.add(Transaction.Action.builder().fromAddress(from).toAddress(transaction.getMessage().getAccountKeys().get(i)).fee(meta.getFee()).symbol(getMainCoinName()).index(index.getAndIncrement()).fromAmount(meta.getPostBalances().get(i).subtract(meta.getPreBalances().get(i))).toAmount(meta.getPostBalances().get(i).subtract(meta.getPreBalances().get(i))).fee(meta.getFee()).build());
            }
        }


        meta.getPostTokenBalances().stream().filter(i -> (Objects.nonNull(i.getUiTokenAmount().getUiAmount()))).forEach(post -> {
            List<SolConfirmedTransaction.MetaModel.PreTokenBalancesModel> preTokenBalances = meta.getPreTokenBalances().stream().filter(pre -> Objects.equals(pre.getAccountIndex(), post.getAccountIndex()) && !Objects.isNull(pre.getUiTokenAmount().getUiAmount())).collect(Collectors.toList());

            BigDecimal amount;
            if (CollectionUtils.isEmpty(preTokenBalances)) {
                amount = new BigDecimal(post.getUiTokenAmount().getAmount());
            } else if (post.getUiTokenAmount().getUiAmount().compareTo(preTokenBalances.get(0).getUiTokenAmount().getUiAmount()) > 0) {
                amount = new BigDecimal(post.getUiTokenAmount().getAmount()).subtract(new BigDecimal(preTokenBalances.get(0).getUiTokenAmount().getAmount()));
            } else {
                return;
            }
            actions.add(Transaction.Action.builder().fromAddress(from).toAddress(post.getOwner()).fee(meta.getFee()).index(index.getAndIncrement()).fromAmount(amount).toAmount(amount).contractAddress(post.getMint()).symbol(post.getMint()).build());
        });
        return Transaction.builder().blockHeight(blockHeight).status(paymentStatus.getCode()).txHash(hash).actions(actions).build();
    }

}
