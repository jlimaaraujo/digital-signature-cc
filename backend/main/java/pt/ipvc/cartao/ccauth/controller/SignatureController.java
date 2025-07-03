package pt.ipvc.cartao.ccauth.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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
    public String startSigning(@RequestBody SignRequest request) {
        return signatureService.startSigning(request);
    }

    @PostMapping("/validate-otp")
    public ResponseEntity<Resource> validateOtp(@RequestBody OtpRequest request) throws IOException {
        try {
            SignResult response = signatureService.validateOtp(request);

            if (response == null || response.getSignedPdfBytes() == null || response.getSignedPdfBytes().length == 0) {
                System.err.println("No signed PDF data returned from service");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            ByteArrayResource resource = new ByteArrayResource(response.getSignedPdfBytes());

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=documento_assinado.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(response.getSignedPdfBytes().length)
                    .body(resource);

        } catch (Exception e) {
            System.err.println("Error in validateOtp: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/verify")
    public boolean verifySignature(@RequestParam String pdfBase64,
                                   @RequestParam String signature,
                                   @RequestParam String certBase64) {
        return signatureService.verify(pdfBase64, signature, certBase64);
    }
}
