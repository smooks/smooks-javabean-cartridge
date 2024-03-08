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

import org.junit.jupiter.api.Test;
import org.smooks.api.ExecutionContext;
import org.smooks.tck.MockExecutionContext;

import java.lang.reflect.InvocationTargetException;

public class PerfTestCase {

	private static boolean DISABLED = true;

	private static final int PARSE_COUNT = 1000;

	private static final int INVOKE_COUNT = 1000000;

    @Test
    public void test_parse_basic() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InterruptedException {
    	if(DISABLED) return;

    	ExecutionContext executionContext = new MockExecutionContext();

        loopParseBasic(10000, executionContext);

        Thread.sleep(1000);

        long start = System.currentTimeMillis();
        loopParseBasic(PARSE_COUNT, executionContext);
        System.out.println("Basic Parser Time: " + (System.currentTimeMillis() - start));
    }

    @Test
    public void test_invoke_static_basic() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InterruptedException {
    	if(DISABLED) return;

    	ExecutionContext executionContext = new MockExecutionContext();

    	BasicFactoryDefinitionParser parser = new BasicFactoryDefinitionParser();
        Factory<?> factory = parser.parse("org.smooks.cartridges.javabean.TestFactory#newArrayList");

        loopInvoke(10000, factory, executionContext);

        Thread.sleep(1000);

        long start = System.currentTimeMillis();
        loopInvoke(INVOKE_COUNT, factory, executionContext);
        System.out.println("Basic Invoke Static factory Time: " + (System.currentTimeMillis() - start));
    }

    @Test
    public void test_invoke_instance_basic() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InterruptedException {
    	if(DISABLED) return;

    	ExecutionContext executionContext = new MockExecutionContext();

    	BasicFactoryDefinitionParser parser = new BasicFactoryDefinitionParser();
        Factory<?> factory = parser.parse("org.smooks.cartridges.javabean.TestFactory#newInstance.newLinkedList");

        loopInvoke(10000, factory, executionContext);

        Thread.sleep(1000);

        long start = System.currentTimeMillis();
        loopInvoke(INVOKE_COUNT, factory, executionContext);
        System.out.println("Basic Invoke instance factory Time: " + (System.currentTimeMillis() - start));
    }


        @Test
	public void test_parse_MVEL() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InterruptedException {
		if(DISABLED) return;

		ExecutionContext executionContext = new MockExecutionContext();

        loopParseMVEL(10000, executionContext);

        Thread.sleep(1000);

        long start = System.currentTimeMillis();
        loopParseMVEL(PARSE_COUNT, executionContext);
        System.out.println("MVEL Parser Time: " + (System.currentTimeMillis() - start));
    }

        @Test
	public void test_invoke_static_MVEL() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InterruptedException {
		if(DISABLED) return;

		ExecutionContext executionContext = new MockExecutionContext();

    	MVELFactoryDefinitionParser parser = new MVELFactoryDefinitionParser();
        Factory<?> factory = parser.parse("org.smooks.cartridges.javabean.TestFactory.newArrayList()");

        loopInvoke(10000, factory, executionContext);

        Thread.sleep(1000);

        long start = System.currentTimeMillis();
        loopInvoke(INVOKE_COUNT, factory, executionContext);

        System.out.println("MVEL Invoke static factory Time: " + (System.currentTimeMillis() - start));
    }

        @Test
	public void test_invoke_instance_MVEL() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InterruptedException {
		if(DISABLED) return;

		ExecutionContext executionContext = new MockExecutionContext();

    	MVELFactoryDefinitionParser parser = new MVELFactoryDefinitionParser();
        Factory<?> factory = parser.parse("org.smooks.cartridges.javabean.TestFactory.newInstance().newLinkedList()");

        loopInvoke(10000, factory, executionContext);

        Thread.sleep(1000);

        long start = System.currentTimeMillis();
        loopInvoke(INVOKE_COUNT, factory, executionContext);

        System.out.println("MVEL Invoke instance factory Time: " + (System.currentTimeMillis() - start));
    }

	private void loopParseBasic(int count, ExecutionContext executionContext) {
		for(int i = 0; i < count; i++) {
			BasicFactoryDefinitionParser parser = new BasicFactoryDefinitionParser();
	        parser.parse("org.smooks.cartridges.javabean.TestFactory#newInstance.newLinkedList");
        }
	}

	private void loopParseMVEL(int count, ExecutionContext executionContext) {
		for(int i = 0; i < count; i++) {
            MVELFactoryDefinitionParser parser = new MVELFactoryDefinitionParser();
            parser.parse("org.smooks.cartridges.javabean.TestFactory.newInstance().newLinkedList()");
        }
	}

	private void loopInvoke(int count, Factory<?> factory, ExecutionContext executionContext) {
		for(int i = 0; i < count; i++) {
			factory.create(executionContext);
        }
	}

}
