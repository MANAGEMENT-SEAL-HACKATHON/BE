package com.sealhackathon.api.certificates.support;

import java.nio.charset.StandardCharsets;

/** PDF tối giản khi chưa có file lưu trữ — đủ mở/tải trên trình duyệt. */
public final class CertificatePdfSupport {

    private CertificatePdfSupport() {
    }

    public static byte[] minimalPdf(String title, String recipient) {
        String safeTitle = sanitize(title);
        String safeRecipient = sanitize(recipient);
        String text = "SEAL Hackathon Certificate - " + safeTitle + " - " + safeRecipient;
        String stream = "BT /F1 14 Tf 50 700 Td (" + escapePdf(text) + ") Tj ET";
        int streamLen = stream.getBytes(StandardCharsets.US_ASCII).length;

        String pdf = """
                %PDF-1.4
                1 0 obj<< /Type /Catalog /Pages 2 0 R >>endobj
                2 0 obj<< /Type /Pages /Kids [3 0 R] /Count 1 >>endobj
                3 0 obj<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R /Resources<< /Font<< /F1 5 0 R >> >> >>endobj
                4 0 obj<< /Length %d >>stream
                %s
                endstream
                endobj
                5 0 obj<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>endobj
                xref
                0 6
                0000000000 65535 f
                0000000009 00000 n
                0000000058 00000 n
                0000000115 00000 n
                0000000240 00000 n
                0000000%03d 00000 n
                trailer<< /Size 6 /Root 1 0 R >>
                startxref
                %d
                %%EOF
                """.formatted(streamLen, stream, 300 + streamLen, 380 + streamLen);

        return pdf.getBytes(StandardCharsets.US_ASCII);
    }

    private static String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "Participant";
        }
        return value.replaceAll("[\\r\\n]", " ").trim();
    }

    private static String escapePdf(String text) {
        return text.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }
}
