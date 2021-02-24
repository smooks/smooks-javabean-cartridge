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
package org.smooks;

import org.TestConstants;
import org.junit.Test;
import org.smooks.api.ExecutionContext;
import org.smooks.api.bean.context.BeanContext;
import org.smooks.api.bean.lifecycle.BeanContextLifecycleEvent;
import org.smooks.api.bean.lifecycle.BeanContextLifecycleObserver;
import org.smooks.api.bean.lifecycle.BeanLifecycle;
import org.smooks.api.bean.repository.BeanId;
import org.smooks.engine.bean.repository.DefaultBeanId;
import org.smooks.io.payload.JavaResult;
import org.xml.sax.SAXException;

import javax.xml.transform.stream.StreamSource;
import java.io.IOException;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class SmooksPerfTest {

    @Test
    public void test() throws IOException, SAXException {
        Smooks smooks = new Smooks(getClass().getResourceAsStream("smooks-config.xml"));

        for (int i = 0; i < TestConstants.NUM_WARMUPS; i++) {
            JavaResult javaResult = new JavaResult();
            smooks.filterSource(new StreamSource(TestConstants.getMessageReader()), javaResult);
        }

        long start = System.currentTimeMillis();
        JavaResult javaResult = null;
        NoddyObserver nobserver = new NoddyObserver();
        for (int i = 0; i < TestConstants.NUM_ITERATIONS; i++) {
            ExecutionContext executionContext = smooks.createExecutionContext();
            BeanContext beanContext = executionContext.getBeanContext();

            for (int ii = 0; ii < 15; ii++) {
                beanContext.addObserver(nobserver);
            }

            javaResult = new JavaResult();
            smooks.filterSource(executionContext, new StreamSource(TestConstants.getMessageReader()), javaResult);
        }

        System.out.println("Smooks took: " + (System.currentTimeMillis() - start));
        Order order = (Order) javaResult.getBean("order");
        if (order != null) {
            System.out.println("Num order items: " + order.getOrderItems().size());
        }
    }

    static class NoddyObserver implements BeanContextLifecycleObserver {

        private BeanId beanId = new DefaultBeanId(null, 0, null);

        public void onBeanLifecycleEvent(BeanContextLifecycleEvent event) {
            if (event.getBeanId() == beanId && event.getLifecycle() == BeanLifecycle.ADD) {
                int s = beanId.getIndex();
                s++;
            }
        }
    }
}
