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
import org.smooks.api.bean.context.BeanContext;
import org.smooks.api.bean.lifecycle.BeanContextLifecycleEvent;
import org.smooks.api.bean.lifecycle.BeanContextLifecycleObserver;
import org.smooks.api.bean.lifecycle.BeanLifecycle;
import org.smooks.api.bean.repository.BeanId;
import org.smooks.cartridges.javabean.BeanInstancePopulator;

import java.lang.annotation.Annotation;

/**
 * {@link BeanContext} Observer performing bean wiring.
 * 
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class BeanWiringObserver implements BeanContextLifecycleObserver {

	private BeanId watchedBeanId;
	private Class<?> watchedBeanType;
	private Class<? extends Annotation> watchedBeanAnnotation;
	private BeanId watchingBeanId;
	private BeanInstancePopulator populator;

	public BeanWiringObserver(BeanId watchingBean, BeanInstancePopulator populator) {
		this.watchingBeanId = watchingBean;
		this.populator = populator;		
	}
	
	public BeanWiringObserver watchedBeanId(BeanId watchedBeanId) {
		this.watchedBeanId = watchedBeanId;
		return this;
	}

	public BeanWiringObserver watchedBeanType(Class watchedBeanType) {
		this.watchedBeanType = watchedBeanType;
		return this;
	}

	public BeanWiringObserver watchedBeanAnnotation(Class<? extends Annotation> watchedBeanAnnotation) {
		this.watchedBeanAnnotation = watchedBeanAnnotation;
		return this;
	}

	/* (non-Javadoc)
	 * @see org.smooks.cartridges.javabean.lifecycle.BeanContextLifecycleObserver#onBeanLifecycleEvent(org.smooks.cartridges.javabean.lifecycle.BeanContextLifecycleEvent)
	 */
	public void onBeanLifecycleEvent(BeanContextLifecycleEvent event) {
		BeanId beanId = event.getBeanId();
		BeanLifecycle lifecycle = event.getLifecycle();		
		
		if(lifecycle == BeanLifecycle.ADD) {
			if(watchedBeanId != null && beanId != watchedBeanId) {
				return;
			}

			Object bean = event.getBean();
			if(!isMatchingBean(bean, watchedBeanType, watchedBeanAnnotation)) {
				return;
			}
			
			ExecutionContext executionContext = event.getExecutionContext();
			populator.populateAndSetPropertyValue(bean, executionContext.getBeanContext(), watchingBeanId, executionContext, event.getSource());
		} else if(beanId == watchingBeanId && lifecycle == BeanLifecycle.REMOVE) {
			BeanContext beanContext = event.getExecutionContext().getBeanContext();
			
			beanContext.removeObserver(this);
			// Need to remove the watched bean from the bean context too because it's lifecycle is associated 
			// with the lifecycle of the watching bean, which has been removed...
			if(watchedBeanId != null) {
				beanContext.removeBean(watchedBeanId, event.getSource());
			}
		}
	}

	public static boolean isMatchingBean(Object bean, Class<?> type, Class<? extends Annotation> annotation) {
		Class<?> beanClass = bean.getClass();

		if (type != null && !type.isAssignableFrom(beanClass)) {
			return false;
		}
		return annotation == null || beanClass.isAnnotationPresent(annotation);
	}
}
