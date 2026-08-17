-- Run BEFORE deploying code that removes AssignmentResponseStatus.DECLINED
UPDATE judge_assignments SET response_status = 'ACCEPTED', decline_reason = NULL WHERE response_status <> 'ACCEPTED';
UPDATE mentor_assignments SET response_status = 'ACCEPTED', decline_reason = NULL WHERE response_status <> 'ACCEPTED';
UPDATE mentor_team_assignments SET response_status = 'ACCEPTED', decline_reason = NULL WHERE response_status <> 'ACCEPTED';
