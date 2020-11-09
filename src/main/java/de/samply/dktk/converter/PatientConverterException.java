package de.samply.dktk.converter;

public class PatientConverterException extends Exception {

  public PatientConverterException(String s) {
    super(s);
  }

  public PatientConverterException(String s, Throwable throwable) {
    super(s, throwable);
  }

  public PatientConverterException(Throwable throwable) {
    super(throwable);
  }
}
