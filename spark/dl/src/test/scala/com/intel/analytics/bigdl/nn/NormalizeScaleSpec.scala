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

import com.intel.analytics.bigdl.optim.L2Regularizer
import com.intel.analytics.bigdl.tensor.{Storage, Tensor}
import org.scalatest.{FlatSpec, Matchers}

class NormalizeScaleSpec extends FlatSpec with Matchers {
  "Normalize with 4d" should "work properly" in {
    val input = Tensor(Storage(Array(
      0.5507978797, 0.7081478238, 0.2909047306, 0.5108276010,
      0.8929469585, 0.8962931037, 0.1255853176, 0.2072428763,
      0.0514672026, 0.4408098459, 0.0298762117, 0.4568332136,
      0.6491440535, 0.2784872949, 0.6762549281, 0.5908628106,
      0.0239818823, 0.5588541031, 0.2592524588, 0.4151012003,
      0.2835250795, 0.6931379437, 0.4404537082, 0.1568677425,
      0.5446490049, 0.7803147435, 0.3063635230, 0.2219578773,
      0.3879712522, 0.9363836646, 0.9759954214, 0.6723836660,
      0.9028341174, 0.8457508683, 0.3779940307, 0.0922170058)
      .map(x => x.toFloat))).resize(2, 3, 2, 3)

    val expectedOutput = Tensor(Storage(Array(
      12.8011369705, 17.9583473206, 7.8839497566, 11.3913326263,
      19.9816169739, 15.5767812729, 2.9187383652, 5.2555966377,
      1.3948376179, 9.8299531937, 0.6685447693, 7.9393572807,
      15.0868082047, 7.0623264313, 18.3275127411, 13.1760978699,
      0.5366464257, 9.7123899460, 4.5191359520, 7.4756622314,
      5.7009272575, 12.4241008759, 12.6179037094, 3.2889757156,
      9.4940004349, 14.0528850555, 6.1601471901, 3.9784679413,
      11.1144113541, 19.6327362061, 17.0129756927, 12.1091270447,
      18.1535682678, 15.1595993042, 10.8285894394, 1.9334726334)
      .map(x => x.toFloat))).resize(2, 3, 2, 3)


    val normalizer = Normalize[Float](2)
    val output = normalizer.forward(input)
    val mul = CMul[Float](size = Array(1, 3, 1, 1))
    mul.weight.setValue(1, 1, 1, 1, 20)
    mul.weight.setValue(1, 2, 1, 1, 20)
    mul.weight.setValue(1, 3, 1, 1, 20)
    mul.forward(output) should be(expectedOutput)
  }

  "normalize with more data" should "work properly" in {
    val input = Tensor(Storage(Array(
      0.5507978797, 0.7081478238, 0.2909047306, 0.5108276010,
      0.8929469585, 0.8962931037, 0.1255853176, 0.2072428763,
      0.0514672026, 0.4408098459, 0.0298762117, 0.4568332136,
      0.6491440535, 0.2784872949, 0.6762549281, 0.5908628106,
      0.0239818823, 0.5588541031, 0.2592524588, 0.4151012003,
      0.2835250795, 0.6931379437, 0.4404537082, 0.1568677425,
      0.5446490049, 0.7803147435, 0.3063635230, 0.2219578773,
      0.3879712522, 0.9363836646, 0.9759954214, 0.6723836660,
      0.9028341174, 0.8457508683, 0.3779940307, 0.0922170058,
      0.6534109116, 0.5578407645, 0.3615647554, 0.2250545025,
      0.4065199196, 0.4689402580, 0.2692355812, 0.2917927802,
      0.4576863945, 0.8605338931, 0.5862529278, 0.2834878564,
      0.2779774964, 0.4546220899, 0.2054103464, 0.2013787180,
      0.5140350461, 0.0872293711, 0.4835855365, 0.3621762097,
      0.7076866031, 0.7467462420, 0.6910929084, 0.6891804338,
      0.3736001253, 0.6681348085, 0.3398486674, 0.5727938414,
      0.3258071542, 0.4451450408, 0.0615289323, 0.2426754236,
      0.9716026187, 0.2305842042, 0.6914775372, 0.6504768729,
      0.7239391208, 0.4750885963, 0.5966637731, 0.0669694245,
      0.0725621358, 0.1989760250, 0.1518609971, 0.1001043469,
      0.1292938590, 0.5532777309, 0.1878148317, 0.9521012306,
      0.6816117764, 0.5410196781, 0.7071806192, 0.2638866603,
      0.9267256856, 0.8391930461)
      .map(x => x.toFloat))).reshape(Array(2, 5, 3, 3))

    val expectedOutput = Tensor(Storage(Array(
      10.7962512970, 13.2859182358, 4.9504461288, 6.9418644905,
      13.7528429031, 11.7681846619, 2.0863511562, 4.4173331261,
      1.3001513481, 8.6403636932, 0.5605226755, 7.7741193771,
      8.8215084076, 4.2891592979, 8.8791189194, 9.8160142899,
      0.5111681819, 14.1176309586, 5.0816369057, 7.7879228592,
      4.8248634338, 9.4193611145, 6.7837071419, 2.0596482754,
      9.0482635498, 16.6322250366, 7.7392773628, 4.3506212234,
      7.2789239883, 15.9348278046, 13.2632369995, 10.3558073044,
      11.8540668488, 14.0504741669, 8.0568532944, 2.3295626640,
      12.8075809479, 10.4659318924, 6.1528968811, 3.0583662987,
      6.2610712051, 6.1571102142, 4.4728155136, 6.2194943428,
      11.5619573593, 11.7851772308, 12.7779006958, 3.9678306580,
      5.2822084427, 8.9109497070, 2.9043090343, 7.0540165901,
      7.1670479774, 1.5497876406, 6.6227970123, 7.8939504623,
      9.9051179886, 14.1898860931, 13.5459632874, 9.7443618774,
      13.0866928101, 9.3156175613, 6.0380268097, 7.8445219994,
      7.1012549400, 6.2304615974, 1.1691904068, 4.7566289902,
      13.7375459671, 8.0770444870, 9.6410789490, 11.5568981171,
      9.9144859314, 10.3549757004, 8.3511896133, 1.2725722790,
      1.4222748280, 2.8133335114, 5.3194799423, 1.3957271576,
      2.2971394062, 7.5772452354, 4.0935902596, 13.3260612488,
      12.9521827698, 10.6044101715, 9.9988679886, 9.2435827255,
      12.9210777283, 14.9097824097)
      .map(x => x.toFloat))).resize(2, 5, 3, 3)


    val normalizer = new NormalizeScale[Float](2, scale = 20, size = Array(1, 5, 1, 1))
    val output = normalizer.forward(input)

    output should be(expectedOutput)
  }

  "A Normalize Module" should "generate correct gradInput with data" in {
    var input = Tensor(Storage(Array(
      0.5507978797, 0.7081478238, 0.2909047306, 0.5108276010,
      0.8929469585, 0.8962931037, 0.1255853176, 0.2072428763,
      0.0514672026, 0.4408098459, 0.0298762117, 0.4568332136,
      0.6491440535, 0.2784872949, 0.6762549281, 0.5908628106,
      0.0239818823, 0.5588541031, 0.2592524588, 0.4151012003,
      0.2835250795, 0.6931379437, 0.4404537082, 0.1568677425,
      0.5446490049, 0.7803147435, 0.3063635230, 0.2219578773,
      0.3879712522, 0.9363836646, 0.9759954214, 0.6723836660,
      0.9028341174, 0.8457508683, 0.3779940307, 0.0922170058,
      0.6534109116, 0.5578407645, 0.3615647554, 0.2250545025,
      0.4065199196, 0.4689402580, 0.2692355812, 0.2917927802,
      0.4576863945, 0.8605338931, 0.5862529278, 0.2834878564,
      0.2779774964, 0.4546220899, 0.2054103464, 0.2013787180,
      0.5140350461, 0.0872293711, 0.4835855365, 0.3621762097,
      0.7076866031, 0.7467462420, 0.6910929084, 0.6891804338,
      0.3736001253, 0.6681348085, 0.3398486674, 0.5727938414,
      0.3258071542, 0.4451450408, 0.0615289323, 0.2426754236,
      0.9716026187, 0.2305842042, 0.6914775372, 0.6504768729,
      0.7239391208, 0.4750885963, 0.5966637731, 0.0669694245,
      0.0725621358, 0.1989760250, 0.1518609971, 0.1001043469,
      0.1292938590, 0.5532777309, 0.1878148317, 0.9521012306,
      0.6816117764, 0.5410196781, 0.7071806192, 0.2638866603,
      0.9267256856, 0.8391930461)
      .map(x => x.toFloat))).reshape(Array(2, 5, 3, 3))

    var gradOut = Tensor(Storage(Array(
      0.7263194919, 0.4802399576, 0.8421031833, 0.7447523475,
      0.6603258848, 0.9139752388, 0.6336655617, 0.3659405708,
      0.5528445840, 0.1963805705, 0.1920723021, 0.7256696224,
      0.7849367261, 0.9720983505, 0.8509714007, 0.5435943007,
      0.0897908732, 0.4888732433, 0.9279363751, 0.7876182199,
      0.4850942194, 0.4552793503, 0.2179857641, 0.1772133857,
      0.0736236721, 0.8923931718, 0.6401765943, 0.1433323175,
      0.4141269326, 0.0491089262, 0.2093733549, 0.7307081223,
      0.6511227489, 0.4789783061, 0.2747805119, 0.6522231102,
      0.9564495087, 0.4355205595, 0.0701325014, 0.0577314869,
      0.0828710198, 0.9597072005, 0.5407608151, 0.8374624252,
      0.1700335443, 0.2603450716, 0.6919775009, 0.8955703378,
      0.3406884968, 0.0646732002, 0.8641196489, 0.2908724546,
      0.7410824299, 0.1580336541, 0.6949634552, 0.8414196372,
      0.7271520495, 0.3591075242, 0.7266897559, 0.1394671202,
      0.3138191104, 0.4195827544, 0.8772120476, 0.1537402123,
      0.8801248074, 0.7989643216, 0.9716243148, 0.3677029908,
      0.2049397677, 0.2405703217, 0.8278627992, 0.9652281404,
      0.6988099813, 0.4824970365, 0.2870497704, 0.8336879015,
      0.8721795082, 0.0921315923, 0.2159494758, 0.8317610621,
      0.8483039141, 0.3146530092, 0.2792946100, 0.4308150113,
      0.5394464731, 0.0955668166, 0.8369121552, 0.5347348452,
      0.7749677896, 0.2308362722)
      .map(x => x.toFloat))).reshape(Array(2, 5, 3, 3))

    val expectedOutput = Tensor(Storage(Array(
      10.7962512970, 13.2859182358, 4.9504461288, 6.9418644905,
      13.7528429031, 11.7681846619, 2.0863511562, 4.4173331261,
      1.3001513481, 8.6403636932, 0.5605226755, 7.7741193771,
      8.8215084076, 4.2891592979, 8.8791189194, 9.8160142899,
      0.5111681819, 14.1176309586, 5.0816369057, 7.7879228592,
      4.8248634338, 9.4193611145, 6.7837071419, 2.0596482754,
      9.0482635498, 16.6322250366, 7.7392773628, 4.3506212234,
      7.2789239883, 15.9348278046, 13.2632369995, 10.3558073044,
      11.8540668488, 14.0504741669, 8.0568532944, 2.3295626640,
      12.8075809479, 10.4659318924, 6.1528968811, 3.0583662987,
      6.2610712051, 6.1571102142, 4.4728155136, 6.2194943428,
      11.5619573593, 11.7851772308, 12.7779006958, 3.9678306580,
      5.2822084427, 8.9109497070, 2.9043090343, 7.0540165901,
      7.1670479774, 1.5497876406, 6.6227970123, 7.8939504623,
      9.9051179886, 14.1898860931, 13.5459632874, 9.7443618774,
      13.0866928101, 9.3156175613, 6.0380268097, 7.8445219994,
      7.1012549400, 6.2304615974, 1.1691904068, 4.7566289902,
      13.7375459671, 8.0770444870, 9.6410789490, 11.5568981171,
      9.9144859314, 10.3549757004, 8.3511896133, 1.2725722790,
      1.4222748280, 2.8133335114, 5.3194799423, 1.3957271576,
      2.2971394062, 7.5772452354, 4.0935902596, 13.3260612488,
      12.9521827698, 10.6044101715, 9.9988679886, 9.2435827255,
      12.9210777283, 14.9097824097)
      .map(x => x.toFloat))).resize(2, 5, 3, 3)

    var expectedGradInput = Tensor(Storage(Array(
      -0.1148514375, -3.5743882656, 11.5156641006, 5.5605020523,
      -1.9116518497, -0.4783028662, 9.0997104645, 2.1677801609,
      12.6471023560, -7.6364088058, 3.0726387501, 7.9287652969,
      4.8718037605, 11.2039165497, 1.7579507828, 2.3150684834,
      1.2621252537, -1.9694392681, 11.4335355759, 7.4001874924,
      5.5116991997, -0.0008006793, -2.6020886898, 0.1427903473,
      -4.9673018456, -2.1851525307, 8.3221836090, -2.9738335609,
      0.8750523329, -8.2246103287, -5.8676581383, 2.1566159725,
      -4.0205812454, -1.6554248333, -4.4157114029, 14.1134653091,
      1.7222809792, -1.7423020601, -2.3049762249, -1.2245724201,
      -4.2239460945, 6.0719752312, 5.9235711098, 9.9203786850,
      -7.4317178726, -3.7739505768, -4.3251948357, 9.2204341888,
      2.4390163422, -5.0301928520, 10.6459598541, 1.4211689234,
      3.2438371181, 1.2873532772, 5.3931946754, 6.3499174118,
      1.9036595821, -4.0151600838, 4.6700425148, -3.3019390106,
      -5.2732696533, -3.3638772964, 9.6617259979, -2.7798123360,
      8.3975009918, 5.9782786369, 17.5699920654, 3.8455066681,
      -4.5374197960, -1.6124024391, 2.0067462921, 5.8112678528,
      3.3959164619, -5.2109961510, -2.9581990242, 14.8699150085,
      16.0902061462, -0.2199868113, 0.9526393414, 10.2165107727,
      12.8180732727, -0.4096293151, -0.1299941391, -5.1015834808,
      0.3571199179, -5.6215124130, 6.4215326309, 7.2418398857,
      -1.9749751091, -10.5258388519)
      .map(x => x.toFloat))).reshape(Array(2, 5, 3, 3))

    val module = NormalizeScale[Float](2, scale = 20, size = Array(1, 5, 1, 1))

    val out = module.forward(input)
    out should be(expectedOutput)
    val gradInput = module.backward(input, gradOut)
    gradInput.map(expectedGradInput, (a, b) => {
      assert(Math.abs(a - b) < 1e-5)
      a
    })

    input = Tensor(Storage(Array(
      0.9652933478, 0.7510272861, 0.3430938721, 0.9485276341,
      0.7005117536, 0.8405610919, 0.0454973057, 0.0556415394,
      0.7427372932, 0.3046864271, 0.5167843699, 0.1562624276,
      0.9779524207, 0.5027510524, 0.8290010691, 0.0740377977,
      0.4789154530, 0.0622794814, 0.8842414021, 0.4458101690,
      0.0685499161, 0.0764962807, 0.5387926698, 0.0755664036,
      0.1837723106, 0.4363570809, 0.4977828264, 0.5833119154,
      0.6205126643, 0.3728114963, 0.6187365651, 0.1572446525,
      0.2755084634, 0.7987182736, 0.1530892998, 0.2233229727,
      0.2429781854, 0.4795072973, 0.0007455220, 0.0303113610,
      0.4615481496, 0.1625206918, 0.6795018315, 0.7952045798,
      0.5781633854, 0.6947649717, 0.3909580112, 0.0462962016,
      0.4394215345, 0.3719803095, 0.5970032215, 0.1342181712,
      0.2277128994, 0.8147824407, 0.2643255293, 0.4103201926,
      0.9359721541, 0.2755524516, 0.1452899575, 0.7020201087,
      0.5670665503, 0.6115938425, 0.0420308523, 0.4172669053,
      0.0042664343, 0.2465354651, 0.7060561776, 0.0615407154,
      0.2946934998, 0.9881127477, 0.9712222219, 0.4818463922,
      0.7356933951, 0.6326557994, 0.4656930566, 0.8570664525,
      0.1725182533, 0.8284319043, 0.0420695245, 0.8669536710,
      0.6759966016, 0.4459141195, 0.6715702415, 0.4603685141,
      0.7800032496, 0.1134440824, 0.5081574321, 0.8714895248,
      0.9275292754, 0.2203363329)
      .map(x => x.toFloat))).reshape(Array(2, 5, 3, 3))

    gradOut = Tensor(Storage(Array(
      0.2519302964, 0.9040125012, 0.3455173373, 0.0568725727,
      0.7148866653, 0.4144188464, 0.7353242040, 0.4770916402,
      0.5005655289, 0.4175619185, 0.2904849052, 0.9007108808,
      0.6687875986, 0.7888323665, 0.8957395554, 0.2501889467,
      0.9198206067, 0.8626050949, 0.5060852766, 0.5679021478,
      0.1149322391, 0.7621158361, 0.1732250005, 0.5338360667,
      0.1264373064, 0.0729170963, 0.6090274453, 0.4912031293,
      0.6748099327, 0.7908541560, 0.9826874733, 0.0840806812,
      0.6312121153, 0.5769748688, 0.6506421566, 0.5431794524,
      0.5573673844, 0.8685546517, 0.4520116150, 0.5349755883,
      0.6909636855, 0.0894949883, 0.4319253266, 0.1021342203,
      0.1378098875, 0.0021255584, 0.2097064406, 0.2166372687,
      0.5162444711, 0.5702082515, 0.8239611983, 0.4867111742,
      0.8914545774, 0.3950607777, 0.7446114421, 0.5527572632,
      0.6238250732, 0.2703920007, 0.9375950098, 0.7696546912,
      0.1455950290, 0.5374343991, 0.9957863092, 0.0490563326,
      0.3028198481, 0.7408133745, 0.0393614471, 0.7466710806,
      0.5734212995, 0.7317056656, 0.5508325696, 0.9394034743,
      0.3464016914, 0.5744694471, 0.0507799797, 0.5534328222,
      0.4193929434, 0.9831618071, 0.9456073046, 0.1382794082,
      0.0416156426, 0.1206750646, 0.1927469671, 0.7261679769,
      0.5439779758, 0.5349253416, 0.4645369649, 0.0066942186,
      0.3924719095, 0.9989384413)
      .map(x => x.toFloat))).reshape(Array(2, 5, 3, 3))

    val expectedOut = Tensor(Storage(Array(
      12.9988918304, 11.7118844986, 12.8350353241, 12.6591348648,
      12.4246129990, 13.7180128098, 0.8518700600, 1.0714917183,
      13.6318149567, 4.1029872894, 8.0589866638, 5.8457288742,
      13.0518417358, 8.9170351028, 13.5293521881, 1.3862485886,
      9.2224969864, 1.1430453062, 11.9074258804, 6.9521808624,
      2.5644311905, 1.0209262371, 9.5562858582, 1.2332487106,
      3.4408659935, 8.4029483795, 9.1360473633, 7.8550300598,
      9.6765766144, 13.9467630386, 8.2577142715, 2.7889668941,
      4.4963164330, 14.9548234940, 2.9480478764, 4.0987539291,
      3.2720077038, 7.4776701927, 0.0278897490, 0.4045381546,
      8.1862401962, 2.6523485184, 12.7226715088, 15.3132915497,
      10.6113109589, 11.4732933044, 7.2211399078, 0.7917130589,
      6.0494828224, 16.3954753876, 8.7067680359, 1.8625209332,
      2.6377274990, 13.7559747696, 4.3650503159, 7.5787663460,
      16.0060958862, 3.7935094833, 6.4038276672, 10.2383470535,
      7.8690776825, 7.0844383240, 0.7096070051, 6.8907122612,
      0.0788026229, 4.2160124779, 9.7202215195, 2.7124800682,
      4.2978463173, 13.7118587494, 11.2502174377, 8.1350145340,
      12.1491813660, 11.6853866577, 7.9638342857, 11.7991685867,
      7.6039476395, 12.0819520950, 0.5837910175, 10.0424156189,
      11.4128532410, 7.3637895584, 12.4041509628, 7.8727793694,
      10.7382450104, 5.0001831055, 7.4110307693, 12.0935001373,
      10.7440967560, 3.7199389935)
      .map(x => x.toFloat))).reshape(Array(2, 5, 3, 3))

    expectedGradInput = Tensor(Storage(Array(
      -3.9140403271, 0.4445342720, -12.3271293640, -7.0794482231,
      -0.2464490235, -5.2912111282, 13.1485214233, 8.5121822357,
      -1.4850496054, 3.3167335987, -4.8647637367, 22.1938934326,
      0.8440631032, 4.7142181396, 2.7297582626, 3.6765637398,
      11.9015321732, 14.9369316101, 0.1219845936, 0.7516693473,
      -0.7459176183, 9.5391139984, -6.8695392609, 7.6285362244,
      -0.1342885494, -3.8908934593, 4.0252819061, 2.1994044781,
      -0.7571096420, 2.1454718113, 8.0018997192, -1.4102233648,
      6.3503351212, -0.0697237849, 10.6717529297, 6.7603731155,
      5.6664791107, 4.8276038170, 16.8547420502, 6.8893437386,
      3.7386598587, -0.8701580763, -1.1627024412, -7.6827645302,
      -5.7781653404, -4.0904965401, -1.0719879866, 3.0480358601,
      3.5878252983, -16.8572063446, 1.5917342901, 5.9322166443,
      8.9542407990, -3.7660286427, 10.7268571854, 5.0193696022,
      -2.6081981659, 1.5156005621, 24.9250202179, -1.0341093540,
      -1.4516141415, 2.5405220985, 16.2735252380, -1.6676687002,
      5.5392270088, 9.1717061996, -5.1128301620, 25.9635906219,
      3.2168364525, 4.1037721634, 0.5289410949, 9.6884002686,
      1.3518137932, 2.6080131531, -5.7372117043, 0.7549284697,
      -0.9889311790, -0.1277296096, 12.8644371033, -3.6216723919,
      -7.9556565285, -0.6550734639, -4.9347825050, 5.8881497383,
      1.2419538498, 10.7716960907, -2.0987062454, -5.2430224419,
      -1.0421879292, 14.0429840088)
      .map(x => x.toFloat))).reshape(Array(2, 5, 3, 3))

    val out2 = module.forward(input)
    val gradInput2 = module.backward(input, gradOut)

    out2 should be(expectedOut)
    gradInput2.map(expectedGradInput, (a, b) => {
      assert(Math.abs(a - b) < 1e-5)
      a
    })
  }

  "A NormalizeScale zeroGrad" should "work" in {
    val input = Tensor[Float](Array(2, 5, 3, 3)).randn()

    val gradOut = Tensor[Float](Array(2, 5, 3, 3)).randn()

    val module = NormalizeScale[Float](2, scale = 20, size = Array(1, 5, 1, 1))

    val out = module.forward(input)
    val gradInput = module.backward(input, gradOut)

    println(module.parameters()._2(0))

    module.zeroGradParameters()
    module.parameters()._2(0).apply1(x => {
      assert(x == 0); x
    })
  }
}
