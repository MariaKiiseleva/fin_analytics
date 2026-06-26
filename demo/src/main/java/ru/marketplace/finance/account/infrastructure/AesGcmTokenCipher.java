package ru.marketplace.finance.account.infrastructure;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.marketplace.finance.account.application.TokenCipher;

@Component
public class AesGcmTokenCipher implements TokenCipher {

	private static final String PREFIX = "v1:";
	private static final int IV_SIZE_BYTES = 12;
	private static final int TAG_SIZE_BITS = 128;
	private static final String DEV_SECRET = "local-dev-token-encryption-key";

	private final SecureRandom secureRandom = new SecureRandom();
	private final SecretKeySpec secretKey;

	public AesGcmTokenCipher(@Value("${app.security.token-encryption-key:${WB_TOKEN_ENCRYPTION_KEY:}}") String secret) {
		this.secretKey = new SecretKeySpec(sha256(normalizeSecret(secret)), "AES");
	}

	@Override
	public String encrypt(String plainText) {
		if (plainText == null || plainText.isBlank()) {
			throw new IllegalArgumentException("plainText must not be blank");
		}
		byte[] iv = new byte[IV_SIZE_BYTES];
		secureRandom.nextBytes(iv);
		byte[] encrypted = crypt(Cipher.ENCRYPT_MODE, iv, plainText.getBytes(StandardCharsets.UTF_8));
		ByteBuffer payload = ByteBuffer.allocate(iv.length + encrypted.length);
		payload.put(iv);
		payload.put(encrypted);
		return PREFIX + Base64.getEncoder().encodeToString(payload.array());
	}

	@Override
	public String decrypt(String encryptedText) {
		if (encryptedText == null || encryptedText.isBlank()) {
			throw new IllegalArgumentException("encryptedText must not be blank");
		}
		if (!encryptedText.startsWith(PREFIX)) {
			throw new IllegalArgumentException("Unsupported encrypted token format");
		}
		byte[] payload = Base64.getDecoder().decode(encryptedText.substring(PREFIX.length()));
		if (payload.length <= IV_SIZE_BYTES) {
			throw new IllegalArgumentException("Encrypted token payload is too short");
		}
		ByteBuffer buffer = ByteBuffer.wrap(payload);
		byte[] iv = new byte[IV_SIZE_BYTES];
		buffer.get(iv);
		byte[] encrypted = new byte[buffer.remaining()];
		buffer.get(encrypted);
		return new String(crypt(Cipher.DECRYPT_MODE, iv, encrypted), StandardCharsets.UTF_8);
	}

	private byte[] crypt(int mode, byte[] iv, byte[] input) {
		try {
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(mode, secretKey, new GCMParameterSpec(TAG_SIZE_BITS, iv));
			return cipher.doFinal(input);
		}
		catch (GeneralSecurityException exception) {
			throw new IllegalStateException(errorMessage(mode), exception);
		}
	}

	private static String errorMessage(int mode) {
		return mode == Cipher.DECRYPT_MODE ? "Token decryption failed" : "Token encryption failed";
	}

	private static byte[] sha256(String value) {
		try {
			return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
		}
		catch (GeneralSecurityException exception) {
			throw new IllegalStateException("SHA-256 is not available", exception);
		}
	}

	private static String normalizeSecret(String secret) {
		return secret == null || secret.isBlank() ? DEV_SECRET : secret;
	}
}
