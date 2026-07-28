package com.sealhackathon.api.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PresentationDurationResponse {

    private Integer roundId;
    /** Null khi scope = ROUND (GĐ5 hoặc default sơ loại). */
    private Integer trackId;
    /** {@code ROUND} — lưu trên round; {@code TRACK} — override trên track (GĐ3). */
    private String scope;
    /** Giá trị đang lưu tại scope (track có thể null = chưa override). */
    private Integer presentationMinutes;
    private Integer qaMinutes;
    /** Thời lượng timer thực tế sau khi resolve (track override → round default → 10/5). */
    private Integer effectivePresentationMinutes;
    private Integer effectiveQaMinutes;
}
