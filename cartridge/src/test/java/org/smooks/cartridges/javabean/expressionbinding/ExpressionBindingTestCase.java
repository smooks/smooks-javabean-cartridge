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
package org.smooks.cartridges.javabean.expressionbinding;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smooks.Smooks;
import org.smooks.api.ExecutionContext;
import org.smooks.io.payload.JavaResult;

import javax.xml.transform.stream.StreamSource;
import java.util.Date;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class ExpressionBindingTestCase {

	private static final Logger LOGGER = LoggerFactory.getLogger(ExpressionBindingTestCase.class);

    @Test
    public void test_data_variable() throws Exception {
    	Smooks smooks = new Smooks(getClass().getResourceAsStream("02_binding.xml"));

    	JavaResult result = new JavaResult();

    	ExecutionContext context = smooks.createExecutionContext();
    	//context.setEventListener(new HtmlReportGenerator("target/expression_data_variable.html"));

    	smooks.filterSource(context, new StreamSource(getClass().getResourceAsStream("02_number.xml")), result);

    	Total total = (Total) result.getBean("total");

    	assertEquals(20, (int) total.getTotal());
    	assertEquals("10,20,30,40", total.getCsv());

    }

    private void assertDateValue(JavaResult result, String beanId) {
        Map<?, ?> message = (Map<?, ?>) result.getBean(beanId);
        Date messageDate = (Date) message.get("date");
        LOGGER.debug("Date: " + messageDate);
        assertEquals(946143900000L, messageDate.getTime());
    }
}
