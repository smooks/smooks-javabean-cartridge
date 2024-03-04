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
package org.smooks.cartridges.javabean.performance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smooks.Smooks;
import org.smooks.api.ExecutionContext;
import org.smooks.io.payload.JavaResult;
import org.smooks.support.ClassUtils;
import org.xml.sax.SAXException;

import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @author <a href="mailto:maurice.zeijen@smies.com">maurice.zeijen@smies.com</a>
 *
 */
public class PerformanceMeasurement {

	private static final Logger LOGGER = LoggerFactory.getLogger(PerformanceMeasurement.class);

	/**
	 * @param args
	 * @throws SAXException
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException, SAXException {
		try {
			LOGGER.info("Start");

			boolean simple = false;

			String file = simple ? "/smooks-config-simple.xml" : "/smooks-config-orders.xml";

			String name = simple ? "simple" : "orders";

			test(file, name + "-1.xml", name + "-1.xml");
			test(file, name + "-1.xml", name + "-50.xml");
			test(file, name + "-1.xml", name + "-500.xml");
			test(file, name + "-1.xml", name + "-5000.xml");
			test(file, name + "-1.xml", name + "-50000.xml");
			test(file, name + "-1.xml", name + "-500000.xml");



		} catch (Exception e) {
			LOGGER.debug("Exception", e);
		}
	}

	/**
	 * @throws IOException
	 * @throws SAXException
	 */
	private static void test(String configFile, String warmupFilename, String inFilename) throws IOException, SAXException {
		Long beginTime = System.currentTimeMillis();

		String packagePath = ClassUtils.toFilePath(PerformanceMeasurement.class.getPackage());
		Smooks smooks = new Smooks(packagePath + configFile);

		ExecutionContext executionContext = smooks.createExecutionContext();

		JavaResult result = new JavaResult();

		File warmupFile = new File(warmupFilename);
		File inFile  = new File(inFilename);
		Long endTime = System.currentTimeMillis();

		LOGGER.info(inFile + " initialize: " + (endTime - beginTime) + "ms");

		beginTime = System.currentTimeMillis();

		smooks.filterSource(executionContext, new StreamSource(new InputStreamReader(new FileInputStream(warmupFile))), result);

		endTime = System.currentTimeMillis();

		LOGGER.info(inFile + " filter warmup: " + (endTime - beginTime) + "ms");

		beginTime = System.currentTimeMillis();

		smooks.filterSource(executionContext, new StreamSource(new InputStreamReader(new FileInputStream(inFile))), result);

		endTime = System.currentTimeMillis();

		LOGGER.info(inFile + " filter run: " + (endTime - beginTime) + "ms");

		System.gc();

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
		}
	}
}
