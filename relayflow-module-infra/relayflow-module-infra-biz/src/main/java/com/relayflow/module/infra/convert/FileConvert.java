package com.relayflow.module.infra.convert;

import com.relayflow.module.infra.api.file.dto.FileRespDTO;
import com.relayflow.module.infra.controller.admin.file.vo.FileListItemRespVO;
import com.relayflow.module.infra.dal.dataobject.InfraFileDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface FileConvert {

    FileConvert INSTANCE = Mappers.getMapper(FileConvert.class);

    FileRespDTO toDto(InfraFileDO file);

    FileListItemRespVO toListItem(InfraFileDO file);
}
