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

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.smooks.api.ExecutionContext;
import org.smooks.api.SmooksConfigException;
import org.smooks.api.SmooksException;
import org.smooks.api.TypedKey;
import org.smooks.api.bean.context.BeanContext;
import org.smooks.api.bean.lifecycle.BeanLifecycle;
import org.smooks.api.resource.visitor.VisitAfterReport;
import org.smooks.api.resource.visitor.VisitBeforeReport;
import org.smooks.api.resource.visitor.sax.ng.ParameterizedVisitor;
import org.smooks.assertion.AssertArgument;
import org.smooks.engine.bean.lifecycle.DefaultBeanContextLifecycleEvent;
import org.smooks.engine.delivery.fragment.NodeFragment;
import org.smooks.engine.memento.VisitorMemento;
import org.w3c.dom.Element;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@VisitBeforeReport(summary = "Created <b>${resource.parameters.beanId!'undefined'}</b> bean instance. Associated lifecycle if wired to another bean.",
        detailTemplate = "reporting/JaxbBeanProducerReport_Before.html")
@VisitAfterReport(condition = "parameters.containsKey('setOn') || parameters.beanClass.value.endsWith('[]')",
        summary = "Ended bean lifecycle. Set bean on any targets.",
        detailTemplate = "reporting/JaxbBeanProducerReport_After.html")
public class JaxbBeanProducer extends AbstractBeanProducer implements ParameterizedVisitor {

    @Inject
    protected List<String> objectFactories = new ArrayList<>();

    protected JAXBContext jaxbContext;
    protected TypedKey<Unmarshaller> unmarshallerMementoTypedKey = TypedKey.of();

    /**
     * Public default constructor.
     */
    public JaxbBeanProducer() {
    }

    /**
     * Public default constructor.
     *
     * @param beanId    The beanId under which the bean instance is registered in the bean context.
     * @param beanClass The bean runtime class.
     */
    public <T> JaxbBeanProducer(String beanId, Class<T> beanClass) {
        AssertArgument.isNotNull(beanId, "beanId");
        AssertArgument.isNotNull(beanClass, "beanClass");

        this.beanIdName = beanId;
        this.beanClassName = toClassName(beanClass);
    }

    @Override
    public void doPostConstruct() {
        List<Class<?>> classesToBeBound = objectFactories.stream().map(of -> {
            try {
                return Class.forName(of, true, applicationContext.getClassLoader());
            } catch (ClassNotFoundException e) {
                throw new SmooksException(e);
            }
        }).collect(Collectors.toList());
        classesToBeBound.add(beanRuntimeInfo.getPopulateType());

        try {
            jaxbContext = JAXBContext.newInstance(classesToBeBound.toArray(new Class[0]));
        } catch (JAXBException e) {
            throw new SmooksConfigException(e);
        }
    }

    @Override
    public void visitAfter(Element element, ExecutionContext executionContext) throws SmooksException {
        NodeFragment nodeFragment = new NodeFragment(element);
        VisitorMemento<Unmarshaller> visitorMemento = new VisitorMemento<>(nodeFragment, this, unmarshallerMementoTypedKey);
        if (executionContext.getMementoCaretaker().exists(visitorMemento)) {
            visitorMemento.restore(visitorMemento);
        } else {
            try {
                visitorMemento = new VisitorMemento<>(nodeFragment, this, unmarshallerMementoTypedKey, jaxbContext.createUnmarshaller());
            } catch (JAXBException e) {
                throw new SmooksException(e);
            }
            executionContext.getMementoCaretaker().capture(visitorMemento);
        }

        Object bean;
        try {
            bean = visitorMemento.getState().unmarshal(element);
        } catch (JAXBException e) {
            throw new SmooksConfigException("Unable to create bean instance [" + beanIdName + ":" + beanRuntimeInfo.getPopulateType().getName() + "].", e);
        }

        BeanContext beanContext = executionContext.getBeanContext();
        beanContext.notifyObservers(new DefaultBeanContextLifecycleEvent(executionContext, nodeFragment, BeanLifecycle.END_FRAGMENT, beanId, bean));
        beanContext.addBean(beanId, bean, nodeFragment);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Bean [{}] instance created", beanIdName);
        }
    }

    @Override
    public int getMaxNodeDepth() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void visitBefore(Element element, ExecutionContext executionContext) {

    }
}
