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
package org.smooks.cartridges.javabean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smooks.api.ExecutionContext;
import org.smooks.api.SmooksConfigException;
import org.smooks.assertion.AssertArgument;
import org.smooks.support.ClassUtils;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Bean utility methods.
 *
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public abstract class BeanUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(BeanUtils.class);

    /**
     * Create the bean setter method instance for this visitor.
     *
     * @param setterName The setter method name.
     * @param setterParamType
     * @return The bean setter method.
     */
    public static Method createSetterMethod(String setterName, Object bean, Class<?> setterParamType) {
        Method beanSetterMethod = ClassUtils.getSetterMethod(setterName, bean, setterParamType);

        // Try it as a list...
        if (beanSetterMethod == null && List.class.isAssignableFrom(setterParamType)) {
            String setterNamePlural = setterName + "s";

            // Try it as a List using the plural name...
            beanSetterMethod = ClassUtils.getSetterMethod(setterNamePlural, bean, setterParamType);
            if (beanSetterMethod == null) {
                // Try it as an array using the non-plural name...
            }
        }

        // Try it as a primitive...
        if(beanSetterMethod == null && Integer.class.isAssignableFrom(setterParamType)) {
            beanSetterMethod = ClassUtils.getSetterMethod(setterName, bean, Integer.TYPE);
        }
        if(beanSetterMethod == null && Long.class.isAssignableFrom(setterParamType)) {
            beanSetterMethod = ClassUtils.getSetterMethod(setterName, bean, Long.TYPE);
        }
        if(beanSetterMethod == null && Float.class.isAssignableFrom(setterParamType)) {
            beanSetterMethod = ClassUtils.getSetterMethod(setterName, bean, Float.TYPE);
        }
        if(beanSetterMethod == null && Double.class.isAssignableFrom(setterParamType)) {
            beanSetterMethod = ClassUtils.getSetterMethod(setterName, bean, Double.TYPE);
        }
        if(beanSetterMethod == null && Character.class.isAssignableFrom(setterParamType)) {
            beanSetterMethod = ClassUtils.getSetterMethod(setterName, bean, Character.TYPE);
        }
        if(beanSetterMethod == null && Short.class.isAssignableFrom(setterParamType)) {
            beanSetterMethod = ClassUtils.getSetterMethod(setterName, bean, Short.TYPE);
        }
        if(beanSetterMethod == null && Byte.class.isAssignableFrom(setterParamType)) {
            beanSetterMethod = ClassUtils.getSetterMethod(setterName, bean, Byte.TYPE);
        }
        if(beanSetterMethod == null && Boolean.class.isAssignableFrom(setterParamType)) {
            beanSetterMethod = ClassUtils.getSetterMethod(setterName, bean, Boolean.TYPE);
        }

        return beanSetterMethod;
    }

    /**
     * Get the bean instance on which this populator instance is to set data.
     *
     * @param execContext The execution context.
     * @return The bean instance.
     * @deprecated
     */
    @Deprecated
    public static Object getBean(String beanId, ExecutionContext execContext) {
        Object bean;


        // Get the bean instance from the request.  If there is non, it's a bad config!!
        bean =  execContext.getBeanContext().getBean(beanId);

        if (bean == null) {
            throw new SmooksConfigException("Bean instance [" + beanId + "] not available and bean runtime class not set on configuration.");
        }

        return bean;
    }

    /**
     * Convert the supplied List into an array of the specified array type.
     * @param list The List instance to be converted.
     * @param arrayClass The array type.
     * @return The array.
     */
    public static Object convertListToArray(List<?> list, Class<?> arrayClass) {
        AssertArgument.isNotNull(list, "list");
        AssertArgument.isNotNull(arrayClass, "arrayClass");

        int length = list.size();
        Object arrayObj = Array.newInstance(arrayClass, list.size());
        for(int i = 0; i < length; i++) {
            try {
                Array.set(arrayObj, i, list.get(i));
            } catch(ClassCastException e) {
                LOGGER.error("Failed to cast type '" + list.get(i).getClass().getName() + "' to '" + arrayClass.getName() + "'.", e);
            }
        }

        return arrayObj;
    }
}
