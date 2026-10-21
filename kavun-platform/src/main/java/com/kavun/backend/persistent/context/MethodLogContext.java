package com.kavun.backend.persistent.context;

public record MethodLogContext(
    String tenantCode, String method, String logType, long durationMs,
    String stateBefore, String stateAfter, String stateDiff) {
}
