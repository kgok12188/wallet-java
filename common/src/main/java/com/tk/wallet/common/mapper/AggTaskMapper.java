package com.tk.wallet.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tk.wallet.common.entity.AggTask;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;


public interface AggTaskMapper extends BaseMapper<AggTask> {

    @Insert("insert into agg_task_dependency(task_id,parent_task_id) values( #{taskId}, #{parentTaskId})")
    void saveAggTaskDependency(@Param("taskId") Long taskId, @Param("parentTaskId") Long parentTaskId);


    @Update("update agg_task set status = 0 where status = 4  and id in ( select task_id from agg_task_dependency where parent_task_id = #{parentTaskId})")
    void updateNextTaskToChain(@Param("parentTaskId") Long parentTaskId);

    @Update("update agg_task set status = 5 where status = 4  and id in ( select task_id from agg_task_dependency where parent_task_id = #{parentTaskId})")
    void updateNextTaskFail(@Param("parentTaskId") Long parentTaskId);

}
