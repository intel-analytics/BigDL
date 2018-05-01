# Analytics Zoo Image Classification API

Analytics Zoo provides a collection of pre-trained models for Image Classification. These models can be used for out-of-the-box inference if you are interested in categories already in the corresponding datasets. According to the business scenarios, users can embed the models locally, distributedly in Spark such as Apache Storm and Apache Flink.

***Image Classfication models***

Analytics Zoo provides several typical kind of pre-trained Image Classfication models : [Alexnet](http://papers.nips.cc/paper/4824-imagenet-classification-with-deep-convolutional-neural-networksese), [Inception-V1](https://arxiv.org/abs/1409.4842), [VGG](https://arxiv.org/abs/1409.1556), [Resnet](https://arxiv.org/abs/1512.03385), [Densenet](https://arxiv.org/abs/1608.06993), [Mobilenet](https://arxiv.org/abs/1704.04861), [Squeezenet](https://arxiv.org/abs/1602.07360) models, please check below examples.

[Scala example](../../zoo/src/main/scala/com/intel/analytics/zoo/examples/imageclassification/Predict.scala)

It's very easy to apply the model for inference with below code piece.

```scala
val model = ImageClassifier.loadModel[Float](params.model)
val data = ImageSet.read(params.image, sc, params.nPartition)
val output = model.predictImageSet(data)
```

For preprocessors for Image Classification models, please check [Image Classification Config](../../zoo/src/main/scala/com/intel/analytics/zoo/models/image/imageclassification/ImageClassificationConfig.scala)


### Image Classification

* [Alexnet](https://s3-ap-southeast-1.amazonaws.com/analytics-zoo-models/imageclassification/imagenet/analytics-zoo_alexnet_imagenet_0.1.0.model)
* [Alexnet Quantize](https://s3-ap-southeast-1.amazonaws.com/analytics-zoo-models/imageclassification/imagenet/analytics-zoo_alexnet-quantize_imagenet_0.1.0.model)
* [Inception-V1](https://s3-ap-southeast-1.amazonaws.com/analytics-zoo-models/imageclassification/imagenet/analytics-zoo_inception-v1_imagenet_0.1.0.model)
* [Inception-V1 Quantize](https://s3-ap-southeast-1.amazonaws.com/analytics-zoo-models/imageclassification/imagenet/analytics-zoo_inception-v1-quantize_imagenet_0.1.0.model)
* [VGG-16](https://s3-ap-southeast-1.amazonaws.com/analytics-zoo-models/imageclassification/imagenet/analytics-zoo_vgg-16_imagenet_0.1.0.model)
* [VGG-16 Quantize](https://s3-ap-southeast-1.amazonaws.com/analytics-zoo-models/imageclassification/imagenet/analytics-zoo_vgg-16-quantize_imagenet_0.1.0.model)
* [VGG-19](https://s3-ap-southeast-1.amazonaws.com/analytics-zoo-models/imageclassification/imagenet/analytics-zoo_vgg-19_imagenet_0.1.0.model)
* [VGG-19 Quantize](https://s3-ap-southeast-1.amazonaws.com/analytics-zoo-models/imageclassification/imagenet/analytics-zoo_vgg-19-quantize_imagenet_0.1.0.model)
* [Resnet-50](https://s3-ap-southeast-1.amazonaws.com/analytics-zoo-models/imageclassification/imagenet/analytics-zoo_resnet-50_imagenet_0.4.0.model)
* [Resnet-50 Quantize](https://s3-ap-southeast-1.amazonaws.com/analytics-zoo-models/imageclassification/imagenet/analytics-zoo_resnet-50-quantize_imagenet_0.1.0.model)
* [Densenet-161](https://s3-ap-southeast-1.amazonaws.com/analytics-zoo-models/imageclassification/imagenet/analytics-zoo_densenet-161_imagenet_0.1.0.model)
* [Densenet-161 Quantize](https://s3-ap-southeast-1.amazonaws.com/analytics-zoo-models/imageclassification/imagenet/analytics-zoo_densenet-161-quantize_imagenet_0.1.0.model)
* [Mobilenet](https://s3-ap-southeast-1.amazonaws.com/analytics-zoo-models/imageclassification/imagenet/analytics-zoo_mobilenet_imagenet_0.1.0.model)
* [Squeezenet](https://s3-ap-southeast-1.amazonaws.com/analytics-zoo-models/imageclassification/imagenet/analytics-zoo_squeezenet_imagenet_0.1.0.model)
* [Squeezenet Quantize](https://s3-ap-southeast-1.amazonaws.com/analytics-zoo-models/imageclassification/imagenet/analytics-zoo_squeezenet-quantize_imagenet_0.1.0.model)