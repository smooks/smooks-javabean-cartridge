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
package org.smooks.cartridges.javabean.dynamic.serialize.freemarker;

import freemarker.core.Environment;
import freemarker.ext.beans.BeanModel;
import freemarker.template.*;
import org.smooks.cartridges.javabean.dynamic.Model;

import javax.xml.XMLConstants;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
class WriteNamespacesDirective implements TemplateDirectiveModel {

    public void execute(Environment environment, Map params, TemplateModel[] templateModels, TemplateDirectiveBody templateDirectiveBody) throws TemplateException, IOException {
        Writer writer = environment.getOut();
        BeanModel modelBeanModel = (BeanModel) environment.getDataModel().get(FreeMarkerBeanWriter.MODEL_CTX_KEY);
        Model<?> model = (Model<?>) modelBeanModel.getWrappedObject();
        Map<String, String> namespaces = model.getNamespacePrefixMappings();
        Set<Map.Entry<String, String>> nsEntries = namespaces.entrySet();
        boolean addNewline = false;
        SimpleScalar indentScalar = (SimpleScalar) params.get("indent");
        int indent = 12;

        if (indentScalar != null) {
            String indentParamVal = indentScalar.getAsString().trim();
            try {
                indent = Integer.parseInt(indentParamVal);
                indent = Math.min(indent, 100);
            } catch (NumberFormatException e) {
                indent = 12;
            }
        }


        for (Map.Entry<String, String> nsEntry : nsEntries) {
            if (addNewline) {
                writer.write('\n');
                for (int i = 0; i < indent; i++) {
                    writer.write(' ');
                }
            }

            String uri = nsEntry.getKey();
            String prefix = nsEntry.getValue();

            if (prefix == null || prefix.equals(XMLConstants.DEFAULT_NS_PREFIX) || prefix.equals("xmlns")) {
                writer.write("xmlns=");
            } else {
                writer.write("xmlns:" + prefix + "=");
            }
            writer.write('"');
            writer.write(uri);
            writer.write('"');

            addNewline = true;
        }
    }
}
