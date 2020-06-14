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

import org.smooks.cartridges.javabean.dynamic.serialize.BeanWriter;
import org.smooks.cartridges.javabean.ext.BeanConfigUtil;
import org.smooks.cdr.SmooksConfigurationException;
import org.smooks.cdr.SmooksResourceConfiguration;
import org.smooks.cdr.annotation.AppContext;
import org.smooks.cdr.annotation.Config;
import org.smooks.cdr.annotation.ConfigParam;
import org.smooks.cdr.annotation.Configurator;
import org.smooks.container.ApplicationContext;
import org.smooks.delivery.ContentHandler;
import org.smooks.delivery.annotation.Initialize;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@link BeanWriter} Factory.
 *
 * <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
@SuppressWarnings({ "WeakerAccess", "unchecked" })
public class BeanWriterFactory implements ContentHandler {

    @ConfigParam
    private String beanId;
    @ConfigParam(name = "class")
    private Class<? extends BeanWriter> beanWriterClass;
    @ConfigParam(name = BeanConfigUtil.BEAN_CLASS_CONFIG)
    private Class<?> beanClass;
    @Config
    private SmooksResourceConfiguration config;
    @AppContext
    private ApplicationContext appContext;

    @Initialize
    public void createBeanWriter() {
        try {
            BeanWriter beanWriter = beanWriterClass.newInstance();

            Configurator.configure(beanWriter, config, appContext);
            getBeanWriters(beanClass, appContext).put(config.getSelectorNamespaceURI(), beanWriter);
        } catch (InstantiationException e) {
            throw new SmooksConfigurationException("Unable to create BeanWriter instance.", e);
        } catch (IllegalAccessException e) {
            throw new SmooksConfigurationException("Unable to create BeanWriter instance.", e);
        }
    }

    public static Map<String, BeanWriter> getBeanWriters(Class<?> beanClass, ApplicationContext appContext) {
        Map<Class<?>, Map<String, BeanWriter>> beanWriterMap = getBeanWriters(appContext);
        Map<String, BeanWriter> beanWriters = beanWriterMap.get(beanClass);

        if(beanWriters == null) {
            beanWriters = new LinkedHashMap<String, BeanWriter>();
            beanWriterMap.put(beanClass, beanWriters);
        }

        return beanWriters;
    }

    public static Map<Class<?>, Map<String, BeanWriter>> getBeanWriters(ApplicationContext appContext) {
        Map<Class<?>, Map<String, BeanWriter>> beanWriters = (Map<Class<?>, Map<String, BeanWriter>>) appContext.getAttribute(BeanWriter.class);

        if(beanWriters == null) {
            beanWriters = new HashMap<Class<?>, Map<String, BeanWriter>>();
            appContext.setAttribute(BeanWriter.class, beanWriters);
        }

        return beanWriters;
    }
}
