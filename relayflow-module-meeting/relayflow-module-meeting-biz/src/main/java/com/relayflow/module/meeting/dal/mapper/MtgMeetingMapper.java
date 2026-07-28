package com.relayflow.module.meeting.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.relayflow.module.meeting.dal.dataobject.MtgMeetingDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MtgMeetingMapper extends BaseMapper<MtgMeetingDO> {
}
