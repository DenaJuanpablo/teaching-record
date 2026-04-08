package com.web.repository;

import com.web.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    // Spring Data JPA 会自动解析这个方法名，生成类似：
    // SELECT * FROM users WHERE username = ?
    User findByUsername(String username);
}