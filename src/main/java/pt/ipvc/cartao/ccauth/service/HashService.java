package pt.ipvc.cartao.ccauth.service;

import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class HashService {

    public byte[] generatePkcs1Hash(byte[] pdfBytes) throws NoSuchAlgorithmException {
        byte[] prefix = hexToBytes("3031300d060960864801650304020105000420");

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(pdfBytes);

        byte[] result = new byte[prefix.length + hash.length];
        System.arraycopy(prefix, 0, result, 0, prefix.length);
        System.arraycopy(hash, 0, result, prefix.length, hash.length);

        return result;
    }

    private byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] out = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            out[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return out;
    }
}