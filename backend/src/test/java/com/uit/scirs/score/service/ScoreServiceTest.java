package com.uit.scirs.score.service;

import com.uit.scirs.report.entity.Report;
import com.uit.scirs.score.dto.LeaderboardEntryDTO;
import com.uit.scirs.score.entity.PointReason;
import com.uit.scirs.score.entity.PointTransaction;
import com.uit.scirs.score.mapper.ScoreMapper;
import com.uit.scirs.score.repository.PointTransactionRepository;
import com.uit.scirs.user.entity.RoleName;
import com.uit.scirs.user.entity.User;
import com.uit.scirs.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScoreServiceTest {

    @Mock PointTransactionRepository pointTransactionRepository;
    @Mock UserRepository userRepository;
    @Mock ScoreMapper scoreMapper;
    @InjectMocks ScoreService scoreService;

    @Test
    void award_firstTimeForReportAndReason_createsTransactionAndIncrementsScore() {
        User citizen = citizen(7L, 0);
        Report report = report(1L);

        when(pointTransactionRepository.existsByUserIdAndReportIdAndReason(7L, 1L, PointReason.REPORT_APPROVED))
                .thenReturn(false);

        scoreService.award(citizen, PointReason.REPORT_APPROVED, report);

        ArgumentCaptor<PointTransaction> captor = ArgumentCaptor.forClass(PointTransaction.class);
        verify(pointTransactionRepository).save(captor.capture());
        assertThat(captor.getValue().getPoints()).isEqualTo(10);
        assertThat(citizen.getScorePoints()).isEqualTo(10);
        verify(userRepository).save(citizen);
    }

    @Test
    void award_whenAlreadyAwardedForSameReportAndReason_isNoOp() {
        User citizen = citizen(7L, 10);
        Report report = report(1L);

        when(pointTransactionRepository.existsByUserIdAndReportIdAndReason(7L, 1L, PointReason.REPORT_APPROVED))
                .thenReturn(true);

        scoreService.award(citizen, PointReason.REPORT_APPROVED, report);

        verify(pointTransactionRepository, never()).save(any(PointTransaction.class));
        verify(userRepository, never()).save(any(User.class));
        assertThat(citizen.getScorePoints()).isEqualTo(10);
    }

    @Test
    void getLeaderboard_ordersByRankMatchingRepositoryOrder() {
        User first = citizen(1L, 100);
        first.setFullName("Alice");
        User second = citizen(2L, 80);
        second.setFullName("Bob");

        when(userRepository.findByRoleNameOrderByScorePointsDescCreatedAtAsc(eq(RoleName.CITIZEN), any(Pageable.class)))
                .thenReturn(List.of(first, second));
        when(scoreMapper.toLeaderboardEntry(first, 1)).thenReturn(entry(1, "Alice", 100));
        when(scoreMapper.toLeaderboardEntry(second, 2)).thenReturn(entry(2, "Bob", 80));

        List<LeaderboardEntryDTO> leaderboard = scoreService.getLeaderboard(50);

        assertThat(leaderboard).extracting(LeaderboardEntryDTO::getFullName).containsExactly("Alice", "Bob");
        assertThat(leaderboard).extracting(LeaderboardEntryDTO::getRank).containsExactly(1, 2);
    }

    private User citizen(Long id, int scorePoints) {
        User user = new User();
        user.setId(id);
        user.setFullName("Test Citizen");
        user.setScorePoints(scorePoints);
        return user;
    }

    private Report report(Long id) {
        Report report = new Report();
        report.setId(id);
        return report;
    }

    private LeaderboardEntryDTO entry(int rank, String name, int points) {
        LeaderboardEntryDTO dto = new LeaderboardEntryDTO();
        dto.setRank(rank);
        dto.setFullName(name);
        dto.setScorePoints(points);
        return dto;
    }
}
