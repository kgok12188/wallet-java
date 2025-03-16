package com.tk.wallet.common.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tk.wallet.common.entity.ChainTransaction;
import com.tk.wallet.common.mapper.ChainTransactionMapper;
import org.springframework.stereotype.Service;

@Service
public class ChainTransactionService extends ServiceImpl<ChainTransactionMapper, ChainTransaction> {

}
