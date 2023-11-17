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
package org.smooks.cartridges.javabean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smooks.api.ApplicationContext;
import org.smooks.api.ExecutionContext;
import org.smooks.api.SmooksConfigException;
import org.smooks.api.SmooksException;
import org.smooks.api.bean.context.BeanContext;
import org.smooks.api.bean.lifecycle.BeanLifecycle;
import org.smooks.api.bean.repository.BeanId;
import org.smooks.api.delivery.event.ContentDeliveryConfigBuilderLifecycleEvent;
import org.smooks.api.delivery.event.ContentDeliveryConfigBuilderLifecycleListener;
import org.smooks.api.delivery.fragment.Fragment;
import org.smooks.api.delivery.ordering.Producer;
import org.smooks.api.lifecycle.VisitLifecycleCleanable;
import org.smooks.api.resource.config.Parameter;
import org.smooks.api.resource.config.ResourceConfig;
import org.smooks.api.resource.visitor.VisitAfterReport;
import org.smooks.api.resource.visitor.VisitBeforeReport;
import org.smooks.api.resource.visitor.sax.ng.AfterVisitor;
import org.smooks.api.resource.visitor.sax.ng.BeforeVisitor;
import org.smooks.assertion.AssertArgument;
import org.smooks.cartridges.javabean.binding.model.ModelSet;
import org.smooks.cartridges.javabean.ext.BeanConfigUtil;
import org.smooks.cartridges.javabean.factory.Factory;
import org.smooks.cartridges.javabean.factory.FactoryDefinitionParser.FactoryDefinitionParserFactory;
import org.smooks.engine.bean.lifecycle.DefaultBeanContextLifecycleEvent;
import org.smooks.engine.delivery.fragment.NodeFragment;
import org.smooks.engine.expression.MVELExpressionEvaluator;
import org.w3c.dom.Element;

import jakarta.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Bean instance creator visitor class.
 * <p/>
 *
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
@VisitBeforeReport(summary = "Created <b>${resource.parameters.beanId!'undefined'}</b> bean instance.  Associated lifecycle if wired to another bean.",
        detailTemplate = "reporting/BeanInstanceCreatorReport_Before.html")
@VisitAfterReport(condition = "parameters.containsKey('setOn') || parameters.beanClass.value.endsWith('[]')",
        summary = "Ended bean lifecycle. Set bean on any targets.",
        detailTemplate = "reporting/BeanInstanceCreatorReport_After.html")
public class BeanInstanceCreator implements BeforeVisitor, AfterVisitor, ContentDeliveryConfigBuilderLifecycleListener, Producer, VisitLifecycleCleanable {

    private static final Logger LOGGER = LoggerFactory.getLogger(BeanInstanceCreator.class);

    public static final String INIT_VAL_EXPRESSION = "initValExpression";

    private String id;

    @Inject
    @Named("beanId")
    private String beanIdName;

    @Inject
    @Named(BeanConfigUtil.BEAN_CLASS_CONFIG)
    private Optional<String> beanClassName;

    @Inject
    @Named("beanFactory")
    private Optional<String> beanFactoryDefinition;

    @Inject
    private Boolean retain = true;

    @Inject
    private ResourceConfig config;

    @Inject
    private ApplicationContext appContext;

    private BeanRuntimeInfo beanRuntimeInfo;

    private BeanId beanId;

    private MVELExpressionEvaluator initValsExpression;

    private Factory<?> factory;

    /**
     * Public default constructor.
     */
    public BeanInstanceCreator() {
    }

    /**
     * Public default constructor.
     *
     * @param beanId    The beanId under which the bean instance is registered in the bean context.
     * @param beanClass The bean runtime class.
     */
    public BeanInstanceCreator(String beanId, Class<?> beanClass) {
        this(beanId, beanClass, null);
    }

    /**
     * Public default constructor.
     *
     * @param beanId    The beanId under which the bean instance is registered in the bean context.
     * @param beanClass The bean runtime class.
     */
    public <T> BeanInstanceCreator(String beanId, Class<T> beanClass, Factory<? extends T> factory) {
        AssertArgument.isNotNull(beanId, "beanId");
        AssertArgument.isNotNull(beanClass, "beanClass");

        this.beanIdName = beanId;
        this.beanClassName = Optional.of(toClassName(beanClass));
        this.factory = factory;
    }

    /**
     * Get the beanId of this Bean configuration.
     *
     * @return The beanId of this Bean configuration.
     */
    public String getBeanId() {
        return beanIdName;
    }

    public ResourceConfig getConfig() {
        return config;
    }

    /**
     * Set the resource configuration on the bean populator.
     *
     * @throws org.smooks.api.SmooksConfigException Incorrectly configured resource.
     */
    @PostConstruct
    public void postConstruct() throws SmooksConfigException {
        buildId();

        beanId = appContext.getBeanIdStore().register(beanIdName);
        beanId.setCreateResourceConfiguration(config);

        if (!beanFactoryDefinition.orElse("").isEmpty()) {
            String alias = null;
            String definition = beanFactoryDefinition.get();

            if (!definition.contains("#")) {
                try {
                    URI definitionURI = new URI(definition);
                    if (definitionURI.getScheme() == null) {
                        // Default it to MVEL...
                        definition = "mvel:" + definition;
                    }
                } catch (URISyntaxException e) {
                    // Let it run...
                }
            }

            int aliasSplitterIndex = definition.indexOf(':');
            if (aliasSplitterIndex > 0) {
                alias = definition.substring(0, aliasSplitterIndex);
                definition = definition.substring(aliasSplitterIndex + 1);
            }

            factory = FactoryDefinitionParserFactory.getInstance(alias, appContext).parse(definition);
        }

        beanRuntimeInfo = BeanRuntimeInfo.getBeanRuntimeInfo(beanIdName, beanClassName.orElse(null), appContext);

        if (factory == null) {
            checkForDefaultConstructor();
        } else if (beanRuntimeInfo.getClassification() == BeanRuntimeInfo.Classification.ARRAY_COLLECTION) {
            throw new SmooksConfigException("Using a factory with an array is not supported");
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("BeanInstanceCreator created for [" + beanIdName + "]. BeanRuntimeInfo: " + beanRuntimeInfo);
        }

        List<Parameter<?>> initValExpressions = config.getParameters(INIT_VAL_EXPRESSION);
        if (initValExpressions != null && !initValExpressions.isEmpty()) {
            StringBuilder initValsExpressionString = new StringBuilder();

            for (Parameter<?> initValExpression : initValExpressions) {
                initValsExpressionString.append(initValExpression.getValue());
                initValsExpressionString.append("\n");
            }

            initValsExpression = new MVELExpressionEvaluator();
            initValsExpression.setExpression(initValsExpressionString.toString());
        }
    }

    @Override
    public void handle(ContentDeliveryConfigBuilderLifecycleEvent event) throws SmooksConfigException {
        if (event == ContentDeliveryConfigBuilderLifecycleEvent.CONTENT_DELIVERY_BUILDER_CREATED) {
            ModelSet.build(appContext);
        }
    }

    /**
     * Get the bean runtime information.
     *
     * @return The bean runtime information.
     */
    public BeanRuntimeInfo getBeanRuntimeInfo() {
        return beanRuntimeInfo;
    }

    private void buildId() {
        StringBuilder idBuilder = new StringBuilder();
        idBuilder.append(BeanInstanceCreator.class.getName());
        idBuilder.append("#");
        idBuilder.append(beanIdName);

        id = idBuilder.toString();
    }
    
    @Override
    public void visitBefore(Element element, ExecutionContext executionContext) throws SmooksException {
        createAndSetBean(executionContext, new NodeFragment(element));
    }

    /* (non-Javadoc)
     * @see org.smooks.delivery.sax.SAXVisitAfter#visitAfter(org.smooks.delivery.sax.SAXElement, org.smooks.api.ExecutionContext)
     */
    @Override
    public void visitAfter(Element element, ExecutionContext executionContext) throws SmooksException {
        visitAfter(executionContext, new NodeFragment(element));
    }

    public void visitAfter(ExecutionContext executionContext, Fragment source) {
        BeanRuntimeInfo.Classification thisBeanType = beanRuntimeInfo.getClassification();
        boolean isBeanTypeArray = (thisBeanType == BeanRuntimeInfo.Classification.ARRAY_COLLECTION);

        BeanContext beanContext = executionContext.getBeanContext();
        beanContext.setBeanInContext(beanId, false);

        if (isBeanTypeArray) {
            Object bean = beanContext.getBean(beanId);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Converting bean [" + beanIdName + "] to an array and rebinding to context.");
            }
            bean = convert(executionContext, bean, source);
        }
    }


    private Object convert(ExecutionContext executionContext, Object bean, Fragment source) {

        bean = BeanUtils.convertListToArray((List<?>) bean, beanRuntimeInfo.getArrayType());

        executionContext.getBeanContext().changeBean(beanId, bean, source);

        return bean;
    }

    private void createAndSetBean(ExecutionContext executionContext, Fragment source) {
        Object bean;
        BeanContext beanContext = executionContext.getBeanContext();

        bean = createBeanInstance(executionContext);

        executionContext.getBeanContext().notifyObservers(new DefaultBeanContextLifecycleEvent(executionContext, source, BeanLifecycle.START_FRAGMENT, beanId, bean));

        if (initValsExpression != null) {
            initValsExpression.exec(bean);
        }

        beanContext.setBeanInContext(beanId, false);
        beanContext.addBean(beanId, bean, source);
        beanContext.setBeanInContext(beanId, true);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Bean [" + beanIdName + "] instance created.");
        }
    }

    /**
     * Create a new bean instance, generating relevant configuration exceptions.
     *
     * @return A new bean instance.
     */
    private Object createBeanInstance(ExecutionContext executionContext) {
        Object bean;

        if (factory == null) {
            try {
                bean = beanRuntimeInfo.getPopulateType().newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new SmooksConfigException("Unable to create bean instance [" + beanIdName + ":" + beanRuntimeInfo.getPopulateType().getName() + "].", e);
            }
        } else {
            try {
                bean = factory.create(executionContext);
            } catch (RuntimeException e) {
                throw new SmooksConfigException("The factory was unable to create the bean instance [" + beanIdName + "] using the factory '" + factory + "'.", e);
            }
        }

        return bean;
    }

    public Set<?> getProducts() {
        return Stream.of(beanIdName).collect(Collectors.toSet());
    }

    private String getId() {
        return id;
    }

    @Override
    public String toString() {
        return getId();
    }

    private static String toClassName(Class<?> beanClass) {
        if (!beanClass.isArray()) {
            return beanClass.getName();
        } else {
            return beanClass.getComponentType().getName() + "[]";
        }
    }

    /**
     * Checks if the class has a default constructor
     */
    private void checkForDefaultConstructor() {
        try {
            beanRuntimeInfo.getPopulateType().getConstructor();
        } catch (NoSuchMethodException e) {
            throw new SmooksConfigException("Invalid Smooks bean configuration.  Bean class " + beanRuntimeInfo.getPopulateType().getName() + " doesn't have a public default constructor.");
        }
    }

    @Override
    public void executeVisitLifecycleCleanup(Fragment fragment, ExecutionContext executionContext) {
        BeanContext beanContext = executionContext.getBeanContext();
        Object bean = beanContext.getBean(beanId);

        beanContext.notifyObservers(new DefaultBeanContextLifecycleEvent(executionContext, fragment, BeanLifecycle.END_FRAGMENT, beanId, bean));

        if (!retain) {
            beanContext.removeBean(beanId, null);
        }
    }
}
