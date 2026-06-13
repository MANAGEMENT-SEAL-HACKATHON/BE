package com.sealhackathon.api.certificates.support;

import com.sealhackathon.api.certificates.entity.Certificate;
import com.sealhackathon.api.me.student.dto.response.CertificateDownload;
import com.sealhackathon.api.storage.ObjectStorageService;
import com.sealhackathon.api.storage.StoredObject;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;

@Component
@RequiredArgsConstructor
public class CertificateFileResolver {

    private final ObjectStorageService objectStorage;

    public CertificateDownload resolve(Certificate cert) {
        String storageKey = resolveStorageKey(cert);
        if (storageKey != null && objectStorage.exists(storageKey)) {
            StoredObject stored = objectStorage.get(storageKey);
            return new CertificateDownload(stored, filenameFor(cert));
        }
        byte[] pdf = CertificatePdfSupport.minimalPdf(
                cert.getHackathon().getName(),
                cert.getUser().getFullName());
        StoredObject generated = new StoredObject(
                new ByteArrayInputStream(pdf),
                "application/pdf",
                pdf.length);
        return new CertificateDownload(generated, filenameFor(cert));
    }

    private String resolveStorageKey(Certificate cert) {
        String fileUrl = cert.getFileUrl();
        if (fileUrl == null || fileUrl.isBlank()) {
            return null;
        }
        if (fileUrl.startsWith("http://") || fileUrl.startsWith("https://")) {
            return null;
        }
        return fileUrl;
    }

    public static String filenameFor(Certificate cert) {
        String hackathon = cert.getHackathon().getSlug() != null
                ? cert.getHackathon().getSlug()
                : "hackathon-" + cert.getHackathon().getId();
        return "certificate-" + hackathon + "-" + cert.getId() + ".pdf";
    }
}
