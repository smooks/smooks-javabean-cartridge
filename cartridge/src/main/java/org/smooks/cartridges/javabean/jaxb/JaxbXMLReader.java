/*-
 * ========================LICENSE_START=================================
 * smooks-javabean-cartridge
 * %%
 * Copyright (C) 2020 - 2025 Smooks
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
package org.smooks.cartridges.javabean.jaxb;

import com.thoughtworks.xstream.io.xml.SaxWriter;
import jakarta.annotation.PostConstruct;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import org.smooks.api.ApplicationContext;
import org.smooks.api.ExecutionContext;
import org.smooks.api.SmooksConfigException;
import org.smooks.api.SmooksException;
import org.smooks.api.resource.reader.JavaXMLReader;
import org.smooks.cartridges.javabean.BeanRuntimeInfo;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class JaxbXMLReader implements JavaXMLReader {

    @Inject
    @Named("beanClass")
    protected String beanClassName;

    @Inject
    protected List<String> objectFactories = new ArrayList<>();

    @Inject
    protected ApplicationContext applicationContext;

    protected JAXBContext jaxbContext;
    protected ExecutionContext executionContext;
    protected ContentHandler contentHandler;
    protected List<Object> sourceObjects;
    protected ErrorHandler errorHandler;
    protected Marshaller marshaller;

    @PostConstruct
    public void postConstruct() {
        List<Class<?>> classesToBeBound = objectFactories.stream().map(of -> {
            try {
                return Class.forName(of, true, applicationContext.getClassLoader());
            } catch (ClassNotFoundException e) {
                throw new SmooksException(e);
            }
        }).collect(Collectors.toList());
        classesToBeBound.add(new BeanRuntimeInfo(beanClassName).getPopulateType());


        try {
            jaxbContext = JAXBContext.newInstance(classesToBeBound.toArray(new Class[0]));
            marshaller = jaxbContext.createMarshaller();
        } catch (JAXBException e) {
            throw new SmooksConfigException(e);
        }
    }

    @Override
    public void setSourceObjects(List<Object> sourceObjects) throws SmooksConfigException {
        this.sourceObjects = sourceObjects;
    }

    @Override
    public void setExecutionContext(ExecutionContext executionContext) {
        this.executionContext = executionContext;
    }

    @Override
    public boolean getFeature(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
        return false;
    }

    @Override
    public void setFeature(String name, boolean value) throws SAXNotRecognizedException, SAXNotSupportedException {

    }

    @Override
    public Object getProperty(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
        return null;
    }

    @Override
    public void setProperty(String name, Object value) throws SAXNotRecognizedException, SAXNotSupportedException {

    }

    @Override
    public void setEntityResolver(EntityResolver entityResolver) {
    }

    @Override
    public EntityResolver getEntityResolver() {
        return null;
    }

    @Override
    public void setDTDHandler(DTDHandler handler) {

    }

    @Override
    public DTDHandler getDTDHandler() {
        return null;
    }

    @Override
    public void setContentHandler(ContentHandler contentHandler) {
        this.contentHandler = contentHandler;
    }

    @Override
    public ContentHandler getContentHandler() {
        return contentHandler;
    }

    @Override
    public void setErrorHandler(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    @Override
    public ErrorHandler getErrorHandler() {
        return errorHandler;
    }

    @Override
    public void parse(InputSource input) {
        try {
            for (Object sourceObject : sourceObjects) {
                marshaller.marshal(sourceObject, contentHandler);
            }
        } catch (JAXBException e) {
            throw new SmooksException(e);
        }
    }

    @Override
    public void parse(String systemId) throws IOException, SAXException {

    }
}
