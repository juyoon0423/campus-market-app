package com.compus.campusmarket.global.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Component
public class FileUploadUtil {

    private static final List<String> ALLOWED_EXTENSIONS = List.of("jpg", "jpeg", "png", "gif", "webp");

    @Value("${file.upload-dir}")
    private String uploadDir;

    public String saveFile(MultipartFile multipartFile) throws IOException {
        if (multipartFile.isEmpty()) return null;

        // 원본 파일명은 저장 경로에 그대로 쓰지 않는다 — "../"가 섞여 들어오면 업로드 디렉터리
        // 밖에 파일을 쓸 수 있는 경로 조작 취약점이 되므로, 확장자만 화이트리스트로 검증해서 뽑아 쓴다.
        String extension = extractAllowedExtension(multipartFile.getOriginalFilename());
        String storeFilename = UUID.randomUUID() + "." + extension;

        File directory = new File(uploadDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        File saveFile = new File(uploadDir, storeFilename);
        multipartFile.transferTo(saveFile);

        return storeFilename;
    }

    private String extractAllowedExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new IllegalArgumentException("파일 확장자를 확인할 수 없습니다.");
        }
        String extension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1)
                .toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("이미지 파일(jpg, jpeg, png, gif, webp)만 업로드할 수 있습니다.");
        }
        return extension;
    }
}