package gg.duo.repository;

import gg.duo.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long> {
    List<Application> findByPostIdOrderByCreatedAtDesc(Long postId);
    List<Application> findByApplicantIdOrderByCreatedAtDesc(Long applicantId);
    Optional<Application> findByPostIdAndApplicantId(Long postId, Long applicantId);
    boolean existsByPostIdAndApplicantId(Long postId, Long applicantId);
    long countByPostIdAndStatus(Long postId, Application.Status status);
    void deleteByPostId(Long postId);
}
