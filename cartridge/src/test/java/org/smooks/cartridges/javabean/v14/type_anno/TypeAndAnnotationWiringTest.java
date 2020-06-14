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
package org.smooks.cartridges.javabean.v14.type_anno;

import java.io.IOException;

import javax.xml.transform.stream.StreamSource;

import org.smooks.Smooks;
import org.smooks.SmooksException;
import org.smooks.cartridges.javabean.extendedconfig.ExtendedOrder;
import org.smooks.payload.JavaResult;
import org.xml.sax.SAXException;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class TypeAndAnnotationWiringTest {

    @Test
    public void test_by_type() throws IOException, SAXException {
        test("test_bean_01.xml");
    }

    @Test
    public void test_by_anno() throws IOException, SAXException {
        test("test_bean_02.xml");
    }

    @Test
    public void test_bad_vonfig() throws IOException, SAXException {
        try {
            test("test_bean_03.xml");
        } catch (SmooksException e) {
            assertEquals("One or more of attributes 'beanIdRef', 'beanType' and 'beanAnnotation' must be specified on a bean wiring configuration.", e.getCause().getMessage());
        }
    }

    public void test(String config) throws IOException, SAXException {
        Smooks smooks = new Smooks(getClass().getResourceAsStream(config));
        JavaResult result = new JavaResult();

        smooks.filterSource(new StreamSource(getClass().getResourceAsStream("order-01.xml")), result);

        ExtendedOrder order = (ExtendedOrder) result.getBean("order");

        assertEquals("[{productId: 111, quantity: 2, price: 8.9}, {productId: 222, quantity: 7, price: 5.2}]", order.getOrderItems().toString());
    }
}
