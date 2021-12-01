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
package org.smooks.cartridges.javabean.JIRA.MILYN_443;

import org.junit.Test;
import org.smooks.Smooks;
import org.smooks.cartridges.javabean.Bean;
import org.smooks.io.payload.JavaResult;
import org.xml.sax.SAXException;

import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

/**
 * http://jira.codehaus.org/browse/MILYN-443
 * 
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class MILYN_443_Test {

    @Test
	public void test() throws IOException, SAXException {
		Smooks smooks = new Smooks(getClass().getResourceAsStream("smooks-config.xml"));
		test(smooks);
    }
	
    @Test 
    public void test_programmatic() {
		Smooks smooks = new Smooks();
		
		Properties namespaces = new Properties();
		namespaces.setProperty("e", "http://www.example.net");
		namespaces.setProperty("f", "http://www.blah");
		smooks.setNamespaces(namespaces);
		
		Bean bean = new Bean(HashMap.class, "theBean", smooks.getApplicationContext().getRegistry());
		bean.bindTo("attr1", "test1/@e:attr1");
		bean.bindTo("attr2", "test1/@f:attr2");
		smooks.addVisitors(bean);
		
		test(smooks);
    }

	private void test(Smooks smooks) {
		JavaResult result = new JavaResult();
		smooks.filterSource(new StreamSource(getClass().getResourceAsStream("message.xml")), result);
		
		Map theBean = (Map) result.getBean("theBean");
		assertEquals("xxx", theBean.get("attr1"));
		assertEquals(null, theBean.get("attr2"));
	}
}
