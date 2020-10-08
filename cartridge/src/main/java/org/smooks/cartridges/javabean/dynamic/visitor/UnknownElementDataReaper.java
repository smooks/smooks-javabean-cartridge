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
import org.smooks.cartridges.javabean.dynamic.BeanMetadata;
import org.smooks.delivery.Fragment;
import org.smooks.delivery.dom.serialize.DefaultDOMSerializerVisitor;
import org.smooks.javabean.lifecycle.BeanContextLifecycleEvent;
import org.w3c.dom.*;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Unknown element data reaper.
 * <p/>
 * Models can sometimes be created from XML which contains valid elements that are not being mapped into the model.
 * We don't want to loose this data in the model, so we capture it as "pre
 * 
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class UnknownElementDataReaper {

    public static String getPreText(Element element, List<BeanMetadata> beanMetadataSet, BeanContextLifecycleEvent event) {
        StringWriter serializeWriter = new StringWriter();
        List<Node> toSerializeNodes = new ArrayList<Node>();
        Node current = element;

        // Skip back through the siblings until we get an element that has an associated
        // bean...
        while(current != null) {
            current = current.getPreviousSibling();

            if(current == null) {
                // This will result in all siblings back to the start
                // of this sibling set...
                break;
            }

            if(current instanceof Element) {
                if(isOnModelSourcePath(new Fragment((Element) current), beanMetadataSet)) {
                    // The "previous" element is associated with the creation/population of a bean in the
                    // model, so stop here...
                    break;
                }
            }

            toSerializeNodes.add(0, current);
        }

        for(Node node : toSerializeNodes) {
            try {
                serialize(node, serializeWriter);
            } catch (IOException e) {
                throw new SmooksException("Unexpected pre-text node serialization exception.", e);
            }
        }

        // Get rid of leading and space characters (only spaces - not all whitespace).
        // This helps eliminate ugly indentation issues in the serialized XML...
        String xml;
        try {
            xml = normalizeLines(serializeWriter.toString());
        } catch (IOException e) {
            throw new SmooksException("Unexpected pre-text node serialization exception while attempting to remove excess whitespace.", e);
        }
        StringBuilder trimEnd = new StringBuilder(xml);
        while(trimEnd.length() > 0 && trimEnd.charAt(0) == ' ') {
            trimEnd.deleteCharAt(0);
        }
        while(trimEnd.length() > 1 && trimEnd.charAt(0) == '\n' && trimEnd.charAt(1) == '\n') {
            trimEnd.deleteCharAt(0);
        }
        while(trimEnd.length() > 0 && trimEnd.charAt(trimEnd.length() - 1) == ' ') {
            trimEnd.deleteCharAt(trimEnd.length() - 1);
        }
        while(trimEnd.length() > 1 && trimEnd.charAt(trimEnd.length() - 1) == '\n' && trimEnd.charAt(trimEnd.length() - 2) == '\n') {
            trimEnd.deleteCharAt(trimEnd.length() - 1);
        }

        return trimEnd.toString();
    }

    private static boolean isOnModelSourcePath(Fragment fragment, List<BeanMetadata> beanMetadataSet) {
        for(BeanMetadata beanMetadata : beanMetadataSet) {
            if(fragment.equals(beanMetadata.getCreateSource())) {
                return true;
            }

            for(Fragment populateSource : beanMetadata.getPopulateSources()) {
                if(fragment.isParentFragment(populateSource)) {
                    return true;
                }
            }
        }

        return false;
    }

    public static String normalizeLines(String xml) throws IOException {
        StringBuffer stringBuf = new StringBuffer();
        int xmlLength = xml.length();

        for(int i = 0; i < xmlLength; i++) {
            char character = xml.charAt(i);
            if(character != '\r') {
                stringBuf.append(character);
            }
        }

        return stringBuf.toString();
    }

    private static final DefaultDOMSerializerVisitor SERIALIZER_VISITOR;
    static {
        SERIALIZER_VISITOR = new DefaultDOMSerializerVisitor();
        SERIALIZER_VISITOR.setCloseEmptyElements(Optional.of(true));
        SERIALIZER_VISITOR.setRewriteEntities(Optional.of(true)); 
        SERIALIZER_VISITOR.postConstruct();
    }
    
    private static void serialize(Node node, Writer writer) throws IOException {
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            Element element = (Element) node;
            NodeList children = element.getChildNodes();
            int childCount = children.getLength();

            SERIALIZER_VISITOR.writeStartElement(element, writer, null);

            // Write the child nodes...
            for(int i = 0; i < childCount; i++) {
                serialize(children.item(i), writer);
            }

            SERIALIZER_VISITOR.writeEndElement(element, writer, null);
        } else {
            SERIALIZER_VISITOR.writeCharacterData(node, writer, null);
        }
    }
}
