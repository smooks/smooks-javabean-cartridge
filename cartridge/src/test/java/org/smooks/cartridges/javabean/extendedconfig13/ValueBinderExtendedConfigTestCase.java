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
package org.smooks.cartridges.javabean.extendedconfig13;

import org.junit.Test;
import org.smooks.Smooks;
import org.smooks.api.ExecutionContext;
import org.smooks.io.payload.JavaResult;
import org.smooks.support.ClassUtil;
import org.xml.sax.SAXException;

import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ValueBinderExtendedConfigTestCase {

	@Test
	public void test_01() throws IOException, SAXException {
		Smooks smooks = new Smooks(getClass().getResourceAsStream("test_value_01.xml"));
		JavaResult result = new JavaResult();
		ExecutionContext execContext = smooks.createExecutionContext();

		//execContext.setEventListener(new HtmlReportGenerator("target/report/ValueBinderExtendedConfigTest-report.html"));

		smooks.filterSource(execContext, new StreamSource(getInput("order-01.xml")), result);

		assertEquals("Joe", result.getBean("customerName"));
		assertEquals(123123, result.getBean("customerNumber"));
		assertEquals(Boolean.TRUE, result.getBean("privatePerson"));
		assertEquals(1163616328000L, ((Date) result.getBean("date")).getTime());

		assertNull(result.getBean("product"));
	}

	@Test
	public void test_01_other() throws IOException, SAXException {
		Smooks smooks = new Smooks(getClass().getResourceAsStream("test_value_01.xml"));

		JavaResult result = new JavaResult();
		ExecutionContext execContext = smooks.createExecutionContext("other");

		smooks.filterSource(execContext, new StreamSource(getInput("order-01.xml")), result);

		assertEquals(222, result.getBean("product"));
	}


	/**
	 * @return
	 */
	private InputStream getInput(String file) {
		return ClassUtil.getResourceAsStream("/org/smooks/cartridges/javabean/extendedconfig/" + file, this.getClass());
	}
}
