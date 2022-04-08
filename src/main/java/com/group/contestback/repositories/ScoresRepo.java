package com.group.contestback.repositories;

import com.group.contestback.models.Scores;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ScoresRepo extends JpaRepository<Scores, Integer>{
    List<Scores> findAllByUserId(Integer userId);
    List<Scores> findAllByUserIdAndTaskId(Integer userId, Integer taskId);
    @Query(
            value = "select Scores.id, userId, taskId, " +
                    "teacherId, date, score, solution from Scores " +
                    "join Users U on Scores.userId = U.id where groupId = ?1" +
                    " and taskId = ?2",
            nativeQuery = true)
    List<Scores> findAllScoresByTaskAndGroup(Integer groupId, Integer taskId);
}
