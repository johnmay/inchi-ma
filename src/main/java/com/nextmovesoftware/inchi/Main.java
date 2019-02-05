/*
 * =====================================
 *  Copyright (c) 2019 NextMove Software
 * =====================================
 */

package com.nextmovesoftware.inchi;

import net.sf.jniinchi.INCHI_OPTION;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.geometry.GeometryUtil;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.io.iterator.IteratingSDFReader;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Main {

  private static final IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();
  private static final SmilesParser       smipar  = new SmilesParser(builder);

  private static final int                SMIFMT  = 1;
  private static final int                SDFFMT  = 2;
  private static       InputStream        in;
  private static       OutputStream       out;
  private static       int                fmt     = SMIFMT;
  private static       List<INCHI_OPTION> options = new ArrayList<>();

  private static int determineFormat(String val) {
    switch (val.toLowerCase(Locale.ROOT)) {
      case "smi":
      case "csmi":
      case "cxsmi":
      case "ism":
        return SMIFMT;
      case "sdf":
      case "mol":
      case "mdl":
      case "molfile":
        return SDFFMT;
      default:
        System.err.println("Unknown format: " + val);
        return SMIFMT;
    }
  }

  private static int determineFormatFromFilename(String fname) {
    int idx = fname.lastIndexOf('.');
    return determineFormat(fname.substring(idx + 1));
  }

  private static boolean processCommandLine(String[] args) {
    int j = 0;
    for (int i = 0; i < args.length; i++) {
      if (args[i].startsWith("-fmt")) {
        String val;
        if (args[i].startsWith("-fmt="))
          val = args[i].substring(5);
        else if (i + 1 < args.length)
          val = args[++i];
        else {
          System.err.println("Argument error -fmt [SMI|SDF]");
          return false;
        }
        fmt = determineFormat(val);
      } else if (args[i].equals("-RecMet")) {
        options.add(INCHI_OPTION.RecMet);
      } else if (args[i].equals("-FixedH")) {
        options.add(INCHI_OPTION.FixedH);
      } else {
        switch (j++) {
          case 0:
            if (args[i].equals("-"))
              in = System.in;
            else {
              try {
                in = new FileInputStream(args[i]);
              } catch (FileNotFoundException ex) {
                System.err.println("File not found: " + args[i]);
                return false;
              }
              fmt = determineFormatFromFilename(args[i]);
            }
            break;
          case 1:
            if (args[i].equals("-"))
              out = System.out;
            else {
              try {
                out = new FileOutputStream(args[i]);
              } catch (FileNotFoundException ex) {
                System.err.println("File not found: " + args[i]);
                return false;
              }
            }
            break;
        }
      }
    }
    if (out == null)
      out = System.out;
    return in != null;
  }

  private static void displayUsage() {
    System.err.println("Copyright (c) 2019 NextMove Software");
    System.err.println("InChI Metal Arch\n");
    System.err.println("Usage:");
    System.err.println("  java -jar inchi-ma.jar [-fmt {SMI|SDF} -RecMet -FixedH] <infile> [<outfile>]\n");
    System.err.println("Examples:");
    System.err.println(" Processing a file of SMILES or SDfile");
    System.err.println("   java -jar inchi-ma.jar <input.smi>");
    System.err.println("   java -jar inchi-ma.jar <input.sdf>\n");
    System.err.println(" Processing SMILES from STDIN");
    System.err.println("   java -jar inchi-ma.jar -\n");
    System.err.println(" Processing SDfile from STDIN by specifying in format");
    System.err.println("   java -jar inchi-ma.jar -fmt SDF -\n");
    System.exit(1);
  }

  private static void processSmilesFile(BufferedWriter wtr,
                                        BufferedReader brdr) throws IOException {
    String line;
    while ((line = brdr.readLine()) != null) {
      try {
        IAtomContainer mol   = smipar.parseSmiles(line);
        String         inchi = InChIMetalArch.toInChI(mol, options);
        if (inchi != null)
          wtr.write(inchi);
        if (mol.getTitle() != null) {
          wtr.write('\t');
          wtr.write(mol.getTitle());
        }
        wtr.write('\n');
      } catch (InvalidSmilesException e) {
        System.err.println("BAD SMILES: " + line);
      }
    }
  }

  private static void processSdfile(BufferedWriter wtr,
                                    IteratingSDFReader sdf) throws IOException {
    while (sdf.hasNext()) {
      IAtomContainer mol = sdf.next();

      if (!GeometryUtil.has3DCoordinates(mol)) {
        System.err.println("Skipping non-3D record: " + mol.getTitle());
        continue;
      }

      PerceiveFrom3d.perceive(mol);

      String inchi = InChIMetalArch.toInChI(mol, options);
      if (inchi != null)
        wtr.write(inchi);
      if (mol.getTitle() != null) {
        wtr.write('\t');
        wtr.write(mol.getTitle());
      }
      wtr.write('\n');
    }
  }

  private static void processInputStream(BufferedWriter wtr) {
    switch (fmt) {
      case SMIFMT:
        try (Reader rdr = new InputStreamReader(in, StandardCharsets.UTF_8);
             BufferedReader brdr = new BufferedReader(rdr)) {
          processSmilesFile(wtr, brdr);
        } catch (IOException e) {
          System.err.println("Low level IO Error: " + e.getMessage());
        }
        break;
      case SDFFMT:
        try (IteratingSDFReader sdfr = new IteratingSDFReader(in, builder, true)) {
          processSdfile(wtr, sdfr);
        } catch (IOException e) {
          System.err.println("Low level IO Error: " + e.getMessage());
        }
        break;
    }
  }

  public static void main(String[] args) {
    if (!processCommandLine(args))
      displayUsage();
    try (Writer wtr = new OutputStreamWriter(out, StandardCharsets.UTF_8);
         BufferedWriter bwtr = new BufferedWriter(wtr)) {
      processInputStream(bwtr);
    } catch (IOException e) {
      System.err.println("Low level IO Error");
    }
  }
}
