package org.learning.grklibrarypractice.entity;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name="users")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class Users {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    @Column(name="id")
    private int id;

    @Column(name="full_name", nullable=false, length=100)
    private String fullName;

    @Column(name="username", unique = true, nullable = false,  length=50)
    private String username;

    @Column(name="password", nullable = false, length=50)
    private String password;

    @Column(name="dob", nullable = false)
    private LocalDate dob;

    @Column(name="email", unique = true, nullable = false, length=50)
    private String email;

    @Column(name="door_no", nullable = true)
    private String doorNo;

    @Column(name="street", nullable=false)
    private String street;

    @Column(name="area", nullable=false)
    private String area;

    @Column(name="city", nullable=false)
    private String city;

    @Column(name="state", nullable=false)
    private String state;

    @Column(name="country", nullable = false)
    private String country;

    @Column(name="is_active", nullable = false)
    private boolean isActive = true;

    @Column(name="last_login")
    private LocalDateTime lastLogin;
}
