package com.sealhackathon.api.certificates.repository;

import com.sealhackathon.api.certificates.entity.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CertificateRepository extends JpaRepository<Certificate, Integer> {
    // Hàm lấy danh sách chứng nhận của một User
    List<Certificate> findByUser_Id(Integer userId);
}
