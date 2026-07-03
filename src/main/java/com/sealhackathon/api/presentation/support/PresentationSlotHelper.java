package com.sealhackathon.api.presentation.support;

import com.sealhackathon.api.events.entity.PresentationSlot;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.teams.entity.TeamRoundTrack;
import com.sealhackathon.api.teams.entity.Team;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class PresentationSlotHelper {

    private static final DateTimeFormatter SCHEDULE_FMT =
            DateTimeFormatter.ofPattern("HH:mm");

    private PresentationSlotHelper() {}

    public static LocalDateTime resolveStart(TeamRoundTrack trt, PresentationSlot slot) {
        if (slot != null && slot.getStartsAt() != null) {
            return slot.getStartsAt();
        }
        if (trt == null) {
            return LocalDateTime.now().withSecond(0).withNano(0);
        }
        LocalDateTime base = resolveExamAt(trt);
        int offset = trt.getTeam() != null ? trt.getTeam().getId() % 10 : 0;
        return base.plusMinutes(offset * 15L);
    }

    public static LocalDateTime resolveEnd(TeamRoundTrack trt, PresentationSlot slot) {
        if (slot != null && slot.getEndsAt() != null) {
            return slot.getEndsAt();
        }
        return resolveStart(trt, slot).plusMinutes(15);
    }

    public static String resolveLocation(TeamRoundTrack trt, PresentationSlot slot) {
        if (slot != null && slot.getLocation() != null && !slot.getLocation().isBlank()) {
            return slot.getLocation();
        }
        Team team = trt != null ? trt.getTeam() : null;
        int room = team != null ? (team.getId() % 3 + 1) : 1;
        return "Online (Teams) - Phòng " + room;
    }

    public static String formatSchedule(LocalDateTime start, LocalDateTime end) {
        if (start == null) {
            return "Chưa có lịch";
        }
        String startStr = start.format(SCHEDULE_FMT);
        String endStr = end != null ? end.format(SCHEDULE_FMT) : start.plusMinutes(15).format(SCHEDULE_FMT);
        return startStr + " - " + endStr + " ngày "
                + String.format("%02d", start.getDayOfMonth()) + "/"
                + String.format("%02d", start.getMonthValue());
    }

    public static int parseGroupNumber(String assignedGroup) {
        if (assignedGroup == null || assignedGroup.isBlank()) {
            return 1;
        }
        String trimmed = assignedGroup.trim();
        if (trimmed.length() == 1 && Character.isLetter(trimmed.charAt(0))) {
            return trimmed.charAt(0) - 'A' + 1;
        }
        for (int i = trimmed.length() - 1; i >= 0; i--) {
            if (Character.isDigit(trimmed.charAt(i))) {
                try {
                    return Integer.parseInt(trimmed.substring(i));
                } catch (NumberFormatException ignored) {
                    return 1;
                }
            }
        }
        return 1;
    }

    private static LocalDateTime resolveExamAt(TeamRoundTrack trt) {
        Round round = trt.getTrack() != null ? trt.getTrack().getRound() : null;
        if (round != null && round.getExamAt() != null) {
            return round.getExamAt();
        }
        return LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);
    }
}
