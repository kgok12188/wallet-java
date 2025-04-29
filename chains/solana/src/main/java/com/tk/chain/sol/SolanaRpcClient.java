package com.tk.chain.sol;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tk.chain.sol.model.*;
import com.tk.chains.BlockChain;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


@Slf4j
@Data
public class SolanaRpcClient {

    private static final Logger logger = LoggerFactory.getLogger(SolanaRpcClient.class);
    protected final static ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();
    private String url;

    private HttpHeaders httpHeaders;

    public SolanaRpcClient(String url, String... headers) {
        httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.add("User-Agent", "Java/11");
        if (headers != null && headers.length > 0 && headers.length % 2 == 0) {
            for (int i = 0; i < headers.length; i = +2) {
                httpHeaders.add(headers[i], headers[i + 1]);
            }
        }
        this.url = url;
    }

    private String getMainCoinName() {
        return "SOL";
    }

    @SneakyThrows
    public <T> ResponseEntity<T> exchange(JsonRpcRequest request, ParameterizedTypeReference<T> responseType) {
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//        headers.add("User-Agent", "Java/11");
//        headers.add("x-api-key", "t-680f9e2a72fc4543b7e39cb8-df8a6c02a9424cbea793ffef");
        HttpEntity<String> requestEntity = new HttpEntity<>(objectMapper.writeValueAsString(request), httpHeaders);
        return restTemplate.exchange(url, HttpMethod.POST, requestEntity, responseType);
    }

    @SneakyThrows
    public BlockHeight getHeight() {
        ResponseEntity<JsonRpcResponseGenerics<SolEpochInfo>> response = exchange(new JsonRpcRequest<>(SolConstant.getEpochInfo, new ArrayList<>()), new ParameterizedTypeReference<JsonRpcResponseGenerics<SolEpochInfo>>() {
        });
        return BlockHeight.builder().blockHeight(Objects.requireNonNull(response.getBody()).getResult().getAbsoluteSlot().longValue() - 120).build();
    }

    @SneakyThrows
    public BlockChain.LastBlockInfo getLastBlockInfo() {
        ResponseEntity<JsonRpcResponseGenerics<SolEpochInfo>> response = exchange(new JsonRpcRequest<>(SolConstant.getEpochInfo, new ArrayList<>()), new ParameterizedTypeReference<JsonRpcResponseGenerics<SolEpochInfo>>() {
        });
        BlockChain.LastBlockInfo lastBlockInfo = new BlockChain.LastBlockInfo();
        if (response.getBody() == null) {
            return null;
        }
        lastBlockInfo.setBlockNumber(response.getBody().getResult().getAbsoluteSlot());
        lastBlockInfo.setBlockTime(new Date(getBlock(Objects.requireNonNull(response.getBody().getResult()).getAbsoluteSlot().longValue()).getBlockTime()));
        return lastBlockInfo;
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
        return Objects.requireNonNull(response.getBody()).getResult();
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


        if (Objects.nonNull(Objects.requireNonNull(jsonRpcResponse.getBody()).getError())) {
            String errMsg = jsonRpcResponse.getBody().getError().toString();
            throw new RuntimeException(errMsg);
        }
        SolanaConfirmedBlock result = jsonRpcResponse.getBody().getResult();
        return Block.builder().blockHash(result.getBlockhash()).blockHeight(height).transactionIds(result.getSignatures()).blockTime(result.getBlockTime().longValue() * 1000).parentHash(result.getPreviousBlockhash()).build();
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

        if (Objects.nonNull(Objects.requireNonNull(jsonRpcResponse.getBody()).getError())) {
            String errMsg = jsonRpcResponse.getBody().getError().toString();
            if (errMsg.contains("skip") || errMsg.contains("miss")) {
                return BlockTx.builder().blockHash(height.toString()).blockHeight(height).txs(Lists.newArrayList()).build();
            }
            throw new RuntimeException(errMsg);
        }
        SolanaConfirmedBlock result = jsonRpcResponse.getBody().getResult();
        return BlockTx.builder().blockHash(result.getBlockhash()).blockHeight(height).blockTime(result.getBlockTime().longValue() * 1000).parentHash(result.getPreviousBlockhash()).txs(result.getTransactions().stream().map(tx -> buildSolTx(tx.getMeta(), tx.getTransaction(), height)).filter(tx -> !CollectionUtils.isEmpty(tx.getActions())).collect(Collectors.toList())).build();
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
        if (Objects.nonNull(Objects.requireNonNull(jsonRpcResponse.getBody()).getError())) {
            throw new RuntimeException(jsonRpcResponse.getBody().getError().toString());
        }
        return buildSolTx(jsonRpcResponse.getBody().getResult().getMeta(), jsonRpcResponse.getBody().getResult().getTransaction(), jsonRpcResponse.getBody().getResult().getSlot().longValue());
    }

    @SneakyThrows
    public AddressBalance getBalance(String address, String contractAddress) {
        if (StringUtils.isEmpty(contractAddress)) {
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
            if (CollectionUtils.isEmpty(Objects.requireNonNull(jsonRpcResponse.getBody()).getResult().getValue())) {
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
            return Transaction.builder().fee(meta.getFee()).blockHeight(blockHeight).status(paymentStatus.getCode()).txHash(hash).actions(Lists.newArrayList()).build();
        }

        ArrayList<Transaction.Action> actions = Lists.newArrayList();

        for (int i = 0; i < meta.getPostBalances().size(); i++) {
            if (meta.getPostBalances().get(i).compareTo(meta.getPreBalances().get(i)) > 0) {
                BigDecimal postBalance = meta.getPostBalances().get(i);
                actions.add(Transaction.Action.builder().fromAddress(from)
                        .postBalance(postBalance)
                        .toAddress(transaction.getMessage().getAccountKeys().get(i))
                        .symbol(getMainCoinName()).index(index.getAndIncrement())
                        .fromAmount(meta.getPostBalances().get(i).subtract(meta.getPreBalances().get(i)))
                        .toAmount(meta.getPostBalances().get(i).subtract(meta.getPreBalances().get(i))).build());
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
            actions.add(Transaction.Action.builder()
                    .fromAddress(from)
                    .toAddress(post.getOwner())
                    .index(index.getAndIncrement()).fromAmount(amount)
                    .toAmount(amount).contractAddress(post.getMint())
                    .postBalance(post.getUiTokenAmount().getUiAmount())
                    .symbol(post.getMint()).build());
        });
        return Transaction.builder().fee(meta.getFee()).blockHeight(blockHeight).status(paymentStatus.getCode()).txHash(hash).actions(actions).build();
    }

//    public static void main(String[] args) {
//        SolanaRpcClient solanaRpcClient = new SolanaRpcClient(
//                // "https://api.mainnet-beta.solana.com"
//                // "https://sol.nownodes.io/4e2966c3-bc74-40a8-883e-2edb4550c557"
//                "https://solana-mainnet.gateway.tatum.io",
//                "x-api-key", "t-680f9e2a72fc4543b7e39cb8-df8a6c02a9424cbea793ffef"
//        );
//        Long blockHeight = solanaRpcClient.getHeight().getBlockHeight();
//        BlockTx blockTx = solanaRpcClient.getBlockTx(blockHeight);
//        System.out.println(JSON.toJSONString(blockTx.getTxs(), true));
//    }

}
