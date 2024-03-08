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
package org.smooks.cartridges.javabean.programatic;

import org.junit.jupiter.api.Test;
import org.smooks.Smooks;
import org.smooks.api.ExecutionContext;
import org.smooks.cartridges.javabean.Bean;
import org.smooks.cartridges.javabean.Header;
import org.smooks.cartridges.javabean.Order;
import org.smooks.cartridges.javabean.OrderItem;
import org.smooks.cartridges.javabean.factory.MVELFactory;
import org.smooks.engine.converter.StringToDoubleConverterFactory;
import org.smooks.engine.converter.StringToIntegerConverterFactory;
import org.smooks.io.payload.JavaResult;
import org.xml.sax.SAXException;

import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Programmatic Binding config test for the Bean class.
 *
 * @author <a href="mailto:tom.fennelly@jboss.com">tom.fennelly@jboss.com</a>
 */
@SuppressWarnings("unchecked")
public class ProgrammaticBeanConfigTestCase {

    @Test
    public void test_01_fluent() {
        Smooks smooks = new Smooks();
        Bean orderBean = new Bean(Order.class, "order", "/order", smooks.getApplicationContext().getRegistry());

        orderBean.bindTo("header",
                orderBean.newBean(Header.class, "/order")
                        .bindTo("order", orderBean)
                        .bindTo("customerNumber", "header/customer/@number")
                        .bindTo("customerName", "header/customer")
                        .bindTo("privatePerson", "header/privatePerson")
        ).bindTo("orderItems",
                orderBean.newBean(ArrayList.class, "/order")
                        .bindTo(orderBean.newBean(OrderItem.class, "order-item")
                                .bindTo("productId", "order-item/product")
                                .bindTo("quantity", "order-item/quantity")
                                .bindTo("price", "order-item/price"))
        ).bindTo("orderItems",
                orderBean.newBean(OrderItem[].class, "/order")
                        .bindTo(orderBean.newBean(OrderItem.class, "order-item")
                                .bindTo("productId", "order-item/product")
                                .bindTo("quantity", "order-item/quantity")
                                .bindTo("price", "order-item/price")));

        smooks.addVisitors(orderBean);

        execute_01_test(smooks);
    }

    @Test
    public void test_01_factory() {
        Smooks smooks = new Smooks();
        Bean orderBean = new Bean(Order.class, "order", "/order", executionContext -> new Order(), smooks.getApplicationContext().getRegistry());

        orderBean.bindTo("header",
                orderBean.newBean(Header.class, "/order")
                        .bindTo("order", orderBean)
                        .bindTo("customerNumber", "header/customer/@number")
                        .bindTo("customerName", "header/customer")
                        .bindTo("privatePerson", "header/privatePerson")
        ).bindTo("orderItems",
                orderBean.newBean(Collection.class, "/order", new MVELFactory<Collection>("new java.util.ArrayList()"))
                        .bindTo(orderBean.newBean(OrderItem.class, "order-item")
                                .bindTo("productId", "order-item/product")
                                .bindTo("quantity", "order-item/quantity")
                                .bindTo("price", "order-item/price"))
        ).bindTo("orderItems",
                orderBean.newBean(OrderItem[].class, "/order")
                        .bindTo(orderBean.newBean(OrderItem.class, "order-item")
                                .bindTo("productId", "order-item/product")
                                .bindTo("quantity", "order-item/quantity")
                                .bindTo("price", "order-item/price")));

        smooks.addVisitors(orderBean);

        execute_01_test(smooks);
    }

    @Test
    public void test_invalid_bindTo() {
        Smooks smooks = new Smooks();
        Bean orderBean = new Bean(Order.class, "order", "/order", smooks.getApplicationContext().getRegistry());

        smooks.addVisitors(orderBean);

        try {
            // invalid attempt to bindTo after it has been added to the Smooks instance...
            orderBean.bindTo("header",
                    orderBean.newBean(Header.class, "/order")
                            .bindTo("privatePerson", "header/privatePerson"));

            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            assertEquals("Unexpected attempt to bindTo Bean instance after the Bean instance has been added to a Smooks instance.", e.getMessage());
        }
    }

    @Test
    public void test_01_flat() {
        Smooks smooks = new Smooks();
        Bean orderBean = new Bean(Order.class, "order", "/order", smooks.getApplicationContext().getRegistry());
        Bean headerBean = new Bean(Header.class, "header", "/order", smooks.getApplicationContext().getRegistry())
                .bindTo("order", orderBean)
                .bindTo("customerNumber", "header/customer/@number")
                .bindTo("customerName", "header/customer")
                .bindTo("privatePerson", "header/privatePerson");

        orderBean.bindTo("header", headerBean);
        orderBean.bindTo("orderItems", orderBean.newBean(ArrayList.class, "/order")
                .bindTo(orderBean.newBean(OrderItem.class, "order-item")
                        .bindTo("productId", "order-item/product")
                        .bindTo("quantity", "order-item/quantity")
                        .bindTo("price", "order-item/price")));
        orderBean.bindTo("orderItems", orderBean.newBean(OrderItem[].class, "/order")
                .bindTo(orderBean.newBean(OrderItem.class, "order-item")
                        .bindTo("productId", "order-item/product")
                        .bindTo("quantity", "order-item/quantity")
                        .bindTo("price", "order-item/price")));

        smooks.addVisitors(orderBean);

        execute_01_test(smooks);
    }

    private void execute_01_test(Smooks smooks) {
        JavaResult result = new JavaResult();
        smooks.filterSource(new StreamSource(getClass().getResourceAsStream("/order-01.xml")), result);

        Order order = (Order) result.getBean("order");
        int identity = System.identityHashCode(order);

        assertEquals("Order:" + identity + "[header[null, 123123, Joe, false, Order:" + identity + "]\n" +
                "orderItems[[{productId: 111, quantity: 2, price: 8.9}, {productId: 222, quantity: 7, price: 5.2}]]\n" +
                "norderItemsArray[[{productId: 111, quantity: 2, price: 8.9}, {productId: 222, quantity: 7, price: 5.2}]]]", order.toString());
    }

    @Test
    public void test_02_Map_fluid() {
        Smooks smooks = new Smooks();

        Bean orderBean = new Bean(HashMap.class, "order", "/order", smooks.getApplicationContext().getRegistry());
        orderBean.bindTo("header",
                orderBean.newBean(HashMap.class, "/order")
                        .bindTo("customerNumber", "header/customer/@number", new StringToIntegerConverterFactory().createTypeConverter())
                        .bindTo("customerName", "header/customer")
                        .bindTo("privatePerson", "header/privatePerson")
        ).bindTo("orderItems",
                orderBean.newBean(ArrayList.class, "/order")
                        .bindTo(orderBean.newBean(HashMap.class, "order-item")
                                .bindTo("productId", "order-item/product")
                                .bindTo("quantity", "order-item/quantity")
                                .bindTo("price", "order-item/price", new StringToDoubleConverterFactory().createTypeConverter()))
        );

        smooks.addVisitors(orderBean);

        JavaResult result = new JavaResult();
        smooks.filterSource(new StreamSource(getClass().getResourceAsStream("/order-01.xml")), result);

        Map order = (Map) result.getBean("order");

        HashMap headerMap = (HashMap) order.get("header");
        assertEquals("Joe", headerMap.get("customerName"));
        assertEquals(123123, headerMap.get("customerNumber"));
        assertEquals("", headerMap.get("privatePerson"));

        ArrayList<HashMap> orderItems = (ArrayList<HashMap>) order.get("orderItems");
        for (HashMap orderItem : orderItems) {
            String quantity = (String) orderItem.get("quantity");
            if (quantity.equals("2")) {
                assertEquals("111", orderItem.get("productId"));
                assertEquals(8.9, orderItem.get("price"));
            } else {
                assertEquals("222", orderItem.get("productId"));
                assertEquals(5.2, orderItem.get("price"));
            }
        }
    }

    @Test
    public void test_02_arrays_programmatic() {
        Smooks smooks = new Smooks();

        Bean orderBean = new Bean(Order.class, "order", "order", smooks.getApplicationContext().getRegistry());
        Bean orderItemArray = new Bean(OrderItem[].class, "orderItemsArray", "order", smooks.getApplicationContext().getRegistry());
        Bean orderItem = new Bean(OrderItem.class, "orderItem", "order-item", smooks.getApplicationContext().getRegistry());

        orderItem.bindTo("productId", "order-item/product");
        orderItemArray.bindTo(orderItem);
        orderBean.bindTo("orderItems", orderItemArray);

        smooks.addVisitors(orderBean);

        execSmooksArrays(smooks);
    }

    @Test
    public void test_02_arrays_xml() throws IOException, SAXException {
        Smooks smooks = new Smooks(getClass().getResourceAsStream("xmlconfig_01.xml"));
        execSmooksArrays(smooks);
    }

    private void execSmooksArrays(Smooks smooks) {
        JavaResult result = new JavaResult();
        ExecutionContext execContext = smooks.createExecutionContext();

        smooks.filterSource(execContext, new StreamSource(getClass().getResourceAsStream("order-01.xml")), result);

        Order order = (Order) result.getBean("order");
        int identity = System.identityHashCode(order);

        assertEquals("Order:" + identity + "[header[null]\n" +
                "orderItems[null]\n" +
                "norderItemsArray[[{productId: 111, quantity: null, price: null}, {productId: 222, quantity: null, price: null}]]]", order.toString());
    }
}
