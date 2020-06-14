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
package org.smooks.cartridges.javabean;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class TypePopCheckBean {
    // Primitives...
	private byte byteVal;
	private short shortVal;
    private int intVal;
    private long longVal;
    private boolean boolVal;
    private float floatVal;
    private double doubleVal;
    private char charVal;

    // Object...
    private Integer integerVal;
    private Date dateVal;

    // Primitive Arrays...
    private int[] intValArray;

    // Object Arrays...
    private Integer[] integerValArray;

    // List...
    private List<Integer> integerValList;

    // Map...
    private Map<String, Integer> integerValMap;

    /**
	 * @return the byteVal
	 */
	public byte getByteVal() {
		return byteVal;
	}

	/**
	 * @param byteVal the byteVal to set
	 */
	public void setByteVal(byte byteVal) {
		this.byteVal = byteVal;
	}

	/**
	 * @return the shortVal
	 */
	public short getShortVal() {
		return shortVal;
	}

	/**
	 * @param shortVal the shortVal to set
	 */
	public void setShortVal(short shortVal) {
		this.shortVal = shortVal;
	}
	
    public int getIntVal() {
        return intVal;
    }

	public void setIntVal(int intVal) {
        this.intVal = intVal;
    }

    public long getLongVal() {
        return longVal;
    }

    public void setLongVal(long longVal) {
        this.longVal = longVal;
    }

    public boolean isBoolVal() {
        return boolVal;
    }

    public void setBoolVal(boolean boolVal) {
        this.boolVal = boolVal;
    }

    public float getFloatVal() {
        return floatVal;
    }

    public void setFloatVal(float floatVal) {
        this.floatVal = floatVal;
    }

    public double getDoubleVal() {
        return doubleVal;
    }

    public void setDoubleVal(double doubleVal) {
        this.doubleVal = doubleVal;
    }

    public char getCharVal() {
        return charVal;
    }

    public void setCharVal(char charVal) {
        this.charVal = charVal;
    }

    public Date getDateVal() {
        return dateVal;
    }

    public Integer getIntegerVal() {
        return integerVal;
    }

    public void setIntegerVal(Integer integerVal) {
        this.integerVal = integerVal;
    }

    public int[] getIntValArray() {
        return intValArray;
    }

    public void setIntValArray(int[] intValArray) {
        this.intValArray = intValArray;
    }

    public Integer[] getIntegerValArray() {
        return integerValArray;
    }

    public void setIntegerValArray(Integer[] integerValArray) {
        this.integerValArray = integerValArray;
    }

    public void setDateVal(Date dateVal) {
        this.dateVal = dateVal;
    }

    public List<Integer> getIntegerValList() {
        return integerValList;
    }

    public void setIntegerValList(List<Integer> integerValList) {
        this.integerValList = integerValList;
    }

    public Map<String, Integer> getIntegerValMap() {
        return integerValMap;
    }

    public void setIntegerValMap(Map<String, Integer> integerValMap) {
        this.integerValMap = integerValMap;
    }

    @Override
	public String toString() {
        StringBuffer string = new StringBuffer();

        string.append(intVal + ", ");
        string.append(longVal + ", ");
        string.append(boolVal + ", ");
        string.append(floatVal + ", ");
        string.append(doubleVal + ", ");
        string.append(charVal + ", ");

        string.append(integerVal + ", ");
        string.append((dateVal != null?dateVal.getTime():"null") + ", ");

        // Primitive Arrays...
        if(intValArray != null) {
            string.append(intValArray[0] + ", ");
            string.append(intValArray[1] + ", ");
            string.append(intValArray[2] + ", ");
        }

        // Object Arrays...
        if(integerValArray != null) {
            string.append(Arrays.asList(integerValArray) + ", ");
        }

        // List...
        string.append(integerValList + ", ");

        // Map...
        string.append(integerValMap + ", ");

        return string.toString();
    }
}
