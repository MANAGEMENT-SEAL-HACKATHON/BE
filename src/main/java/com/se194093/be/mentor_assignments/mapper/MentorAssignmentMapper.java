package com.se194093.be.mentor_assignments.mapper;

import com.se194093.be.mentor_assignments.dto.response.MentorAssignmentResponse;
import com.se194093.be.mentor_assignments.entity.MentorAssignment;
import org.springframework.stereotype.Component;

@Component
public class MentorAssignmentMapper {

    public MentorAssignmentResponse toResponse(MentorAssignment e) {
        if (e == null) {
            return null;
        }
        return MentorAssignmentResponse.builder()
                .id(e.getId())
                .mentorId(e.getMentor() == null ? null : e.getMentor().getId())
                .mentorFullName(e.getMentor() == null ? null : e.getMentor().getFullName())
                .mentorEmail(e.getMentor() == null ? null : e.getMentor().getEmail())
                .trackId(e.getTrack() == null ? null : e.getTrack().getId())
                .trackName(e.getTrack() == null ? null : e.getTrack().getName())
                .assignedAt(e.getAssignedAt())
                .assignedById(e.getAssignedBy() == null ? null : e.getAssignedBy().getId())
                .build();
    }
}
