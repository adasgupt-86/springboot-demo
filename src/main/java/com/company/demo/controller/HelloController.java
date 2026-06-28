package com.company.demo.controller;

import com.company.demo.model.Message;
import com.company.demo.service.HelloService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class HelloController {

    private final HelloService helloService;

    public HelloController(HelloService helloService) {
        this.helloService = helloService;
    }

    @GetMapping("/hello")
    public Message hello() {
        return helloService.getMessage();
    }

    @GetMapping("/health")
    public String health() {
        return "Application is UP";
    }

}
