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
package org.smooks.cartridges.javabean.binding.xml;

import org.smooks.api.resource.config.xpath.SelectorPath;
import org.smooks.api.resource.config.xpath.SelectorStep;
import org.smooks.cartridges.javabean.binding.SerializationContext;
import org.smooks.cartridges.javabean.binding.model.get.Getter;
import org.smooks.cartridges.javabean.binding.model.get.GetterGraph;
import org.smooks.engine.resource.config.xpath.step.AttributeSelectorStep;
import org.smooks.engine.resource.config.xpath.step.ElementSelectorStep;
import org.smooks.support.XmlUtil;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Abstract XML Serialization Node.
 *
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class XMLElementSerializationNode extends XMLSerializationNode {

    private final List<XMLAttributeSerializationNode> attributes = new ArrayList<>();
    private final List<XMLElementSerializationNode> elements = new ArrayList<>();

    public XMLElementSerializationNode(QName qName) {
        super(qName);
    }

    public List<XMLAttributeSerializationNode> getAttributes() {
        return attributes;
    }

    public List<XMLElementSerializationNode> getElements() {
        return elements;
    }

    @Override
    public void serialize(Writer outputStream, SerializationContext context) throws IOException {

        // Write the start of the element...
        indent(outputStream, context);
        outputStream.write("<");
        writeName(outputStream);

        // Write the attributes...
        writeAttributes(outputStream, context);

        if(elements.isEmpty()) {
            String value = getValue(context);

            if(value != null) {
                char[] characters = value.toCharArray();

                outputStream.write(">");
                XmlUtil.encodeTextValue(characters, 0, characters.length, outputStream);
                outputStream.write("</");
                writeName(outputStream);
                outputStream.write(">");
            } else {
                outputStream.write("/>");
            }
        } else {
            outputStream.write(">");

            // Write the child elements...
            writeElements(outputStream, context);

            // Write the end of the element...
            outputStream.write("\n");
            indent(outputStream, context);
            outputStream.write("</");
            writeName(outputStream);
            outputStream.write(">");
        }
    }

    private void writeAttributes(Writer outputStream, SerializationContext context) throws IOException {
        for(XMLAttributeSerializationNode attribute : attributes) {
            attribute.serialize(outputStream, context);
        }
    }

    private void writeElements(Writer outputStream, SerializationContext context) throws IOException {
        context.incDepth();
        for(XMLElementSerializationNode element : elements) {
            if(element.isCollection) {
                NodeGetter collectionNodeGetter = element.getCollectionGetter();
                Getter collectionGetter = collectionNodeGetter.getter;
                Object collectionObject;
                List<?> collection = null;

                if (collectionGetter instanceof GetterGraph) {
                    collectionObject = context.getValue(((GetterGraph) collectionGetter).getContextObjectName(), collectionGetter);
                } else {
                    collectionObject = context.getValue(collectionGetter);
                }

                if(collectionObject instanceof List) {
                    collection = (List<?>) collectionObject;
                } else if(collectionObject instanceof Object[]) {
                    collection = Arrays.asList((Object[]) collectionObject);
                }

                if(collection != null && !collection.isEmpty()) {
                    try {
                        for (Object collectionItem : collection) {
                            context.addObject(collectionNodeGetter.contextObjectName, collectionItem);
                            outputStream.write("\n");
                            element.serialize(outputStream, context);
                        }
                    } finally {
                        // Be sure to clear this from the context...
                        context.removeObject(collectionNodeGetter.contextObjectName);
                    }
                }
            } else {
                if(element.hasData(context)) {
                    outputStream.write("\n");
                    element.serialize(outputStream, context);
                }
            }
        }
        context.decDepth();
    }

    @Override
    protected boolean hasData(SerializationContext context) {
        // If any part of the element has data...
        if(super.hasData(context)) {
            return true;
        }
        for(XMLAttributeSerializationNode attribute : attributes) {
            if(attribute.hasData(context)) {
                return true;
            }
        }

        return false;
    }

    private static final char[] INDENT_BUF = new char[512];
    static {
        Arrays.fill(INDENT_BUF, ' ');
    }

    private void indent(Writer outputStream, SerializationContext context) throws IOException {
        int indent = context.getCurrentDepth() * 4; // Indent 4 spaces per level in the hierarchy...

        if(indent > 0) {
            indent = Math.min(indent, INDENT_BUF.length); // Making sure we don't OOB on the indent buffer
            outputStream.write(INDENT_BUF, 0, indent);
        }
    }

    public XMLSerializationNode findNode(SelectorPath selectorPath) {
        if (selectorPath.size() == 3 && selectorPath.get(2) instanceof AttributeSelectorStep) {
            return getAttribute(selectorPath.get(2), attributes, false);
        } else if (selectorPath.size() == 2) {
            return this;
        } else {
            return getPathNode(selectorPath, 2, false);
        }
    }

    public XMLSerializationNode getPathNode(SelectorPath selectorPath, int stepIndex, boolean create) {
        if (stepIndex >= selectorPath.size()) {
            throw new IllegalStateException("Unexpected call to 'addPathNode'.  SelectorStep index out of bounds.");
        }

        SelectorStep selectorStep = selectorPath.get(stepIndex);

        if (stepIndex == selectorPath.size() - 1 && selectorStep instanceof AttributeSelectorStep) {
            // It's an attribute node...
            XMLElementSerializationNode elementNode = getElement(selectorPath.get(stepIndex - 1), getParent().elements, create);
            return addAttributeNode(elementNode, selectorStep, create);
        } else {
            // It's an element...
            XMLElementSerializationNode childElement = getElement(selectorStep, elements, create);
            if (childElement != null) {
                childElement.setParent(this);
                if (stepIndex < selectorPath.size() - 1) {
                    // Drill down again...
                    return childElement.getPathNode(selectorPath, stepIndex + 1, create);
                } else {
                    return childElement;
                }
            } else {
                return null;
            }
        }
    }

    public static XMLSerializationNode addAttributeNode(XMLElementSerializationNode elementNode, SelectorStep selectorStep, boolean create) {
        XMLAttributeSerializationNode attribute = getAttribute(selectorStep, elementNode.attributes, create);
        if (attribute != null) {
            attribute.setParent(elementNode);
        }
        return attribute;
    }

    public static XMLElementSerializationNode getElement(SelectorStep step, Collection<XMLElementSerializationNode> elementList, boolean create) {
        QName qName = ((ElementSelectorStep) step).getQName();
        XMLElementSerializationNode element = getNode(qName, elementList);

        if (element == null && create) {
            element = new XMLElementSerializationNode(qName);
            elementList.add(element);
        }

        return element;
    }

    public static XMLAttributeSerializationNode getAttribute(SelectorStep step, Collection<XMLAttributeSerializationNode> attributeList, boolean create) {
        QName qName = ((AttributeSelectorStep) step).getQName();
        XMLAttributeSerializationNode attribute = getNode(qName, attributeList);

        if (attribute == null && create) {
            attribute = new XMLAttributeSerializationNode(qName);
            attributeList.add(attribute);
        }

        return attribute;
    }

    @Override
    protected Object clone() {
        XMLElementSerializationNode clone = new XMLElementSerializationNode(qName);

        copyProperties(clone);
        for (XMLElementSerializationNode element : elements) {
            XMLElementSerializationNode elementClone = (XMLElementSerializationNode) element.clone();
            clone.elements.add(elementClone);
            elementClone.setParent(clone);
        }
        for (XMLAttributeSerializationNode attribute : attributes) {
            XMLAttributeSerializationNode attributeClone = (XMLAttributeSerializationNode) attribute.clone();
            clone.attributes.add(attributeClone);
            attributeClone.setParent(clone);
        }

        return clone;
    }
}
