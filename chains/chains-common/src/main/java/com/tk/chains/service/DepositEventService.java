package com.tk.chains.service;

import com.tk.chains.event.ChainEventListener;
import com.tk.chains.event.Event;
import com.tk.chains.event.TransactionEvent;
import com.tk.wallet.common.entity.ChainTransaction;
import com.tk.wallet.common.entity.WalletAddress;
import com.tk.wallet.common.entity.WalletDeposit;
import com.tk.wallet.common.fingerprint.CalcFingerprintService;
import com.tk.wallet.common.service.WalletAddressService;
import com.tk.wallet.common.service.WalletDepositService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.function.Consumer;

@Service
public class DepositEventService implements ChainEventListener {

    @Autowired
    private WalletDepositService walletDepositService;
    @Autowired
    private CalcFingerprintService calcFingerprintService;
    @Autowired
    private WalletAddressService walletAddressService;


    @Override
    public void process(Event event) {
        if (event instanceof TransactionEvent) {
            TransactionEvent transactionEvent = (TransactionEvent) event;

            Consumer<ChainTransaction> consumer = (chainTransaction) -> {
                WalletAddress walletAddress = walletAddressService.lambdaQuery().eq(WalletAddress::getAddress, chainTransaction.getToAddress())
                        .eq(WalletAddress::getBaseSymbol, chainTransaction.getChainId()).one();
                if (walletAddress == null) {
                    return;
                }
                if (StringUtils.isBlank(chainTransaction.getBusinessId())) {
                    String transferId = String.valueOf(chainTransaction.getId());
                    WalletDeposit dbWalletDeposit = walletDepositService.lambdaQuery().eq(WalletDeposit::getTransferId, transferId).one();
                    if (dbWalletDeposit == null) {
                        WalletDeposit walletDeposit = new WalletDeposit();
                        walletDeposit.setCtime(chainTransaction.getCtime());
                        walletDeposit.setMtime(chainTransaction.getMtime());
                        walletDeposit.setAddressTo(chainTransaction.getToAddress());
                        walletDeposit.setContract(chainTransaction.getContract());
                        walletDeposit.setAmount(chainTransaction.getAmount());
                        walletDeposit.setBaseSymbol(chainTransaction.getChainId());
                        walletDeposit.setSymbol(chainTransaction.getSymbol());
                        walletDeposit.setNoticeStatus(WalletDeposit.NotifyStatus.INIT.ordinal());
                        walletDeposit.setTxid(chainTransaction.getHash());
                        walletDeposit.setWalletId(walletAddress.getWalletId());
                        if (StringUtils.equals(chainTransaction.getTxStatus(), ChainTransaction.TX_STATUS.PENDING.name())) {
                            walletDeposit.setConfirmations(0);
                            walletDeposit.setStatus(WalletDeposit.STATUS.INIT.ordinal());
                        } else if (StringUtils.equals(chainTransaction.getTxStatus(), ChainTransaction.TX_STATUS.SUCCESS.name())) {
                            walletDeposit.setStatus(WalletDeposit.STATUS.SUCCESS.ordinal());
                            walletDeposit.setConfirmations(chainTransaction.getNeedConfirmNum());
                        } else {
                            walletDeposit.setStatus(WalletDeposit.STATUS.FAIL.ordinal());
                        }
                        walletDeposit.setTransferId(transferId);
                        walletDepositService.save(walletDeposit);
                        // 计算指纹，并更新到数据库
                        calcFingerprintService.calcFingerprint(walletDeposit, walletDepositService, new WalletDeposit());
                    } else if (Objects.equals(dbWalletDeposit.getStatus(), WalletDeposit.STATUS.INIT.ordinal())) {
                        if (StringUtils.equals(chainTransaction.getTxStatus(), ChainTransaction.TX_STATUS.SUCCESS.name())) {
                            WalletDeposit update = new WalletDeposit();
                            update.setId(dbWalletDeposit.getId());
                            update.setStatus(WalletDeposit.STATUS.SUCCESS.ordinal());
                            walletDepositService.updateById(update);
                        }
                    }
                }
            };

            if (transactionEvent.getChainTransaction() != null) {
                ChainTransaction chainTransaction = transactionEvent.getChainTransaction();
                if (StringUtils.isBlank(chainTransaction.getBusinessId())) {
                    consumer.accept(chainTransaction);
                }
            } else if (CollectionUtils.isNotEmpty(transactionEvent.getChainTransactions())) {
                for (ChainTransaction chainTransaction : transactionEvent.getChainTransactions()) {
                    if (StringUtils.isBlank(chainTransaction.getBusinessId())) {
                        consumer.accept(chainTransaction);
                    }
                }
            }
        }
    }

}
