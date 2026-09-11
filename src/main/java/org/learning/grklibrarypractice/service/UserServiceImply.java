package org.learning.grklibrarypractice.service;

import org.learning.grklibrarypractice.dao.UserDAO;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImply implements  UserService {

    private UserDAO userDAO;

    public UserServiceImply(UserDAO userDAO) {
        this.userDAO = userDAO;
    }


}
