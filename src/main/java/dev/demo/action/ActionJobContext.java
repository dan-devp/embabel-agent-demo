package dev.demo.action;

import org.springframework.stereotype.Component;

@Component
public class ActionJobContext {
    private static final InheritableThreadLocal<String> JOB_ID = new InheritableThreadLocal<>();

    public void set(String jobId) { JOB_ID.set(jobId); }
    public String get() { return JOB_ID.get(); }
    public void clear() { JOB_ID.remove(); }
}
