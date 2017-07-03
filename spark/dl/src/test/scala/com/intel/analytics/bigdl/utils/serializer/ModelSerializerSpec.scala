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
package com.intel.analytics.bigdl.utils

import com.intel.analytics.bigdl.Module
import com.intel.analytics.bigdl.nn._
import com.intel.analytics.bigdl.nn.abstractnn.{AbstractModule, Activity, TensorModule}
import com.intel.analytics.bigdl.tensor.Tensor
import com.intel.analytics.bigdl.tensor.TensorNumericMath.TensorNumeric
import com.intel.analytics.bigdl.tensor.TensorNumericMath.TensorNumeric.NumericFloat
import com.intel.analytics.bigdl.utils.RandomGenerator.RNG
import com.intel.analytics.bigdl.utils.serializer._
import org.scalatest.{FlatSpec, Matchers}
import scala.reflect.ClassTag
import scala.util.Random


class ModelSerializerSpec extends FlatSpec with Matchers {

  "Abs serializer" should "work properly" in {
    val abs = Abs().setName("abs")
    val tensor1 = Tensor(5, 5).apply1(_ => Random.nextFloat())
    val tensor2 = Tensor()
    val res1 = abs.forward(tensor1)
    tensor2.resizeAs(tensor1).copy(tensor1)
    ModulePersister.saveToFile("/tmp/abs.bigdl", abs, true)
    val loadedModule = ModuleLoader.loadFromFile("/tmp/abs.bigdl")
    val res2 = loadedModule.forward(tensor2)
    res1 should be (res2)
  }


  "Add serializer" should "work properly" in {
    val add = Add(5)
    val tensor1 = Tensor(5).apply1(_ => Random.nextFloat())
    val tensor2 = Tensor()
    val res1 = add.forward(tensor1)
    tensor2.resizeAs(tensor1).copy(tensor1)
    ModulePersister.saveToFile("/tmp/add.bigdl", add, true)
    val loadedAdd = ModuleLoader.loadFromFile("/tmp/add.bigdl")
    val res2 = loadedAdd.forward(tensor2)
    res1 should be (res2)
  }

  "AddConst serializer" should "work properly" in {
    val addconst = AddConstant(5)
    val tensor1 = Tensor(5).apply1(_ => Random.nextFloat())
    val tensor2 = Tensor()
    val res1 = addconst.forward(tensor1)
    tensor2.resizeAs(tensor1).copy(tensor1)
    ModulePersister.saveToFile("/tmp/addconst.bigdl", addconst, true)
    val loadedAddConst = ModuleLoader.loadFromFile("/tmp/addconst.bigdl")
    val res2 = loadedAddConst.forward(tensor2)
    res1 should be (res2)
  }

  "BatchNormalization serializer" should "work properly" in {
    val batchNorm = BatchNormalization(5)
    val tensor1 = Tensor(2, 5).apply1(_ => Random.nextFloat())
    val tensor2 = Tensor()
    tensor2.resizeAs(tensor1).copy(tensor1)
    val res1 = batchNorm.forward(tensor1)
    ModulePersister.saveToFile("/tmp/batchNorm.bigdl", batchNorm, true)
    val loadedBatchNorm = ModuleLoader.loadFromFile("/tmp/batchNorm.bigdl")
    val res2 = loadedBatchNorm.forward(tensor2)
    res1 should be (res2)
  }


  "BiLinear serializer" should "work properly" in {
    val input1 = Tensor(5, 5).apply1(e => Random.nextFloat())
    val input2 = Tensor(5, 3).apply1(e => Random.nextFloat())
    var input = new Table()
    input(1.toFloat) = input1
    input(2.toFloat) = input2

    val biLinear = Bilinear(5, 3, 2)
    val res1 = biLinear.forward(input)
    ModulePersister.saveToFile("/tmp/biLinear.bigdl", biLinear, true)
    val loadedBiLinear = ModuleLoader.loadFromFile("/tmp/biLinear.bigdl")
    val res2 = loadedBiLinear.forward(input)
    res1 should be (res2)
  }
/*
  "BiRecurrent serializer" should "work properly" in {
    val input1 = Tensor[Double](5, 5, 5).apply1(e => Random.nextDouble())
    val input2 = Tensor[Double]()
    input2.resizeAs(input1).copy(input1)
    val biRecurrent = BiRecurrent[Double]()
    val res1 = biRecurrent.forward(input1)
    ModelPersister.saveToFile("/tmp/biRecurrent.bigdl", biRecurrent, true)
    val loadedRecurent = ModelLoader.loadFromFile("/tmp/biRecurrent.bigdl")
    val res2 = loadedRecurent.asInstanceOf[BiRecurrent[Double]].forward(input2)
    res1 should be (res2)
  }
  */

  "Bottle serializer" should "work properly" in {
    val input1 = Tensor(10).apply1(e => Random.nextFloat())
    val input2 = Tensor()
    input2.resizeAs(input1).copy(input1)

    val bottle = new Bottle(Linear(10, 2).asInstanceOf[Module[Float]], 2, 2)

    val res1 = bottle.forward(input1)
    ModulePersister.saveToFile("/tmp/bottle.bigdl", bottle, true)
    val loadedBottle = ModuleLoader.loadFromFile("/tmp/bottle.bigdl")
    val res2 = loadedBottle.forward(input2)
    res1 should be (res2)
  }

  "Caddserializer" should "work properly" in {
    val input1 = Tensor(5, 1).apply1(e => Random.nextFloat())
    val input2 = Tensor()
    input2.resizeAs(input1).copy(input1)
    val cadd = CAdd(Array(5, 1))
    val res1 = cadd.forward(input1)
    ModulePersister.saveToFile("/tmp/cadd.bigdl", cadd, true)
    ModulePersister.saveModelDefinitionToFile("/tmp/cadd.prototxt", cadd, true)
    val loadedCadd = ModuleLoader.loadFromFile("/tmp/cadd.bigdl")
    val res2 = loadedCadd.forward(input2)
    res1 should be (res2)
  }

  "CaddTable serializer" should "work properly" in {
    val input1 = Tensor(5, 5).apply1(e => Random.nextFloat())
    val input2 = Tensor(5, 5).apply1(e => Random.nextFloat())
    var input = new Table()
    input(1.toFloat) = input1
    input(2.toFloat) = input2

    val caddTable = CAddTable(false)

    val res1 = caddTable.forward(input)
    ModulePersister.saveToFile("/tmp/caddTable.bigdl", caddTable, true)
    val loadedCaddTable = ModuleLoader.loadFromFile("/tmp/caddTable.bigdl")
    val res2 = loadedCaddTable.forward(input)
    res1 should be (res2)
  }

  "CDivTable serializer" should "work properly" in {
    val cdivTable = new CDivTable()
    val input1 = Tensor(10).apply1(e => Random.nextFloat())
    val input2 = Tensor(10).apply1(e => Random.nextFloat())
    var input = new Table()
    input(1.toFloat) = input1
    input(2.toFloat) = input2

    val res1 = cdivTable.forward(input)

    ModulePersister.saveToFile("/tmp/cdivTable.bigdl", cdivTable, true)
    val loadedCdivTable = ModuleLoader.loadFromFile("/tmp/cdivTable.bigdl")
    val res2 = cdivTable.forward(input)
    res1 should be (res2)
  }

  "Clamp serializer" should "work properly" in {

    val input1 = Tensor(10).apply1(e => Random.nextFloat())

    val input2 = Tensor()
    input2.resizeAs(input1).copy(input1)

    val clamp = Clamp(1, 10)
    val res1 = clamp.forward(input1)

    ModulePersister.saveToFile("/tmp/clamp.bigdl", clamp, true)
    val loadedClamp = ModuleLoader.loadFromFile("/tmp/clamp.bigdl")
    val res2 = loadedClamp.forward(input2)
    res1 should be (res2)
  }

  "CMaxTable serializer" should "work properly" in {
    val cmaxTable = new CMaxTable()
    val input1 = Tensor(10).apply1(e => Random.nextFloat())
    val input2 = Tensor(10).apply1(e => Random.nextFloat())
    var input = new Table()
    input(1.toFloat) = input1
    input(2.toFloat) = input2

    val res1 = cmaxTable.forward(input)

    ModulePersister.saveToFile("/tmp/cmaxTable.bigdl", cmaxTable, true)
    val loadedCmaxTable = ModuleLoader.loadFromFile("/tmp/cmaxTable.bigdl")
    val res2 = loadedCmaxTable.forward(input)
    res1 should be (res2)
  }

  "CMinTable serializer" should "work properly" in {
    val cminTable = new CMinTable()
    val input1 = Tensor(10).apply1(e => Random.nextFloat())
    val input2 = Tensor(10).apply1(e => Random.nextFloat())
    var input = new Table()
    input(1.toFloat) = input1
    input(2.toFloat) = input2

    val res1 = cminTable.forward(input)

    ModulePersister.saveToFile("/tmp/cminTable.bigdl", cminTable, true)
    val loadedCminTable = ModuleLoader.loadFromFile("/tmp/cminTable.bigdl")
    val res2 = loadedCminTable.forward(input)
    res1 should be (res2)
  }

  "CMulserializer" should "work properly" in {
    val input1 = Tensor(5, 1).apply1(e => Random.nextFloat())
    val input2 = Tensor()
    input2.resizeAs(input1).copy(input1)

    val cmul = CMul(Array(5, 1))

    val res1 = cmul.forward(input1)
    ModulePersister.saveToFile("/tmp/cmul.bigdl", cmul, true)
    val loadedCmul = ModuleLoader.loadFromFile("/tmp/cmul.bigdl")
    val res2 = loadedCmul.forward(input2)
    res1 should be (res2)
  }
/*
  "CMulTable serializer" should "work properly" in {
    val input1 = Tensor[Double](5, 5).apply1(e => Random.nextDouble())
    val input2 = Tensor[Double](5, 5).apply1(e => Random.nextDouble())
    var input = new Table()
    input(1.toDouble) = input1
    input(2.toDouble) = input2

    val cmulTable = CMulTable[Double]()

    val res1 = cmulTable.forward(input)
    ModelPersister.saveToFile("/tmp/cmulTable.bigdl", cmulTable, true)
    val loadedCmulTable = ModelLoader.loadFromFile("/tmp/cmulTable.bigdl")
    val res2 = loadedCmulTable.asInstanceOf[CMulTable[Double]].forward(input)
    res1 should be (res2)
  }

  "Concatserializer" should "work properly" in {
    val input1 = Tensor[Double](2, 2, 2).apply1(e => Random.nextDouble())
    val input2 = Tensor[Double]()
    input2.resizeAs(input1).copy(input1)

    val concat = Concat[Double](2)

    concat.add(Abs[Double]())
    concat.add(Abs[Double]())

    val res1 = concat.forward(input1)
    ModelPersister.saveToFile("/tmp/concat.bigdl", concat, true)
    val loadedConcat = ModelLoader.loadFromFile("/tmp/concat.bigdl")
    val res2 = loadedConcat.asInstanceOf[Concat[Double]].forward(input2)
    res1 should be (res2)
  }

  "ConcatTable serializer" should "work properly" in {
    val concatTable = new ConcatTable[Double]()
    concatTable.add(Linear[Double](10, 2))
    concatTable.add(Linear[Double](10, 2))

    val tensor1 = Tensor[Double](10).apply1(_ => Random.nextDouble())
    val tensor2 = Tensor[Double]()
    tensor2.resizeAs(tensor1).copy(tensor1)
    val res1 = concatTable.forward(tensor1)

    ModelPersister.saveToFile("/tmp/concatTable.bigdl", concatTable, true)
    val loadedConcatTable = ModelLoader.loadFromFile("/tmp/concatTable.bigdl")
    val res2 = loadedConcatTable.asInstanceOf[ConcatTable[Double]].forward(tensor2)
    res1 should be (res2)
  }

  "Contiguous serializer" should "work properly" in {
    val contiguous = new Contiguous[Double]()

    val tensor1 = Tensor[Double](10).apply1(_ => Random.nextDouble())
    val tensor2 = Tensor[Double]()
    tensor2.resizeAs(tensor1).copy(tensor1)

    val res1 = contiguous.forward(tensor1)

    ModelPersister.saveToFile("/tmp/contiguous.bigdl", contiguous, true)
    val loadedContiguous = ModelLoader.loadFromFile("/tmp/contiguous.bigdl")
    val res2 = loadedContiguous.asInstanceOf[Contiguous[Double]].forward(tensor2)
    res1 should be (res2)
  }

  "Cosine serializer" should "work properly" in {
    val cosine = new Cosine[Double](5, 5)

    val tensor1 = Tensor[Double](5).apply1(_ => Random.nextDouble())
    val tensor2 = Tensor[Double]()
    tensor2.resizeAs(tensor1).copy(tensor1)

    val res1 = cosine.forward(tensor1)

    ModelPersister.saveToFile("/tmp/cosine.bigdl", cosine, true)
    val loadedCosine = ModelLoader.loadFromFile("/tmp/cosine.bigdl")
    val res2 = loadedCosine.asInstanceOf[Cosine[Double]].forward(tensor2)
    res1 should be (res2)
  }

  "CosineDistance serializer" should "work properly" in {
    val cosineDistance = CosineDistance[Double]()

    val input1 = Tensor[Double](5, 5).apply1(e => Random.nextDouble())
    val input2 = Tensor[Double](5, 5).apply1(e => Random.nextDouble())
    var input = new Table()
    input(1.toDouble) = input1
    input(2.toDouble) = input2

    val res1 = cosineDistance.forward(input)

    ModelPersister.saveToFile("/tmp/cosineDistance.bigdl", cosineDistance, true)
    val loadedCosineDistance = ModelLoader.loadFromFile("/tmp/cosineDistance.bigdl")
    val res2 = loadedCosineDistance.asInstanceOf[CosineDistance[Double]].forward(input)
    res1 should be (res2)
  }

  "CSubTable serializer" should "work properly" in {
    val csubTable = CSubTable[Double]()

    val input1 = Tensor[Double](5, 5).apply1(e => Random.nextDouble())
    val input2 = Tensor[Double](5, 5).apply1(e => Random.nextDouble())
    var input = new Table()
    input(1.toDouble) = input1
    input(2.toDouble) = input2

    val res1 = csubTable.forward(input)

    ModelPersister.saveToFile("/tmp/csubTable.bigdl", csubTable, true)
    val loadedCSubTable = ModelLoader.loadFromFile("/tmp/csubTable.bigdl")
    val res2 = loadedCSubTable.asInstanceOf[CSubTable[Double]].forward(input)
    res1 should be (res2)
  }

  "Dotproduct serializer" should "work properly" in {
    val dotProduct = DotProduct[Double]()

    val input1 = Tensor[Double](5, 5).apply1(e => Random.nextDouble())
    val input2 = Tensor[Double](5, 5).apply1(e => Random.nextDouble())
    var input = new Table()
    input(1.toDouble) = input1
    input(2.toDouble) = input2

    val res1 = dotProduct.forward(input)

    ModelPersister.saveToFile("/tmp/dotProduct.bigdl", dotProduct, true)
    val loadedDotProduct = ModelLoader.loadFromFile("/tmp/dotProduct.bigdl")
    val res2 = loadedDotProduct.asInstanceOf[DotProduct[Double]].forward(input)
    res1 should be (res2)
  }

  "Dropout serializer" should "work properly" in {
    RNG.setSeed(100)
    val dropout = Dropout[Double]()

    val tensor1 = Tensor[Double](10).apply1(_ => Random.nextDouble())
    val tensor2 = Tensor[Double]()
    tensor2.resizeAs(tensor1).copy(tensor1)
    val res1 = dropout.forward(tensor1)

    ModelPersister.saveToFile("/tmp/dropout.bigdl", dropout, true)
    RNG.setSeed(100)
    val loadedDropout = ModelLoader.loadFromFile("/tmp/dropout.bigdl")
    val res2 = loadedDropout.asInstanceOf[Dropout[Double]].forward(tensor2)
    res1 should be (res2)
  }

  "ELU serializer" should "work properly" in {
    val elu = ELU[Double]()

    val tensor1 = Tensor[Double](10).apply1(_ => Random.nextDouble())
    val tensor2 = Tensor[Double]()
    tensor2.resizeAs(tensor1).copy(tensor1)
    val res1 = elu.forward(tensor1)

    ModelPersister.saveToFile("/tmp/elu.bigdl", elu, true)
    val loadedElu = ModelLoader.loadFromFile("/tmp/elu.bigdl")
    val res2 = loadedElu.asInstanceOf[ELU[Double]].forward(tensor2)
    res1 should be (res2)
  }

  "Euclidena serializer" should "work properly" in {
    val euclidean = Euclidean[Double](7, 7)

    val tensor1 = Tensor[Double](8, 7).apply1(_ => Random.nextDouble())
    val tensor2 = Tensor[Double]()
    tensor2.resizeAs(tensor1).copy(tensor1)
    val res1 = euclidean.forward(tensor1)

    ModelPersister.saveToFile("/tmp/euclidean.bigdl", euclidean, true)
    val loadedEuclidean = ModelLoader.loadFromFile("/tmp/euclidean.bigdl")
    val res2 = loadedEuclidean.asInstanceOf[Euclidean[Double]].forward(tensor2)
    res1 should be (res2)
  }

  "Exp serializer" should "work properly" in {
    val exp = Exp[Double]()

    val tensor1 = Tensor[Double](10).apply1(_ => Random.nextDouble())
    val tensor2 = Tensor[Double]()
    tensor2.resizeAs(tensor1).copy(tensor1)
    val res1 = exp.forward(tensor1)

    ModelPersister.saveToFile("/tmp/exp.bigdl", exp, true)
    val loadedExp = ModelLoader.loadFromFile("/tmp/exp.bigdl")
    val res2 = loadedExp.asInstanceOf[Exp[Double]].forward(tensor2)
    res1 should be (res2)
  }

  "FlattenTable serializer" should "work properly" in {
    val flattenTable = FlattenTable[Double]()

    val input1 = Tensor[Double](5, 5).apply1(e => Random.nextDouble())
    val input2 = Tensor[Double](5, 5).apply1(e => Random.nextDouble())
    var input = new Table()
    input(1.toDouble) = input1
    input(2.toDouble) = input2

    val res1 = flattenTable.forward(input)

    ModelPersister.saveToFile("/tmp/flattenTable.bigdl", flattenTable, true)
    val loadedFlattenTable = ModelLoader.loadFromFile("/tmp/flattenTable.bigdl")
    val res2 = loadedFlattenTable.asInstanceOf[FlattenTable[Double]].forward(input)
    res1 should be (res2)
  }

  "GradientReversal serializer" should "work properly" in {
    val gradientReversal = GradientReversal[Double]()

    val tensor1 = Tensor[Double](10).apply1(_ => Random.nextDouble())
    val tensor2 = Tensor[Double]()
    tensor2.resizeAs(tensor1).copy(tensor1)
    val res1 = gradientReversal.forward(tensor1)

    ModelPersister.saveToFile("/tmp/gradientReversal.bigdl", gradientReversal, true)
    val loadedGradientReversal = ModelLoader.loadFromFile("/tmp/gradientReversal.bigdl")
    val res2 = loadedGradientReversal.asInstanceOf[GradientReversal[Double]].forward(tensor2)
    res1 should be (res2)
  }

  "Graph serializer " should "work properly" in {
    val linear = Linear[Double](10, 2).apply()
    val graph = Graph(linear, linear)
    val tensor1 = Tensor[Double](10).apply1(_ => Random.nextDouble())
    val tensor2 = Tensor[Double]()
    val res1 = graph.forward(tensor1)
    tensor2.resizeAs(tensor1).copy(tensor1)
    ModelPersister.saveToFile("/tmp/graph.bigdl", graph, true)
    val loadedGraph = ModelLoader.loadFromFile("/tmp/graph.bigdl")
    val res2 = loadedGraph.asInstanceOf[Graph[Double]].forward(tensor2)
    res1 should be (res2)
  }
/*
  "GRU serializer " should "work properly" in {
    val gru = GRU[Double](10, 10)
    val input1 = Tensor[Double](10, 10).apply1(e => Random.nextDouble())
    val input2 = Tensor[Double](10, 10).apply1(e => Random.nextDouble())
    var input = new Table()
    input(1.toDouble) = input1
    input(2.toDouble) = input2
    val res1 = gru.forward(input)
    ModelPersister.saveToFile("/tmp/gru.bigdl", gru, true)
    val loadedGRU = ModelLoader.loadFromFile("/tmp/gru.bigdl")
    val res2 = loadedGRU.asInstanceOf[GRU[Double]].forward(input)
    res1 should be (res2)
  }
*/
  "HardShrink serializer" should "work properly" in {
    val hardShrink = HardShrink[Double]()

    val tensor1 = Tensor[Double](10).apply1(_ => Random.nextDouble())
    val tensor2 = Tensor[Double]()
    tensor2.resizeAs(tensor1).copy(tensor1)
    val res1 = hardShrink.forward(tensor1)

    ModelPersister.saveToFile("/tmp/hardShrink.bigdl", hardShrink, true)
    val loadedHardShrink = ModelLoader.loadFromFile("/tmp/hardShrink.bigdl")
    val res2 = loadedHardShrink.asInstanceOf[HardShrink[Double]].forward(tensor2)
    res1 should be (res2)
  }

  "HardTanh serializer" should "work properly" in {
    val hardTanh = HardTanh[Double]()

    val tensor1 = Tensor[Double](10).apply1(_ => Random.nextDouble())
    val tensor2 = Tensor[Double]()
    tensor2.resizeAs(tensor1).copy(tensor1)
    val res1 = hardTanh.forward(tensor1)

    ModelPersister.saveToFile("/tmp/hardTanh.bigdl", hardTanh, true)
    val loadedHardTanh = ModelLoader.loadFromFile("/tmp/hardTanh.bigdl")
    val res2 = loadedHardTanh.asInstanceOf[HardTanh[Double]].forward(tensor2)
    res1 should be (res2)
  }

  "Identity serializer" should "work properly" in {
    val identity = Identity[Double]()

    val tensor1 = Tensor[Double](10).apply1(_ => Random.nextDouble())
    val tensor2 = Tensor[Double]()
    tensor2.resizeAs(tensor1).copy(tensor1)
    val res1 = identity.forward(tensor1)

    ModelPersister.saveToFile("/tmp/identity.bigdl", identity, true)
    val loadedIdentity = ModelLoader.loadFromFile("/tmp/identity.bigdl")
    val res2 = loadedIdentity.asInstanceOf[Identity[Double]].forward(tensor2)
    res1 should be (res2)
  }

  "Index serializer" should "work properly" in {
    val index = Index[Double](1)

    val input1 = Tensor[Double](3).apply1(e => Random.nextDouble())
    val input2 = Tensor[Double](4)
    input2(Array(1)) = 1
    input2(Array(2)) = 2
    input2(Array(3)) = 2
    input2(Array(4)) = 3
    val gradOutput = Tensor[Double](4).apply1(e => Random.nextDouble())

    val input = new Table()
    input(1.toDouble) = input1
    input(2.toDouble) = input2

    val res1 = index.forward(input)

    ModelPersister.saveToFile("/tmp/index.bigdl", index, true)
    val loadedIndex = ModelLoader.loadFromFile("/tmp/index.bigdl")
    val res2 = loadedIndex.asInstanceOf[Index[Double]].forward(input)
    res1 should be (res2)
  }

  "Linear serializer " should "work properly" in {
    val linear = Linear[Double](10, 2)
    val tensor1 = Tensor[Double](10).apply1(_ => Random.nextDouble())
    val tensor2 = Tensor[Double]()
    val res1 = linear.forward(tensor1)
    tensor2.resizeAs(tensor1).copy(tensor1)
    ModelPersister.saveToFile("/tmp/linear.bigdl", linear, true)
    val loadedLinear = ModelLoader.loadFromFile("/tmp/linear.bigdl")
    val res2 = loadedLinear.asInstanceOf[Linear[Double]].forward(tensor2)
    res1 should be (res2)
   }

  "Sequantial Container" should "work properly" in {
    val sequential = Sequential[Double]()
    val linear = Linear[Double](10, 2)
    sequential.add(linear)
    val tensor1 = Tensor[Double](10).apply1(_ => Random.nextDouble())
    val tensor2 = Tensor[Double]()
    val res1 = sequential.forward(tensor1)
    tensor2.resizeAs(tensor1).copy(tensor1)
    ModelPersister.saveToFile("/tmp/sequential.bigdl", sequential, true)
    val loadedSequential = ModelLoader.loadFromFile("/tmp/sequential.bigdl")
    val res2 = loadedSequential.asInstanceOf[Sequential[Double]].forward(tensor2)
    res1 should be (res2)
  }

  "Customized Module " should "work properly" in {
    val testModule = new TestModule[Double](1.0)
    CustomizedDelegator.registerCustomizedModule(testModule.getClass,
      TestSerializer, Serialization.customizedData, "Test")
    val tensor1 = Tensor[Double](10).apply1(_ => Random.nextDouble())
    val tensor2 = Tensor[Double]()
    tensor2.resizeAs(tensor1).copy(tensor1)
    val res1 = testModule.forward(tensor1)
    ModelPersister.saveToFile("/tmp/testModule.bigdl", testModule, true)
    ModelPersister.saveModelDefinitionToFile("/tmp/testModule.prototxt", testModule, true)
    val loadedModule = ModelLoader.loadFromFile("/tmp/testModule.bigdl")
    val res2 = loadedModule.asInstanceOf[TestModule[Double]].forward(tensor2)
    res1 should be (res2)
  }
}
class TestModule[T: ClassTag](val constant_scalar: Double)
  (implicit ev: TensorNumeric[T]) extends TensorModule[T] {
  val addConst = AddConstant(constant_scalar)
  override def updateOutput(input: Tensor[T]): Tensor[T] = {
    output = addConst.forward(input).asInstanceOf[Tensor[T]]
    output
  }

  override def updateGradInput(input: Tensor[T], gradOutput: Tensor[T]): Tensor[T] = {
    gradInput = addConst.updateGradInput(input, gradOutput).asInstanceOf[Tensor[T]]
    gradInput
  }
}
case object TestSerializer extends AbstractModelSerializer {

  override def loadModule[T: ClassTag](model : BigDLModel)(implicit ev: TensorNumeric[T])
  : BigDLModule[T] = {
    val customParam = model.getCustomParam
    val customType = customParam.getCustomType
    val customizedData = customParam.getExtension(Serialization.customizedData)
    createBigDLModule(model, new TestModule(customizedData.getScalar).
      asInstanceOf[AbstractModule[Activity, Activity, T]])
  }

  override def serializeModule[T: ClassTag](module : BigDLModule[T])
                                           (implicit ev: TensorNumeric[T]): BigDLModel = {
    val bigDLModelBuilder = BigDLModel.newBuilder
    bigDLModelBuilder.setModuleType(ModuleType.CUSTOMIZED)
    val customParam = CustomParam.newBuilder
    customParam.setCustomType("Test")
    val testParam = TestParam.newBuilder
    testParam.setScalar(module.module.asInstanceOf[TestModule[T]].constant_scalar)
    customParam.setExtension(Serialization.customizedData, testParam.build)
    bigDLModelBuilder.setCustomParam(customParam.build)
    createSerializeBigDLModule(bigDLModelBuilder, module)
  }
  */
}
