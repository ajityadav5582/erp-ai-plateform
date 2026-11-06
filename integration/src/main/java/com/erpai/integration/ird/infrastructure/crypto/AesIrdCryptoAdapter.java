package com.erpai.integration.ird.infrastructure.crypto;

import com.erpai.integration.ird.port.out.IrdCryptoPort;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * AES-based implementation of {@link IrdCryptoPort}.
 * Uses a fixed dev key; replace with a KMS-backed key in production.
 */
@Component
public class AesIrdCryptoAdapter implements IrdCryptoPort {

    private static final String ALGORITHM = "AES";
    private static final byte[] KEY = "ErpAiIrdKey12345".getBytes(StandardCharsets.UTF_8); // 16 bytes

    @Override
    public String encrypt(String plainText) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(KEY, ALGORITHM));
            return Base64.getEncoder().encodeToString(cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Encryption failed", e);
        }
    }

    @Override
    public String decrypt(String cipherText) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(KEY, ALGORITHM));
            return new String(cipher.doFinal(Base64.getDecoder().decode(cipherText)), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Decryption failed", e);
        }
    }
}
