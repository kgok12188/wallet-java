package com.tk.wallet.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tk.wallet.common.entity.WalletSymbolConfig;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 * 币种配置表 Mapper 接口
 * </p>
 *
 * @author ${author}
 * @since 2022-10-08
 */
public interface WalletSymbolConfigMapper extends BaseMapper<WalletSymbolConfig> {

    @Select("select distinct wallet_id from wallet_symbol_config where agg_police = 0")
    List<Integer> distinctWalletIds();

    @Select("select agg_address_public_key from wallet_symbol_config where agg_address = #{address} limit 1")
    String getPublicKeyByAddress(@Param("address") String address);

}
