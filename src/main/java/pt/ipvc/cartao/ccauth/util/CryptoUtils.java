package pt.ipvc.cartao.ccauth.util;

import java.io.InputStream;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.xml.bind.DatatypeConverter;

public class CryptoUtils {

    public static String encrypt(String plainText, String certPath) {
        try {
            PublicKey publicKey = loadPublicKeyFromCert(certPath);
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encrypted = cipher.doFinal(plainText.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao cifrar com certificado AMA", e);
        }
    }

    public static PublicKey loadPublicKeyFromCert(String certFileName) throws Exception {
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(certFileName)) {
            if (is == null) {
                throw new java.io.FileNotFoundException("Certificado não encontrado no classpath: " + certFileName);
            }
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            Certificate cert = factory.generateCertificate(is);
            return cert.getPublicKey();
        }
    }

    public static boolean verifySignature(byte[] content, byte[] signature, byte[] certBytes) {
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) cf.generateCertificate(new java.io.ByteArrayInputStream(certBytes));
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(cert.getPublicKey());
            sig.update(content);
            return sig.verify(signature);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
