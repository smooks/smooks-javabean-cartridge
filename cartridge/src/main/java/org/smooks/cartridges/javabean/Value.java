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

import org.smooks.assertion.AssertArgument;
import org.smooks.cartridges.javabean.ext.SelectorPropertyResolver;
import org.smooks.cdr.SmooksResourceConfiguration;
import org.smooks.cdr.registry.lookup.converter.SourceTargetTypeConverterFactoryLookup;
import org.smooks.converter.TypeConverter;
import org.smooks.converter.TypeConverterFactoryLoader;
import org.smooks.converter.factory.TypeConverterFactory;
import org.smooks.delivery.VisitorConfigMap;

import java.util.Set;

/**
 * Programmatic Value Configurator.
 * <p/>
 * This class can be used to programmatically configure a Smooks instance for creating value
 * objects using the Smooks DataDecoders.
 * <p/>
 * This class uses a Fluent API (all methods return the Bean instance), making it easy to
 * string configurations together.
 * <p/>
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
 *         &lt;!-- .... --!&gt;
 *     &lt;/order-items&gt;
 * &lt;/order&gt;
 * </pre>
 * <p/>
 * <h4>The Binding Configuration and Execution Code</h4>
 * The configuration code (Note: Smooks instance defined and instantiated globally):
 * <pre>
 * Smooks smooks = new Smooks();
 *
 * Value customerNumberValue = new Value( "customerNumber", "customer/@number")
 *                                   .setDecoder("Integer");
 * Value customerNameValue = new Value( "customerName", "customer")
 *                                   .setDefault("Unknown");
 *
 * smooks.addVisitors(customerNumberValue);
 * smooks.addVisitors(customerNameValue);
 * </pre>
 * <p/>
 * And the execution code:
 * <pre>
 * JavaResult result = new JavaResult();
 *
 * smooks.filterSource(new StreamSource(orderMessageStream), result);
 * Integer customerNumber = (Integer) result.getBean("customerNumber");
 * String customerName = (String) result.getBean("customerName");
 * </pre>
 *
 * @author <a href="mailto:maurice.zeijen@smies.com">maurice.zeijen@smies.com</a>
 * @see Bean
 */
public class Value extends BindingAppender {

	private static final Set<TypeConverterFactory<?, ?>> TYPE_CONVERTER_FACTORIES = new TypeConverterFactoryLoader().load();
	
	private String dataSelector;

	private String targetNamespace;

	private String defaultValue;

	private TypeConverter<? super String, ?> typeConverter;

	/**
     * Create a Value binding configuration.
     *
	 * @param beanId The bean id under which the value will be stored.
	 * @param data The data selector for the data value to be bound.
	 */
	public Value(String beanId, String data) {
		super(beanId);
		AssertArgument.isNotNullAndNotEmpty(beanId, "beanId");
		AssertArgument.isNotNullAndNotEmpty(data, "dataSelector");

		this.dataSelector = data;
	}

	/**
     * Create a Value binding configuration.
     *
	 * @param beanId The bean id under which the value will be stored.
	 * @param data The data selector for the data value to be bound.
	 * @param type Data type.
	 */
	public Value(String beanId, String data, Class<?> type) {
		this(beanId, data);
		AssertArgument.isNotNull(type, "type");

		
		this.typeConverter = new SourceTargetTypeConverterFactoryLookup<>(String.class, type).lookup(TYPE_CONVERTER_FACTORIES).createTypeConverter();
	}

	/**
	 * The namespace for the data selector for the data value to be bound.
	 *
	 * @param targetNamespace The namespace
	 * @return <code>this</code> Value configuration instance.
	 */
	public Value setTargetNamespace(String targetNamespace) {
		this.targetNamespace = targetNamespace;

		return this;
	}

	/**
	 * The default value for if the data is null or empty
	 *
	 * @param targetNamespace The default value
	 * @return <code>this</code> Value configuration instance.
	 */
	public Value setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;

		return this;
	}

	/**
	 * Set the binding value data type.
	 * @param type The data type.
	 * 
	 * @return <code>this</code> Value configuration instance.
	 */
	public Value setType(Class<?> type) {
		this.typeConverter = new SourceTargetTypeConverterFactoryLookup<>(String.class, type).lookup(TYPE_CONVERTER_FACTORIES).createTypeConverter();

		return this;
	}

	/**
	 * The {@link org.smooks.cartridges.javabean.DataDecoder} to be used for decoding
     * the data value.
	 *
	 * @param targetNamespace The {@link org.smooks.cartridges.javabean.DataDecoder}
	 * @return <code>this</code> Value configuration instance.
	 */
	public Value setTypeConverter(TypeConverter<String, ?> typeConverter) {
		this.typeConverter = typeConverter;

		return this;
	}

	/**
	 * Used by Smooks to retrieve the visitor configuration of this Value Configuration
	 */
	public void addVisitors(VisitorConfigMap visitorMap) {

		ValueBinder binder = new ValueBinder(getBeanId());
		SmooksResourceConfiguration populatorConfig = new SmooksResourceConfiguration(dataSelector);

		SelectorPropertyResolver.resolveSelectorTokens(populatorConfig);

		binder.setTypeConverter(typeConverter);
		binder.setDefaultValue(defaultValue);
		binder.setValueAttributeName(populatorConfig.getParameterValue(BeanInstancePopulator.VALUE_ATTRIBUTE_NAME, String.class));

		visitorMap.addVisitor(binder, populatorConfig.getSelector(), targetNamespace, true);
	}

}
