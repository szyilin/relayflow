package com.relayflow.module.calendar.convert;

import com.relayflow.module.calendar.controller.app.vo.CalCalendarRespVO;
import com.relayflow.module.calendar.dal.dataobject.CalCalendarDO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;
import org.springframework.util.StringUtils;

@Mapper
public interface CalCalendarConvert {

    CalCalendarConvert INSTANCE = Mappers.getMapper(CalCalendarConvert.class);

    CalCalendarRespVO toResp(CalCalendarDO row);

    @Mapping(target = "type", expression = "java(com.relayflow.module.calendar.enums.CalendarType.SHARED.name())")
    @Mapping(target = "permission", source = "permission", qualifiedByName = "defaultRead")
    @Mapping(target = "id", source = "calendar.id")
    @Mapping(target = "name", source = "calendar.name")
    @Mapping(target = "color", source = "calendar.color")
    @Mapping(target = "description", source = "calendar.description")
    @Mapping(target = "ownerUserId", source = "calendar.ownerUserId")
    CalCalendarRespVO toSharedResp(CalCalendarDO calendar, String permission);

    @Named("defaultRead")
    default String defaultRead(String permission) {
        return StringUtils.hasText(permission) ? permission : "READ";
    }
}
