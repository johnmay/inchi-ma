/*
 * =====================================
 *  Copyright (c) 2019 NextMove Software
 * =====================================
 */

package com.nextmovesoftware.inchi;

import com.google.common.collect.FluentIterable;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.graph.GraphUtil;
import org.openscience.cdk.graph.invariant.Canon;
import org.openscience.cdk.graph.invariant.InChINumbersTools;
import org.openscience.cdk.inchi.InChIGenerator;
import org.openscience.cdk.inchi.InChIGeneratorFactory;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.interfaces.IStereoElement;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.stereo.Octahedral;
import org.openscience.cdk.stereo.SquarePlanar;
import org.openscience.cdk.stereo.TrigonalBipyramidal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class InChIMetalArch {

  private final static IChemObjectBuilder    bldr   = SilentChemObjectBuilder.getInstance();
  private final static SmilesParser          smipar = new SmilesParser(bldr);
  private final static InChIGeneratorFactory inchifact;

  static {
    try {
      inchifact = InChIGeneratorFactory.getInstance();
    } catch (CDKException e) {
      throw new IllegalStateException("Could no get InChI instance!");
    }
  }

  /**
   * This would normally be done as part of the Canonical labelling algorithm.
   * In McKay's algorithm (which InChI is based) ties are split by backtracking,
   * he we re-do the backtracking using our symetry partition defined
   * permutation.
   *
   * @param table the permutation table for this configuration class
   * @param perm  the permutation
   * @param best  the best index found so far
   * @return the best index
   */
  private static int findByBacktrack(int[][] table,
                                     int[] perm,
                                     int best) {
    int tmp;
    for (int i = 0; i < perm.length; i++) {
      int numSym = 0;
      for (int x : perm)
        if (x == i)
          ++numSym;
      if (numSym > 1) {
        for (int j = 0; j < perm.length; j++) {
          if (perm[j] == i) {
            int[] copy = perm.clone();
            for (int k = 0; k < perm.length; k++) {
              if (k != j && copy[k] == i)
                copy[k] = i + 1;
            }
            tmp = findByBacktrack(table, copy, best);
            if (tmp < best)
              best = tmp;
          }
        }
        return best;
      }
    }
    for (int i = 0; i < table.length; i++) {
      int[] subtable = table[i];
      for (int j = 0; j < subtable.length; j += perm.length) {
        boolean found = true;
        for (int k = 0; k < perm.length; k++)
          if (subtable[j + k] != perm[k]) {
            found = false;
            break;
          }
        if (found) {
          return i + 1;
        }
      }
    }

    // should not get here
    throw new IllegalStateException();
  }

  private static int getConfigNum(IStereoElement<IAtom, IAtom> se,
                                  final int[][] table,
                                  final long[] sym,
                                  final long[] numbers) {
    List<IAtom>         carriers        = se.getCarriers();
    List<IAtom>         carriersInOrder = new ArrayList<>(carriers);
    Map<IAtom, Integer> map             = new HashMap<IAtom, Integer>();

    // need to normalize ranks ot 0-5
    Collections.sort(carriersInOrder, new Comparator<IAtom>() {
      public int compare(IAtom o1, IAtom o2) {
        return Integer.compare((int) numbers[o1.getIndex()],
                               (int) numbers[o2.getIndex()]);
      }
    });

    int num  = 0;
    int nCls = 1;
    for (int i = 0; i < carriersInOrder.size(); i++) {
      if (i > 0 && sym[carriersInOrder.get(i).getIndex()] != sym[carriersInOrder.get(i - 1).getIndex()]) {
        num = i;
        nCls++;
      }
      map.put(carriersInOrder.get(i), num);
    }

    int[] perm = new int[carriers.size()];
    int   idx  = 0;
    for (IAtom atom : carriers) {
      perm[idx++] = map.get(atom);
    }

    return findByBacktrack(table, perm, 100);
  }

  public static String toInChI(IAtomContainer mol) {
    try {
      InChIGenerator inchigen = inchifact.getInChIGenerator(mol, "");
      String         inchi    = inchigen.getInchi();
      String         auxinfo  = inchigen.getAuxInfo();

      final long[] numbers = new long[mol.getAtomCount()];
      InChINumbersTools.parseAuxInfo(auxinfo, numbers);

      // This can be done as part of the InChI canon algorithm
      // whilst back-tracking, here we need to do some extra
      // external symmetry perception canonical labelling to handle
      // cases where are symmetric numbers
      long[] sym = Canon.symmetry(mol, GraphUtil.toAdjList(mol));

      StringBuilder malayer = new StringBuilder();

      // need to traverse geometries in canonical order
      List<IStereoElement> stereos = new ArrayList<>(FluentIterable.from(mol.stereoElements())
                                                                   .toList());
      Collections.sort(stereos, new Comparator<IStereoElement>() {
        @Override
        public int compare(IStereoElement a, IStereoElement b) {
          // only care about atom-based stereo ignore cis-trans for example
          if (a.getFocus() instanceof IAtom && b.getFocus() instanceof IAtom) {
            return Integer.compare((int) numbers[((IAtom) a.getFocus()).getIndex()],
                                   (int) numbers[((IAtom) b.getFocus()).getIndex()]);
          } else if (!(a.getFocus() instanceof IAtom))
            return -1;
          else if (!(b.getFocus() instanceof IAtom))
            return +1;
          else
            return 0;
        }
      });
      for (IStereoElement se : stereos) {
        switch (se.getConfigClass()) {
          case IStereoElement.OC:
            Octahedral oc = (Octahedral) se;
            if (malayer.length() != 0)
              malayer.append(',');
            malayer.append(numbers[oc.getFocus().getIndex()])
                   .append("o").append(getConfigNum(oc.normalize(), ConfigTables.OC,
                                                    sym, numbers));
            break;
          case IStereoElement.SP:
            SquarePlanar sp = (SquarePlanar) se;
            if (malayer.length() != 0)
              malayer.append(',');
            malayer.append(numbers[sp.getFocus().getIndex()])
                   .append("sp").append(getConfigNum(sp.normalize(),
                                                     ConfigTables.SP,
                                                     sym, numbers));
            break;
          case IStereoElement.TBPY:
            TrigonalBipyramidal tbpy = (TrigonalBipyramidal) se;
            if (malayer.length() != 0)
              malayer.append(',');
            malayer.append(numbers[tbpy.getFocus().getIndex()])
                   .append("tb").append(getConfigNum(tbpy.normalize(),
                                                     ConfigTables.TBPY,
                                                     sym, numbers));
            break;
        }
      }
      inchi += "/ma" + malayer.toString();
      return inchi;
    } catch (CDKException e) {
      System.err.println("ERROR: Could not generate inchi " + e.getMessage());
      return null;
    }
  }

  public static String toInChI(String smi) {
    try {
      return toInChI(smipar.parseSmiles(smi));
    } catch (InvalidSmilesException e) {
      System.err.println("ERROR: Bad SMILES " + e.getMessage());
      return null;
    }
  }
}
