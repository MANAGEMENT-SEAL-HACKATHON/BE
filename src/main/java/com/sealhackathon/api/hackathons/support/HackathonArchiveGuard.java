package com.sealhackathon.api.hackathons.support;

import com.sealhackathon.api.common.exception.ConflictException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.criteria.entity.Criteria;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.tracks.entity.Track;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Chặn mọi mutation khi hackathon ở {@link HackathonStatus#FINISHED} (lưu trữ / chỉ đọc).
 *
 * <p>Không gọi từ timeline validator — chỉ từ service CRUD.
 */
@Component
public class HackathonArchiveGuard {

    public void assertNotArchived(Hackathon hackathon) {
        if (hackathon == null || hackathon.getStatus() != HackathonStatus.FINISHED) {
            return;
        }
        throw new ConflictException(ErrorCode.HACKATHON_ARCHIVED,
                "Hackathon đã kết thúc — chỉ xem lịch sử, không thể thay đổi",
                Map.of(
                        "hackathonId", hackathon.getId(),
                        "status", hackathon.getStatus().name()));
    }

    public void assertNotArchivedForRound(Round round) {
        if (round != null) {
            assertNotArchived(round.getHackathon());
        }
    }

    public void assertNotArchivedForTrack(Track track) {
        if (track != null && track.getRound() != null) {
            assertNotArchived(track.getRound().getHackathon());
        }
    }

    public void assertNotArchivedForCriteria(Criteria criteria) {
        if (criteria == null) {
            return;
        }
        if (criteria.getTrack() != null) {
            assertNotArchivedForTrack(criteria.getTrack());
        } else if (criteria.getRound() != null) {
            assertNotArchivedForRound(criteria.getRound());
        }
    }
}
