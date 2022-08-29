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
package org.slf4j.helpers;

import java.util.Deque;
import java.util.Map;

/**
 * This class add some utility functions to the SLF4J ThreadLocalMapOfStacks
 */
public class PeakableThreadLocalMapOfStacks extends ThreadLocalMapOfStacks {

    /**
     * Retrieves, but does not remove, the head of the stack indexed by the key.
     * Returns null if key is null or if there isn't any element in the stack reference by the key
     */
    public String peekByKey(String key) {
        if (key == null) {
            return null;
        }

        Map<String, Deque<String>> map = tlMapOfStacks.get();
        if (map == null) {
            return null;
        }
        Deque<String> deque = map.get(key);
        if (deque == null) {
            return null;
        }
        return deque.peek();
    }

    /**
     * Clear the whole thread local
     */
    public void clear() {
        tlMapOfStacks.remove();
    }
}
