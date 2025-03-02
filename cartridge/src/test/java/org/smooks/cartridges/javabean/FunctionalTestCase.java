/*-
 * ========================LICENSE_START=================================
 * smooks-javabean-cartridge
 * %%
 * Copyright (C) 2020 - 2025 Smooks
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

import org.junit.jupiter.api.Test;
import org.smooks.Smooks;
import org.smooks.api.ExecutionContext;
import org.smooks.cartridges.javabean.jaxb.model.POType;
import org.smooks.io.sink.JavaSink;
import org.smooks.io.sink.StringSink;
import org.smooks.io.source.JavaSource;
import org.smooks.io.source.StreamSource;
import org.smooks.io.source.StringSource;
import org.smooks.support.StreamUtils;
import org.xml.sax.SAXException;
import org.xmlunit.builder.DiffBuilder;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

public class FunctionalTestCase {

    public static class Header {
        private double netAmount;
        private BigDecimal netAmountObj;
        private double totalAmount;
        private double tax;

        public double getNetAmount() {
            return netAmount;
        }

        public void setNetAmount(double netAmount) {
            this.netAmount = netAmount;
        }

        public BigDecimal getNetAmountObj() {
            return netAmountObj;
        }

        public void setNetAmountObj(BigDecimal netAmountObj) {
            this.netAmountObj = netAmountObj;
        }

        public double getTotalAmount() {
            return totalAmount;
        }

        public void setTotalAmount(double totalAmount) {
            this.totalAmount = totalAmount;
        }

        public double getTax() {
            return tax;
        }

        public void setTax(double tax) {
            this.tax = tax;
        }
    }

    public static class OrderItem {
        private Integer quantity = 0;
        private Double price = 0.0;

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }

        public Double getPrice() {
            return price;
        }

        public void setPrice(Double price) {
            this.price = price;
        }
    }

    public static class X {

        private String val1;
        private int val2;
        private Double val3;

        public String getVal1() {
            return val1;
        }

        public void setVal1(String val1) {
            this.val1 = val1;
        }

        public int getVal2() {
            return val2;
        }

        public void setVal2(int val2) {
            this.val2 = val2;
        }

        public Double getVal3() {
            return val3;
        }

        public void setVal3(Double val3) {
            this.val3 = val3;
        }
    }

    public static class RootObject {

        private List<TheEnumContainer> enums = new ArrayList<TheEnumContainer>();

        public List<TheEnumContainer> getEnums() {
            return enums;
        }

        public void setEnums(List<TheEnumContainer> enums) {
            this.enums = enums;
        }
    }

    public static class TheEnumContainer {

        private TheEnum theEnum = TheEnum.FIRST;

        public TheEnum getTheEnum() {
            return theEnum;
        }

        public void setTheEnum(TheEnum theEnum) {
            this.theEnum = theEnum;
        }
    }

    public enum TheEnum {
        FIRST
    }

    /**
     * test BeanValueBinder does not try to access accumulated SAXText for expression bindings
     */
    @Test
    public void testExpressionBinding() throws IOException, SAXException {
        Smooks smooks = new Smooks(getClass().getResourceAsStream("smooks.xml"));
        JavaSource source = new JavaSource("x", "XValObject");
        JavaSink sink = new JavaSink();

        smooks.filterSource(source, sink);

        assertEquals("{xxxx=XValObject}", sink.getBean("bean").toString());
    }

    @Test
    public void testDecodeParamWithDecoder() throws IOException, SAXException {
        Smooks smooks = new Smooks(getClass().getResourceAsStream("config_01.xml"));
        JavaSink javaSink = new JavaSink();

        smooks.filterSource(new StringSource("<price>123'456,00</price>"), javaSink);

        org.smooks.cartridges.javabean.OrderItem orderItem = (org.smooks.cartridges.javabean.OrderItem) javaSink.getBean("orderItem");
        assertEquals(123456.00D, orderItem.getPrice(), 0D);

        BigDecimal baseBigD = (BigDecimal) javaSink.getBean("price");
        assertEquals(new BigDecimal("123456.00"), baseBigD);
    }

    @Test
    public void testDecodeParamWithoutDecoder() throws IOException, SAXException {
        Smooks smooks = new Smooks(getClass().getResourceAsStream("config_02.xml"));
        JavaSink javaSink = new JavaSink();

        smooks.filterSource(new StringSource("<price>123'456,00</price>"), javaSink);

        org.smooks.cartridges.javabean.OrderItem orderItem = (org.smooks.cartridges.javabean.OrderItem) javaSink.getBean("orderItem");
        assertEquals(123456.00D, orderItem.getPrice(), 0D);
    }

    @Test
    public void testInitValAttribute() throws IOException, SAXException {
        try (Smooks smooks = new Smooks(getClass().getResourceAsStream("config.xml"))) {
            StringSink stringSink = new StringSink();
            JavaSink javaSink = new JavaSink();

            smooks.filterSource(new StreamSource<>(getClass().getResourceAsStream("order.xml")), stringSink, javaSink);

            Header bean = (Header) javaSink.getBean("header");

            // Truncate to avoid rounding differences etc...
            assertEquals(81, (int) bean.getNetAmount());
            assertEquals(18, bean.getNetAmountObj().intValue());
            assertEquals(16, (int) bean.getTax());
            assertEquals(98, (int) bean.getTotalAmount());
        }
    }

    @Test
    public void testBeanValueBinderWithNamespaceInDataAttributeUsingXmlApi() throws IOException, SAXException {
        Smooks smooks = new Smooks(getClass().getResourceAsStream("smooks-config.xml"));
        JavaSink sink = new JavaSink();
        smooks.filterSource(new StreamSource<>(getClass().getResourceAsStream("message-2.xml")), sink);

        Map theBean = (Map) sink.getBean("theBean");
        assertEquals("xxx", theBean.get("attr1"));
        assertNull(theBean.get("attr2"));
    }

    @Test
    public void testBeanValueBinderWithNamespaceInDataSelectorUsingJavaApi() {
        Smooks smooks = new Smooks();

        Properties namespaces = new Properties();
        namespaces.setProperty("e", "http://www.example.net");
        namespaces.setProperty("f", "http://www.blah");
        smooks.setNamespaces(namespaces);

        Bean bean = new Bean(HashMap.class, "theBean", smooks.getApplicationContext().getRegistry());
        bean.bindTo("attr1", "test1/@e:attr1");
        bean.bindTo("attr2", "test1/@f:attr2");
        smooks.addVisitors(bean);

        JavaSink sink = new JavaSink();
        smooks.filterSource(new StreamSource<>(getClass().getResourceAsStream("message-2.xml")), sink);

        Map theBean = (Map) sink.getBean("theBean");
        assertEquals("xxx", theBean.get("attr1"));
        assertNull(theBean.get("attr2"));
    }

    @Test
    public void testBeanValueBinderDefaultAttribute() throws IOException, SAXException {
        Smooks smooks = new Smooks(getClass().getResourceAsStream("smooks-config-2.xml"));
        JavaSink javaSink = new JavaSink();

        smooks.filterSource(new StreamSource<>(getClass().getResourceAsStream("message.xml")), javaSink);

        X x = javaSink.getBean(X.class);
        assertEquals("456", x.getVal1()); // default will be overridden by value in the message
        assertEquals(987, x.getVal2()); // default will be applied
        assertEquals(99.65d, x.getVal3(), 0d); // default will be applied
    }

    @Test
    public void testJavaSinkIsAttachedToManuallyCreatedExecutionContext() throws IOException, SAXException {
        Smooks smooks = new Smooks(getClass().getResourceAsStream("smooks-config-3.xml"));
        ExecutionContext executionContext = smooks.createExecutionContext();
        JavaSink javaSink = new JavaSink();

        smooks.filterSource(executionContext, new StreamSource<>(getClass().getResourceAsStream("message-2.xml")), javaSink);
        assertEquals(3, javaSink.getResultMap().size());
    }

    @Test
    public void testRepeatedEnumsInJavaSource() throws IOException {
        RootObject rootObj = new RootObject();

        rootObj.getEnums().add(new TheEnumContainer());
        rootObj.getEnums().add(new TheEnumContainer());

        Smooks smooks = new Smooks();
        StringSink stringSink = new StringSink();

        smooks.filterSource(new JavaSource(rootObj), stringSink);

        String expected = StreamUtils.readStreamAsString(getClass().getResourceAsStream("expected.xml"), "UTF-8");
        assertFalse(DiffBuilder.compare(expected).
                withTest(stringSink.getResult()).
                ignoreComments().
                ignoreWhitespace().
                build().
                hasDifferences());
    }

    @Test
    public void testJaxbBinding() throws IOException, SAXException {
        Smooks smooks = new Smooks(getClass().getResourceAsStream("jaxb-config.xml"));

        JavaSink javaSink = new JavaSink();
        smooks.filterSource(new StringSource(StreamUtils.readStreamAsString(getClass().getResourceAsStream("jaxb/po.xml"), "UTF-8")), javaSink);

        POType poType = (POType) javaSink.getBean("purchaseOrder");
        assertEquals("1999-10-20", poType.getOrderDate().toString());
        assertEquals("US", poType.getBillTo().getCountry());
        assertEquals("8 Oak Avenue", poType.getBillTo().getStreet());
        assertEquals(3, poType.getItems().getItem().size());
        assertEquals("The Mummy (1959)", poType.getItems().getItem().get(1).getProductName());
    }
}
