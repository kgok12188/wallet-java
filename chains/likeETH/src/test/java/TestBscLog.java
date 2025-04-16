import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.protocol.http.HttpService;

import java.io.IOException;

public class TestBscLog {
    public static void main(String[] args) throws IOException {

        // Web3j web3j = Web3j.build(new HttpService("https://bsc.nownodes.io/4e2966c3-bc74-40a8-883e-2edb4550c557"));
        Web3j web3j = Web3j.build(new HttpService("https://binance.llamarpc.com"));
        EthFilter ethFilter = new EthFilter("0x436ecf680484af469f9514d599d4689bc9b99d17cf2e2204483b63b2aaad3a67", "0x55d398326f99059ff775485246999027b3197955");
        ethFilter.addSingleTopic("0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef");

        EthLog ethLog = web3j.ethGetLogs(ethFilter).send();

        if (ethLog.getLogs() != null) {
            for (EthLog.LogResult log : ethLog.getLogs()) {
                System.out.println(log);
            }
        }
    }

}
