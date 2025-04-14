package com.tk.wallet.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tk.wallet.common.entity.ChainScanConfig;
import com.tk.wallet.common.entity.ScanChainQueue;
import org.apache.ibatis.annotations.*;

import java.math.BigInteger;
import java.util.Date;
import java.util.List;

public interface ChainScanConfigMapper extends BaseMapper<ChainScanConfig> {

    @Update("update chain_scan_config set block_number = #{blockNumber},scan_time = now() where  cast(block_number as UNSIGNED) <  cast(#{blockNumber} as UNSIGNED) and  chain_id = #{chainId}")
    void updateBlockNumber(@Param("chainId") String chainId,
                           @Param("blockNumber") BigInteger blockNumber);

    @Update("update chain_scan_config set task_update_time = now() where status = 1 and task_id = #{taskId}")
    void taskUpdateTime(@Param("taskId") String taskId);

    @Update("update chain_scan_config set task_update_time = now(),task_id = #{taskId}  where status = 1 and task_update_time < #{taskUpdateTime} and chain_id = #{chainId}")
    Integer updateTaskId(@Param("taskId") String taskId, @Param("chainId") String chainId, @Param("taskUpdateTime") Date taskUpdateTime);

    @Insert("replace into chain_scan_hosts(task_id,update_time) values(#{taskId},now())")
    Integer replaceChainScanHosts(@Param("taskId") String taskId);

    @Select("select count(1) from chain_scan_hosts")
    Integer hosts();

    @Delete("delete from chain_scan_hosts where update_time < #{date}")
    Integer deleteLostHost(@Param("date") Date date);

    @Update("update chain_scan_config set task_id = ''  where task_id = #{taskId} and chain_id = #{chainId}")
    void removeTaskId(@Param("chainId") String chainId, @Param("taskId") String taskId);

    @Select("select chain_id from agg_circuit_breaker")
    List<String> circuitBreaker();

    @Insert("replace into agg_circuit_breaker(chain_id,ctime) values( #{chainId}, now() )")
    void saveCircuitBreaker(@Param("chainId") String chainId);

    @Select("select id from scan_chain_queue where status in (0,1) and  chain_id = #{chainId} limit 1")
    Long hasUnFinishValue(@Param("chainId") String chainId);

    @Select("select max(block_number) from scan_chain_queue where chain_id = #{chainId} ")
    BigInteger maxBlockNumber(@Param("chainId") String chainId);

    @Insert("insert into scan_chain_queue(chain_id,block_number) values ( #{chainId}, #{blockNumber} ) ")
    void addScanBlockNumberToQueue(@Param("chainId") String chainId, @Param("blockNumber") BigInteger blockNumber);

    @Select("select id,chain_id as chainId , block_number as blockNumber , status , ctime from scan_chain_queue where status = 0 and chain_id = #{chainId} order by id asc limit ${limit}")
    List<ScanChainQueue> getBlockNumberList(@Param("limit") int limit, @Param("chainId") String chainId);

    @Select("select id,chain_id as chainId , block_number as blockNumber , status , ctime from scan_chain_queue where status in(0,1) and chain_id = #{chainId} order by id asc limit ${limit}")
    List<ScanChainQueue> getBlockNumberListNotEnd(@Param("limit") int limit, @Param("chainId") String chainId);

    @Update("update scan_chain_queue set status = 1,ctime = now() where status = 0 and id = #{id}")
    Integer updateStatus(@Param("id") Long id);


    @Update("delete from scan_chain_queue where id = #{id}")
    void deleteQueue(@Param("id") Long id);

    @Update("update scan_chain_queue set status = 0 where status = 1 and id = #{id}")
    Integer releaseBlock(@Param("id") Long id);

    @Update("update scan_chain_queue set status = 0 where status = 1 and ctime < #{date}")
    Integer updateLostTime(@Param("date") Date date);

    @Update("update chain_scan_config set task_id = ''  where task_id = #{taskId}")
    void remove(@Param("taskId") String taskId);

}
