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
package org.smooks.cartridges.javabean.performance.generator;

import freemarker.template.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * @author <a href="mailto:maurice.zeijen@smies.com">maurice.zeijen@smies.com</a>
 *
 */
public class GenerateXML {
	public static void main(String[] args) throws IOException, TemplateException {
		Locale.setDefault(Locale.US);

		boolean simple = false;

		Configuration cfg = new Configuration(Configuration.VERSION_2_3_30);
		// Specify the data source where the template files come from.
		// Here I set a file directory for it:
		cfg.setDirectoryForTemplateLoading(new File("src/test/resources/templates"));

		// Specify how templates will see the data-model. This is an advanced topic...
		// but just use this:
		cfg.setObjectWrapper(new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_30).build());

		Template temp;
		String name;
		if(simple) {
			temp = cfg.getTemplate("simple.ftl");
			name = "simple";
		} else {
			temp = cfg.getTemplate("extended.ftl");
			name = "orders";
		}

		String path = "";

		write(temp, 1, path + name +"-1.xml", simple);
		write(temp, 50, path + name +"-50.xml", simple);
		write(temp, 500, path + name +"-500.xml", simple);
		write(temp, 5000, path + name +"-5000.xml", simple);
		write(temp, 50000, path + name +"-50000.xml", simple);
		write(temp, 500000, path + name +"-500000.xml", simple);

        System.out.println("done");

	}

	private static void write(Template template, int numCustomers, String fileName, boolean simple) throws TemplateException,
			IOException {
		System.out.println("Writing " + fileName);

		Map<String, TemplateSequenceModel> root = new HashMap<String, TemplateSequenceModel>();
		if(simple) {
			root.put("customers", new SimpleGenerator(numCustomers));
		} else {
			root.put("customers", new CustomerGenerator(numCustomers));
		}

		Writer out = null;
		try {
			out = createWriter(fileName);

			template.process(root, out);
		} finally {
			closeWriter(out);
		}
	}

	private static Writer createWriter(final String filepath) {

		try {
			File file = new File(filepath);

			file.mkdirs();
			if(file.exists()) {
				file.delete();
			}
			file.createNewFile();

			return new FileWriter(file);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static void closeWriter(Writer writer) {
		try {
			if(writer != null) {
				writer.close();
			}
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}
}
