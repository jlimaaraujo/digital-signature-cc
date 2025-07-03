package pt.ipvc.cartao.ccauth.model;

public class SignResult {
    private String assinaturaBase64;
    private String certBase64;
    private byte[] signedPdfBytes;

    public SignResult(String assinaturaBase64, String certBase64) {
        this.assinaturaBase64 = assinaturaBase64;
        this.certBase64 = certBase64;
    }

    public SignResult(String assinaturaBase64, String certBase64, byte[] signedPdfBytes) {
        this.assinaturaBase64 = assinaturaBase64;
        this.certBase64 = certBase64;
        this.signedPdfBytes = signedPdfBytes;
    }

    // Getters e setters
    public String getAssinaturaBase64() { return assinaturaBase64; }
    public void setAssinaturaBase64(String assinaturaBase64) { this.assinaturaBase64 = assinaturaBase64; }

    public String getCertBase64() { return certBase64; }
    public void setCertBase64(String certBase64) { this.certBase64 = certBase64; }

    public byte[] getSignedPdfBytes() { return signedPdfBytes; }
    public void setSignedPdfBytes(byte[] signedPdfBytes) { this.signedPdfBytes = signedPdfBytes; }
}