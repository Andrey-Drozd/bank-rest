package com.example.bankcards.util;

import com.example.bankcards.config.CardEncryptionProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class CardNumberCodec {

    private static final String AES_ALGORITHM = "AES";
    private static final String AES_GCM_ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH = 128;
    private static final int CARD_LENGTH = 16;
    private static final int CARD_PREFIX = 400000;

    private final CardEncryptionProperties properties;
    private final SecureRandom secureRandom = createSecureRandom();

    public String generateCardNumber() {
        String payload = CARD_PREFIX + randomDigits(9);
        int checkDigit = calculateLuhnCheckDigit(payload);
        return payload + checkDigit;
    }

    public String encrypt(String cardNumber) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AES_GCM_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(), new GCMParameterSpec(TAG_LENGTH, iv));

            byte[] encrypted = cipher.doFinal(cardNumber.getBytes(StandardCharsets.UTF_8));
            byte[] payload = ByteBuffer.allocate(iv.length + encrypted.length)
                    .put(iv)
                    .put(encrypted)
                    .array();

            return Base64.getEncoder().encodeToString(payload);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to encrypt card number", exception);
        }
    }

    public String decrypt(String encryptedCardNumber) {
        try {
            byte[] payload = Base64.getDecoder().decode(encryptedCardNumber);
            byte[] iv = new byte[IV_LENGTH];
            byte[] encrypted = new byte[payload.length - IV_LENGTH];

            System.arraycopy(payload, 0, iv, 0, IV_LENGTH);
            System.arraycopy(payload, IV_LENGTH, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance(AES_GCM_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), new GCMParameterSpec(TAG_LENGTH, iv));

            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to decrypt card number", exception);
        }
    }

    public String hash(String cardNumber) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(deriveKeyMaterial("hash"), "HmacSHA256"));
            byte[] hash = mac.doFinal(cardNumber.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                hex.append(String.format("%02x", value));
            }
            return hex.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to hash card number", exception);
        }
    }

    public String mask(String cardNumber) {
        if (cardNumber == null || cardNumber.length() != CARD_LENGTH) {
            throw new IllegalArgumentException("Card number must contain 16 digits");
        }

        return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
    }

    private SecretKey getSecretKey() {
        return new SecretKeySpec(deriveKeyMaterial("enc"), AES_ALGORITHM);
    }

    private static SecureRandom createSecureRandom() {
        try {
            return SecureRandom.getInstanceStrong();
        } catch (Exception ignored) {
            return new SecureRandom();
        }
    }

    private String randomDigits(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int index = 0; index < length; index++) {
            builder.append(secureRandom.nextInt(10));
        }
        return builder.toString();
    }

    private int calculateLuhnCheckDigit(String payload) {
        int sum = 0;
        boolean doubleDigit = true;

        for (int index = payload.length() - 1; index >= 0; index--) {
            int digit = payload.charAt(index) - '0';

            if (doubleDigit) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }

            sum += digit;
            doubleDigit = !doubleDigit;
        }

        return (10 - (sum % 10)) % 10;
    }

    private byte[] deriveKeyMaterial(String purpose) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest((properties.getSecret() + ":" + purpose).getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to initialize card encryption key", exception);
        }
    }
}
