package com.intel.webscaleml.nn.nn

import org.scalatest.FlatSpec
import com.intel.webscaleml.nn.tensor.{torch, DenseTensor$}
import scala.math.abs

class SigmoidSpec extends  FlatSpec{
  "A Sigmoid Module " should "generate correct output and grad" in {
    val module = new Sigmoid[Double]
    val input = torch.Tensor[Double](2,2,2)
    input(Array(1,1,1)) = 0.063364277360961
    input(Array(1,1,2)) = 0.90631252736785
    input(Array(1,2,1)) = 0.22275671223179
    input(Array(1,2,2)) = 0.37516756891273
    input(Array(2,1,1)) = 0.99284988618456
    input(Array(2,1,2)) = 0.97488326719031
    input(Array(2,2,1)) = 0.94414822547697
    input(Array(2,2,2)) = 0.68123375508003
    val gradOutput = torch.Tensor[Double](2,2,2)
    gradOutput(Array(1,1,1)) = 0.38652365817688
    gradOutput(Array(1,1,2)) = 0.034144022269174
    gradOutput(Array(1,2,1)) = 0.68105488433503
    gradOutput(Array(1,2,2)) = 0.41517980070785
    gradOutput(Array(2,1,1)) = 0.91740695876069
    gradOutput(Array(2,1,2)) = 0.35317355184816
    gradOutput(Array(2,2,1)) = 0.24361599306576
    gradOutput(Array(2,2,2)) = 0.65869987895712
    val expectedOutput = torch.Tensor[Double](2,2,2)
    expectedOutput(Array(1,1,1)) = 0.51583577126786
    expectedOutput(Array(1,1,2)) = 0.71224499952187
    expectedOutput(Array(1,2,1)) = 0.55546003768115
    expectedOutput(Array(1,2,2)) = 0.59270705262321
    expectedOutput(Array(2,1,1)) = 0.72965046058394
    expectedOutput(Array(2,1,2)) = 0.72609176575892
    expectedOutput(Array(2,2,1)) = 0.71993681755829
    expectedOutput(Array(2,2,2)) = 0.66401400310487
    val expectedGrad = torch.Tensor[Double](2,2,2)
    expectedGrad(Array(1,1,1)) = 0.096533985368059
    expectedGrad(Array(1,1,2)) = 0.0069978877068295
    expectedGrad(Array(1,2,1)) = 0.16816892172375
    expectedGrad(Array(1,2,2)) = 0.1002266468557
    expectedGrad(Array(2,1,1)) = 0.18096830763559
    expectedGrad(Array(2,1,2)) = 0.070240043677749
    expectedGrad(Array(2,2,1)) = 0.049119755820981
    expectedGrad(Array(2,2,2)) = 0.14695555224503
    val inputOrg = input.clone()
    val gradOutputOrg = gradOutput.clone()
    val output = module.forward(input)
    val gradInput = module.backward(input,gradOutput)
    expectedOutput.map(output,(v1,v2)=>{assert(abs(v1-v2)<1e-6);v1})
    expectedGrad.map(gradInput,(v1,v2)=>{assert(abs(v1-v2)<1e-6);v1})
    assert(input == inputOrg)
    assert(gradOutput == gradOutputOrg)
  }
}
