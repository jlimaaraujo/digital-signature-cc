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
    private static final String CERT_PATH = "cifra/cifra.cer";

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

        // Sempre obter o certificado antes de assinar
        try {
            this.lastCertificate = SoapClientService.getCertificate(
                    APPLICATION_ID.getBytes(),
                    encryptedPhone
            );

            if (this.lastCertificate != null) {
                System.out.println("Certificate obtained for signing: present (" +
                        this.lastCertificate.length() + " chars)");
            } else {
                System.err.println("Failed to obtain certificate - null response");
                throw new RuntimeException("Failed to obtain certificate");
            }
        } catch (Exception e) {
            System.err.println("Failed to get certificate: " + e.getMessage());
            throw new RuntimeException("Failed to get certificate: " + e.getMessage());
        }

        String response = SoapClientService.sign(APPLICATION_ID.getBytes(), request.getDocName(), lastHash, encryptedPhone, encryptedPin);
        this.processId = response;
        System.out.println("Sign process started with processId: " + this.processId);
        return response;
    }

    public SignResult validateOtp(OtpRequest request) {
        try {
            String encryptedOtp = CryptoUtils.encrypt(request.getOtp(), CERT_PATH);
            SignResult result = SoapClientService.validateOtp(encryptedOtp, this.processId, APPLICATION_ID.getBytes());

            String certificateToUse = this.lastCertificate;
            if (certificateToUse == null) {
                System.out.println("No certificate available");
                return null;
            }

            byte[] decodedSignature = Base64.getDecoder().decode(result.getAssinaturaBase64());

            if (request.isHasVisualSignature()) {
                // Processar com assinatura visual posicionada
                byte[] signedPdf = processSignedPdfWithPosition(
                        this.originalPdfBytes,
                        decodedSignature,
                        certificateToUse,
                        request.getSignaturePage(),
                        request.getSignatureXPercent(),
                        request.getSignatureYPercent()
                );

                if (signedPdf != null) {
                    String savedPath = saveSignedPdf(signedPdf, "_positioned.pdf");
                    System.out.println("PDF com assinatura posicionada: " + savedPath);
                    result.setSignedPdfBytes(signedPdf);
                }
            } else {
                // Processar sem assinatura visual
                byte[] signedPdf = processSignedPdfWithoutVisual(
                        this.originalPdfBytes,
                        decodedSignature,
                        certificateToUse
                );

                if (signedPdf != null) {
                    String savedPath = saveSignedPdf(signedPdf, "_no_visual.pdf");
                    System.out.println("PDF sem marca visual: " + savedPath);
                    result.setSignedPdfBytes(signedPdf);
                }
            }

            return result;

        } catch (Exception e) {
            System.err.println("Erro na validação OTP: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private byte[] processSignedPdfWithPosition(byte[] pdfData, byte[] signature,
                                                String certificate, int pageNumber,
                                                float xPercent, float yPercent) {
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

            // Add visual signature at custom position
            addVisualSignatureWithPosition(pdfDoc, nome, ccNumber, pageNumber, xPercent, yPercent);

            pdfDoc.close();
            return outputStream.toByteArray();

        } catch (Exception e) {
            System.err.println("Erro ao processar PDF com posição: " + e.getMessage());
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



    private void addVisualSignatureWithPosition(PdfDocument pdfDoc, String nome, String ccNumber,
                                                int pageNumber, float xPercent, float yPercent) {
        try {
            Document document = new Document(pdfDoc);

            // Obter as dimensões da página
            float pageWidth = pdfDoc.getPage(pageNumber).getPageSize().getWidth();
            float pageHeight = pdfDoc.getPage(pageNumber).getPageSize().getHeight();

            // Dimensões da assinatura
            float signatureWidth = 120;
            float signatureHeight = 40;

            // Converter percentagem para coordenadas absolutas
            // Nota: No PDF, Y=0 está no fundo da página, então precisamos inverter
            float xPos = (xPercent / 100) * pageWidth - (signatureWidth / 2);
            float yPos = pageHeight - ((yPercent / 100) * pageHeight) - (signatureHeight / 2);

            // Dimensões do logo
            float logoW = 36;
            float logoH = 36;
            float logoX = xPos;
            float logoY = yPos - 8;

            // Adicionar logo com opacidade
            try {
                String logoPath = "src/main/resources/logo/CMD-assinatura-2.png";
                java.nio.file.Path path = java.nio.file.Paths.get(logoPath);

                if (java.nio.file.Files.exists(path)) {
                    com.itextpdf.io.image.ImageData imageData =
                            com.itextpdf.io.image.ImageDataFactory.create(logoPath);
                    com.itextpdf.layout.element.Image logo =
                            new com.itextpdf.layout.element.Image(imageData);

                    logo.setWidth(logoW);
                    logo.setHeight(logoH);
                    logo.setFixedPosition(pageNumber, logoX, logoY);
                    logo.setOpacity(0.15f);

                    document.add(logo);
                }
            } catch (Exception e) {
                System.err.println("Erro ao adicionar logo: " + e.getMessage());
            }

            // Adicionar texto da assinatura
            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss"));

            // Configurar fonte menor
            float fontSize = 6f;

            // Texto completo da assinatura
            Paragraph assinadoPor = new Paragraph("Assinado por: " + nome)
                    .setFontSize(fontSize)
                    .setFixedPosition(pageNumber, xPos, yPos + 24, signatureWidth);
            document.add(assinadoPor);

            Paragraph ccText = new Paragraph("Num. de Identificação: " + ccNumber)
                    .setFontSize(fontSize)
                    .setFixedPosition(pageNumber, xPos, yPos + 16, signatureWidth);
            document.add(ccText);

            Paragraph dataText = new Paragraph("Data: " + timestamp)
                    .setFontSize(fontSize)
                    .setFixedPosition(pageNumber, xPos, yPos + 8, signatureWidth);
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
                String dataHex = hexString.substring(4); // Removes 130a
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

        public pt.ipvc.cartao.ccauth.soap.SignStatus forceSms(String processId, String citizenId) {
            System.out.println("ForceSMS called with processId: " + processId + ", citizenId: " + citizenId);
            
            // citizenId must be base64 encoded and encrypted before sending
            String encryptedCitizenId = CryptoUtils.encrypt(citizenId, CERT_PATH);
            System.out.println("Encrypted citizenId length: " + (encryptedCitizenId != null ? encryptedCitizenId.length() : "null"));
            
            pt.ipvc.cartao.ccauth.soap.SignStatus result = SoapClientService.forceSms(processId, encryptedCitizenId, APPLICATION_ID.getBytes());
            System.out.println("ForceSMS result: " + (result != null ? "Success" : "Failed"));
            
            return result;
        }
    }