package pt.ipvc.cartao.ccauth.service;

import io.github.cdimascio.dotenv.Dotenv;

import javax.xml.bind.JAXBElement;
import pt.ipvc.cartao.ccauth.model.SignResult;
import pt.ipvc.cartao.ccauth.soap.*;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import java.util.Base64;

public class SoapClientService {

    private static final Dotenv dotenv = Dotenv.load();
    private static final String WSDL_URL = dotenv.get("WSDL_URL");
    private static final String USERNAME = dotenv.get("USERNAME_");
    private static final String PASSWORD = dotenv.get("PASSWORD");

    private static SCMDService port;

    public static SCMDService getPort() {
        return port;
    }

    static {
        try {
            SCMDService_Service service = new SCMDService_Service();
            port = service.getBasicHttpBindingSCMDService();

            // Autenticação básica
            BindingProvider bp = (BindingProvider) port;
            bp.getRequestContext().put(BindingProvider.USERNAME_PROPERTY, USERNAME);
            bp.getRequestContext().put(BindingProvider.PASSWORD_PROPERTY, PASSWORD);

            // Timeout (opcional)
            bp.getRequestContext().put("com.sun.xml.internal.ws.connect.timeout", 10000);
            bp.getRequestContext().put("com.sun.xml.internal.ws.request.timeout", 10000);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getCertificate(byte[] applicationId, String encryptedUserId) {
        return port.getCertificate(applicationId, encryptedUserId);
    }

    public static String sign(byte[] applicationId, String docName, byte[] hash, String encryptedUserId, String encryptedPin) {
        SignRequest request = new SignRequest();
        request.setApplicationId(applicationId);
        request.setDocName(docName);
        request.setHash(hash);
        request.setUserId(encryptedUserId);
        request.setPin(encryptedPin);

        SignStatus status = port.scmdSign(request);
        return status.getProcessId();
    }

    public static SignResult validateOtp(String encryptedOtp, String processId, byte[] applicationId) {
        System.out.println("[SOAP Client] Encrypted OTP: " + encryptedOtp);
        System.out.println("[SOAP Client] Process ID: " + processId);
        System.out.println("[SOAP Client] Application ID: " + (applicationId != null ? applicationId.length : "null"));

        SignResponse response = port.validateOtp(encryptedOtp, processId, applicationId, false);

        if (response == null) {
            System.out.println("[SOAP Client] Response is null");
            throw new IllegalArgumentException("Response is null");
        }

        if (response.getSignature() == null) {
            throw new IllegalArgumentException("Signature is null");
        }

        String assinaturaBase64 = Base64.getEncoder().encodeToString(response.getSignature());
        String certBase64 = response.getCertificate();

        return new SignResult(assinaturaBase64, certBase64);
    }

    public static pt.ipvc.cartao.ccauth.soap.SignStatus forceSms(String processId, String encryptedCitizenId, byte[] applicationId) {
        pt.ipvc.cartao.ccauth.soap.SignStatus result = port.forceSMS(processId, encryptedCitizenId, applicationId);
        
        return result;
    }
}