package de.samply.dktk.converter.sort.mdr;

import static de.samply.dktk.converter.Constants.URN_ADT_DATAELEMENT_77;
import static de.samply.dktk.converter.Constants.URN_ADT_DATAELEMENT_78;
import static de.samply.dktk.converter.Constants.URN_ADT_DATAELEMENT_90;
import static de.samply.dktk.converter.Constants.URN_ADT_DATAELEMENT_93;

import de.samply.common.mdrclient.MdrClient;
import java.util.Collections;
import java.util.List;

public class DktkSortRules implements SortRules {

  private MdrClient mdrClient;

  public DktkSortRules(MdrClient mdrClient) {
    this.mdrClient = mdrClient;
  }

  @Override
  public List<String> sortMdrIds(List<String> listOfMdrIds) {

    listOfMdrIds = sortMdrIdsByDktkId(listOfMdrIds);
    listOfMdrIds = sortMdrIdsAssuringAdt90preceedsAdt93(listOfMdrIds);
    listOfMdrIds = sortMdrIdsAssuringAdt77preceedsAdt78(listOfMdrIds);
    listOfMdrIds = sortMdrIdsByPuttingDatesAtTheFirstPosition(listOfMdrIds);

    return listOfMdrIds;

  }

  private List<String> sortMdrIdsByDktkId(List<String> listOfMdrIds) {

    Collections.sort(listOfMdrIds, new DktkIdComparator(mdrClient));
    return listOfMdrIds;

  }

  private List<String> sortMdrIdsAssuringAdt90preceedsAdt93(List<String> listOfMdrIds) {
    return assureElement1PrecedesElement2(listOfMdrIds, URN_ADT_DATAELEMENT_90,
        URN_ADT_DATAELEMENT_93);
  }

  private List<String> sortMdrIdsAssuringAdt77preceedsAdt78(List<String> listOfMdrIds) {
    return assureElement1PrecedesElement2(listOfMdrIds, URN_ADT_DATAELEMENT_77,
        URN_ADT_DATAELEMENT_78);
  }


  private List<String> sortMdrIdsByPuttingDatesAtTheFirstPosition(List<String> listOfMdrIds) {

    Collections.sort(listOfMdrIds, new DateFirstComparator(mdrClient));
    return listOfMdrIds;

  }

  private List<String> assureElement1PrecedesElement2(List<String> listOfMdrIds, String mdrId1,
      String mdrId2) {

    Integer dataElement1index = null;
    Integer dataElement2index = null;

    for (String mdrId : listOfMdrIds) {

      if (mdrId1.contains(mdrId)) {
        dataElement1index = listOfMdrIds.indexOf(mdrId);
      } else if (mdrId2.contains(mdrId)) {
        dataElement2index = listOfMdrIds.indexOf(mdrId);
      }

    }

    if (dataElement1index != null && dataElement2index != null
        && dataElement1index > dataElement2index) {
      Collections.swap(listOfMdrIds, dataElement1index, dataElement2index);
    }

    return listOfMdrIds;

  }


}
