package com.tk.wallet.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tk.wallet.common.entity.CoinBalance;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

/**
 * private String chainId;
 * private String coin;
 * private String apiCoin;
 * private String address;
 * private String contractAddress;
 * private BigDecimal balance;
 * private BigInteger blockHeight;
 * private java.util.Date blockTime;
 * private java.util.Date mtime;
 */
public interface CoinBalanceMapper extends BaseMapper<CoinBalance> {

    @Update("update coin_balance set balance = balance + #{balance} where id = #{id}")
    void incrementAmount(@Param("id") Long id, @Param("balance") BigDecimal value);

    @Insert("        insert into coin_balance (chain_id, coin, api_coin," +
            "                             address, contract_address, balance, block_height," +
            "                             block_time, mtime)" +
            "        values (#{chainId}, #{coin}, #{apiCoin}," +
            "                #{address}, #{contractAddress}, #{balance}, #{blockHeight}," +
            "                #{blockTime}, now())" +
            "        on duplicate key update balance                = IF(${blockHeight} > block_height, #{balance}, balance)," +
            "                                mtime                  = IF(${blockHeight} > block_height, now(), mtime)," +
            "                                block_height           = IF(${blockHeight} > block_height, ${blockHeight}, block_height)")
    void upsert(CoinBalance coinBalance);

}
