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
package org.smooks.cartridges.javabean.dynamic;

import org.smooks.api.SmooksException;
import org.smooks.cartridges.javabean.dynamic.serialize.DefaultNamespace;

/**
 * Bean Registration Exception.
 * <p/>
 * <b>See factory methods.</b>.
 *
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class BeanRegistrationException extends SmooksException {

    /**
     * Private constructor.
     * @param message Exception message.
     */
    private BeanRegistrationException(String message) {
        super(message);
    }

    /**
     * Throw a {@link BeanRegistrationException} exception for the specified "unregistered" bean instance.
     * <p/>
     * This exception is thrown when one of the root bean instances for a namespace used within
     * a model doesn't have registered {@link BeanMetadata}
     * (via the {@link Model#registerBean(Object)}).
     *
     * @param bean The unknown bean instance.
     * @throws BeanRegistrationException The exception.
     */
    public static void throwUnregisteredBeanInstanceException(Object bean) throws BeanRegistrationException {
        throw new BeanRegistrationException("No BeanMetaData is registered for the specified bean instance.  Bean type '" + bean.getClass().getName() + "'.  All namespace 'root' bean instances in the Model must have registered BeanMetaData via the 'Model.registerBean(Object)' method.");
    }

    /**
     * Throw a {@link BeanRegistrationException} exception for the specified bean instance that
     * is already registered.
     *
     * @param bean The bean instance.
     * @throws BeanRegistrationException The exception.
     */
    public static void throwBeanInstanceAlreadyRegisteredException(Object bean) throws BeanRegistrationException {
        throw new BeanRegistrationException("The specified bean instance is already registered with the model.  Bean type '" + bean.getClass().getName() + "'.");
    }

    /**
     * Throw a {@link BeanRegistrationException} exception for a bean that is not annotated with the
     * {@link DefaultNamespace} annotation.
     * <p/>
     * All namespace root bean types must be annotated with the {@link DefaultNamespace}
     * annotation.
     *
     * @param bean The bean instance.
     * @throws BeanRegistrationException The exception.
     */
    public static void throwBeanNotAnnotatedWithDefaultNamespace(Object bean) throws BeanRegistrationException {
        throw new BeanRegistrationException("Bean type '" + bean.getClass().getName() + "' cannot be registered via Model.registerBean(Object) because it's not annotated with the @DefaultNamespace annotation.");
    }
}
