/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 3.0.12
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package com.intel.analytics.bigdl.friesian.serving.recall.faiss.swighnswlib;

public class MaskedInvertedLists extends ReadOnlyInvertedLists {
  private transient long swigCPtr;

  protected MaskedInvertedLists(long cPtr, boolean cMemoryOwn) {
    super(swigfaissJNI.MaskedInvertedLists_SWIGUpcast(cPtr), cMemoryOwn);
    swigCPtr = cPtr;
  }

  protected static long getCPtr(MaskedInvertedLists obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  protected void finalize() {
    delete();
  }

  public synchronized void delete() {
    if (swigCPtr != 0) {
      if (swigCMemOwn) {
        swigCMemOwn = false;
        swigfaissJNI.delete_MaskedInvertedLists(swigCPtr);
      }
      swigCPtr = 0;
    }
    super.delete();
  }

  public void setIl0(InvertedLists value) {
    swigfaissJNI.MaskedInvertedLists_il0_set(swigCPtr, this, getCPtr(value), value);
  }

  public InvertedLists getIl0() {
    long cPtr = swigfaissJNI.MaskedInvertedLists_il0_get(swigCPtr, this);
    return (cPtr == 0) ? null : new InvertedLists(cPtr, false);
  }

  public void setIl1(InvertedLists value) {
    swigfaissJNI.MaskedInvertedLists_il1_set(swigCPtr, this, getCPtr(value), value);
  }

  public InvertedLists getIl1() {
    long cPtr = swigfaissJNI.MaskedInvertedLists_il1_get(swigCPtr, this);
    return (cPtr == 0) ? null : new InvertedLists(cPtr, false);
  }

  public MaskedInvertedLists(InvertedLists il0, InvertedLists il1) {
    this(swigfaissJNI.new_MaskedInvertedLists(getCPtr(il0), il0, getCPtr(il1), il1), true);
  }

  public long list_size(long list_no) {
    return swigfaissJNI.MaskedInvertedLists_list_size(swigCPtr, this, list_no);
  }

  public SWIGTYPE_p_unsigned_char get_codes(long list_no) {
    long cPtr = swigfaissJNI.MaskedInvertedLists_get_codes(swigCPtr, this, list_no);
    return (cPtr == 0) ? null : new SWIGTYPE_p_unsigned_char(cPtr, false);
  }

  public SWIGTYPE_p_long get_ids(long list_no) {
    long cPtr = swigfaissJNI.MaskedInvertedLists_get_ids(swigCPtr, this, list_no);
    return (cPtr == 0) ? null : new SWIGTYPE_p_long(cPtr, false);
  }

  public void release_codes(long list_no, SWIGTYPE_p_unsigned_char codes) {
    swigfaissJNI.MaskedInvertedLists_release_codes(swigCPtr, this, list_no, SWIGTYPE_p_unsigned_char.getCPtr(codes));
  }

  public void release_ids(long list_no, SWIGTYPE_p_long ids) {
    swigfaissJNI.MaskedInvertedLists_release_ids(swigCPtr, this, list_no, SWIGTYPE_p_long.getCPtr(ids));
  }

  public int get_single_id(long list_no, long offset) {
    return swigfaissJNI.MaskedInvertedLists_get_single_id(swigCPtr, this, list_no, offset);
  }

  public SWIGTYPE_p_unsigned_char get_single_code(long list_no, long offset) {
    long cPtr = swigfaissJNI.MaskedInvertedLists_get_single_code(swigCPtr, this, list_no, offset);
    return (cPtr == 0) ? null : new SWIGTYPE_p_unsigned_char(cPtr, false);
  }

  public void prefetch_lists(SWIGTYPE_p_long list_nos, int nlist) {
    swigfaissJNI.MaskedInvertedLists_prefetch_lists(swigCPtr, this, SWIGTYPE_p_long.getCPtr(list_nos), nlist);
  }

}
