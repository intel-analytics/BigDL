/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 3.0.12
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package com.intel.analytics.bigdl.friesian.serving.recall.faiss.swighnswlib;

public class IndexFlat extends Index {
  private transient long swigCPtr;

  protected IndexFlat(long cPtr, boolean cMemoryOwn) {
    super(swigfaissJNI.IndexFlat_SWIGUpcast(cPtr), cMemoryOwn);
    swigCPtr = cPtr;
  }

  protected static long getCPtr(IndexFlat obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  protected void finalize() {
    delete();
  }

  public synchronized void delete() {
    if (swigCPtr != 0) {
      if (swigCMemOwn) {
        swigCMemOwn = false;
        swigfaissJNI.delete_IndexFlat(swigCPtr);
      }
      swigCPtr = 0;
    }
    super.delete();
  }

  public void setXb(FloatVector value) {
    swigfaissJNI.IndexFlat_xb_set(swigCPtr, this, FloatVector.getCPtr(value), value);
  }

  public FloatVector getXb() {
    long cPtr = swigfaissJNI.IndexFlat_xb_get(swigCPtr, this);
    return (cPtr == 0) ? null : new FloatVector(cPtr, false);
  }

  public IndexFlat(int d, MetricType metric) {
    this(swigfaissJNI.new_IndexFlat__SWIG_0(d, metric.swigValue()), true);
  }

  public IndexFlat(int d) {
    this(swigfaissJNI.new_IndexFlat__SWIG_1(d), true);
  }

  public void add(int n, SWIGTYPE_p_float x) {
    swigfaissJNI.IndexFlat_add(swigCPtr, this, n, SWIGTYPE_p_float.getCPtr(x));
  }

  public void reset() {
    swigfaissJNI.IndexFlat_reset(swigCPtr, this);
  }

  public void search(int n, SWIGTYPE_p_float x, int k, SWIGTYPE_p_float distances, SWIGTYPE_p_long labels) {
    swigfaissJNI.IndexFlat_search(swigCPtr, this, n, SWIGTYPE_p_float.getCPtr(x), k, SWIGTYPE_p_float.getCPtr(distances), SWIGTYPE_p_long.getCPtr(labels));
  }

  public void range_search(int n, SWIGTYPE_p_float x, float radius, RangeSearchResult result) {
    swigfaissJNI.IndexFlat_range_search(swigCPtr, this, n, SWIGTYPE_p_float.getCPtr(x), radius, RangeSearchResult.getCPtr(result), result);
  }

  public void reconstruct(int key, SWIGTYPE_p_float recons) {
    swigfaissJNI.IndexFlat_reconstruct(swigCPtr, this, key, SWIGTYPE_p_float.getCPtr(recons));
  }

  public void compute_distance_subset(int n, SWIGTYPE_p_float x, int k, SWIGTYPE_p_float distances, SWIGTYPE_p_long labels) {
    swigfaissJNI.IndexFlat_compute_distance_subset(swigCPtr, this, n, SWIGTYPE_p_float.getCPtr(x), k, SWIGTYPE_p_float.getCPtr(distances), SWIGTYPE_p_long.getCPtr(labels));
  }

  public long remove_ids(IDSelector sel) {
    return swigfaissJNI.IndexFlat_remove_ids(swigCPtr, this, IDSelector.getCPtr(sel), sel);
  }

  public IndexFlat() {
    this(swigfaissJNI.new_IndexFlat__SWIG_2(), true);
  }

  public DistanceComputer get_distance_computer() {
    long cPtr = swigfaissJNI.IndexFlat_get_distance_computer(swigCPtr, this);
    return (cPtr == 0) ? null : new DistanceComputer(cPtr, true);
  }

  public long sa_code_size() {
    return swigfaissJNI.IndexFlat_sa_code_size(swigCPtr, this);
  }

  public void sa_encode(int n, SWIGTYPE_p_float x, SWIGTYPE_p_unsigned_char bytes) {
    swigfaissJNI.IndexFlat_sa_encode(swigCPtr, this, n, SWIGTYPE_p_float.getCPtr(x), SWIGTYPE_p_unsigned_char.getCPtr(bytes));
  }

  public void sa_decode(int n, SWIGTYPE_p_unsigned_char bytes, SWIGTYPE_p_float x) {
    swigfaissJNI.IndexFlat_sa_decode(swigCPtr, this, n, SWIGTYPE_p_unsigned_char.getCPtr(bytes), SWIGTYPE_p_float.getCPtr(x));
  }

}
