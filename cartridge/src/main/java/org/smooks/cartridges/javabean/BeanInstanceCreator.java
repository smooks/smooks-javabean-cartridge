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

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smooks.SmooksException;
import org.smooks.assertion.AssertArgument;
import org.smooks.cartridges.javabean.binding.model.ModelSet;
import org.smooks.cartridges.javabean.ext.BeanConfigUtil;
import org.smooks.cartridges.javabean.factory.Factory;
import org.smooks.cartridges.javabean.factory.FactoryDefinitionParser.FactoryDefinitionParserFactory;
import org.smooks.cdr.Parameter;
import org.smooks.cdr.SmooksConfigurationException;
import org.smooks.cdr.SmooksResourceConfiguration;
import org.smooks.container.ApplicationContext;
import org.smooks.container.ExecutionContext;
import org.smooks.delivery.ContentDeliveryConfigBuilderLifecycleEvent;
import org.smooks.delivery.ContentDeliveryConfigBuilderLifecycleListener;
import org.smooks.delivery.Fragment;
import org.smooks.delivery.VisitLifecycleCleanable;
import org.smooks.delivery.dom.DOMElementVisitor;
import org.smooks.delivery.ordering.Producer;
import org.smooks.delivery.sax.SAXElement;
import org.smooks.delivery.sax.SAXVisitAfter;
import org.smooks.delivery.sax.SAXVisitBefore;
import org.smooks.event.report.annotation.VisitAfterReport;
import org.smooks.event.report.annotation.VisitBeforeReport;
import org.smooks.expression.MVELExpressionEvaluator;
import org.smooks.javabean.context.BeanContext;
import org.smooks.javabean.lifecycle.BeanContextLifecycleEvent;
import org.smooks.javabean.lifecycle.BeanLifecycle;
import org.smooks.javabean.repository.BeanId;
import org.smooks.util.CollectionsUtil;
import org.w3c.dom.Element;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
public class BeanInstanceCreator implements DOMElementVisitor, SAXVisitBefore, SAXVisitAfter, ContentDeliveryConfigBuilderLifecycleListener, Producer, VisitLifecycleCleanable {

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
    private SmooksResourceConfiguration config;

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

    public SmooksResourceConfiguration getConfig() {
        return config;
    }

    /**
     * Set the resource configuration on the bean populator.
     *
     * @throws SmooksConfigurationException Incorrectly configured resource.
     */
    @PostConstruct
    public void initialize() throws SmooksConfigurationException {
        buildId();

        beanId = appContext.getBeanIdStore().register(beanIdName);
        beanId.setCreateResourceConfiguration(config);

        if (StringUtils.isNotBlank(beanFactoryDefinition.orElse(null))) {
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
            throw new SmooksConfigurationException("Using a factory with an array is not supported");
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("BeanInstanceCreator created for [" + beanIdName + "]. BeanRuntimeInfo: " + beanRuntimeInfo);
        }

        List<Parameter> initValExpressions = config.getParameters(INIT_VAL_EXPRESSION);
        if (initValExpressions != null && !initValExpressions.isEmpty()) {
            StringBuilder initValsExpressionString = new StringBuilder();

            for (Parameter initValExpression : initValExpressions) {
                initValsExpressionString.append(initValExpression.getValue());
                initValsExpressionString.append("\n");
            }

            initValsExpression = new MVELExpressionEvaluator();
            initValsExpression.setExpression(initValsExpressionString.toString());
        }
    }

    public void handle(ContentDeliveryConfigBuilderLifecycleEvent event) throws SmooksConfigurationException {
        if (event == ContentDeliveryConfigBuilderLifecycleEvent.CONFIG_BUILDER_CREATED) {
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

    public void visitBefore(Element element, ExecutionContext executionContext) throws SmooksException {
        createAndSetBean(executionContext, new Fragment(element));
    }

    public void visitBefore(SAXElement element, ExecutionContext executionContext) throws SmooksException, IOException {
        createAndSetBean(executionContext, new Fragment(element));
    }


    /* (non-Javadoc)
     * @see org.smooks.delivery.dom.DOMVisitAfter#visitAfter(org.w3c.dom.Element, org.smooks.container.ExecutionContext)
     */
    public void visitAfter(Element element, ExecutionContext executionContext) throws SmooksException {
        visitAfter(executionContext, new Fragment(element));
    }

    /* (non-Javadoc)
     * @see org.smooks.delivery.sax.SAXVisitAfter#visitAfter(org.smooks.delivery.sax.SAXElement, org.smooks.container.ExecutionContext)
     */
    public void visitAfter(SAXElement element, ExecutionContext executionContext) throws SmooksException, IOException {
        visitAfter(executionContext, new Fragment(element));
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

        executionContext.getBeanContext().notifyObservers(new BeanContextLifecycleEvent(executionContext,
                source, BeanLifecycle.START_FRAGMENT, beanId, bean));

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
                throw new SmooksConfigurationException("Unable to create bean instance [" + beanIdName + ":" + beanRuntimeInfo.getPopulateType().getName() + "].", e);
            }
        } else {
            try {
                bean = factory.create(executionContext);
            } catch (RuntimeException e) {
                throw new SmooksConfigurationException("The factory was unable to create the bean instance [" + beanIdName + "] using the factory '" + factory + "'.", e);
            }
        }

        return bean;
    }

    public Set<? extends Object> getProducts() {
        return CollectionsUtil.toSet(beanIdName);
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
            throw new SmooksConfigurationException("Invalid Smooks bean configuration.  Bean class " + beanRuntimeInfo.getPopulateType().getName() + " doesn't have a public default constructor.");
        }
    }

    public void executeVisitLifecycleCleanup(Fragment fragment, ExecutionContext executionContext) {
        BeanContext beanContext = executionContext.getBeanContext();
        Object bean = beanContext.getBean(beanId);

        beanContext.notifyObservers(new BeanContextLifecycleEvent(executionContext,
                fragment, BeanLifecycle.END_FRAGMENT, beanId, bean));

        if (!retain) {
            beanContext.removeBean(beanId, null);
        }
    }
}
