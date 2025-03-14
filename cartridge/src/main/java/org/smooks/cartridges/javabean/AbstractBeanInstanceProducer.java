/*-
 * ========================LICENSE_START=================================
 * smooks-javabean-cartridge
 * %%
 * Copyright (C) 2020 - 2025 Smooks
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
package org.smooks.cartridges.javabean;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smooks.api.ApplicationContext;
import org.smooks.api.ExecutionContext;
import org.smooks.api.bean.context.BeanContext;
import org.smooks.api.bean.lifecycle.BeanLifecycle;
import org.smooks.api.bean.repository.BeanId;
import org.smooks.api.delivery.fragment.Fragment;
import org.smooks.api.delivery.ordering.Producer;
import org.smooks.api.lifecycle.PostFragmentLifecycle;
import org.smooks.api.resource.config.ResourceConfig;
import org.smooks.assertion.AssertArgument;
import org.smooks.engine.bean.lifecycle.DefaultBeanContextLifecycleEvent;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AbstractBeanInstanceProducer implements Producer, PostFragmentLifecycle {
    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractBeanInstanceProducer.class);

    protected String id;

    @Inject
    @Named("beanId")
    protected String beanIdName;

    @Inject
    @Named("beanClass")
    protected String beanClassName;

    @Inject
    protected Boolean retain = true;

    @Inject
    protected ResourceConfig resourceConfig;

    @Inject
    protected ApplicationContext applicationContext;

    protected BeanRuntimeInfo beanRuntimeInfo;

    protected BeanId beanId;

    /**
     * Set the resource configuration on the bean populator.
     *
     * @throws org.smooks.api.SmooksConfigException Incorrectly configured resource.
     */
    @PostConstruct
    public void postConstruct() {
        AssertArgument.isNotNull(beanClassName, "beanClassName");
        buildId();
        beanId = applicationContext.getBeanIdStore().register(beanIdName);
        beanRuntimeInfo = BeanRuntimeInfo.getBeanRuntimeInfo(beanIdName, beanClassName, applicationContext);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("[{}] created for [{}]. BeanRuntimeInfo: {}", getClass().getName(), beanIdName, beanRuntimeInfo);
        }
        doPostConstruct();
    }

    protected abstract void doPostConstruct();

    /**
     * Get the bean runtime information.
     *
     * @return The bean runtime information.
     */
    public BeanRuntimeInfo getBeanRuntimeInfo() {
        return beanRuntimeInfo;
    }

    protected void buildId() {
        StringBuilder idBuilder = new StringBuilder();
        idBuilder.append(BeanInstanceProducer.class.getName());
        idBuilder.append("#");
        idBuilder.append(beanIdName);

        id = idBuilder.toString();
    }

    @Override
    public void onPostFragment(Fragment<?> fragment, ExecutionContext executionContext) {
        BeanContext beanContext = executionContext.getBeanContext();
        Object bean = beanContext.getBean(beanId);

        beanContext.notifyObservers(new DefaultBeanContextLifecycleEvent(executionContext, fragment, BeanLifecycle.END_FRAGMENT, beanId, bean));

        if (!retain) {
            beanContext.removeBean(beanId, null);
        }
    }

    @Override
    public String toString() {
        return getId();
    }

    protected String getId() {
        return id;
    }

    @Override
    public Set<?> getProducts() {
        return Stream.of(beanIdName).collect(Collectors.toSet());
    }

    /**
     * Get the beanId of this Bean configuration.
     *
     * @return The beanId of this Bean configuration.
     */
    public String getBeanId() {
        return beanIdName;
    }

    public ResourceConfig getResourceConfig() {
        return resourceConfig;
    }

    protected String toClassName(Class<?> beanClass) {
        if (!beanClass.isArray()) {
            return beanClass.getName();
        } else {
            return beanClass.getComponentType().getName() + "[]";
        }
    }
}
