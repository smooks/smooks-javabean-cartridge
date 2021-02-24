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

import org.apache.commons.lang.exception.ExceptionUtils;
import org.junit.Test;
import org.smooks.tck.MockExecutionContext;

import java.util.ArrayList;
import java.util.LinkedList;

import static org.junit.Assert.*;

public class BasicFactoryDefinitionParserTest {

        @Test
	public void test_create_StaticMethodFactory() {

		BasicFactoryDefinitionParser parser = new BasicFactoryDefinitionParser();

		Factory<?> factory = parser.parse("org.smooks.cartridges.javabean.TestFactory#newArrayList");

		Object result = factory.create(new MockExecutionContext());

		assertNotNull(result);
		assertTrue(result instanceof ArrayList<?>);

	}

        @Test
	public void test_create_FactoryInstanceFactory() {

		BasicFactoryDefinitionParser parser = new BasicFactoryDefinitionParser();

		Factory<?> factory = parser.parse("org.smooks.cartridges.javabean.TestFactory#newInstance.newLinkedList");

		Object result = factory.create(new MockExecutionContext());

		assertNotNull(result);
		assertTrue(result instanceof LinkedList<?>);

	}

        @Test
	public void test_caching() {

		BasicFactoryDefinitionParser parser = new BasicFactoryDefinitionParser();

		Factory<?> factory1 = parser.parse("org.smooks.cartridges.javabean.TestFactory#newArrayList");
		Factory<?> factory2 = parser.parse("org.smooks.cartridges.javabean.TestFactory#newArrayList");
		Factory<?> factory3 = parser.parse("org.smooks.cartridges.javabean.TestFactory#newInstance.newLinkedList");

		assertSame(factory1, factory2);
		assertNotSame(factory1, factory3);

	}

        @Test
	public void test_invalid_definition() {

		BasicFactoryDefinitionParser parser = new BasicFactoryDefinitionParser();

		InvalidFactoryDefinitionException exception = null;

		try {
			parser.parse("garbage");
		} catch (InvalidFactoryDefinitionException e) {
			exception = e;
		}

		if(exception == null) {
			fail("The parser didn't throw an exception");
		}

		assertTrue(exception.getMessage().contains("garbage"));
	}

        @Test
	public void test_null_factory() {

		BasicFactoryDefinitionParser parser = new BasicFactoryDefinitionParser();

		Factory<?> factory = parser.parse("org.smooks.cartridges.javabean.TestFactory#getNull.newLinkedList");

		NullPointerException exception = null;

		try {
			factory.create(new MockExecutionContext());
		} catch (NullPointerException e) {
			exception = e;
		}

		if(exception == null) {
			fail("The parser didn't throw an NullPointerException");
		}
	}

        @Test
	public void test_invalid_class() {

		BasicFactoryDefinitionParser parser = new BasicFactoryDefinitionParser();


		FactoryException exception = null;

		try {
			parser.parse("org.smooks.cartridges.javabean.DoesNotExist#newArrayList");
		} catch (FactoryException e) {
			exception = e;
		}

		if(exception == null) {
			fail("The parser didn't throw a FactoryException");
		}

		assertTrue(ExceptionUtils.indexOfThrowable(exception, ClassNotFoundException.class) >= 0);
	}

        @Test
	public void test_invalid_method() {

		BasicFactoryDefinitionParser parser = new BasicFactoryDefinitionParser();

		FactoryException exception = null;

		try {
			parser.parse("org.smooks.cartridges.javabean.TestFactory#doesNotExist");
		} catch (FactoryException e) {
			exception = e;
		}

		if(exception == null) {
			fail("The parser didn't throw a FactoryException");
		}

		assertTrue(ExceptionUtils.indexOfThrowable(exception, NoSuchMethodException.class) >= 0);
	}

        @Test
	public void test_not_static_method() {

		BasicFactoryDefinitionParser parser = new BasicFactoryDefinitionParser();

		FactoryException exception = null;

		try {
			parser.parse("org.smooks.cartridges.javabean.TestFactory#newLinkedList");
		} catch (FactoryException e) {
			exception = e;
		}

		if(exception == null) {
			fail("The parser didn't throw a FactoryException");
		}

		assertTrue(ExceptionUtils.indexOfThrowable(exception, NoSuchMethodException.class) >= 0);
		assertTrue(ExceptionUtils.getFullStackTrace(exception).contains("static"));
	}
}
