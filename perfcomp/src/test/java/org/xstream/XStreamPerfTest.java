/*-
 * ========================LICENSE_START=================================
 * smooks-javabean-perfcomp
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
package org.xstream;

import org.junit.Test;
import static org.junit.Assert.*;

import org.smooks.Order;
import org.smooks.OrderItem;
import org.TestConstants;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class XStreamPerfTest {

    @Test
    public void test() {
        XStream xstream = new XStream(new StaxDriver());
        xstream.allowTypesByWildcard(new String[] {
                "org.smooks.Order",
                "org.smooks.OrderItem"
        });

        xstream.alias("order", Order.class);
        xstream.alias("orderItem", OrderItem.class);
        
        for(int i = 0; i < TestConstants.NUM_WARMUPS; i++) {
            xstream.fromXML(TestConstants.getMessageReader());
        }

        long start = System.currentTimeMillis();
        for(int i = 0; i < TestConstants.NUM_ITERATIONS; i++) {
            xstream.fromXML(TestConstants.getMessageReader());
        }
        System.out.println("XStream Took: " + (System.currentTimeMillis() - start));

        Order order = (Order) xstream.fromXML(TestConstants.getMessageReader());
        System.out.println("Num order items: " + order.getOrderItems().size());
    }
}
