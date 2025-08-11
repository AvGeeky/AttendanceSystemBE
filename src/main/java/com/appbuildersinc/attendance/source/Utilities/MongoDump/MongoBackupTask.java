package com.appbuildersinc.attendance.source.Utilities.MongoDump;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
@Component
public class MongoBackupTask {
    @Value("${MONGO_BACKUP}")
    private String backup;

    @Value("${API_KEY}")
    private String mongoUri;

    @Value("${BACKUP_DIR:/backups/mongo}") // default if not set
    private String backupDir;

    // Runs daily at 10PM
    @Scheduled(cron = "0 0 22 * * ?")
    public void backupMongo() {
        if (backup == null || backup.isEmpty() || !backup.equalsIgnoreCase("true")) {
            System.out.println("Mongo backup is disabled.");
            return;
        }
        try {
            Files.createDirectories(Paths.get(backupDir));

            String dateSuffix = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
            String dumpPath = backupDir + "/dump_" + dateSuffix;

            ProcessBuilder pb = new ProcessBuilder(
                    "mongodump",
                    "--uri=" + mongoUri,
                    "--out=" + dumpPath
            );
            pb.inheritIO();

            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                System.out.println("Mongo dump created: " + dumpPath);
                deleteOldBackups();
            } else {
                System.err.println("Mongo dump failed, exit code: " + exitCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void deleteOldBackups() throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(backupDir), "dump_*")) {
            for (Path path : stream) {
                if (Files.getLastModifiedTime(path).toMillis() <
                        System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000)) { // older than 7 days
                    Files.walk(path)
                            .sorted((a, b) -> b.compareTo(a)) // delete files before dirs
                            .forEach(p -> {
                                try {
                                    Files.deleteIfExists(p);
                                    System.out.println("Deleted: " + p);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            });
                }
            }
        }
    }
}