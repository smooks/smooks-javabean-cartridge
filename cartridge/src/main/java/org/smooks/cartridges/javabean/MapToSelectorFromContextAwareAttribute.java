/*-
 * ========================LICENSE_START=================================
 * smooks-javabean-cartridge
 * %%
 * Copyright (C) 2020 - 2021 Smooks
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
import org.smooks.api.resource.config.ResourceConfig;
import org.smooks.api.resource.visitor.dom.DOMVisitBefore;
import org.smooks.engine.resource.config.loader.xml.extension.ExtensionContext;
import org.smooks.support.DollarBraceDecoder;
import org.smooks.support.DomUtils;
import org.w3c.dom.Element;

import javax.inject.Inject;
import java.util.*;

public class MapToSelectorFromContextAwareAttribute implements DOMVisitBefore {

    private static final Logger LOGGER = LoggerFactory.getLogger(MapToSelectorFromContextAwareAttribute.class);
    private static final Map<String, String> BASE_BEANS = new HashMap<>();

    @Inject
    private String attribute;

    @Inject
    private ApplicationContext applicationContext;

    @Override
    public void visitBefore(Element element, ExecutionContext executionContext) throws SmooksException {
        ResourceConfig resourceConfig;
        String value = DomUtils.getAttributeValue(element, attribute);

        try {
            resourceConfig = executionContext.get(ExtensionContext.EXTENSION_CONTEXT_TYPED_KEY).getResourceStack().peek();
        } catch (EmptyStackException e) {
            throw new SmooksException("No ResourceConfig available in ExtensionContext stack. Unable to set ResourceConfig property 'selector' with attribute '" + attribute + "' value '" + value + "'.");
        }

        if (value == null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Not setting property 'selector' on resource configuration. Attribute '{}' value on element '{}' is null.  You may need to set a default value in the binding configuration.", attribute, DomUtils.getName(element));
            }
            return;
        } else {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Setting property 'selector' on resource configuration to a value of '{}'.", value);
            }
        }

        String foundBeanId = DomUtils.getAttributeValue(element, "beanId");
        if (foundBeanId != null) {
            BASE_BEANS.put(foundBeanId, DomUtils.getAttributeValue(element, "createOnElement"));
        }

        String absoluteSelector;
        if (value.startsWith("#") && element.getParentNode() != null && element.getParentNode().getAttributes().getNamedItem("createOnElement") != null) {
            String contextSelector = element.getParentNode().getAttributes().getNamedItem("createOnElement").getNodeValue();
            absoluteSelector = contextSelector + value.substring(1);
        } else {
            absoluteSelector = value;
        }

        resourceConfig.setSelector(resolveBeandIdSelector(absoluteSelector), new Properties());
    }

    protected String resolveBeandIdSelector(String selector) {
        List<String> dollarBraceTokens = DollarBraceDecoder.getTokens(selector.substring(0, selector.contains("/") ? selector.indexOf("/") : selector.length()));
        if (dollarBraceTokens.size() == 1) {
            String beanId = dollarBraceTokens.get(0);
            String beanSelector = BASE_BEANS.get(beanId);
            if (beanSelector != null) {
                return resolveBeandIdSelector(beanSelector) + selector.substring(selector.indexOf("/"));
            } else {
                throw new SmooksConfigException("Invalid selector '" + selector + "'.  Unknown beanId '" + beanId + "'.");
            }
        } else {
            return selector;
        }
    }

}