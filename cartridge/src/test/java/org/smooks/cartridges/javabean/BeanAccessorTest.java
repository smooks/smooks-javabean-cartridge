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

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import org.smooks.container.ExecutionContext;
import org.smooks.container.MockExecutionContext;
import org.smooks.javabean.context.BeanIdStore;

/**
 *
 * @author tfennelly
 */
@SuppressWarnings("deprecation")
public class BeanAccessorTest {

	private ExecutionContext executionContext;

	/**
	 * Tests adding a bean
	 */
        @Test
	public void test_add_bean() {
        Object bean1 = new MyGoodBean();
        Object bean2 = new MyGoodBean();

        getBeanIdStore().register("bean1");
        getBeanIdStore().register("bean2");

        assertNull(BeanAccessor.getBean(executionContext, "bean1"));
        assertNull(BeanAccessor.getBean(executionContext, "bean2"));

        BeanAccessor.addBean(executionContext, "bean1", bean1);
        BeanAccessor.addBean(executionContext, "bean2", bean2);

        assertEquals(bean1, BeanAccessor.getBean(executionContext, "bean1"));
        assertEquals(bean2, BeanAccessor.getBean(executionContext, "bean2"));

        assertEquals(2, BeanAccessor.getBeanMap(executionContext).size());
        assertEquals(bean1, BeanAccessor.getBeanMap(executionContext).get("bean1"));
        assertEquals(bean2, BeanAccessor.getBeanMap(executionContext).get("bean2"));
    }

	/**
	 * Test adding and replacing a bean
	 */
        @Test
	public void test_add_and_replace_bean() {
        Object bean1 = new MyGoodBean();
        Object newBean1 = new MyGoodBean();

        getBeanIdStore().register("bean1");

        assertNull(BeanAccessor.getBean(executionContext, "bean1"));

        BeanAccessor.addBean(executionContext, "bean1", bean1);

        assertEquals(bean1, BeanAccessor.getBean(executionContext, "bean1"));

        BeanAccessor.addBean(executionContext, "bean1", newBean1);

        assertEquals(newBean1, BeanAccessor.getBean(executionContext, "bean1"));
    }

	/**
	 * Test adding and replacing a bean
	 */
        @Test
	public void test_change_bean() {
        Object bean1 = new MyGoodBean();
        Object newBean1 = new MyGoodBean();

        getBeanIdStore().register("bean1");
        getBeanIdStore().register("notExisting");

        BeanAccessor.addBean(executionContext, "bean1", bean1);

        assertEquals(bean1, BeanAccessor.getBean(executionContext, "bean1"));

        BeanAccessor.changeBean(executionContext, "bean1", newBean1);

        assertEquals(newBean1, BeanAccessor.getBean(executionContext, "bean1"));

        boolean fired = false;

        try {
        	BeanAccessor.changeBean(executionContext, "notExisting", new Object());
        } catch (IllegalStateException e) {
        	fired = true;
		}
        assertTrue(fired);
    }

        @Before
	public void setUp() throws Exception {
		executionContext = new MockExecutionContext();
	}

    /**
	 *
	 */
	private BeanIdStore getBeanIdStore() {
        return executionContext.getContext().getBeanIdStore();
	}
}
