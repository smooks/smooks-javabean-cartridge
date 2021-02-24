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

import org.smooks.api.ExecutionContext;
import org.smooks.api.SmooksConfigException;
import org.smooks.api.SmooksException;
import org.smooks.api.resource.visitor.dom.DOMVisitBefore;
import org.smooks.support.ClassUtil;
import org.smooks.support.DomUtils;
import org.w3c.dom.Element;

import java.util.Collection;
import java.util.Map;

/**
 * Binding "property" attribute checker.
 * <p/>
 * The binding "property" attribute should not be specified when binding to a Collection/Array, and must be
 * specified when binding to a non-collection.  This visitor enforces these constraints.
 *
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class PropertyChecker implements DOMVisitBefore {

    private enum BeanType {
        ARRAY,
        COLLECTION,
        MAP,
        OTHER
    }

    public void visitBefore(Element element, ExecutionContext execContext) throws SmooksException {
        BeanType beanType = getBeanType(element);
        boolean isPropertSpecified = (DomUtils.getAttributeValue(element, "property") != null);
        boolean isSetterMethodSpecified = (DomUtils.getAttributeValue(element, "setterMethod") != null);
        String bindingType = DomUtils.getName(element);

        if(isPropertSpecified && isSetterMethodSpecified) {
        	throw new SmooksConfigException("'" + bindingType + "' binding specifies a 'property' and a 'setterMethod' attribute.  Only one of both may be set.");
        }
        if(isPropertSpecified && beanType == BeanType.COLLECTION) {
            throw new SmooksConfigException("'" + bindingType + "' binding specifies a 'property' attribute.  This is not valid for a Collection target.");
        }
        if(isPropertSpecified && beanType == BeanType.ARRAY) {
            throw new SmooksConfigException("'" + bindingType + "' binding specifies a 'property' attribute.  This is not valid for an Array target.");
        }
        if(isSetterMethodSpecified && beanType == BeanType.COLLECTION) {
            throw new SmooksConfigException("'" + bindingType + "' binding specifies a 'setterMethod' attribute.  This is not valid for a Collection target.");
        }
        if(isSetterMethodSpecified && beanType == BeanType.ARRAY) {
            throw new SmooksConfigException("'" + bindingType + "' binding specifies a 'setterMethod' attribute.  This is not valid for an Array target.");
        }
        if(!isPropertSpecified && !isSetterMethodSpecified && beanType == BeanType.OTHER) {
            throw new SmooksConfigException("'" + bindingType + "' binding for bean class '" + getBeanTypeName(element) + "' must specify a 'property' or 'setterMethod' attribute.");
        }
    }

    private BeanType getBeanType(Element bindingElement) {
        String beanClassName = getBeanTypeName(bindingElement);

        if(beanClassName.endsWith("[]")) {
            return BeanType.ARRAY;
        } else {
            Class<?> beanClass = getBeanClass(bindingElement);

            if (Collection.class.isAssignableFrom(beanClass)) {
                return BeanType.COLLECTION;
            } else if (Map.class.isAssignableFrom(beanClass)) {
                return BeanType.MAP;
            } else {
                return BeanType.OTHER;
            }
        }
    }

    private Class<?> getBeanClass(Element bindingElement) {
        String beanClassName = getBeanTypeName(bindingElement);

        Class<?> beanClass;
        try {
            beanClass = ClassUtil.forName(beanClassName, getClass());
        } catch (ClassNotFoundException e) {
            throw new SmooksConfigException("Bean class '" + beanClassName + "' not available on classpath.");
        }
        return beanClass;
    }

    private String getBeanTypeName(Element bindingElement) {
        return ((Element)bindingElement.getParentNode()).getAttribute("class");
    }
}
