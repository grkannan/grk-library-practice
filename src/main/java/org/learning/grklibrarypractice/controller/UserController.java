package org.learning.grklibrarypractice.controller;

import org.learning.grklibrarypractice.service.UserService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/")
    public String index(){
        return "Testing API!";
    }
}
