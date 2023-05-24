// Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
// or more contributor license agreements. Licensed under the Elastic License
// 2.0; you may not use this file except in compliance with the Elastic License
// 2.0.
package org.elasticsearch.compute.aggregation;

import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import org.elasticsearch.common.util.BigArrays;
import org.elasticsearch.compute.data.AggregatorStateVector;
import org.elasticsearch.compute.data.Block;
import org.elasticsearch.compute.data.DoubleBlock;
import org.elasticsearch.compute.data.DoubleVector;
import org.elasticsearch.compute.data.ElementType;
import org.elasticsearch.compute.data.IntVector;
import org.elasticsearch.compute.data.Page;
import org.elasticsearch.compute.data.Vector;

/**
 * {@link AggregatorFunction} implementation for {@link AvgDoubleAggregator}.
 * This class is generated. Do not edit it.
 */
public final class AvgDoubleAggregatorFunction implements AggregatorFunction {
  private final AvgDoubleAggregator.AvgState state;

  private final int channel;

  private final Object[] parameters;

  public AvgDoubleAggregatorFunction(int channel, AvgDoubleAggregator.AvgState state,
      Object[] parameters) {
    this.channel = channel;
    this.state = state;
    this.parameters = parameters;
  }

  public static AvgDoubleAggregatorFunction create(BigArrays bigArrays, int channel,
      Object[] parameters) {
    return new AvgDoubleAggregatorFunction(channel, AvgDoubleAggregator.initSingle(), parameters);
  }

  @Override
  public void addRawInput(Page page) {
    assert channel >= 0;
    ElementType type = page.getBlock(channel).elementType();
    if (type == ElementType.NULL) {
      return;
    }
    DoubleBlock block = page.getBlock(channel);
    DoubleVector vector = block.asVector();
    if (vector != null) {
      addRawVector(vector);
    } else {
      addRawBlock(block);
    }
  }

  private void addRawVector(DoubleVector vector) {
    for (int i = 0; i < vector.getPositionCount(); i++) {
      AvgDoubleAggregator.combine(state, vector.getDouble(i));
    }
    AvgDoubleAggregator.combineValueCount(state, vector.getPositionCount());
  }

  private void addRawBlock(DoubleBlock block) {
    for (int p = 0; p < block.getPositionCount(); p++) {
      if (block.isNull(p)) {
        continue;
      }
      int start = block.getFirstValueIndex(p);
      int end = start + block.getValueCount(p);
      for (int i = start; i < end; i++) {
        AvgDoubleAggregator.combine(state, block.getDouble(i));
      }
    }
    AvgDoubleAggregator.combineValueCount(state, block.getTotalValueCount());
  }

  @Override
  public void addIntermediateInput(Block block) {
    assert channel == -1;
    Vector vector = block.asVector();
    if (vector == null || vector instanceof AggregatorStateVector == false) {
      throw new RuntimeException("expected AggregatorStateBlock, got:" + block);
    }
    @SuppressWarnings("unchecked") AggregatorStateVector<AvgDoubleAggregator.AvgState> blobVector = (AggregatorStateVector<AvgDoubleAggregator.AvgState>) vector;
    // TODO exchange big arrays directly without funny serialization - no more copying
    BigArrays bigArrays = BigArrays.NON_RECYCLING_INSTANCE;
    AvgDoubleAggregator.AvgState tmpState = AvgDoubleAggregator.initSingle();
    for (int i = 0; i < block.getPositionCount(); i++) {
      blobVector.get(i, tmpState);
      AvgDoubleAggregator.combineStates(state, tmpState);
    }
    tmpState.close();
  }

  @Override
  public Block evaluateIntermediate() {
    AggregatorStateVector.Builder<AggregatorStateVector<AvgDoubleAggregator.AvgState>, AvgDoubleAggregator.AvgState> builder =
        AggregatorStateVector.builderOfAggregatorState(AvgDoubleAggregator.AvgState.class, state.getEstimatedSize());
    builder.add(state, IntVector.range(0, 1));
    return builder.build().asBlock();
  }

  @Override
  public Block evaluateFinal() {
    return AvgDoubleAggregator.evaluateFinal(state);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName()).append("[");
    sb.append("channel=").append(channel);
    sb.append("]");
    return sb.toString();
  }

  @Override
  public void close() {
    state.close();
  }
}
