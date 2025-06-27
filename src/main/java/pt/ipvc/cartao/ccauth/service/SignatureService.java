package pt.ipvc.cartao.ccauth.service;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfDocumentInfo;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import org.springframework.stereotype.Service;
import pt.ipvc.cartao.ccauth.model.OtpRequest;
import pt.ipvc.cartao.ccauth.model.SignRequest;
import pt.ipvc.cartao.ccauth.model.SignResult;
import pt.ipvc.cartao.ccauth.util.CryptoUtils;
import pt.ipvc.cartao.ccauth.util.HashUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

@Service
public class SignatureService {

    private static final String APPLICATION_ID = "882ec3e2-97c4-4abc-bb6c-5f9a59fbbf39";
    private static final String CERT_PATH = "cifra.cer";

    private String processId;
    private byte[] lastHash;
    private String lastCertificate;
    private byte[] originalPdfBytes;

    public byte[] generateHash(byte[] pdfContent) {
        this.originalPdfBytes = pdfContent;
        this.lastHash = HashUtils.generatePkcs1Hash(pdfContent);
        return this.lastHash;
    }

    public String getCertificate(String phoneNumber) {
        String encryptedPhone = CryptoUtils.encrypt(phoneNumber, CERT_PATH);
        String cert = SoapClientService.getCertificate(APPLICATION_ID.getBytes(), encryptedPhone);

        System.out.println("Certificate retrieved from SOAP: " +
                (cert != null ? "present (" + cert.length() + " chars)" : "null"));

        this.lastCertificate = cert;

        System.out.println("Certificate stored in service: " +
                (this.lastCertificate != null ? "present (" + this.lastCertificate.length() + " chars)" : "null"));

        return cert;
    }

    public String startSigning(SignRequest request) {
        String encryptedPhone = CryptoUtils.encrypt(request.getPhoneNumber(), CERT_PATH);
        String encryptedPin = CryptoUtils.encrypt(request.getPin(), CERT_PATH);

        if (this.lastCertificate == null || this.lastCertificate.trim().isEmpty()) {
            try {
                this.lastCertificate = SoapClientService.getCertificate(
                        APPLICATION_ID.getBytes(),
                        encryptedPhone
                );

                if (this.lastCertificate != null) {
                    System.out.println("Certificate obtained: present (" +
                            this.lastCertificate.length() + " chars)");
                }
            } catch (Exception e) {
                System.err.println("Failed to get certificate: " + e.getMessage());
            }
        }

        String response = SoapClientService.sign(APPLICATION_ID.getBytes(), request.getDocName(), lastHash, encryptedPhone, encryptedPin);
        this.processId = response;
        return response;
    }

    public SignResult validateOtp(OtpRequest request) {
        // Por enquanto gera ambos (comportamento atual)
        // Futuramente pode ser alterado conforme necessário
        return validateOtpWithChoice(request, SignatureType.BOTH);
    }

    public SignResult validateOtpWithoutVisual(OtpRequest request) {
        try {
            String encryptedOtp = CryptoUtils.encrypt(request.getOtp(), CERT_PATH);

            SignResult result = SoapClientService.validateOtp(encryptedOtp, this.processId, APPLICATION_ID.getBytes());

            System.out.println("Certificate from SOAP response: " +
                    (result.getCertBase64() != null ? "present (" + result.getCertBase64().length() + " chars)" : "null"));
            System.out.println("Stored certificate: " +
                    (this.lastCertificate != null ? "present (" + this.lastCertificate.length() + " chars)" : "null"));

            String certificateToUse = this.lastCertificate;

            if (certificateToUse != null) {
                System.out.println("Using stored certificate from getCertificate call");
            } else {
                System.out.println("No certificate available");
                return null;
            }

            // Process the signed PDF without visual signature
            byte[] signedPdfBytes = processSignedPdfWithoutVisual(this.originalPdfBytes,
                    Base64.getDecoder().decode(result.getAssinaturaBase64()),
                    certificateToUse);

            if (signedPdfBytes != null) {
                System.out.println("PDF processado e assinado com sucesso (sem marca visual)");

                // Save the signed PDF to file
                String savedPath = saveSignedPdf(signedPdfBytes, "_no_visual.pdf");

                result.setSignedPdfBytes(signedPdfBytes);

                return result;
            } else {
                System.out.println("Falha no processamento do PDF");
                return null;
            }

        } catch (Exception e) {
            System.err.println("Erro na validação OTP: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private byte[] processSignedPdfWithoutVisual(byte[] pdfData, byte[] signature, String certificate) {
        try {
            PdfReader reader = new PdfReader(new ByteArrayInputStream(pdfData));
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(outputStream);
            PdfDocument pdfDoc = new PdfDocument(reader, writer);

            // Add signature as metadata only (no visual signature)
            String signatureBase64 = Base64.getEncoder().encodeToString(signature);
            PdfDocumentInfo info = pdfDoc.getDocumentInfo();
            info.setMoreInfo("AssinaturaCMD", signatureBase64);

            // Extract name and ID from certificate for metadata
            String[] userData = extractUserDataFromCertificate(certificate);
            String nome = userData[0];
            String ccNumber = userData[1];

            // Add signer info to metadata
            info.setMoreInfo("AssinadoPor", nome);
            info.setMoreInfo("NumeroCC", ccNumber);
            info.setMoreInfo("DataAssinatura", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss")));

            pdfDoc.close();

            return outputStream.toByteArray();

        } catch (Exception e) {
            System.err.println("Erro ao processar PDF assinado sem marca visual: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private byte[] processSignedPdf(byte[] pdfData, byte[] signature, String certificate) {
        try {
            PdfReader reader = new PdfReader(new ByteArrayInputStream(pdfData));
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(outputStream);
            PdfDocument pdfDoc = new PdfDocument(reader, writer);

            // Add signature as metadata
            String signatureBase64 = Base64.getEncoder().encodeToString(signature);
            PdfDocumentInfo info = pdfDoc.getDocumentInfo();
            info.setMoreInfo("AssinaturaCMD", signatureBase64);

            // Extract name and ID from certificate
            String[] userData = extractUserDataFromCertificate(certificate);
            String nome = userData[0];
            String ccNumber = userData[1];

            // Add visual signature overlay to first page
            addVisualSignature(pdfDoc, nome, ccNumber);

            pdfDoc.close();

            return outputStream.toByteArray();

        } catch (Exception e) {
            System.err.println("Erro ao processar PDF assinado: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private void addVisualSignature(PdfDocument pdfDoc, String nome, String ccNumber) {
        try {
            Document document = new Document(pdfDoc);

            // Posições (similar ao Python)
            float textX = 130;
            float textY = 107;
            float logoW = 36;
            float logoH = 36;
            float logoX = textX - 2;
            float logoY = textY - 32;

            // Adicionar logo com opacidade
            try {
                String logoPath = "src/main/resources/logo/CMD-assinatura-2.png";
                java.nio.file.Path path = java.nio.file.Paths.get(logoPath);

                if (java.nio.file.Files.exists(path)) {
                    com.itextpdf.io.image.ImageData imageData = com.itextpdf.io.image.ImageDataFactory.create(logoPath);
                    com.itextpdf.layout.element.Image logo = new com.itextpdf.layout.element.Image(imageData);

                    // Configurar tamanho e posição
                    logo.setWidth(logoW);
                    logo.setHeight(logoH);
                    logo.setFixedPosition(logoX, logoY);

                    // Definir opacidade (0.15 = 15% opacidade)
                    logo.setOpacity(0.15f);

                    document.add(logo);
                } else {
                    System.out.println("Logo não encontrado em: " + logoPath);
                }
            } catch (Exception e) {
                System.err.println("Erro ao adicionar logo: " + e.getMessage());
            }

            // Adicionar texto da assinatura
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss"));

            // Texto "Assinado por:" em negrito
            Paragraph assinadoPor = new Paragraph("Assinado por: ")
                    .setFontSize(6)
                    .setFixedPosition(textX, textY - 8, 200);
            document.add(assinadoPor);

            // Nome do utilizador
            Paragraph nomeText = new Paragraph(nome)
                    .setFontSize(6)
                    .setFixedPosition(textX + 45, textY - 8, 150);
            document.add(nomeText);

            // Texto "Num. de Identificação:"
            Paragraph numIdText = new Paragraph("Num. de Identificação: ")
                    .setFontSize(6)
                    .setFixedPosition(textX, textY - 16, 200);
            document.add(numIdText);

            // Número do CC
            Paragraph ccText = new Paragraph(ccNumber)
                    .setFontSize(6)
                    .setFixedPosition(textX + 67, textY - 16, 100);
            document.add(ccText);

            // Data
            Paragraph dataText = new Paragraph("Data: " + timestamp)
                    .setFontSize(6)
                    .setFixedPosition(textX, textY - 24, 200);
            document.add(dataText);

            document.close();

        } catch (Exception e) {
            System.err.println("Erro ao adicionar assinatura visual: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String[] extractUserDataFromCertificate(String certificate) {
        try {
            byte[] certBytes = null;

            // Determinar se é PEM ou DER/Base64
            if (certificate != null && certificate.trim().startsWith("-----BEGIN")) {
                // É PEM
                certBytes = certificate.getBytes("UTF-8");
            } else if (certificate != null && !certificate.trim().isEmpty()) {
                // É DER ou Base64
                try {
                    certBytes = Base64.getDecoder().decode(certificate.trim());
                } catch (IllegalArgumentException e) {
                    // Se falhar, pode ser que já seja bytes raw
                    certBytes = certificate.getBytes("UTF-8");
                }
            }

            if (certBytes == null) {
                return new String[]{"Utilizador", "—"};
            }

            X509Certificate cert = null;

            // Tentar carregar como certificado X.509 direto
            try {
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                cert = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(certBytes));
            } catch (Exception e) {
                System.err.println("Erro ao carregar certificado diretamente: " + e.getMessage());
            }

            if (cert == null) {
                return new String[]{"Utilizador", "—"};
            }

            // Usar o parsing X.509 nativo em vez de regex no DN string
            javax.security.auth.x500.X500Principal subject = cert.getSubjectX500Principal();

            // Extrair dados do Subject DN
            String subjectDN = cert.getSubjectX500Principal().getName();
            System.out.println("Subject DN completo: " + subjectDN);

            // Extrair campos usando o parsing nativo
            String nome = getFieldFromX500Principal(subject, "CN");
            if (nome == null || nome.trim().isEmpty()) {
                String givenName = getFieldFromX500Principal(subject, "GIVENNAME");
                String surname = getFieldFromX500Principal(subject, "SURNAME");
                nome = ((givenName != null ? givenName : "") + " " +
                        (surname != null ? surname : "")).trim();
                if (nome.isEmpty()) {
                    nome = "Utilizador";
                }
            }

            String ccNumber = getFieldFromX500Principal(subject, "SERIALNUMBER");
            if (ccNumber == null || ccNumber.trim().isEmpty()) {
                ccNumber = "—";
            }

            System.out.println("Nome extraído: " + nome);
            System.out.println("CC extraído: " + ccNumber);

            return new String[]{nome, ccNumber};

        } catch (Exception e) {
            System.err.println("Erro ao extrair dados do certificado: " + e.getMessage());
            e.printStackTrace();
            return new String[]{"Utilizador", "—"};
        }
    }

    private String getFieldFromX500Principal(javax.security.auth.x500.X500Principal principal, String fieldName) {
        try {
            String dn = principal.getName(javax.security.auth.x500.X500Principal.RFC2253);

            // Mapear nomes de campos para OIDs
            String oid = null;
            switch (fieldName.toUpperCase()) {
                case "CN": oid = "2.5.4.3"; break;
                case "SERIALNUMBER": oid = "2.5.4.5"; break;
                case "GIVENNAME": oid = "2.5.4.42"; break;
                case "SURNAME": oid = "2.5.4.4"; break;
            }

            if (oid != null) {
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                        oid + "=([^,]+)",
                        java.util.regex.Pattern.CASE_INSENSITIVE
                );
                java.util.regex.Matcher matcher = pattern.matcher(dn);
                if (matcher.find()) {
                    String value = matcher.group(1).trim();
                    // Decodificar se estiver em formato hex
                    if (value.startsWith("#")) {
                        return decodeHexString(value.substring(1));
                    }
                    return value;
                }
            }

            return null;
        } catch (Exception e) {
            System.err.println("Erro ao extrair campo " + fieldName + ": " + e.getMessage());
            return null;
        }
    }

    private String decodeHexString(String hexString) {
        try {
            // Remove os primeiros 4 caracteres que são metadados ASN.1
            // e decodifica o resto como string UTF-8
            if (hexString.length() > 4) {
                String dataHex = hexString.substring(4); // Remove 130a
                StringBuilder result = new StringBuilder();
                for (int i = 0; i < dataHex.length(); i += 2) {
                    String hex = dataHex.substring(i, i + 2);
                    result.append((char) Integer.parseInt(hex, 16));
                }
                return result.toString();
            }
        } catch (Exception e) {
            System.err.println("Erro na decodificação hex: " + e.getMessage());
        }
        return null;
    }



    public boolean verify(String pdfBase64, String assinaturaBase64, String certBase64) {
        try {
            byte[] pdfBytes = Base64.getDecoder().decode(pdfBase64);
            byte[] assinatura = Base64.getDecoder().decode(assinaturaBase64);
            byte[] cert = Base64.getDecoder().decode(certBase64);

            return CryptoUtils.verifySignature(pdfBytes, assinatura, cert);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private String saveSignedPdf(byte[] signedPdfBytes, String suffix) {
        try {
            // Create output directory if it doesn't exist
            String outputDir = "src/main/resources/output";
            java.nio.file.Path outputPath = java.nio.file.Paths.get(outputDir);
            if (!java.nio.file.Files.exists(outputPath)) {
                java.nio.file.Files.createDirectories(outputPath);
            }

            // Generate filename with timestamp
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = "signed_" + timestamp + suffix;
            String filePath = outputDir + "/" + fileName;

            // Write the signed PDF to file
            java.nio.file.Files.write(java.nio.file.Paths.get(filePath), signedPdfBytes);

            System.out.println("Signed PDF saved to: " + filePath);
            return filePath;

        } catch (Exception e) {
            System.err.println("Error saving signed PDF: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // Enum para escolher o tipo de assinatura
    public enum SignatureType {
        WITH_VISUAL, WITHOUT_VISUAL, BOTH
    }

    public SignResult validateOtpWithChoice(OtpRequest request, SignatureType signatureType) {
        try {
            String encryptedOtp = CryptoUtils.encrypt(request.getOtp(), CERT_PATH);

            SignResult result = SoapClientService.validateOtp(encryptedOtp, this.processId, APPLICATION_ID.getBytes());

            System.out.println("Certificate from SOAP response: " +
                    (result.getCertBase64() != null ? "present (" + result.getCertBase64().length() + " chars)" : "null"));
            System.out.println("Stored certificate: " +
                    (this.lastCertificate != null ? "present (" + this.lastCertificate.length() + " chars)" : "null"));

            String certificateToUse = this.lastCertificate;

            if (certificateToUse == null) {
                System.out.println("No certificate available");
                return null;
            }

            byte[] decodedSignature = Base64.getDecoder().decode(result.getAssinaturaBase64());
            byte[] signedPdfWithVisual = null;
            byte[] signedPdfWithoutVisual = null;

            // Gerar conforme a escolha do utilizador
            switch (signatureType) {
                case WITH_VISUAL:
                    signedPdfWithVisual = processSignedPdf(this.originalPdfBytes, decodedSignature, certificateToUse);
                    if (signedPdfWithVisual != null) {
                        String savedPath = saveSignedPdf(signedPdfWithVisual, "_with_visual.pdf");
                        System.out.println("Ficheiro com marca visual gerado: " + savedPath);
                        result.setSignedPdfBytes(signedPdfWithVisual);
                    }
                    break;

                case WITHOUT_VISUAL:
                    signedPdfWithoutVisual = processSignedPdfWithoutVisual(this.originalPdfBytes, decodedSignature, certificateToUse);
                    if (signedPdfWithoutVisual != null) {
                        String savedPath = saveSignedPdf(signedPdfWithoutVisual, "_no_visual.pdf");
                        System.out.println("Ficheiro sem marca visual gerado: " + savedPath);
                        result.setSignedPdfBytes(signedPdfWithoutVisual);
                    }
                    break;

                case BOTH:
                    signedPdfWithVisual = processSignedPdf(this.originalPdfBytes, decodedSignature, certificateToUse);
                    signedPdfWithoutVisual = processSignedPdfWithoutVisual(this.originalPdfBytes, decodedSignature, certificateToUse);

                    if (signedPdfWithVisual != null && signedPdfWithoutVisual != null) {
                        String savedPathWithVisual = saveSignedPdf(signedPdfWithVisual, "_with_visual.pdf");
                        String savedPathWithoutVisual = saveSignedPdf(signedPdfWithoutVisual, "_no_visual.pdf");

                        System.out.println("Ambos os ficheiros gerados:");
                        System.out.println("  Com marca visual: " + savedPathWithVisual);
                        System.out.println("  Sem marca visual: " + savedPathWithoutVisual);

                        // Retornar o PDF com marca visual como principal
                        result.setSignedPdfBytes(signedPdfWithVisual);
                    }
                    break;
            }

            if (result.getSignedPdfBytes() != null) {
                System.out.println("PDF(s) processado(s) e assinado(s) com sucesso");
                return result;
            } else {
                System.out.println("Falha no processamento do(s) PDF(s)");
                return null;
            }

        } catch (Exception e) {
            System.err.println("Erro na validação OTP: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}