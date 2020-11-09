package de.samply.dktk.converter.sort.mdr;

import de.samply.common.mdrclient.MdrClient;
import de.samply.dktk.converter.sort.MdrUtils;
import de.samply.dktk.converter.sort.MdrUtilsException;
import java.util.Comparator;


public class DateFirstComparator implements Comparator<String> {

  MdrUtils mdrUtils;

  public DateFirstComparator(MdrClient mdrClient) {
    mdrUtils = new MdrUtils(mdrClient);
  }

  @Override
  public int compare(String mdrId1, String mdrId2) {

    boolean isO1Datum = isDatum(mdrId1);
    boolean isO2Datum = isDatum(mdrId2);

    if (isO1Datum && !isO2Datum) {
      return -1;
    } else if (!isO1Datum && isO2Datum) {
      return 1;
    } else {
      return 0;
    }

  }

  private boolean isDatum(String mdrId) {

    try {
      return mdrUtils.isDatum(mdrId);
    } catch (MdrUtilsException e) {
      e.printStackTrace();
      return false;
    }

  }


}
