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

import org.smooks.api.ExecutionContext;
import org.smooks.api.SmooksConfigException;
import org.smooks.api.resource.config.ResourceConfig;
import org.smooks.cartridges.javabean.BeanInstanceCreator;
import org.smooks.engine.resource.config.DefaultConfigSearch;
import org.smooks.engine.resource.config.loader.xml.extension.ExtensionContext;

import java.util.List;

/**
 * Bean configuration utilities.
 *
 * <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public abstract class BeanConfigUtil {

    public static final String BEAN_CLASS_CONFIG = "beanClass";

    public static ResourceConfig findBeanCreatorConfig(String beanId, ExecutionContext executionContext) {
        ExtensionContext extensionContext = executionContext.get(ExtensionContext.EXTENSION_CONTEXT_TYPED_KEY);
        List<ResourceConfig> creatorConfigs = extensionContext.lookupResourceConfigs(new DefaultConfigSearch().resource(BeanInstanceCreator.class.getName()).param("beanId", beanId));

        if (creatorConfigs.size() > 1) {
            throw new SmooksConfigException("Multiple <jb:bean> configurations exist for beanId '" + beanId + "'.  'beanId' values must be unique.");
        }

        return creatorConfigs.get(0);
    }
}
