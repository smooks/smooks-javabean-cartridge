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
import org.smooks.api.bean.repository.BeanId;
import org.smooks.api.converter.TypeConverter;
import org.smooks.api.converter.TypeConverterException;
import org.smooks.api.delivery.fragment.Fragment;
import org.smooks.api.delivery.ordering.Producer;
import org.smooks.api.resource.visitor.VisitAfterReport;
import org.smooks.api.resource.visitor.VisitBeforeReport;
import org.smooks.api.resource.visitor.sax.ng.AfterVisitor;
import org.smooks.api.resource.visitor.sax.ng.BeforeVisitor;
import org.smooks.api.resource.visitor.sax.ng.ChildrenVisitor;
import org.smooks.engine.delivery.fragment.NodeFragment;
import org.smooks.engine.lookup.converter.NameTypeConverterFactoryLookup;
import org.smooks.engine.memento.TextAccumulatorMemento;
import org.smooks.engine.memento.TextAccumulatorVisitorMemento;
import org.smooks.support.CollectionsUtil;
import org.smooks.support.DomUtils;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Element;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Value Binder.
 * <p/>
 * This class can be used to configure a Smooks instance for creating value
 * objects using the Smooks DataDecoders.
 * <h3>XML Schema & Namespace</h3>
 * The Value Binder XML configuration schema is in the following XML Schema Namespace:
 * <p/>
 * <a href="https://www.smooks.org/xsd/smooks/javabean-1.6.xsd"><b>https://www.smooks.org/xsd/smooks/javabean-1.6.xsd</b></a>
 * <p/>
 * The value binder element is '&lt;value&gt;'. Take a look in the schema for all
 * the configuration attributes.
 *
 * <h3>Programmatic configuration</h3>
 * The value binder can be programmatic configured using the {@link Value} Object.
 *
 * <h3>Example</h3>
 * Taking the "classic" Order message as an example and getting the order number and
 * name as Value Objects in the form of an Integer and String.
 * <h4>The Message</h4>
 * <pre>
 * &lt;order xmlns="http://x"&gt;
 *     &lt;header&gt;
 *         &lt;y:date xmlns:y="http://y"&gt;Wed Nov 15 13:45:28 EST 2006&lt;/y:date&gt;
 *         &lt;customer number="123123"&gt;Joe&lt;/customer&gt;
 *         &lt;privatePerson&gt;&lt;/privatePerson&gt;
 *     &lt;/header&gt;
 *     &lt;order-items&gt;
 *         &lt;!-- .... --&gt;
 *     &lt;/order-items&gt;
 * &lt;/order&gt;
 * </pre>
 *
 * <h4>The Binding Configuration</h4>
 * <pre>
 * &lt;?xml version=&quot;1.0&quot;?&gt;
 * &lt;smooks-resource-list xmlns=&quot;https://www.smooks.org/xsd/smooks-1.2.xsd&quot; xmlns:jb=&quot;https://www.smooks.org/xsd/smooks/javabean-1.6.xsd&quot;&gt;
 *
 *    &lt;jb:value
 *       beanId=&quot;customerName&quot;
 *       data=&quot;customer&quot;
 *       default=&quot;unknown&quot;
 *    /&gt;
 *
 *    &lt;jb:value
 *       beanId=&quot;customerNumber&quot;
 *       data=&quot;customer/@number&quot;
 *	     decoder=&quot;Integer&quot;
 *    /&gt;
 *
 * &lt;/smooks-resource-list&gt;
 * </pre>
 *
 * @author <a href="mailto:maurice.zeijen@smies.com">maurice.zeijen@smies.com</a>
 */
@VisitBeforeReport(condition = "parameters.containsKey('valueAttributeName')",
        summary = "Creating object under bean id <b>${resource.parameters.beanId}</b> with a value from the attribute <b>${resource.parameters.valueAttributeName}</b>.",
        detailTemplate = "reporting/ValueBinderReport_Before.html")
@VisitAfterReport(condition = "!parameters.containsKey('valueAttributeName')",
        summary = "Creating object <b>${resource.parameters.beanId}</b> with a value from this element.",
        detailTemplate = "reporting/ValueBinderReport_After.html")
public class ValueBinder implements BeforeVisitor, AfterVisitor, ChildrenVisitor, Producer {

	private static final Logger LOGGER = LoggerFactory.getLogger(ValueBinder.class);

    @Inject
	@Named("beanId")
    private String beanIdName;

    @Inject
    private Optional<String> valueAttributeName;

    @Inject
	@Named("default")
    private Optional<String> defaultValue;

    @Inject
	@Named("type")
    private String typeAlias = "String";

    private BeanId beanId;

    @Inject
    private ApplicationContext appContext;

    private boolean isAttribute;

    private TypeConverter<? super String, ?> typeConverter;

    /**
     *
     */
    public ValueBinder() {
	}

    /**
     * @param beanId
     */
	public ValueBinder(String beanId) {
		this.beanIdName = beanId;
	}

	/**
	 * @return the beanIdName
	 */
	public String getBeanIdName() {
		return beanIdName;
	}

	/**
	 * @param beanIdName the beanIdName to set
	 */
	public void setBeanIdName(String beanIdName) {
		this.beanIdName = beanIdName;
	}

	/**
	 * @return the valueAttributeName
	 */
	public String getValueAttributeName() {
		return valueAttributeName.orElse(null);
	}

	/**
	 * @param valueAttributeName the valueAttributeName to set
	 */
	public void setValueAttributeName(String valueAttributeName) {
		this.valueAttributeName = Optional.ofNullable(valueAttributeName);
	}

	/**
	 * @return the defaultValue
	 */
	public String getDefaultValue() {
		return defaultValue.orElse(null);
	}

	/**
	 * @param defaultValue the defaultValue to set
	 */
	public void setDefaultValue(String defaultValue) {
		this.defaultValue = Optional.ofNullable(defaultValue);
	}

	/**
	 * @return the typeAlias
	 */
	public String getTypeAlias() {
		return typeAlias;
	}

	/**
	 * @param typeAlias the typeAlias to set
	 */
	public void setTypeAlias(String typeAlias) {
		this.typeAlias = typeAlias;
	}

	/**
	 * @return the decoder
	 */
	public TypeConverter<? super String, ?> getTypeConverter() {
		return typeConverter;
	}

	/**
	 * @param typeConverter the decoder to set
	 */
	public void setTypeConverter(TypeConverter<? super String, ?> typeConverter) {
		this.typeConverter = typeConverter;
	}

	/**
     * Set the resource configuration on the bean populator.
     * @throws SmooksConfigException Incorrectly configured resource.
     */
    @PostConstruct
    public void postConstruct() throws SmooksConfigException {
    	isAttribute = (valueAttributeName.isPresent());

        beanId = appContext.getBeanIdStore().register(beanIdName);

        if(LOGGER.isDebugEnabled()) {
        	LOGGER.debug("Value Binder created for [" + beanIdName + "].");
        }
    }

	@Override
	public void visitBefore(Element element, ExecutionContext executionContext) throws SmooksException {
		if (isAttribute) {
			bindValue(DomUtils.getAttributeValue(element, valueAttributeName.orElse(null)), executionContext, new NodeFragment(element));
		}
	}

	@Override
	public void visitAfter(Element element, ExecutionContext executionContext) throws SmooksException {
		if(!isAttribute) {
			NodeFragment nodeFragment = new NodeFragment(element);
			TextAccumulatorMemento textAccumulatorMemento = new TextAccumulatorVisitorMemento(nodeFragment, this);
			executionContext.getMementoCaretaker().restore(textAccumulatorMemento);
			bindValue(textAccumulatorMemento.getText(), executionContext, nodeFragment);
		}
	}

	private void bindValue(String dataString, ExecutionContext executionContext, Fragment<?> source) {
		Object valueObj = decodeDataString(dataString, executionContext);

		BeanContext beanContext = executionContext.getBeanContext();

		if(valueObj == null) {
			beanContext.removeBean(beanId, source);
		} else {
			beanContext.addBean(beanId, valueObj, source);
		}
	}

	public Set<?> getProducts() {
		return CollectionsUtil.toSet(beanIdName);
	}

	private Object decodeDataString(String dataString, ExecutionContext executionContext) throws TypeConverterException {
		if ((dataString == null || dataString.length() == 0) && defaultValue.isPresent()) {
			if (defaultValue.get().equals("null")) {
				return null;
			}
			dataString = defaultValue.get();
		}

		try {
			return getTypeConverter(executionContext).convert(dataString);
		} catch (TypeConverterException e) {
			throw new TypeConverterException("Failed to convert the value '" + dataString + "' for the bean id '" + beanIdName + "'.", e);
		}
    }

	private TypeConverter<? super String, ?> getTypeConverter(ExecutionContext executionContext) throws TypeConverterException {
		if(typeConverter == null) {
			@SuppressWarnings("unchecked")
			List decoders = executionContext.getContentDeliveryRuntime().getContentDeliveryConfig().getObjects("decoder:" + typeAlias);

	        if (decoders == null || decoders.isEmpty()) {
	            typeConverter = appContext.getRegistry().lookup(new NameTypeConverterFactoryLookup<>(typeAlias)).createTypeConverter();
	        } else if (!(decoders.get(0) instanceof TypeConverter)) {
	            throw new TypeConverterException("Configured type converter '" + typeAlias + ":" + decoders.get(0).getClass().getName() + "' is not an instance of " + TypeConverter.class.getName());
	        } else {
	            typeConverter = (TypeConverter<? super String, ?>) decoders.get(0);
	        }
		}
        return typeConverter;
    }

	@Override
	public void visitChildText(CharacterData characterData, ExecutionContext executionContext) throws SmooksException {
		if (!isAttribute) {
			TextAccumulatorMemento textAccumulatorMemento = new TextAccumulatorVisitorMemento(new NodeFragment(characterData.getParentNode()), this);
			textAccumulatorMemento.accumulateText(characterData.getTextContent());
			executionContext.getMementoCaretaker().capture(textAccumulatorMemento);
		}
	}

	@Override
	public void visitChildElement(Element childElement, ExecutionContext executionContext) throws SmooksException {

	}
}
