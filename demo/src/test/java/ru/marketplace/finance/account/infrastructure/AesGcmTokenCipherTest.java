package ru.marketplace.finance.account.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AesGcmTokenCipherTest {

	@Test
	void encryptsAndDecryptsToken() {
		AesGcmTokenCipher cipher = new AesGcmTokenCipher("test-key");

		String encrypted = cipher.encrypt("wb-token-value");

		assertThat(encrypted).startsWith("v1:");
		assertThat(encrypted).doesNotContain("wb-token-value");
		assertThat(cipher.decrypt(encrypted)).isEqualTo("wb-token-value");
	}

	@Test
	void failsToDecryptWithDifferentKey() {
		AesGcmTokenCipher firstCipher = new AesGcmTokenCipher("first-key");
		AesGcmTokenCipher secondCipher = new AesGcmTokenCipher("second-key");
		String encrypted = firstCipher.encrypt("wb-token-value");

		assertThatThrownBy(() -> secondCipher.decrypt(encrypted))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Token decryption failed");
	}
}
