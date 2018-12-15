package com.hoyan.services.servivesImpl;

import com.hoyan.dto.UserDTO;
import com.hoyan.entity.Role;
import com.hoyan.entity.UserInfo;
import com.hoyan.repository.RoleRepository;
import com.hoyan.repository.UserRepository;
import com.hoyan.services.IUserInfoService;
import com.hoyan.services.ServiceResult;
import com.hoyan.utils.LoginUserUtil;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.encoding.Md5PasswordEncoder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.expression.Lists;

import javax.xml.crypto.Data;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * Created by 20160709 on 2018/12/8.
 */

@Service
public class UserInfoServiceImpl implements IUserInfoService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private final Md5PasswordEncoder passwordEncoder = new Md5PasswordEncoder();

    @Autowired
    private ModelMapper modelMapper;

    @Override
    public UserInfo findUserInfoByName(String userName) {
        UserInfo userInfo = userRepository.findUserInfoByName(userName);
        if (Objects.isNull(userInfo)) {
            return null;
        }
        List<Role> roles = roleRepository.findByUserId(userInfo.getId());
        if (roles == null || roles.isEmpty()) {
            throw new DisabledException("非法权限!!!!");
        }else {
            List<GrantedAuthority> authorities = com.google.common.collect.Lists.newArrayList();
            roles.forEach(ro->authorities.add(new SimpleGrantedAuthority("ROLE_"+ro.getName())));
            userInfo.setAuthorityList(authorities);
        }
        return userInfo;
    }

    @Override
    public ServiceResult<UserDTO> findById(Long userId) {
        UserInfo user = userRepository.findOne(userId);
        if (user == null) {
            return ServiceResult.notFound();
        }
        UserDTO userDTO = modelMapper.map(user, UserDTO.class);
        return ServiceResult.of(userDTO);
    }

    @Override
    public UserInfo findUserByTelephone(String telephone) {
        UserInfo user = userRepository.findByPhoneNumber(telephone);
        if(user==null){
            return null;
        }
        List<Role> roles = roleRepository.findByUserId(user.getId());
        if(roles==null || roles.isEmpty()){
            throw new DisabledException("权限不正常");
        }
        List<GrantedAuthority> authorities = com.google.common.collect.Lists.newArrayList();
        roles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_"+role.getName())));
        user.setAuthorityList(authorities);
        return user;
    }

    @Override
    @Transactional
    public UserInfo addUserByPhone(String telephone) {
        UserInfo userInfo =new UserInfo();
        userInfo.setPhoneNumber(telephone);
        userInfo.setName(telephone.substring(0,3)+"****"+telephone.substring(7,telephone.length()));
        Date now =new Date();
        userInfo.setCreateTime(now);
        userInfo.setLastLoginTime(now);
        userInfo.setLastUpdateTime(now);
        userInfo = userRepository.save(userInfo);
        Role role =new Role();
        role.setName("USER");
        role.setUserId(userInfo.getId());
        roleRepository.save(role);
        userInfo.setAuthorityList(com.google.common.collect.Lists.newArrayList(new SimpleGrantedAuthority("ROLE_USER")));
        return userInfo;
    }

    @Override
    @Transactional
    public ServiceResult modifyUserProfile(String profile, String value) {
        Long userId = LoginUserUtil.getLoginUserId();
        if (profile == null || profile.isEmpty()) {
            return new ServiceResult(false, "属性不可以为空");
        }
        switch (profile) {
            case "name":
                userRepository.updateUsername(userId, value);
                break;
            case "email":
                userRepository.updateEmail(userId, value);
                break;
            case "password":
                userRepository.updatePassword(userId, this.passwordEncoder.encodePassword(value, userId));
                break;
            default:
                return new ServiceResult(false, "不支持的属性 非法操作!");
        }
        return ServiceResult.success();
    }


}
