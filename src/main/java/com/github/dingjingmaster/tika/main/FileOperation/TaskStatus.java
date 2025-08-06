package com.github.dingjingmaster.tika.main.FileOperation;

import java.time.Instant;
import java.util.Optional;

public class TaskStatus {
    final ServerStatus.TASK task;
    final Instant started;
    final Optional<String> fileName;
    final long timeoutMillis;

    TaskStatus(ServerStatus.TASK task, Instant started, String fileName, long timeoutMillis) {
        this.task = task;
        this.started = started;
        this.fileName = Optional.ofNullable(fileName);
        this.timeoutMillis = timeoutMillis;
    }


    @Override
    public String toString() {
        return "TaskStatus{" + "task=" + task + ", started=" + started + ", fileName=" + fileName + ", timeoutMillis=" + timeoutMillis + '}';
    }
}
