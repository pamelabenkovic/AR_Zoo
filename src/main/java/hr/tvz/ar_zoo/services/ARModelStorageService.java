package hr.tvz.ar_zoo.services;

import hr.tvz.ar_zoo.models.ARModel;
import hr.tvz.ar_zoo.repositories.ARModelRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class ARModelStorageService {

    @Value("${app.model.upload-dir}")
    private String uploadDir;

    private final ARModelRepository repository;

    public ARModelStorageService(ARModelRepository repository) {
        this.repository = repository;
    }

    public ARModel saveModel(String name, String modelId, String message, MultipartFile zipFile) throws IOException {
        String folderName = UUID.randomUUID().toString();
        File targetDir = new File(uploadDir, folderName);
        targetDir.mkdirs();

        try (ZipInputStream zis = new ZipInputStream(zipFile.getInputStream())) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File newFile = new File(targetDir, entry.getName());
                if (entry.isDirectory()) {
                    newFile.mkdirs();
                } else {
                    newFile.getParentFile().mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
            }
        }

        File gltfFile = Files.walk(targetDir.toPath())
                .filter(p -> p.toString().endsWith(".gltf"))
                .map(java.nio.file.Path::toFile)
                .findFirst()
                .orElse(null);

        String modelUrl = gltfFile != null ? "/models/" + folderName + "/" + gltfFile.getName() : null;

        ARModel model = new ARModel();
        model.setName(name);
        model.setModelId(modelId);
        model.setMessage(message);
        model.setSizeMb(zipFile.getSize() / 1024.0 / 1024.0);
        model.setModelUrl(modelUrl);

        return repository.save(model);
    }

}