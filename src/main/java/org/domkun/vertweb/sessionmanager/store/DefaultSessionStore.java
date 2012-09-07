/*
 * Copyright 2011-2012 the original author or authors.
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

package org.domkun.vertweb.sessionmanager.store;

import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;

import java.util.concurrent.ConcurrentMap;

/**
 * Created with IntelliJ IDEA.
 * User: kun
 * Date: 29.08.12
 * Time: 12:01
 * To change this template use File | Settings | File Templates.
 */
public class DefaultSessionStore extends SessionStore {

    private static final String defaultSessionMapName = "vertweb.sessions";

    private final Vertx vertx;

    private final String sessionMapName;

    private final ConcurrentMap<String, String> sessions;

    public DefaultSessionStore(final JsonObject config, final Vertx vertx) {
        super(config);
        this.vertx = vertx;
        sessionMapName = config.getString("session-map", defaultSessionMapName);
        sessions = this.vertx.sharedData().getMap(sessionMapName);
    }

    @Override
    public String remove(final String sessionid) {
        return sessions.remove(sessionid);
    }

    @Override
    public String getSession(final String sessionid) {
        return sessions.get(sessionid);  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String putSession(String sessionid) {
        final String previous;

        previous = sessions.putIfAbsent(sessionid, new JsonObject().encode());
        if (previous == null) {
            return null;
        } else {
            return sessionid;
        }

    }

    @Override
    public String saveSession(String sessionid, JsonObject session) {
        String result = sessions.put(sessionid, session.encode());
        return result;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isAlive(String sessionid) {
        return sessions.containsKey(sessionid);  //To change body of implemented methods use File | Settings | File Templates.
    }
}
