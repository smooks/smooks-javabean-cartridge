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
import org.smooks.SmooksException;
import org.smooks.cartridges.javabean.observers.BeanWiringObserver;
import org.smooks.cartridges.javabean.observers.ListToArrayChangeObserver;
import org.smooks.cdr.SmooksConfigurationException;
import org.smooks.cdr.SmooksResourceConfiguration;
import org.smooks.cdr.registry.lookup.NamespaceManagerLookup;
import org.smooks.cdr.registry.lookup.converter.NameTypeConverterFactoryLookup;
import org.smooks.cdr.registry.lookup.converter.SourceTargetTypeConverterFactoryLookup;
import org.smooks.container.ApplicationContext;
import org.smooks.container.ExecutionContext;
import org.smooks.converter.TypeConverter;
import org.smooks.converter.TypeConverterException;
import org.smooks.converter.factory.PreprocessTypeConverter;
import org.smooks.converter.factory.TypeConverterFactory;
import org.smooks.converter.factory.system.StringConverterFactory;
import org.smooks.delivery.ContentDeliveryConfig;
import org.smooks.delivery.Fragment;
import org.smooks.delivery.dom.DOMElementVisitor;
import org.smooks.delivery.ordering.Consumer;
import org.smooks.delivery.ordering.Producer;
import org.smooks.delivery.sax.SAXElement;
import org.smooks.delivery.sax.SAXUtil;
import org.smooks.delivery.sax.SAXVisitAfter;
import org.smooks.delivery.sax.SAXVisitBefore;
import org.smooks.event.report.annotation.VisitAfterReport;
import org.smooks.event.report.annotation.VisitBeforeReport;
import org.smooks.expression.MVELExpressionEvaluator;
import org.smooks.javabean.context.BeanContext;
import org.smooks.javabean.context.BeanIdStore;
import org.smooks.javabean.lifecycle.BeanContextLifecycleEvent;
import org.smooks.javabean.lifecycle.BeanLifecycle;
import org.smooks.javabean.repository.BeanId;
import org.smooks.util.ClassUtil;
import org.smooks.util.CollectionsUtil;
import org.smooks.xml.DomUtils;
import org.w3c.dom.Element;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Bean instance populator visitor class.
 * <p/>
 *
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 * @author <a href="mailto:maurice.zeijen@smies.com">maurice.zeijen@smies.com</a>
 */
@VisitBeforeReport(condition = "parameters.containsKey('wireBeanId') || parameters.containsKey('valueAttributeName')",
        summary = "<#if resource.parameters.wireBeanId??>Create bean lifecycle observer for bean <b>${resource.parameters.wireBeanId}</b>." +
        		  "<#else>Populating <b>${resource.parameters.beanId}</b> with the value from the attribute <b>${resource.parameters.valueAttributeName}</b>.</#if>",
        detailTemplate = "reporting/BeanInstancePopulatorReport_Before.html")
@VisitAfterReport(condition = "!parameters.containsKey('wireBeanId') && !parameters.containsKey('valueAttributeName')",
        summary = "Populating <b>${resource.parameters.beanId}</b> with a value from this element.",
        detailTemplate = "reporting/BeanInstancePopulatorReport_After.html")
public class BeanInstancePopulator implements DOMElementVisitor, SAXVisitBefore, SAXVisitAfter, Producer, Consumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(BeanInstancePopulator.class);

    private static final String EXPRESSION_VALUE_VARIABLE_NAME = "_VALUE";

    public static final String VALUE_ATTRIBUTE_NAME = "valueAttributeName";
    public static final String VALUE_ATTRIBUTE_PREFIX = "valueAttributePrefix";

    public static final String NOTIFY_POPULATE = "org.smooks.cartridges.javabean.notify.populate";

    private String id;

    @Inject
    @Named("beanId")
    private String beanIdName;

    @Inject
    @Named("wireBeanId")
    private Optional<String> wireBeanIdName;

    @Inject
    private Optional<Class<?>> wireBeanType;

    @Inject
    private Optional<Class<? extends Annotation>> wireBeanAnnotation;

    @Inject
    private Optional<String> expression;
    private MVELExpressionEvaluator expressionEvaluator;
    private boolean expressionHasDataVariable = false;

    @Inject
    private Optional<String> property;

    @Inject
    private Optional<String> setterMethod;

    @Inject
    private Optional<String> valueAttributeName;

    @Inject
    private Optional<String> valueAttributePrefix;
    private String valueAttributeNS;

    @Inject
    @Named("type")
    private Optional<String> typeAlias;

    @Inject
    @Named("default")
    private Optional<String> defaultVal;

    @Inject
    @Named(NOTIFY_POPULATE)
    private Boolean notifyPopulate = false;

    @Inject
    private SmooksResourceConfiguration config;

    @Inject
    private ApplicationContext appContext;

    private BeanIdStore beanIdStore;

    private BeanId beanId;

    private BeanId wireBeanId;

    private BeanRuntimeInfo beanRuntimeInfo;

    private BeanRuntimeInfo wiredBeanRuntimeInfo;
    private Method propertySetterMethod;
    private boolean checkedForSetterMethod;
    private boolean isAttribute = true;
    private TypeConverter<? super String, ?> typeConverter;

    private String mapKeyAttribute;

    private boolean isBeanWiring;
    private BeanWiringObserver wireByBeanIdObserver;
    private ListToArrayChangeObserver listToArrayChangeObserver;

    public SmooksResourceConfiguration getConfig() {
        return config;
    }

    public void setBeanId(String beanId) {
        this.beanIdName = beanId;
    }

    public String getBeanId() {
        return beanIdName;
    }

    public void setWireBeanId(String wireBeanId) {
        this.wireBeanIdName = Optional.ofNullable(wireBeanId);
    }

    public String getWireBeanId() {
        return wireBeanIdName.orElse(null);
    }

    public void setExpression(MVELExpressionEvaluator expression) {
        this.expressionEvaluator = expression;
    }

    public void setProperty(String property) {
        this.property = Optional.ofNullable(property);
    }

    public String getProperty() {
        return property.orElse(null);
    }

    public void setSetterMethod(String setterMethod) {
        this.setterMethod = Optional.ofNullable(setterMethod);
    }

    public void setValueAttributeName(String valueAttributeName) {
        this.valueAttributeName = Optional.ofNullable(valueAttributeName);
    }

    public void setValueAttributePrefix(String valueAttributePrefix) {
        this.valueAttributePrefix = Optional.ofNullable(valueAttributePrefix);
    }

    public void setTypeAlias(String typeAlias) {
        this.typeAlias = Optional.ofNullable(typeAlias);
    }

    public void setTypeConverter(TypeConverter<? super String, ?> typeConverter) {
        this.typeConverter = typeConverter;
    }

    public TypeConverter<? super String, ?> getTypeConverter() {
        return typeConverter;
    }

    public void setDefaultVal(String defaultVal) {
        this.defaultVal = Optional.ofNullable(defaultVal);
    }

    public boolean isBeanWiring() {
        return isBeanWiring;
    }

    /**
     * Set the resource configuration on the bean populator.
     *
     * @throws SmooksConfigurationException Incorrectly configured resource.
     */
    @PostConstruct
    public void initialize() throws SmooksConfigurationException {
        buildId();

        beanRuntimeInfo = BeanRuntimeInfo.getBeanRuntimeInfo(beanIdName, appContext);
        isBeanWiring = wireBeanIdName.isPresent() || wireBeanType.isPresent() || wireBeanAnnotation.isPresent();
        isAttribute = valueAttributeName.isPresent();

        if (valueAttributePrefix.isPresent()) {
            Properties namespaces = appContext.getRegistry().lookup(new NamespaceManagerLookup());
            valueAttributeNS = namespaces.getProperty(valueAttributePrefix.get());
        }

        beanIdStore = appContext.getBeanIdStore();
        beanId = beanIdStore.getBeanId(beanIdName);

        if (!setterMethod.isPresent() && !property.isPresent()) {
            if (isBeanWiring && (beanRuntimeInfo.getClassification() == BeanRuntimeInfo.Classification.NON_COLLECTION || beanRuntimeInfo.getClassification() == BeanRuntimeInfo.Classification.MAP_COLLECTION)) {
                // Default the property name if it's a wiring...
                property = Optional.of(wireBeanIdName.get());
            } else if (beanRuntimeInfo.getClassification() == BeanRuntimeInfo.Classification.NON_COLLECTION) {
                throw new SmooksConfigurationException("Binding configuration for beanIdName='" + beanIdName + "' must contain " +
                        "either a 'property' or 'setterMethod' attribute definition, unless the target bean is a Collection/Array." +
                        "  Bean is type '" + beanRuntimeInfo.getPopulateType().getName() + "'.");
            }
        }

        if (beanRuntimeInfo.getClassification() == BeanRuntimeInfo.Classification.MAP_COLLECTION && property.isPresent()) {
            property = Optional.of(property.get().trim());
            if (property.get().length() > 1 && property.get().charAt(0) == '@') {
                mapKeyAttribute = property.get().substring(1);
            }
        }

        if (expression.isPresent()) {
            expression = Optional.of(expression.get().trim());

            expressionHasDataVariable = expression.get().contains(EXPRESSION_VALUE_VARIABLE_NAME);

            expression = Optional.of(expression.get().replace("this.", beanIdName + "."));
            if (expression.get().startsWith("+=")) {
                expression = Optional.of(beanIdName + "." + property.orElse(null) + " +" + expression.get().substring(2));
            }
            if (expression.get().startsWith("-=")) {
                expression = Optional.of(beanIdName + "." + property.orElse(null) + " -" + expression.get().substring(2));
            }

            expressionEvaluator = new MVELExpressionEvaluator();
            expressionEvaluator.setExpression(expression.get());

            // If we can determine the target binding type, tell MVEL.
            // If there's a decoder (a typeAlias), we define a String var instead and leave decoding
            // to the decoder...
            Class<?> bindingType = resolveBindTypeReflectively();
            if (bindingType != null) {
                if (typeAlias.isPresent()) {
                    bindingType = String.class;
                }
                expressionEvaluator.setToType(bindingType);
            }
        }

        if (wireBeanIdName.isPresent()) {
            wireBeanId = beanIdStore.getBeanId(wireBeanIdName.get());
            if (wireBeanId == null) {
                wireBeanId = beanIdStore.register(wireBeanIdName.get());
            }
        }

        if (isBeanWiring) {
            // These observers can be used concurrently across multiple execution contexts...
            wireByBeanIdObserver = new BeanWiringObserver(beanId, this).watchedBeanId(wireBeanId).watchedBeanType(wireBeanType.orElse(null)).watchedBeanAnnotation(wireBeanAnnotation.orElse(null));
            if (wireBeanId != null) {
                // List to array change observer only makes sense if wiring by beanId.
                listToArrayChangeObserver = new ListToArrayChangeObserver(wireBeanId, property.orElse(null), this);
            }
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Bean Instance Populator created for [" + beanIdName + "].  property=" + property.orElse(null));
        }
    }

    private void buildId() {
        StringBuilder idBuilder = new StringBuilder();
        idBuilder.append(BeanInstancePopulator.class.getName());
        idBuilder.append("#");
        idBuilder.append(beanIdName);

        property.ifPresent(s -> idBuilder.append("#")
                .append(s));
        setterMethod.ifPresent(s -> idBuilder.append("#")
                .append(s)
                .append("()"));
        wireBeanIdName.ifPresent(s -> idBuilder.append("#")
                .append(s));

        id = idBuilder.toString();
    }

    public void visitBefore(Element element, ExecutionContext executionContext) throws SmooksException {
        if (!beanExists(executionContext)) {
            LOGGER.debug("Cannot bind data onto bean '" + beanId + "' as bean does not exist in BeanContext.");
            return;
        }

        if (isBeanWiring) {
            bindBeanValue(executionContext, new Fragment(element));
        } else if (isAttribute) {
            // Bind attribute (i.e. selectors with '@' prefix) values on the visitBefore...
            bindDomDataValue(element, executionContext);
        }
    }

    public void visitAfter(Element element, ExecutionContext executionContext) throws SmooksException {
        if (!beanExists(executionContext)) {
            LOGGER.debug("Cannot bind data onto bean '" + beanId + "' as bean does not exist in BeanContext.");
            return;
        }

        if (!isBeanWiring && !isAttribute) {
            bindDomDataValue(element, executionContext);
        }
    }

    public void visitBefore(SAXElement element, ExecutionContext executionContext) throws SmooksException, IOException {
        if (!beanExists(executionContext)) {
            LOGGER.debug("Cannot bind data onto bean '" + beanId + "' as bean does not exist in BeanContext.");
            return;
        }

        if (isBeanWiring) {
            bindBeanValue(executionContext, new Fragment(element));
        } else if (isAttribute) {
            // Bind attribute (i.e. selectors with '@' prefix) values on the visitBefore...
            bindSaxDataValue(element, executionContext);
        } else if (expressionEvaluator == null || expressionHasDataVariable) {
            // It's not a wiring, attribute or expression binding => it's the element's text.
            // Turn on Text Accumulation...
            element.accumulateText();
        }
    }

    public void visitAfter(SAXElement element, ExecutionContext executionContext) throws SmooksException, IOException {
        if (!beanExists(executionContext)) {
            LOGGER.debug("Cannot bind data onto bean '" + beanId + "' as bean does not exist in BeanContext.");
            return;
        }

        if (!isBeanWiring && !isAttribute) {
            bindSaxDataValue(element, executionContext);
        }
    }

    private boolean beanExists(ExecutionContext executionContext) {
        return (executionContext.getBeanContext().getBean(beanId) != null);
    }

    private void bindDomDataValue(Element element, ExecutionContext executionContext) {
        String dataString;

        if (isAttribute) {
            if (valueAttributeNS != null) {
                dataString = DomUtils.getAttributeValue(element, valueAttributeName.orElse(null), valueAttributeNS);
            } else {
                dataString = DomUtils.getAttributeValue(element, valueAttributeName.orElse(null));
            }
        } else {
            dataString = DomUtils.getAllText(element, false);
        }

        String propertyName;
        if (mapKeyAttribute != null) {
            propertyName = DomUtils.getAttributeValue(element, mapKeyAttribute);
            if (propertyName == null) {
                propertyName = DomUtils.getName(element);
            }
        } else {
            propertyName = property.orElseGet(() -> DomUtils.getName(element));
        }

        if (expressionEvaluator != null) {
            bindExpressionValue(propertyName, dataString, executionContext, new Fragment(element));
        } else {
            decodeAndSetPropertyValue(propertyName, dataString, executionContext, new Fragment(element));
        }
    }

    private void bindSaxDataValue(SAXElement element, ExecutionContext executionContext) {
        String propertyName;

        if (mapKeyAttribute != null) {
            propertyName = SAXUtil.getAttribute(mapKeyAttribute, element.getAttributes(), null);
            if (propertyName == null) {
                propertyName = element.getName().getLocalPart();
            }
        } else {
            propertyName = property.orElseGet(() -> element.getName().getLocalPart());
        }

        String dataString = null;
        if (expressionEvaluator == null || expressionHasDataVariable) {
            if (isAttribute) {
                if (valueAttributeNS != null) {
                    dataString = SAXUtil.getAttribute(valueAttributeNS, valueAttributeName.orElse(null), element.getAttributes(), null);
                } else {
                    dataString = SAXUtil.getAttribute(valueAttributeName.orElse(null), element.getAttributes(), null);
                }
            } else {
                dataString = element.getTextContent();
            }
        }

        if (expressionEvaluator != null) {
            bindExpressionValue(propertyName, dataString, executionContext, new Fragment(element));
        } else {
            decodeAndSetPropertyValue(propertyName, dataString, executionContext, new Fragment(element));
        }
    }

    private void bindBeanValue(final ExecutionContext executionContext, Fragment source) {
        final BeanContext beanContext = executionContext.getBeanContext();
        Object bean = null;

        if (wireBeanId != null) {
            bean = beanContext.getBean(wireBeanId);
        }

        if (bean != null) {
            if (!BeanWiringObserver.isMatchingBean(bean, wireBeanType.orElse(null), wireBeanAnnotation.orElse(null))) {
                bean = null;
            }
        }

        if (bean == null) {

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Registering bean ADD wiring observer for wiring bean '" + wireBeanId + "' onto target bean '" + beanId.getName() + "'.");
            }

            // Register the observer which looks for the creation of the selected bean via its beanIdName...
            beanContext.addObserver(wireByBeanIdObserver);
        } else {
            populateAndSetPropertyValue(bean, beanContext, wireBeanId, executionContext, source);
        }
    }

    public void populateAndSetPropertyValue(Object bean, BeanContext beanContext, BeanId targetBeanId, final ExecutionContext executionContext, Fragment source) {
        BeanRuntimeInfo wiredBeanRI = getWiredBeanRuntimeInfo();

        // When this observer is triggered then we look if we got something we can set immediately or that we got an array collection.
        // For an array collection, we need the array representation and not the list representation, so we register and observer that
        // listens for the change from the list to the array...
        if (wiredBeanRI != null && wiredBeanRI.getClassification() == BeanRuntimeInfo.Classification.ARRAY_COLLECTION) {

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Registering bean CHANGE wiring observer for wiring bean '" + targetBeanId + "' onto target bean '" + beanId.getName() + "' after it has been converted from a List to an array.");
            }
            // Register an observer which looks for the change that the mutable list of the selected bean gets converted to an array. We
            // can then set this array
            beanContext.addObserver(listToArrayChangeObserver);
        } else {
            setPropertyValue(property.orElse(null), bean, executionContext, source);
        }
    }

    private void bindExpressionValue(String mapPropertyName, String dataString, ExecutionContext executionContext, Fragment source) {
        Map<String, Object> beanMap = executionContext.getBeanContext().getBeanMap();

        Map<String, Object> variables = new HashMap<String, Object>();
        if (expressionHasDataVariable) {
            variables.put(EXPRESSION_VALUE_VARIABLE_NAME, dataString);
        }

        Object dataObject = expressionEvaluator.exec(beanMap, variables);
        decodeAndSetPropertyValue(mapPropertyName, dataObject, executionContext, source);
    }

    private void decodeAndSetPropertyValue(String mapPropertyName, Object dataObject, ExecutionContext executionContext, Fragment source) {
        if (dataObject instanceof String) {
            setPropertyValue(mapPropertyName, decodeDataString((String) dataObject, executionContext), executionContext, source);
        } else {
            setPropertyValue(mapPropertyName, dataObject, executionContext, source);
        }

    }

    @SuppressWarnings("unchecked")
    public void setPropertyValue(String mapPropertyName, Object dataObject, ExecutionContext executionContext, Fragment source) {
        if (dataObject == null) {
            return;
        }

        Object bean = executionContext.getBeanContext().getBean(beanId);

        BeanRuntimeInfo.Classification beanType = beanRuntimeInfo.getClassification();

        createPropertySetterMethod(bean, dataObject.getClass());

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Setting data object '" + wireBeanIdName.orElse(null) + "' (" + dataObject.getClass().getName() + ") on target bean '" + beanId + "'.");
        }

        // Set the data on the bean...
        try {
            if (propertySetterMethod != null) {
                propertySetterMethod.invoke(bean, dataObject);
            } else if (beanType == BeanRuntimeInfo.Classification.MAP_COLLECTION) {
                ((Map) bean).put(mapPropertyName, dataObject);
            } else if (beanType == BeanRuntimeInfo.Classification.ARRAY_COLLECTION || beanType == BeanRuntimeInfo.Classification.COLLECTION_COLLECTION) {
                ((Collection) bean).add(dataObject);
            } else {
                if (setterMethod.isPresent()) {
                    throw new SmooksConfigurationException("Bean [" + beanIdName + "] configuration invalid.  Bean setter method [" + setterMethod.get() + "(" + dataObject.getClass().getName() + ")] not found on type [" + beanRuntimeInfo.getPopulateType().getName() + "].  You may need to set a 'decoder' on the binding config.");
                } else if (property.isPresent()) {
                    boolean throwException = true;

                    if (beanRuntimeInfo.isJAXBType() && getWiredBeanRuntimeInfo().getClassification() != BeanRuntimeInfo.Classification.NON_COLLECTION) {
                        // It's a JAXB collection type.  If the wired in bean is created by a factory then it's most
                        // probable that there's no need to set the collection because the JAXB type is creating it lazily
                        // in the getter method.  So... we're going to ignore this.
                        if (wireBeanId.getCreateResourceConfiguration().getParameter("beanFactory", String.class) != null) {
                            throwException = false;
                        }
                    }

                    if (throwException) {
                        throw new SmooksConfigurationException("Bean [" + beanIdName + "] configuration invalid.  Bean setter method [" + ClassUtil.toSetterName(property.get()) + "(" + dataObject.getClass().getName() + ")] not found on type [" + beanRuntimeInfo.getPopulateType().getName() + "].  You may need to set a 'decoder' on the binding config.");
                    }
                }
            }

            if (notifyPopulate) {
                BeanContextLifecycleEvent event = new BeanContextLifecycleEvent(executionContext, source, BeanLifecycle.POPULATE, beanId, bean);
                executionContext.getBeanContext().notifyObservers(event);
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new SmooksConfigurationException("Error invoking bean setter method [" + ClassUtil.toSetterName(property.orElse(null)) + "] on bean instance class type [" + bean.getClass() + "].", e);
        }
    }

    private void createPropertySetterMethod(Object bean, Class<?> parameter) {

        if (!checkedForSetterMethod && propertySetterMethod == null) {
            String methodName = null;
            if (setterMethod.isPresent() && !setterMethod.get().trim().equals("")) {
                methodName = setterMethod.get();
            } else if (property.isPresent() && !property.get().trim().equals("")) {
                methodName = ClassUtil.toSetterName(property.get());
            }

            if (methodName != null) {
                propertySetterMethod = createPropertySetterMethod(bean, methodName, parameter);
            }

            checkedForSetterMethod = true;
        }
    }

    /**
     * Create the bean setter method instance for this visitor.
     *
     * @param setterName The setter method name.
     * @return The bean setter method.
     */
    private synchronized Method createPropertySetterMethod(Object bean, String setterName, Class<?> setterParamType) {
        if (propertySetterMethod == null) {
            propertySetterMethod = BeanUtils.createSetterMethod(setterName, bean, setterParamType);
        }

        return propertySetterMethod;
    }

    private Object decodeDataString(String dataString, ExecutionContext executionContext) throws TypeConverterException {
        if ((dataString == null || dataString.length() == 0) && defaultVal.isPresent()) {
            if (defaultVal.get().equals("null")) {
                return null;
            }
            dataString = defaultVal.get();
        }

        if (typeConverter == null) {
            typeConverter = getTypeConverter(executionContext);
        }

        try {
            return typeConverter.convert(dataString);
        } catch (TypeConverterException e) {
            throw new TypeConverterException("Failed to decode binding value '" + dataString + "' for property '" + property + "' on bean '" + beanId.getName() + "'.", e);
        }
    }

    private TypeConverter<? super String, ?> getTypeConverter(ExecutionContext executionContext) throws TypeConverterException {
        return getTypeConverter(executionContext.getDeliveryConfig());
    }

    public TypeConverter<? super String, ?> getTypeConverter(ContentDeliveryConfig deliveryConfig) {
        @SuppressWarnings("unchecked")
        List<?> typeConverters = deliveryConfig.getObjects("decoder:" + typeAlias.orElse(null));

        if (typeConverters == null || typeConverters.isEmpty()) {
            if (typeAlias.isPresent()) {
                typeConverter = appContext.getRegistry().lookup(new NameTypeConverterFactoryLookup<>(typeAlias.get())).createTypeConverter();
            } else {
                typeConverter = resolveDecoderReflectively();
            }
        } else if (!(typeConverters.get(0) instanceof TypeConverter)) {
            throw new TypeConverterException("Configured type converter '" + typeAlias.orElse(null) + ":" + typeConverters.get(0).getClass().getName() + "' is not an instance of " + TypeConverter.class.getName());
        } else {
            typeConverter = (TypeConverter<String, ?>) typeConverters.get(0);
        }

        if (typeConverter instanceof PreprocessTypeConverter) {
            PreprocessTypeConverter preprocessTypeConverter = (PreprocessTypeConverter) typeConverter;
            if (preprocessTypeConverter.getDelegateTypeConverter() == null) {
                preprocessTypeConverter.setDelegateTypeConverter(resolveDecoderReflectively());
            }
        }

        return typeConverter;
    }

    private TypeConverter<? super String, ?> resolveDecoderReflectively() throws TypeConverterException {
        Class<?> bindType = resolveBindTypeReflectively();

        if (bindType != null) {
            if (bindType.isEnum()) {
                return value -> Enum.valueOf((Class) bindType, value);
            } else {
                final TypeConverterFactory<? super String, ?> typeConverterFactory = appContext.getRegistry().lookup(new SourceTargetTypeConverterFactoryLookup<>(String.class, bindType));

                if (typeConverterFactory != null) {
                    return typeConverterFactory.createTypeConverter();
                }
            }
        }

        return new StringConverterFactory().createTypeConverter();
    }

    private Class<?> resolveBindTypeReflectively() throws TypeConverterException {
        String bindingMember = (setterMethod.orElseGet(() -> property.orElse(null)));

        if (bindingMember != null && beanRuntimeInfo.getClassification() == BeanRuntimeInfo.Classification.NON_COLLECTION) {
            final Method bindingMethod = Bean.getBindingMethod(bindingMember, beanRuntimeInfo.getPopulateType());
            if (bindingMethod != null) {
                return bindingMethod.getParameterTypes()[0];
            }
        }

        return null;
    }

    private BeanRuntimeInfo getWiredBeanRuntimeInfo() {
        if (wiredBeanRuntimeInfo == null) {
            // Don't need to synchronize this.  Worse thing that can happen is we initialize it
            // more than once... no biggie...
            wiredBeanRuntimeInfo = BeanRuntimeInfo.getBeanRuntimeInfo(wireBeanIdName.orElse(null), appContext);
        }
        return wiredBeanRuntimeInfo;
    }

    private String getId() {
        return id;
    }

    public Set<?> getProducts() {
        return CollectionsUtil.toSet(beanIdName + "." + property.orElse(null), "]." + property.orElse(null));
    }

    public boolean consumes(Object object) {
        if (object.equals(beanIdName)) {
            return true;
        } else if (object.equals(wireBeanIdName.orElse(null))) {
            return true;
        } else {
            return expressionEvaluator != null && expressionEvaluator.getExpression().contains(object.toString());
        }
    }
}
