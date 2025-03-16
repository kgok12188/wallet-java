//package com.tk.chain;
//
//import org.web3j.abi.FunctionEncoder;
//import org.web3j.abi.TypeReference;
//import org.web3j.abi.datatypes.Address;
//import org.web3j.abi.datatypes.DynamicArray;
//import org.web3j.abi.datatypes.StaticStruct;
//import org.web3j.abi.datatypes.Type;
//import org.web3j.abi.datatypes.generated.Uint256;
//import org.web3j.crypto.Credentials;
//import org.web3j.crypto.RawTransaction;
//import org.web3j.crypto.TransactionEncoder;
//import org.web3j.protocol.Web3j;
//import org.web3j.protocol.core.DefaultBlockParameterName;
//import org.web3j.protocol.http.HttpService;
//import org.web3j.utils.Numeric;
//
//import java.io.IOException;
//import java.math.BigInteger;
//import java.util.ArrayList;
//import java.util.List;
//
//public class TransferParams extends StaticStruct {
//
//    public TransferParams(String token, String address, BigInteger amount) {
//        super(
//                new org.web3j.abi.datatypes.Address(token),
//                new org.web3j.abi.datatypes.Address(address),
//                new org.web3j.abi.datatypes.generated.Uint256(amount)
//        );
//    }
//
//    public static void main(String[] args) throws IOException, InterruptedException {
//
//
//        String methodName = "transfer"; // solidity 合约方法
//        List<Type> inputParameters = new ArrayList<>();
//        List<TypeReference<?>> outputParameters = new ArrayList<>();
//
//        // struct 数组
////        DynamicArray<TransferParams> dynamicStruct1 = new DynamicArray<>(
////                new TransferParams("0x84b9B910527Ad5C03A9Ca831909E21e236EA7b06",
////                        "0x27F8219F6DFE695b9409e5e7f89F2218F126efec", BigInteger.TEN.multiply(BigInteger.TEN.pow(8)).multiply(new BigInteger("1"))),
////
////                new TransferParams("0x01bC06B31e9bd4CA058Cd4982A9241321C08A363",
////                        "0x27F8219F6DFE695b9409e5e7f89F2218F126efec", BigInteger.TEN.multiply(BigInteger.TEN.pow(8)).multiply(new BigInteger("5")))
////        );
//
//
//        // inputParameters 也可以添加其他类型
//        inputParameters.add(new Address("0x02eE3B978E91630BBCA5d3Dd76eB949d4FccE054"));
//        Uint256 uint256 = new Uint256(new BigInteger("10").pow(18));
//        inputParameters.add(uint256);
//
//
////        inputParameters.add(new TransferParams("0x12b47688216B66DBF987c50eF4DCBb6064057624",
////                "0x27F8219F6DFE695b9409e5e7f89F2218F126efec", BigInteger.TEN.multiply(BigInteger.TEN.pow(18))));
////
////        inputParameters.add(new TransferParams("0xe70252BB07dEA8d3362b58e77476366855A125A4",
////                "0x27F8219F6DFE695b9409e5e7f89F2218F126efec", BigInteger.TEN.multiply(BigInteger.TEN.pow(18))));
//
//        org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(methodName, inputParameters, outputParameters);
//
//
//        Web3j web3j = Web3j.build(new HttpService("https://rpc.sepolia.ethpandaops.io"));
//
//
//        BigInteger chainId = web3j.ethChainId().send().getChainId();
//
//        //  Credentials credentials = Credentials.create("私钥");
//
//        BigInteger gasPrice = web3j.ethGasPrice().send().getGasPrice();
//
//        System.out.println("gasPrice=" + gasPrice.multiply(new BigInteger("2")));
//        // 发起交易的地址，获取nonce
//        BigInteger nonce = web3j.ethGetTransactionCount("0x27F8219F6DFE695b9409e5e7f89F2218F126efec", DefaultBlockParameterName.LATEST)
//                .send().getTransactionCount();
//
//        System.out.println("nonce=" + nonce);
//        System.out.println("encode:" + FunctionEncoder.encode(function));
//        System.out.println(chainId);
//
////        // to 是合约地址
//        RawTransaction rawTransaction = RawTransaction.createTransaction(nonce, gasPrice.max(new BigInteger("3")),
//                new BigInteger("90000"), "0x72D3F214f14eD444B5B5b1E90EE08d0F09075C65", FunctionEncoder.encode(function));
//        rawTransaction.getData();
////        byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, chainId.intValue(), credentials);
////        String hexValue = Numeric.toHexString(signedMessage);
////        String transactionHash = web3j.ethSendRawTransaction(hexValue).send().getTransactionHash();
////        System.out.println(transactionHash);
//
//
//    }
//
//}
