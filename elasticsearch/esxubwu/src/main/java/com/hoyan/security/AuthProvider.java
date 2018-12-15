package com.hoyan.security;

import com.hoyan.entity.UserInfo;
import com.hoyan.services.IUserInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.encoding.Md5PasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.www.NonceExpiredException;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Created by 20160709 on 2018/12/8.
 */
public class AuthProvider implements AuthenticationProvider {

    @Autowired
    private IUserInfoService userInfoService;

    private  final Md5PasswordEncoder passwordEncoder =new Md5PasswordEncoder();

    @Override
    @ResponseStatus(code=HttpStatus.NOT_IMPLEMENTED,reason="111")
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String userName =authentication.getName();
        String inputPassword = (String) authentication.getCredentials();
        UserInfo userInfo = userInfoService.findUserInfoByName(userName);
        Assert.isTrue(userInfo!=null,"authFail");
        if(this.passwordEncoder.isPasswordValid(userInfo.getPassword(),inputPassword,userInfo.getId())){
            return new UsernamePasswordAuthenticationToken(userInfo,null,userInfo.getAuthorities());
        }
        throw new NonceExpiredException("authError");
    }

    @Override
    public boolean supports(Class<?> aClass) {
        return true;
    }
}
