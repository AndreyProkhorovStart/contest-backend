package com.group.contestback.services;

import com.group.contestback.models.*;
import com.group.contestback.repositories.*;
import com.group.contestback.responseTypes.GroupCoursesWithNames;
import com.group.contestback.responseTypes.StudentTaskResponse;
import com.group.contestback.responseTypes.TaskResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TaskServiceClass implements TaskService {
    private final TasksRepo tasksRepo;
    private final TaskTypesRepo taskTypesRepo;
    private final AppUserService appUserService;
    private final CoursesRepo coursesRepo;
    private final TaskCoursesRepo taskCoursesRepo;
    private final GroupsRepo groupsRepo;
    private final GroupCoursesRepo groupCoursesRepo;
    private final AppUserRepo appUserRepo;
    private final SolutionVariantsRepo solutionVariantsRepo;

    @Override
    public void addTaskType(String name) {
        TaskTypes taskTypes = new TaskTypes(name);
        taskTypesRepo.save(taskTypes);
    }

    @Override
    public List<TaskTypes> getTaskTypes() {
        return taskTypesRepo.findAll();
    }

    @Override
    public void addTask(String name, String solution, String description, Integer taskTypeId) {
        try {
            Tasks tasks = new Tasks(name, solution, description, taskTypeId);
            tasksRepo.save(tasks);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    private void fillTasks(List<TaskResponse> taskResponses, Tasks task, TaskResponse taskResponse) {
        try {
            taskResponse.setTask(new Tasks(task.getId(), task.getName(), task.getDescription(), "", task.getTaskTypeId()));
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        List<SolutionVariants> solutionVariants = solutionVariantsRepo.findAllByTaskId(task.getId());
        for (int k = 0; k < solutionVariants.size(); ++k) {
            taskResponse.addSolutionVariant(solutionVariants.get(k).getId(), solutionVariants.get(k).getSolution(), solutionVariants.get(k).getTaskId());
        }
        taskResponses.add(taskResponse);
    }

    @Override
    public List<TaskResponse> getTasks() {
        List<TaskResponse> taskResponses = new ArrayList<>();
        List<Tasks> tasks = tasksRepo.findAll();
        for (Tasks task : tasks) {
            TaskResponse taskResponse = new TaskResponse();
            fillTasks(taskResponses, task, taskResponse);
        }
        return taskResponses;
    }

    @Override
    public List<TaskResponse> getTasksByCourse(Integer courseId) {
        List<TaskResponse> taskResponses = new ArrayList<>();
        List<TaskCourses> taskCourses = taskCoursesRepo.findAllByCourseId(courseId);
        for (int i = 0; i < taskCourses.size(); ++i) {
            TaskResponse taskResponse = new TaskResponse();
            Tasks task = tasksRepo.findById(taskCourses.get(i).getTaskId()).get();
            fillTasks(taskResponses, task, taskResponse);
        }
        return taskResponses;
    }


    @Override
    public List<Courses> getAllCourses() {
        return coursesRepo.findAll();
    }

    @Override
    public void addCourse(String name, Integer year) {
        Courses courses = new Courses(name, year);
        coursesRepo.save(courses);
    }

    @Override
    public String addTaskToCourse(Integer taskId, Integer courseId) {
        Optional<Tasks> task = tasksRepo.findById(taskId);
        Optional<Courses> course = coursesRepo.findById(courseId);
        if (task.isEmpty()) {
            log.error("there is no task with this id");
        } else if (course.isEmpty()) {
            log.error("there is no course with this id");
        } else {
            TaskCourses taskCourses = new TaskCourses(taskId, courseId);
            taskCoursesRepo.save(taskCourses);
        }
        return "";
    }

    @Override
    public List<Groups> getAllGroups() {
        return groupsRepo.findAll();
    }

    @Override
    public void addGroup(String number, Integer year) {
        Groups groups = new Groups(number, year);
        groupsRepo.save(groups);
    }

    @Override
    public List<GroupCoursesWithNames> getAllGroupCourses() {
        List<GroupCoursesWithNames> groupCoursesWithNames = new ArrayList<>();
        groupCoursesRepo.findAll().forEach(groupCourses -> groupCoursesWithNames.add(new GroupCoursesWithNames(
                        groupCourses.getId()
                        , coursesRepo.getById(groupCourses.getCourseId()).getName(),
                        groupCourses.getCourseId(),
                        groupsRepo.getById(groupCourses.getGroupId()).getNumber(),
                        groupCourses.getGroupId()
                ))
        );
        return groupCoursesWithNames;
    }

    @Override
    public String addGroupOnCourse(Integer courseId, Integer groupId) {
        Optional<Courses> courses = coursesRepo.findById(courseId);
        Optional<Groups> groups = groupsRepo.findById(groupId);
        if (courses.isEmpty()) {
            log.error("there is no course with this id");
            return "there is no course with this id";
        } else if (groups.isEmpty()) {
            log.error("there is no group with this id");
            return "there is no group with this id";
        } else {
            GroupCourses groupCourses = new GroupCourses(courseId, groupId);
            groupCoursesRepo.save(groupCourses);
        }
        return "";
    }

    @Override
    public StudentTaskResponse getStudentCourses() {
        AppUser appUser = appUserRepo.findByLogin(SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString());
        List<GroupCourses> groupCourses
                = groupCoursesRepo.findAllByGroupId(appUser.getGroupId());
        List<Courses> courses = new ArrayList<>();

        for (int i = 0; i < groupCourses.size(); ++i) {
            Courses courses1 = coursesRepo.findById(groupCourses.get(i).getCourseId()).get();
            courses.add(courses1);
        }
        StudentTaskResponse studentTaskResponse = new StudentTaskResponse();
        studentTaskResponse.setUserId(appUser.getId());
        studentTaskResponse.setCourses(courses);
        studentTaskResponse.setCompletion(100);// to do later
        studentTaskResponse.setNearestDeadline(new Date()); // to do later
        return studentTaskResponse;
    }
}
