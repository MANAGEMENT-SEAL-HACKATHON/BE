package com.sealhackathon.api.invitations.service.impl;

import com.sealhackathon.api.config.MailProperties;
import com.sealhackathon.api.invitations.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Gửi email SMTP thật khi {@code app.mail.enabled=true}. Cần cấu hình {@code spring.mail.*}.
 * Khi tắt (mặc định) Spring dùng {@link NoOpEmailServiceImpl}.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "true")
public class SmtpEmailServiceImpl implements EmailService {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("HH:mm 'ngày' dd/MM/yyyy");

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;

    @Override
    public void sendGuestJudgeInvitation(String email, String fullName, String tempPassword,
                                         String loginUrl, LocalDateTime expiresAt) {
        String body = paragraph("Xin chào <b>" + escape(fullName) + "</b>,")
                + paragraph("Bạn được mời tham gia chấm thi với vai trò <b>Giám khảo khách mời</b> trên hệ thống SEAL Hackathon.")
                + credentialsBox(email, tempPassword)
                + paragraph("Lời mời có hiệu lực đến <b>" + formatDate(expiresAt) + "</b>. "
                    + "Vui lòng đăng nhập và đổi mật khẩu trước thời hạn này.")
                + button(loginUrl, "Đăng nhập ngay");
        send(email, "Lời mời làm Giám khảo — SEAL Hackathon", wrap("Lời mời làm Giám khảo", body));
    }

    @Override
    public void resendGuestJudgeInvitation(String email, String fullName, String tempPassword,
                                           String loginUrl, LocalDateTime expiresAt) {
        String body = paragraph("Xin chào <b>" + escape(fullName) + "</b>,")
                + paragraph("Lời mời làm Giám khảo của bạn đã được gửi lại với mật khẩu tạm mới.")
                + credentialsBox(email, tempPassword)
                + paragraph("Lời mời có hiệu lực đến <b>" + formatDate(expiresAt) + "</b>.")
                + button(loginUrl, "Đăng nhập ngay");
        send(email, "Lời mời làm Giám khảo (gửi lại) — SEAL Hackathon",
                wrap("Lời mời làm Giám khảo", body));
    }

    @Override
    public void sendRegistrationWelcome(String email, String fullName, String loginUrl) {
        String body = paragraph("Xin chào <b>" + escape(fullName) + "</b>,")
                + paragraph("Cảm ơn bạn đã đăng ký tài khoản SEAL Hackathon. Tài khoản đã được tạo thành công.")
                + paragraph("Bước tiếp theo: đăng nhập, hoàn thiện hồ sơ và tải thẻ sinh viên để chờ Ban tổ chức phê duyệt.")
                + button(loginUrl, "Đăng nhập & hoàn thiện hồ sơ");
        send(email, "Chào mừng bạn đến với SEAL Hackathon", wrap("Đăng ký thành công", body));
    }

    @Override
    public void sendEmailVerification(String email, String fullName, String verifyUrl,
                                      LocalDateTime expiresAt) {
        String body = paragraph("Xin chào <b>" + escape(fullName) + "</b>,")
                + paragraph("Cảm ơn bạn đã đăng ký tài khoản SEAL Hackathon. "
                    + "Vui lòng xác thực email để có thể đăng nhập và hoàn thiện hồ sơ.")
                + button(verifyUrl, "Xác thực email")
                + paragraph("Link có hiệu lực đến <b>" + formatDate(expiresAt) + "</b>. "
                    + "Nếu bạn không đăng ký, hãy bỏ qua email này.")
                + paragraph("<span style=\"color:#888;font-size:12px\">Nếu nút không hoạt động, sao chép liên kết: "
                    + escape(verifyUrl) + "</span>");
        send(email, "Xác thực email — SEAL Hackathon", wrap("Xác thực email", body));
    }

    @Override
    public void sendPasswordReset(String email, String fullName, String resetUrl,
                                  LocalDateTime expiresAt) {
        String body = paragraph("Xin chào <b>" + escape(fullName) + "</b>,")
                + paragraph("Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản này. "
                    + "Nhấn nút bên dưới để tạo mật khẩu mới.")
                + button(resetUrl, "Đặt lại mật khẩu")
                + paragraph("Link có hiệu lực đến <b>" + formatDate(expiresAt) + "</b>. "
                    + "Nếu bạn không yêu cầu, hãy bỏ qua email này.")
                + paragraph("<span style=\"color:#888;font-size:12px\">Nếu nút không hoạt động, sao chép liên kết: "
                    + escape(resetUrl) + "</span>");
        send(email, "Đặt lại mật khẩu — SEAL Hackathon", wrap("Đặt lại mật khẩu", body));
    }

    @Override
    public void sendMentorAssignment(String email, String fullName, String trackName,
                                     String hackathonName, String loginUrl) {
        String body = paragraph("Xin chào <b>" + escape(fullName) + "</b>,")
                + paragraph("Bạn vừa được phân công làm <b>Mentor</b> cho bảng đấu <b>"
                    + escape(trackName) + "</b>"
                    + (hackathonName == null ? "" : " thuộc sự kiện <b>" + escape(hackathonName) + "</b>") + ".")
                + paragraph("Vui lòng đăng nhập để xem chi tiết và bắt đầu hỗ trợ các đội thi.")
                + button(loginUrl, "Xem phân công");
        send(email, "Bạn được phân công làm Mentor — SEAL Hackathon",
                wrap("Phân công Mentor", body));
    }

    @Override
    public void sendJudgeAssignment(String email, String fullName, String assignmentLabel,
                                    String hackathonName, String loginUrl) {
        String body = paragraph("Xin chào <b>" + escape(fullName) + "</b>,")
                + paragraph("Bạn vừa được phân công làm <b>Giám khảo</b> cho <b>"
                    + escape(assignmentLabel) + "</b>"
                    + (hackathonName == null ? "" : " thuộc sự kiện <b>" + escape(hackathonName) + "</b>") + ".")
                + paragraph("Vui lòng đăng nhập để xem tiêu chí chấm và lịch chấm thi.")
                + button(loginUrl, "Xem phân công");
        send(email, "Bạn được phân công làm Giám khảo — SEAL Hackathon",
                wrap("Phân công Giám khảo", body));
    }

    private void send(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(mailProperties.getFrom(), mailProperties.getFromName());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("[EmailService] sent '{}' to {}", subject, to);
        } catch (MessagingException | UnsupportedEncodingException | MailException ex) {
            log.error("[EmailService] failed to send '{}' to {}: {}", subject, to, ex.getMessage());
            throw new IllegalStateException("Không gửi được email tới " + to, ex);
        }
    }

    private static String formatDate(LocalDateTime dateTime) {
        return dateTime == null ? "—" : DATE_FMT.format(dateTime);
    }

    private static String paragraph(String html) {
        return "<p style=\"margin:0 0 16px;line-height:1.6;color:#333;font-size:14px\">" + html + "</p>";
    }

    private static String credentialsBox(String email, String tempPassword) {
        return "<div style=\"background:#f6f8fa;border:1px solid #e1e4e8;border-radius:8px;"
                + "padding:16px;margin:0 0 16px;font-size:14px;color:#333\">"
                + "<div style=\"margin-bottom:8px\">Email đăng nhập: <b>" + escape(email) + "</b></div>"
                + "<div>Mật khẩu tạm: <b style=\"font-family:monospace\">" + escape(tempPassword) + "</b></div>"
                + "</div>";
    }

    private static String button(String url, String label) {
        return "<div style=\"margin:24px 0\"><a href=\"" + escape(url) + "\" "
                + "style=\"background:#1677ff;color:#fff;text-decoration:none;padding:12px 24px;"
                + "border-radius:8px;font-size:14px;font-weight:600;display:inline-block\">"
                + escape(label) + "</a></div>";
    }

    private static String wrap(String heading, String body) {
        return "<!DOCTYPE html><html lang=\"vi\"><body style=\"margin:0;background:#f0f2f5;"
                + "font-family:-apple-system,Segoe UI,Roboto,Arial,sans-serif\">"
                + "<div style=\"max-width:560px;margin:0 auto;padding:24px\">"
                + "<div style=\"background:#fff;border-radius:12px;padding:32px;"
                + "box-shadow:0 4px 12px rgba(0,0,0,0.06)\">"
                + "<h2 style=\"margin:0 0 24px;color:#1677ff;font-size:20px\">" + escape(heading) + "</h2>"
                + body
                + "<hr style=\"border:none;border-top:1px solid #eee;margin:24px 0\"/>"
                + "<p style=\"margin:0;color:#999;font-size:12px\">Email tự động từ hệ thống SEAL Hackathon. "
                + "Vui lòng không trả lời email này.</p>"
                + "</div></div></body></html>";
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
