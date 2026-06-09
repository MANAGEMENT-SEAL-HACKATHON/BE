package com.sealhackathon.api.submissions.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sealhackathon.api.submissions.value_object.SubmissionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubmissionResponse {

    private Integer id;
    private Integer teamId;
    private String teamName;
    private Integer trackId;
    private Integer roundId;
    private String repoUrl;
    /** Tên file PDF đã upload (multipart GĐ3). Null nếu chưa có slide lưu trữ. */
    private String slideFile;
    /** Đường API tải slide PDF — dùng GET với Bearer token. */
    private String slideDownloadPath;
    private String displayCode;
    private SubmissionStatus status;
    private Boolean isLate;
    private String lateReason;
    private Integer reviewedBy;
    private LocalDateTime reviewedAt;
    private String reviewNote;
    private LocalDateTime submittedAt;
}
