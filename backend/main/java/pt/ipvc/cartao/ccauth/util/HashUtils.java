package pt.ipvc.cartao.ccauth.util;

import java.security.MessageDigest;

public class HashUtils {

    // Prefixo SHA-256 para PKCS#1 v1.5 (RFC 8017, seção 9.2)
    private static final byte[] SHA256_PREFIX = new byte[] {
            0x30, 0x31, 0x30, 0x0d, 0x06, 0x09,
            0x60, (byte)0x86, 0x48, 0x01, 0x65, 0x03, 0x04, 0x02, 0x01,
            0x05, 0x00, 0x04, 0x20
    };

    public static byte[] generatePkcs1Hash(byte[] pdfContent) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(pdfContent);

            byte[] fullHash = new byte[SHA256_PREFIX.length + hash.length];
            System.arraycopy(SHA256_PREFIX, 0, fullHash, 0, SHA256_PREFIX.length);
            System.arraycopy(hash, 0, fullHash, SHA256_PREFIX.length, hash.length);
            return fullHash;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar hash SHA-256 com prefixo PKCS#1", e);
        }
    }
}
