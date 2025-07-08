package pt.ipvc.cartao.ccauth.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.ipvc.cartao.ccauth.service.FileCleanupService;

@RestController
@RequestMapping("/api/admin")
public class FileCleanupController {

    @Autowired
    private FileCleanupService fileCleanupService;

    /**
     * Endpoint para limpeza manual de arquivos antigos
     * GET /api/admin/cleanup?hours=24
     */
    @GetMapping("/cleanup")
    public ResponseEntity<String> cleanupFiles(@RequestParam(defaultValue = "24") long hours) {
        try {
            int deletedCount = fileCleanupService.cleanupFilesOlderThan(hours);
            
            String message = String.format(
                "Limpeza concluída com sucesso!\n" +
                "- Arquivos deletados: %d\n" +
                "- Critério: mais antigos que %d horas",
                deletedCount, hours
            );
            
            return ResponseEntity.ok(message);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erro durante a limpeza: " + e.getMessage());
        }
    }

    /**
     * Endpoint para obter estatísticas dos arquivos
     * GET /api/admin/file-stats
     */
    @GetMapping("/file-stats")
    public ResponseEntity<String> getFileStatistics() {
        try {
            String stats = fileCleanupService.getFileStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erro ao obter estatísticas: " + e.getMessage());
        }
    }

    /**
     * Endpoint para limpeza completa (CUIDADO!)
     * DELETE /api/admin/cleanup-all
     */
    @DeleteMapping("/cleanup-all")
    public ResponseEntity<String> cleanupAllFiles() {
        try {
            int deletedCount = fileCleanupService.cleanupAllSignedFiles();
            
            String message = String.format(
                "Limpeza total concluída!\n" +
                "- Todos os arquivos assinados foram deletados: %d arquivos",
                deletedCount
            );
            
            return ResponseEntity.ok(message);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erro durante a limpeza total: " + e.getMessage());
        }
    }

    /**
     * Endpoint para forçar limpeza automática agora
     * POST /api/admin/force-cleanup
     */
    @PostMapping("/force-cleanup")
    public ResponseEntity<String> forceCleanup() {
        try {
            fileCleanupService.cleanupOldFiles();
            return ResponseEntity.ok("Limpeza automática executada com sucesso!");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erro ao executar limpeza: " + e.getMessage());
        }
    }
}
