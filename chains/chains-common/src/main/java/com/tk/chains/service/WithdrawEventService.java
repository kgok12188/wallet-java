package com.tk.chains.service;

import com.tk.chains.event.ChainEventListener;
import com.tk.chains.event.Event;
import com.tk.chains.event.TransactionEvent;
import com.tk.wallet.common.entity.ChainTransaction;
import com.tk.wallet.common.entity.WalletAddress;
import com.tk.wallet.common.entity.WalletDeposit;
import com.tk.wallet.common.entity.WalletWithdraw;
import com.tk.wallet.common.fingerprint.CalcFingerprintService;
import com.tk.wallet.common.service.WalletAddressService;
import com.tk.wallet.common.service.WalletDepositService;
import com.tk.wallet.common.service.WalletWithdrawService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.function.Consumer;

@Service
public class WithdrawEventService implements ChainEventListener {

    @Autowired
    private WalletWithdrawService walletWithdrawService;
    @Autowired
    private CalcFingerprintService calcFingerprintService;
    @Autowired
    private WalletAddressService walletAddressService;


    private static final String WITHDRAW_PRE = "withdraw-";

    @Override
    public void process(Event event) {
        if (event instanceof TransactionEvent) {
            TransactionEvent transactionEvent = (TransactionEvent) event;

            Consumer<ChainTransaction> consumer = (chainTransaction) -> {
                Long id = Long.valueOf(chainTransaction.getBusinessId().split("-")[1]);
                WalletWithdraw walletWithdraw = walletWithdrawService.getById(id);
                if (walletWithdraw != null && (Objects.equals(walletWithdraw.getStatus(), WalletWithdraw.Status.WITHDRAWING.ordinal()) || Objects.equals(walletWithdraw.getStatus(), WalletWithdraw.Status.SUCCESS.ordinal()))) {
                    if (StringUtils.equals(chainTransaction.getTxStatus(), ChainTransaction.TX_STATUS.SUCCESS.name())) {
                        WalletWithdraw update = new WalletWithdraw();
                        update.setId(walletWithdraw.getId());
                        update.setTxid(chainTransaction.getHash());
                        update.setStatus(WalletWithdraw.Status.SUCCESS.ordinal());
                        update.setConfirmations(chainTransaction.getNeedConfirmNum());
                        walletWithdrawService.updateById(update);
                    } else if (StringUtils.equals(chainTransaction.getTxStatus(), ChainTransaction.TX_STATUS.PENDING.name())) {
                        WalletWithdraw update = new WalletWithdraw();
                        update.setId(walletWithdraw.getId());
                        update.setTxid(chainTransaction.getHash());
                        update.setStatus(WalletWithdraw.Status.WITHDRAWING.ordinal());
                        update.setConfirmations(0);
                        walletWithdrawService.updateById(update);
                    }
                }
            };

            if (transactionEvent.getChainTransaction() != null) {
                ChainTransaction chainTransaction = transactionEvent.getChainTransaction();
                if (StringUtils.isNotBlank(chainTransaction.getBusinessId()) && StringUtils.startsWith(chainTransaction.getBusinessId(), WITHDRAW_PRE)) {
                    consumer.accept(chainTransaction);
                }
            } else if (CollectionUtils.isNotEmpty(transactionEvent.getChainTransactions())) {
                for (ChainTransaction chainTransaction : transactionEvent.getChainTransactions()) {
                    if (StringUtils.isNotBlank(chainTransaction.getBusinessId()) && StringUtils.startsWith(chainTransaction.getBusinessId(), WITHDRAW_PRE)) {
                        consumer.accept(chainTransaction);
                    }
                }
            }
        }
    }

}
