package hr.tvz.ar_zoo.controllers;

import hr.tvz.ar_zoo.models.ARModel;
import hr.tvz.ar_zoo.repositories.ARModelRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Value("${app.model.upload-dir}")
    private String uploadDir;

    private final ARModelRepository repository;

    public AdminController(ARModelRepository repository) {
        this.repository = repository;
    }

    @PostMapping("/upload")
    public ResponseEntity<ARModel> uploadModel(
            @RequestParam String name,
            @RequestParam String modelId,
            @RequestParam String message,
            @RequestParam MultipartFile file,
            @RequestParam(required = false) MultipartFile sound
    ) throws IOException {


        String folderName = UUID.randomUUID().toString();
        File targetDir = new File(uploadDir, folderName);
        targetDir.mkdirs();

        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null
                ? originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase()
                : "";

        File modelFile = null;

        if ("zip".equals(extension)) {
            try (ZipInputStream zis = new ZipInputStream(file.getInputStream())) {
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

            modelFile = Files.walk(targetDir.toPath())
                    .filter(p -> p.toString().endsWith(".gltf"))
                    .map(java.nio.file.Path::toFile)
                    .findFirst()
                    .orElse(null);

        } else if ("glb".equals(extension)) {
            modelFile = new File(targetDir, "model.glb");

            modelFile.getParentFile().mkdirs();

            file.transferTo(modelFile);
            System.out.println("📦 Spremam u: " + targetDir.getAbsolutePath());

        } else {
            return ResponseEntity.badRequest().build();
        }

        if (modelFile == null || !modelFile.exists()) {
            return ResponseEntity.status(500).body(null);
        }

        String modelUrl = "/models/" + folderName + "/" + modelFile.getName();

        String soundUrl = null;
        if (sound != null && !sound.isEmpty()) {
            String soundExt = sound.getOriginalFilename()
                    .substring(sound.getOriginalFilename().lastIndexOf('.') + 1)
                    .toLowerCase();

            if ("mp3".equals(soundExt) || "wav".equals(soundExt)) {
                File soundFile = new File(targetDir, "sound." + soundExt);
                sound.transferTo(soundFile);
                soundUrl = "/models/" + folderName + "/" + soundFile.getName();
            }
        }


        ARModel model = new ARModel();
        model.setName(name);
        model.setModelId(modelId);
        model.setMessage(message);
        model.setSizeMb(file.getSize() / 1024.0 / 1024.0);
        model.setModelUrl(modelUrl);
        model.setSoundUrl(soundUrl);


        return ResponseEntity.ok(repository.save(model));
    }


}