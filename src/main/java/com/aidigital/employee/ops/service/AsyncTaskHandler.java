package com.aidigital.employee.ops.service;

import com.aidigital.employee.ops.entity.AsyncTask;

public interface AsyncTaskHandler {

    String taskType();

    /**
     * Executes a single durable task. Throwing an exception makes the worker retry it.
     */
    void handle(AsyncTask task);
}
