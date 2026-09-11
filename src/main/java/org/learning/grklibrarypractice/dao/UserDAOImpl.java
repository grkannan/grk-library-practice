package org.learning.grklibrarypractice.dao;

import jakarta.persistence.EntityManager;

import org.learning.grklibrarypractice.entity.Users;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class UserDAOImpl implements UserDAO {

    private final EntityManager em;

    @Autowired
    public UserDAOImpl(EntityManager em) {
        this.em = em;
    }


    @Override
    public Users findByUsername(String username) {
        return em.find(Users.class, username);
    }

    @Override
    public Users save(Users user) {
        return em.merge(user);
    }

    @Override
    public void deleteUserById(int id) {
        Users theUser = em.find(Users.class, id);
        em.remove(theUser);
    }
}
