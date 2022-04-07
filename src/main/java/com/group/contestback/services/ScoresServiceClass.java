package com.group.contestback.services;


import com.group.contestback.models.AppUser;
import com.group.contestback.models.Attempts;
import com.group.contestback.models.Scores;
import com.group.contestback.repositories.*;
import com.group.contestback.responseTypes.ResultsResponse;
import com.group.contestback.responseTypes.Result;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ScoresServiceClass implements ScoresService{
    private final ScoresRepo scoresRepo;
    private final AttemptsRepo attemptsRepo;
    private final AppUserRepo appUserRepo;
    private final RolesRepo rolesRepo;
    private final TasksRepo tasksRepo;
    @Override
    public void addScore(Scores score) {
        scoresRepo.save(score);
    }

    @Override
    public List<Scores> getAllScores() {
        return scoresRepo.findAll();
    }

    @Override
    public void addAttempt(Attempts attempt) {
        attemptsRepo.save(attempt);
    }

    @Override
    public List<Attempts> getAllAttempts() {
        return attemptsRepo.findAll();
    }

    @Override
    public ResultsResponse checkSolution(Integer userId, Integer taskId, String solution) {
        ResultsResponse resultsResponse = new ResultsResponse();
        if(tasksRepo.findById(taskId).get().getDeadLine().getTime() - new Date().getTime() < 0) {
            resultsResponse.setDeadlinePassed(true);
            return resultsResponse;
        }
        List<Attempts> attempts = attemptsRepo.findAllByTaskIdAndUserId(taskId,userId);
        Comparator<Attempts> comparator = (p1, p2) -> (int) (p2.getTime().getTime() - p1.getTime().getTime());
        attempts.sort(comparator);

        if(attempts.size() > 0 && (new Date().getTime() - attempts.get(0).getTime().getTime() < 60*1000)) {
            resultsResponse.setTimeout((int) (attempts.size()*60*1000 - (new Date().getTime() - attempts.get(0).getTime().getTime())));
            return resultsResponse;
        }

        //needed logic go check solution
        for(int i = 0; i < 2; ++i) {
            Result result = new Result("Test" + i,false);
            resultsResponse.getResults().add(result);
        }
        Boolean succeeded = false;
        Attempts attempt = new Attempts(userId, taskId, succeeded, solution);
        resultsResponse.setTimeout((attempts.size() + 1)*60*1000);
        attemptsRepo.save(attempt);

        return resultsResponse;
    }

    @Override
    public List<Scores> getStudentScores() {
        return scoresRepo.findAllByUserId(appUserRepo.findByLogin(SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString()).getId());
    }

    @Override
    public List<Scores> getGroupScoresForTask(Integer groupId, Integer taskId) {
        List<Scores> scores = scoresRepo.findAllScoresByTaskAndGroup(groupId, taskId);
        List<AppUser> users = appUserRepo.findAllByGroupId(groupId);
        List<Scores> result = new ArrayList<>();
        users.forEach(appUser -> {
            int resSize = result.size();
            scores.forEach(scores1 -> {
                if(appUser.getId().equals(scores1.getUserId())){
                    result.add(scores1);
                }
            });
            if(resSize == result.size() && rolesRepo.getById(appUser.getRoleId()).getName().equals("ROLE_USER")){
                result.add(new Scores(null,appUser.getId(),taskId,null,null,null,null));
            }
        });
        return result;
    }

    @Override
    public List<Attempts> getStudentAttemptsOnTask(Integer taskId) {
        log.info(appUserRepo.findByLogin(SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString()).getId().toString());
        log.info(taskId.toString());
        return attemptsRepo.findAllByTaskIdAndUserId(taskId,appUserRepo.findByLogin(SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString()).getId());
    }
}



