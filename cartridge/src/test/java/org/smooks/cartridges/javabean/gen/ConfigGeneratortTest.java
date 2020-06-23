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
package org.smooks.cartridges.javabean.gen;

import org.junit.Test;
import org.smooks.cartridges.javabean.Order;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class ConfigGeneratortTest {

    @Test
    public void test() throws ClassNotFoundException, java.io.IOException {
        java.util.Properties properties = new java.util.Properties();
        java.io.StringWriter writer = new java.io.StringWriter();

        properties.setProperty(ConfigGenerator.ROOT_BEAN_CLASS, Order.class.getName());

        ConfigGenerator generator = new ConfigGenerator(properties, writer);

        generator.generate();

        String expected = org.smooks.io.StreamUtils.readStreamAsString(getClass().getResourceAsStream("expected-01.xml"), "UTF-8");
        assertTrue("Generated config not as expected.", org.smooks.io.StreamUtils.compareCharStreams(new java.io.StringReader(expected), new java.io.StringReader(writer.toString())));
    }

    @Test
    public void test_commandLine() throws ClassNotFoundException, java.io.IOException {
        ConfigGenerator.main(new String[] {"-c", Order.class.getName(), "-o", "./target/binding-config-test.xml"});
    }
}
