package de.isogmbh.vertweb.sessionmanager;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.framework.TestClientBase;

/**
 * Created with IntelliJ IDEA.
 * User: kun
 * Date: 30.08.12
 * Time: 07:49
 * To change this template use File | Settings | File Templates.
 */
public class SessionManagerTestClient extends TestClientBase {

    private String moduleString = "org.domkun.vertweb.session-manager-v" + System.getProperty("module.version");

    private EventBus eb;

    @Override
    public void start() {
        super.start();

        eb = vertx.eventBus();

        container.deployModule(moduleString, null, 1, new Handler<String>() {
            @Override
            public void handle(String event) {
                tu.appReady();
            }
        });
    }

    public void testStartSession() {
        eb.send("vertweb.session.manager", new JsonObject().putString("action", "start"), new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                tu.azzert("ok".equals(message.body.getString("status")) &&
                            message.body.getString("sessionid") != null);
                tu.testComplete();
            }
        });

    }

    public void testSessionPut() {
        eb.send("vertweb.session.manager", new JsonObject().putString("action", "start"), new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                final String sessionid = message.body.getString("sessionid");
                eb.send("vertweb.session.manager", new JsonObject().putString("action", "put").putString("sessionid", sessionid)
                    .putObject("data", new JsonObject().putString("testfield", "testvalue")), new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(Message<JsonObject> putresponse) {
                        tu.azzert(putresponse.body.getString("status").equals("ok"));
                        tu.testComplete();
                    }
                });

            }
        });

    }

}
