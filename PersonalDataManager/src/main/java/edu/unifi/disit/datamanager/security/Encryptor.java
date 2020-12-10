package edu.unifi.disit.datamanager.security;

import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Encryptor {

	private static final Logger logger = LogManager.getLogger();

	@Value("${security.encryptor.aes.secretkey}")
	private String secretKey;

	@Value("${security.encryptor.aes.ivparameter}")
	private String ivParameter;

	public String encrypt(String value) {

		String sha256SecretKey = DigestUtils.sha256Hex(secretKey);
		String sha256IvParameter = DigestUtils.sha256Hex(ivParameter);

		IvParameterSpec iv;
		try {
			iv = new IvParameterSpec(sha256IvParameter.substring(0, 16).getBytes(StandardCharsets.UTF_8));

			SecretKeySpec skeySpec = new SecretKeySpec(sha256SecretKey.substring(0, 32).getBytes(StandardCharsets.UTF_8), "AES");
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);
			byte[] encrypted = cipher.doFinal(value.getBytes());

			return Base64.encodeBase64String(Base64.encodeBase64String(encrypted).getBytes());
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException
				| InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
			logger.warn("Hashing Problems", e);
		}
		return null;
	}

	public String decrypt(String encrypted) {
		try {
			IvParameterSpec iv = new IvParameterSpec(ivParameter.getBytes(StandardCharsets.UTF_8));

			SecretKeySpec skeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "AES");
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
			cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
			byte[] original = cipher.doFinal(Base64.decodeBase64(encrypted));

			return new String(original);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException
				| InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
			logger.warn("Dehashing Problems", e);
		}
		return null;
	}
}