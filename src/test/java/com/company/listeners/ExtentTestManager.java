package com.company.listeners;

import com.aventstack.extentreports.ExtentTest;

public class ExtentTestManager {
    private static final ThreadLocal<ExtentTest> extentTest = new ThreadLocal<>();

    public static synchronized void setExtentTest(ExtentTest test) {
        extentTest.set(test);
    }

    public static synchronized ExtentTest getExtentTest() {
        return extentTest.get();
    }

    public static synchronized void removeExtentTest() {
        extentTest.remove();
    }
}

