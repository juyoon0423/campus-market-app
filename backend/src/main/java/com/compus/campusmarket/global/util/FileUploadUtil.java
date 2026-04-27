package com.compus.campusmarket.global.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Component
public class FileUploadUtil {

    @Value("${file.upload-dir}")
    private String uploadDir;

    public String saveFile(MultipartFile multipartFile) throws IOException {
        if (multipartFile.isEmpty()) return null;

        // 1. 저장할 폴더가 없으면 생성
        File directory = new File(uploadDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // 2. 파일명 중복 방지를 위해 UUID 생성
        String originalFilename = multipartFile.getOriginalFilename();
        String storeFilename = UUID.randomUUID().toString() + "_" + originalFilename;

        // 3. 실제 파일 저장
        File saveFile = new File(uploadDir + storeFilename);
        multipartFile.transferTo(saveFile);

        // 4. DB에 저장할 파일명(또는 경로) 반환
        return storeFilename;
    }
}