/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.domkun.vertweb.sessionmanager;

import org.domkun.vertweb.sessionmanager.exception.SessionNotFoundException;
import org.domkun.vertweb.sessionmanager.store.DefaultSessionStore;
import org.domkun.vertweb.sessionmanager.store.SessionStore;
import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

/**
 * Created with IntelliJ IDEA.
 * User: kun
 * Date: 29.08.12
 * Time: 11:32
 * To change this template use File | Settings | File Templates.
 */
public class SessionManager extends BusModBase implements Handler<Message<JsonObject>> {

    private static final String defaultAddress = "vertweb.session.manager";

    private static final long defaultTimeout = 30 * 60 * 1000;

    private static final String defaultClientSessionPrefix = "vertweb.client.session.";

    private static final String defaultSessionStore = "default";

    private static final JsonObject defaultSessionStoreConfig = new JsonObject()
            .putString("session-map", "vertweb.sessions");

    private static final String defaultTimeoutMap = "vertweb.session.timeouts";

    private String clientSessionPrefix;

    private long timeout;

    private String cleanupAddress;

    private String sessionStore;

    private JsonObject sessionStoreConfig;

    private String timeoutMap;

    private ConcurrentMap<String, Long> sessionTimeouts;

    private SessionStore store;


    /**
     * Start the busmod
     */
    @Override
    public void start() {
        super.start();

        eb.registerHandler(getOptionalStringConfig("address", defaultAddress), this);

        timeout = getOptionalLongConfig("timeout", defaultTimeout);

        cleanupAddress = getOptionalStringConfig("cleaner", null);

        clientSessionPrefix = getOptionalStringConfig("prefix", defaultClientSessionPrefix);

        sessionStore = getOptionalStringConfig("session-store", defaultSessionStore);

        sessionStoreConfig = getOptionalObjectConfig("session-store-config", defaultSessionStoreConfig);

        timeoutMap = getOptionalStringConfig("timeout-map", defaultTimeoutMap);

        store = getSessionStore(sessionStore);

        sessionTimeouts = vertx.sharedData().getMap(timeoutMap);

    }

    private SessionStore getSessionStore(final String type) {
        SessionStore store;
        switch (type) {
            case "default":
                store = new DefaultSessionStore(sessionStoreConfig, vertx);
                break;
            default:
                logger.warn("Unrecognized session store, using default.");
                store = new DefaultSessionStore(sessionStoreConfig, vertx);
        }

        return store;
    }

    private long createTimer(final String sessionid, final long timeout) {
        long timerId = vertx.setTimer(timeout, new Handler<Long>() {
            @Override
            public void handle(Long event) {
                destroySession(sessionid, InvalidationCause.TIMEOUT);
            }
        });
        return timerId;
    }

    private boolean resetTimer(final String sessionid) {
        final boolean cancelled;
        if (vertx.cancelTimer(sessionTimeouts.get(sessionid))) {
            sessionTimeouts.put(sessionid, createTimer(sessionid, timeout));
            cancelled = true;
        } else {
            cancelled = false;
        }

        return cancelled;
    }

    private void destroySession(final String sessionid, final InvalidationCause cause) {
        logger.info("About to destroy session: " + sessionid);
        vertx.eventBus().send(clientSessionPrefix + sessionid, new JsonObject().putString("status", cause.name()));
        final String session = store.remove(sessionid);
        vertx.cancelTimer(sessionTimeouts.remove(sessionid));
        if (cleanupAddress != null) {
            eb.send(cleanupAddress, new JsonObject().putString("sessionid", sessionid)
                    .putObject("session", new JsonObject(session)));
        }
    }

    private JsonObject getSession(final String sessionid) throws SessionNotFoundException {
        String sessionString = store.getSession(sessionid);
        if (sessionString == null) {
            throw new SessionNotFoundException();
        }

        resetTimer(sessionid);
        JsonObject session = new JsonObject(sessionString);

        return session;
    }

    private String startSession() {
        String sessionid = UUID.randomUUID().toString();
        String previousSession = store.putSession(sessionid);
        if (previousSession != null) {
            destroySession(sessionid, InvalidationCause.CONFLICT);
            return startSession();
        }
        return sessionid;
    }

    private boolean isAlive(final String sessionid) {
        final boolean alive = store.isAlive(sessionid);
        if (alive) {
            resetTimer(sessionid);
        }
        return alive;
    }

    /**
     * Something has happened, so handle it.
     */
    @Override
    public void handle(Message<JsonObject> message) {
        final String action = message.body.getString("action");

        if (action != null) {

            switch (action) {
                case "get":
                    get(message);
                    break;
                case "put":
                    put(message);
                    break;
                case "start":
                    start(message);
                    break;
                case "heartbeat":
                    break;
                case "destroy":
                    destroy(message);
                    break;
                case "status":
                    status(message);
                    break;
                case "clear":
                    break;
                default:
                    sendError(message, "ACTION_UNKNOWN");
                    break;
            }
        } else {
            sendError(message, "NO_ACTION");
            return;
        }
    }

    private void status(Message<JsonObject> message) {
        final String sessionid = message.body.getString("sessionid");

        if (sessionid == null) {
            sendError(message, "SESSIONID_MISSING");
        } else {
            if (isAlive(sessionid)) {
                message.reply(new JsonObject().putString("status", "ok"));
            } else {
                message.reply(new JsonObject().putString("status", "SESSION_NOT_FOUND"));
            }
        }
    }

    private void put(Message<JsonObject> message) {
        final String sessionid = message.body.getString("sessionid");

        if (sessionid == null) {
            sendError(message, "SESSIONID_MISSING");
        } else {
            Object data = message.body.getField("data");
            if (data != null && data instanceof JsonObject) {
                try {
                    JsonObject session = getSession(sessionid);
                    session.mergeIn((JsonObject) data);
                    store.saveSession(sessionid, session);
                    sendOK(message);
                } catch (SessionNotFoundException e) {
                    sendError(message, "SESSION_NOT_FOUND");
                }
            }
        }
    }

    private void start(Message<JsonObject> message) {
        final String sessionid = startSession();
        sessionTimeouts.put(sessionid, createTimer(sessionid, timeout));
        sendOK(message, new JsonObject().putString("sessionid", sessionid));
    }

    private void destroy(Message<JsonObject> message) {
        final String sessionid = message.body.getString("sessionid");
        if (sessionid == null) {
            sendError(message, "SESSIONID_MISSING");
        } else {
            destroySession(sessionid, InvalidationCause.KILL);
            sendOK(message);
        }
    }

    private void get(Message<JsonObject> message) {
        final String sessionid = message.body.getString("sessionid");
        if (sessionid == null) {
            sendError(message, "SESSIONID_MISSING");
        } else {
            try {
                JsonObject session = getSession(sessionid);
                JsonObject result = new JsonObject();
                JsonArray fields = message.body.getArray("fields");
                for (Object fieldObject : fields.toArray()) {
                    String field = (String) fieldObject;
                    Object value = session.getField(field);
                    if (value instanceof JsonArray) {
                        result.putArray(field, (JsonArray) value);
                    } else if (value instanceof JsonObject) {
                        result.putObject(field, (JsonObject) value);
                    } else if (value instanceof byte[]) {
                        result.putBinary(field, (byte[]) value);
                    } else if (value instanceof Boolean) {
                        result.putBoolean(field, (Boolean) value);
                    } else if (value instanceof Number) {
                        result.putNumber(field, (Number) value);
                    } else if (value instanceof String) {
                        result.putString(field, (String) value);
                    }
                }
                message.reply(new JsonObject().putObject("data", result));
            } catch (SessionNotFoundException e) {
                sendError(message, "NO_SESSION_FOUND");
            }
        }
    }

    private static enum InvalidationCause {
        KILL,
        TIMEOUT,
        CONFLICT;
    }

}
