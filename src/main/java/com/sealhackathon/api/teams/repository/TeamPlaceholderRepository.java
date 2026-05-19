package com.sealhackathon.api.teams.repository;

import org.springframework.stereotype.Component;

/**
 * Placeholder repository cho bảng {@code teams} (chưa có entity ở phase MF-01).
 *
 * <p><b>Mục đích:</b> các guard rule trong FR-02 (TRACK delete / update CANCELLED) tham chiếu
 * tới số team ACTIVE/PENDING; cần một bean để inject mà không phải tạo entity tạm.
 *
 * <p><b>Hiện tại:</b> mọi method luôn trả {@code 0L} → mọi guard "có team" sẽ PASS.
 * Khi entity {@code Team} được tạo (FR ngoài MF-01), thay class này bằng JpaRepository thật
 * và implement query thực; các service đang inject signature này vẫn build PASS không sửa.
 */
@Component
public class TeamPlaceholderRepository {

    /**
     * Đếm team ACTIVE/PENDING trên track. TODO: replace bằng JpaRepository thật khi có entity Team.
     */
    public long countActiveByTrackId(Integer trackId) {
        return 0L;
    }

    /**
     * Đếm team ACTIVE/PENDING trên toàn hackathon. TODO: replace khi có entity Team.
     */
    public long countActiveByHackathonId(Integer hackathonId) {
        return 0L;
    }
}
