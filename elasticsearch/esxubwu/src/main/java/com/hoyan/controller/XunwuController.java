package com.hoyan.controller;

import com.hoyan.utils.Result;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by 20160709 on 2018/12/6.
 */
@Controller
public class XunwuController {

    @GetMapping("/hello")
    @ResponseBody
    public Result<String> xunwu(){
        return Result.success("hello xunwu");
    }

    @GetMapping("/index")
    public String index(Model model){
        model.addAttribute("name","ljh");
        return "index";
    }

    @GetMapping("/logout/page")
    public String logout(){
        return "logout";
    }

}
