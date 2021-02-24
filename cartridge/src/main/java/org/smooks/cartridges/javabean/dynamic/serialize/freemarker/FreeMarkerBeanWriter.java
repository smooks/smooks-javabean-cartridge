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

import freemarker.template.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smooks.api.ApplicationContext;
import org.smooks.cartridges.javabean.dynamic.BeanMetadata;
import org.smooks.cartridges.javabean.dynamic.BeanRegistrationException;
import org.smooks.cartridges.javabean.dynamic.Model;
import org.smooks.cartridges.javabean.dynamic.serialize.BeanWriter;
import org.smooks.support.FreeMarkerTemplate;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

/**
 * FreeMarker bean writer.
 * 
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class FreeMarkerBeanWriter implements BeanWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeMarkerBeanWriter.class);

    public static final String MODEL_CTX_KEY = "dyna_model_inst";

    @Inject
    private ApplicationContext appContext;
    @Inject
    @Named("template")
    private String templateConfig;

    private FreeMarkerTemplate template;

    private static final WriteNamespacesDirective writeNamespacesDirective = new WriteNamespacesDirective();
    private static final WriteBeanDirective writeBeanDirective = new WriteBeanDirective();
    private static final WriteBeanPreTextDirective writePreTextDirective = new WriteBeanPreTextDirective();
    private static final WriteAttribsDirective writeAttribsDirective = new WriteAttribsDirective();

    @PostConstruct
    public void postConstruct() {
        final String trimmedTemplateConfig = templateConfig.trim();

        // Only attempt to load as a template resource URI if the configured 'template'
        // value is all on one line.  If it has line breaks then we know it's not an
        // external resource...
        if (trimmedTemplateConfig.trim().indexOf('\n') == -1) {
            try {
                final InputStream templateStream = appContext.getResourceLocator().getResource(trimmedTemplateConfig);
                if (templateStream != null) {
                    templateStream.close();
                    final Configuration ftlConfiguration = new Configuration(Configuration.VERSION_2_3_30);
                    ftlConfiguration.setClassLoaderForTemplateLoading(appContext.getClassLoader(), "/");
                    templateConfig = ftlConfiguration.getTemplate(trimmedTemplateConfig).toString();
                }
            } catch (IOException e) {
                LOGGER.debug("'template' configuration value '" + trimmedTemplateConfig + "' does not resolve to an external FreeMarker template.  Using configured value as the actual template.");
            }
        }

        // Create the template instance...
        template = new FreeMarkerTemplate(templateConfig);
    }

    @Override
    public void write(final Object bean, final Writer writer, final Model model) throws BeanRegistrationException, IOException {
        final Map<String, Object> templateContext = new HashMap<>();
        final BeanMetadata beanMetadata = model.getBeanMetadata(bean);

        if (beanMetadata == null) {
            BeanRegistrationException.throwUnregisteredBeanInstanceException(bean);
        }

        templateContext.put("bean", bean);
        templateContext.put(MODEL_CTX_KEY, model);
        templateContext.put("nsp", beanMetadata.getNamespacePrefix());

        templateContext.put("writeNamespaces", writeNamespacesDirective);
        templateContext.put("writeBean", writeBeanDirective);
        templateContext.put("writePreText", writePreTextDirective);
        templateContext.put("writeAttribs", writeAttribsDirective);

        template.apply(templateContext, writer);
    }
}
