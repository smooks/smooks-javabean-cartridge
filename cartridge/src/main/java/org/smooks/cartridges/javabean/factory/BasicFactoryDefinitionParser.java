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
package org.smooks.cartridges.javabean.factory;

import org.smooks.api.ExecutionContext;
import org.smooks.support.ClassUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * The BasicFactoryDefinitionParser supports two kinds of
 * factory definitions:
 * <ol>
 * 	<li><b>some.package.SomeFactory#createObject</b><br>
 * 		 	This defines that the 'createObject' of the class 'some.package.SomeFactory'
 * 			should be called for creating the target object.
 *  <li><b>some.package.SomeFactorySingleton#getFactoryMethod.createObject</b><br>
 *  	 	This defines that the 'getFactoryMethod' of the class 'some.package.SomeFactorySingleton'
 * 			should be called to retrieve the factory object on which the 'createObject' should
 * 			be called for creating the target object.<br/>
 * 			The factory retrieval method should never return <code>null</code>. This
 * 			will result in a {@link NullPointerException}.
 * </ol>
 *
 * @author <a href="mailto:maurice.zeijen@smies.com">maurice.zeijen@smies.com</a>
 */
@Alias("basic")
public class BasicFactoryDefinitionParser extends
        AbstractCachingFactoryDefinitionParser {


    /**
     * Parses the factory definition and creates a {@link StaticMethodFactory}
     * or {@link FactoryInstanceFactory#} accordingly.
     */
    @Override
    protected Factory<?> createFactory(String factoryDefinition) {
        String[] defParts = factoryDefinition.split("#");
        if (defParts.length == 2) {
            String className = defParts[0];
            String methodDef = defParts[1];
            try {
                String[] methodParts = methodDef.split("\\.");

                if (methodParts.length == 1) {
                    return createStaticMethodFactory(factoryDefinition, className, methodDef);
                } else if (methodParts.length == 2) {

                    String staticGetInstanceMethodDef = methodParts[0];
                    String factoryMethodDef = methodParts[1];

                    return createFactoryInstanceFactory(factoryDefinition, className, staticGetInstanceMethodDef, factoryMethodDef);

                } else {
                    throw createInvalidDefinitionException(factoryDefinition);
                }
            } catch (InvalidFactoryDefinitionException e) {
                throw e;
            } catch (Exception e) {
                throw new FactoryException("The factory could not be created from the definition '" + factoryDefinition + "'.", e);
            }

        } else {
            throw createInvalidDefinitionException(factoryDefinition);
        }
    }

    /**
     * Creates a StaticMethodFactory object.
     *
     * @param factoryDefinition
     * @param className
     * @param methodDef
     * @return
     * @throws ClassNotFoundException
     * @throws SecurityException
     * @throws NoSuchMethodException
     */
    protected Factory<?> createStaticMethodFactory(String factoryDefinition, String className, String methodDef) throws ClassNotFoundException, SecurityException, NoSuchMethodException {
        Class<?> factoryClass = ClassUtils.forName(className, this.getClass());
        Method factoryMethod = factoryClass.getMethod(methodDef);

        if (!Modifier.isStatic(factoryMethod.getModifiers())) {
            throw new NoSuchMethodException("No static method with the name '" + methodDef + "' can be found on the class '" + className + "' while processing the factory definition '" + factoryDefinition + "'.");
        }

        return new StaticMethodFactory(factoryDefinition, factoryMethod);
    }

    /**
     * Creates a FactoryInstanceFactory object.
     *
     * @param factoryDefinition
     * @param className
     * @param staticGetInstanceMethodDef
     * @param factoryMethodDef
     * @return
     * @throws ClassNotFoundException
     * @throws SecurityException
     * @throws NoSuchMethodException
     */
    protected Factory<?> createFactoryInstanceFactory(String factoryDefinition, String className, String staticGetInstanceMethodDef, String factoryMethodDef) throws ClassNotFoundException, SecurityException, NoSuchMethodException {
        Class<?> factoryClass = ClassUtils.forName(className, this.getClass());
        Method getInstanceMethod = factoryClass.getMethod(staticGetInstanceMethodDef);
        Class<?> factoryType = getInstanceMethod.getReturnType();
        Method factoryMethod = factoryType.getMethod(factoryMethodDef);

        if (!Modifier.isStatic(getInstanceMethod.getModifiers())) {
            throw new NoSuchMethodException("No static method with the name '" + staticGetInstanceMethodDef + "' can be found on the class '" + className + "'.");
        }

        return new FactoryInstanceFactory(factoryDefinition, getInstanceMethod, factoryMethod);
    }


    protected InvalidFactoryDefinitionException createInvalidDefinitionException(String factoryDefinition) {
        return new InvalidFactoryDefinitionException("The factory definition '" + factoryDefinition + "' " +
                "isn't valid. The definition is 'some.package.SomeFactory#createObject' or " +
                "'some.package.SomeFactorySingleton#getFactoryMethod.createObject'");
    }

    protected static String toClassDefinition(Method method) {
        return method.getDeclaringClass().getName() + "#" + method.getName() + "()";
    }

    /**
     * The StaticMethodFactory uses a static factory method create the target objects.
     */
    protected static class StaticMethodFactory implements Factory<Object> {

        protected final String factoryDefinition;

        protected final Method factoryMethod;

        public StaticMethodFactory(String factoryDefinition, Method factoryMethod) {
            this.factoryDefinition = factoryDefinition;
            this.factoryMethod = factoryMethod;
        }

        public Object create(ExecutionContext executionContext) {
            try {
                return factoryMethod.invoke(null);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new FactoryException("Could not invoke the static factory method '" + toClassDefinition(factoryMethod) + "' defined by the factory definition '" + factoryDefinition + "'");
            }
        }

        @Override
        public String toString() {
            return String.format("%s[factoryDefinition=%s,factoryMethod=%s]", this, factoryDefinition, factoryMethod);
        }
    }

    /**
     * The FactoryInstanceFactory uses a static method to retrieve the factory object and
     * then calls the factory method on the factory object to create the target objects.
     */
    protected static class FactoryInstanceFactory implements Factory<Object> {

        protected final String factoryDefinition;

        protected final Method getInstanceMethod;

        protected final Method factoryMethod;

        public FactoryInstanceFactory(String factoryDefinition, Method getInstanceMethod, Method factoryMethod) {
            this.factoryDefinition = factoryDefinition;
            this.getInstanceMethod = getInstanceMethod;
            this.factoryMethod = factoryMethod;
        }

        public Object create(ExecutionContext executionContext) {
            Object factoryObj;

            try {
                factoryObj = getInstanceMethod.invoke(null);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new FactoryException("Could not invoke the static method '" + toClassDefinition(getInstanceMethod) + "' to retrieve the factory defined by the factory definition '" + factoryDefinition + "'");
            }

            if (factoryObj == null) {
                throw new NullPointerException("The static method '" + toClassDefinition(getInstanceMethod) + "' returned null, which is not allowed");
            }

            try {
                return factoryMethod.invoke(factoryObj);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new FactoryException("Could not invoke the method '" + toClassDefinition(factoryMethod) + "' on the factory object '" + factoryObj + "'. This factory is defined by the factory definition '" + factoryDefinition + "'");
            }
        }

        @Override
        public String toString() {
            return String.format("%s[factoryDefinition=%s,factoryMethod=%s]", this, factoryDefinition, factoryMethod);
        }

    }

}
