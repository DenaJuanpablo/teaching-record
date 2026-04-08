package com.web.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data // 自动生成 Getter、Setter、toString、equals 和 hashCode
@NoArgsConstructor // 自动生成无参构造函数（JPA 必须）
@AllArgsConstructor // 自动生成全参构造函数
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    // 存加密后的密文
    @Column(nullable = false, length = 100)
    private String password;

    @Column(nullable = false)
    private LocalDateTime createdAt;

}