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
package org.smooks.cartridges.javabean.ext;

import org.smooks.api.ExecutionContext;
import org.smooks.api.SmooksConfigException;
import org.smooks.api.SmooksException;
import org.smooks.api.resource.visitor.dom.DOMVisitBefore;
import org.w3c.dom.Element;

/**
 * Bean wiring target attribute checker.
 * <p/>
 * Makes sure that one of "beanIdRef", "beanClass" and "beanAnnotation" are configured.
 *
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class WireTargetChecker implements DOMVisitBefore {

    public void visitBefore(Element element, ExecutionContext execContext) throws SmooksException {
        boolean isBeanIdRefSpecified = element.hasAttribute("beanIdRef");
        boolean isBeanTypeSpecified = element.hasAttribute("beanType");
        boolean isBeanAnnotationSpecified = element.hasAttribute("beanAnnotation");

        if (!isBeanIdRefSpecified && !isBeanTypeSpecified && !isBeanAnnotationSpecified) {
            throw new SmooksConfigException("One or more of attributes 'beanIdRef', 'beanType' and 'beanAnnotation' must be specified on a bean wiring configuration.");
        }
    }
}
