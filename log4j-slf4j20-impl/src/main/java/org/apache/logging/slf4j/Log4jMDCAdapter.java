/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.slf4j;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.status.StatusLogger;
import org.slf4j.helpers.PeakableThreadLocalMapOfStacks;
import org.slf4j.spi.MDCAdapter;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 */
public class Log4jMDCAdapter implements MDCAdapter {
    private static final Logger LOGGER = StatusLogger.getLogger();

    private final PeakableThreadLocalMapOfStacks threadLocalMapOfDeques = new PeakableThreadLocalMapOfStacks();
    private final AtomicInteger ndcDepth = new AtomicInteger();

    @Override
    public void put(final String key, final String val) {
        ThreadContext.put(key, val);
    }

    @Override
    public String get(final String key) {
        return ThreadContext.get(key);
    }

    @Override
    public void remove(final String key) {
        ThreadContext.remove(key);
    }

    @Override
    public Map<String, String> getCopyOfContextMap() {
        return ThreadContext.getContext();
    }

    @Override
    public void setContextMap(final Map<String, String> map) {
        ThreadContext.clearMap();
        ThreadContext.putAll(map);
    }

    @Override
    public void clear() {
        ThreadContext.clearMap();
        // Unfortunately, SLF4J doesn't expose a method like: MDC.clearStacks() or MDC.clearStackByKey(key)
        // It is possible that the NDC context is called outside from this class
        // In this case, we don't want to clear the Stack, but we leave this responsibility to the user
        if (ndcDepth.get() == ThreadContext.getDepth()) {
            ThreadContext.clearStack();
        }
        threadLocalMapOfDeques.clear();
    }

    @Override
    public void pushByKey(String key, String value) {
        if (key == null) {
            // Delegate to NDC
            ThreadContext.push(value);
            // We need to record if the NDC is touch by MDC Stack
            ndcDepth.incrementAndGet();
            return;
        }

        if (!Objects.equals(threadLocalMapOfDeques.peekByKey(key), ThreadContext.get(key))) {
            LOGGER.warn("MDC already contains a value for key [{}]. Value will not be pushed", key);
            return;
        }
        threadLocalMapOfDeques.pushByKey(key, value);
        put(key, value);
    }

    @Override
    public String popByKey(String key) {
        if (key == null) {
            // Delegate to NDC
            ThreadContext.pop();
            ndcDepth.updateAndGet(current -> {
                if (current > 1) {
                    return current - 1;
                }
                return 0;
            });
        }
        String value = threadLocalMapOfDeques.popByKey(key);
        String head = threadLocalMapOfDeques.peekByKey(key);
        if (head != null) {
            put(key, head);
        } else {
            remove(key);
        }
        return value;
    }

    @Override
    public Deque<String> getCopyOfDequeByKey(String key) {
        if (key == null) {
            // Delegate to NDC
            return new ArrayDeque<>(ThreadContext.getImmutableStack().asList());
        }
        Deque<String> deque = threadLocalMapOfDeques.getCopyOfDequeByKey(key);
        if (deque == null) {
            return new ArrayDeque<>();
        }
        return deque;
    }

    @Override
    public void clearDequeByKey(String key) {
        if (key == null) {
            // Delegate to NDC
            ThreadContext.clearStack();
        } else {
            threadLocalMapOfDeques.clearDequeByKey(key);
        }
    }


}
