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
package org.smooks.cartridges.javabean.dynamic.visitor;

import org.smooks.SmooksException;
import org.smooks.container.ExecutionContext;
import org.smooks.delivery.dom.DOMVisitBefore;
import org.smooks.delivery.sax.SAXUtil;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Namespace Reaper.
 *
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
@SuppressWarnings("unchecked")
public class NamespaceReaper implements DOMVisitBefore {

    public void visitBefore(Element element, ExecutionContext executionContext) throws SmooksException {
        Map<String, String> namespacePrefixMappings = getNamespacePrefixMappings(executionContext);
        NamedNodeMap attributes = element.getAttributes();
        int attributeCount = attributes.getLength();

        for (int i = 0; i < attributeCount; i++) {
            Attr attr = (Attr) attributes.item(i);

            if (XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(attr.getNamespaceURI())) {
                String uri = attr.getValue();
                QName attrQName = SAXUtil.toQName(uri, attr.getLocalName(), attr.getNodeName());

                if (attrQName != null) {
                    addMapping(namespacePrefixMappings, uri, attrQName.getLocalPart());
                }
            }
        }
    }

    private void addMapping(Map<String, String> namespacePrefixMappings, String uri, String prefix) {
        if (uri != null && prefix != null && !namespacePrefixMappings.containsKey(uri)) {
            namespacePrefixMappings.put(uri, prefix);
        }
    }

    public static Map<String, String> getNamespacePrefixMappings(ExecutionContext executionContext) {
        Map<String, String> namespacePrefixMappings = executionContext.getAttribute(NamespaceReaper.class);

        if (namespacePrefixMappings == null) {
            namespacePrefixMappings = new LinkedHashMap<>();
            executionContext.setAttribute(NamespaceReaper.class, namespacePrefixMappings);
        }

        return namespacePrefixMappings;
    }
}
