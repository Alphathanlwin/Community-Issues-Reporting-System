package com.uit.scirs.score.repository;

import com.uit.scirs.score.entity.PointReason;
import com.uit.scirs.score.entity.PointTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {

    List<PointTransaction> findByUserIdOrderByCreatedAtDesc(Long userId);

    boolean existsByUserIdAndReportIdAndReason(Long userId, Long reportId, PointReason reason);

    @Query("SELECT COALESCE(SUM(pt.points), 0) FROM PointTransaction pt WHERE pt.user.id = :userId")
    Integer sumPointsByUserId(Long userId);
}
