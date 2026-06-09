package com.sealhackathon.api.me.judge.dto.response;

import com.sealhackathon.api.submissions.value_object.SubmissionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JudgeSubmissionListItemResponse {

    private Integer submissionId;
    private String displayCode;
    private Integer trackId;
    private String trackName;
    private SubmissionStatus status;
    /** Tên file PDF gốc khi đã nộp (vd. `pitch-v3.pdf`) — null nếu chưa có slide. */
    private String slideFile;
    private String repoUrl;
}
