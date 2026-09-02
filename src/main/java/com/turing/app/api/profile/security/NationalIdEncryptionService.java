package com.turing.app.api.profile.security;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class NationalIdEncryptionService {
  private static final int IV_LENGTH = 12;
  private final SecretKeySpec key;
  private final SecureRandom random = new SecureRandom();

  public NationalIdEncryptionService(@Value("${app.profile.encryption-key}") String encodedKey) {
    byte[] decoded = Base64.getDecoder().decode(encodedKey);
    if (decoded.length != 32)
      throw new IllegalStateException("PROFILE_ENCRYPTION_KEY must decode to 32 bytes");
    key = new SecretKeySpec(decoded, "AES");
  }

  public byte[] encrypt(String value) {
    if (value == null || value.isBlank()) return null;
    try {
      byte[] iv = new byte[IV_LENGTH];
      random.nextBytes(iv);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
      byte[] encrypted = cipher.doFinal(value.trim().getBytes(StandardCharsets.UTF_8));
      return ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted).array();
    } catch (Exception exception) {
      throw new IllegalStateException("National ID encryption failed", exception);
    }
  }

  public String decrypt(byte[] value) {
    if (value == null) return null;
    try {
      ByteBuffer buffer = ByteBuffer.wrap(value);
      byte[] iv = new byte[IV_LENGTH];
      buffer.get(iv);
      byte[] encrypted = new byte[buffer.remaining()];
      buffer.get(encrypted);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
      return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
    } catch (Exception exception) {
      throw new IllegalStateException("National ID decryption failed", exception);
    }
  }
}
