package com.sealhackathon.api.me.service;

import com.sealhackathon.api.me.dto.request.AssignmentDeclineRequest;
import com.sealhackathon.api.me.dto.response.AssignmentResponseStatusResponse;

public interface AssignmentResponseService {

    AssignmentResponseStatusResponse declineJudgeAssignment(Integer assignmentId, AssignmentDeclineRequest request);

    AssignmentResponseStatusResponse acceptJudgeAssignment(Integer assignmentId);

    AssignmentResponseStatusResponse declineMentorAssignment(Integer assignmentId, AssignmentDeclineRequest request);

    AssignmentResponseStatusResponse acceptMentorAssignment(Integer assignmentId);

    AssignmentResponseStatusResponse declineMentorTeamAssignment(Integer assignmentId, AssignmentDeclineRequest request);

    AssignmentResponseStatusResponse acceptMentorTeamAssignment(Integer assignmentId);
}
