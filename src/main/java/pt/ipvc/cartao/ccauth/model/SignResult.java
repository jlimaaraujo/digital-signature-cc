package pt.ipvc.cartao.ccauth.model;

public class SignResult {
    private String signatureBase64;
    private String certBase64;
    private byte[] signedPdfBytes;

    public SignResult(String signatureBase64, String certBase64) {
        this.signatureBase64 = signatureBase64;
        this.certBase64 = certBase64;
    }

    public SignResult(String signatureBase64, String certBase64, byte[] signedPdfBytes) {
        this.signatureBase64 = signatureBase64;
        this.certBase64 = certBase64;
        this.signedPdfBytes = signedPdfBytes;
    }

    // Getters e setters
    public String getSignatureBase64() { return signatureBase64; }
    public void setSignatureBase64(String signatureBase64) { this.signatureBase64 = signatureBase64; }

    public String getCertBase64() { return certBase64; }
    public void setCertBase64(String certBase64) { this.certBase64 = certBase64; }

    public byte[] getSignedPdfBytes() { return signedPdfBytes; }
    public void setSignedPdfBytes(byte[] signedPdfBytes) { this.signedPdfBytes = signedPdfBytes; }
}