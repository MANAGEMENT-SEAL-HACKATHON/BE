package com.sealhackathon.api.me.support;

import com.sealhackathon.api.common.exception.AuthException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.mentor_assignments.repository.MentorAssignmentRepository;
import com.sealhackathon.api.mentor_team_assignments.repository.MentorTeamAssignmentRepository;
import com.sealhackathon.api.team_round_tracks.entity.TeamRoundTrack;
import com.sealhackathon.api.team_round_tracks.repository.TeamRoundTrackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Kiểm tra phạm vi Mentor (track/team assignment).
 * Thực thi BR-M-4 và fix M-BUG-1.
 */
@Component
@RequiredArgsConstructor
public class MentorAccessGuard {

    private final CurrentUserAccessor currentUserAccessor;
    private final MentorTeamAssignmentRepository mentorTeamAssignmentRepository;
    private final MentorAssignmentRepository mentorAssignmentRepository;
    private final TeamRoundTrackRepository teamRoundTrackRepository;

    public void assertAssignedToTeam(Integer teamId) {
        Integer mentorId = currentUserAccessor.currentUserId();

        // 1. Kiểm tra cấp độ Đội (Mentor Team)
        // Tìm xem mentor này có được phân công trực tiếp cho team này ở bất kỳ vòng nào không
        boolean isDirectMentor = mentorTeamAssignmentRepository.findByTeam_IdOrderByRound_IdAsc(teamId)
                .stream()
                .anyMatch(mta -> mta.getMentor().getId().equals(mentorId));

        if (isDirectMentor) {
            return; // Cho phép đi tiếp
        }

        // 2. Kiểm tra cấp độ Track (Mentor Track)
        // Lấy danh sách các Track mà Team này đang tham gia
        List<TeamRoundTrack> teamTracks = teamRoundTrackRepository.findByTeam_Id(teamId);

        // Kiểm tra xem mentor này có quản lý Track nào trong số các Track của Team không
        boolean isTrackMentor = teamTracks.stream()
                .anyMatch(trt -> mentorAssignmentRepository.existsByMentorIdAndTrackId(mentorId, trt.getTrack().getId()));

        if (isTrackMentor) {
            return; // Cho phép đi tiếp
        }

        // Nếu rớt cả 2 điều kiện trên -> Chặn đứng!
        throw new AuthException(ErrorCode.FORBIDDEN,
                "Truy cập bị từ chối: Bạn không được phân công cố vấn (Track hoặc Team) cho đội thi này.",
                HttpStatus.FORBIDDEN);
    }
}