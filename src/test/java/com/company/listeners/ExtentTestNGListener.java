package com.company.listeners;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

public class ExtentTestNGListener implements ITestListener {

    private ExtentReports extent = ExtentManager.getInstance();

    @Override
    public void onStart(ITestContext context) {
        // no-op
    }

    @Override
    public void onFinish(ITestContext context) {
        extent.flush();
    }

    @Override
    public void onTestStart(ITestResult result) {
        String testName = result.getMethod().getMethodName();
        ExtentTest test = extent.createTest(testName);
        ExtentTestManager.setExtentTest(test);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        ExtentTestManager.getExtentTest().pass("Test passed");
        ExtentTestManager.removeExtentTest();
    }

    @Override
    public void onTestFailure(ITestResult result) {
        ExtentTestManager.getExtentTest().fail(result.getThrowable());
        ExtentTestManager.removeExtentTest();
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        ExtentTestManager.getExtentTest().skip("Test skipped");
        ExtentTestManager.removeExtentTest();
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
        // no-op
    }
}

