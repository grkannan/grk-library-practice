package org.learning.grklibrarypractice.controller;

import org.learning.grklibrarypractice.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/")
    public String index(){
        return "index"; // d
    }

    @GetMapping("/login")
    public String login(Model model) {
        return "login";
    }
}
