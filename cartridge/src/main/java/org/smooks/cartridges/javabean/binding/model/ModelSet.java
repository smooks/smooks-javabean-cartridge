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
package org.smooks.cartridges.javabean.binding.model;

import org.smooks.api.ApplicationContext;
import org.smooks.api.SmooksConfigException;
import org.smooks.api.converter.TypeConverter;
import org.smooks.api.delivery.ContentHandlerFactory;
import org.smooks.api.resource.config.ResourceConfig;
import org.smooks.api.resource.config.ResourceConfigSeq;
import org.smooks.api.resource.config.xpath.SelectorPath;
import org.smooks.cartridges.javabean.BeanInstanceCreator;
import org.smooks.cartridges.javabean.BeanInstancePopulator;
import org.smooks.engine.lookup.ContentHandlerFactoryLookup;
import org.smooks.engine.lookup.UserDefinedResourceConfigListLookup;
import org.smooks.engine.resource.config.ParameterAccessor;
import org.smooks.engine.resource.config.xpath.DefaultSelectorPath;
import org.smooks.engine.xml.NamespaceManager;
import org.smooks.support.DollarBraceDecoder;

import javax.xml.namespace.QName;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Bean binding model set.
 *
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class ModelSet {

    /**
     * ModelSet base beans.
     * <p/>
     * A Smooks configuration can have multiple <jb:baseBeans> that can be wired together
     * in all sorts of ways to create models.  This is a Map of these baseBeans.  These
     * baseBeans are used (cloned) to create all possible models (with baseBeans all wired together).
     */
    private Map<String, Bean> baseBeans = new LinkedHashMap<String, Bean>();
    /**
     * Models.
     * <p/>
     * Should contain clones of the same baseBeans as in the baseBeans property (above), but
     * with their full graphs expanded i.e. all the bean wirings resolved and wired into
     * parent baseBeans etc.
     */
    private Map<String, Bean> models = new LinkedHashMap<String, Bean>();
    /**
     * Is the associated Smooks instance a binding only configuration.
     */
    private Boolean isBindingOnlyConfig;

    private ModelSet(ResourceConfigSeq userConfigList, ContentHandlerFactory<?> javaContentHandlerFactory) throws SmooksConfigException {
        createBaseBeanMap(userConfigList, javaContentHandlerFactory);
        createExpandedModels();
        resolveModelSelectors(userConfigList);
    }

    public Bean getModel(String beanId) {
        return models.get(beanId);
    }

    public Bean getModel(Class<?> beanType) {
        for(Bean model : models.values()) {
            if(model.getCreator().getBeanRuntimeInfo().getPopulateType() == beanType) {
                return model;
            }
        }
        return null;
    }

    public Map<String, Bean> getModels() {
        return models;
    }

    public boolean isBindingOnlyConfig() {
        return isBindingOnlyConfig;
    }

    private void createBaseBeanMap(final ResourceConfigSeq resourceConfigList, final ContentHandlerFactory<?> contentHandlerFactory) {
        for (int i = 0; i < resourceConfigList.size(); i++) {
            final ResourceConfig resourceConfig = resourceConfigList.get(i);
            final Object javaResource;
            if (resourceConfig.isJavaResource()) {
                javaResource = contentHandlerFactory.create(resourceConfig);
            } else {
                javaResource = null;
            }

            if (javaResource instanceof BeanInstanceCreator) {
                BeanInstanceCreator beanCreator = (BeanInstanceCreator) javaResource;
                Bean bean = new Bean(beanCreator).setCloneable(true);

                baseBeans.put(bean.getBeanId(), bean);

                if (isBindingOnlyConfig == null) {
                    isBindingOnlyConfig = true;
                }
            } else if (javaResource instanceof BeanInstancePopulator) {
                BeanInstancePopulator beanPopulator = (BeanInstancePopulator) javaResource;
                Bean bean = baseBeans.get(beanPopulator.getBeanId());

                if (bean == null) {
                    throw new SmooksConfigException("Unexpected binding configuration exception.  Unknown parent beanId '' for binding configuration.");
                }

                if (beanPopulator.isBeanWiring()) {
                    bean.getBindings().add(new WiredBinding(beanPopulator));
                } else {
                    bean.getBindings().add(new DataBinding(beanPopulator));
                }
            } else if (isNonBindingResource(javaResource) && !isGlobalParamsConfig(resourceConfig)) {
                // The user has configured something other than a bean binding config.
                isBindingOnlyConfig = false;
            }
        }
    }

    private boolean isNonBindingResource(Object javaResource) {
        if(javaResource instanceof TypeConverter) {
            return false;
        }

        // Ignore resource that do not manipulate the event stream...
        if(javaResource instanceof NamespaceManager) {
            return false;
        }

        return true;
    }

    private boolean isGlobalParamsConfig(ResourceConfig config) {
        return ParameterAccessor.GLOBAL_PARAMETERS.equals(config.getSelectorPath().getSelector());
    }

    private void createExpandedModels() {
        for(Bean bean : baseBeans.values()) {
            models.put(bean.getBeanId(), bean.clone(baseBeans, null));
        }
    }

    private void resolveModelSelectors(ResourceConfigSeq userConfigList) {
        // Do the beans first...
        for(Bean model : models.values()) {
            resolveModelSelectors(model);
        }

        // Now run over all configs.. there may be router configs etc using hashed selectors...
        for(int i = 0; i < userConfigList.size(); i++) {
            expandSelector(userConfigList.get(i), false, null);
        }
    }

    private void resolveModelSelectors(Bean model) {
        ResourceConfig beanConfig = model.getConfig();

        expandSelector(beanConfig, true, null);

        for(Binding binding : model.getBindings()) {
            ResourceConfig bindingConfig = binding.getConfig();
            expandSelector(bindingConfig, true, beanConfig);

            if(binding instanceof WiredBinding) {
                resolveModelSelectors(((WiredBinding) binding).getWiredBean());
            }
        }
    }

    private void expandSelector(ResourceConfig resourceConfiguration, boolean failOnMissingBean, ResourceConfig context) {
        SelectorPath selectorPath = resourceConfiguration.getSelectorPath();
        QName targetElement = selectorPath.get(0).getElement();

        if(targetElement == null) {
            return;
        }

        String localPart = targetElement.getLocalPart();
        if(localPart.equals("#") && context != null) {
            resourceConfiguration.setSelectorPath(concat(context.getSelectorPath(), selectorPath));
            return;
        }

        List<String> dollarBraceTokens = DollarBraceDecoder.getTokens(localPart);
        if(dollarBraceTokens.size() == 1) {
            String beanId = dollarBraceTokens.get(0);
            Bean bean = baseBeans.get(beanId);

            if(bean != null) {
                resourceConfiguration.setSelectorPath(concat(bean.getConfig().getSelectorPath(), selectorPath));
            } else if(failOnMissingBean) {
                throw new SmooksConfigException("Invalid selector '" + selectorPath.toString() + "'.  Unknown beanId '" + beanId + "'.");
            }

        }
    }

    private SelectorPath concat(SelectorPath context, SelectorPath beanSelectorPath) {
        SelectorPath newSelectorPath = new DefaultSelectorPath();
        newSelectorPath.addAll(Stream.concat(context.stream(), beanSelectorPath.subList(1, beanSelectorPath.size()).stream()).collect(Collectors.toList()));
        
        return newSelectorPath;
    }

    public static void build(ApplicationContext appContext) {
        ModelSet modelSet = get(appContext);
        if (modelSet == null) {
            modelSet = new ModelSet(appContext.getRegistry().lookup(new UserDefinedResourceConfigListLookup(appContext.getRegistry())), appContext.getRegistry().lookup(new ContentHandlerFactoryLookup("class")));
            appContext.getRegistry().registerObject(ModelSet.class, modelSet);
        }
    }

    public static ModelSet get(ApplicationContext appContext) {
        return appContext.getRegistry().lookup(ModelSet.class);
    }
}
