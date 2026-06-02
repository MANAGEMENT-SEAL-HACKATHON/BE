package com.sealhackathon.api.certificates.repository;

import com.sealhackathon.api.certificates.entity.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CertificateRepository extends JpaRepository<Certificate, Integer> {
}
