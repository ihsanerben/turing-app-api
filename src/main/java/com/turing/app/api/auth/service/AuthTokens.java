package com.turing.app.api.auth.service;

import com.turing.app.api.user.entity.User;

public record AuthTokens(String accessToken, String refreshToken, User user) {}
