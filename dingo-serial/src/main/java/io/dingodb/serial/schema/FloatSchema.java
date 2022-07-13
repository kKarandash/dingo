/*
 * Copyright 2021 DataCanvas
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
 */

package io.dingodb.serial.schema;

public class FloatSchema implements DingoSchema {
    private int index;
    private boolean notNull;
    private Float defaultValue;

    public FloatSchema(int index) {
        setIndex(index);
        setNotNull(false);
    }
    public FloatSchema(int index, Object defaultValue) {
        setIndex(index);
        setNotNull(true);
        setDefaultValue(defaultValue);
    }

    @Override
    public Type getType() {
        return Type.FLOAT;
    }

    @Override
    public void setIndex(int index) {
        this.index = index;
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public void setLength(int length) {
        throw new UnsupportedOperationException("Float Schema data length always be 5");
    }

    @Override
    public int getLength() {
        return 5;
    }

    @Override
    public void setMaxLength(int maxLength) {
        throw new UnsupportedOperationException("Float Schema data length always be 5");
    }

    @Override
    public int getMaxLength() {
        return 5;
    }

    @Override
    public void setPrecision(int precision) {
        throw new UnsupportedOperationException("Float Schema not support Precision");
    }

    @Override
    public int getPrecision() {
        return 0;
    }

    @Override
    public void setScale(int scale) {
        throw new UnsupportedOperationException("Float Schema not support Scale");
    }

    @Override
    public int getScale() {
        return 0;
    }

    @Override
    public void setNotNull(boolean notNull) {
        this.notNull = notNull;
    }

    @Override
    public boolean isNotNull() {
        return notNull;
    }

    @Override
    public void setDefaultValue(Object t) throws ClassCastException {
        this.defaultValue = (Float) t;
    }

    @Override
    public Object getDefaultValue() {
        return defaultValue;
    }
}