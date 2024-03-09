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
package org.smooks.cartridges.javabean.binding.model;

import org.smooks.api.resource.config.ResourceConfig;
import org.smooks.cartridges.javabean.BeanInstanceCreator;
import org.smooks.cartridges.javabean.BeanRuntimeInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Bean.
 *
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class Bean {

    private BeanInstanceCreator creator;
    private String beanId;
    private boolean cloneable = false;
    private Bean wiredInto;
    private List<Binding> bindings = new ArrayList<Binding>();

    public Bean(BeanInstanceCreator creator) {
        this.creator = creator;
        this.beanId = creator.getBeanId();
    }

    public ResourceConfig getConfig() {
        return creator.getConfig();
    }

    public BeanInstanceCreator getCreator() {
        return creator;
    }

    public String getBeanId() {
        return beanId;
    }

    public Bean setCloneable(boolean cloneable) {
        this.cloneable = cloneable;
        return this;
    }

    public Bean getWiredInto() {
        return wiredInto;
    }

    public Bean wiredInto(Bean wiredInto) {
        if (cloneable) {
            throw new IllegalStateException("Illegal wiring of a bean that is cloneable.  Only non cloneable beans (e.g. non base beans) can be wired together.");
        }

        this.wiredInto = wiredInto;
        return this;
    }

    public List<Binding> getBindings() {
        return bindings;
    }

    public WiredBinding getWiredBinding(Bean wiredBean) {
        for (Binding binding : bindings) {
            if (binding instanceof WiredBinding) {
                WiredBinding wiredBinding = (WiredBinding) binding;
                if (wiredBinding.getWiredBean() == wiredBean) {
                    return wiredBinding;
                }
            }
        }

        return null;
    }

    protected Bean clone(Map<String, Bean> baseBeans, Bean parentBean) {
        if (!cloneable) {
            throw new IllegalStateException("Illegal call to clone a Bean instance that is not cloneable.");
        }

        Bean beanClone = new Bean(creator).wiredInto(parentBean);

        for (Binding binding : bindings) {
            Binding bindingClone = (Binding) binding.clone();

            bindingClone.setParentBean(beanClone);
            if (bindingClone instanceof WiredBinding) {
                WiredBinding wiredBinding = (WiredBinding) bindingClone;
                String wiredBeanId = wiredBinding.getWiredBeanId();
                Bean beanToBeWired = baseBeans.get(wiredBeanId);

                if (beanToBeWired != null) {
                    if (parentBean == null || (!parentBean.getBeanId().equals(wiredBeanId) && parentBean.getParentBean(wiredBeanId) == null)) {
                        wiredBinding.setWiredBean(beanToBeWired.clone(baseBeans, beanClone));
                        beanClone.bindings.add(wiredBinding);
                    }
                }
            } else {
                beanClone.bindings.add(bindingClone);
            }
        }

        return beanClone;
    }

    public Bean getParentBean(String beanId) {
        if (wiredInto != null) {
            if (wiredInto.getBeanId().equals(beanId)) {
                return wiredInto;
            } else {
                return wiredInto.getParentBean(beanId);
            }
        }
        return null;
    }

    public Class<?> getBeanClass() {
        return creator.getBeanRuntimeInfo().getPopulateType();
    }

    public boolean isCollection() {
        BeanRuntimeInfo.Classification classification = creator.getBeanRuntimeInfo().getClassification();
        return (classification == BeanRuntimeInfo.Classification.COLLECTION_COLLECTION || classification == BeanRuntimeInfo.Classification.ARRAY_COLLECTION);
    }

    @Override
    public String toString() {
        return beanId;
    }
}
