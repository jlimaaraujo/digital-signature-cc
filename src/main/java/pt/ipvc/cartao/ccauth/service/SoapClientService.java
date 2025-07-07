package pt.ipvc.cartao.ccauth.service;

import javax.xml.bind.JAXBElement;
import pt.ipvc.cartao.ccauth.model.SignResult;
import pt.ipvc.cartao.ccauth.soap.*;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import java.util.Base64;

public class SoapClientService {

    private static final String WSDL_URL = "https://preprod.cmd.autenticacao.gov.pt/Ama.Authentication.Frontend/SCMDService.svc";
    private static final String USERNAME = "KzMry3YB";
    private static final String PASSWORD = "aWaSkfqbCOn6upI5FAMK";

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
        SignResponse response = port.validateOtp(encryptedOtp, processId, applicationId, false);

        String assinaturaBase64 = Base64.getEncoder().encodeToString(response.getSignature());
        String certBase64 = response.getCertificate();

        // Debug logging to see what we're getting
        System.out.println("SOAP Response - Signature length: " +
                (response.getSignature() != null ? response.getSignature().length : "null"));
        System.out.println("SOAP Response - Certificate: " +
                (certBase64 != null ? "present (" + certBase64.length() + " chars)" : "null"));

        return new SignResult(assinaturaBase64, certBase64);
    }

    public static pt.ipvc.cartao.ccauth.soap.SignStatus forceSms(String processId, String encryptedCitizenId, byte[] applicationId) {
        System.out.println("SOAP ForceSMS - processId: " + processId);
        System.out.println("SOAP ForceSMS - encryptedCitizenId length: " + (encryptedCitizenId != null ? encryptedCitizenId.length() : "null"));
        System.out.println("SOAP ForceSMS - applicationId length: " + (applicationId != null ? applicationId.length : "null"));
        
        pt.ipvc.cartao.ccauth.soap.SignStatus result = port.forceSMS(processId, encryptedCitizenId, applicationId);
        
        System.out.println("SOAP ForceSMS - Result: " + (result != null ? "Success" : "null"));
        if (result != null) {
            System.out.println("SOAP ForceSMS - Result Code: " + result.getCode());
            System.out.println("SOAP ForceSMS - Result Field: " + result.getField());
            System.out.println("SOAP ForceSMS - Result FieldValue: " + result.getFieldValue());
            System.out.println("SOAP ForceSMS - Result Message: " + result.getMessage());
            System.out.println("SOAP ForceSMS - Result ProcessId: " + result.getProcessId());
        }
        
        return result;
    }
}