package com.intel.analytics.bigdl.ppml.generated;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.37.0)",
    comments = "Source: psi_service.proto")
public final class PSIServiceGrpc {

  private PSIServiceGrpc() {}

  public static final String SERVICE_NAME = "PSIService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.intel.analytics.bigdl.ppml.fl.generated.PSIServiceProto.SaltRequest,
      com.intel.analytics.bigdl.ppml.fl.generated.PSIServiceProto.SaltReply> getGetSaltMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "getSalt",
      requestType = com.intel.analytics.bigdl.ppml.fl.generated.PSIServiceProto.SaltRequest.class,
      responseType = com.intel.analytics.bigdl.ppml.fl.generated.PSIServiceProto.SaltReply.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.intel.analytics.bigdl.ppml.fl.generated.PSIServiceProto.SaltRequest,
      com.intel.analytics.bigdl.ppml.fl.generated.PSIServiceProto.SaltReply> getGetSaltMethod() {
    io.grpc.MethodDescriptor<com.intel.analytics.bigdl.ppml.fl.generated.PSIServiceProto.SaltRequest, com.intel.analytics.bigdl.ppml.fl.generated.PSIServiceProto.SaltReply> getGetSaltMethod;
    if ((getGetSaltMethod = PSIServiceGrpc.getGetSaltMethod) == null) {
      synchronized (PSIServiceGrpc.class) {
        if ((getGetSaltMethod = PSIServiceGrpc.getGetSaltMethod) == null) {
          PSIServiceGrpc.getGetSaltMethod = getGetSaltMethod =
              io.grpc.MethodDescriptor.<com.intel.analytics.bigdl.ppml.fl.generated.PSIServiceProto.SaltRequest, com.intel.analytics.bigdl.ppml.fl.generated.PSIServiceProto.SaltReply>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "getSalt"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intel.analytics.bigdl.ppml.fl.generated.PSIServiceProto.SaltRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intel.analytics.bigdl.ppml.fl.generated.PSIServiceProto.SaltReply.getDefaultInstance()))
              .setSchemaDescriptor(new PSIServiceMethodDescriptorSupplier("getSalt"))
              .build();
        }
      }
    }
    return getGetSaltMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.intel.analytics.bigdl.ppml.fl.generated.PSIServiceProto.UploadSetRequest,
      com.intel.analytics.bigdl.ppml.fl.generated.PSIServiceProto.UploadSetResponse> getUploadSetMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "uploadSet",
      requestType = com.intel.analytics.bigdl.ppml.fl.generated.PSIServiceProto.UploadSetRequest.class,
      responseType = com.intel.analytics.bigdl.ppml.fl.generated.PSIServiceProto.UploadSetResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.intel.analytics.bigdl.ppml.fl.generated.PSIServiceProto.UploadSetRequest,
      com.intel.analytics.bigdl.ppml.fl.generated.PSIServiceProto.UploadSetResponse> getUploadSetMethod() {
    io.grpc.MethodDescriptor<com.intel.analytics.bigdl.ppml.fl.generated.PSIServiceProto.UploadSetRequest, com.intel.analytics.bigdl.ppml.fl.generated.PSIServiceProto.UploadSetResponse> getUploadSetMethod;
    if ((getUploadSetMethod = PSIServiceGrpc.getUploadSetMethod) == null) {
      synchronized (PSIServiceGrpc.class) {
        if ((getUploadSetMethod = PSIServiceGrpc.getUploadSetMethod) == null) {
          PSIServiceGrpc.getUploadSetMethod = getUploadSetMethod =
              io.grpc.MethodDescriptor.<com.intel.analytics.bigdl.ppml.fl.generated.PSIServiceProto.UploadSetRequest, com.intel.analytics.bigdl.ppml.fl.generated.PSIServiceProto.UploadSetResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "uploadSet"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intel.analytics.bigdl.ppml.fl.generated.PSIServiceProto.UploadSetRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intel.analytics.bigdl.ppml.fl.generated.PSIServiceProto.UploadSetResponse.getDefaultInstance()))
              .setSchemaDescriptor(new PSIServiceMethodDescriptorSupplier("uploadSet"))
              .build();
        }
      }
    }
    return getUploadSetMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.intel.analytics.bigdl.ppml.fl.generated.PSIServiceProto.DownloadIntersectionRequest,
      com.intel.analytics.bigdl.ppml.fl.generated.PSIServiceProto.DownloadIntersectionResponse> getDownloadIntersectionMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "downloadIntersection",
      requestType = com.intel.analytics.bigdl.ppml.fl.generated.PSIServiceProto.DownloadIntersectionRequest.class,
      responseType = com.intel.analytics.bigdl.ppml.fl.generated.PSIServiceProto.DownloadIntersectionResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.intel.analytics.bigdl.ppml.fl.generated.PSIServiceProto.DownloadIntersectionRequest,
      com.intel.analytics.bigdl.ppml.fl.generated.PSIServiceProto.DownloadIntersectionResponse> getDownloadIntersectionMethod() {
    io.grpc.MethodDescriptor<com.intel.analytics.bigdl.ppml.fl.generated.PSIServiceProto.DownloadIntersectionRequest, com.intel.analytics.bigdl.ppml.fl.generated.PSIServiceProto.DownloadIntersectionResponse> getDownloadIntersectionMethod;
    if ((getDownloadIntersectionMethod = PSIServiceGrpc.getDownloadIntersectionMethod) == null) {
      synchronized (PSIServiceGrpc.class) {
        if ((getDownloadIntersectionMethod = PSIServiceGrpc.getDownloadIntersectionMethod) == null) {
          PSIServiceGrpc.getDownloadIntersectionMethod = getDownloadIntersectionMethod =
              io.grpc.MethodDescriptor.<com.intel.analytics.bigdl.ppml.fl.generated.PSIServiceProto.DownloadIntersectionRequest, com.intel.analytics.bigdl.ppml.fl.generated.PSIServiceProto.DownloadIntersectionResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "downloadIntersection"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intel.analytics.bigdl.ppml.fl.generated.PSIServiceProto.DownloadIntersectionRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intel.analytics.bigdl.ppml.fl.generated.PSIServiceProto.DownloadIntersectionResponse.getDefaultInstance()))
              .setSchemaDescriptor(new PSIServiceMethodDescriptorSupplier("downloadIntersection"))
              .build();
        }
      }
    }
    return getDownloadIntersectionMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static PSIServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<PSIServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<PSIServiceStub>() {
        @Override
        public PSIServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new PSIServiceStub(channel, callOptions);
        }
      };
    return PSIServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static PSIServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<PSIServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<PSIServiceBlockingStub>() {
        @Override
        public PSIServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new PSIServiceBlockingStub(channel, callOptions);
        }
      };
    return PSIServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static PSIServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<PSIServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<PSIServiceFutureStub>() {
        @Override
        public PSIServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new PSIServiceFutureStub(channel, callOptions);
        }
      };
    return PSIServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public static abstract class PSIServiceImplBase implements io.grpc.BindableService {

    /**
     * <pre>
     * Gives SHA256 Hash salt
     * </pre>
     */
    public void getSalt(com.intel.analytics.bigdl.ppml.fl.generated.PSIServiceProto.SaltRequest request,
        io.grpc.stub.StreamObserver<com.intel.analytics.bigdl.ppml.fl.generated.PSIServiceProto.SaltReply> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetSaltMethod(), responseObserver);
    }

    /**
     */
    public void uploadSet(com.intel.analytics.bigdl.ppml.fl.generated.PSIServiceProto.UploadSetRequest request,
        io.grpc.stub.StreamObserver<com.intel.analytics.bigdl.ppml.fl.generated.PSIServiceProto.UploadSetResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getUploadSetMethod(), responseObserver);
    }

    /**
     */
    public void downloadIntersection(com.intel.analytics.bigdl.ppml.fl.generated.PSIServiceProto.DownloadIntersectionRequest request,
        io.grpc.stub.StreamObserver<com.intel.analytics.bigdl.ppml.fl.generated.PSIServiceProto.DownloadIntersectionResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDownloadIntersectionMethod(), responseObserver);
    }

    @Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getGetSaltMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.intel.analytics.bigdl.ppml.fl.generated.PSIServiceProto.SaltRequest,
                com.intel.analytics.bigdl.ppml.fl.generated.PSIServiceProto.SaltReply>(
                  this, METHODID_GET_SALT)))
          .addMethod(
            getUploadSetMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.intel.analytics.bigdl.ppml.fl.generated.PSIServiceProto.UploadSetRequest,
                com.intel.analytics.bigdl.ppml.fl.generated.PSIServiceProto.UploadSetResponse>(
                  this, METHODID_UPLOAD_SET)))
          .addMethod(
            getDownloadIntersectionMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.intel.analytics.bigdl.ppml.fl.generated.PSIServiceProto.DownloadIntersectionRequest,
                com.intel.analytics.bigdl.ppml.fl.generated.PSIServiceProto.DownloadIntersectionResponse>(
                  this, METHODID_DOWNLOAD_INTERSECTION)))
          .build();
    }
  }

  /**
   */
  public static final class PSIServiceStub extends io.grpc.stub.AbstractAsyncStub<PSIServiceStub> {
    private PSIServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @Override
    protected PSIServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new PSIServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * Gives SHA256 Hash salt
     * </pre>
     */
    public void getSalt(com.intel.analytics.bigdl.ppml.fl.generated.PSIServiceProto.SaltRequest request,
        io.grpc.stub.StreamObserver<com.intel.analytics.bigdl.ppml.fl.generated.PSIServiceProto.SaltReply> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetSaltMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void uploadSet(com.intel.analytics.bigdl.ppml.fl.generated.PSIServiceProto.UploadSetRequest request,
        io.grpc.stub.StreamObserver<com.intel.analytics.bigdl.ppml.fl.generated.PSIServiceProto.UploadSetResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getUploadSetMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void downloadIntersection(com.intel.analytics.bigdl.ppml.fl.generated.PSIServiceProto.DownloadIntersectionRequest request,
        io.grpc.stub.StreamObserver<com.intel.analytics.bigdl.ppml.fl.generated.PSIServiceProto.DownloadIntersectionResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDownloadIntersectionMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class PSIServiceBlockingStub extends io.grpc.stub.AbstractBlockingStub<PSIServiceBlockingStub> {
    private PSIServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @Override
    protected PSIServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new PSIServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Gives SHA256 Hash salt
     * </pre>
     */
    public com.intel.analytics.bigdl.ppml.fl.generated.PSIServiceProto.SaltReply getSalt(com.intel.analytics.bigdl.ppml.fl.generated.PSIServiceProto.SaltRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetSaltMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.intel.analytics.bigdl.ppml.fl.generated.PSIServiceProto.UploadSetResponse uploadSet(com.intel.analytics.bigdl.ppml.fl.generated.PSIServiceProto.UploadSetRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getUploadSetMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.intel.analytics.bigdl.ppml.fl.generated.PSIServiceProto.DownloadIntersectionResponse downloadIntersection(com.intel.analytics.bigdl.ppml.fl.generated.PSIServiceProto.DownloadIntersectionRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDownloadIntersectionMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class PSIServiceFutureStub extends io.grpc.stub.AbstractFutureStub<PSIServiceFutureStub> {
    private PSIServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @Override
    protected PSIServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new PSIServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * Gives SHA256 Hash salt
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.intel.analytics.bigdl.ppml.fl.generated.PSIServiceProto.SaltReply> getSalt(
        com.intel.analytics.bigdl.ppml.fl.generated.PSIServiceProto.SaltRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetSaltMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.intel.analytics.bigdl.ppml.fl.generated.PSIServiceProto.UploadSetResponse> uploadSet(
        com.intel.analytics.bigdl.ppml.fl.generated.PSIServiceProto.UploadSetRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getUploadSetMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.intel.analytics.bigdl.ppml.fl.generated.PSIServiceProto.DownloadIntersectionResponse> downloadIntersection(
        com.intel.analytics.bigdl.ppml.fl.generated.PSIServiceProto.DownloadIntersectionRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDownloadIntersectionMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_GET_SALT = 0;
  private static final int METHODID_UPLOAD_SET = 1;
  private static final int METHODID_DOWNLOAD_INTERSECTION = 2;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final PSIServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(PSIServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_GET_SALT:
          serviceImpl.getSalt((com.intel.analytics.bigdl.ppml.fl.generated.PSIServiceProto.SaltRequest) request,
              (io.grpc.stub.StreamObserver<com.intel.analytics.bigdl.ppml.fl.generated.PSIServiceProto.SaltReply>) responseObserver);
          break;
        case METHODID_UPLOAD_SET:
          serviceImpl.uploadSet((com.intel.analytics.bigdl.ppml.fl.generated.PSIServiceProto.UploadSetRequest) request,
              (io.grpc.stub.StreamObserver<com.intel.analytics.bigdl.ppml.fl.generated.PSIServiceProto.UploadSetResponse>) responseObserver);
          break;
        case METHODID_DOWNLOAD_INTERSECTION:
          serviceImpl.downloadIntersection((com.intel.analytics.bigdl.ppml.fl.generated.PSIServiceProto.DownloadIntersectionRequest) request,
              (io.grpc.stub.StreamObserver<com.intel.analytics.bigdl.ppml.fl.generated.PSIServiceProto.DownloadIntersectionResponse>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @Override
    @SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class PSIServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    PSIServiceBaseDescriptorSupplier() {}

    @Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.intel.analytics.bigdl.ppml.fl.generated.PSIServiceProto.getDescriptor();
    }

    @Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("PSIService");
    }
  }

  private static final class PSIServiceFileDescriptorSupplier
      extends PSIServiceBaseDescriptorSupplier {
    PSIServiceFileDescriptorSupplier() {}
  }

  private static final class PSIServiceMethodDescriptorSupplier
      extends PSIServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    PSIServiceMethodDescriptorSupplier(String methodName) {
      this.methodName = methodName;
    }

    @Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (PSIServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new PSIServiceFileDescriptorSupplier())
              .addMethod(getGetSaltMethod())
              .addMethod(getUploadSetMethod())
              .addMethod(getDownloadIntersectionMethod())
              .build();
        }
      }
    }
    return result;
  }
}
