package de.samply.dktk.converter.sort.mdr;

import de.samply.common.mdrclient.MdrClient;
import java.util.Collections;
import java.util.List;

public class GbaSortRules implements SortRules {

  private MdrClient mdrClient;

  public GbaSortRules(MdrClient mdrClient) {
    this.mdrClient = mdrClient;
  }

  @Override
  public List<String> sortMdrIds(List<String> listOfMdrIds) {

    listOfMdrIds = sortMdrIdsByDktkId(listOfMdrIds);

    return listOfMdrIds;

  }

  private List<String> sortMdrIdsByDktkId(List<String> listOfMdrIds) {

    Collections.sort(listOfMdrIds, new DktkIdComparator(mdrClient));
    return listOfMdrIds;

  }

}
