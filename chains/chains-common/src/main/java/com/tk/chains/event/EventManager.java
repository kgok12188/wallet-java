package com.tk.chains.event;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.tk.chains.BlockChain;
import com.tk.wallet.common.entity.ChainTransaction;
import com.tk.wallet.common.entity.SymbolConfig;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class EventManager implements BeanPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(EventManager.class);

    @Autowired
    private ApplicationContext applicationContext;

    private List<ChainEventListener> chainEventListeners = new ArrayList<>();

    private ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    public void emit(Event event) {
        if (event instanceof TransactionEvent) {
            TransactionEvent transactionEvent = (TransactionEvent) event;
            List<ChainTransaction> chainTransactions = transactionEvent.getChainTransactions();
            if (CollectionUtils.isNotEmpty(chainTransactions)) {
                for (ChainTransaction transaction : chainTransactions) {
                    SetSymbol(transaction);
                }
            }

            ChainTransaction chainTransaction = transactionEvent.getChainTransaction();
            if (chainTransaction != null) {
                SetSymbol(chainTransaction);
            }

        }
        if (CollectionUtils.isNotEmpty(chainEventListeners)) {
            for (ChainEventListener chainEventListener : chainEventListeners) {
                executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            chainEventListener.process(event);
                        } catch (Exception e) {
                            EventManager.log.warn("emit", e);
                        }
                    }
                });
            }
        }
    }

    private void SetSymbol(ChainTransaction chainTransaction) {
        BlockChain<?> blockChain = applicationContext.getBean(chainTransaction.getChainId(), BlockChain.class);
        if (StringUtils.isNotBlank(chainTransaction.getContract())) {
            SymbolConfig tokenConfig = blockChain.getTokenConfig(chainTransaction.getContract());
            chainTransaction.setApiCoin(tokenConfig.getSymbol());
        } else {
            chainTransaction.setApiCoin(blockChain.getMainCoinConfig().getSymbol());
        }
    }

    @Nullable
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if ( bean instanceof ChainEventListener) {
            chainEventListeners.add((ChainEventListener) bean);
        }
        return bean;
    }

}
