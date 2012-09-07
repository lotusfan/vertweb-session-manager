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

import org.vertx.java.core.json.JsonObject;

/**
 * Created with IntelliJ IDEA.
 * User: kun
 * Date: 29.08.12
 * Time: 11:57
 * To change this template use File | Settings | File Templates.
 */
public abstract class SessionStore {

    protected final JsonObject config;

    protected SessionStore(JsonObject config) {
        this.config = config;
    }

    public abstract String remove(final String sessionid);

    public abstract String getSession(final String sessionid);

    /**
     * Puts a session in the store.
     * @param sessionid the sessionid for the session to add
     * @return <code>null</code> if there was no previously assigned session to the provided sessionid,
     * otherwise returns the sessionid
     */
    public abstract String putSession(final String sessionid);

    public abstract String saveSession(final String sessionid, final JsonObject session);

    public abstract boolean isAlive(String sessionid);

}
