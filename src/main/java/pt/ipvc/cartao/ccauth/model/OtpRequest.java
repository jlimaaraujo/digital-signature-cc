package pt.ipvc.cartao.ccauth.model;

public class OtpRequest {
    private String otp;
    private String documentName;
    private boolean hasVisualSignature;

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
}
