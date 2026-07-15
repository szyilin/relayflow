package com.relayflow.module.system.convert;

import com.relayflow.module.system.controller.admin.permission.vo.PermissionRespVO;
import com.relayflow.module.system.dal.dataobject.SysPermissionDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mapper
public interface PermissionConvert {

    PermissionConvert INSTANCE = Mappers.getMapper(PermissionConvert.class);

    PermissionRespVO toVo(SysPermissionDO permission);

    default List<PermissionRespVO> buildTree(List<SysPermissionDO> permissions) {
        Map<Long, PermissionRespVO> nodeById = new HashMap<>();
        for (SysPermissionDO permission : permissions) {
            nodeById.put(permission.getId(), toVo(permission));
        }

        List<PermissionRespVO> roots = new ArrayList<>();
        for (SysPermissionDO permission : permissions) {
            PermissionRespVO node = nodeById.get(permission.getId());
            Long parentId = permission.getParentId() != null ? permission.getParentId() : 0L;
            if (parentId == 0L || !nodeById.containsKey(parentId)) {
                roots.add(node);
                continue;
            }
            PermissionRespVO parent = nodeById.get(parentId);
            if (parent.getChildren() == null) {
                parent.setChildren(new ArrayList<>());
            }
            parent.getChildren().add(node);
        }

        sortTree(roots);
        return roots;
    }

    private static void sortTree(List<PermissionRespVO> nodes) {
        nodes.sort(Comparator
                .comparing(PermissionRespVO::getSort, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(PermissionRespVO::getId));
        for (PermissionRespVO node : nodes) {
            if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                sortTree(node.getChildren());
            }
        }
    }
}
