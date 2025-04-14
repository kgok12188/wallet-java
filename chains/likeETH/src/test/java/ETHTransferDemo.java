import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.*;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public class ETHTransferDemo extends StaticStruct {

    public ETHTransferDemo(String token, String address, BigInteger amount) {
        super(
                new org.web3j.abi.datatypes.Address(token),
                new org.web3j.abi.datatypes.Address(address),
                new org.web3j.abi.datatypes.generated.Uint256(amount)
        );
    }


    public static void erc20Transfer() throws IOException {

        String methodName = "transfer"; // solidity 合约方法

        List<Type> inputParameters = new ArrayList<>();

        inputParameters.add(new Address("0xE93Da3C3C227e59EfE7D4C5cB7D576225DD8CF55"));
        Uint256 uint256 = new Uint256(new BigInteger("10").pow(18));
        inputParameters.add(uint256);
        List<TypeReference<?>> outputParameters = new ArrayList<>();

        outputParameters.add(new TypeReference<Bool>() {
        });

        Web3j web3j = Web3j.build(new HttpService("https://data-seed-prebsc-1-s3.bnbchain.org:8545"));


        BigInteger chainId = web3j.ethChainId().send().getChainId();

        Credentials credentials = Credentials.create("私钥");

        BigInteger gasPrice = web3j.ethGasPrice().send().getGasPrice();

        System.out.println("gasPrice=" + gasPrice.multiply(new BigInteger("2")));
        // 发起交易的地址，获取nonce
        BigInteger nonce = web3j.ethGetTransactionCount("0x27F8219F6DFE695b9409e5e7f89F2218F126efec", DefaultBlockParameterName.LATEST)
                .send().getTransactionCount();


        org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(methodName, inputParameters, outputParameters);


        System.out.println("nonce=" + nonce);
        System.out.println("encode:" + FunctionEncoder.encode(function));
        System.out.println(chainId);

//        // to 是合约地址
        RawTransaction rawTransaction = RawTransaction.createTransaction(nonce, gasPrice.multiply(new BigInteger("2")),
                new BigInteger("90000"), "0x01bC06B31e9bd4CA058Cd4982A9241321C08A363", FunctionEncoder.encode(function));
        rawTransaction.getData();
        byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, chainId.intValue(), credentials);
        String hexValue = Numeric.toHexString(signedMessage);
        String transactionHash = web3j.ethSendRawTransaction(hexValue).send().getTransactionHash();
        System.out.println(transactionHash);
    }


    /**
     * 调用合约交易
     *
     * @throws IOException
     * @throws InterruptedException
     */
    public static void batchTransfer() throws IOException, InterruptedException {

        String methodName = "transfer"; // solidity 合约方法
        List<Type> inputParameters = new ArrayList<>();
        List<TypeReference<?>> outputParameters = new ArrayList<>();

        // struct 数组
        DynamicArray<ETHTransferDemo> dynamicStruct1 = new DynamicArray<>(
//                new TransferParams("0x84b9B910527Ad5C03A9Ca831909E21e236EA7b06",
//                        "0x27F8219F6DFE695b9409e5e7f89F2218F126efec", BigInteger.TEN.multiply(BigInteger.TEN.pow(8)).multiply(new BigInteger("1"))),

                new ETHTransferDemo("0x01bC06B31e9bd4CA058Cd4982A9241321C08A363",
                        "0x27F8219F6DFE695b9409e5e7f89F2218F126efec", BigInteger.TEN.multiply(BigInteger.TEN.pow(8)).multiply(new BigInteger("5")))
        );

        inputParameters.add(dynamicStruct1);


        org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(methodName, inputParameters, outputParameters);


        Web3j web3j = Web3j.build(new HttpService("https://data-seed-prebsc-1-s3.bnbchain.org:8545"));


        BigInteger chainId = web3j.ethChainId().send().getChainId();

        Credentials credentials = Credentials.create("私钥");

        BigInteger gasPrice = web3j.ethGasPrice().send().getGasPrice();

        System.out.println("gasPrice=" + gasPrice.multiply(new BigInteger("2")));
        // 发起交易的地址，获取nonce
        BigInteger nonce = web3j.ethGetTransactionCount("0x27F8219F6DFE695b9409e5e7f89F2218F126efec", DefaultBlockParameterName.LATEST)
                .send().getTransactionCount();

        System.out.println("nonce=" + nonce);
        System.out.println("encode:" + FunctionEncoder.encode(function));
        System.out.println(chainId);

//        // to 是合约地址
        RawTransaction rawTransaction = RawTransaction.createTransaction(nonce, gasPrice.max(new BigInteger("3")),
                new BigInteger("90000"), "0x72D3F214f14eD444B5B5b1E90EE08d0F09075C65", FunctionEncoder.encode(function));
        rawTransaction.getData();
        byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, chainId.intValue(), credentials);
        String hexValue = Numeric.toHexString(signedMessage);
        String transactionHash = web3j.ethSendRawTransaction(hexValue).send().getTransactionHash();
        System.out.println(transactionHash);
    }


    /**
     * 生成助记词
     *
     * @return 助记词
     */
    public static String getMnemonic() {
        // 生成 128 位随机熵 (生成12个单词)
        byte[] entropy = new byte[16];
        new SecureRandom().nextBytes(entropy);
        String mnemonic = MnemonicUtils.generateMnemonic(entropy);
        // 通过助记词生成种子（用于后续密钥派生）
        String passphrase = ""; // 可选项，用于加强安全性
        byte[] seed = MnemonicUtils.generateSeed(mnemonic, passphrase);

        System.out.println("Mnemonic: " + mnemonic);
        return mnemonic;
    }


    public static void mnemonic() throws IOException {
        // 1. 输入助记词（示例）
        String mnemonic = getMnemonic();

        // 2. 验证助记词有效性
        if (!MnemonicUtils.validateMnemonic(mnemonic)) {
            throw new IllegalArgumentException("无效助记词");
        }

        // 3. 通过助记词生成钱包,password 可选
        Credentials credentials = Bip44WalletUtils.loadBip44Credentials("001", mnemonic);

        ECKeyPair ecKeyPair = credentials.getEcKeyPair();
        String privateKey = ecKeyPair.getPrivateKey().toString(16);
        // 8. 生成以太坊地址
        String address = Keys.getAddress(ecKeyPair);
        System.out.println("ETH Address: 0x" + address);
        System.out.println("Private Key: " + ecKeyPair.getPrivateKey().toString(16));
        System.out.println("ETH Address: 0x" + Credentials.create(privateKey).getAddress());


    }

    /**
     * 生成助记词
     *
     * @param args
     * @throws IOException
     * @throws InterruptedException
     */
    public static void main(String[] args) throws IOException {
        erc20Transfer();
    }

}
