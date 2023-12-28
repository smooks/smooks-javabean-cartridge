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
package org.smooks.cartridges.javabean.JIRA.MILYN_356;

import org.junit.Test;
import org.smooks.Smooks;
import org.smooks.cartridges.javabean.OrderItem;
import org.smooks.io.payload.JavaResult;
import org.smooks.io.payload.StringSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class MILYN_356_TestCase {

    @Test
    public void test_decoder_defined() throws IOException, SAXException {
        Smooks smooks = new Smooks(getClass().getResourceAsStream("config_01.xml"));
        JavaResult javaResult = new JavaResult();

        smooks.filterSource(new StringSource("<price>123'456,00</price>"), javaResult);

        OrderItem orderItem = (OrderItem) javaResult.getBean("orderItem");
        assertEquals(123456.00D, orderItem.getPrice(), 0D);

        BigDecimal baseBigD = (BigDecimal) javaResult.getBean("price");
        assertEquals(new BigDecimal("123456.00"), baseBigD);
    }

    @Test
    public void test_decoder_undefined() throws IOException, SAXException {
        Smooks smooks = new Smooks(getClass().getResourceAsStream("config_02.xml"));
        JavaResult javaResult = new JavaResult();

        smooks.filterSource(new StringSource("<price>123'456,00</price>"), javaResult);

        OrderItem orderItem = (OrderItem) javaResult.getBean("orderItem");
        assertEquals(123456.00D, orderItem.getPrice(), 0D);
    }

}
