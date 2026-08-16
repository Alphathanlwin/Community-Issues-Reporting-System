package com.uit.scirs.score.mapper;

import com.uit.scirs.score.dto.LeaderboardEntryDTO;
import com.uit.scirs.score.dto.PointTransactionDTO;
import com.uit.scirs.score.entity.PointTransaction;
import com.uit.scirs.user.entity.User;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ScoreMapper {

    public PointTransactionDTO toDTO(PointTransaction entity) {
        PointTransactionDTO dto = new PointTransactionDTO();
        dto.setId(entity.getId());
        dto.setPoints(entity.getPoints());
        dto.setReason(entity.getReason().name());
        if (entity.getReport() != null) {
            dto.setReportId(entity.getReport().getId());
            dto.setReportCode(entity.getReport().getReportCode());
        }
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    public List<PointTransactionDTO> toDTOList(List<PointTransaction> entities) {
        return entities.stream().map(this::toDTO).toList();
    }

    public LeaderboardEntryDTO toLeaderboardEntry(User user, int rank) {
        LeaderboardEntryDTO dto = new LeaderboardEntryDTO();
        dto.setRank(rank);
        dto.setUserId(user.getId());
        dto.setFullName(user.getFullName());
        dto.setScorePoints(user.getScorePoints());
        return dto;
    }
}
