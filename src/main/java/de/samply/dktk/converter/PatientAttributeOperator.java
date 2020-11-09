package de.samply.dktk.converter;

import de.samply.share.model.ccp.Attribute;
import de.samply.share.model.ccp.Case;
import de.samply.share.model.ccp.Container;
import de.samply.share.model.ccp.Patient;
import de.samply.share.model.ccp.Sample;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public abstract class PatientAttributeOperator {


  protected abstract Attribute operateAttribute(Attribute attribute);


  protected Patient operateAttributes(Patient patient) {

    List<Case> caseListP = patient.getCase();
    for (Case caseP : caseListP) {
      operateAttributes(caseP);
    }
    List<Sample> sampleList = patient.getSample();
    for (Sample sample : sampleList) {
      operateAttributes(sample);
    }

    operateAttributes(() -> patient.getAttribute(), () -> patient.getContainer());

    return patient;

  }


  private void operateAttributes(Case caseP) {
    operateAttributes(() -> caseP.getAttribute(), () -> caseP.getContainer());
  }

  private void operateAttributes(Sample sample) {
    operateAttributes(() -> sample.getAttribute(), () -> sample.getContainer());
  }

  private void operateAttributes(Supplier<List<Attribute>> attributesSupplier,
      Supplier<List<Container>> containersSupplier) {

    List<Attribute> attributes = attributesSupplier.get();
    operateAttributes(attributes);
    List<Container> containers = containersSupplier.get();

    for (Container container : containers) {
      operateAttributes(() -> container.getAttribute(), () -> container.getContainer());
    }
  }


  private void operateAttributes(List<Attribute> attributes) {

    List<Attribute> newAttributes = new ArrayList<>();

    for (Attribute attribute : attributes) {

      Attribute newAttribute = operateAttribute(attribute);
      if (newAttribute != null) {
        newAttributes.add(newAttribute);
      }

    }

    attributes.clear();
    attributes.addAll(newAttributes);

  }

}
