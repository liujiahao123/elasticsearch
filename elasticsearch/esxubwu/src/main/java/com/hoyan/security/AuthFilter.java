package com.hoyan.security;

import com.hoyan.entity.UserInfo;
import com.hoyan.services.ISmsService;
import com.hoyan.services.IUserInfoService;
import com.hoyan.utils.LoginUserUtil;
import com.qiniu.util.StringUtils;
import joptsimple.internal.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Objects;

/**
 * Created by 20160709 on 2018/12/14.
 */
public class AuthFilter extends UsernamePasswordAuthenticationFilter {

    @Autowired
    private IUserInfoService userInfoService;

    @Autowired
    private ISmsService smsService;


    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        String name = obtainUsername(request);
        if (!Strings.isNullOrEmpty(name)) {
            request.setAttribute("username", name);
            return super.attemptAuthentication(request, response);
        }
        String telephone = request.getParameter("telephone");
        if (Strings.isNullOrEmpty(telephone) || !LoginUserUtil.checkTelephone(telephone)) {
            throw new RuntimeException("手机号码有误!请核对");
        }
        UserInfo user = userInfoService.findUserByTelephone(telephone);
        String inputCode = request.getParameter("smsCode");
        String sessionCode = smsService.getSmsCode(telephone);
        if(Objects.equals(inputCode,sessionCode)){
            /*如果用户第一次使用时登录 泽自动注册该用户*/
            if (user == null) {
                user = userInfoService.addUserByPhone(telephone);
            }
            return new UsernamePasswordAuthenticationToken(user,null,user.getAuthorities());
        }else{
            throw new BadCredentialsException("smsCodeError");
        }
    }
}
