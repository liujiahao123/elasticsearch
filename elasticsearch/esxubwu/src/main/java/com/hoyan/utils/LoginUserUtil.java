package com.hoyan.utils;

import com.hoyan.entity.UserInfo;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by 20160709 on 2018/12/9.
 */
public  class LoginUserUtil {

    public static UserInfo getUser(){
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal != null && principal instanceof UserInfo) {
            return (UserInfo) principal;
        }
        return null;
    }

    public static Long getLoginUserId() {
        UserInfo user = getUser();
        if (user == null) {
            return -1L;
        }

        return user.getId();
    }


    private static final String PHONE_REGEX = "^((13[0-9])|(14[5|7])|(15([0-3]|[5-9]))|(18[0,5-9]))\\d{8}$";
    private static final Pattern PHONE_PATTERN = Pattern.compile(PHONE_REGEX);

    private static final String EMAIL_REGEX = "^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$";
    private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);

    /**
     * 验证手机号码
     *
     * 移动号码段:139、138、137、136、135、134、150、151、152、157、158、159、182、183、187、188、147
     * 联通号码段:130、131、132、136、185、186、145
     * 电信号码段:133、153、180、189
     *
     * @param target 目标号码
     * @return 如果是手机号码 返回true; 反之,返回false
     */
    public static boolean checkTelephone(String target) {
        return PHONE_PATTERN.matcher(target).matches();
    }

    /**
     * 验证一般的英文邮箱
     * @param target 目标邮箱
     * @return 如果符合邮箱规则 返回true; 反之,返回false
     */
    public static boolean checkEmail(String target) {
        return EMAIL_PATTERN.matcher(target).matches();
    }

}
