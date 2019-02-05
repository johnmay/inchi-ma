# InChI Configuration Index

Proof of concept to generate InChI's that distinguish different Square Planar,
Trigonal Bipyramidal, and Octahedral configurations.

It adds a '/ma' (metal architecture) layer as proposed by J Goodman to the end
of an InChI. Currently the following geometries are supported:

* Square Planar ``/ma<atom>s<order>`` where order is 1,2,3
* Trigonal bipyramidal ``/ma<atom>tb<order>`` where order is 1-20
* Octahedral ``/ma<atom>o<order>`` where order is 1-30

Daylight used the term 'chiral order', I prefer simply 'order' to also cover
configuration as these geometries are not necessarily chiral.

The order specifies a permutation index and currently uses the same coding
scheme as SMILES (see. [relevant blog post](http://timvdm.blogspot.com/2010/09/smiles-stereochemistry-enigma.html)).
The scheme works as follows, first we know we need to be able to specify any
order of the neighbors (we shall call them 'carriers') around the central (or
'focus') atom.

To specify any ordering for octahedral we have 6 neighbors so there are 720
(6 factorial) possible ways to order the carriers. However there are 24 symmetries
and so we only need 720/24 = 30 possible orders. For trigonal bipyramidal, 120
(i.e. 5!) ways to order but 6 symmetries, 120/5 = 20 possible orders.

For each of each geometry we use a table to look up the
ordering we have ended up with from the canonical labelling. When there are
symmetries within the neighbors we choose the lowest possible ordering.

Currently the symmetry had to be shoe-horned in but in practice a backtracking
canonical labelling algorithm (such as that used by the InChI) can take care of
it.

### Examples

There are 16 possible orderings of Square Planar neighbors (4!), for platin
when two of each neighbors are symmetric there two possibilities: *cis-platin*
and *trans-platin*. As shown below the ``/ma`` layer divides the possibilities
with ``/ma5sp1`` corresponding to *cis-*, and ``/ma5sp2`` to *trans-*.

Note the numbers here refer to the input atom order in the SMILES (see
``examples/platin.smi``) and not the InChI canonical numbers.

![](https://www.simolecule.com/cdkdepict/depict/bow/svg?smi=Cl%5BPt%40SP1%5D%28Cl%29%28%5BNH3%5D%29%5BNH3%5D+%2Fma5sp1&abbr=on&hdisp=bridgehead&showtitle=true&zoom=1.6&annotate=number)
![](https://www.simolecule.com/cdkdepict/depict/bow/svg?smi=Cl%5BPt%40SP2%5D%28Cl%29%28%5BNH3%5D%29%5BNH3%5D+%2Fma5sp2&abbr=on&hdisp=bridgehead&showtitle=true&zoom=1.6&annotate=number)
![](https://www.simolecule.com/cdkdepict/depict/bow/svg?smi=Cl%5BPt%40SP3%5D%28Cl%29%28%5BNH3%5D%29%5BNH3%5D+%2Fma5sp1&abbr=on&hdisp=bridgehead&showtitle=true&zoom=1.6&annotate=number)
![](https://www.simolecule.com/cdkdepict/depict/bow/svg?smi=Cl%5BPt%40SP1%5D%28%5BNH3%5D%29%28Cl%29%5BNH3%5D+%2Fma5sp2&abbr=on&hdisp=bridgehead&showtitle=true&zoom=1.6&annotate=number)
![](https://www.simolecule.com/cdkdepict/depict/bow/svg?smi=Cl%5BPt%40SP2%5D%28%5BNH3%5D%29%28Cl%29%5BNH3%5D+%2Fma5sp1&abbr=on&hdisp=bridgehead&showtitle=true&zoom=1.6&annotate=number)
![](https://www.simolecule.com/cdkdepict/depict/bow/svg?smi=Cl%5BPt%40SP3%5D%28%5BNH3%5D%29%28Cl%29%5BNH3%5D+%2Fma5sp1&abbr=on&hdisp=bridgehead&showtitle=true&zoom=1.6&annotate=number)
![](https://www.simolecule.com/cdkdepict/depict/bow/svg?smi=Cl%5BPt%40SP1%5D%28%5BNH3%5D%29%28%5BNH3%5D%29Cl+%2Fma5sp1&abbr=on&hdisp=bridgehead&showtitle=true&zoom=1.6&annotate=number)
![](https://www.simolecule.com/cdkdepict/depict/bow/svg?smi=Cl%5BPt%40SP2%5D%28%5BNH3%5D%29%28%5BNH3%5D%29Cl+%2Fma5sp1&abbr=on&hdisp=bridgehead&showtitle=true&zoom=1.6&annotate=number)
![](https://www.simolecule.com/cdkdepict/depict/bow/svg?smi=Cl%5BPt%40SP3%5D%28%5BNH3%5D%29%28%5BNH3%5D%29Cl+%2Fma5sp2&abbr=on&hdisp=bridgehead&showtitle=true&zoom=1.6&annotate=number)
![](https://www.simolecule.com/cdkdepict/depict/bow/svg?smi=%5BNH3%5D%5BPt%40SP1%5D%28Cl%29%28%5BNH3%5D%29Cl+%2Fma5sp2&abbr=on&hdisp=bridgehead&showtitle=true&zoom=1.6&annotate=number)
![](https://www.simolecule.com/cdkdepict/depict/bow/svg?smi=%5BNH3%5D%5BPt%40SP2%5D%28Cl%29%28%5BNH3%5D%29Cl+%2Fma5sp1&abbr=on&hdisp=bridgehead&showtitle=true&zoom=1.6&annotate=number)
![](https://www.simolecule.com/cdkdepict/depict/bow/svg?smi=%5BNH3%5D%5BPt%40SP3%5D%28Cl%29%28%5BNH3%5D%29Cl+%2Fma5sp1&abbr=on&hdisp=bridgehead&showtitle=true&zoom=1.6&annotate=number)
![](https://www.simolecule.com/cdkdepict/depict/bow/svg?smi=%5BNH3%5D%5BPt%40SP1%5D%28Cl%29%28Cl%29%5BNH3%5D+%2Fma5sp1&abbr=on&hdisp=bridgehead&showtitle=true&zoom=1.6&annotate=number)
![](https://www.simolecule.com/cdkdepict/depict/bow/svg?smi=%5BNH3%5D%5BPt%40SP2%5D%28Cl%29%28Cl%29%5BNH3%5D+%2Fma5sp1&abbr=on&hdisp=bridgehead&showtitle=true&zoom=1.6&annotate=number)
![](https://www.simolecule.com/cdkdepict/depict/bow/svg?smi=%5BNH3%5D%5BPt%40SP3%5D%28Cl%29%28Cl%29%5BNH3%5D+%2Fma5sp2&abbr=on&hdisp=bridgehead&showtitle=true&zoom=1.6&annotate=number)

### Usage

A command line application is provided that can provide InChIs for a SMILES or
3D SDfile:

```
$ java -jar inchi-ma.jar input.smi
```

Some example inputs are provided in the ``examples/`` directory.

### Limitations

Currently only constitutionally different neighbors are handled. The system used
can be used to encode geometries such as lambda/delta Fe(ox<sub>3</sub>) if
there was tighter integration with the main canonically labelling algorithm.

