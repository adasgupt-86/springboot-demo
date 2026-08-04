package com.devops.demo.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.SpringVersion;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.net.InetAddress;
import java.time.LocalDateTime;

@Controller
public class HomeController {

    @Value("${app.environment:LOCAL}")
    private String environment;

    @GetMapping("/")
    public String home(Model model) throws Exception {

        InetAddress inetAddress = InetAddress.getLocalHost();

        model.addAttribute("appName", "Enterprise DevOps Demo");
        model.addAttribute("version", "v2.0.0");
        model.addAttribute("environment", environment);

        model.addAttribute("hostname", inetAddress.getHostName());
        model.addAttribute("ip", inetAddress.getHostAddress());

        model.addAttribute("javaVersion",
                System.getProperty("java.version"));
       
        model.addAttribute("springVersion",
                SpringVersion.getVersion());

        model.addAttribute("time",
                LocalDateTime.now());

        model.addAttribute("status", "HEALTHY");

        return "index";
    }
}
