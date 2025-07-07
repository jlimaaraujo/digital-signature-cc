package pt.ipvc.cartao.ccauth.model;

public class OtpRequest {
    private String otp;
    private String documentName;
    private boolean hasVisualSignature;
    private int signaturePage = 1;
    private float signatureXPercent = 50.0f;
    private float signatureYPercent = 50.0f;

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

    public String getDocumentName() {
        return documentName;
    }

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }

    public boolean isHasVisualSignature() {
        return hasVisualSignature;
    }

    public void setHasVisualSignature(boolean hasVisualSignature) {
        this.hasVisualSignature = hasVisualSignature;
    }

    public int getSignaturePage() {
        return signaturePage;
    }

    public void setSignaturePage(int signaturePage) {
        this.signaturePage = signaturePage;
    }

    public float getSignatureXPercent() {
        return signatureXPercent;
    }

    public void setSignatureXPercent(float signatureXPercent) {
        this.signatureXPercent = signatureXPercent;
    }

    public float getSignatureYPercent() {
        return signatureYPercent;
    }

    public void setSignatureYPercent(float signatureYPercent) {
        this.signatureYPercent = signatureYPercent;
    }
}
