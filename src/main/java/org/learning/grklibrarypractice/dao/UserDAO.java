package org.learning.grklibrarypractice.dao;

import org.learning.grklibrarypractice.entity.Users;

public interface UserDAO {

    Users findByUsername(String username);
    Users save(Users user);
    void deleteUserById(int id);
}
