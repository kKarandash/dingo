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

package io.dingodb.exec.operator;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.Arrays;
import javax.annotation.Nonnull;

@JsonTypeName("coalesce")
@JsonPropertyOrder({"inputNum"})
public final class CoalesceOperator extends SoleOutMultiInputOperator {
    @JsonCreator
    public CoalesceOperator(
        @JsonProperty("inputNum") int inputNum
    ) {
        super(inputNum);
    }

    @Override
    public synchronized boolean push(int pin, @Nonnull Object[] tuple) {
        if (!Arrays.equals(tuple, FIN)) {
            return pushOutput(tuple);
        }
        setFin(pin);
        if (isAllFin()) {
            return pushOutput(FIN);
        }
        return true;
    }
}
