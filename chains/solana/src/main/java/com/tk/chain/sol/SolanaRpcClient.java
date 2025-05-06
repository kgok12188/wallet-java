package com.tk.chain.sol;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tk.chain.sol.model.*;
import com.tk.chains.BlockChain;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.bitcoinj.core.Base58;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Data
public class SolanaRpcClient {

    public static final String SPLAssociatedTokenAccountProgramID = "ATokenGPvbdGVxr1b2hvZbsiqW5xWH25efTNsLJA8knL";
    public static final String ComputeBudgetProgramID = "ComputeBudget111111111111111111111111111111";
    public static final String TokenProgramID = "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA";
    public static final String SystemProgramID = "11111111111111111111111111111111";


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
        HttpEntity<String> requestEntity = new HttpEntity<>(objectMapper.writeValueAsString(request), httpHeaders);
        return restTemplate.exchange(url, HttpMethod.POST, requestEntity, responseType);
    }

    @SneakyThrows
    public BlockHeight getHeight() {
        ResponseEntity<JsonRpcResponseGenerics<SolEpochInfo>> response = exchange(new JsonRpcRequest<>(SolConstant.getEpochInfo, new ArrayList<>()), new ParameterizedTypeReference<JsonRpcResponseGenerics<SolEpochInfo>>() {
        });
        return BlockHeight.builder().blockHeight(Objects.requireNonNull(response.getBody()).getResult().getAbsoluteSlot().longValue() - 120).build();
    }


    public String getLatestBlocHash() {
        ResponseEntity<JsonRpcResponseGenerics<RecentBlockHash>> response = exchange(new JsonRpcRequest<>(SolConstant.getLatestBlockhash, new ArrayList<>()), new ParameterizedTypeReference<JsonRpcResponseGenerics<RecentBlockHash>>() {
        });
        if (response.getBody() == null || response.getBody().getResult() == null) {
            return "";
        }
        return response.getBody().getResult().getValue().getBlockhash();
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
        return BlockTx.builder().blockHash(result.getBlockhash()).blockHeight(height).blockTime(result.getBlockTime().longValue()).parentHash(result.getPreviousBlockhash()).txs(result.getTransactions().stream().map(tx ->
                buildSolTx(tx.getMeta(), tx.getTransaction(), height, result.getBlockTime())).filter(tx -> !CollectionUtils.isEmpty(tx.getActions())).collect(Collectors.toList())).build();
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
        if (jsonRpcResponse.getBody() == null || jsonRpcResponse.getBody().getResult() == null) {
            return null;
        }
        if (Objects.nonNull(Objects.requireNonNull(jsonRpcResponse.getBody()).getError())) {
            throw new RuntimeException(jsonRpcResponse.getBody().getError().toString());
        }
        return buildSolTx(jsonRpcResponse.getBody().getResult().getMeta(), jsonRpcResponse.getBody().getResult().getTransaction(),
                jsonRpcResponse.getBody().getResult().getSlot().longValue(), jsonRpcResponse.getBody().getResult().getBlockTime());
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


    protected Transaction buildSolTx(SolConfirmedTransaction.MetaModel meta, SolConfirmedTransaction.TransactionModel transaction, Long blockHeight, BigInteger blockTime) {
        PaymentStatusEnum paymentStatus = Objects.nonNull(meta.getErr()) || Objects.nonNull(meta.getStatus().getErr()) ? PaymentStatusEnum.FAIL : PaymentStatusEnum.CONFIRMED;
        String hash = transaction.getSignatures().get(0);
        if (meta.getPostBalances().size() != transaction.getMessage().getAccountKeys().size()) {
            return Transaction.builder().fee(meta.getFee()).blockHeight(blockHeight).status(paymentStatus.getCode()).txHash(hash).actions(Lists.newArrayList()).build();
        }

        List<SolConfirmedTransaction.TransactionModel.MessageModel.InstructionsModel> instructions = new LinkedList<>(transaction.getMessage().getInstructions());
        if (!CollectionUtils.isEmpty(meta.getInnerInstructions())) {
            for (JSONObject innerInstruction : meta.getInnerInstructions()) {
                List<SolConfirmedTransaction.TransactionModel.MessageModel.InstructionsModel> instructionList = innerInstruction.getJSONArray("instructions").toJavaList(SolConfirmedTransaction.TransactionModel.MessageModel.InstructionsModel.class);
                if (!CollectionUtils.isEmpty(instructionList)) {
                    instructions.addAll(instructionList);
                }
            }
        }

        List<String> accountKeys = transaction.getMessage().getAccountKeys();
        ArrayList<Transaction.Action> actions = Lists.newArrayList();
        for (SolConfirmedTransaction.TransactionModel.MessageModel.InstructionsModel instruction : instructions) {
            String programId = instruction.getProgramId(accountKeys);
            List<Integer> accounts = instruction.getAccounts();
            ArrayList<String> insAccountKeys = new ArrayList<>();
            for (Integer account : accounts) {
                insAccountKeys.add(accountKeys.get(account));
            }
            if (org.apache.commons.lang3.StringUtils.equals(SystemProgramID, programId)) {
                SystemTransferData systemTransferData = new SystemTransferData();
                systemTransferData.parse(instruction.getData(), hash);
                String fromAddress;
                String toAddress;
                if (systemTransferData.getInstructionType() == SystemInstructionType.InstructionTransfer.ordinal()) { // InstructionTransfer
                    fromAddress = insAccountKeys.get(0);
                    toAddress = insAccountKeys.get(1);
                } else if (systemTransferData.getInstructionType() == SystemInstructionType.InstructionTransferWithSeed.ordinal()) { // InstructionTransferWithSeed
                    fromAddress = insAccountKeys.get(0);
                    toAddress = insAccountKeys.get(2);
                } else {
                    continue;
                }
                Transaction.Action.ActionBuilder actionBuilder = Transaction.Action.builder().fromAddress(fromAddress).toAddress(toAddress).amount(new BigDecimal(systemTransferData.getAmount())).contractAddress("").symbol(getMainCoinName());
                for (int i = 0; i < meta.getPostBalances().size(); i++) {
                    String account = accountKeys.get(i);
                    if (org.apache.commons.lang3.StringUtils.equals(account, fromAddress)) {
                        actionBuilder.fromPostBalance(meta.getPostBalances().get(i));
                    }
                    if (org.apache.commons.lang3.StringUtils.equals(account, toAddress)) {
                        actionBuilder.toPostBalance(meta.getPostBalances().get(i));
                    }
                }
                actions.add(actionBuilder.build());

            } else if (org.apache.commons.lang3.StringUtils.equals(TokenProgramID, programId)) {
                TokenTransferData tokenTransferData = new TokenTransferData();
                tokenTransferData.parse(instruction.getData(), hash);
                if (tokenTransferData.getAmount() <= 0) {
                    continue;
                }
                String ataFrom = "";
                String mint = "";
                String ataTo = "";
                String fromAddress = "";
                if (tokenTransferData.getInstructionType() == TokenInstructionType.InstructionTransferChecked.ordinal()) {
                    ataFrom = insAccountKeys.get(0);
                    mint = insAccountKeys.get(1);
                    ataTo = insAccountKeys.get(2);
                    fromAddress = insAccountKeys.get(3);
                } else if (tokenTransferData.getInstructionType() == TokenInstructionType.InstructionTransfer.ordinal()) {
                    ataFrom = insAccountKeys.get(0);
                    ataTo = insAccountKeys.get(1);
                    fromAddress = insAccountKeys.get(2);
                    for (SolConfirmedTransaction.MetaModel.PostTokenBalancesModel postTokenBalance : meta.getPostTokenBalances()) {
                        Integer accountIndex = postTokenBalance.getAccountIndex();
                        String ataAccount = accountKeys.get(accountIndex);
                        if (StringUtils.equals(ataAccount, ataFrom) || StringUtils.equals(ataAccount, ataTo)) {
                            mint = postTokenBalance.getMint();
                            break;
                        }
                    }
                } else if (tokenTransferData.getInstructionType() == TokenInstructionType.InstructionMintToChecked.ordinal()) {
                    mint = insAccountKeys.get(0);
                    ataTo = insAccountKeys.get(1);
                    fromAddress = insAccountKeys.get(2);
                } else if (tokenTransferData.getInstructionType() == TokenInstructionType.InstructionMintTo.ordinal()) {
                    mint = insAccountKeys.get(0);
                    ataTo = insAccountKeys.get(1);
                    fromAddress = insAccountKeys.get(2);
                } else {
                    continue;
                }
                String toAddress = "";
                Transaction.Action.ActionBuilder actionBuilder = Transaction.Action.builder().fromAddress(fromAddress).toAddress(toAddress).ataFrom(ataFrom).ataTo(ataTo).amount(new BigDecimal(tokenTransferData.getAmount())).contractAddress(mint).symbol(mint);
                for (SolConfirmedTransaction.MetaModel.PostTokenBalancesModel postTokenBalance : meta.getPostTokenBalances()) {
                    Integer accountIndex = postTokenBalance.getAccountIndex();
                    String account = accountKeys.get(accountIndex);
                    if (org.apache.commons.lang3.StringUtils.equals(account, ataTo)) {
                        toAddress = postTokenBalance.getOwner();
                        actionBuilder.toAddress(toAddress);
                        actionBuilder.toPostBalance(postTokenBalance.getUiTokenAmount().getUiAmount());
                    } else if (org.apache.commons.lang3.StringUtils.equals(account, ataFrom)) {
                        actionBuilder.fromPostBalance(postTokenBalance.getUiTokenAmount().getUiAmount());
                    }
                }
                actions.add(actionBuilder.build());
            }
        }
        return Transaction.builder().fee(meta.getFee().stripTrailingZeros()).blockHeight(blockHeight)
                .feePayer(transaction.getMessage().getAccountKeys().get(0))
                .status(paymentStatus.getCode()).txHash(hash)
                .blockTime(blockTime.longValue())
                .actions(actions).build();
    }

    public void sendTransaction(String rawTx) {
        List<Object> list = new ArrayList<>();
        list.add(rawTx);
        HashMap<String, Object> base = new HashMap<>();
        base.put("encoding", "base64");
        base.put("maxRetries", 5);
        list.add(base);
        ResponseEntity<JSONObject> responseEntity = exchange(new JsonRpcRequest<>("sendTransaction", list), new ParameterizedTypeReference<JSONObject>() {
        });
        if (responseEntity.getBody() == null) {
            throw new IllegalArgumentException("响应失败");
        }
        if (responseEntity.getBody().containsKey("error")) {
            throw new IllegalArgumentException(String.valueOf(responseEntity.getBody().get("error")));
        }
        logger.info("sendTransaction : {}", responseEntity.getBody());
    }

    public enum SystemInstructionType {
        InstructionCreateAccount,
        InstructionAssign,
        InstructionTransfer,
        InstructionCreateAccountWithSeed,
        InstructionAdvanceNonceAccount,
        InstructionWithdrawNonceAccount,
        InstructionInitializeNonceAccount,
        InstructionAuthorizeNonceAccount,
        InstructionAllocate,
        InstructionAllocateWithSeed,
        InstructionAssignWithSeed,
        InstructionTransferWithSeed,
        InstructionUpgradeNonceAccount,
    }

    @Data
    @ToString
    public static class SystemTransferData {
        public int instructionType;
        public long amount;

        public void parse(String data, String hash) {
            try {
                parse(Base58.decode(data));
            } catch (Exception e) {
                logger.error("SystemTransferData {},\t{}", data, hash, e);
            }
        }

        public void parse(byte[] data) {
            ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
            this.instructionType = buffer.getInt();
            if (SystemInstructionType.InstructionTransfer.ordinal() == instructionType || SystemInstructionType.InstructionTransferWithSeed.ordinal() == instructionType) {
                this.amount = buffer.getLong();
            }
        }
    }


    public enum TokenInstructionType {
        InstructionInitializeMint,
        InstructionInitializeAccount,
        InstructionInitializeMultisig,
        InstructionTransfer,
        InstructionApprove,
        InstructionRevoke,
        InstructionSetAuthority,
        InstructionMintTo,
        InstructionBurn,
        InstructionCloseAccount,
        InstructionFreezeAccount,
        InstructionThawAccount,
        InstructionTransferChecked,
        InstructionApproveChecked,
        InstructionMintToChecked,
        InstructionBurnChecked,
        InstructionInitializeAccount2,
        InstructionSyncNative,
        InstructionInitializeAccount3,
        InstructionInitializeMultisig2,
        InstructionInitializeMint2,
    }

    @Data
    @ToString
    public static class TokenTransferData {

        public int instructionType;
        public long amount;
        private int decimals;

        public void parse(String data, String message) {
            try {
                parse(Base58.decode(data));
            } catch (Exception e) {
                logger.error("TokenTransferData {},\t{}", data, message, e);
            }
        }

        public void parse(byte[] data) {
            ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
            this.instructionType = buffer.get();
            if (this.instructionType == TokenInstructionType.InstructionTransferChecked.ordinal() || this.instructionType == TokenInstructionType.InstructionMintToChecked.ordinal()) {
                this.amount = buffer.getLong();
                this.decimals = buffer.get();
            } else if (this.instructionType == TokenInstructionType.InstructionMintTo.ordinal() || this.instructionType == TokenInstructionType.InstructionTransfer.ordinal()) {
                this.amount = buffer.getLong();
            }
        }
    }

    public static void main(String[] args) {
        SolanaRpcClient solanaRpcClient = new SolanaRpcClient(
                // "https://api.mainnet-beta.solana.com"
                // "https://sol.nownodes.io/4e2966c3-bc74-40a8-883e-2edb4550c557"
                "https://solana-mainnet.gateway.tatum.io", "x-api-key", "t-680f9e2a72fc4543b7e39cb8-df8a6c02a9424cbea793ffef");
//        SolanaRpcClient solanaRpcClient = new SolanaRpcClient("https://api.devnet.solana.com");
//        Long blockHeight = solanaRpcClient.getHeight().getBlockHeight();
//        BlockTx blockTx = solanaRpcClient.getBlockTx(blockHeight);
//        System.out.println(JSON.toJSONString(blockTx.getTxs(), true));
        Transaction tx = solanaRpcClient.getTx("5rcF1b5vZHyZNb3dFZqBkfsTQdE4xvRbyBiigyR9N2VG7eDA2HLdfrjHbRp2BnVahfyc1vKiL18mxGm2wbC9JKJb");
        System.out.println(JSON.toJSONString(tx, true));
//
//        SolTokenAccounts tokenAccountsByOwner = solanaRpcClient.getTokenAccountsByOwner("9rPYyANsfQZw3DnDmKE3YCQF5E8oD89UXoHn9JFEhJUz", "So11111111111111111111111111111111111111112");
//
//        System.out.println(JSON.toJSONString(tokenAccountsByOwner, true));
//
//        if (tokenAccountsByOwner != null && CollectionUtils.isNotEmpty(tokenAccountsByOwner.getValue())) {
//            String ataTo = tokenAccountsByOwner.getValue().get(0).getPubkey();
//        }

//////        SolTokenAccounts tokenAccountsByOwner = solanaRpcClient.getTokenAccountsByOwner("HkVXjLrCVjaoyjgFX828yDRBnu4XVwMw5gLbgTSG7gtr", "7kMRdbsnUbpyrTPoJ13hVopnSotm9uC8bLpTUk4H9Qsk");
//////
//////
//////        System.out.println(JSON.toJSONString(tokenAccountsByOwner, true));
////
////        System.out.println(solanaRpcClient.getLatestBlocHash());
//
//
////        SystemTransferData systemTransferData = new SystemTransferData();
////        systemTransferData.parse(Base58.decode("3Bxs3zzLZLuLQEYX"));
////        System.out.println(systemTransferData);
////
////        TokenTransferData tokenTransferData = new TokenTransferData();
////        tokenTransferData.parse(Base58.decode("g7Bq2vfyeK8ue"));
////        System.out.println(tokenTransferData);
////
////        SolTokenAccountValue solTokenAccountValue = solanaRpcClient.getTokenAccountInfo("GuTdNcub79PPrcD4ARvnCvpEXJhofyGRDzLs5GC3uWQj");
////        System.out.println(solTokenAccountValue);
    }

}
