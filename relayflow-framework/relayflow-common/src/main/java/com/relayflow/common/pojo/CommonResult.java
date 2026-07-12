package com.relayflow.common.pojo;

import com.relayflow.common.exception.ErrorCode;
import com.relayflow.common.exception.ServiceException;
import lombok.Data;

@Data
public class CommonResult<T> {

    private Integer code;
    private String msg;
    private T data;

    public static <T> CommonResult<T> success(T data) {
        CommonResult<T> result = new CommonResult<>();
        result.code = 0;
        result.msg = "success";
        result.data = data;
        return result;
    }

    public static <T> CommonResult<T> error(Integer code, String msg) {
        CommonResult<T> result = new CommonResult<>();
        result.code = code;
        result.msg = msg;
        return result;
    }

    public static <T> CommonResult<T> error(ErrorCode errorCode) {
        return error(errorCode.getCode(), errorCode.getMsg());
    }

    public static <T> CommonResult<T> error(ServiceException exception) {
        CommonResult<T> result = new CommonResult<>();
        result.code = exception.getCode();
        result.msg = exception.getMessage();
        @SuppressWarnings("unchecked")
        T payload = (T) exception.getData();
        result.data = payload;
        return result;
    }
}
