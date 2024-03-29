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
package org.smooks.cartridges.javabean.dynamic;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.smooks.support.StreamUtils;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xmlunit.builder.DiffBuilder;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class ModelBuilderTestCase {

    public static final String NS_DESCRIPTOR = "META-INF/services/org/smooks/cartridges/javabean/dynamic/ns-descriptors.properties";

    @Test
    public void test_1_schema() throws SAXException, IOException {
        ModelBuilder builder = new ModelBuilder(NS_DESCRIPTOR, true);

        Model<AAA> model = builder.readModel(getClass().getResourceAsStream("/org/smooks/cartridges/javabean/dynamic/aaa-message.xml"), AAA.class);
        AAA aaa = model.getModelRoot();
        assertEquals(1234.98765, aaa.getDoubleProperty(), 0d);
        assertEquals("http://www.acme.com/xsd/aaa.xsd", model.getBeanMetadata(aaa).getNamespace());

        aaa = builder.readObject(getClass().getResourceAsStream("/org/smooks/cartridges/javabean/dynamic/aaa-message.xml"), AAA.class);
        assertEquals(1234.98765, aaa.getDoubleProperty(), 0d);
    }

    @Test
    public void test_2_schema_with_validation_1() throws SAXException, IOException {
        test_2_schema(new ModelBuilder(NS_DESCRIPTOR, true), "bbb-message.xml");
    }

    @Test
    public void test_2_schema_with_validation_2() throws SAXException, IOException {
        try {
            test_2_schema(new ModelBuilder(NS_DESCRIPTOR, true), "bbb-message-invalid.xml");
            fail("Expected SAXParseException");
        } catch (SAXParseException e) {
            assertTrue(e.getMessage().contains("Invalid content was found starting with element '{\"http://boohoo.com\":ddd}'"));
        }
    }

    @Test
    public void test_2_schema_without_validation() throws SAXException, IOException {
        test_2_schema(new ModelBuilder(NS_DESCRIPTOR, false), "bbb-message-invalid.xml");
    }

    private void test_2_schema(ModelBuilder builder, String message) throws SAXException, IOException {
        Model<BBB> model = builder.readModel(getClass().getResourceAsStream("/org/smooks/cartridges/javabean/dynamic/" + message), BBB.class);
        BBB bbb = model.getModelRoot();
        assertEquals(1234, bbb.getFloatProperty(), 1.0);

        assertEquals("http://www.acme.com/xsd/bbb.xsd", model.getBeanMetadata(bbb).getNamespace());
        List<AAA> aaas = bbb.getAaas();
        assertEquals(3, aaas.size());
        assertEquals("http://www.acme.com/xsd/aaa.xsd", model.getBeanMetadata(aaas.get(0)).getNamespace());

        bbb = builder.readObject(getClass().getResourceAsStream(message), BBB.class);
        assertEquals(1234, bbb.getFloatProperty(), 1.0);

        aaas = bbb.getAaas();
        assertEquals(1234.98765, aaas.get(0).getDoubleProperty(), 0d);

        StringWriter writer = new StringWriter();
        model.writeModel(writer);
        assertFalse(DiffBuilder.compare(StreamUtils.readStreamAsString(getClass().getResourceAsStream(message), "UTF-8")).
                withTest(writer.toString()).
                ignoreWhitespace().
                build().
                hasDifferences());
    }

    @Test
    public void test_build_model() throws IOException, SAXException {
        ModelBuilder builder = new ModelBuilder(NS_DESCRIPTOR, false);
        BBB bbb = new BBB();
        List<AAA> aaas = new ArrayList<AAA>();
        Model<BBB> model = new Model<BBB>(bbb, builder);

        bbb.setFloatProperty(1234.87f);
        bbb.setAaas(aaas);

        aaas.add(new AAA());
        aaas.get(0).setDoubleProperty(1234.98765d);
        aaas.get(0).setIntProperty((double) 123);
        model.registerBean(aaas.get(0));
        aaas.add(new AAA());
        aaas.get(1).setDoubleProperty(2234.98765d);
        aaas.get(1).setIntProperty((double) 223);
        model.registerBean(aaas.get(1));
        aaas.add(new AAA());
        aaas.get(2).setDoubleProperty(3234.98765d);
        aaas.get(2).setIntProperty((double) 323);
        model.registerBean(aaas.get(2));

        StringWriter writer = new StringWriter();
        model.writeModel(writer);
        assertFalse(DiffBuilder.compare(StreamUtils.readStreamAsString(getClass().getResourceAsStream("bbb-message.xml"), "UTF-8")).
                withTest(writer.toString()).
                ignoreWhitespace().
                build().
                hasDifferences());
    }

    @BeforeEach
    public void setUp() throws Exception {
        Locale.setDefault(new Locale("en", "IE"));
    }

}