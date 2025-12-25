package com.company.listeners;

import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class RetryAnalyzer implements IRetryAnalyzer {
    private int retryCount = 0;
    private static final int maxRetryCount = 2; // configurable

    @Override
    public boolean retry(ITestResult result) {
        if (retryCount < maxRetryCount) {
            retryCount++;

            // Prepare log entry
            try {
                Map<String, Object> entry = new HashMap<>();
                entry.put("timestamp", Instant.now().toString());
                entry.put("testClass", result.getTestClass() != null ? result.getTestClass().getName() : "");
                entry.put("testMethod", result.getMethod() != null ? result.getMethod().getMethodName() : "");
                entry.put("attempt", retryCount);
                entry.put("maxRetryCount", maxRetryCount);
                entry.put("status", "RETRY");

                if (result.getThrowable() != null) {
                    Throwable t = result.getThrowable();
                    entry.put("errorMessage", t.getMessage());
                    StringWriter sw = new StringWriter();
                    t.printStackTrace(new PrintWriter(sw));
                    entry.put("stackTrace", sw.toString());
                }

                // Use centralized flaky logger
                FlakyLogger.log(entry);
            } catch (Exception ignored) {
                // Do not fail tests if logging fails
            }

            return true;
        }
        return false;
    }
}
