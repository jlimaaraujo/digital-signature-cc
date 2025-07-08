package pt.ipvc.cartao.ccauth.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.ipvc.cartao.ccauth.model.ForceSmsRequest;
import pt.ipvc.cartao.ccauth.model.OtpRequest;
import pt.ipvc.cartao.ccauth.model.SignRequest;
import pt.ipvc.cartao.ccauth.model.SignResult;
import pt.ipvc.cartao.ccauth.service.SignatureService;

import java.io.IOException;

@RestController
@RequestMapping("/api/signature")
public class SignatureController {

    @Autowired
    private SignatureService signatureService;

    @PostMapping("/hash")
    public byte[] generateHash(@RequestBody byte[] pdfContent) {
        return signatureService.generateHash(pdfContent);
    }

    @PostMapping("/get-certificate")
    public String getCertificate(@RequestBody String phoneNumber) {
        return signatureService.getCertificate(phoneNumber);
    }

    @PostMapping("/sign")
    public ResponseEntity<String> startSigning(@RequestBody SignRequest request) {
        try {
            String processId = signatureService.startSigning(request);
            return ResponseEntity.ok(processId);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/validate-otp")
    public ResponseEntity<Resource> validateOtp(@RequestBody OtpRequest request) throws IOException {
        try {
            SignResult response = signatureService.validateOtp(request);

            if (response == null || response.getSignedPdfBytes() == null || response.getSignedPdfBytes().length == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            ByteArrayResource resource = new ByteArrayResource(response.getSignedPdfBytes());

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=documento_assinado.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(response.getSignedPdfBytes().length)
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/verify")
    public boolean verifySignature(@RequestParam String pdfBase64,
                                   @RequestParam String signature,
                                   @RequestParam String certBase64) {
        return signatureService.verify(pdfBase64, signature, certBase64);
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendOtp(@RequestBody ForceSmsRequest request) {
        try {
            
            pt.ipvc.cartao.ccauth.soap.SignStatus result = signatureService.forceSms(request.getProcessId(), request.getCitizenId());
            
            if (result != null) {
                
                // Verificar se o código indica sucesso
                if ("200".equals(result.getCode()) || "0".equals(result.getCode())) {
                    return ResponseEntity.ok().body("{\"message\":\"Código reenviado com sucesso\",\"code\":\"" + result.getCode() + "\"}");
                } else {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"message\":\"" + result.getMessage() + "\",\"code\":\"" + result.getCode() + "\"}");
                }
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"message\":\"Erro interno no serviço\"}");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"message\":\"Erro: " + e.getMessage() + "\"}");
        }
    }
}
