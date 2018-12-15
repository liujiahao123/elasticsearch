package com.hoyan.services;

import com.hoyan.dto.UserDTO;
import com.hoyan.entity.UserInfo;

/**
 * Created by 20160709 on 2018/12/8. 用户服务
 */
public interface IUserInfoService {
    UserInfo findUserInfoByName(String userName);

    ServiceResult<UserDTO> findById(Long adminId);

    /*根据手机号查找用户是否存在*/
    UserInfo findUserByTelephone(String telephone);

    /*根据手机号直接注册用户*/
    UserInfo addUserByPhone(String telephone);

    /**
     * 修改指定属性值
     * @param profile
     * @param value
     * @return
     */
    ServiceResult modifyUserProfile(String profile, String value);

}
