package com.company.listeners;

import org.testng.IAnnotationTransformer;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.annotations.ITestAnnotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class RetryListener implements IAnnotationTransformer, ITestListener {

    @Override
    public void transform(ITestAnnotation annotation, Class testClass, Constructor testConstructor, Method testMethod) {
        // Some TestNG versions used here do not expose getRetryAnalyzer; set unconditionally to ensure retry analyzer is applied
        annotation.setRetryAnalyzer(RetryAnalyzer.class);
    }

    // Optional ITestListener no-op implementations so this can be registered as a listener
    @Override
    public void onTestStart(org.testng.ITestResult result) { }

    @Override
    public void onTestSuccess(org.testng.ITestResult result) { }

    @Override
    public void onTestFailure(org.testng.ITestResult result) { }

    @Override
    public void onTestSkipped(org.testng.ITestResult result) { }

    @Override
    public void onTestFailedButWithinSuccessPercentage(org.testng.ITestResult result) { }

    @Override
    public void onStart(ITestContext context) { }

    @Override
    public void onFinish(ITestContext context) { }
}
