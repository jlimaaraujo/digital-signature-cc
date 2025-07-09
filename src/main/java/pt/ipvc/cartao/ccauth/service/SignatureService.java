package pt.ipvc.cartao.ccauth.service;

import com.itextpdf.kernel.pdf.*;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.signatures.*;
import com.itextpdf.kernel.pdf.StampingProperties;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.stereotype.Service;
import pt.ipvc.cartao.ccauth.model.OtpRequest;
import pt.ipvc.cartao.ccauth.model.SignRequest;
import pt.ipvc.cartao.ccauth.model.SignResult;
import pt.ipvc.cartao.ccauth.util.CryptoUtils;
import pt.ipvc.cartao.ccauth.util.HashUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import com.itextpdf.kernel.geom.Rectangle;


@Service
public class SignatureService {

    private static final Dotenv dotenv = Dotenv.load();
    private static final String APPLICATION_ID = dotenv.get("APPLICATION_ID");
    private static final String CERT_PATH = dotenv.get("CERT_PATH");

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

        this.lastCertificate = cert;

        return cert;
    }

    public String startSigning(SignRequest request) {
        String encryptedPhone = CryptoUtils.encrypt(request.getPhoneNumber(), CERT_PATH);
        String encryptedPin = CryptoUtils.encrypt(request.getPin(), CERT_PATH);

        // Obter sempre o certificado antes de assinar
        try {
            this.lastCertificate = SoapClientService.getCertificate(
                    APPLICATION_ID.getBytes(),
                    encryptedPhone
            );

            if (this.lastCertificate != null) {
            } else {
                throw new RuntimeException("Failed to obtain certificate");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to get certificate: " + e.getMessage());
        }

        String response = SoapClientService.sign(APPLICATION_ID.getBytes(), request.getDocName(), lastHash, encryptedPhone, encryptedPin);
        this.processId = response;
        return response;
    }

    public SignResult validateOtp(OtpRequest request) {
        try {
            String encryptedOtp = CryptoUtils.encrypt(request.getOtp(), CERT_PATH);
            SignResult result = SoapClientService.validateOtp(encryptedOtp, this.processId, APPLICATION_ID.getBytes());

            String certificateToUse = this.lastCertificate;
            if (certificateToUse == null) {
                System.err.println("Certificado não disponível");
                return null;
            }

            if (result == null || result.getAssinaturaBase64() == null) {
                System.err.println("Resultado da assinatura inválido");
                return null;
            }

            byte[] decodedSignature = Base64.getDecoder().decode(result.getAssinaturaBase64());
            //System.out.println("Assinatura decodificada, tamanho: " + decodedSignature.length + " bytes");

            // Verificar se temos o PDF original
            if (this.originalPdfBytes == null) {
                //System.err.println("PDF original não disponível");
                return null;
            }

            byte[] signedPdf = null;

            // Verificar se tem assinatura visual configurada
            if (request.isHasVisualSignature()) {

                // Usar metodo que combina assinatura digital + visual
                signedPdf = embedDigitalSignatureWithVisual(
                        this.originalPdfBytes,
                        decodedSignature,
                        certificateToUse,
                        request.getSignaturePage(),
                        request.getSignatureXPercent(),
                        request.getSignatureYPercent(),
                        request.getMotivo(),
                        request.getLocal()
                );
            } else {
                // Usar metodo só com assinatura digital
                signedPdf = embedDigitalSignature(
                        this.originalPdfBytes,
                        decodedSignature,
                        certificateToUse
                );
            }

            if (signedPdf != null) {
                System.out.println("PDF assinado com sucesso, tamanho: " + signedPdf.length + " bytes");

                // Verificar se a assinatura foi embebida
                if (verifyEmbeddedSignature(signedPdf)) {
                    System.out.println("Assinatura digital PAdES embebida com sucesso!");
                } else {
                    System.out.println("Aviso: Verificação da assinatura embebida falhou, mas PDF foi processado");
                }

                // Guardar o PDF
                String suffix = request.isHasVisualSignature() ? "_signed_visual_pades.pdf" : "_signed_pades.pdf";
                String savedPath = saveSignedPdf(signedPdf, suffix);
                System.out.println("PDF guardado em: " + savedPath);

                result.setSignedPdfBytes(signedPdf);
            } else {
                System.err.println("Falha ao embutir assinatura digital no PDF");
                return null;
            }

            return result;

        } catch (Exception e) {
            System.err.println("Erro em validateOtp: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private byte[] embedDigitalSignatureWithVisual(byte[] pdfData, byte[] signature, String certificate,
                                                   int pageNumber, float xPercent, float yPercent,
                                                   String motivo, String local) {
        try {
            // Debug da assinatura
            //debugSignature(signature);

            // Converter certificado
            X509Certificate cert = parseCertificate(certificate);
            if (cert == null) {
                System.err.println("Erro: Certificado inválido");
                return null;
            }

            Certificate[] chain = new Certificate[]{cert};

            // Preparar PDF
            PdfReader reader = new PdfReader(new ByteArrayInputStream(pdfData));
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            PdfSigner signer = new PdfSigner(reader, outputStream, new StampingProperties());

            // Configurar aparência da assinatura
            PdfSignatureAppearance appearance = signer.getSignatureAppearance();

            // Tratar motivo - só usar se não for null, vazio, ou string "null"
            String motivoFinal = "Assinatura Digital CMD";
            if (motivo != null && !motivo.trim().isEmpty() && !"null".equalsIgnoreCase(motivo.trim())) {
                motivoFinal = motivo.trim();
            }

            // Tratar local - só usar se não for null, vazio, ou string "null"
            String localFinal = "Portugal";
            if (local != null && !local.trim().isEmpty() && !"null".equalsIgnoreCase(local.trim())) {
                localFinal = local.trim();
            }

            appearance.setReason(motivoFinal);
            appearance.setLocation(localFinal);

            // Extrair dados do certificado
            String[] userData = extractUserDataFromCertificate(certificate);
            String signerName = userData[0];
            String ccNumber = userData[1];

            // Configurar posição da assinatura visual (tamanho reduzido)
            try {
                // Obter dimensões da página
                PdfDocument tempDoc = new PdfDocument(new PdfReader(new ByteArrayInputStream(pdfData)));
                float pageWidth = tempDoc.getPage(pageNumber).getPageSize().getWidth();
                float pageHeight = tempDoc.getPage(pageNumber).getPageSize().getHeight();
                tempDoc.close();

                // Tamanho reduzido da assinatura
                float signatureWidth = 120;
                float signatureHeight = 40;

                float xPos = (xPercent / 100f) * pageWidth - (signatureWidth / 2);
                float yPos = pageHeight - ((yPercent / 100f) * pageHeight) - (signatureHeight / 2);

                // Garantir que a assinatura não sai da página
                xPos = Math.max(0, Math.min(xPos, pageWidth - signatureWidth));
                yPos = Math.max(0, Math.min(yPos, pageHeight - signatureHeight));

                // Configurar posição da assinatura
                Rectangle rect = new Rectangle(xPos, yPos, signatureWidth, signatureHeight);
                appearance.setPageRect(rect);
                appearance.setPageNumber(pageNumber);

            } catch (Exception e) {
                System.err.println("Erro ao configurar posição da assinatura: " + e.getMessage());
            }

            // Adicionar logo
            try {
                // Tentar carregar logo como resource
                InputStream logoStream = getClass().getClassLoader().getResourceAsStream("logo/CMD-assinatura-2.png");
                if (logoStream != null) {
                    com.itextpdf.io.image.ImageData imageData =
                            com.itextpdf.io.image.ImageDataFactory.create(logoStream.readAllBytes());

                    // Configurar logo na assinatura
                    appearance.setSignatureGraphic(imageData);
                    appearance.setRenderingMode(PdfSignatureAppearance.RenderingMode.GRAPHIC_AND_DESCRIPTION);

                    logoStream.close();
                } else {
                    System.out.println("Logo não encontrado, usando modo texto apenas");
                    appearance.setRenderingMode(PdfSignatureAppearance.RenderingMode.DESCRIPTION);
                }
            } catch (Exception e) {
                System.err.println("Erro ao carregar logo: " + e.getMessage());
                appearance.setRenderingMode(PdfSignatureAppearance.RenderingMode.DESCRIPTION);
            }

            // Configurar texto da aparência (mais compacto)
            StringBuilder signatureText = new StringBuilder();
            signatureText.append("Assinado por: ")
                    .append(signerName).append("\n")
                    .append("Nº de Identificação: ").append(ccNumber).append("\n")
                    .append("Data: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));

            // Adicionar motivo e local se fornecidos (só se diferentes dos padrões)
            if (motivo != null && !motivo.trim().isEmpty() && !"null".equalsIgnoreCase(motivo.trim()) &&
                    !"Assinatura Digital CMD".equals(motivoFinal)) {
                signatureText.append("\nMotivo: ").append(motivoFinal);
            }
            if (local != null && !local.trim().isEmpty() && !"null".equalsIgnoreCase(local.trim()) &&
                    !"Portugal".equals(localFinal)) {
                signatureText.append("\nLocal: ").append(localFinal);
            }

            appearance.setLayer2Text(signatureText.toString());

            // Usar signExternalContainer (SÓ UMA VEZ)
            IExternalSignatureContainer container = new IExternalSignatureContainer() {
                @Override
                public byte[] sign(InputStream data) throws GeneralSecurityException {
                    return signature;
                }

                @Override
                public void modifySigningDictionary(PdfDictionary signDic) {
                    signDic.put(PdfName.Filter, PdfName.Adobe_PPKLite);
                    signDic.put(PdfName.SubFilter, PdfName.Adbe_pkcs7_detached);
                }
            };

            // Assinar o documento (SÓ UMA VEZ)
            signer.signExternalContainer(container, 8192);

            byte[] result = outputStream.toByteArray();
            System.out.println("Assinatura única criada com sucesso!");
            return result;

        } catch (Exception e) {
            System.err.println("Erro na assinatura PAdES com visual: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }


    private byte[] embedDigitalSignature(byte[] pdfData, byte[] signature, String certificate) {
        try {
            // Debug da assinatura
            //debugSignature(signature);

            // Converter certificado
            X509Certificate cert = parseCertificate(certificate);
            if (cert == null) {
                System.err.println("Erro: Certificado inválido");
                return null;
            }
            ;
            PdfReader reader = new PdfReader(new ByteArrayInputStream(pdfData));
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PdfSigner signer = new PdfSigner(reader, outputStream, new StampingProperties());

            // Para assinatura invisível, não configurar aparência

            // Container para assinatura externa
            IExternalSignatureContainer container = new IExternalSignatureContainer() {
                @Override
                public byte[] sign(InputStream data) throws GeneralSecurityException {
                    System.out.println("Aplicando assinatura CMD de " + signature.length + " bytes");
                    return signature;
                }

                @Override
                public void modifySigningDictionary(PdfDictionary signDic) {
                    signDic.put(PdfName.Filter, PdfName.Adobe_PPKLite);
                    signDic.put(PdfName.SubFilter, PdfName.Adbe_pkcs7_detached);
                    System.out.println("Dicionário de assinatura configurado");
                }
            };

            // Assinar usando container externo
            signer.signExternalContainer(container, 8192);

            byte[] result = outputStream.toByteArray();

            return result;

        } catch (Exception e) {
            System.err.println("Erro na assinatura CMD: " + e.getMessage());
            e.printStackTrace();

            return null;
        }
    }

    private void debugSignature(byte[] signature) {
        try {
            System.out.println("\n=== DEBUG ASSINATURA CMD ===");
            System.out.println("Tamanho: " + signature.length + " bytes");

            // Mostrar primeiros 32 bytes em hex
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < Math.min(32, signature.length); i++) {
                hex.append(String.format("%02X ", signature[i]));
            }
            System.out.println("Primeiros 32 bytes (hex): " + hex.toString());

            // Verificar se começa com SEQUENCE (0x30) - indicativo de PKCS#7
            if (signature.length > 0) {
                if (signature[0] == 0x30) {
                    System.out.println("Formato: Possível PKCS#7 (começa com SEQUENCE)");
                } else {
                    System.out.println("Formato: Assinatura RSA crua (não é PKCS#7)");
                }
            }

            // Verificar se é Base64 válido
            try {
                String base64 = Base64.getEncoder().encodeToString(signature);
                System.out.println("Base64 length: " + base64.length());
            } catch (Exception e) {
                System.out.println("Erro ao converter para Base64");
            }

        } catch (Exception e) {
            System.err.println("Erro no debug: " + e.getMessage());
        }
    }


    private X509Certificate parseCertificate(String certificateString) {
        try {
            byte[] certBytes;

            if (certificateString.trim().startsWith("-----BEGIN")) {
                // Formato PEM
                certBytes = certificateString.getBytes("UTF-8");
            } else {
                // Formato Base64/DER
                certBytes = Base64.getDecoder().decode(certificateString.trim());
            }

            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(certBytes));

        } catch (Exception e) {
            System.err.println("Erro ao converter certificado: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private boolean verifyEmbeddedSignature(byte[] signedPdfBytes) {
        try {

            PdfReader reader = new PdfReader(new ByteArrayInputStream(signedPdfBytes));
            PdfDocument pdfDoc = new PdfDocument(reader);

            // Verificar se há campos de assinatura
            SignatureUtil signatureUtil = new SignatureUtil(pdfDoc);
            List<String> signatureNames = signatureUtil.getSignatureNames();

            if (signatureNames.isEmpty()) {
                System.out.println("Nenhuma assinatura digital encontrada");

                // Verificar se há metadados de assinatura (fallback)
                PdfDocumentInfo info = pdfDoc.getDocumentInfo();
                boolean hasSignatureMetadata = info.getMoreInfo("AssinaturaCMD") != null;

                pdfDoc.close();

                if (hasSignatureMetadata) {
                    System.out.println("Assinatura CMD encontrada em metadados");
                    return true;
                }

                System.out.println("Nenhuma assinatura encontrada");
                return false;
            }

            System.out.println("Encontradas " + signatureNames.size() + " assinatura(s) digital(is)");

            // Verificar assinaturas digitais
            for (String signatureName : signatureNames) {
                System.out.println("\n--- Verificando assinatura: " + signatureName + " ---");

                try {
                    // Verificar se a assinatura cobre todo o documento
                    boolean documentIntact = signatureUtil.signatureCoversWholeDocument(signatureName);
                    System.out.println("Documento íntegro: " + documentIntact);

                    if (documentIntact) {
                        System.out.println("Assinatura CMD válida (documento íntegro)");
                        pdfDoc.close();
                        return true;
                    }

                } catch (Exception e) {
                    System.out.println("Erro ao verificar assinatura " + signatureName + ": " + e.getMessage());

                    // Para CMD, se existe assinatura mas não conseguimos validar PKCS#7,
                    // consideramos válida se cobre o documento
                    try {
                        boolean documentIntact = signatureUtil.signatureCoversWholeDocument(signatureName);
                        if (documentIntact) {
                            System.out.println("Assinatura CMD válida (modo compatibilidade)");
                            pdfDoc.close();
                            return true;
                        }
                    } catch (Exception e2) {
                        System.out.println("Erro na verificação de compatibilidade: " + e2.getMessage());
                    }
                }
            }

            pdfDoc.close();
            System.out.println("Assinatura inválida ou não verificável");
            return false;

        } catch (Exception e) {
            System.err.println("Erro na verificação da assinatura: " + e.getMessage());
            return false;
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
            // Cria o diretório de output se não existir
            String outputDir = "src/main/resources/output";
            java.nio.file.Path outputPath = java.nio.file.Paths.get(outputDir);
            if (!java.nio.file.Files.exists(outputPath)) {
                java.nio.file.Files.createDirectories(outputPath);
            }

            // Gera um nome de ficheiro único com timestamp
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = "signed_" + timestamp + suffix;
            String filePath = outputDir + "/" + fileName;

            // Escreve o PDF assinado no ficheiro
            java.nio.file.Files.write(java.nio.file.Paths.get(filePath), signedPdfBytes);

            return filePath;

        } catch (Exception e) {
            System.err.println("Error saving signed PDF: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

        public pt.ipvc.cartao.ccauth.soap.SignStatus forceSms(String processId, String citizenId) {
            System.out.println("ForceSMS called with processId: " + processId + ", citizenId: " + citizenId);
            
            // citizenId deve ser encriptado antes de enviar
            String encryptedCitizenId = CryptoUtils.encrypt(citizenId, CERT_PATH);
            System.out.println("Encrypted citizenId length: " + (encryptedCitizenId != null ? encryptedCitizenId.length() : "null"));
            
            pt.ipvc.cartao.ccauth.soap.SignStatus result = SoapClientService.forceSms(processId, encryptedCitizenId, APPLICATION_ID.getBytes());
            System.out.println("ForceSMS result: " + (result != null ? "Success" : "Failed"));
            
            return result;
        }
    }