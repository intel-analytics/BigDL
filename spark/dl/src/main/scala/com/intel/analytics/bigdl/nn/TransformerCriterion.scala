/*
 * Copyright 2016 The BigDL Authors.
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
package com.intel.analytics.bigdl.nn

import com.intel.analytics.bigdl.nn.abstractnn.{AbstractCriterion, AbstractModule, Activity}
import com.intel.analytics.bigdl.tensor.Tensor
import com.intel.analytics.bigdl.tensor.TensorNumericMath.TensorNumeric
import com.intel.analytics.bigdl.utils.Table

import scala.reflect.ClassTag

/**
 * The criterion that takes two modules to transform input and target, and take
 * one criterion to compute the loss with the transformed input and target.
 * @param inputTransformer
 * @param targetTransformer
 * @param criterion
 * @tparam T The numeric type in the criterion, usually which are [[Float]] or [[Double]]
 */
class TransformerCriterion[T: ClassTag](
         criterion: AbstractCriterion[Activity, Activity, T],
         inputTransformer: Option[AbstractModule[Activity, Activity, T]] = None,
         targetTransformer: Option[AbstractModule[Activity, Activity, T]] = None
         )(implicit ev: TensorNumeric[T]) extends AbstractCriterion[Activity, Activity, T]{

  private var transformedInput: Activity = _
  private var transformedTarget: Activity = _

  override def updateOutput(input: Activity, target: Activity): T = {
    transformedInput = inputTransformer.map(t => t.forward(input))
      .getOrElse(input) match {
      case t: Tensor[T] => t.clone()
      case t: Table => t.clone()
    }
    transformedTarget = targetTransformer.map(t => t.forward(target))
      .getOrElse(target) match {
      case t: Tensor[T] => t.clone()
      case t: Table => t.clone()
    }
    output = criterion.forward(transformedInput, transformedTarget)
    output
  }

  override def updateGradInput(input: Activity, target: Activity): Activity = {
    require(transformedTarget != null && transformedInput != null, "please run forward first")

    val gradInputCriterion = criterion.backward(transformedInput, transformedTarget)
    gradInput = inputTransformer
      .map(t => t.updateGradInput(input, gradInputCriterion))
      .getOrElse(gradInputCriterion)
    gradInput
  }
}

object TransformerCriterion {

  def apply[T: ClassTag](
             criterion: AbstractCriterion[Activity, Activity, T],
             inputTransformer: Option[AbstractModule[Activity, Activity, T]] = None,
             targetTransformer: Option[AbstractModule[Activity, Activity, T]] = None
           )(implicit ev: TensorNumeric[T]): TransformerCriterion[T] =
    new TransformerCriterion(criterion, inputTransformer, targetTransformer)
}