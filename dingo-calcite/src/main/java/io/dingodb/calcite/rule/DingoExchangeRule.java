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

package io.dingodb.calcite.rule;

import io.dingodb.calcite.DingoConventions;
import io.dingodb.calcite.rel.DingoExchange;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelOptRuleCall;
import org.apache.calcite.plan.RelRule;
import org.apache.calcite.rel.RelNode;

import javax.annotation.Nonnull;

public class DingoExchangeRule extends RelRule<DingoExchangeRule.Config> {
    protected DingoExchangeRule(Config config) {
        super(config);
    }

    @Override
    public void onMatch(@Nonnull RelOptRuleCall call) {
        RelNode rel = call.rel(0);
        RelOptCluster cluster = rel.getCluster();
        call.transformTo(
            new DingoExchange(
                cluster,
                cluster.traitSetOf(DingoConventions.PARTITIONED),
                rel
            )
        );
    }

    public interface Config extends RelRule.Config {
        Config DEFAULT = EMPTY
            .withOperandSupplier(b0 ->
                b0.operand(RelNode.class).trait(DingoConventions.DISTRIBUTED).anyInputs()
            )
            .withDescription("DingoExchangePartitionedRule")
            .as(Config.class);

        @Override
        default DingoExchangeRule toRule() {
            return new DingoExchangeRule(this);
        }
    }
}
