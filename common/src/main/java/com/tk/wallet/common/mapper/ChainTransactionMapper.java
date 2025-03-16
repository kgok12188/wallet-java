package com.tk.wallet.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tk.wallet.common.entity.ChainTransaction;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigInteger;
import java.util.Date;
import java.util.List;

public interface ChainTransactionMapper extends BaseMapper<ChainTransaction> {

    @Update("update chain_transaction set block_num = #{blockNum} where chain_id = #{chainId} and hash = #{hash}")
    void updateBlockNum(@Param("chainId") String chainId, @Param("hash") String hash, @Param("blockNum") BigInteger blockNum);

    @Select("select id from chain_transaction where tx_status = 'PENDING' and block_num > 0 and  (block_num + need_confirm_num) <= ${blockNumber} " +
            " and chain_id = #{chainId} order by block_num asc limit ${limit}")
    List<Long> queryPendingChainTransaction(@Param("chainId") String chainId, @Param("blockNumber") BigInteger blockNumber, @Param("limit") int limit);

    @Update("update chain_transaction set tx_status = #{txStatus},mtime = now(),fail_code = #{failCode},message = #{message} where chain_id = #{chainId} and hash = #{hash}")
    void updateTxStatus(@Param("chainId") String chainId, @Param("hash") String hash, @Param("txStatus") String txStatus, @Param("failCode") String failCode, @Param("message") String message);

    @Update("update chain_transaction set act_gas = #{actGas}, gas_address = #{gasAddress}, block_time = #{blockTime}  where hash = #{hash} limit 1")
    void updateGas(ChainTransaction chainTransaction);

    @Update("update chain_transaction set tx_status = 'WAIT_TO_CHAIN',hash = #{hash},chain_info = #{chainInfo},mtime = now()  where id = #{id}")
    void updateHash(@Param("id") Long id, @Param("hash") String hash, @Param("chainInfo") String chainInfo);

    @Select("select id from chain_transaction where tx_status = 'INIT' and chain_id = #{chainId} " +
            " and from_address not in( select from_address from chain_transaction where  tx_status = 'WAIT_TO_CHAIN' and chain_id = #{chainId}) order by priority desc limit 200")
    List<Long> queryWaitToChain(@Param("chainId") String chainId);

    @Update("update chain_transaction set tx_status = 'WAITING_HASH', mtime = now(), transfer_block_number = #{transferBlockNumber},url_code = #{url}, nonce = #{nonce} where tx_status in('INIT','WAIT_TO_CHAIN') and  id = #{id}")
    Integer prepareTransfer(@Param("id") Long id, @Param("transferBlockNumber") BigInteger transferBlockNumber, @Param("url") String url, @Param("nonce") BigInteger nonce);

    @Update("update chain_transaction set tx_status = 'INIT',transfer_block_number = 0, error_count = error_count + 1  where tx_status = 'WAITING_HASH' and  id = #{id}")
    void releaseWaitingHash(@Param("id") Long id);

    @Update("update chain_transaction set tx_status = #{txStatus},mtime = now(),fail_code = #{failCode},message = #{message},transfer_block_number = #{transferBlockNumber} where id = #{id}")
    void updateTxStatusById(@Param("id") Long id, @Param("txStatus") String txStatus, @Param("failCode") String failCode, @Param("message") String message, @Param("transferBlockNumber") BigInteger transferBlockNumber);

    @Update("update chain_transaction set block_num = #{blockNum},act_gas = #{actGas},gas_address = #{gasAddress}, tx_status = #{txStatus},mtime = now(),fail_code = #{failCode},message = #{message},url_code = #{urlCode}, need_confirm_num = #{needConfirmNum},block_time = #{blockTime}  where chain_id = #{chainId} and hash = #{hash}")
    void mergeChainTransaction(ChainTransaction transaction);

    @Update("update chain_transaction set tx_status = 'INIT' where mtime < #{endTime} and  chain_id = #{chainId} and tx_status = 'WAITING_HASH'")
    void updateTooLongTimeGetHash(@Param("chainId") String chainId, @Param("endTime") Date time);

    @Select("select * from chain_transaction where hash = #{hash}")
    List<ChainTransaction> getByHash(String hash);
}