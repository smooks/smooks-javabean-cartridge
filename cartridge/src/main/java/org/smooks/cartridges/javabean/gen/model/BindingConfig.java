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
package org.smooks.cartridges.javabean.gen.model;

import org.smooks.converter.TypeConverterFactoryLoader;
import org.smooks.converter.factory.TypeConverterFactory;
import org.smooks.registry.lookup.converter.SourceTargetTypeConverterFactoryLookup;

import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.util.Set;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class BindingConfig {
    private static final Set<TypeConverterFactory<?, ?>> TYPE_CONVERTER_FACTORIES = new TypeConverterFactoryLoader().load();

    private Field property;
    private String wireBeanId;

    public BindingConfig(Field property) {
        this.property = property;
    }

    public BindingConfig(String wireBeanId) {
        this.wireBeanId = wireBeanId;
    }

    public BindingConfig(Field property, String wireBeanId) {
        this.property = property;
        this.wireBeanId = wireBeanId;
    }

    public Field getProperty() {
        return property;
    }

    public String getSelector() {
        if(wireBeanId != null) {
            return wireBeanId;
        }

        return "$TODO$";
    }

    public boolean isWiring() {
        return (wireBeanId != null);
    }

    public boolean isBoundToProperty() {
        return (property != null);
    }

    public String getType() {
        Class type = property.getType();

        if (type.isArray()) {
            return "$DELETE:NOT-APPLICABLE$";
        }

        final TypeConverterFactory<? extends String, ?> typeConverterFactory = new SourceTargetTypeConverterFactoryLookup<String, Object>(String.class, type).lookup(TYPE_CONVERTER_FACTORIES);
        if (typeConverterFactory != null && typeConverterFactory.getClass().isAnnotationPresent(Resource.class) && !typeConverterFactory.getClass().getAnnotation(Resource.class).name().equals("")) {
            return typeConverterFactory.getClass().getAnnotation(Resource.class).name();
        } else {
            return "$TODO$";
        }
    }
}
