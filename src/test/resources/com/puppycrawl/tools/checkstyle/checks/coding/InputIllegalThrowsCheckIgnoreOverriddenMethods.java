package com.puppycrawl.tools.checkstyle.checks.coding;

public class InputIllegalThrowsCheckIgnoreOverriddenMethods
             extends InputIllegalThrowsCheck
{
    @Override
    public void methodTwo() throws RuntimeException {
        
    }
    
    @java.lang.Override
    public java.lang.Throwable methodOne() throws RuntimeException {
        return null;
    }
}
