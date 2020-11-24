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
package org.smooks.cartridges.javabean.ext;

import org.smooks.SmooksException;
import org.smooks.cartridges.javabean.BeanInstancePopulator;
import org.smooks.cdr.ResourceConfig;
import org.smooks.cdr.extension.ExtensionContext;
import org.smooks.container.ExecutionContext;
import org.smooks.delivery.dom.DOMVisitBefore;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;

/**
 * Selector Property Resolver.
 * <p/>
 * Some binding selectors can be of the form "order/customer/@customerNumber", where the
 * last token in the selector represents an attribute on the customer element (for example).  This
 * extension visitor translates this type of selector into "order/customer" plus a new property
 * on the BeanInstancePopulator config named "valueAttributeName" containing a value of
 * "customerNumber".
 *
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class SelectorPropertyResolver implements DOMVisitBefore {

    public void visitBefore(Element element, ExecutionContext executionContext) throws SmooksException {
        ExtensionContext extensionContext = ExtensionContext.getExtensionContext(executionContext);
        ResourceConfig populatorConfig = extensionContext.getResourceStack().peek();
        resolveSelectorTokens(populatorConfig);
    }

    public static void resolveSelectorTokens(ResourceConfig populatorConfig) {
        QName valueAttributeQName = populatorConfig.getSelectorPath().isEmpty() ? null : populatorConfig.getSelectorPath().getTargetSelectorStep().getAttribute();
        
        if(valueAttributeQName != null) {
	        String valueAttributeName = valueAttributeQName.getLocalPart();
	        String valueAttributePrefix = valueAttributeQName.getPrefix();
	
	        populatorConfig.setParameter(BeanInstancePopulator.VALUE_ATTRIBUTE_NAME, valueAttributeName);
	        if(valueAttributePrefix != null && !valueAttributePrefix.trim().equals("")) {
	        	populatorConfig.setParameter(BeanInstancePopulator.VALUE_ATTRIBUTE_PREFIX, valueAttributePrefix);
	        }
        }
    }
}
