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
package org.smooks.cartridges.javabean.dynamic.ext;

import org.smooks.api.ApplicationContext;
import org.smooks.api.SmooksConfigException;
import org.smooks.api.delivery.ContentHandler;
import org.smooks.api.resource.config.ResourceConfig;
import org.smooks.cartridges.javabean.dynamic.serialize.BeanWriter;
import org.smooks.cartridges.javabean.ext.BeanConfigUtil;
import org.smooks.engine.injector.Scope;
import org.smooks.engine.lifecycle.PostConstructLifecyclePhase;
import org.smooks.engine.lookup.LifecycleManagerLookup;

import jakarta.annotation.PostConstruct;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@link BeanWriter} Factory.
 *
 * <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
@SuppressWarnings({"WeakerAccess", "unchecked"})
public class BeanWriterFactory implements ContentHandler {

    @Inject
    protected String beanId;
    @Inject
    @Named("class")
    protected Class<? extends BeanWriter> beanWriterClass;
    @Inject
    @Named(BeanConfigUtil.BEAN_CLASS_CONFIG)
    protected Class<?> beanClass;
    @Inject
    protected ResourceConfig resourceConfig;
    @Inject
    protected ApplicationContext appContext;

    @PostConstruct
    public void createBeanWriter() {
        try {
            BeanWriter beanWriter = beanWriterClass.newInstance();
            appContext.getRegistry().lookup(new LifecycleManagerLookup()).applyPhase(beanWriter, new PostConstructLifecyclePhase(new Scope(appContext.getRegistry(), resourceConfig, beanWriter)));
            Map<String, BeanWriter> beanWriters = getBeanWriters(beanClass, appContext);
            for (Object namespaceUri : resourceConfig.getSelectorPath().getNamespaces().values()) {
                beanWriters.put((String) namespaceUri, beanWriter);
            }
        } catch (InstantiationException | IllegalAccessException e) {
            throw new SmooksConfigException("Unable to create BeanWriter instance.", e);
        }
    }

    public static Map<String, BeanWriter> getBeanWriters(Class<?> beanClass, ApplicationContext appContext) {
        Map<Class<?>, Map<String, BeanWriter>> beanWriterMap = getBeanWriters(appContext);
        Map<String, BeanWriter> beanWriters = beanWriterMap.computeIfAbsent(beanClass, k -> new LinkedHashMap<>());

        return beanWriters;
    }

    public static Map<Class<?>, Map<String, BeanWriter>> getBeanWriters(ApplicationContext appContext) {
        Map<Class<?>, Map<String, BeanWriter>> beanWriters = appContext.getRegistry().lookup(BeanWriter.class);

        if (beanWriters == null) {
            beanWriters = new HashMap<>();
            appContext.getRegistry().registerObject(BeanWriter.class, beanWriters);
        }

        return beanWriters;
    }
}
