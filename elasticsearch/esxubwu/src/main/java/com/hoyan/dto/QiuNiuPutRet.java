package com.hoyan.dto;

import com.qiniu.util.StringMap;
import lombok.ToString;

/**
 * Created by 20160709 on 2018/12/9.
 */
@ToString
public final class QiuNiuPutRet {
    /*返回结果集工具类*/

    public String key;

    public String hash;

    public String bucket;

    public int width;

    public int height;


}
