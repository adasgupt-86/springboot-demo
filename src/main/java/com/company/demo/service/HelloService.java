package com.company.demo.service;

import com.company.demo.model.Message;
import org.springframework.stereotype.Service;

@Service
public class HelloService {

    public Message getMessage() {

        return new Message("Welcome to Enterprise DevOps Pipeline!");

    }

}
