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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.dingodb.common.table.TupleSchema;
import io.dingodb.exec.base.Id;
import io.dingodb.exec.base.Operator;
import io.dingodb.exec.base.Task;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    property = "type"
)
@JsonSubTypes({
    @JsonSubTypes.Type(AggregateOperator.class),
    @JsonSubTypes.Type(CoalesceOperator.class),
    @JsonSubTypes.Type(GetByKeysOperator.class),
    @JsonSubTypes.Type(PartDeleteOperator.class),
    @JsonSubTypes.Type(PartInsertOperator.class),
    @JsonSubTypes.Type(PartitionOperator.class),
    @JsonSubTypes.Type(PartScanOperator.class),
    @JsonSubTypes.Type(PartUpdateOperator.class),
    @JsonSubTypes.Type(ProjectOperator.class),
    @JsonSubTypes.Type(ReceiveOperator.class),
    @JsonSubTypes.Type(ReduceOperator.class),
    @JsonSubTypes.Type(RootOperator.class),
    @JsonSubTypes.Type(SendOperator.class),
    @JsonSubTypes.Type(SumUpOperator.class),
    @JsonSubTypes.Type(ValuesOperator.class),
})
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class AbstractOperator implements Operator {
    public static Object[] FIN = new Object[0];
    public static byte[] FIN_BYTES = new byte[0];

    @Getter
    @Setter
    protected Id id;
    @Getter
    @Setter
    protected Task task;

    protected static String formatTuple(TupleSchema schema, Object[] tuple) {
        return Arrays.equals(tuple, FIN) ? "{<<FIN>>}" : schema.formatTuple(tuple);
    }
}
