package com.web.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    // 密钥 (防伪钢印的模具)。
    // 注意：实际企业级开发中，这段字符串会放在 application.yml 里，防止硬编码泄露。
    // 为了满足 HS256 算法的安全性要求，密钥长度必须足够长（至少256位）。
    private static final String SECRET_KEY = "TeachFlow-Super-Secret-Key-For-Jwt-Authentication-Must-Be-Long-Enough";

    // Token 过期时间，这里设置为 24 小时
    private static final long EXPIRATION_TIME = 1000 * 60 * 60 * 24;

    // 生成加密 Key 对象
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }

    /**
     * 1. 签发 Token (给登录成功的用户发胸牌)
     */
    public String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username) // 核心信息：把用户名存进去
                .setIssuedAt(new Date(System.currentTimeMillis())) // 签发时间
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME)) // 过期时间
                .signWith(getSigningKey(), SignatureAlgorithm.HS256) // 盖上防伪钢印 (使用 HS256 算法)
                .compact(); // 压扁成一个字符串
    }

    /**
     * 2. 解析 Token (从胸牌里读出用户名)
     */
    public String extractUsername(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey()) // 必须用我们自己的模具才能解开
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    /**
     * 3. 校验 Token 的有效性
     */
    public boolean isTokenValid(String token) {
        try {
            // 如果 Token 被篡改过（哪怕改了一个字母），或者已经过了 24 小时过期时间
            // 这里解析时就会直接抛出异常
            Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}