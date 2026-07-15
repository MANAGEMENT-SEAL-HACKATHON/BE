package com.sealhackathon.api.submissions.support;

import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.storage.LocalFilesystemObjectStorageService;
import com.sealhackathon.api.storage.StorageProperties;
import com.sealhackathon.api.submissions.entity.Submission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubmissionSlideStorageTest {

    @TempDir
    Path tempDir;

    private SubmissionSlideStorage storage;

    @BeforeEach
    void setUp() {
        StorageProperties props = new StorageProperties();
        props.setLocalDir(tempDir.toString());
        props.setSubmissionSlideMaxMb(25);
        storage = new SubmissionSlideStorage(new LocalFilesystemObjectStorageService(props), props);
    }

    @Test
    void validatePdf_rejectsNonPdf() {
        MockMultipartFile file = new MockMultipartFile("slideFile", "x.txt", "text/plain", "hello".getBytes());
        assertThatThrownBy(() -> storage.validatePdf(file, true))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void validatePdf_acceptsOctetStreamWhenPdfMagicAndExtension() {
        byte[] pdf = "%PDF-1.4 test".getBytes();
        MockMultipartFile file = new MockMultipartFile("slideFile", "deck.pdf", "application/octet-stream", pdf);
        storage.validatePdf(file, true);
    }

    @Test
    void validatePdf_acceptsNullContentTypeWhenPdfMagicAndExtension() {
        byte[] pdf = "%PDF-1.4 test".getBytes();
        MockMultipartFile file = new MockMultipartFile("slideFile", "deck.pdf", null, pdf);
        storage.validatePdf(file, true);
    }

    @Test
    void storeAndLoadSlide_roundTrip() {
        byte[] pdf = "%PDF-1.4 test".getBytes();
        MockMultipartFile file = new MockMultipartFile("slideFile", "deck.pdf", "application/pdf", pdf);
        Submission submission = Submission.builder()
                .id(7)
                .hackathon(Hackathon.builder().id(1).build())
                .round(Round.builder().id(2).build())
                .build();
        storage.storeSlide(submission, file);
        assertThat(submission.getSlideStorageKey()).isNotBlank();
        assertThat(submission.getSlideOriginalFilename()).isEqualTo("deck.pdf");
        assertThat(SubmissionSlideStorage.displayFilename(submission)).isEqualTo("deck.pdf");
        var loaded = storage.loadSlide(submission);
        assertThat(loaded.contentType()).contains("pdf");
    }
}
