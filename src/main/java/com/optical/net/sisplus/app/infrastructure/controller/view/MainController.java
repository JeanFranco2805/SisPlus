package com.optical.net.sisplus.app.infrastructure.controller.view;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
public class MainController {
    @GetMapping("dashboard")
    public String index(){
        return "index";
    }
    @GetMapping("employee")
    public String employee(){
        return "employee";
    }
    @GetMapping("assistance")
    public String assistance(){
        return "assistance";
    }
    @GetMapping("login")
    public String login(){
        return "login";
    }
}
