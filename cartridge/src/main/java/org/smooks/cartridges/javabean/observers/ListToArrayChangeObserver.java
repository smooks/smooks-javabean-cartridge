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
package org.smooks.cartridges.javabean.observers;

import org.smooks.api.ExecutionContext;
import org.smooks.api.bean.lifecycle.BeanContextLifecycleEvent;
import org.smooks.api.bean.lifecycle.BeanContextLifecycleObserver;
import org.smooks.api.bean.lifecycle.BeanLifecycle;
import org.smooks.api.bean.repository.BeanId;
import org.smooks.cartridges.javabean.BeanInstancePopulator;

/**
 * List to array change event listener.
 * <p/>
 * Arrays start out their lives as Lists.  When the list is populated with all
 * wired in object entries, the List is converted to an Array.  This observer listens
 * for that event and triggers the wiring of the array into the target bean.
 *
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class ListToArrayChangeObserver implements BeanContextLifecycleObserver {

    private String property;
    private BeanInstancePopulator populator;
    private BeanId watchedBean;

    public ListToArrayChangeObserver(BeanId watchedBean, String property, BeanInstancePopulator populator) {
        this.watchedBean = watchedBean;
        this.property = property;
        this.populator = populator;
    }

    /* (non-Javadoc)
     * @see org.smooks.cartridges.javabean.lifecycle.BeanContextLifecycleObserver#onBeanLifecycleEvent(org.smooks.cartridges.javabean.lifecycle.BeanContextLifecycleEvent)
     */
    public void onBeanLifecycleEvent(BeanContextLifecycleEvent event) {
        if (event.getBeanId() == watchedBean && event.getLifecycle() == BeanLifecycle.CHANGE) {
            ExecutionContext executionContext = event.getExecutionContext();

            // Set the array on the object, via the populator...
            populator.setPropertyValue(property, event.getBean(), executionContext, event.getSource());
            // Remove this observer...
            executionContext.getBeanContext().removeObserver(this);
        }
    }
}
