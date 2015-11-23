/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package test.mujmail.tasks;

import j2meunit.framework.TestCase;
import j2meunit.framework.Test;
import j2meunit.framework.TestSuite;
import mujmail.util.Functions;
import mujmail.tasks.BackgroundTask;
import mujmail.tasks.StoppableBackgroundTask;

/**
 * Tests for the class RMSStorage.
 * @author David Hauzar
 */
public class BackgroundTaskTest extends TestCase {
    private static final String TEST_SLEEPING_THREAD = "1";
    
    protected void runTest() throws Throwable {
        if (getName().equals(TEST_SLEEPING_THREAD)) {
            testSleepingThread();
        }
    }
    
    public Test suite() {
        return new TestSuite(new BackgroundTaskTest().getClass(), new String[] {
            TEST_SLEEPING_THREAD});
    }
    
    public void testSleepingThread() {
        BackgroundTask task = new NewTask("Test sleeping thread");
        task.setAlert("Alert");
        task.start();
    }
    
    public void testStoppableThread() {
        StoppableBackgroundTask task = new StoppableBackgroundTask("Test stoppable thread") {

            public void doWork() {
                setTitle("Working");
                for (int i = 0; i < 10; i++) {
                    Functions.sleep(100);
                    System.out.println("working");
                    updateProgress(i+1, 10);
                    if (stopped()) {
                        System.out.println("stopped");
                        return;
                    }
                }
                System.out.println("end work");
            }
        };
        task.setAlert("Task finished");
        task.start();
    }
    
    private static class NewTask extends BackgroundTask {

        public NewTask(String taskName) {
			super(taskName);
		}

		public void doWork() {
            setTitle("Working");
            for (int i = 0; i < 10; i++) {
                Functions.sleep(1000);
                System.out.println("working");
                updateProgress(i+1, 10);
            }
            System.out.println("end work");
        }
        
    }
}
