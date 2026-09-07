package com.travlejava.ai.pojo.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    private Integer code;
    private String message;
    private String token;
    private String refreshToken; // Optional: for refresh token mechanism
    private Long userId;
    private String username;
    // Potentially other user details like roles, permissions, etc.
}