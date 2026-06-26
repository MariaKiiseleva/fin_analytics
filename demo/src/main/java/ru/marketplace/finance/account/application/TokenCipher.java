package ru.marketplace.finance.account.application;

public interface TokenCipher {

	String encrypt(String plainText);

	String decrypt(String encryptedText);
}
