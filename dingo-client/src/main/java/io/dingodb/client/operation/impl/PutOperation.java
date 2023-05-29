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

package io.dingodb.client.operation.impl;

import io.dingodb.client.OperationContext;
import io.dingodb.client.common.ArrayWrapperList;
import io.dingodb.client.common.Record;
import io.dingodb.client.common.RouteTable;
import io.dingodb.sdk.common.DingoCommonId;
import io.dingodb.sdk.common.KeyValue;
import io.dingodb.sdk.common.table.Table;
import io.dingodb.sdk.common.utils.Any;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;

import static io.dingodb.client.utils.OperationUtils.checkParameters;

public class PutOperation implements Operation {

    private static final PutOperation INSTANCE = new PutOperation();

    private PutOperation() {
    }

    public static PutOperation getInstance() {
        return INSTANCE;
    }

    @Override
    public Fork fork(Any parameters, Table table, RouteTable routeTable) {
        try {
            List<Record> records = parameters.getValue();
            NavigableSet<Task> subTasks = new TreeSet<>(Comparator.comparingLong(t -> t.getRegionId().entityId()));
            Map<DingoCommonId, Any> subTaskMap = new HashMap<>();
            for (int i = 0; i < records.size(); i++) {
                Record record = records.get(i);
                checkParameters(table, record);

                KeyValue keyValue = routeTable.codec.encode(record.getValues().toArray());

                Map<KeyValue, Integer> regionParams = subTaskMap.computeIfAbsent(
                    routeTable.calcRegionId(keyValue.getKey()), k -> new Any(new HashMap<>())
                ).getValue();

                regionParams.put(keyValue, i);
            }
            subTaskMap.forEach((k, v) -> subTasks.add(new Task(k, v)));
            return new Fork(new Boolean[records.size()], subTasks, true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Fork fork(OperationContext context, RouteTable routeTable) {
        Map<KeyValue, Integer> parameters = context.parameters();
        NavigableSet<Task> subTasks = new TreeSet<>(Comparator.comparingLong(t -> t.getRegionId().entityId()));

        Map<DingoCommonId, Any> subTaskMap = new HashMap<>();
        for (Map.Entry<KeyValue, Integer> parameter : parameters.entrySet()) {
            Map<KeyValue, Integer> regionParams = subTaskMap.computeIfAbsent(
                    routeTable.calcRegionId(parameter.getKey().getKey()), k -> new Any(new HashMap<>())
            ).getValue();

            regionParams.put(parameter.getKey(), parameter.getValue());
        }
        subTaskMap.forEach((k, v) -> subTasks.add(new Task(k, v)));

        return new Fork(context.result(), subTasks, true);
    }

    @Override
    public void exec(OperationContext context) {
        boolean result = context.getStoreService().kvBatchPut(
            context.getTableId(),
            context.getRegionId(),
            new ArrayList<>(context.<Map<KeyValue, Integer>>parameters().keySet())
        );
        context.<Map<KeyValue, Integer>>parameters().values().forEach(i -> context.<Boolean[]>result()[i] = result);
    }

    @Override
    public <R> R reduce(Fork fork) {
        return (R) new ArrayWrapperList<>(fork.<Boolean[]>result());
    }
}