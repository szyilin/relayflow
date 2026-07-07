package com.relayflow.common.pojo;

import lombok.Data;

import java.util.Collections;
import java.util.List;

@Data
public class PageResult<T> {

    private List<T> list;
    /** 分页总数；使用 int 避免 Long 全局序列化为字符串（雪花 ID 专用策略不适用于计数）。 */
    private int total;

    public static <T> PageResult<T> of(List<T> list, long total) {
        PageResult<T> result = new PageResult<>();
        result.list = list;
        result.total = total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
        return result;
    }

    public static <T> PageResult<T> empty() {
        return of(Collections.emptyList(), 0L);
    }
}
