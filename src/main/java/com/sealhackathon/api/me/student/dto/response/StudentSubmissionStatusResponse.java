package com.sealhackathon.api.me.student.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentSubmissionStatusResponse {

    private Integer submissionId;
    private Integer roundId;
    private String repoUrl;
    private String demoUrl;
    private String slideUrl;
    /** Tên file PDF đã lưu — null nếu chưa upload thành công. */
    private String slideFile;
    /** API tải slide — null nếu chưa có file. */
    private String slideDownloadPath;
    private Boolean hasSlide;
    private String status;
    private LocalDateTime submittedAt;
}
