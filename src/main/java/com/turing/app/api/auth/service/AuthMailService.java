package com.turing.app.api.auth.service;

public interface AuthMailService {
    void sendVerification(String email, String token);
    void sendPasswordReset(String email, String token);
}
