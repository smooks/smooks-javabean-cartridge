/*-
 * ========================LICENSE_START=================================
 * smooks-javabean-cartridge
 * %%
 * Copyright (C) 2020 Smooks
 * %%
 * Licensed under the terms of the Apache License Version 2.0, or
 * the GNU Lesser General Public License version 3.0 or later.
 *
 * SPDX-License-Identifier: Apache-2.0 OR LGPL-3.0-or-later
 *
 * ======================================================================
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
 *
 * ======================================================================
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 * =========================LICENSE_END==================================
 */
package org.smooks.cartridges.javabean.binding;

import org.smooks.assertion.AssertArgument;
import org.smooks.cartridges.javabean.binding.model.get.Getter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
@SuppressWarnings("unchecked")
public class SerializationContext {

    public static final String ROOT_OBJ = SerializationContext.class.getName() + "#ROOT_OBJ";

    protected Object rootObject;
    protected Map<String, Object> contextObjects = new LinkedHashMap<>();
    protected int currentDepth;

    public SerializationContext(Object rootObject, String rootObjectBeanId) {
        AssertArgument.isNotNull(rootObject, "rootObject");
        this.rootObject = rootObject;
        addObject(rootObjectBeanId, rootObject);
    }

    public int getCurrentDepth() {
        return currentDepth;
    }

    public void incDepth() {
        currentDepth++;
    }

    public void decDepth() {
        currentDepth--;
    }

    public void addObject(String name, Object contextObject) {
        contextObjects.put(name, contextObject);
    }

    public Object removeObject(String name) {
        return contextObjects.remove(name);
    }

    public Object getValue(Getter getter) {
        return getter.get(rootObject);
    }

    public Object getValue(String contextObjectName, Getter getter) {
        if (ROOT_OBJ.equals(contextObjectName)) {
            return getter.get(rootObject);
        }

        Object contextObject = contextObjects.get(contextObjectName);

        if (contextObject == null) {
            throw new IllegalStateException("Unknown context object name '" + contextObjectName + "'.");
        }

        return getter.get(contextObject);
    }
}
