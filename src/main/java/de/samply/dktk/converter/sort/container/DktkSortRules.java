package de.samply.dktk.converter.sort.container;

import de.samply.common.mdrclient.MdrClient;
import de.samply.dktk.converter.sort.MdrUtils;
import de.samply.dktk.converter.sort.MdrUtilsException;
import de.samply.share.model.common.Attribute;
import de.samply.share.model.common.Container;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class DktkSortRules implements SortRules {


  private MdrUtils mdrUtils;

  public DktkSortRules(MdrClient mdrClient) {
    mdrUtils = new MdrUtils(mdrClient);
  }

  @Override
  public List<Container> sortContainers(List<Container> containerList) {

    containerList = sortContainersByDate(containerList);
    return containerList;

  }

  private List<Container> sortContainersByDate(List<Container> containerList) {

    Collections.sort(containerList, new DateOfSameContainerComparator(containerList));
    return containerList;

  }

  private String getContainerDesignation(Container container) {
    return container.getDesignation();
  }

  private Date getFirstDate(Container container) {

    for (Attribute attribute : container.getAttribute()) {

      if (isDatum(attribute)) {
        String sdate = attribute.getValue().getValue();
        return getFirstDate(sdate);
      }

    }

    return null;
  }

  private Date getFirstDate(String date) {

    try {
      return getDate_WithoutManagementException(date);
    } catch (ParseException e) {
      return null;
    }

  }

  private Date getDate_WithoutManagementException(String date) throws ParseException {

    String[] split = date.split("\\.");

    String format = null;

    switch (split.length) {

      case 3:
        format = "dd.MM.yyyy";
        break;
      case 2:
        format = "MM.yyyy";
        break;
      case 1:
        format = "yyyy";
        break;
      default:

    }

    SimpleDateFormat simpleDateFormat = (format != null) ? new SimpleDateFormat(format) : null;

    return (simpleDateFormat != null) ? simpleDateFormat.parse(date) : null;

  }

  private boolean isDatum(Attribute attribute) {

    try {

      return mdrUtils.isDatum(attribute.getMdrKey());

    } catch (MdrUtilsException e) {

      e.printStackTrace();
      return false;

    }

  }

  private class DateOfSameContainerComparator implements Comparator<Container> {

    private List<String> containerDesignationSet = new ArrayList<>();

    public DateOfSameContainerComparator(List<Container> containerList) {

      for (Container container : containerList) {

        String containerDesignation = getContainerDesignation(container);
        if (containerDesignation != null && !containerDesignationSet
            .contains(containerDesignation)) {
          containerDesignationSet.add(containerDesignation);
        }

      }

    }

    @Override
    public int compare(Container o1, Container o2) {

      String container1Designation = getContainerDesignation(o1);
      String container2Designation = getContainerDesignation(o2);

      return
          (container1Designation != null && container2Designation != null && container1Designation
              .equalsIgnoreCase(container2Designation)) ? compareDates(o1, o2)
              : compareDesignations(container1Designation, container2Designation);

    }

    private int compareDesignations(String designation1, String designation2) {

      Integer index1 = containerDesignationSet.indexOf(designation1);
      Integer index2 = containerDesignationSet.indexOf(designation2);

      return index1.compareTo(index2);

    }

    private int compareDates(Container o1, Container o2) {

      Date date1 = getFirstDate(o1);
      Date date2 = getFirstDate(o2);

      int result = 0;

      if (date1 != null && date2 == null) {
        result = -1;
      } else if (date1 == null && date2 != null) {
        result = 1;
      } else if (date1 != null && date2 != null) {
        result = date1.compareTo(date2);
      }

      return result;

    }

  }

}
