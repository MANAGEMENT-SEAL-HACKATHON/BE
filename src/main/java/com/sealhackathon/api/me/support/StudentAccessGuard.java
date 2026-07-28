package com.sealhackathon.api.me.support;

import com.sealhackathon.api.common.exception.AuthException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.hackathons.repository.HackathonRegistrationRepository;
import com.sealhackathon.api.teams.entity.TeamMember;
import com.sealhackathon.api.teams.entity.TeamMemberId;
import com.sealhackathon.api.teams.repository.TeamMemberRepository;
import com.sealhackathon.api.teams.value_object.TeamMemberRole;
import com.sealhackathon.api.teams.value_object.TeamMemberStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Pháo đài bảo mật cho Student Portal (Thực thi phân quyền Leader vs Member).
 * Chặn đứng mọi hành vi IDOR (Insecure Direct Object Reference).
 */
@Component
@RequiredArgsConstructor
public class StudentAccessGuard {

    private final CurrentUserAccessor currentUserAccessor;
    private final TeamMemberRepository teamMemberRepository;
    private final HackathonRegistrationRepository hackathonRegistrationRepository;

    /**
     * GATE 1: Kiểm tra xem Student đã ĐĂNG KÝ tham gia Hackathon này chưa.
     * Dùng cho các API xem sự kiện, bảng xếp hạng cấp Hackathon.
     */
    public void assertRegisteredForHackathon(Integer hackathonId) {
        Integer userId = currentUserAccessor.currentUserId();

        if (!hackathonRegistrationRepository.existsByHackathon_IdAndUser_Id(hackathonId, userId)) {
            throw new AuthException(ErrorCode.FORBIDDEN,
                    "Truy cập bị từ chối: Bạn chưa đăng ký tham gia kỳ Hackathon này.",
                    HttpStatus.FORBIDDEN);
        }
    }

    /**
     * GATE 1b: Student đã đăng ký HOẶC từng/đang là member ACCEPTED của đội trong hackathon
     * (kể cả team ELIMINATED) — dùng cho leaderboard/rankings.
     */
    public void assertParticipatedInHackathon(Integer hackathonId) {
        Integer userId = currentUserAccessor.currentUserId();
        if (hackathonRegistrationRepository.existsByHackathon_IdAndUser_Id(hackathonId, userId)) {
            return;
        }
        if (teamMemberRepository.existsAcceptedMembershipInHackathon(userId, hackathonId)) {
            return;
        }
        throw new AuthException(ErrorCode.FORBIDDEN,
                "Truy cập bị từ chối: Bạn không thuộc kỳ Hackathon này.",
                HttpStatus.FORBIDDEN);
    }

    /**
     * GATE 2: Kiểm tra quyền THÀNH VIÊN ĐỘI (Read-Only).
     * Dùng cho các API xem thông tin đội, xem bài nộp, lấy đề bài (FR-U-14, 17, 20).
     */
    public void assertTeamMember(Integer teamId) {
        Integer userId = currentUserAccessor.currentUserId();

        // Kiểm tra xem User có nằm trong đội và trạng thái phải là ACCEPTED (Đã chấp nhận lời mời)
        if (!teamMemberRepository.existsByUser_IdAndTeam_IdAndStatus(userId, teamId, TeamMemberStatus.ACCEPTED)) {
            throw new AuthException(ErrorCode.NOT_TEAM_MEMBER,
                    "Truy cập bị từ chối: Bạn không phải là thành viên chính thức của đội này.",
                    HttpStatus.FORBIDDEN);
        }
    }

    /**
     * GATE 3: Kiểm tra quyền NHÓM TRƯỞNG (Mutation - Active Writer).
     * Dùng cho các API cực kỳ nhạy cảm: Nộp bài, Đổi Track, Khiếu nại (FR-U-16, 18, 19, 30).
     */
    public void assertTeamLeader(Integer teamId) {
        Integer userId = currentUserAccessor.currentUserId();

        // 1. Tìm bản ghi TeamMember của User này trong Đội
        TeamMember member = teamMemberRepository.findById(new TeamMemberId(teamId, userId))
                .orElseThrow(() -> new AuthException(ErrorCode.NOT_TEAM_MEMBER,
                        "Truy cập bị từ chối: Bạn không thuộc đội này.",
                        HttpStatus.FORBIDDEN));

        // 2. Chặn nếu chỉ mới được mời (INVITED) mà chưa ACCEPTED
        if (member.getStatus() != TeamMemberStatus.ACCEPTED) {
            throw new AuthException(ErrorCode.NOT_TEAM_MEMBER,
                    "Truy cập bị từ chối: Bạn chưa phải là thành viên chính thức của đội này.",
                    HttpStatus.FORBIDDEN);
        }

        // 3. Chặn nếu chỉ là MEMBER thường
        if (member.getRoleInTeam() != TeamMemberRole.LEADER) {
            throw new AuthException(ErrorCode.FORBIDDEN,
                    "Truy cập bị từ chối: Chỉ Nhóm trưởng (Leader) mới có quyền thực hiện hành động này.",
                    HttpStatus.FORBIDDEN);
        }
    }
}