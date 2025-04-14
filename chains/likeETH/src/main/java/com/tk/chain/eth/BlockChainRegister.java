package com.tk.chain.eth;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;

import java.util.HashSet;

@Service
public class BlockChainRegister implements ApplicationContextAware {

    private static final HashSet<String> chainIdList = new HashSet<>();

    private ConfigurableApplicationContext applicationContext;

    @Override
    public void setApplicationContext(@NotNull ApplicationContext applicationContext) {
        this.applicationContext = (ConfigurableApplicationContext) applicationContext;
        registerBean();
    }

    public BlockChainRegister(@Value("${chainIds:}") String chainIds) {
        String[] list = chainIds.split(",");
        for (String chainId : list) {
            if (StringUtils.isNotBlank(chainId)) {
                chainIdList.add(chainId.trim());
            }
        }
    }

    public void registerBean() {
        for (String chainId : chainIdList) {
            // 1. 通过 BeanDefinitionBuilder 构建定义
            BeanDefinitionBuilder builder = BeanDefinitionBuilder
                    .genericBeanDefinition(LIkeETHBlockChain.class)
                    .setScope(ConfigurableBeanFactory.SCOPE_SINGLETON);
            // 2、注册到spring容器中
            ((BeanDefinitionRegistry) applicationContext.getBeanFactory())
                    .registerBeanDefinition(chainId, builder.getBeanDefinition());
        }
    }

}
