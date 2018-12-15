package com.hoyan.services;

import lombok.Data;

import java.util.List;

/**
 * Created by 20160709 on 2018/12/9.
 */
@Data
public class ServiceMuleiResult<T> {

    private long total;

    private List<T> result;

    public ServiceMuleiResult(long total, List<T> result) {
        this.total = total;
        this.result = result;
    }
    public int getResultSize(){
        if(this.result ==null){
            return 0;
        }
        return result.size();
    }


}
