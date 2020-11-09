package de.samply.dktk.converter.model;

import java.util.Map;
import java.util.TreeMap;

public class GenderDistribution {

  private Map<String, Integer> data;

  public GenderDistribution() {
    data = new TreeMap<>();
  }

  public Map<String, Integer> getData() {
    return data;
  }

  public void setData(Map<String, Integer> data) {
    this.data = data;
  }

  public int getAmountByGender(String gender) {
    try {
      return data.get(gender);
    } catch (Exception e) {
      return 0;
    }
  }

  public synchronized void increaseCountForGender(String gender) {
    addToGender(gender, 1);
  }

  public synchronized void addToGender(String gender, int amount) {
    data.put(gender, getAmountByGender(gender) + amount);
  }

  @Override
  public String toString() {
    return "GenderDistribution{" +
        "data=" + data +
        '}';
  }
}
