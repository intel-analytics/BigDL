/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 3.0.12
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package com.intel.analytics.bigdl.friesian.serving.recall.faiss.swighnswlib;

public class ZnSphereCodec extends ZnSphereSearch {
  private transient long swigCPtr;

  protected ZnSphereCodec(long cPtr, boolean cMemoryOwn) {
    super(swigfaissJNI.ZnSphereCodec_SWIGUpcast(cPtr), cMemoryOwn);
    swigCPtr = cPtr;
  }

  protected static long getCPtr(ZnSphereCodec obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  protected void finalize() {
    delete();
  }

  public synchronized void delete() {
    if (swigCPtr != 0) {
      if (swigCMemOwn) {
        swigCMemOwn = false;
        swigfaissJNI.delete_ZnSphereCodec(swigCPtr);
      }
      swigCPtr = 0;
    }
    super.delete();
  }

  static public class CodeSegment extends Repeats {
    private transient long swigCPtr;
  
    protected CodeSegment(long cPtr, boolean cMemoryOwn) {
      super(swigfaissJNI.ZnSphereCodec_CodeSegment_SWIGUpcast(cPtr), cMemoryOwn);
      swigCPtr = cPtr;
    }
  
    protected static long getCPtr(CodeSegment obj) {
      return (obj == null) ? 0 : obj.swigCPtr;
    }
  
    protected void finalize() {
      delete();
    }
  
    public synchronized void delete() {
      if (swigCPtr != 0) {
        if (swigCMemOwn) {
          swigCMemOwn = false;
          swigfaissJNI.delete_ZnSphereCodec_CodeSegment(swigCPtr);
        }
        swigCPtr = 0;
      }
      super.delete();
    }
  
    public CodeSegment(Repeats r) {
      this(swigfaissJNI.new_ZnSphereCodec_CodeSegment(Repeats.getCPtr(r), r), true);
    }
  
    public void setC0(long value) {
      swigfaissJNI.ZnSphereCodec_CodeSegment_c0_set(swigCPtr, this, value);
    }
  
    public long getC0() {
      return swigfaissJNI.ZnSphereCodec_CodeSegment_c0_get(swigCPtr, this);
    }
  
    public void setSignbits(int value) {
      swigfaissJNI.ZnSphereCodec_CodeSegment_signbits_set(swigCPtr, this, value);
    }
  
    public int getSignbits() {
      return swigfaissJNI.ZnSphereCodec_CodeSegment_signbits_get(swigCPtr, this);
    }
  
  }

  public void setCode_segments(SWIGTYPE_p_std__vectorT_faiss__ZnSphereCodec__CodeSegment_t value) {
    swigfaissJNI.ZnSphereCodec_code_segments_set(swigCPtr, this, SWIGTYPE_p_std__vectorT_faiss__ZnSphereCodec__CodeSegment_t.getCPtr(value));
  }

  public SWIGTYPE_p_std__vectorT_faiss__ZnSphereCodec__CodeSegment_t getCode_segments() {
    long cPtr = swigfaissJNI.ZnSphereCodec_code_segments_get(swigCPtr, this);
    return (cPtr == 0) ? null : new SWIGTYPE_p_std__vectorT_faiss__ZnSphereCodec__CodeSegment_t(cPtr, false);
  }

  public void setNv(long value) {
    swigfaissJNI.ZnSphereCodec_nv_set(swigCPtr, this, value);
  }

  public long getNv() {
    return swigfaissJNI.ZnSphereCodec_nv_get(swigCPtr, this);
  }

  public void setCode_size(long value) {
    swigfaissJNI.ZnSphereCodec_code_size_set(swigCPtr, this, value);
  }

  public long getCode_size() {
    return swigfaissJNI.ZnSphereCodec_code_size_get(swigCPtr, this);
  }

  public ZnSphereCodec(int dim, int r2) {
    this(swigfaissJNI.new_ZnSphereCodec(dim, r2), true);
  }

  public long search_and_encode(SWIGTYPE_p_float x) {
    return swigfaissJNI.ZnSphereCodec_search_and_encode(swigCPtr, this, SWIGTYPE_p_float.getCPtr(x));
  }

  public void decode(long code, SWIGTYPE_p_float c) {
    swigfaissJNI.ZnSphereCodec_decode(swigCPtr, this, code, SWIGTYPE_p_float.getCPtr(c));
  }

  public long encode(SWIGTYPE_p_float x) {
    return swigfaissJNI.ZnSphereCodec_encode(swigCPtr, this, SWIGTYPE_p_float.getCPtr(x));
  }

}
