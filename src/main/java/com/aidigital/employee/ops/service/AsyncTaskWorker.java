package com.aidigital.employee.ops.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.aidigital.employee.common.config.AppProperties;
import com.aidigital.employee.ops.entity.AsyncTask;
import com.aidigital.employee.ops.mapper.AsyncTaskMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AsyncTaskWorker {

    private static final Logger log = LoggerFactory.getLogger(AsyncTaskWorker.class);

    private final AsyncTaskMapper asyncTaskMapper;
    private final AppProperties appProperties;
    private final Map<String, AsyncTaskHandler> handlers;
    private final Counter taskSuccessCounter;
    private final Counter taskFailureCounter;

    public AsyncTaskWorker(
            AsyncTaskMapper asyncTaskMapper,
            AppProperties appProperties,
            List<AsyncTaskHandler> handlers,
            MeterRegistry meterRegistry) {
        this.asyncTaskMapper = asyncTaskMapper;
        this.appProperties = appProperties;
        this.handlers = handlers.stream().collect(Collectors.toMap(AsyncTaskHandler::taskType, Function.identity()));
        this.taskSuccessCounter = Counter.builder("employee_async_task_success_total").register(meterRegistry);
        this.taskFailureCounter = Counter.builder("employee_async_task_failure_total").register(meterRegistry);
    }

    /**
     * Polls the durable task table so local deployments can run without a RabbitMQ consumer process.
     */
    @Scheduled(fixedDelay = 5000)
    public void pollReadyTasks() {
        if (!appProperties.getWorker().isEnabled()) {
            return;
        }
        List<AsyncTask> tasks = asyncTaskMapper.selectReadyTasks(Instant.now(), appProperties.getAsync().getPollSize());
        for (AsyncTask task : tasks) {
            execute(task.getId());
        }
    }

    @Transactional
    public void execute(Long taskId) {
        AsyncTask task = asyncTaskMapper.selectById(taskId);
        if (task == null || (!"PENDING".equals(task.getStatus()) && !"RETRY".equals(task.getStatus()))) {
            return;
        }
        AsyncTaskHandler handler = handlers.get(task.getTaskType());
        if (handler == null) {
            markFailed(task, "No handler for task type " + task.getTaskType());
            return;
        }
        task.setStatus("RUNNING").setAttempts(task.getAttempts() + 1).setUpdatedAt(Instant.now());
        asyncTaskMapper.updateById(task);
        try {
            handler.handle(task);
            task.setStatus("SUCCEEDED").setLastError(null).setUpdatedAt(Instant.now());
            asyncTaskMapper.updateById(task);
            taskSuccessCounter.increment();
        } catch (Exception ex) {
            log.warn("Async task failed id={} type={}", task.getId(), task.getTaskType(), ex);
            markFailed(task, ex.getMessage());
        }
    }

    @Transactional
    public AsyncTask retry(Long taskId) {
        AsyncTask task = asyncTaskMapper.selectById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("Task does not exist");
        }
        task.setStatus("RETRY").setNextRunAt(Instant.now()).setUpdatedAt(Instant.now());
        asyncTaskMapper.updateById(task);
        return task;
    }

    public List<AsyncTask> listFailures() {
        return asyncTaskMapper.selectList(new LambdaQueryWrapper<AsyncTask>()
                .eq(AsyncTask::getStatus, "FAILED")
                .orderByDesc(AsyncTask::getUpdatedAt)
                .last("limit 50"));
    }

    public List<AsyncTask> listTasks(String taskType, String status) {
        return asyncTaskMapper.selectList(new LambdaQueryWrapper<AsyncTask>()
                .eq(taskType != null && !taskType.isBlank(), AsyncTask::getTaskType, taskType)
                .eq(status != null && !status.isBlank(), AsyncTask::getStatus, normalizeStatus(status))
                .orderByDesc(AsyncTask::getUpdatedAt)
                .last("limit 100"));
    }

    private String normalizeStatus(String status) {
        if ("COMPLETED".equals(status)) {
            return "SUCCEEDED";
        }
        if ("DEAD_LETTER".equals(status)) {
            return "FAILED";
        }
        return status;
    }

    private void markFailed(AsyncTask task, String errorMessage) {
        boolean exhausted = task.getAttempts() >= task.getMaxAttempts();
        task.setStatus(exhausted ? "FAILED" : "RETRY")
                .setLastError(truncate(errorMessage))
                .setNextRunAt(Instant.now().plus(Math.max(1, task.getAttempts()), ChronoUnit.MINUTES))
                .setUpdatedAt(Instant.now());
        asyncTaskMapper.updateById(task);
        taskFailureCounter.increment();
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= 1000 ? value : value.substring(0, 1000);
    }
}
