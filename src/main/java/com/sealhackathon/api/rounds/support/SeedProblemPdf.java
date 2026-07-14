package com.sealhackathon.api.rounds.support;

import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * PDF đề bài seed — ưu tiên file classpath {@code seed/HistAR_CP3Part2_EXE101.pdf},
 * fallback PDF tối thiểu nếu thiếu.
 */
public final class SeedProblemPdf {

    public static final String CLASSPATH_RESOURCE = "seed/HistAR_CP3Part2_EXE101.pdf";
    public static final String DISPLAY_FILENAME = "HistAR_CP3Part2_EXE101.pdf";

    private static final byte[] MINIMAL_PDF = (
            "%PDF-1.4\n"
                    + "1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n"
                    + "2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj\n"
                    + "3 0 obj<</Type/Page/MediaBox[0 0 612 792]/Parent 2 0 R>>endobj\n"
                    + "xref\n0 4\n0000000000 65535 f \n0000000009 00000 n \n0000000052 00000 n \n0000000101 00000 n \n"
                    + "trailer<</Size 4/Root 1 0 R>>\nstartxref\n178\n%%EOF")
            .getBytes(StandardCharsets.US_ASCII);

    private static final byte[] CACHED_BYTES;
    private static final String CACHED_FILENAME;

    static {
        byte[] loaded = null;
        String name = DISPLAY_FILENAME;
        try {
            ClassPathResource resource = new ClassPathResource(CLASSPATH_RESOURCE);
            if (resource.exists()) {
                try (InputStream in = resource.getInputStream()) {
                    loaded = StreamUtils.copyToByteArray(in);
                }
            }
        } catch (IOException ignored) {
            loaded = null;
        }
        if (loaded == null || loaded.length < 5) {
            loaded = MINIMAL_PDF;
            name = "de-bai-seed.pdf";
        }
        CACHED_BYTES = loaded;
        CACHED_FILENAME = name;
    }

    private SeedProblemPdf() {
    }

    public static byte[] bytes() {
        return CACHED_BYTES;
    }

    public static String displayFilename() {
        return CACHED_FILENAME;
    }

    public static long size() {
        return CACHED_BYTES.length;
    }
}
