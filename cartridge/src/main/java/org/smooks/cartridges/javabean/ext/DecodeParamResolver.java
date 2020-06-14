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
import org.smooks.cdr.SmooksResourceConfiguration;
import org.smooks.cdr.extension.ExtensionContext;
import org.smooks.container.ExecutionContext;
import org.smooks.delivery.dom.DOMVisitBefore;
import org.smooks.javabean.DataDecoder;
import org.smooks.javabean.decoders.PreprocessDecoder;
import org.smooks.xml.DomUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.UUID;

/**
 * Type decoder parameter mapping visitor.
 *
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class DecodeParamResolver implements DOMVisitBefore {

    public void visitBefore(Element element, ExecutionContext executionContext) throws SmooksException {
        NodeList decodeParams = element.getElementsByTagNameNS(element.getNamespaceURI(), "decodeParam");

        if(decodeParams.getLength() > 0) {
            ExtensionContext extensionContext = ExtensionContext.getExtensionContext(executionContext);
            SmooksResourceConfiguration populatorConfig = extensionContext.getResourceStack().peek();
            SmooksResourceConfiguration decoderConfig = new SmooksResourceConfiguration();

            extensionContext.addResource(decoderConfig);
            try {
                String type = populatorConfig.getStringParameter("type");
                DataDecoder decoder = DataDecoder.Factory.create(type);
                String reType = UUID.randomUUID().toString();

                // Need to retype the populator configuration so as to get the
                // value binding BeanInstancePopulator to lookup the new decoder
                // config that we're creating here...
                populatorConfig.removeParameter("type"); // Need to remove because we only want 1
                populatorConfig.setParameter("type", reType);

                // Configure the new decoder config...
                decoderConfig.setSelector("decoder:" + reType);
                decoderConfig.setTargetProfile(extensionContext.getDefaultProfile());
                
                if(type != null) {
                	decoderConfig.setResource(decoder.getClass().getName());
                }
                
                for(int i = 0; i < decodeParams.getLength(); i++) {
                    Element decoderParam = (Element) decodeParams.item(i);
                    String name = decoderParam.getAttribute("name");
                    
                    if(name.equals(PreprocessDecoder.VALUE_PRE_PROCESSING)) {
                    	// Wrap the decoder in the PreprocessDecoder...
                    	decoderConfig.setResource(PreprocessDecoder.class.getName());
                    	if(type != null) {
                    		decoderConfig.setParameter(PreprocessDecoder.BASE_DECODER, decoder.getClass().getName());
                    	}
                    }                    
                    decoderConfig.setParameter(name, DomUtils.getAllText(decoderParam, true));
                }
            } finally {
                extensionContext.getResourceStack().pop();
            }
        }
    }
}
