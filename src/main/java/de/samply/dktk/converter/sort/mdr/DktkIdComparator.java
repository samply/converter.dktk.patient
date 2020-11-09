package de.samply.dktk.converter.sort.mdr;

import de.samply.common.mdrclient.MdrClient;
import de.samply.dktk.converter.sort.MdrUtils;
import de.samply.dktk.converter.sort.MdrUtilsException;
import java.util.Comparator;

public class DktkIdComparator implements Comparator<String> {

  private MdrUtils mdrUtils;
  private int prefixSize = 2;

  public DktkIdComparator(MdrClient mdrClient) {
    mdrUtils = new MdrUtils(mdrClient);
  }

  @Override
  public int compare(String o1, String o2) {

    String o1DktkId = getDktkId(o1);
    String o2DktkId = getDktkId(o2);

    if (o1DktkId != null && o2DktkId == null) {
      return -1;
    } else if (o1DktkId == null && o2DktkId == null) {
      return 0;
    } else if (o1DktkId == null && o2DktkId != null) {
      return 1;
    } else {
      return compareNotNullDktkIds(o1DktkId, o2DktkId);
    }

  }

  private int compareNotNullDktkIds(String dktkId1, String dktkId2) {

    String dktkId1Prefix = getDktkIdPrefix(dktkId1);
    String dktkId2Prefix = getDktkIdPrefix(dktkId2);
    Integer dktkId1Number = getDktkIdNumber(dktkId1);
    Integer dktkId2Number = getDktkIdNumber(dktkId2);

    return (dktkId1Prefix.equals(dktkId2Prefix)) ? dktkId1Number.compareTo(dktkId2Number)
        : dktkId1Prefix.compareTo(dktkId2Prefix);

  }

  private String getDktkIdPrefix(String dktkId) {
    return dktkId.substring(0, prefixSize - 1);
  }

  private Integer getDktkIdNumber(String dktkId) {
    return getInteger(dktkId.substring(prefixSize));
  }

  private Integer getInteger(String number) {

    try {
      return new Integer(number);
    } catch (Exception e) {
      return null;
    }

  }

  private String getDktkId(String mdrId) {

    try {
      return mdrUtils.getDktkId(mdrId);
    } catch (MdrUtilsException e) {
      return null;
    }

  }


}
