package com.uit.scirs.score.service;

import com.uit.scirs.common.config.CacheConfig;
import com.uit.scirs.common.exception.ResourceNotFoundException;
import com.uit.scirs.report.entity.Report;
import com.uit.scirs.score.dto.LeaderboardEntryDTO;
import com.uit.scirs.score.dto.PointTransactionDTO;
import com.uit.scirs.score.dto.ScoreSummaryDTO;
import com.uit.scirs.score.entity.PointReason;
import com.uit.scirs.score.entity.PointTransaction;
import com.uit.scirs.score.mapper.ScoreMapper;
import com.uit.scirs.score.repository.PointTransactionRepository;
import com.uit.scirs.user.entity.RoleName;
import com.uit.scirs.user.entity.User;
import com.uit.scirs.user.repository.UserRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class ScoreService {

    // Single source of truth for the Scoring Rules table in project-overview.md
    // — callers only ever say *why*, never *how many*.
    private static final Map<PointReason, Integer> POINTS_BY_REASON = new EnumMap<>(PointReason.class);

    static {
        POINTS_BY_REASON.put(PointReason.REPORT_APPROVED, 10);
        POINTS_BY_REASON.put(PointReason.REPORT_RESOLVED, 20);
        POINTS_BY_REASON.put(PointReason.FEEDBACK_GIVEN, 5);
        POINTS_BY_REASON.put(PointReason.REPORT_REJECTED, -5);
    }

    private final PointTransactionRepository pointTransactionRepository;
    private final UserRepository userRepository;
    private final ScoreMapper scoreMapper;

    public ScoreService(PointTransactionRepository pointTransactionRepository,
                         UserRepository userRepository,
                         ScoreMapper scoreMapper) {
        this.pointTransactionRepository = pointTransactionRepository;
        this.userRepository = userRepository;
        this.scoreMapper = scoreMapper;
    }

    /**
     * Idempotent: a (user, report, reason) triple is only ever awarded once,
     * so re-approving/re-resolving/re-submitting-feedback on the same report
     * never double-awards.
     */
    @Transactional
    public void award(User user, PointReason reason, Report report) {
        Long reportId = report != null ? report.getId() : null;
        if (reportId != null
                && pointTransactionRepository.existsByUserIdAndReportIdAndReason(user.getId(), reportId, reason)) {
            return;
        }

        int points = POINTS_BY_REASON.get(reason);

        PointTransaction transaction = new PointTransaction();
        transaction.setUser(user);
        transaction.setReport(report);
        transaction.setPoints(points);
        transaction.setReason(reason);
        pointTransactionRepository.save(transaction);

        user.setScorePoints(user.getScorePoints() + points);
        userRepository.save(user);
    }

    // Not user-scoped — every caller with leaderboard access sees the same
    // top-N citizens, so a single shared cache entry per `limit` is safe.
    @Cacheable(CacheConfig.LEADERBOARD)
    @Transactional(readOnly = true)
    public List<LeaderboardEntryDTO> getLeaderboard(int limit) {
        List<User> ranked = userRepository.findByRoleNameOrderByScorePointsDescCreatedAtAsc(
                RoleName.CITIZEN, PageRequest.of(0, limit));

        List<LeaderboardEntryDTO> entries = new ArrayList<>();
        int rank = 1;
        for (User user : ranked) {
            entries.add(scoreMapper.toLeaderboardEntry(user, rank++));
        }
        return entries;
    }

    @Transactional(readOnly = true)
    public ScoreSummaryDTO getMyScore(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        List<PointTransactionDTO> history = scoreMapper.toDTOList(
                pointTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId));

        ScoreSummaryDTO summary = new ScoreSummaryDTO();
        summary.setTotalPoints(user.getScorePoints());
        summary.setHistory(history);
        return summary;
    }
}
