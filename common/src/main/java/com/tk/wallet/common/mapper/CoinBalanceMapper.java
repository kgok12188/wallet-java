package com.tk.wallet.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tk.wallet.common.entity.CoinBalance;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

public interface CoinBalanceMapper extends BaseMapper<CoinBalance> {

    @Update("update coin_balance set balance = balance + #{balance} where id = #{id}")
    void incrementAmount(@Param("id") Long id, @Param("balance") BigDecimal value);

}
