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
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import org.smooks.cartridges.javabean.dynamic.BeanMetadata;
import org.smooks.cartridges.javabean.dynamic.Model;

import java.io.IOException;
import java.util.Map;

/**
 * Write bean pretext directive.
 *
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class WriteBeanPreTextDirective extends AbstractBeanDirective {

    public void execute(Environment environment, Map params, TemplateModel[] templateModels, TemplateDirectiveBody templateDirectiveBody) throws TemplateException, IOException {
        Object bean = getBeanObject(environment, params, "writePreText");

        BeanModel modelBeanModel = (BeanModel) environment.getDataModel().get(FreeMarkerBeanWriter.MODEL_CTX_KEY);
        Model model = (Model) modelBeanModel.getWrappedObject();
        BeanMetadata beanMetadata = model.getBeanMetadata(bean);

        if (beanMetadata != null && beanMetadata.getPreText() != null) {
            String preText = beanMetadata.getPreText().trim();
            if (preText.length() > 0) {
                environment.getOut().write(trimPretext(beanMetadata.getPreText()));
            }
        }
    }

    private String trimPretext(String string) {
        StringBuffer stringBuf = new StringBuffer(string);

        while (stringBuf.length() > 0) {
            char firstChar = stringBuf.charAt(0);

            if (firstChar == '\r' || firstChar == '\n') {
                stringBuf.deleteCharAt(0);
            } else {
                break;
            }
        }

        return stringBuf.toString();
    }
}
