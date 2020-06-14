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

import org.smooks.cartridges.javabean.binding.SerializationContext;
import org.smooks.cartridges.javabean.binding.model.get.Getter;
import org.smooks.cartridges.javabean.binding.model.get.GetterGraph;
import org.smooks.javabean.DataEncoder;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;

/**
 * Abstract XML Serialization Node.
 *
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public abstract class XMLSerializationNode {

    protected QName qName;
    protected XMLElementSerializationNode parent;
    protected DataEncoder encoder;
    protected String defaultVal;
    protected boolean isCollection = false;
    protected NodeGetter nodeGetter;
    protected NodeGetter collectionGetter;

    protected XMLSerializationNode(QName qName) {
        this.qName = qName;
    }

    public QName getQName() {
        return qName;
    }

    public XMLElementSerializationNode getParent() {
        return parent;
    }

    public void setParent(XMLElementSerializationNode parent) {
        this.parent = parent;
    }

    public void setEncoder(DataEncoder encoder) {
        this.encoder = encoder;
    }

    public void setDefaultVal(String defaultVal) {
        this.defaultVal = defaultVal;
    }

    public void setIsCollection(boolean isCollection) {
        this.isCollection = isCollection;
    }

    public abstract void serialize(Writer outputStream, SerializationContext context) throws IOException;

    protected String getValue(SerializationContext context) {
        if(nodeGetter != null) {
            Object value = context.getValue(nodeGetter.contextObjectName, nodeGetter.getter);

            if(value == null) {
                value = defaultVal;
                if(value == null) {
                    return null;
                }
            }

            if(encoder != null) {
                return encoder.encode(value);
            } else {
                return value.toString();
            }
        } else {
            return null;
        }
    }

    public void setGetter(GetterGraph getter) {
        this.nodeGetter = new NodeGetter(getter.getContextObjectName(), getter);
    }

    public void setGetter(Getter getter) {
        this.nodeGetter = new NodeGetter(getter);
    }

    public void setCollectionGetter(String contextObjectName, GetterGraph getter) {
        this.collectionGetter = new NodeGetter(contextObjectName, getter);
    }

    public NodeGetter getNodeGetter() {
        return nodeGetter;
    }

    public NodeGetter getCollectionGetter() {
        return collectionGetter;
    }

    protected void writeName(Writer outputStream) throws IOException {
        String prefix = qName.getPrefix();
        String localPart = qName.getLocalPart();

        if(prefix != null && !prefix.equals(XMLConstants.DEFAULT_NS_PREFIX)) {
            outputStream.write(prefix);
            outputStream.write(":");
        }
        outputStream.write(localPart);
    }

    public static <T extends XMLSerializationNode> T getNode(QName qName, Collection<T> nodeList) {
        for(T node : nodeList) {
            if(node.getQName().equals(qName)) {
                return node;
            }
        }

        return null;
    }

    protected void copyProperties(XMLSerializationNode node) {
        node.qName = qName;
        node.encoder = encoder;
        node.defaultVal = defaultVal;
    }

    @Override
    public String toString() {
        return qName.toString();
    }

    protected boolean hasData(SerializationContext context) {
        if(nodeGetter == null || context.getValue(nodeGetter.contextObjectName, nodeGetter.getter) != null) {
            return true;
        }
        return false;
    }

    protected class NodeGetter {
        protected String contextObjectName;
        protected Getter getter;

        public NodeGetter(Getter getter) {
            this.contextObjectName = SerializationContext.ROOT_OBJ;
            this.getter = getter;
        }

        private NodeGetter(String contextObjectName, Getter getter) {
            this.contextObjectName = contextObjectName;
            this.getter = getter;
        }
    }
}
