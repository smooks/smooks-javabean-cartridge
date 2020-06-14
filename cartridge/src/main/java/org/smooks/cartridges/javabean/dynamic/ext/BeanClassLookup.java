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
package org.smooks.cartridges.javabean.dynamic.ext;

import org.smooks.SmooksException;
import org.smooks.cartridges.javabean.ext.BeanConfigUtil;
import org.smooks.cdr.SmooksConfigurationException;
import org.smooks.cdr.SmooksResourceConfiguration;
import org.smooks.cdr.extension.ExtensionContext;
import org.smooks.container.ExecutionContext;
import org.smooks.delivery.dom.DOMVisitBefore;
import org.w3c.dom.Element;

/**
 * Bean class lookup visitor.
 * <p/>
 * Used during processing of the <dmb:writer> extended DMB configuration
 * for looking up the actual bean runtime Class from the beanId
 * specified on the on the <dmb:writer>.
 *
 * <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class BeanClassLookup implements DOMVisitBefore {

    public void visitBefore(Element element, ExecutionContext executionContext) throws SmooksException {
        // The current config on the stack must be <dmb:writer>...
        ExtensionContext extensionContext = ExtensionContext.getExtensionContext(executionContext);
        SmooksResourceConfiguration dmbWriterConfig = extensionContext.getResourceStack().peek();
        if(dmbWriterConfig.getStringParameter("beanClass") == null) {
            String beanId = dmbWriterConfig.getStringParameter("beanId");

            if(beanId == null) {
                throw new SmooksConfigurationException("One of the 'beanClass' or 'beanId' attributes must be configured on the <dmb:writer> configuration.");                
            }

            SmooksResourceConfiguration beanCreatorConfig = BeanConfigUtil.findBeanCreatorConfig(beanId, executionContext);
            if(beanCreatorConfig == null) {
                throw new SmooksConfigurationException("Cannot find <jb:bean> configuration for beanId '" + beanId + "' for <dmb:writer>.  Reordered <dmb:writer> after <jb:bean> config.");
            }

            String beanClass = beanCreatorConfig.getStringParameter(BeanConfigUtil.BEAN_CLASS_CONFIG);
            if(beanClass == null) {
                throw new SmooksConfigurationException("Cannot create find BeanWriter for beanId '" + beanId + "'.  The associated <jb:bean> configuration does not define a bean Class name.");
            }

            dmbWriterConfig.setParameter(BeanConfigUtil.BEAN_CLASS_CONFIG, beanClass);
        }
    }
}
