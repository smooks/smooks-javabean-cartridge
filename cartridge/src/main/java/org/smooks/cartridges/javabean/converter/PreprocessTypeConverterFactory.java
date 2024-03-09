/*-
 * ========================LICENSE_START=================================
 * smooks-javabean-cartridge
 * %%
 * Copyright (C) 2020 - 2024 Smooks
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
package org.smooks.cartridges.javabean.converter;

import jakarta.annotation.PostConstruct;
import org.smooks.api.ApplicationContext;
import org.smooks.api.SmooksConfigException;
import org.smooks.api.converter.TypeConverter;
import org.smooks.api.converter.TypeConverterDescriptor;
import org.smooks.api.converter.TypeConverterException;
import org.smooks.api.converter.TypeConverterFactory;
import org.smooks.api.resource.config.Configurable;
import org.smooks.engine.lookup.converter.NameTypeConverterFactoryLookup;
import org.smooks.support.ClassUtils;

import javax.inject.Inject;
import java.util.Properties;

public class PreprocessTypeConverterFactory implements Configurable, TypeConverterFactory<String, Object> {

    public static final String VALUE_PRE_PROCESSING = "valuePreprocess";
    public static final String DELEGATE_TYPE_CONVERTER_FACTORY = "baseDecoder";

    @Inject
    protected ApplicationContext applicationContext;

    protected Properties properties;
    protected TypeConverterFactory<? super String, ?> delegateTypeConverterFactory;

    @PostConstruct
    public void postConstruct() {
        String delegateTypeConverterFactoryName = properties.getProperty(DELEGATE_TYPE_CONVERTER_FACTORY);

        if (delegateTypeConverterFactoryName != null) {
            try {
                final Class<TypeConverterFactory<?, ?>> typeConverterFactoryClass = (Class<TypeConverterFactory<?, ?>>) ClassUtils.forName(delegateTypeConverterFactoryName, TypeConverterFactory.class);
                try {
                    delegateTypeConverterFactory = (TypeConverterFactory<String, Object>) typeConverterFactoryClass.newInstance();
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new TypeConverterException("Failed to load TypeConverterFactory class '" + typeConverterFactoryClass.getName() + "'.", e);
                }
            } catch (ClassNotFoundException e) {
                delegateTypeConverterFactory = (TypeConverterFactory<String, Object>) applicationContext.getRegistry().lookup(new NameTypeConverterFactoryLookup(delegateTypeConverterFactoryName));
            }
        }
    }

    @Override
    public TypeConverter<? super String, ?> createTypeConverter() {
        TypeConverter<? super String, ?> delegateTypeConverter = delegateTypeConverterFactory.createTypeConverter();
        if (delegateTypeConverter instanceof Configurable) {
            ((Configurable) delegateTypeConverter).setConfiguration(properties);
        }

        return new PreprocessTypeConverter(properties.getProperty(VALUE_PRE_PROCESSING), delegateTypeConverter);
    }

    @Override
    public TypeConverterDescriptor<Class<String>, Class<Object>> getTypeConverterDescriptor() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setConfiguration(Properties properties) throws SmooksConfigException {
        this.properties = properties;
    }

    @Override
    public Properties getConfiguration() {
        return properties;
    }

    public TypeConverterFactory<? super String, ?> getDelegateTypeConverterFactory() {
        return delegateTypeConverterFactory;
    }

    public void setDelegateTypeConverterFactory(TypeConverterFactory<? super String, ?> delegateTypeConverterFactory) {
        this.delegateTypeConverterFactory = delegateTypeConverterFactory;
    }
}
