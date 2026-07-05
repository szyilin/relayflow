package com.relayflow.common.pojo;

import lombok.Data;

import java.util.Collections;
import java.util.List;

@Data
public class PageResult<T> {

    private List<T> list;
    private Long total;

    public static <T> PageResult<T> of(List<T> list, long total) {
        PageResult<T> result = new PageResult<>();
        result.list = list;
        result.total = total;
        return result;
    }

    public static <T> PageResult<T> empty() {
        return of(Collections.emptyList(), 0L);
    }
}
