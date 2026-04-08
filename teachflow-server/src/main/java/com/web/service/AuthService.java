package com.web.service;

import com.web.dto.AuthRequest;
import com.web.model.User;
import com.web.repository.UserRepository;
import com.web.util.JwtUtil;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service // 告诉 Spring 这是一个负责业务逻辑的组件
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    // 构造函数注入依赖
    public AuthService(UserRepository userRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    /**
     * 注册业务逻辑
     */
    public void register(AuthRequest request) {
        // 1. 查数据库，看用户名是不是被占用了
        if (userRepository.findByUsername(request.getUsername()) != null) {
            throw new RuntimeException("用户名已存在");
        }

        // 2. 加密密码
        String hashedPassword = BCrypt.hashpw(request.getPassword(), BCrypt.gensalt());

        // 3. 构建用户实体并存入数据库
        User newUser = new User();
        newUser.setUsername(request.getUsername());
        newUser.setPassword(hashedPassword);
        newUser.setCreatedAt(LocalDateTime.now());

        userRepository.save(newUser);
    }

    /**
     * 登录业务逻辑
     */
    public String login(AuthRequest request) {
        // 1. 查数据库找人
        User user = userRepository.findByUsername(request.getUsername());
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 2. 比对密文密码
        if (!BCrypt.checkpw(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("密码错误");
        }

        // 3. 校验全部通过，找 JwtUtil 要一个 Token 并返回
        return jwtUtil.generateToken(user.getUsername());
    }
}