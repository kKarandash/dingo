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

import javax.annotation.Nonnull;

/**
 * Source operator has no inputs and only one output.
 */
public abstract class SourceOperator extends SoleOutOperator {
    public abstract boolean push();

    @Override
    public synchronized boolean push(int pin, @Nonnull Object[] tuple) {
        return push();
    }
}
