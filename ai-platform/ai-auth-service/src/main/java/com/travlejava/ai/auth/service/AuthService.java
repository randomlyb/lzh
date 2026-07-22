package com.travlejava.ai.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.travlejava.ai.auth.mapper.UserMapper;
import com.travlejava.ai.auth.security.JwtTokenProvider;
import com.travlejava.ai.pojo.auth.User;
import com.travlejava.ai.pojo.auth.dto.AuthResponse;
import com.travlejava.ai.pojo.auth.dto.LoginRequest;
import com.travlejava.ai.pojo.auth.dto.RegisterRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    public AuthService(AuthenticationManager authenticationManager, JwtTokenProvider tokenProvider, PasswordEncoder passwordEncoder, UserMapper userMapper) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
    }

    public AuthResponse login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = tokenProvider.generateToken(authentication);
        // In a real application, you might generate a refresh token here as well
        String refreshToken = "mockRefreshToken"; // Placeholder

        User user = userMapper.selectOne(new QueryWrapper<User>().eq("username", loginRequest.getUsername()));

        return new AuthResponse(jwt, refreshToken, user.getId(), user.getUsername());
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
}
