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

import org.smooks.assertion.AssertArgument;
import org.smooks.cartridges.javabean.gen.model.BindingConfig;
import org.smooks.cartridges.javabean.gen.model.ClassConfig;
import org.smooks.converter.TypeConverterFactoryLoader;
import org.smooks.converter.factory.TypeConverterFactory;
import org.smooks.registry.lookup.converter.SourceTargetTypeConverterFactoryLookup;
import org.smooks.util.FreeMarkerTemplate;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Java binding configuration template generator.
 * <h3>Usage</h3>
 * From the commandline:
 * <pre>
 * {@code
 *     $JAVA_HOME/bin/java -classpath <classpath> org.smooks.cartridges.javabean.gen.ConfigGenerator -c <rootBeanClass> -o <outputFilePath> [-p <propertiesFilePath>]
 * }</pre>
 * <ul>
 *  <li>The "-c" commandline arg specifies the root class of the model whose binding config is to be generated.</li>
 *  <li>The "-o" commandline arg specifies the path and filename for the generated config output.</li>
 *  <li>The "-p" commandline arg specifies the path and filename optional binding configuration file that specifies aditional binding parameters.</li>
 * </ul>
 * <p/>
 * The optional "-p" properties file parameter allows specification of additional config parameters:
 * <ul>
 *  <li><b>packages.included</b>: Semi-colon separated list of packages. Any fields in the class matching these packages will be included in the binding configuration generated.</li>
 *  <li><b>packages.excluded</b>: Semi-colon separated list of packages. Any fields in the class matching these packages will be excluded from the binding configuration generated.</li>
 * </ul>
 *
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class ConfigGenerator {

    private static final Set<TypeConverterFactory<?, ?>> TYPE_CONVERTER_FACTORIES = new TypeConverterFactoryLoader().load();

    public static final String ROOT_BEAN_CLASS = "root.beanClass";
    public static final String PACKAGES_INCLUDED = "packages.included";
    public static final String PACKAGES_EXCLUDED = "packages.excluded";

    private Writer outputWriter;
    private Class<?> rootBeanClass;
    private List<String> packagesIncluded;
    private List<String> packagesExcluded;
    private Stack<Class<?>> classStack = new Stack<Class<?>>();

    public static void main(String args[]) throws IOException, ClassNotFoundException {
        String rootBeanClassName = getArgument("-c", "Root Bean Class Name", true, args);
        String outputFileName = getArgument("-o", "Output File Path", true, args);
        String propertiesFile = getArgument("-p", "Binding Generation Config File Path", false, args);
        Properties properties = loadProperties(propertiesFile);
        File outputFile = new File(outputFileName);

        properties.setProperty(ROOT_BEAN_CLASS, rootBeanClassName);

        // Create the directory (and any parent directories) for the output
        // file, if possible.
        if (outputFile.getParentFile() != null) {
            outputFile.getParentFile().mkdirs();
        }

        Writer outputWriter = new FileWriter(outputFile);

        try {
            ConfigGenerator generator = new ConfigGenerator(properties, outputWriter);
            generator.generate();
        } finally {
            try {
                outputWriter.flush();
            } finally {
                outputWriter.close();
            }
        }
    }

    public ConfigGenerator(Properties bindingProperties, Writer outputWriter) throws ClassNotFoundException {
        AssertArgument.isNotNull(bindingProperties, "bindingProperties");
        AssertArgument.isNotNull(outputWriter, "outputWriter");
        this.outputWriter = outputWriter;

        configure(bindingProperties);
    }

    public void generate() throws IOException {
        Map<String, List<ClassConfig>> templatingContextObject = new HashMap<String, List<ClassConfig>>();
        List<ClassConfig> classConfigs = new ArrayList<ClassConfig>();
        FreeMarkerTemplate template;

        addClassConfig(classConfigs, rootBeanClass, null);
        template = new FreeMarkerTemplate("templates/bindingConfig.ftl.xml", getClass());

        templatingContextObject.put("classConfigs", classConfigs);
        outputWriter.write(template.apply(templatingContextObject));
    }

    private ClassConfig addClassConfig(List<ClassConfig> classConfigs, Class<?> beanClass, String beanId) {
        if(classStack.contains(beanClass)) {
            // Don't go into an endless loop... stack overflow etc...
            return null;
        }

        classStack.push(beanClass);
        try {
            ClassConfig classConfig = new ClassConfig(beanClass, beanId);
            Field[] fields = beanClass.getDeclaredFields();
            List<BindingConfig> bindings = classConfig.getBindings();

            // Determine the package name for the root bean class.
            String rootPackage = rootBeanClass.getPackage() != null
                                ? rootBeanClass.getPackage().getName()
                                // Fallback case in the rare situation where the
                                // bean class is in the default package.
                                : "";

            classConfigs.add(classConfig);

            for(Field field : fields) {
                Class<?> type = field.getType();
                TypeConverterFactory<? super String, ?> typeConverterFactory = new SourceTargetTypeConverterFactoryLookup<>(String.class, type).lookup(TYPE_CONVERTER_FACTORIES);

                if(typeConverterFactory != null) {
                    bindings.add(new BindingConfig(field));
                } else {
                    if(type.isArray()) {
                        addArrayConfig(classConfigs, bindings, rootPackage, field);
                    } else if(Collection.class.isAssignableFrom(type)) {
                        addCollectionConfig(classConfigs, bindings, rootPackage, field);
                    } else {
                        String typePackage = type.getPackage().getName();

                        if(isExcluded(typePackage)) {
                            continue;
                        } else if(typePackage.startsWith(rootPackage) || isIncluded(typePackage)) {
                            bindings.add(new BindingConfig(field, field.getName()));
                            addClassConfig(classConfigs, type, field.getName());
                        }
                    }
                }
            }

            return classConfig;
        } finally {
            classStack.pop();
        }
    }

    private void addArrayConfig(List<ClassConfig> classConfigs, List<BindingConfig> bindings, String rootPackage, Field field) {
        Class<?> type = field.getType();
        Class<?> arrayType = type.getComponentType();
        String wireBeanId = field.getName() + "_entry";
        String typePackage = arrayType.getPackage().getName();

        if(isExcluded(typePackage)) {
            return;
        } else if(typePackage.startsWith(rootPackage) || isIncluded(typePackage)) {
            ClassConfig arrayConfig = new ClassConfig(arrayType, field.getName());

            arrayConfig.getBindings().add(new BindingConfig(wireBeanId));
            arrayConfig.setArray(true);
            classConfigs.add(arrayConfig);

            bindings.add(new BindingConfig(field, field.getName()));
            addClassConfig(classConfigs, arrayType, wireBeanId);
        }
    }

    private void addCollectionConfig(List<ClassConfig> classConfigs, List<BindingConfig> bindings, String rootPackage, Field field) {
        ParameterizedType paramType = (ParameterizedType) field.getGenericType();
        Type[] types = paramType.getActualTypeArguments();

        if(types.length == 0) {
            // No generics info.  Can't infer anything...
        } else {
            Class<?> type = (Class<?>) types[0];
            String wireBeanId = field.getName() + "_entry";
            String typePackage = type.getPackage().getName();

            if(isExcluded(typePackage)) {
                return;
            } else if(typePackage.startsWith(rootPackage) || isIncluded(typePackage)) {
                ClassConfig listConfig = new ClassConfig(ArrayList.class, field.getName());

                listConfig.getBindings().add(new BindingConfig(wireBeanId));
                classConfigs.add(listConfig);

                bindings.add(new BindingConfig(field, field.getName()));
                addClassConfig(classConfigs, type, wireBeanId);
            }
        }
    }

    private boolean isIncluded(String packageName) {
        if(packagesIncluded != null) {
          return isInPackageList(packagesIncluded, packageName);
        }
        return false;
    }

    private boolean isExcluded(String packageName) {
        if(packagesExcluded != null) {
          return isInPackageList(packagesExcluded, packageName);
        }
        return false;
    }

    private boolean isInPackageList(List<String> packages, String typePackage) {
        for (String packageName : packages) {
            if(typePackage.startsWith(packageName)) {
                return true;
            }
        }

        return false;
    }

    private void configure(Properties bindingProperties) throws ClassNotFoundException {
        String rootBeanClassConfig = bindingProperties.getProperty(ConfigGenerator.ROOT_BEAN_CLASS);
        String packagesIncludedConfig = bindingProperties.getProperty(ConfigGenerator.PACKAGES_INCLUDED);
        String packagesExcludedConfig = bindingProperties.getProperty(ConfigGenerator.PACKAGES_EXCLUDED);

        if(rootBeanClassConfig == null) {
            throw new IllegalArgumentException("Binding configuration property '" + ConfigGenerator.ROOT_BEAN_CLASS + "' not defined.");
        }
        rootBeanClass = Class.forName(rootBeanClassConfig);

        if(packagesIncludedConfig != null) {
            packagesIncluded = parsePackages(packagesIncludedConfig);
        }
        if(packagesExcludedConfig != null) {
            packagesExcluded = parsePackages(packagesExcludedConfig);
        }
    }

    private List<String> parsePackages(String packagesString) {
        String[] packages = packagesString.split(";");
        List<String> packagesSet = new ArrayList<String>();

        for(String aPackage : packages) {
            packagesSet.add(aPackage.trim());
        }

        return packagesSet;
    }

    private static Properties loadProperties(String fileName) throws IOException {
        Properties properties = new Properties();

        if(fileName != null) {
            File propertiesFile = new File(fileName);

            if(!propertiesFile.exists()) {
                throw new IllegalArgumentException("Binding configuration properties file '" + propertiesFile.getAbsolutePath() + "' doesn't exist.  See class Javadoc.");
            }

            InputStream stream = new FileInputStream(propertiesFile);

            try {
                properties.load(stream);
            } finally {
                stream.close();
            }
        }

        return properties;
    }

    private static String getArgument(String argAlias, String argName, boolean mandatory, String[] args) {
        for(int i = 0; i < args.length; i++) {
            if(args[i].equalsIgnoreCase(argAlias) && i + 1 < args.length) {
                return args[i + 1].trim();
            }
        }

        if(mandatory) {
            throw new IllegalArgumentException("Binding configuration error.  Missing value for commandline arg '" + argAlias + "' (" + argName + ")'.");
        }

        return null;
    }
}
