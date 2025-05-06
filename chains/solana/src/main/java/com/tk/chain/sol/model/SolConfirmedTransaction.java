package com.tk.chain.sol.model;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

@NoArgsConstructor
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SolConfirmedTransaction {


    @JSONField(name = "blockTime")
    private BigInteger blockTime;
    @JSONField(name = "meta")
    private MetaModel meta;
    @JSONField(name = "slot")
    private BigInteger slot;
    @JSONField(name = "transaction")
    private TransactionModel transaction;
    @JsonProperty("version")
    private Object version;


    @NoArgsConstructor
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MetaModel {
        @JSONField(name = "err")
        private Object err;
        @JSONField(name = "fee")
        private BigDecimal fee;
        @JSONField(name = "innerInstructions")
        private List<JSONObject> innerInstructions;
        @JSONField(name = "logMessages")
        private List<String> logMessages;
        @JSONField(name = "postBalances")
        private List<BigDecimal> postBalances;
        @JSONField(name = "postTokenBalances")
        private List<PostTokenBalancesModel> postTokenBalances;
        @JSONField(name = "preBalances")
        private List<BigDecimal> preBalances;
        @JSONField(name = "preTokenBalances")
        private List<PreTokenBalancesModel> preTokenBalances;
        @JSONField(name = "rewards")
        private List<?> rewards;
        @JSONField(name = "status")
        private StatusModel status;

        @NoArgsConstructor
        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class StatusModel {
            @JSONField(name = "Ok")
            private Object ok;

            @JSONField(name = "Err")
            private Object err;
        }

        @NoArgsConstructor
        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class PostTokenBalancesModel {
            @JSONField(name = "accountIndex")
            private Integer accountIndex;
            @JSONField(name = "mint")
            private String mint;
            private String owner;
            @JSONField(name = "uiTokenAmount")
            private UiTokenAmountModel uiTokenAmount;

            @NoArgsConstructor
            @Data
            public static class UiTokenAmountModel {
                @JSONField(name = "amount")
                private String amount;
                @JSONField(name = "decimals")
                private Integer decimals;
                @JSONField(name = "uiAmount")
                private BigDecimal uiAmount;
                @JSONField(name = "uiAmountString")
                private String uiAmountString;
            }
        }

        @NoArgsConstructor
        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class PreTokenBalancesModel {
            @JSONField(name = "accountIndex")
            private Integer accountIndex;
            @JSONField(name = "mint")
            private String mint;
            @JSONField(name = "uiTokenAmount")
            private UiTokenAmountModel uiTokenAmount;

            @NoArgsConstructor
            @Data
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class UiTokenAmountModel {
                @JSONField(name = "amount")
                private String amount;
                @JSONField(name = "decimals")
                private Integer decimals;
                @JSONField(name = "uiAmount")
                private BigDecimal uiAmount;
                @JSONField(name = "uiAmountString")
                private String uiAmountString;
            }
        }

    }


    @NoArgsConstructor
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TransactionModel {
        @JSONField(name = "message")
        private MessageModel message;
        @JSONField(name = "signatures")
        private List<String> signatures;

        @NoArgsConstructor
        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class MessageModel {
            @JSONField(name = "accountKeys")
            private List<String> accountKeys;
            @JSONField(name = "header")
            private HeaderModel header;
            @JSONField(name = "instructions")
            private List<InstructionsModel> instructions;
            @JSONField(name = "recentBlockhash")
            private String recentBlockhash;

            @NoArgsConstructor
            @Data
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class HeaderModel {
                @JSONField(name = "numReadonlySignedAccounts")
                private Integer numReadonlySignedAccounts;
                @JSONField(name = "numReadonlyUnsignedAccounts")
                private Integer numReadonlyUnsignedAccounts;
                @JSONField(name = "numRequiredSignatures")
                private Integer numRequiredSignatures;
            }

            @NoArgsConstructor
            @Data
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class InstructionsModel {
                @JSONField(name = "accounts")
                private List<Integer> accounts;
                @JSONField(name = "data")
                private String data;
                @JSONField(name = "programIdIndex")
                private Integer programIdIndex;

                public String getProgramId(List<String> accountKeys) {
                    return accountKeys.get(programIdIndex);
                }

            }
        }
    }


}
