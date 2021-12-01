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
package org.smooks.cartridges.javabean.binding;

import org.smooks.Smooks;
import org.smooks.api.ExecutionContext;
import org.smooks.api.resource.config.ResourceConfigSeq;
import org.smooks.assertion.AssertArgument;
import org.smooks.cartridges.javabean.binding.model.Bean;
import org.smooks.cartridges.javabean.binding.model.DataBinding;
import org.smooks.cartridges.javabean.binding.model.WiredBinding;
import org.smooks.cartridges.javabean.binding.model.get.GetterGraph;
import org.smooks.cartridges.javabean.binding.xml.XMLBinding;
import org.smooks.engine.lookup.UserDefinedResourceConfigListLookup;
import org.smooks.engine.report.HtmlReportGenerator;
import org.smooks.io.payload.JavaResult;
import org.xml.sax.SAXException;

import javax.xml.transform.Source;
import java.io.IOException;
import java.io.InputStream;

/**
 * Abstract Binding class.
 *
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class AbstractBinding {

    /**
     * Smooks instance.
     */
    private Smooks smooks;
    /**
     * Execution report path.
     */
    private String reportPath;
    /**
     * All configurations added flag.
     */
    private boolean allConfigsAdded = false;
    /**
     * Initialized flag.
     */
    private boolean initialized = false;

    /**
     * Constructor.
     */
    protected AbstractBinding() {
        smooks = new Smooks();
    }

    /**
     * Constructor.
     *
     * @param smooks Smooks instance.
     */
    protected AbstractBinding(Smooks smooks) {
        this.smooks = smooks;
        allConfigsAdded = true;
    }

    /**
     * Add Smooks binding configurations to the binding instance.
     *
     * @param smooksConfigURI Smooks configuration.
     * @throws IOException  Error reading resource stream.
     * @throws SAXException Error parsing the resource stream.
     */
    public AbstractBinding add(String smooksConfigURI) throws IOException, SAXException {
        assertNotAllConfigsAdded();
        assertNotInitialized();
        smooks.addConfigurations(smooksConfigURI);
        return this;
    }

    /**
     * Add Smooks binding configurations to the binding instance.
     *
     * @param smooksConfigStream Smooks configuration.
     * @throws IOException  Error reading resource stream.
     * @throws SAXException Error parsing the resource stream.
     */
    public AbstractBinding add(InputStream smooksConfigStream) throws IOException, SAXException {
        assertNotAllConfigsAdded();
        assertNotInitialized();
        smooks.addConfigurations(smooksConfigStream);
        return this;
    }

    /**
     * Initialize the binding instance.
     */
    public AbstractBinding initialise() {
        assertNotInitialized();
        smooks.createExecutionContext();
        this.allConfigsAdded = true;
        this.initialized = true;
        return this;
    }

    /**
     * Get the underlying {@link Smooks} instance.
     *
     * @return The underlying {@link Smooks} instance.
     */
    public Smooks getSmooks() {
        return smooks;
    }

    /**
     * Set the execution report output path.
     *
     * @param reportPath The execution report output path.
     */
    public AbstractBinding setReportPath(String reportPath) {
        this.reportPath = reportPath;
        return this;
    }

    /**
     * Bind the input source to the specified type.
     * <p/>
     * In order to make a cleaner API, implementing classes should create a more
     * appropriately named method based on the target binding format, that just
     * delegates to this method e.g. {@link XMLBinding#fromXML(javax.xml.transform.Source, Class)}
     * and {@link XMLBinding#toXML(Object, java.io.Writer)}.
     *
     * @param inputSource The input source.
     * @param toType      The target type.
     * @return The target binding type instance.
     * @throws IOException Error binding source to target type.
     */
    protected <T> T bind(Source inputSource, Class<T> toType) throws IOException {
        AssertArgument.isNotNull(inputSource, "inputSource");
        AssertArgument.isNotNull(toType, "toType");

        assertInitialized();

        JavaResult javaResult = new JavaResult();
        ExecutionContext executionContext = smooks.createExecutionContext();

        if (reportPath != null) {
            executionContext.getContentDeliveryRuntime().getExecutionEventListeners().add(new HtmlReportGenerator(reportPath));
        }

        smooks.filterSource(executionContext, inputSource, javaResult);

        return javaResult.getBean(toType);
    }

    protected ResourceConfigSeq getUserDefinedResourceList() {
        return smooks.getApplicationContext().getRegistry().lookup(new UserDefinedResourceConfigListLookup(smooks.getApplicationContext().getRegistry()));
    }

    protected GetterGraph constructContextualGetter(DataBinding binding) {
        GetterGraph contextualGetter = new GetterGraph();

        contextualGetter.add(binding);
        addToContextualGetter(contextualGetter, binding.getParentBean());

        return contextualGetter;
    }

    protected GetterGraph constructContextualGetter(Bean bean) {
        return addToContextualGetter(new GetterGraph(), bean);
    }

    private GetterGraph addToContextualGetter(GetterGraph contextualGetter, Bean bean) {
        Bean theBean = bean;

        while(theBean != null) {
            Bean parentBean = theBean.getWiredInto();

            if(parentBean != null) {
                if(parentBean.isCollection()){
                    // Contextual selectors stop once they hit a parent Collection theBean...
                    Bean wiredInto = parentBean.getWiredInto();
                    if(wiredInto != null) {
                        // Use the collection item's beanId as the context object name
                        // because collection items don't have property names...
                        contextualGetter.setContextObjectName(theBean.getBeanId());
                    }
                    break;
                }

                WiredBinding binding = parentBean.getWiredBinding(theBean);

                if(binding == null) {
                    throw new IllegalStateException("Failed to locate a wiring of theBean '" + theBean + "' on theBean '" + parentBean + "'.");
                }

                contextualGetter.add(parentBean, binding.getProperty());
            }

            theBean = parentBean;
        }

        return contextualGetter;
    }

    protected void assertInitialized() {
        if(!initialized) {
            throw new IllegalStateException("Illegal call to method before instance is initialized.  Must call the 'initialize' method first.");
        }
    }

    protected void assertNotAllConfigsAdded() {
        if(allConfigsAdded) {
            throw new IllegalStateException("Illegal call to method after all configurations have been added.");
        }
    }

    protected void assertNotInitialized() {
        if(initialized) {
            throw new IllegalStateException("Illegal call to method after instance is initialized.");
        }
    }
}
