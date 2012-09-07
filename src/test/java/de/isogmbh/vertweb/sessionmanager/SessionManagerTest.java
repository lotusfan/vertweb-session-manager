package de.isogmbh.vertweb.sessionmanager;

import org.junit.Test;
import org.vertx.java.framework.TestBase;

/**
 * Created with IntelliJ IDEA.
 * User: kun
 * Date: 30.08.12
 * Time: 07:42
 * To change this template use File | Settings | File Templates.
 */
public class SessionManagerTest extends TestBase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        startApp(SessionManagerTestClient.class.getName());
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Test
    public void testStartSession() {
        startTest(getMethodName());
    }

    @Test
    public void testSessionPut() {
        startTest(getMethodName());
    }
}
