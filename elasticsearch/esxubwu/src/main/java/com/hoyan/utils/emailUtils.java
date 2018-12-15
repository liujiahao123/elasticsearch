package com.hoyan.utils;

import org.apache.lucene.search.BooleanQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Created by 20160709 on 2018/12/11.
 */
public class emailUtils {

    @Autowired
    private JavaMailSender mailSender;

    public  Boolean send(String content,int type){
        SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
        simpleMailMessage.setFrom("liujiahao_admin@163.com");
        simpleMailMessage.setTo("2360072777@qq.com");
        if(type ==0){
            simpleMailMessage.setSubject("主题：发布房源");
        }else {
            simpleMailMessage.setSubject("主题：删除房源");
        }
        simpleMailMessage.setText(content);
        mailSender.send(simpleMailMessage);
        return true;
    }

}
