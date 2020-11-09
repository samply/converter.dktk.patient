package de.samply.dktk.converter.sort;

import static de.samply.dktk.converter.Constants.DKTK_ID_SLOT_NAME;
import static de.samply.dktk.converter.Constants.MDR_DATATYPE_DATE;
import static de.samply.dktk.converter.Constants.MDR_LANGUAGE;

import de.samply.common.mdrclient.MdrClient;
import de.samply.common.mdrclient.MdrConnectionException;
import de.samply.common.mdrclient.MdrInvalidResponseException;
import de.samply.common.mdrclient.domain.DataElement;
import de.samply.dktk.converter.PatientConverterUtil;
import java.util.concurrent.ExecutionException;

public class MdrUtils {

  private MdrClient mdrClient;

  public MdrUtils(MdrClient mdrClient) {
    this.mdrClient = mdrClient;
  }

  /**
   * Todo.
   * @param mdrId Todo.
   * @return Todo.
   * @throws MdrUtilsException Todo.
   */
  public boolean isDatum(String mdrId) throws MdrUtilsException {

    boolean isDatum = false;

    if (mdrId != null) {

      String dataType = getDataType(mdrId);
      if (dataType != null && dataType.equalsIgnoreCase(MDR_DATATYPE_DATE)) {
        isDatum = true;
      }

    }

    return isDatum;

  }

  /**
   * Todo.
   * @param mdrId Todo.
   * @return Todo.
   * @throws MdrUtilsException Todo.
   */
  public String getDataType(String mdrId) throws MdrUtilsException {

    try {
      return getDataType_WithoutManagementException(mdrId);
    } catch (ExecutionException e) {
      throw new MdrUtilsException(e);
    } catch (MdrConnectionException e) {
      throw new MdrUtilsException(e);
    } catch (MdrInvalidResponseException e) {
      throw new MdrUtilsException(e);
    }
  }

  private String getDataType_WithoutManagementException(String mdrId)
      throws ExecutionException, MdrConnectionException, MdrInvalidResponseException {

    DataElement dataElement = mdrClient.getDataElement(mdrId, MDR_LANGUAGE);
    return (dataElement != null) ? dataElement.getValidation().getDatatype() : null;

  }

  /**
   * Todo.
   * @param mdrId Todo.
   * @return Todo.
   * @throws MdrUtilsException Todo.
   */
  public String getDktkId(String mdrId) throws MdrUtilsException {

    try {
      return getDktkId_WithoutManagementExeception(mdrId);
    } catch (MdrConnectionException e) {
      throw new MdrUtilsException(e);
    } catch (MdrInvalidResponseException e) {
      throw new MdrUtilsException(e);
    } catch (ExecutionException e) {
      throw new MdrUtilsException(e);
    }

  }

  private String getDktkId_WithoutManagementExeception(String mdrId)
      throws ExecutionException, MdrConnectionException, MdrInvalidResponseException {

    String dktkId = null;

    if (mdrId != null) {
      DataElement dataElement = mdrClient.getDataElement(mdrId, MDR_LANGUAGE);
      dktkId = PatientConverterUtil.getValueFromSlots(dataElement.getSlots(), DKTK_ID_SLOT_NAME);
    }

    return dktkId;

  }


}
