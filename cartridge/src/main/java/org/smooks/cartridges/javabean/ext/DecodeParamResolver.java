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

import org.smooks.api.ApplicationContext;
import org.smooks.api.ExecutionContext;
import org.smooks.api.SmooksException;
import org.smooks.api.converter.TypeConverterFactory;
import org.smooks.api.resource.config.ResourceConfig;
import org.smooks.api.resource.visitor.dom.DOMVisitBefore;
import org.smooks.engine.converter.PreprocessTypeConverter;
import org.smooks.engine.lookup.converter.NameTypeConverterFactoryLookup;
import org.smooks.engine.resource.config.DefaultResourceConfig;
import org.smooks.engine.resource.extension.ExtensionContext;
import org.smooks.support.DomUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.inject.Inject;
import java.util.UUID;

/**
 * Type decoder parameter mapping visitor.
 *
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class DecodeParamResolver implements DOMVisitBefore {

    @Inject
    private ApplicationContext applicationContext;

    public void visitBefore(Element element, ExecutionContext executionContext) throws SmooksException {
        NodeList decodeParams = element.getElementsByTagNameNS(element.getNamespaceURI(), "decodeParam");

        if (decodeParams.getLength() > 0) {
            ExtensionContext extensionContext = executionContext.get(ExtensionContext.EXTENSION_CONTEXT_TYPED_KEY);
            ResourceConfig populatorConfig = extensionContext.getResourceStack().peek();
            ResourceConfig typeConverterConfig = new DefaultResourceConfig();

            extensionContext.addResource(typeConverterConfig);
            try {
                String type = populatorConfig.getParameterValue("type", String.class);
                TypeConverterFactory<?, ?> typeConverterFactory = applicationContext.getRegistry().lookup(new NameTypeConverterFactoryLookup<>(type));
                String reType = UUID.randomUUID().toString();

                // Need to retype the populator configuration so as to get the
                // value binding BeanInstancePopulator to lookup the new decoder
                // config that we're creating here...
                populatorConfig.removeParameter("type"); // Need to remove because we only want 1
                populatorConfig.setParameter("type", reType);

                // Configure the new decoder config...
                typeConverterConfig.setSelector("decoder:" + reType);
                typeConverterConfig.setTargetProfile(extensionContext.getDefaultProfile());

                if (type != null) {
                    typeConverterConfig.setResource(typeConverterFactory.createTypeConverter().getClass().getName());
                }

                for (int i = 0; i < decodeParams.getLength(); i++) {
                    Element decoderParam = (Element) decodeParams.item(i);
                    String name = decoderParam.getAttribute("name");

                    if (name.equals(PreprocessTypeConverter.VALUE_PRE_PROCESSING)) {
                        // Wrap the decoder in the PreprocessDecoder...
                        typeConverterConfig.setResource(PreprocessTypeConverter.class.getName());
                        if (type != null) {
                            typeConverterConfig.setParameter(PreprocessTypeConverter.DELEGATE_TYPE_CONVERTER_FACTORY, typeConverterFactory.getClass().getName());
                        }
                    }
                    typeConverterConfig.setParameter(name, DomUtils.getAllText(decoderParam, true));
                }
            } finally {
                extensionContext.getResourceStack().pop();
            }
        }
    }
}
