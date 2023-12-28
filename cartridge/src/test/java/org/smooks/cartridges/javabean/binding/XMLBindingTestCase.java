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
package org.smooks.cartridges.javabean.binding;

import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Test;
import org.smooks.Smooks;
import org.smooks.cartridges.javabean.binding.config5.Person;
import org.smooks.cartridges.javabean.binding.model.ModelSet;
import org.smooks.cartridges.javabean.binding.ordermodel.Order;
import org.smooks.cartridges.javabean.binding.xml.XMLBinding;
import org.smooks.support.StreamUtils;
import org.xml.sax.SAXException;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class XMLBindingTestCase {

    @Test
    public void test_no_namespaces() throws IOException, SAXException {
        test_pre_created_Smooks("config1");
        test_post_created_Smooks("config1");
    }

    @Test
    public void test_with_namespaces_01() throws IOException, SAXException {
        test_pre_created_Smooks("config2");
        test_post_created_Smooks("config2");
    }

    @Test
    public void test_with_namespaces_02() throws IOException, SAXException {
        test_pre_created_Smooks("config3");
        test_post_created_Smooks("config3");
    }

    @Test
    public void test_with_namespaces_03() throws IOException, SAXException {
        test_pre_created_Smooks("config4");
        test_post_created_Smooks("config4");
    }

    @Test
    public void test_Person_binding() throws IOException, SAXException {
        XMLBinding xmlBinding = new XMLBinding().add(getClass().getResourceAsStream("config5/person-binding-config.xml"));
        xmlBinding.initialise();

        Person person = xmlBinding.fromXML("<person name='Max' age='50' />", Person.class);
        String xml = xmlBinding.toXML(person);
        XMLUnit.setIgnoreWhitespace(true);
        XMLAssert.assertXMLEqual("<person name='Max' age='50' />", xml);

    }

    @Test
    public void test_MILYN629() throws IOException, SAXException {
        test_pre_created_Smooks("config6");
        test_post_created_Smooks("config6");
    }

    @Test
    public void test_add_fails_after_smooks_constructed() throws IOException, SAXException {
        Smooks smooks = new Smooks(getClass().getResourceAsStream("config1/order-binding-config.xml"));
        XMLBinding xmlBinding = new XMLBinding(smooks);

        try {
            xmlBinding.add("blah");
        } catch (IllegalStateException e) {
            assertEquals("Illegal call to method after all configurations have been added.", e.getMessage());
        }
    }

    private void test_pre_created_Smooks(String config) throws IOException, SAXException {
        String inputXML = StreamUtils.readStreamAsString(getClass().getResourceAsStream(config + "/order.xml"), "UTF-8");
        Smooks smooks = new Smooks(getClass().getResourceAsStream(config + "/order-binding-config.xml"));
        XMLBinding xmlBinding = new XMLBinding(smooks);
        xmlBinding.initialise();

        assertTrue("Should be a binding only config.", ModelSet.get(smooks.getApplicationContext()).isBindingOnlyConfig());

        test(inputXML, xmlBinding);
    }

    private void test_post_created_Smooks(String config) throws IOException, SAXException {
        String inputXML = StreamUtils.readStreamAsString(getClass().getResourceAsStream(config + "/order.xml"), "UTF-8");
        XMLBinding xmlBinding = new XMLBinding().add(getClass().getResourceAsStream(config + "/order-binding-config.xml"));
        xmlBinding.initialise();

        test(inputXML, xmlBinding);
    }

    private void test(String inputXML, XMLBinding xmlBinding) throws SAXException, IOException {
        // Read...
        Order order = xmlBinding.fromXML(inputXML, Order.class);

        assertEquals("Joe & Ray", order.getHeader().getCustomerName());

        // write...
        String outputXML = xmlBinding.toXML(order);

//        System.out.println(outputXML);

        // Compare...
        XMLUnit.setIgnoreWhitespace(true);
        XMLAssert.assertXMLEqual(inputXML, outputXML);
    }
}
