package com.example.bankcards.util;

import com.example.bankcards.config.CardEncryptionProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CardNumberCodecTest {

    private final CardNumberCodec cardNumberCodec = createCodec();

    @Test
    void encryptDecryptAndMaskShouldWorkTogether() {
        String cardNumber = "4000001234567899";

        String encrypted = cardNumberCodec.encrypt(cardNumber);
        String decrypted = cardNumberCodec.decrypt(encrypted);
        String masked = cardNumberCodec.mask(decrypted);

        assertThat(encrypted).isNotBlank();
        assertThat(decrypted).isEqualTo(cardNumber);
        assertThat(masked).isEqualTo("**** **** **** 7899");
    }

    @Test
    void generateCardNumberShouldProduceLuhnCompatibleValue() {
        String cardNumber = cardNumberCodec.generateCardNumber();

        assertThat(cardNumber).matches("\\d{16}");
        assertThat(isLuhnValid(cardNumber)).isTrue();
    }

    private CardNumberCodec createCodec() {
        CardEncryptionProperties properties = new CardEncryptionProperties();
        properties.setSecret("test-card-encryption-secret");
        return new CardNumberCodec(properties);
    }

    private boolean isLuhnValid(String cardNumber) {
        int sum = 0;
        boolean doubleDigit = false;

        for (int index = cardNumber.length() - 1; index >= 0; index--) {
            int digit = cardNumber.charAt(index) - '0';

            if (doubleDigit) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }

            sum += digit;
            doubleDigit = !doubleDigit;
        }

        return sum % 10 == 0;
    }
}
