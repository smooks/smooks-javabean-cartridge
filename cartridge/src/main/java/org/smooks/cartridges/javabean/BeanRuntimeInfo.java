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

import org.smooks.cdr.*;
import org.smooks.container.*;
import org.smooks.util.ClassUtil;

import java.lang.annotation.Annotation;
import java.util.*;

/**
 * Java bean runtime info.
 *
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class BeanRuntimeInfo {

    private static final String CONTEXT_KEY = BeanRuntimeInfo.class.getName() + "#CONTEXT_KEY";

    /**
     * The basic type that's created and populated for the associated bean.
     */
    private Class<?> populateType;

    /**
     * The bean classification.
     * <p/>
     * We maintain this classification enum because it helps us avoid performing
     * instanceof checks, which are cheap when the instance being checked is
     * an instanceof, but is expensive if it's not.
     */
    private Classification classification;

    /**
     * If the bean classification is an ARRAY_COLLECTION, this member specifies the
     * actual array type.
     */
    private Class<?> arrayType;
    /**
     * Is the type a JAXB Type.
     */
    private boolean isJAXBType = false;

    /**
     * Bean type classification.
     * <p/>
     * We maintain this classification enum because it helps us avoid performing
     * instanceof checks, which are cheap when the instance being checked is
     * an instanceof, but expensive if it's not.
     */
    public static enum Classification {
        NON_COLLECTION,
        ARRAY_COLLECTION,
        COLLECTION_COLLECTION,
        MAP_COLLECTION,
    }

    public BeanRuntimeInfo() {
    }

    public BeanRuntimeInfo(Class<?> clazz) {
    	resolveBeanRuntimeInfo(clazz);
    }

    public BeanRuntimeInfo(String classname) {
    	resolveBeanRuntimeInfo(classname);
    }

    public static void recordBeanRuntimeInfo(String beanId, BeanRuntimeInfo beanRuntimeInfo, ApplicationContext appContext) {
        Map<String, BeanRuntimeInfo> runtimeInfoMap = getRuntimeInfoMap(appContext);
        BeanRuntimeInfo existingBeanConfig = runtimeInfoMap.get(beanId);

        if(existingBeanConfig != null && !beanRuntimeInfo.equals(existingBeanConfig)) {
            throw new SmooksConfigurationException("Multiple configurations present with beanId='" + beanId + "', but the bean runtime infos are not equal i.e bean classes etc are different.  Use a different beanId and the 'setOnMethod' config if needed.");
        }

        runtimeInfoMap.put(beanId, beanRuntimeInfo);
    }

    public static BeanRuntimeInfo getBeanRuntimeInfo(String beanId, ApplicationContext appContext) {
        Map<String, BeanRuntimeInfo> runtimeInfoMap = getRuntimeInfoMap(appContext);

        return runtimeInfoMap.get(beanId);
    }

    public void setClassification(Class<?> clazz) {
        // We maintain a targetType enum because it helps us avoid performing
        // instanceof checks, which are cheap when the instance being checked is
        // an instanceof, but is expensive if it's not....
        if(Map.class.isAssignableFrom(clazz)) {
            this.setClassification(Classification.MAP_COLLECTION);
        } else if(Collection.class.isAssignableFrom(clazz)) {
            this.setClassification(Classification.COLLECTION_COLLECTION);
        } else {
            this.setClassification(Classification.NON_COLLECTION);
        }
    }

    public static BeanRuntimeInfo getBeanRuntimeInfo(String beanId, String beanClassName, ApplicationContext appContext) {
        Map<String, BeanRuntimeInfo> runtimeInfoMap = getRuntimeInfoMap(appContext);

        BeanRuntimeInfo beanRuntimeInfo = runtimeInfoMap.get(beanId);

        if(beanRuntimeInfo == null) {
        	beanRuntimeInfo = new BeanRuntimeInfo(beanClassName);
        	recordBeanRuntimeInfo(beanId, beanRuntimeInfo, appContext);
        }
        return beanRuntimeInfo;
    }

    /**
     * Resolve the Javabean runtime class.
     * <p/>
     * Also performs some checks on the bean.
     *
     * @param beanClass The beanClass name.
     * @return The bean runtime class instance.
     */
    private void resolveBeanRuntimeInfo(String beanClass) {
        Class<?> clazz;

        // If it's an array, we use a List and extract an array from it on the
        // visitAfter event....
        if(beanClass.endsWith("[]")) {
            this.setClassification(BeanRuntimeInfo.Classification.ARRAY_COLLECTION);
            String arrayTypeName = beanClass.substring(0, beanClass.length() - 2);
            try {
            	this.setArrayType(ClassUtil.forName(arrayTypeName, getClass()));
            } catch (ClassNotFoundException e) {
                throw new SmooksConfigurationException("Invalid Smooks bean configuration.  Bean class " + arrayTypeName + " not on classpath.");
            }
            this.setPopulateType(ArrayList.class);

        } else {

	        try {
	            clazz = ClassUtil.forName(beanClass, getClass());
	        } catch (ClassNotFoundException e) {
	            throw new SmooksConfigurationException("Invalid Smooks bean configuration.  Bean class " + beanClass + " not on classpath.");
	        }

	        this.setPopulateType(clazz);
            this.setClassification(clazz);
        }
    }

    private void resolveBeanRuntimeInfo(Class<?> clazz) {
        // If it's an array, we use a List and extract an array from it on the
        // visitAfter event....
        if(clazz.isArray()) {
            this.setClassification(BeanRuntimeInfo.Classification.ARRAY_COLLECTION);
            this.setArrayType(clazz.getComponentType());
            this.setPopulateType(ArrayList.class);
        } else {

	        this.setPopulateType(clazz);
            this.setClassification(clazz);

            // check for a default constructor.
//	        try {
//	            clazz.getConstructor();
//	        } catch (NoSuchMethodException e) {
//	            throw new SmooksConfigurationException("Invalid Smooks bean configuration.  Bean class " + clazz.getName() + " doesn't have a public default constructor.");
//	        }

        }
    }

    @SuppressWarnings("unchecked")
	private static Map<String, BeanRuntimeInfo> getRuntimeInfoMap(ApplicationContext appContext) {
        Map<String, BeanRuntimeInfo> runtimeInfoMap = (Map<String, BeanRuntimeInfo>) appContext.getAttribute(CONTEXT_KEY);

        if(runtimeInfoMap == null) {
            runtimeInfoMap = new HashMap<String, BeanRuntimeInfo>();
            appContext.setAttribute(CONTEXT_KEY, runtimeInfoMap);
        }

        return runtimeInfoMap;
    }

    public Class<?> getPopulateType() {
        return populateType;
    }

    public void setPopulateType(Class<?> populateType) {
        this.populateType = populateType;

        // Check the annotations and see if one of them is the XmlType annotation.  Can't use
        // a type check because XmlType is not in Java5, so need to do a physical name check...
        for (Annotation anno : populateType.getAnnotations()) {
            isJAXBType = anno.annotationType().getName().equals("javax.xml.bind.annotation.XmlType");
            if (isJAXBType) {
                break;
            }
        }
    }

    public Classification getClassification() {
        return classification;
    }

    public void setClassification(Classification classification) {
        this.classification = classification;
    }

    public Class<?> getArrayType() {
        return arrayType;
    }

    public void setArrayType(Class<?> arrayType) {
        this.arrayType = arrayType;
    }

    public boolean isJAXBType() {
        return isJAXBType;
    }

    @Override
	public boolean equals(Object obj) {
        if(obj == null) {
            return false;
        }
        if(obj == this) {
            return true;
        }
        if(!(obj instanceof BeanRuntimeInfo)) {
            return false;
        }

        BeanRuntimeInfo beanInfo = (BeanRuntimeInfo) obj;
        if(beanInfo.getArrayType() != getArrayType()) {
            return false;
        }
        if(beanInfo.getClassification() != getClassification()) {
            return false;
        }
        if(beanInfo.getPopulateType() != getPopulateType()) {
            return false;
        }

        return true;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("Classification: " + classification);
        stringBuilder.append(", Populate Type: : " + populateType.getName());
        if(arrayType != null) {
            stringBuilder.append(", Array Type: " + arrayType.getName());
        }

        return stringBuilder.toString();
    }
}
