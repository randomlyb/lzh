package com.travlejava.ai.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.travlejava.ai.auth.mapper.UserMapper;
import com.travlejava.ai.auth.security.JwtTokenProvider;
import com.travlejava.ai.pojo.auth.User;
import com.travlejava.ai.pojo.auth.dto.AuthResponse;
import com.travlejava.ai.pojo.auth.dto.LoginRequest;
import com.travlejava.ai.pojo.auth.dto.RegisterRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private static final String TOKEN_PREFIX = "auth:token:";
    private static final long TOKEN_EXPIRE_HOURS = 24;

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final StringRedisTemplate redisTemplate;

    public AuthService(AuthenticationManager authenticationManager, 
                      JwtTokenProvider tokenProvider, 
                      PasswordEncoder passwordEncoder, 
                      UserMapper userMapper,
                      StringRedisTemplate redisTemplate) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
        this.redisTemplate = redisTemplate;
    }

    public AuthResponse login(LoginRequest loginRequest) {
        logger.info("Login attempt for user: {}", loginRequest.getUsername());
        
        User user = userMapper.selectOne(new QueryWrapper<User>().eq("username", loginRequest.getUsername()));
        if (user == null) {
            logger.warn("User not found: {}", loginRequest.getUsername());
            throw new RuntimeException("用户名或密码错误");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            String jwt = tokenProvider.generateToken(authentication);
            String refreshToken = "mockRefreshToken";

            // 存储Token到Redis,24小时过期
            String tokenKey = TOKEN_PREFIX + user.getId();
            redisTemplate.opsForValue().set(tokenKey, jwt, TOKEN_EXPIRE_HOURS, TimeUnit.HOURS);
            logger.info("Token stored in Redis for user: {}", user.getId());

            return new AuthResponse(null, null, jwt, refreshToken, user.getId(), user.getUsername());
        } catch (org.springframework.security.core.AuthenticationException e) {
            logger.warn("Authentication failed for user: {}", loginRequest.getUsername());
            throw new RuntimeException("用户名或密码错误");
        }
    }

    public User register(RegisterRequest registerRequest) {
        if (userMapper.exists(new QueryWrapper<User>().eq("username", registerRequest.getUsername()))) {
            throw new RuntimeException("Username already taken!");
        }
        if (registerRequest.getEmail() != null && userMapper.exists(new QueryWrapper<User>().eq("email", registerRequest.getEmail()))) {
            throw new RuntimeException("Email already in use!");
        }

        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setEmail(registerRequest.getEmail());
        user.setPhone(registerRequest.getPhone());
        user.setStatus(1); // Default to enabled
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        userMapper.insert(user);
        return user;
    }

    public User getUserInfo(Long userId) {
        return userMapper.selectById(userId);
    }

    public User getUserInfoByUsername(String username) {
        return userMapper.selectOne(new QueryWrapper<User>().eq("username", username));
    }

    public AuthResponse refreshToken(String oldRefreshToken) {
        // This is a placeholder for refresh token logic
        // In a real application, you would validate the old refresh token,
        // generate new access and refresh tokens, and update stored tokens.
        throw new UnsupportedOperationException("Refresh token not implemented yet.");
    }

    /**
     * 验证Token是否有效(从Redis检查)
     */
    public boolean validateToken(Long userId, String token) {
        String tokenKey = TOKEN_PREFIX + userId;
        String storedToken = redisTemplate.opsForValue().get(tokenKey);
        return token.equals(storedToken);
    }

    /**
     * 登出:删除Redis中的Token
     */
    public void logout(Long userId) {
        String tokenKey = TOKEN_PREFIX + userId;
        redisTemplate.delete(tokenKey);
        logger.info("User {} logged out, token removed from Redis", userId);
    }
}