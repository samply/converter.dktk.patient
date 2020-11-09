package de.samply.dktk.converter;

import de.samply.share.common.utils.MdrIdDatatype;
import de.samply.share.model.common.Attribute;
import de.samply.share.model.common.Container;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class DiagnosisConverter {

  private Map<Container, String> containerDiagnosisCodeMap = new HashMap<>();


  public DiagnosisConverter(Container patient) {
    parsePatient(patient);
  }

  private void parsePatient(Container patient) {

    if (isPatient(patient)) {

      for (Container container : patient.getContainer()) {
        parseContainer(container);
      }

    }

  }


  private void parseContainer(Container container) {
    parseContainer(container, null);
  }

  private void parseContainer(Container container, Consumer<Container> consumerContainer) {

    if (consumerContainer != null) {
      consumerContainer.accept(container);
    }

    String diagnosisCode = isDiagnosis(container) ? getDiagnosisCodeOfDiagnosis(container) : null;
    consumerContainer =
        (diagnosisCode != null) ? (x) -> addContainerToDiagnosisChildren(x, diagnosisCode)
            : consumerContainer;

    for (Container child : container.getContainer()) {
      parseContainer(child, consumerContainer);
    }

  }

  private void addContainerToDiagnosisChildren(Container container, String diagnosisCode) {
    containerDiagnosisCodeMap.put(container, diagnosisCode);
  }


  private boolean isDiagnosis(Container container) {
    return container.getDesignation().equalsIgnoreCase(Constants.DESIGNATION_DIAGNOSIS);
  }

  private boolean isPatient(Container container) {
    return container.getDesignation().equalsIgnoreCase(Constants.DESIGNATION_PATIENT);
  }

  private String getDiagnosisCodeOfDiagnosis(Container diagnosis) {

    for (Attribute attribute : diagnosis.getAttribute()) {
      if (isDiagnosisCode(attribute)) {
        return attribute.getValue().getValue();
      }
    }

    return null;
  }

  private boolean isDiagnosisCode(Attribute attribute) {

    MdrIdDatatype attributeKey = new MdrIdDatatype(attribute.getMdrKey());
    return Constants.DIAGNOSE.equalsIgnoreVersion(attributeKey);

  }

  public String getDiagnosisCode(Container container) {
    return containerDiagnosisCodeMap.get(container);
  }


}
