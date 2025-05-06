package com.tk.chain.sol.utils;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.bitcoinj.core.Base58;
import org.bouncycastle.jcajce.provider.digest.SHA256;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

public class SolanaPDAUtils {

    private static final Logger logger = LoggerFactory.getLogger(SolanaPDAUtils.class);
    // SPL 代币程序地址
    private static final String TOKEN_PROGRAM_ID = "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA";
    // 关联代币程序地址
    private static final String ASSOCIATED_TOKEN_PROGRAM_ID = "ATokenGPvbdGVxr1b2hvZbsiqW5xWH25efTNsLJA8knL";


    public static final String SystemProgramID = "11111111111111111111111111111111";
    public static final String ConfigProgramID = "Config1111111111111111111111111111111111111";
    public static final String StakeProgramID = "Stake11111111111111111111111111111111111111";
    public static final String VoteProgramID = "Vote111111111111111111111111111111111111111";
    public static final String BPFLoaderProgramID = "BPFLoader1111111111111111111111111111111111";
    public static final String Secp256k1ProgramID = "KeccakSecp256k11111111111111111111111111111";
    public static final String TokenProgramID = "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA";
    public static final String MemoProgramID = "MemoSq4gqABAXKb96qnH8TysNcWxMyWCqXgDLGmfcHr";
    public static final String SPLAssociatedTokenAccountProgramID = "ATokenGPvbdGVxr1b2hvZbsiqW5xWH25efTNsLJA8knL";
    public static final String SPLNameServiceProgramID = "namesLPneVptA9Z5rqUDD9tMTWEJwofgaYwp8cawRkX";
    public static final String MetaplexTokenMetaProgramID = "metaqbxxUerdq28cj1RbAWkYQm3ybzjb6a8bt518x1s";
    public static final String ComputeBudgetProgramID = "ComputeBudget111111111111111111111111111111";
    public static final String AddressLookupTableProgramID = "AddressLookupTab1e1111111111111111111111111";
    public static final String Token2022ProgramID = "TokenzQdBNbLqP5VEhdkAS6EPFLC1PHnBqCXEpPxuEb";
    public static final String BPFLoaderUpgradeableProgramID = "BPFLoaderUpgradeab1e11111111111111111111111";
    public static final String WSOL = "So11111111111111111111111111111111111111112";
    public static final HashSet<String> ProgramIDs = new HashSet<>();

    static {
        ProgramIDs.add(SystemProgramID);
        ProgramIDs.add(ConfigProgramID);
        ProgramIDs.add(StakeProgramID);
        ProgramIDs.add(VoteProgramID);
        ProgramIDs.add(BPFLoaderProgramID);
        ProgramIDs.add(Secp256k1ProgramID);
        ProgramIDs.add(TokenProgramID);
        ProgramIDs.add(MemoProgramID);
        ProgramIDs.add(SPLAssociatedTokenAccountProgramID);
        ProgramIDs.add(SPLNameServiceProgramID);
        ProgramIDs.add(MetaplexTokenMetaProgramID);
        ProgramIDs.add(ComputeBudgetProgramID);
        ProgramIDs.add(AddressLookupTableProgramID);
        ProgramIDs.add(Token2022ProgramID);
        ProgramIDs.add(BPFLoaderUpgradeableProgramID);
        ProgramIDs.add(WSOL);
    }

    // 模拟 Go 的 PublicKey 对象
    @Getter
    public static class PublicKey {
        private final byte[] bytes;

        public PublicKey(String base58) {
            this.bytes = Base58.decode(base58);
        }

        public PublicKey(byte[] bytes) {
            this.bytes = bytes;
        }

        public String toBase58() {
            return Base58.encode(bytes);
        }
    }


    public static String findAssociatedTokenAddress(String walletAddressStr, String tokenMintAddressStr) {
        if (ProgramIDs.contains(walletAddressStr)) {
            return "";
        }
        PublicKey walletAddress = new PublicKey(walletAddressStr);
        PublicKey tokenMintAddress = new PublicKey(tokenMintAddressStr);
        PublicKey associatedTokenAddress = findAssociatedTokenAddress(walletAddress, tokenMintAddress);
        return associatedTokenAddress == null ? "" : associatedTokenAddress.toBase58();
    }

    public static List<String> findAssociatedTokenAddressList(String walletAddressStr, String tokenMintAddressStr) {
        ArrayList<String> list = new ArrayList<>();
        if (ProgramIDs.contains(walletAddressStr)) {
            return list;
        }
        PublicKey walletAddress = new PublicKey(walletAddressStr);
        PublicKey tokenMintAddress = new PublicKey(tokenMintAddressStr);
        List<PublicKey> associatedTokenAddress = findAssociatedTokenAddressList(walletAddress, tokenMintAddress);
        for (PublicKey tokenAddress : associatedTokenAddress) {
            list.add(tokenAddress.toBase58());
        }
        return list;
    }

    // 主入口方法
    public static PublicKey findAssociatedTokenAddress(PublicKey walletAddress, PublicKey tokenMintAddress) {
        try {
            PublicKey tokenProgramId = new PublicKey(TOKEN_PROGRAM_ID);
            PublicKey associatedTokenProgramId = new PublicKey(ASSOCIATED_TOKEN_PROGRAM_ID);

            // 构造种子数组 [wallet, token_program, mint]
            List<byte[]> seeds = new ArrayList<>();
            seeds.add(walletAddress.getBytes());
            seeds.add(tokenProgramId.getBytes());
            seeds.add(tokenMintAddress.getBytes());

            // 查找有效的程序地址
            PublicKeyResult result = findProgramAddress(seeds, associatedTokenProgramId);
            return result.pubkey;
        } catch (Exception e) {
            logger.warn("生成 ATA 失败 {},\t{}", walletAddress.toBase58(), tokenMintAddress.toBase58());
        }
        return null;
    }


    public static List<PublicKey> findAssociatedTokenAddressList(PublicKey walletAddress, PublicKey tokenMintAddress) {
        List<PublicKey> list = new ArrayList<>();
        try {
            PublicKey tokenProgramId = new PublicKey(TOKEN_PROGRAM_ID);
            PublicKey associatedTokenProgramId = new PublicKey(ASSOCIATED_TOKEN_PROGRAM_ID);

            // 构造种子数组 [wallet, token_program, mint]
            List<byte[]> seeds = new ArrayList<>();
            seeds.add(walletAddress.getBytes());
            seeds.add(tokenProgramId.getBytes());
            seeds.add(tokenMintAddress.getBytes());
            // 查找有效的程序地址
            PublicKeyResult result = findProgramAddress(seeds, associatedTokenProgramId);
            list.add(result.pubkey);
        } catch (Exception e) {
            // logger.warn("生成 ATA 失败 {},\t{}", walletAddress.toBase58(), tokenMintAddress.toBase58());
        }
        return list;
    }


    // 查找程序地址（带 nonce）
    private static PublicKeyResult findProgramAddress(List<byte[]> seeds, PublicKey programId) {
        for (int nonce = 0xff; nonce >= 0; nonce--) {
            try {
                // 复制 seeds 并追加 nonce
                List<byte[]> seedsWithNonce = new ArrayList<>(seeds);
                seedsWithNonce.add(new byte[]{(byte) nonce});

                PublicKey address = createProgramAddress(seedsWithNonce, programId);
                return new PublicKeyResult(address, nonce);
            } catch (IllegalArgumentException ignored) {
                // 地址在曲线上，继续尝试
            }
        }
        throw new IllegalArgumentException("无法找到有效的程序地址");
    }

    // 查找程序地址（带 nonce）
    private static List<PublicKeyResult> findProgramAddressList(List<byte[]> seeds, PublicKey programId) {
        ArrayList<PublicKeyResult> results = new ArrayList<>();
        for (int nonce = 0xff; nonce >= 0; nonce--) {
            try {
                // 复制 seeds 并追加 nonce
                List<byte[]> seedsWithNonce = new ArrayList<>(seeds);
                seedsWithNonce.add(new byte[]{(byte) nonce});
                PublicKey address = createProgramAddress(seedsWithNonce, programId);
                results.add(new PublicKeyResult(address, nonce));
            } catch (IllegalArgumentException ignored) {
                // 地址在曲线上，继续尝试
            }
        }
        return results;
    }

    // 创建程序派生地址（PDA）
    private static PublicKey createProgramAddress(List<byte[]> seeds, PublicKey programId) {
        try {
            SHA256.Digest digest = new SHA256.Digest();

            // 拼接所有 seeds 的字节
            for (byte[] seed : seeds) {
                digest.update(seed);
            }

            // 添加程序 ID 和固定后缀
            digest.update(programId.getBytes());
            digest.update("ProgramDerivedAddress".getBytes());

            byte[] hash = digest.digest();

            // 验证是否在曲线上
            if (isOnCurve(hash)) {
                throw new IllegalArgumentException("地址在 Ed25519 曲线上");
            }
            return new PublicKey(hash);
        } catch (Exception e) {
            throw new RuntimeException("PDA 生成失败", e);
        }
    }

    // 检查是否在 Ed25519 曲线上（简化版校验）
    private static boolean isOnCurve(byte[] pubkey) {
        return pubkey.length == 32 && (pubkey[0] & 0xFF) == 0xED;
    }

    // 结果包装类
    private static class PublicKeyResult {
        PublicKey pubkey;
        int nonce;

        PublicKeyResult(PublicKey pubkey, int nonce) {
            this.pubkey = pubkey;
            this.nonce = nonce;
        }
    }

    // 测试用例
    public static void main(String[] args) {
//        PublicKey wallet = new PublicKey("7eWD12cdF6T6USsVBowpMbWxgXePgh8QLtv5jt2BtZ1m");
//        PublicKey mint = new PublicKey("8YMShjK5G6h3XcX6QJVaf8V1YjxwQJ4DKz5rHYvvinn4");

        List<String> associatedTokenAddressList = findAssociatedTokenAddressList("7eWD12cdF6T6USsVBowpMbWxgXePgh8QLtv5jt2BtZ1m", "8YMShjK5G6h3XcX6QJVaf8V1YjxwQJ4DKz5rHYvvinn4");
        System.out.println("ATA: " + associatedTokenAddressList);
        // 输出应该与 Go 版本一致：2Kos1xJ4sAcS7kKRAvqWVBG1epsimz6HcXJYjvH1Tqj7
    }

}