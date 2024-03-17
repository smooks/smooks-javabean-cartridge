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
package org.smooks.cartridges.javabean.binding.model.get;

import org.smooks.assertion.AssertArgument;
import org.smooks.cartridges.javabean.binding.BeanSerializationException;
import org.smooks.cartridges.javabean.binding.SerializationContext;
import org.smooks.cartridges.javabean.binding.model.Bean;
import org.smooks.cartridges.javabean.binding.model.Binding;
import org.smooks.cartridges.javabean.binding.model.DataBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Getter Graph.
 *
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
@SuppressWarnings("unchecked")
public class GetterGraph<T> implements Getter<T> {

    protected String contextObjectName = SerializationContext.ROOT_OBJ;
    protected List<Getter> graph = new ArrayList<>();

    public Object get(final T contextObject) throws BeanSerializationException {
        AssertArgument.isNotNull(contextObject, "contextObject");

        Object value = contextObject;

        for (Getter getter : graph) {
            value = getter.get(value);
            if (value == null) {
                return null;
            }
        }

        return value;
    }

    protected GetterGraph add(Getter getter) {
        // Insert the getter at the start of the graph list...
        graph.add(0, getter);
        return this;
    }

    public void add(DataBinding binding) {
        add(toGetter(binding.getParentBean(), binding));
    }

    public GetterGraph add(Bean bean, String property) {
        AssertArgument.isNotNull(bean, "bean");
        AssertArgument.isNotNullAndNotEmpty(property, "property");

        Getter getter = null;
        for (Binding binding : bean.getBindings()) {
            if (property.equals(binding.getProperty())) {
                getter = toGetter(bean, binding);
                break;
            }
        }

        if (getter == null) {
            throw new IllegalStateException("Failed to create Getter instance for property '" + property + "' on bean type '" + bean.getBeanClass().getName() + "'.");
        }
        add(getter);

        return this;
    }

    protected Getter toGetter(Bean bean, Binding binding) {
        if (Map.class.isAssignableFrom(bean.getBeanClass())) {
            return new MapGetter(binding.getProperty());
        } else {
            return new BeanGetter(bean.getBeanClass(), binding.getProperty());
        }
    }

    public String getContextObjectName() {
        return contextObjectName;
    }

    public void setContextObjectName(String contextObjectName) {
        this.contextObjectName = contextObjectName;
    }
}
