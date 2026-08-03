package com.compus.campusmarket.global.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileUploadUtilTest {

    @TempDir
    Path uploadDir;

    private FileUploadUtil fileUploadUtil;

    @BeforeEach
    void setUp() {
        fileUploadUtil = new FileUploadUtil();
        ReflectionTestUtils.setField(fileUploadUtil, "uploadDir", uploadDir.toString() + File.separator);
    }

    @Test
    void 허용된_확장자는_저장에_성공하고_원본_파일명을_노출하지_않는다() throws Exception {
        MockMultipartFile file = new MockMultipartFile("image", "profile.png", "image/png", "content".getBytes());

        String storedName = fileUploadUtil.saveFile(file);

        assertThat(storedName).endsWith(".png");
        assertThat(storedName).doesNotContain("profile");
        assertThat(uploadDir.resolve(storedName)).exists();
    }

    @Test
    void 허용되지_않은_확장자는_거부한다() {
        MockMultipartFile file = new MockMultipartFile("image", "malware.jsp", "text/plain", "content".getBytes());

        assertThatThrownBy(() -> fileUploadUtil.saveFile(file))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 경로_조작이_섞인_파일명이어도_업로드_디렉터리_밖에는_아무것도_쓰지_않는다() throws Exception {
        MockMultipartFile file = new MockMultipartFile("image", "../../../../etc/evil.png", "image/png", "content".getBytes());

        String storedName = fileUploadUtil.saveFile(file);

        // 원본 파일명을 저장 경로에 전혀 쓰지 않으므로 uploadDir 바로 아래에만 저장된다.
        assertThat(storedName).doesNotContain("/").doesNotContain("..");
        assertThat(uploadDir.resolve(storedName)).exists();
        assertThat(uploadDir.getParent().resolve("evil.png")).doesNotExist();
    }

    @Test
    void 확장자가_없는_파일명은_거부한다() {
        MockMultipartFile file = new MockMultipartFile("image", "noextension", "image/png", "content".getBytes());

        assertThatThrownBy(() -> fileUploadUtil.saveFile(file))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
