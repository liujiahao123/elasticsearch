package com.hoyan.services;

/**
 * Created by 20160709 on 2018/12/14.
 */
public interface ISmsService {
    /**
     * 发送验证码到指定手机号码 并缓存验证码10分钟 请求间隔时间1分钟
     */
      ServiceResult<String> sendSms(String phone);

    /**
     * 获取缓存中的验证码
     */

    String getSmsCode(String phone);


    /**
     * 移除指定手机号的验证码缓存
     */

    void remove(String phone);


}
