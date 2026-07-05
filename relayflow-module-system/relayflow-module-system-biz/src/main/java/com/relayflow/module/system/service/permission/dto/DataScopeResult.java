package com.relayflow.module.system.service.permission.dto;

import lombok.Data;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Data
public class DataScopeResult {

    private boolean all;
    private Set<Long> deptIds = new HashSet<>();
    private boolean selfOnly;

    public static DataScopeResult all() {
        DataScopeResult result = new DataScopeResult();
        result.all = true;
        return result;
    }

    public static DataScopeResult empty() {
        return new DataScopeResult();
    }

    public Set<Long> getDeptIds() {
        return Collections.unmodifiableSet(deptIds);
    }

    public void addDeptIds(Set<Long> ids) {
        if (ids != null) {
            deptIds.addAll(ids);
        }
    }
}
