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

package com.intel.analytics.bigdl.dllib.nn

import com.intel.analytics.bigdl.dllib.tensor.{Storage, Tensor}
import com.intel.analytics.bigdl.dllib.utils.{T, Table, TestUtils}
import com.intel.analytics.bigdl.dllib.utils.serializer.ModuleSerializationTest
import org.scalatest.{FlatSpec, Matchers}

import scala.util.Random

class RoiPoolingSpec extends FlatSpec with Matchers {
  val data = Array(-3.8623801600318241611, -5.5763739585689267031, 10.298773638368681205,
    9.0803885026851531848, 1.3665552448780498018, -0.44133702789011497458, -9.4017101101805629071,
    1.0564141421332300386, 13.553048566835151689, -13.990139481179310721, 0.38796681814726624582,
    1.6085467104900628144, 8.8876250967623846577, 2.3242425937246933287, -4.9687617543318083335,
    3.7455892940541879454, 2.0669240740922889543, 19.429232983437231042, 7.1232979388765649276,
    -10.957751000013594478, 4.5843320542179828436, 16.586359188890380523, -1.030049016926615435,
    -21.753624028643553601, -2.7482613307623209309, 2.2115953537485082414, 0.85470105269720708652,
    1.8852580346394267607, -0.8805346172236995228, -21.679835446808542798, 16.704283714127733163,
    7.8069295625249512938, 3.184262715173278302, -2.7953370037311042751, 18.803346654854180997,
    -5.7659957565117920808, 6.7988882142279774001, -3.1398516809822685225, -12.638708079214094226,
    7.4283926936892994419, 11.19046917617362169, 2.0605789791992457083, 18.29245858118680701,
    -2.0744947827599187207, -3.962217887680556494, 6.2749858922260317584, -3.4943816632045292536,
    -12.521703862229173865, -12.161634362129138509, 14.352821396218683248, 19.435416458943819862,
    4.3579100173182769851, 2.1117505735643584686, -2.7203956428352995545, -7.2751004014404809794,
    -11.037989254462871713, 1.8912296992510557736, -19.130513203340292705, -6.2762421106880914579,
    -2.2190097060154592157, 1.5358789142040989439, -1.3524662191391068067, 11.183090363214319041,
    -7.6827234847292178443, 26.542527340702616101, 1.7405322696704410568, 14.482584826444851345,
    7.3285097875212379392, 0.55095335081533125532, 16.320424340297897459, 0.71185853565756618266,
    0.790313573554438209, -2.6662979819930923142, -7.8560284439278742497, -13.270780066142890519,
    9.8723792015232163521, 2.964228835983377941, -2.6761327743945533264, 7.3081452639291653028,
    15.789275057376750411, -6.0852767250869463922, 2.7476458585515155519, 10.902971735295137634,
    1.2483195511503406561, -6.9951058565364379049, -18.144862108964247227, -6.1687800183791177133,
    1.1891887502282401101, 18.762905888576508318, 19.538445229741810749, 16.882547016418406827,
    -13.729207378553464736, 7.6169767838376767344, -4.3001282734492356497, 12.82375984995585938,
    6.3444583279548103505, 14.536561499268680464, 4.2676645735256322212, -13.043271215195256119,
    5.2999719581727653406, -3.7092838809330563876, -2.7567201545067216983, -5.4493532702108913313,
    13.706340145205974324, -9.953167168491519945, -21.827072358036257782, 1.8325733405664177411,
    6.9580038677754991738, -7.217583439181396443, -0.89356718477611229989, -20.196121323611055942,
    -8.8890210389301174132, -4.2981297828239641845, 1.6653861077773821631, 13.726085191171812028,
    16.082667621732866792, 5.4576363637230072001, 23.407539035708975206, 6.0709364725656165263,
    6.7457438241422504888, -0.8125017834879506573, 1.6638987826465445607, 0.68874384824323164889,
    6.7798070308317290866, -2.2243473396206359105, -0.38818426789780424713, -10.37563270555550865,
    -4.5653050418493865692, -3.6837218701967655576, 1.5062171246446884876, -3.7552280448133563695,
    7.7424763713355186923, -4.4315956440340658062, -1.7430340084964908165, 12.722632781803596913,
    -14.367982661796494526, 9.90652065556709438, -14.136507901592999303, 2.6916374782166436752,
    2.1652290660770234787, -4.5009846925595606848, 10.169019268709778459, -6.3732714498721545482,
    4.0739468778773089142, -3.7805955571599723086, -9.0609142825856672232, -5.3546708528157394014,
    -17.303172571370858179, -8.0058532549707237536, 2.8143970903323412003, -8.1533563484003739319,
    4.4608828174650057008, 3.0498760856107987038, -8.8841646445951756306, -19.909857282568339087,
    -4.9280484345931494516, 10.380412795491212208, -5.4128744096350613901, 28.895757280567341496,
    11.843186009703385864, -16.926143241617641166, 18.582006564459561559, 11.442503742102953623,
    -2.153075194424115324, -4.1683246403305886929, -12.587208527058635354, 2.2496260015994220538,
    -1.8379422665626654609, -0.3950609152895351861, 17.695022031733444123, -2.534678155010076761,
    -16.35368454834718932, 4.552050692854501257, 15.132460251700090126, -16.309632137042633815,
    -3.9168775277184710859, 1.148888727183005054, 13.972657173964147859, -24.902191048464953127,
    10.500061487499074531, 1.3147249464064736379, -4.2239974343158079861, -3.6222618206241747885,
    9.4228932108285068381, -12.797835672522083428, -12.249543899148715553, -10.465310360684402013,
    4.1702984613389917357, 12.788460137193926158, -7.7460246136030788122, -5.3661271210058005821,
    -8.3033233268067245803)
  val rois = Array(0, 0, 0, 7, 5, 1, 6, 2, 7, 5, 1, 3, 1, 6, 4, 0, 3, 3, 3, 3)

  "updateOutput Float type" should "work properly" in {
    val input = new Table
    input.insert(Tensor(Storage(data.map(x => x.toFloat))).resize(2, 2, 6, 8))
    input.insert(Tensor(Storage(rois.map(x => x.toFloat))).resize(4, 5))

    val roiPooling = new RoiPooling[Float](pooledW = 3, pooledH = 2, 1)
    val res = roiPooling.forward(input)
    val expectedRes = Array(19.429232983437231042, 16.586359188890380523, 16.586359188890380523,
      18.803346654854180997, 18.803346654854180997, 16.704283714127733163, 26.542527340702616101,
      19.435416458943819862, 16.320424340297897459, 19.538445229741810749, 16.882547016418406827,
      15.789275057376750411, 6.0709364725656165263, 6.7457438241422504888, 6.7457438241422504888,
      12.722632781803596913, 12.722632781803596913, 4.0739468778773089142, 2.2496260015994220538,
      2.2496260015994220538, -1.8379422665626654609, -3.6222618206241747885, 9.4228932108285068381,
      9.4228932108285068381, 16.082667621732866792, 23.407539035708975206, 23.407539035708975206,
      7.7424763713355186923, -0.38818426789780424713, 12.722632781803596913, 10.380412795491212208,
      10.380412795491212208, 28.895757280567341496, 10.500061487499074531, 15.132460251700090126,
      15.132460251700090126, 1.8852580346394267607, 1.8852580346394267607, 1.8852580346394267607,
      1.8852580346394267607, 1.8852580346394267607, 1.8852580346394267607, 9.8723792015232163521,
      9.8723792015232163521, 9.8723792015232163521, 9.8723792015232163521, 9.8723792015232163521,
      9.8723792015232163521)
    for (i <- expectedRes.indices) {
      TestUtils.conditionFailTest(Math.abs(res.storage().array()(i) - expectedRes(i)) < 1e-6)
    }
  }

  "updateOutput Double type" should "work properly" in {
    val input = new Table
    input.insert(Tensor(Storage(data)).resize(2, 2, 6, 8))
    input.insert(Tensor(Storage(rois.map(x => x.toDouble))).resize(4, 5))

    val roiPooling = new RoiPooling[Double](pooledW = 3, pooledH = 2, 1)
    val res = roiPooling.forward(input)
    val expectedRes = Array(19.429232983437231042, 16.586359188890380523, 16.586359188890380523,
      18.803346654854180997, 18.803346654854180997, 16.704283714127733163, 26.542527340702616101,
      19.435416458943819862, 16.320424340297897459, 19.538445229741810749, 16.882547016418406827,
      15.789275057376750411, 6.0709364725656165263, 6.7457438241422504888, 6.7457438241422504888,
      12.722632781803596913, 12.722632781803596913, 4.0739468778773089142, 2.2496260015994220538,
      2.2496260015994220538, -1.8379422665626654609, -3.6222618206241747885, 9.4228932108285068381,
      9.4228932108285068381, 16.082667621732866792, 23.407539035708975206, 23.407539035708975206,
      7.7424763713355186923, -0.38818426789780424713, 12.722632781803596913, 10.380412795491212208,
      10.380412795491212208, 28.895757280567341496, 10.500061487499074531, 15.132460251700090126,
      15.132460251700090126, 1.8852580346394267607, 1.8852580346394267607, 1.8852580346394267607,
      1.8852580346394267607, 1.8852580346394267607, 1.8852580346394267607, 9.8723792015232163521,
      9.8723792015232163521, 9.8723792015232163521, 9.8723792015232163521, 9.8723792015232163521,
      9.8723792015232163521)
    for (i <- expectedRes.indices) {
      TestUtils.conditionFailTest(Math.abs(res.storage().array()(i) - expectedRes(i)) < 1e-6)
    }
  }


  "updateGradInput Float" should "work properly " in {
    val input = new Table
    input.insert(Tensor(Storage(data.map(x => x.toFloat))).resize(2, 2, 6, 8))
    input.insert(Tensor(Storage(rois.map(x => x.toFloat))).resize(4, 5))

    val roiPooling = new RoiPooling[Float](pooledW = 3, pooledH = 2, 1)
    val res = roiPooling.forward(input)
    val gradOutputData = Array(0.55079787969589233398, 0.70814782381057739258,
      0.29090473055839538574, 0.51082760095596313477, 0.89294695854187011719,
      0.89629310369491577148, 0.12558531761169433594, 0.20724287629127502441,
      0.051467202603816986084, 0.44080984592437744141, 0.029876211658120155334,
      0.45683321356773376465, 0.64914405345916748047, 0.27848729491233825684,
      0.6762549281120300293, 0.59086281061172485352, 0.023981882259249687195,
      0.55885410308837890625, 0.25925245881080627441, 0.41510120034217834473,
      0.28352507948875427246, 0.69313794374465942383, 0.44045370817184448242,
      0.15686774253845214844, 0.54464900493621826172, 0.7803147435188293457,
      0.30636352300643920898, 0.22195787727832794189, 0.3879712522029876709,
      0.93638366460800170898, 0.97599542140960693359, 0.67238366603851318359,
      0.90283411741256713867, 0.84575086832046508789, 0.37799403071403503418,
      0.092217005789279937744, 0.65341091156005859375, 0.55784076452255249023,
      0.36156475543975830078, 0.22505450248718261719, 0.40651991963386535645,
      0.46894025802612304688, 0.2692355811595916748, 0.29179278016090393066,
      0.45768639445304870605, 0.86053389310836791992, 0.58625292778015136719,
      0.28348785638809204102)
    val gradOutput = Tensor(Storage(gradOutputData.map(x => x.toFloat)))
    val gradInputData = roiPooling.backward(input, gradOutput)[Tensor[Float]](1)
    val expectedGradInput = Tensor(Storage(Array(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
      0.55079787969589233398, 0, 0, 0, 0.99905252456665039062, 0, 0, 0, 0, 0,
      2.6733312606811523438, 0, 0, 0.89629310369491577148, 0, 0, 0, 1.4037744998931884766,
      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.20724287629127502441, 0, 0, 0, 0, 0,
      0, 0, 0, 0, 0, 0, 0, 0, 0.12558531761169433594, 0, 0, 0, 0, 0.051467202603816986084,
      0, 0, 0, 0, 0, 2.7489893436431884766, 0, 0, 0, 0.45683321356773376465, 0, 0, 0, 0, 0,
      0, 0, 0, 0, 0.44080984592437744141, 0.029876211658120155334, 0, 0, 0, 0, 0, 0, 0, 0, 0,
      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.54464900493621826172, 0,
      1.0866782665252685547, 0.64914405345916748047, 0.95474219322204589844, 0, 0, 0, 0,
      0, 0.3879712522029876709, 0, 0, 0, 0, 0, 0.22195787727832794189, 0, 0,
      1.5512282848358154297, 0, 0, 0, 0, 0, 0, 0, 0, 0.55885410308837890625, 0, 0, 0, 0,
      0, 0, 0, 0, 0, 0, 0, 0, 1.6483790874481201172, 0, 0.90283411741256713867, 0, 0, 0,
      0, 0, 0, 0, 0.67435365915298461914, 0.28352507948875427246, 0, 0, 0, 0, 0,
      0.470211029052734375, 0, 0, 0, 0, 0, 0.84575086832046508789, 0, 0, 0.69313794374465942383,
      0.59732145071029663086, 0, 0, 0, 0, 0, 0, 0, 0))).resize(2, 2, 6, 8)

    expectedGradInput.size() should be(gradInputData.size())
    (expectedGradInput.storage().array() zip gradInputData.storage().array()).foreach(x =>
      TestUtils.conditionFailTest(Math.abs(x._1 - x._2) < 1e-6))
    val gradOutputData2 = Array(0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
    val gradOutput2 = Tensor(Storage(gradOutputData2.map(x => x.toFloat)))
    val gradInputData2 = roiPooling.backward(input, gradOutput2)(1).
      asInstanceOf[Tensor[Float]].storage().array()
    val expectedGradInput2 = Array(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
      2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)

    for (i <- expectedGradInput2.indices) {
      TestUtils.conditionFailTest(expectedGradInput2.length == gradInputData2.length)
      TestUtils.conditionFailTest(Math.abs(expectedGradInput2(i) - gradInputData2(i)) < 1e-6)
    }
  }

  "updateGradInput Double" should "work properly " in {
    val input = new Table
    input.insert(Tensor(Storage(data)).resize(2, 2, 6, 8))
    input.insert(Tensor(Storage(rois.map(x => x.toDouble))).resize(4, 5))

    val roiPooling = new RoiPooling[Double](pooledW = 3, pooledH = 2, 1)
    val res = roiPooling.forward(input)
    val gradOutputData = Array(0.55079787969589233398, 0.70814782381057739258,
      0.29090473055839538574, 0.51082760095596313477, 0.89294695854187011719,
      0.89629310369491577148, 0.12558531761169433594, 0.20724287629127502441,
      0.051467202603816986084, 0.44080984592437744141, 0.029876211658120155334,
      0.45683321356773376465, 0.64914405345916748047, 0.27848729491233825684,
      0.6762549281120300293, 0.59086281061172485352, 0.023981882259249687195,
      0.55885410308837890625, 0.25925245881080627441, 0.41510120034217834473,
      0.28352507948875427246, 0.69313794374465942383, 0.44045370817184448242,
      0.15686774253845214844, 0.54464900493621826172, 0.7803147435188293457,
      0.30636352300643920898, 0.22195787727832794189, 0.3879712522029876709,
      0.93638366460800170898, 0.97599542140960693359, 0.67238366603851318359,
      0.90283411741256713867, 0.84575086832046508789, 0.37799403071403503418,
      0.092217005789279937744, 0.65341091156005859375, 0.55784076452255249023,
      0.36156475543975830078, 0.22505450248718261719, 0.40651991963386535645,
      0.46894025802612304688, 0.2692355811595916748, 0.29179278016090393066,
      0.45768639445304870605, 0.86053389310836791992, 0.58625292778015136719,
      0.28348785638809204102)
    val gradOutput = Tensor(Storage(gradOutputData))
    val gradInputData = roiPooling.backward(input, gradOutput)[Tensor[Double]](1)
    val expectedGradInput = Tensor(Storage(Array(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
      0.55079787969589233398, 0, 0, 0, 0.99905252456665039062, 0, 0, 0, 0, 0,
      2.6733312606811523438, 0, 0, 0.89629310369491577148, 0, 0, 0, 1.4037744998931884766,
      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.20724287629127502441, 0, 0, 0, 0, 0,
      0, 0, 0, 0, 0, 0, 0, 0, 0.12558531761169433594, 0, 0, 0, 0, 0.051467202603816986084,
      0, 0, 0, 0, 0, 2.7489893436431884766, 0, 0, 0, 0.45683321356773376465, 0, 0, 0, 0, 0,
      0, 0, 0, 0, 0.44080984592437744141, 0.029876211658120155334, 0, 0, 0, 0, 0, 0, 0, 0, 0,
      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.54464900493621826172, 0,
      1.0866782665252685547, 0.64914405345916748047, 0.95474219322204589844, 0, 0, 0, 0,
      0, 0.3879712522029876709, 0, 0, 0, 0, 0, 0.22195787727832794189, 0, 0,
      1.5512282848358154297, 0, 0, 0, 0, 0, 0, 0, 0, 0.55885410308837890625, 0, 0, 0, 0,
      0, 0, 0, 0, 0, 0, 0, 0, 1.6483790874481201172, 0, 0.90283411741256713867, 0, 0, 0,
      0, 0, 0, 0, 0.67435365915298461914, 0.28352507948875427246, 0, 0, 0, 0, 0,
      0.470211029052734375, 0, 0, 0, 0, 0, 0.84575086832046508789, 0, 0, 0.69313794374465942383,
      0.59732145071029663086, 0, 0, 0, 0, 0, 0, 0, 0))).resize(2, 2, 6, 8)

    expectedGradInput.size() should be(gradInputData.size())
    (expectedGradInput.storage().array() zip gradInputData.storage().array()).foreach(x =>
      TestUtils.conditionFailTest(Math.abs(x._1 - x._2) < 1e-6))
    val gradOutputData2 = Array(0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
    val gradOutput2 = Tensor(Storage(gradOutputData2.map(x => x.toDouble)))
    val gradInputData2 = roiPooling.backward(input, gradOutput2)(1).
      asInstanceOf[Tensor[Double]].storage().array()
    val expectedGradInput2 = Array(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
      2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)

    for (i <- expectedGradInput2.indices) {
      TestUtils.conditionFailTest(expectedGradInput2.length == gradInputData2.length)
      TestUtils.conditionFailTest(Math.abs(expectedGradInput2(i) - gradInputData2(i)) < 1e-6)
    }
  }
}

class RoiPoolingSerialTest extends ModuleSerializationTest {
  override def test(): Unit = {
    val input = T()
    val input1 = Tensor[Float](1, 1, 2, 2).apply1(_ => Random.nextFloat())
    val input2 = Tensor[Float](1, 5).apply1(_ => Random.nextFloat())
    input(1.0f) = input1
    input(2.0f) = input2
    val roiPooling = new RoiPooling[Float](pooledW = 3,
      pooledH = 2, 1.0f).setName("roiPooling")
    runSerializationTest(roiPooling, input)
  }
}
