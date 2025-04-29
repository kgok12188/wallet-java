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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class EventManager implements BeanPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(EventManager.class);


    private static final AtomicInteger index = new AtomicInteger(1);


    @Autowired
    private ApplicationContext applicationContext;

    private final List<ChainEventListener> chainEventListeners = new ArrayList<>();

    private final static ExecutorService executorService = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(), Runtime.getRuntime().availableProcessors(), 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), r -> new Thread(r, "event-" + index.getAndIncrement()));

    public void emit(Event event) {
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

    @Nullable
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof ChainEventListener) {
            chainEventListeners.add((ChainEventListener) bean);
        }
        return bean;
    }

}
