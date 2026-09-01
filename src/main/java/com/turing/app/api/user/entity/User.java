package com.turing.app.api.user.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {
    @Id private UUID id;
    @Column(nullable = false, length = 320) private String email;
    @Column(name = "password_hash", nullable = false) private String passwordHash;
    @Column(name = "first_name", nullable = false, length = 100) private String firstName;
    @Column(name = "last_name", nullable = false, length = 100) private String lastName;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Role role;
    @Enumerated(EnumType.STRING) @Column(name = "account_status", nullable = false) private AccountStatus accountStatus;
    @Column(name = "email_verified_at") private Instant emailVerifiedAt;
    @Column(name = "last_login_at") private Instant lastLoginAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Version @Column(nullable = false) private long version;

    protected User() {}

    public static User pending(String email, String passwordHash, String firstName, String lastName, Instant now) {
        User user = new User();
        user.id = UUID.randomUUID();
        user.email = email.toLowerCase();
        user.passwordHash = passwordHash;
        user.firstName = firstName;
        user.lastName = lastName;
        user.role = Role.USER;
        user.accountStatus = AccountStatus.PENDING_VERIFICATION;
        user.createdAt = now;
        user.updatedAt = now;
        return user;
    }

    public void verify(Instant now) { accountStatus = AccountStatus.ACTIVE; emailVerifiedAt = now; updatedAt = now; }
    public void login(Instant now) { lastLoginAt = now; updatedAt = now; }
    public void changePassword(String hash, Instant now) { passwordHash = hash; updatedAt = now; }
    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public Role getRole() { return role; }
    public AccountStatus getAccountStatus() { return accountStatus; }
    public Instant getEmailVerifiedAt() { return emailVerifiedAt; }
}
