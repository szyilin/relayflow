package com.relayflow.module.rtc.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.relayflow.module.rtc.dal.dataobject.RtcSessionDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface RtcSessionMapper extends BaseMapper<RtcSessionDO> {

    @Update("""
            UPDATE rtc_session
            SET participant_count = GREATEST(0, COALESCE(participant_count, 0) + #{delta}),
                update_time = NOW()
            WHERE room_name = #{roomName}
              AND status = 'ACTIVE'
              AND deleted = 0
            """)
    int applyParticipantDelta(@Param("roomName") String roomName, @Param("delta") int delta);
}
