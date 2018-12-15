package com.hoyan.utils;

import lombok.Data;

/**
 * Created by 20160709 on 2018/12/9.
 */
@Data
public class ApiDataTablesResult extends ApiResponse {
    private int draw;
    private long recordsTotal;
    private long recordsFiltered;

    public ApiDataTablesResult(ApiResponse.Status status) {
        this(status.getCode(), status.getStandardMessage(),null);
    }

    public ApiDataTablesResult(int code, String message, Object data) {
        super(code, message, data);
    }


}
