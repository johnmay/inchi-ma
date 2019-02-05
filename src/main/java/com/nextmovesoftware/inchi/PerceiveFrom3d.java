/*
 * =====================================
 *  Copyright (c) 2019 NextMove Software
 * =====================================
 */

package com.nextmovesoftware.inchi;

import com.google.common.collect.FluentIterable;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IStereoElement;
import org.openscience.cdk.stereo.Octahedral;
import org.openscience.cdk.stereo.SquarePlanar;
import org.openscience.cdk.stereo.StereoTool;
import org.openscience.cdk.stereo.TrigonalBipyramidal;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

/**
 * Rough and ready but functional, this class provides functions to converts 3D
 * coordinates to the CDK data structures.
 */
public class PerceiveFrom3d {

  private static void perceiveSquarePlanar(IAtom focus) {
    // need to check if all on a plane...
    IBond[] bonds = FluentIterable.from(focus.bonds()).toArray(IBond.class);
    IAtom[] carriers = new IAtom[]{
        bonds[0].getOther(focus),
        bonds[1].getOther(focus),
        bonds[2].getOther(focus),
        bonds[3].getOther(focus)
    };
    Point3d[] points = new Point3d[]{
        carriers[0].getPoint3d(),
        carriers[1].getPoint3d(),
        carriers[2].getPoint3d(),
        carriers[3].getPoint3d()
    };

    Vector3d normal = StereoTool.getNormal(points[0], points[1], points[2]);
    if (!StereoTool.allCoplanar(normal, focus.getPoint3d(), points)) {
      return;
    }

    StereoTool.SquarePlanarShape shape =
        StereoTool.getSquarePlanarShape(carriers[0],
                                        carriers[1],
                                        carriers[2],
                                        carriers[3]);
    IAtomContainer mol = focus.getContainer();
    switch (shape) {
      case U_SHAPE:
        mol.addStereoElement(new SquarePlanar(focus,
                                              carriers,
                                              IStereoElement.SPU));
        break;
      case FOUR_SHAPE:
        mol.addStereoElement(new SquarePlanar(focus,
                                              carriers,
                                              IStereoElement.SP4));
        break;
      case Z_SHAPE:
        mol.addStereoElement(new SquarePlanar(focus,
                                              carriers,
                                              IStereoElement.SPZ));
        break;
    }
  }

  private static <T> void swap(T[] atom, int i, int j) {
    T tmp = atom[i];
    atom[i] = atom[j];
    atom[j] = tmp;
  }

  /**
   * 3x3 determinant helper for a constant third column
   */
  private static double det(double xa, double ya, double xb, double yb,
                            double xc, double yc) {
    return (xa - xc) * (yb - yc) - (ya - yc) * (xb - xc);
  }

  private static int signedVolume(Point3d... coords) {
    double x1 = coords[0].x;
    double x2 = coords[1].x;
    double x3 = coords[2].x;
    double x4 = coords[3].x;

    double y1 = coords[0].y;
    double y2 = coords[1].y;
    double y3 = coords[2].y;
    double y4 = coords[3].y;

    double z1 = coords[0].z;
    double z2 = coords[1].z;
    double z3 = coords[2].z;
    double z4 = coords[3].z;

    double det = (z1 * det(x2, y2, x3, y3, x4, y4)) - (z2 * det(x1, y1, x3, y3, x4, y4))
                 + (z3 * det(x1, y1, x2, y2, x4, y4)) - (z4 * det(x1, y1, x2, y2, x3, y3));

    return (int) Math.signum(det);
  }

  private static void perceiveTbpy(IAtom focus) {
    // need to check if all on a plane...
    IBond[] bonds = FluentIterable.from(focus.bonds()).toArray(IBond.class);
    IAtom[] carriers = new IAtom[]{
        bonds[0].getOther(focus),
        bonds[1].getOther(focus),
        bonds[2].getOther(focus),
        bonds[3].getOther(focus),
        bonds[4].getOther(focus)
    };
    Point3d[] points = new Point3d[]{
        carriers[0].getPoint3d(),
        carriers[1].getPoint3d(),
        carriers[2].getPoint3d(),
        carriers[3].getPoint3d(),
        carriers[4].getPoint3d()
    };
    Point3d focusXyz = focus.getPoint3d();

    for (int axisBeg = 0; axisBeg < points.length; axisBeg++) {
      for (int axisEnd = axisBeg + 1; axisEnd < points.length; axisEnd++) {

        // axis: axisBeg -> axisEnd
        if (!StereoTool.isColinear(points[axisBeg], focusXyz, points[axisEnd]))
          continue;

        IAtom[]   equatorial    = new IAtom[3];
        Point3d[] equatorialXyz = new Point3d[3];
        int       idx           = 0;
        for (int j = 0; j < carriers.length; j++) {
          if (axisEnd == j || axisBeg == j)
            continue;
          equatorial[idx] = carriers[j];
          equatorialXyz[idx] = points[j];
          idx++;
        }

        Vector3d normal = StereoTool.getNormal(equatorialXyz[0], equatorialXyz[1], equatorialXyz[2]);
        if (!StereoTool.allCoplanar(normal, focusXyz, equatorialXyz))
          return;

        // TODO (jwm) need to double check this
        // when viewed along the axis we need to know if we're anti-clockwise
        // (@) or clockwise (@@): @ => 1, @@ => 2. We do this by computing the
        // signed volume of the tetrahedral formed of first atom of the axis,
        // the focus and the first two equatorial neighbors
        int sign  = signedVolume(points[axisEnd], focusXyz, equatorialXyz[0], equatorialXyz[1]);
        int order = sign < 0 ? 2 : 1;

        IAtomContainer mol = focus.getContainer();
        mol.addStereoElement(new TrigonalBipyramidal(focus,
                                                     new IAtom[]{
                                                         carriers[axisBeg],
                                                         equatorial[0],
                                                         equatorial[1],
                                                         equatorial[2],
                                                         carriers[axisEnd]
                                                     },
                                                     order));
        break;
      }
    }
  }

  private static void perceiveOctahedral(IAtom focus) {
    // need to check if all on a plane...
    IBond[] bonds = FluentIterable.from(focus.bonds()).toArray(IBond.class);
    IAtom[] carriers = new IAtom[]{
        bonds[0].getOther(focus),
        bonds[1].getOther(focus),
        bonds[2].getOther(focus),
        bonds[3].getOther(focus),
        bonds[4].getOther(focus),
        bonds[5].getOther(focus)
    };
    Point3d[] points = new Point3d[]{
        carriers[0].getPoint3d(),
        carriers[1].getPoint3d(),
        carriers[2].getPoint3d(),
        carriers[3].getPoint3d(),
        carriers[4].getPoint3d(),
        carriers[5].getPoint3d()
    };
    Point3d focusXyz = focus.getPoint3d();

    for (int i = 1; i < points.length; i++) {
      // find axis
      if (!StereoTool.isColinear(points[0], focusXyz, points[i]))
        continue;

      IAtom[]   equatorial    = new IAtom[4];
      Point3d[] equatorialXyz = new Point3d[4];
      int       idx           = 0;
      for (int j = 1; j < carriers.length; j++) {
        if (i == j)
          continue;
        equatorial[idx] = carriers[j];
        equatorialXyz[idx] = points[j];
        idx++;
      }

      Vector3d normal = StereoTool.getNormal(equatorialXyz[0], equatorialXyz[1], equatorialXyz[2]);
      if (!StereoTool.allCoplanar(normal, focusXyz, equatorialXyz))
        return;

      StereoTool.SquarePlanarShape shape =
          StereoTool.getSquarePlanarShape(equatorial[0],
                                          equatorial[1],
                                          equatorial[2],
                                          equatorial[3]);

      // "unwind" the configuration such that we get a "U" shapre
      //  4 shape -> we swap the middle 2 i.e. idxs=1,2
      //  Z shape -> we swap the first two (or last two) i.e. idxs=0,1
      if (shape == StereoTool.SquarePlanarShape.FOUR_SHAPE) {
        swap(equatorial, 1, 2);
        swap(equatorialXyz, 1, 2);
      } else if (shape == StereoTool.SquarePlanarShape.Z_SHAPE) {
        swap(equatorial, 0, 1);
        swap(equatorialXyz, 0, 1);
      }

      // TODO (jwm) need to double check this
      // when viewed along the axis we need to know if we're anti-clockwise
      // (@) or clockwise (@@): @ => 1, @@ => 2. We do this by computing the
      // signed volume of the tetrahedral formed of first atom of the axis,
      // the focus and the first two equatorial neighbors
      int sign  = signedVolume(points[0], focusXyz, equatorialXyz[0], equatorialXyz[1]);
      int order = sign < 0 ? 2 : 1;

      IAtomContainer mol = focus.getContainer();
      mol.addStereoElement(new Octahedral(focus,
                                          new IAtom[]{
                                              carriers[0],
                                              equatorial[0],
                                              equatorial[1],
                                              equatorial[2],
                                              equatorial[3],
                                              carriers[i]
                                          },
                                          order));
      break;
    }
  }

  public static void perceive(IAtomContainer mol) {
    for (IAtom atom : mol.atoms()) {
      switch (atom.getBondCount()) {
        case 4:
          perceiveSquarePlanar(atom);
          break;
        case 5:
          perceiveTbpy(atom);
          break;
        case 6:
          perceiveOctahedral(atom);
          break;
      }
    }
  }

}
