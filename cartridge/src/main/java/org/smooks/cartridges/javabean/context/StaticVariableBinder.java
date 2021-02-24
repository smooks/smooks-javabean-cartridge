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
package org.smooks.cartridges.javabean.context;

import org.smooks.api.ApplicationContext;
import org.smooks.api.ExecutionContext;
import org.smooks.api.SmooksConfigException;
import org.smooks.api.SmooksException;
import org.smooks.api.bean.context.BeanContext;
import org.smooks.api.bean.repository.BeanId;
import org.smooks.api.delivery.fragment.Fragment;
import org.smooks.api.resource.config.Parameter;
import org.smooks.api.resource.config.ResourceConfig;
import org.smooks.api.resource.visitor.sax.ng.ElementVisitor;
import org.smooks.engine.delivery.fragment.NodeFragment;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Element;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Static variable binding visitor.
 * <p/>
 * Binds resource paramater variables into the bean context (managed by the
 * {@link BeanContext}).  The paramater values are all bound
 * into a bean accessor Map named "statvar", so variables bound in this way
 * can be referenced in expressions or templates as e.g "<i>${statvar.<b>xxx</b>}</i>"
 * (for static variable "xxx").
 *
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class StaticVariableBinder implements ElementVisitor {

    private static final String STATVAR = "statvar";

    private BeanId beanId;

    @Inject
    private ResourceConfig resourceConfig;


    @Inject
    private ApplicationContext appContext;

    @PostConstruct
    public void postConstruct() throws SmooksConfigException {
        beanId = appContext.getBeanIdStore().getBeanId(STATVAR);

        if(beanId == null) {
        	beanId = appContext.getBeanIdStore().register(STATVAR);
        }


    }
    
    @Override
    public void visitBefore(Element element, ExecutionContext executionContext) throws SmooksException {
        bindParamaters(executionContext, new NodeFragment(element));
    }

    @Override
    public void visitAfter(Element element, ExecutionContext executionContext) throws SmooksException {
    }

    private void bindParamaters(ExecutionContext executionContext, Fragment<?> source) {
        List<?> params = resourceConfig.getParameterValues();

        for (Object parameter : params) {
            // It's either an object, or list of objects...
            if (parameter instanceof List<?>) {
                // Bind the first paramater...
                bindParameter((Parameter<?>) ((List<?>) parameter).get(0), executionContext, source);
            } else if (parameter instanceof Parameter) {
                bindParameter((Parameter<?>) parameter, executionContext, source);
            }
        }
    }


	private void bindParameter(Parameter<?> parameter, ExecutionContext executionContext, Fragment<?> source) {
        Map<String, Object> params = null;

        BeanContext beanContext = executionContext.getBeanContext();

        try {
        	@SuppressWarnings("unchecked")
        	Map<String, Object> castParams = (Map<String, Object>) beanContext.getBean(beanId);

        	params = castParams;
        } catch(ClassCastException e) {
            throw new SmooksException("Illegal use of reserved beanId '" + STATVAR + "'.  Must be a Map.  Is a " + params.getClass().getName(), e);
        }

        if(params == null) {
            params = new HashMap<String, Object>();
            beanContext.addBean(beanId, params, source);
        }

        params.put(parameter.getName(), parameter.getValue(executionContext.getContentDeliveryRuntime().getContentDeliveryConfig()));
    }

    @Override
    public void visitChildText(CharacterData characterData, ExecutionContext executionContext) throws SmooksException {
        
    }

    @Override
    public void visitChildElement(Element childElement, ExecutionContext executionContext) throws SmooksException {

    }
}
