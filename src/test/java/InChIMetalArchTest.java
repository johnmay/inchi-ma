/*
 * =====================================
 *  Copyright (c) 2019 NextMove Software
 * =====================================
 */

import com.nextmovesoftware.inchi.InChIMetalArch;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

public class InChIMetalArchTest {

  @Test
  public void cisTransPlatin() {
    Set<String> inchis = new HashSet<>();
    // generate the 30 possible configurations, there should be two unique
    // one cis- and one trans-
    for (int i = 1; i <= 3; i++) {
      String smi = "Cl[Pt@SP" + i + "](Cl)([NH3])[NH3]";
      inchis.add(InChIMetalArch.toInChI(smi));
    }
    Assert.assertThat(inchis.size(), CoreMatchers.is(2));
  }

  @Test
  public void cisTransCoNO2Cl() {
    Set<String> inchis = new HashSet<>();
    // generate the 4 possible configurations in the shape of U, Z, or 4
    for (int i = 1; i <= 30; i++) {
      String smi = "Cl[Co@OH" + i + "](Cl)(Cl)(N(=O)(=O))(Cl)N(=O)=O";
      inchis.add(InChIMetalArch.toInChI(smi));
    }
    Assert.assertThat(inchis.size(), CoreMatchers.is(2));
  }

  @Test
  public void facMerCoNO2Cl() {
    Set<String> inchis = new HashSet<>();
    // generate the 30 possible configurations, there should be two unique
    // one fac- and one mer-
    for (int i = 1; i <= 30; i++) {
      String smi = "Cl[Co@OH" + i + "](Cl)(Cl)(N(=O)(=O))(N(=O)=O)N(=O)=O";
      inchis.add(InChIMetalArch.toInChI(smi));
    }
    Assert.assertThat(inchis.size(), CoreMatchers.is(2));
  }

  // when we have 2+4 ligands in TBPY we have 3 possible configurations
  @Test
  public void tbpy2and4() {
    Set<String> inchis = new HashSet<>();
    // generate the 20 possible configurations
    for (int i = 1; i <= 20; i++) {
      String smi = "N[Co@TB" + i + "](Cl)(Cl)(Cl)N";
      inchis.add(InChIMetalArch.toInChI(smi));
    }
    Assert.assertThat(inchis.size(), CoreMatchers.is(3));
  }
}