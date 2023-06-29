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

import com.intel.analytics.bigdl.dllib.nn.mkldnn.Equivalent
import com.intel.analytics.bigdl.dllib.tensor.{Storage, Tensor}
import com.intel.analytics.bigdl.dllib.utils.RandomGenerator._
import com.intel.analytics.bigdl.dllib.utils.serializer.ModuleSerializationTest
import com.intel.analytics.bigdl.dllib.utils.{T, Table, TestUtils}
import org.scalatest.{FlatSpec, Matchers}

import java.security.SecureRandom

class RoiAlignSpec extends FlatSpec with Matchers {
  "updateOutput Float type" should "work properly" in {
    val spatio_scale: Float = 1.0f
    val sampling_ratio: Int = 3
    val pooled_height: Int = 2
    val pooled_width: Int = 2

    val data = Array(
      0.327660024166107178, 0.783334434032440186, 0.359168112277984619,
      0.934897661209106445, 0.650066614151000977, 0.834474444389343262,
      0.424300372600555420, 0.149160504341125488, 0.730795919895172119,
      0.484096407890319824, 0.994338274002075195, 0.250495135784149170,
      0.259522974491119385, 0.887678027153015137, 0.194342792034149170,
      0.610941588878631592, 0.416747927665710449, 0.705707132816314697,
      0.435783147811889648, 0.778170645236968994, 0.193895518779754639,
      0.849628329277038574, 0.882959723472595215, 0.721439063549041748,
      0.832545340061187744, 0.774163544178009033, 0.781816542148590088,
      0.729343354701995850, 0.203778445720672607, 0.198633491992950439,
      0.781321287155151367, 0.118729889392852783, 0.643143951892852783,
      0.760397315025329590, 0.285254061222076416, 0.553620159626007080,
      0.232052326202392578, 0.728380799293518066, 0.775489747524261475,
      0.928656220436096191, 0.163158237934112549, 0.718611896038055420,
      0.744661569595336914, 0.593953251838684082, 0.372228324413299561,
      0.902524888515472412, 0.278600215911865234, 0.506435513496398926,
      0.818576753139495850, 0.757465600967407227, 0.705808222293853760,
      0.710981726646423340, 0.963726997375488281, 0.164355456829071045,
      0.780107796192169189, 0.850457072257995605, 0.839718520641326904,
      0.593321025371551514, 0.280547201633453369, 0.348339796066284180,
      0.423507034778594971, 0.949673593044281006, 0.518748283386230469,
      0.845408916473388672, 0.901987016201019287, 0.058945477008819580,
      0.631618440151214600, 0.488164126873016357, 0.698010146617889404,
      0.215178430080413818, 0.665156781673431396, 0.499578237533569336,
      0.863550186157226562, 0.088476061820983887, 0.177395820617675781,
      0.397035181522369385, 0.484034657478332520, 0.105176448822021484,
      0.095181167125701904, 0.111114203929901123, 0.715093195438385010,
      0.993503451347351074, 0.484178066253662109, 0.422980725765228271,
      0.192607104778289795, 0.983097016811370850, 0.638218641281127930,
      0.158814728260040283, 0.990248799324035645, 0.539387941360473633,
      0.657688558101654053, 0.316274046897888184, 0.851949751377105713,
      0.227342128753662109, 0.238007068634033203, 0.980791330337524414)

    val rois = Array(
      0, 0, 7, 5,
      6, 2, 7, 5,
      3, 1, 6, 4,
      3, 3, 3, 3)

    val input = new Table
    input.insert(Tensor(Storage(data.map(x => x.toFloat))).resize(1, 2, 6, 8))
    input.insert(Tensor(Storage(rois.map(x => x.toFloat))).resize(4, 4))

    val roiAlign = RoiAlign[Float](spatio_scale, sampling_ratio, pooled_height, pooled_width, "avg",
      aligned = false)
    val res = roiAlign.forward(input)
    val expectedRes = Array(
      0.614743709564208984, 0.550280153751373291,
      0.648947238922119141, 0.494060248136520386,
      0.514606714248657227, 0.596958041191101074,
      0.494195610284805298, 0.408652573823928833,
      0.707817792892456055, 0.494023799896240234,
      0.637864947319030762, 0.692903101444244385,
      0.308963924646377563, 0.266039490699768066,
      0.451879233121871948, 0.436514317989349365,
      0.393088847398757935, 0.704402685165405273,
      0.384622871875762939, 0.530835568904876709,
      0.525619626045227051, 0.501667082309722900,
      0.407763212919235229, 0.379031181335449219,
      0.566771149635314941, 0.329488337039947510,
      0.504409193992614746, 0.318125635385513306,
      0.405435621738433838, 0.409263730049133301,
      0.378736764192581177, 0.303221583366394043
    )

    for (i <- expectedRes.indices) {
      TestUtils.conditionFailTest(Math.abs(res.storage().array()(i) - expectedRes(i)) < 1e-6)
    }
  }

  "updateOutput Double type" should "work properly" in {
    val spatio_scale: Float = 1.0f
    val sampling_ratio: Int = 3
    val pooled_height: Int = 2
    val pooled_width: Int = 2

    val data = Array(
      0.327660024166107178, 0.783334434032440186, 0.359168112277984619,
      0.934897661209106445, 0.650066614151000977, 0.834474444389343262,
      0.424300372600555420, 0.149160504341125488, 0.730795919895172119,
      0.484096407890319824, 0.994338274002075195, 0.250495135784149170,
      0.259522974491119385, 0.887678027153015137, 0.194342792034149170,
      0.610941588878631592, 0.416747927665710449, 0.705707132816314697,
      0.435783147811889648, 0.778170645236968994, 0.193895518779754639,
      0.849628329277038574, 0.882959723472595215, 0.721439063549041748,
      0.832545340061187744, 0.774163544178009033, 0.781816542148590088,
      0.729343354701995850, 0.203778445720672607, 0.198633491992950439,
      0.781321287155151367, 0.118729889392852783, 0.643143951892852783,
      0.760397315025329590, 0.285254061222076416, 0.553620159626007080,
      0.232052326202392578, 0.728380799293518066, 0.775489747524261475,
      0.928656220436096191, 0.163158237934112549, 0.718611896038055420,
      0.744661569595336914, 0.593953251838684082, 0.372228324413299561,
      0.902524888515472412, 0.278600215911865234, 0.506435513496398926,
      0.818576753139495850, 0.757465600967407227, 0.705808222293853760,
      0.710981726646423340, 0.963726997375488281, 0.164355456829071045,
      0.780107796192169189, 0.850457072257995605, 0.839718520641326904,
      0.593321025371551514, 0.280547201633453369, 0.348339796066284180,
      0.423507034778594971, 0.949673593044281006, 0.518748283386230469,
      0.845408916473388672, 0.901987016201019287, 0.058945477008819580,
      0.631618440151214600, 0.488164126873016357, 0.698010146617889404,
      0.215178430080413818, 0.665156781673431396, 0.499578237533569336,
      0.863550186157226562, 0.088476061820983887, 0.177395820617675781,
      0.397035181522369385, 0.484034657478332520, 0.105176448822021484,
      0.095181167125701904, 0.111114203929901123, 0.715093195438385010,
      0.993503451347351074, 0.484178066253662109, 0.422980725765228271,
      0.192607104778289795, 0.983097016811370850, 0.638218641281127930,
      0.158814728260040283, 0.990248799324035645, 0.539387941360473633,
      0.657688558101654053, 0.316274046897888184, 0.851949751377105713,
      0.227342128753662109, 0.238007068634033203, 0.980791330337524414)

    val rois = Array(
      0, 0, 7, 5,
      6, 2, 7, 5,
      3, 1, 6, 4,
      3, 3, 3, 3)

    val input = new Table
    input.insert(Tensor(Storage(data.map(x => x))).resize(1, 2, 6, 8))
    input.insert(Tensor(Storage(rois.map(x => x.toDouble))).resize(4, 4))

    val roiAlign = RoiAlign[Double](
      spatio_scale, sampling_ratio, pooled_height, pooled_width, "avg", aligned = false)
    val res = roiAlign.forward(input)
    val expectedRes = Array(
      0.614743709564208984, 0.550280153751373291,
      0.648947238922119141, 0.494060248136520386,
      0.514606714248657227, 0.596958041191101074,
      0.494195610284805298, 0.408652573823928833,
      0.707817792892456055, 0.494023799896240234,
      0.637864947319030762, 0.692903101444244385,
      0.308963924646377563, 0.266039490699768066,
      0.451879233121871948, 0.436514317989349365,
      0.393088847398757935, 0.704402685165405273,
      0.384622871875762939, 0.530835568904876709,
      0.525619626045227051, 0.501667082309722900,
      0.407763212919235229, 0.379031181335449219,
      0.566771149635314941, 0.329488337039947510,
      0.504409193992614746, 0.318125635385513306,
      0.405435621738433838, 0.409263730049133301,
      0.378736764192581177, 0.303221583366394043
    )

    for (i <- expectedRes.indices) {
      TestUtils.conditionFailTest(Math.abs(res.storage().array()(i) - expectedRes(i)) < 1e-6)
    }
  }

  "ROIAlign with aligned" should "be ok" in {
    val rois = Tensor[Float](T(T(1.0f, 1.0f, 3.0f, 3.0f)))
    val features = Tensor[Float](T(T(T(
      T( 0.0f, 1.0f, 2.0f, 3.0f, 4.0f),
      T( 5.0f, 6.0f, 7.0f, 8.0f, 9.0f),
      T(10.0f, 11.0f, 12.0f, 13.0f, 14.0f),
      T(15.0f, 16.0f, 17.0f, 18.0f, 19.0f),
      T(20.0f, 21.0f, 22.0f, 23.0f, 24.0f)))))

    val expectedWithAlign = Tensor[Float](T(T(T(
      T(4.5, 5.0, 5.5, 6.0),
      T(7.0, 7.5, 8.0, 8.5),
      T(9.5, 10.0, 10.5, 11.0),
      T(12.0, 12.5, 13.0, 13.5)))))

    val expected = Tensor[Float](T(T(T(
      T(7.5, 8, 8.5, 9),
      T(10, 10.5, 11, 11.5),
      T(12.5, 13, 13.5, 14),
      T(15, 15.5, 16, 16.5)))))

    val roiAlign = RoiAlign[Float](1.0f, 0, 4, 4, "avg", aligned = true)
    val roiNoAlign = RoiAlign[Float](1.0f, 0, 4, 4, "avg", aligned = false)

    val out = roiAlign.forward(T(features, rois))
    val out2 = roiNoAlign.forward(T(features, rois))

    out should be(expectedWithAlign)
    out2 should be(expected)
  }

  "backward" should "work correctly" in {
    val input = Tensor[Float](T(T(T(
      T(0.0611, 0.2246, 0.2343, 0.1771, 0.5561, 0.1094, 0.4609, 0.7084,
        0.5798, 0.4967),
      T(0.5104, 0.3295, 0.7182, 0.3845, 0.0898, 0.1175, 0.6402, 0.1968,
        0.5124, 0.7118),
      T(0.9249, 0.9997, 0.8927, 0.8767, 0.8450, 0.1544, 0.1705, 0.9842,
        0.8127, 0.4358),
      T(0.4143, 0.4284, 0.7578, 0.9225, 0.9643, 0.1760, 0.9539, 0.3134,
        0.4544, 0.2956),
      T(0.1875, 0.2433, 0.3493, 0.4441, 0.4069, 0.2859, 0.8036, 0.3218,
        0.3639, 0.2985),
      T(0.6635, 0.2552, 0.4144, 0.8396, 0.7418, 0.2865, 0.7929, 0.5001,
        0.8977, 0.1051),
      T(0.5809, 0.9867, 0.1315, 0.2391, 0.3047, 0.5158, 0.4514, 0.4929,
        0.5301, 0.2647),
      T(0.1671, 0.5482, 0.2380, 0.5374, 0.4422, 0.6454, 0.5376, 0.2245,
        0.6632, 0.8439),
      T(0.0109, 0.2807, 0.9301, 0.5438, 0.8123, 0.7750, 0.7308, 0.9924,
        0.7282, 0.2328),
      T(0.9997, 0.5540, 0.4200, 0.5419, 0.8642, 0.4312, 0.1213, 0.8956,
        0.8784, 0.9128)))))

    val rois = Tensor[Float](T(T(0.0f, 0.0f, 9.0f, 9.0f),
      T(0.0f, 5.0f, 4.0f, 9.0f),
      T(5.0f, 5.0f, 9.0f, 9.0f)))

    val layer = RoiAlign[Float](spatialScale = 1, samplingRatio = 2, pooledH = 5,
      pooledW = 5, aligned = true)
    val out = layer.forward(T(input, rois))

    val output = Tensor[Float](T(T(T(
      T(0.2593, 0.3618, 0.2819, 0.3935, 0.5265),
      T(0.7170, 0.8159, 0.6562, 0.4006, 0.6567),
      T(0.3210, 0.4949, 0.5372, 0.5892, 0.4368),
      T(0.6147, 0.3702, 0.4642, 0.5216, 0.5698),
      T(0.2292, 0.5687, 0.6427, 0.6625, 0.6822))),

      T(T(T(0.5731, 0.3794, 0.3402, 0.4984, 0.7202),
        T(0.6138, 0.7188, 0.4918, 0.2772, 0.4116),
        T(0.3937, 0.6494, 0.4761, 0.2458, 0.3759),
        T(0.1376, 0.3636, 0.4568, 0.4737, 0.5367),
        T(0.1754, 0.2846, 0.5770, 0.7363, 0.5957))),

      T(T(T(0.3776, 0.6335, 0.6252, 0.5709, 0.6844),
        T(0.4507, 0.5218, 0.5245, 0.5387, 0.5696),
        T(0.5452, 0.5203, 0.4266, 0.4301, 0.5784),
        T(0.6602, 0.6221, 0.5252, 0.5232, 0.6680),
        T(0.7253, 0.6559, 0.7846, 0.8819, 0.6998)))))

    val gradOutput = Tensor[Float](T(T(
      T(T(0.9688, 0.4150, 0.4094, 0.6885, 0.6800),
        T(0.6415, 0.4019, 0.4875, 0.9569, 0.5172),
        T(0.9534, 0.8540, 0.9555, 0.0836, 0.1684),
        T(0.1883, 0.9384, 0.3543, 0.2027, 0.5069),
        T(0.7145, 0.6801, 0.9717, 0.2403, 0.3372))),
      T(T(T(0.5260, 0.1794, 0.4793, 0.3070, 0.7682),
        T(0.6350, 0.7321, 0.9899, 0.1897, 0.6957),
        T(0.1313, 0.9514, 0.3386, 0.5337, 0.1051),
        T(0.1800, 0.4603, 0.7114, 0.5114, 0.2422),
        T(0.1480, 0.2527, 0.2014, 0.3004, 0.7147))),
      T(T(T(0.4033, 0.9819, 0.4697, 0.3446, 0.7631),
        T(0.3554, 0.2396, 0.6231, 0.6009, 0.3054),
        T(0.2082, 0.2404, 0.6693, 0.7529, 0.1088),
        T(0.0441, 0.4054, 0.0348, 0.7627, 0.0077),
        T(0.9582, 0.6859, 0.3182, 0.5291, 0.3420)))))

    Equivalent.nearequals(output, out, 1e-3) should be(true)

    val grad = layer.backward(T(input, rois), gradOutput).toTensor[Float]

    val expectedGrad = Tensor[Float](T(T(
      T(0.3203, 0.2666, 0.1312, 0.1305, 0.1295, 0.1816, 0.2177, 0.2157, 0.2150, 0.0098),
      T(0.2828, 0.2374, 0.1246, 0.1265, 0.1292, 0.1868, 0.2267, 0.2018, 0.1945, 0.0088),
      T(0.2029, 0.1776, 0.1216, 0.1322, 0.1475, 0.2314, 0.2895, 0.1867, 0.1565, 0.0071),
      T(0.2432, 0.2201, 0.1775, 0.1889, 0.2054, 0.1912, 0.1814, 0.1288, 0.1133, 0.0051),
      T(0.3845, 0.3403, 0.3323, 0.3769, 0.3154, 0.2258, 0.1666, 0.1222, 0.1580, 0.0195),
      T(0.8482, 0.8043, 0.8665, 0.9852, 0.3694, 0.7024, 0.9496, 0.7323, 0.8099, 0.1104),
      T(0.8683, 1.2765, 1.0463, 0.7984, 0.2498, 0.4796, 0.7130, 1.1149, 0.6427, 0.0529),
      T(0.6204, 1.1059, 1.0230, 0.6332, 0.3176, 0.4221, 0.5735, 0.9508, 0.4563, 0.0167),
      T(0.4918, 0.6479, 0.7008, 0.8754, 0.5076, 0.9881, 0.7134, 0.6981, 0.5184, 0.0460),
      T(0.0427, 0.0525, 0.0614, 0.1103, 0.0510, 0.1533, 0.1064, 0.0863, 0.0695, 0.0079))))

    Equivalent.nearequals(grad, expectedGrad, 1e-3) should be(true)
  }
}

class RoiAlignSerialTest extends ModuleSerializationTest {
  override def test(): Unit = {
    val input = T()
    RNG.setSeed(10)
    val input1 = Tensor[Float](1, 2, 6, 8).apply1(_ => RNG.uniform(-1, 1).toFloat)
    val input2 = Tensor[Float](T(T( 6, 2, 7, 5)))
    input(1.0f) = input1
    input(2.0f) = input2
    val roiAlign = new RoiAlign[Float](spatialScale = 1.0f, samplingRatio = 1,
      pooledW = 1, pooledH = 1).setName("roiAlign")
    runSerializationTest(roiAlign, input)
  }
}
