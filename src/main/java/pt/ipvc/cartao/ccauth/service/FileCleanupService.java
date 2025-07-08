package pt.ipvc.cartao.ccauth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

@Service
@ConditionalOnProperty(name = "file.cleanup.enabled", havingValue = "true", matchIfMissing = true)
public class FileCleanupService {

    @Value("${file.cleanup.output.directory:src/main/resources/output}")
    private String outputDir;
    
    @Value("${file.cleanup.max-age-hours:24}")
    private long maxAgeHours;
    
    /**
     * Executa limpeza automatica conforme configurado no application.properties
     */
    @Scheduled(cron = "${file.cleanup.schedule.cron:0 0 */6 * * *}")
    public void cleanupOldFiles() {
        try {
            Path outputPath = Paths.get(outputDir);
            
            if (!Files.exists(outputPath)) {
                System.out.println("Output directory does not exist: " + outputDir);
                return;
            }

            long currentTime = System.currentTimeMillis();
            long maxAgeMillis = TimeUnit.HOURS.toMillis(maxAgeHours);
            int deletedCount = 0;

            System.out.println("Starting file cleanup in: " + outputDir);
            System.out.println("Deleting files older than " + maxAgeHours + " hours");

            // Percorrer todos os arquivos no diretório
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(outputPath, "signed_*.pdf")) {
                for (Path file : stream) {
                    try {
                        BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
                        long fileAge = currentTime - attrs.creationTime().toMillis();

                        if (fileAge > maxAgeMillis) {
                            Files.delete(file);
                            deletedCount++;
                            System.out.println("Deleted old file: " + file.getFileName());
                        }
                    } catch (IOException e) {
                        System.err.println("Error processing file " + file.getFileName() + ": " + e.getMessage());
                    }
                }
            }

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            System.out.println("[" + timestamp + "] File cleanup completed. Deleted " + deletedCount + " files.");

        } catch (IOException e) {
            System.err.println("Error during file cleanup: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Método para limpeza manual (pode ser chamado via endpoint se necessário)
     */
    public int cleanupFilesOlderThan(long hours) {
        try {
            Path outputPath = Paths.get(outputDir);
            
            if (!Files.exists(outputPath)) {
                return 0;
            }

            long currentTime = System.currentTimeMillis();
            long maxAgeMillis = TimeUnit.HOURS.toMillis(hours);
            int deletedCount = 0;

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(outputPath, "signed_*.pdf")) {
                for (Path file : stream) {
                    try {
                        BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
                        long fileAge = currentTime - attrs.creationTime().toMillis();

                        if (fileAge > maxAgeMillis) {
                            Files.delete(file);
                            deletedCount++;
                        }
                    } catch (IOException e) {
                        System.err.println("Error deleting file " + file.getFileName() + ": " + e.getMessage());
                    }
                }
            }

            return deletedCount;

        } catch (IOException e) {
            System.err.println("Error during manual cleanup: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Limpar todos os arquivos assinados (usar com cuidado!)
     */
    public int cleanupAllSignedFiles() {
        try {
            Path outputPath = Paths.get(outputDir);
            
            if (!Files.exists(outputPath)) {
                return 0;
            }

            int deletedCount = 0;

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(outputPath, "signed_*.pdf")) {
                for (Path file : stream) {
                    try {
                        Files.delete(file);
                        deletedCount++;
                        System.out.println("Deleted file: " + file.getFileName());
                    } catch (IOException e) {
                        System.err.println("Error deleting file " + file.getFileName() + ": " + e.getMessage());
                    }
                }
            }

            return deletedCount;

        } catch (IOException e) {
            System.err.println("Error during cleanup all: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Obter estatísticas dos arquivos
     */
    public String getFileStatistics() {
        try {
            Path outputPath = Paths.get(outputDir);
            
            if (!Files.exists(outputPath)) {
                return "Output directory does not exist";
            }

            int totalFiles = 0;
            int oldFiles = 0;
            long totalSize = 0;
            long currentTime = System.currentTimeMillis();
            long maxAgeMillis = TimeUnit.HOURS.toMillis(maxAgeHours);

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(outputPath, "signed_*.pdf")) {
                for (Path file : stream) {
                    try {
                        BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
                        totalFiles++;
                        totalSize += attrs.size();
                        
                        long fileAge = currentTime - attrs.creationTime().toMillis();
                        if (fileAge > maxAgeMillis) {
                            oldFiles++;
                        }
                    } catch (IOException e) {
                        System.err.println("Error reading file stats for " + file.getFileName());
                    }
                }
            }

            double totalSizeMB = totalSize / (1024.0 * 1024.0);
            
            return String.format(
                "File Statistics:\n" +
                "- Total files: %d\n" +
                "- Files older than %d hours: %d\n" +
                "- Total size: %.2f MB\n" +
                "- Directory: %s",
                totalFiles, maxAgeHours, oldFiles, totalSizeMB, outputDir
            );

        } catch (IOException e) {
            return "Error getting file statistics: " + e.getMessage();
        }
    }
}
