package de.lmu.ifi.dbs.elki.utilities.pairs;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.Comparator;

/**
 * Utility functions for (boxed) Pair classes.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses Pair
 * @apiviz.has CompareNatural
 * @apiviz.has CompareNaturalFirst
 * @apiviz.has CompareNaturalSecond
 * @apiviz.has CompareNaturalSwapped
 * @apiviz.has Compare
 * @apiviz.has CompareByFirst
 * @apiviz.has CompareBySecond
 * @apiviz.has CompareSwapped
 */
public final class PairUtil {
  /**
   * Return a comparator for this pair, given that both components are already
   * comparable. (So it could have been a CPair)
   * 
   * @param <FIRST> First type
   * @param <SECOND> Second type
   * @return Comparator
   */
  public static <FIRST extends Comparable<? super FIRST>, SECOND extends Comparable<? super SECOND>> Comparator<Pair<? extends FIRST, ? extends SECOND>> comparator() {
    return new CompareNatural<>();
  }

  /**
   * Return a derived comparator given a comparator for each component.
   * 
   * @param <FIRST> First type
   * @param <SECOND> Second type
   * @param c1 First comparator
   * @param c2 Second comparator
   * @return Comparator
   */
  public static <FIRST, SECOND> Comparator<Pair<? extends FIRST, ? extends SECOND>> comparator(Comparator<? super FIRST> c1, Comparator<? super SECOND> c2) {
    return new Compare<>(c1, c2);
  }

  /**
   * Return a comparator by first component for this pair, given that the first
   * component is already comparable. (So it could have been a FCPair)
   * 
   * @param <FIRST> First type
   * @param <SECOND> Second type
   * @return Comparator
   */
  public static <FIRST extends Comparable<? super FIRST>, SECOND> Comparator<Pair<? extends FIRST, ? extends SECOND>> comparatorFirst() {
    return new CompareNaturalFirst<>();
  }

  /**
   * Return a derived comparator by first component given a comparator for this
   * component.
   * 
   * @param <FIRST> First type
   * @param <SECOND> Second type
   * @param c1 Comparator for first
   * @return Comparator
   */
  public static <FIRST, SECOND> Comparator<Pair<? extends FIRST, ? extends SECOND>> comparatorFirst(Comparator<? super FIRST> c1) {
    return new CompareByFirst<>(c1);
  }

  /**
   * Return a comparator by first component for this pair, given that the first
   * component is already comparable. (So it could have been a FCPair)
   * 
   * @param <FIRST> First type
   * @param <SECOND> Second type
   * @return Comparator
   */
  public static <FIRST, SECOND extends Comparable<? super SECOND>> Comparator<Pair<? extends FIRST, ? extends SECOND>> comparatorSecond() {
    return new CompareNaturalSecond<>();
  }

  /**
   * Return a derived comparator by first component given a comparator for this
   * component.
   * 
   * @param <FIRST> First type
   * @param <SECOND> Second type
   * @param c2 Comparator for second
   * @return Comparator
   */
  public static <FIRST, SECOND> Comparator<Pair<? extends FIRST, ? extends SECOND>> comparatorSecond(Comparator<? super SECOND> c2) {
    return new CompareBySecond<>(c2);
  }

  /**
   * Return a component-swapped comparator for this pair, given that both
   * components are already comparable. (So it could have been a CPair)
   * 
   * @param <FIRST> First type
   * @param <SECOND> Second type
   * @return Comparator
   */
  public static <FIRST extends Comparable<? super FIRST>, SECOND extends Comparable<? super SECOND>> Comparator<Pair<? extends FIRST, ? extends SECOND>> comparatorSwapped() {
    return new CompareNaturalSwapped<>();
  }

  /**
   * Return a derived component-swapped comparator given a comparator for each
   * component.
   * 
   * @param <FIRST> First type
   * @param <SECOND> Second type
   * @param c1 First comparator
   * @param c2 Second comparator
   * @return Comparator
   */
  public static <FIRST, SECOND> Comparator<Pair<? extends FIRST, ? extends SECOND>> comparatorSwapped(Comparator<? super FIRST> c1, Comparator<? super SECOND> c2) {
    return new CompareSwapped<>(c1, c2);
  }

  /**
   * Class to do a "natural order" comparison on this class.
   * 
   * @param <FIRST> First type
   * @param <SECOND> Second type
   */
  public static final class CompareNatural<FIRST extends Comparable<? super FIRST>, SECOND extends Comparable<? super SECOND>> implements Comparator<Pair<? extends FIRST, ? extends SECOND>> {
    /**
     * Compare by first, then by second.
     * 
     * @param o1 First object
     * @param o2 Second object
     */
    @Override
    public int compare(Pair<? extends FIRST, ? extends SECOND> o1, Pair<? extends FIRST, ? extends SECOND> o2) {
      // try comparing by first
      if(o1.first != null) {
        if(o2.first == null) {
          return -1;
        }
        int delta1 = o1.first.compareTo(o2.first);
        if(delta1 != 0) {
          return delta1;
        }
      }
      else if(o2.first != null) {
        return +1;
      }
      // try comparing by second
      if(o1.second != null) {
        if(o2.second == null) {
          return -1;
        }
        int delta2 = o1.second.compareTo(o2.second);
        if(delta2 != 0) {
          return delta2;
        }
      }
      else if(o2.second != null) {
        return +1;
      }
      return 0;
    }

  }

  /**
   * Class to do a natural comparison on this class' first component.
   * 
   * @param <FIRST> First type
   * @param <SECOND> Second type
   */
  public static final class CompareNaturalFirst<FIRST extends Comparable<? super FIRST>, SECOND> implements Comparator<Pair<? extends FIRST, ? extends SECOND>> {
    /**
     * Compare by first component natural ordering
     * 
     * @param o1 First object
     * @param o2 Second object
     */
    @Override
    public int compare(Pair<? extends FIRST, ? extends SECOND> o1, Pair<? extends FIRST, ? extends SECOND> o2) {
      // try comparing by first
      if(o1.first != null) {
        if(o2.first == null) {
          return -1;
        }
        int delta1 = o1.first.compareTo(o2.first);
        if(delta1 != 0) {
          return delta1;
        }
      }
      else if(o2.first != null) {
        return +1;
      }
      return 0;
    }

  }

  /**
   * Class to do a natural comparison on this class' second component.
   * 
   * @param <FIRST> First type
   * @param <SECOND> Second type
   */
  public static final class CompareNaturalSecond<FIRST, SECOND extends Comparable<? super SECOND>> implements Comparator<Pair<? extends FIRST, ? extends SECOND>> {
    /**
     * Compare by second components natural ordering
     * 
     * @param o1 First object
     * @param o2 Second object
     */
    @Override
    public int compare(Pair<? extends FIRST, ? extends SECOND> o1, Pair<? extends FIRST, ? extends SECOND> o2) {
      // try comparing by second
      if(o1.second != null) {
        if(o2.second == null) {
          return -1;
        }
        int delta2 = o1.second.compareTo(o2.second);
        if(delta2 != 0) {
          return delta2;
        }
      }
      else if(o2.second != null) {
        return +1;
      }
      return 0;
    }

  }

  /**
   * Class to do a canonical swapped comparison on this class.
   * 
   * @param <FIRST> First type
   * @param <SECOND> Second type
   */
  public static final class CompareNaturalSwapped<FIRST extends Comparable<? super FIRST>, SECOND extends Comparable<? super SECOND>> implements Comparator<Pair<? extends FIRST, ? extends SECOND>> {
    /**
     * Compare by second component, using the ComparableSwapped interface.
     * 
     * @param o1 First object
     * @param o2 Second object
     */
    @Override
    public int compare(Pair<? extends FIRST, ? extends SECOND> o1, Pair<? extends FIRST, ? extends SECOND> o2) {
      // try comparing by second
      if(o1.second != null) {
        if(o2.second == null) {
          return -1;
        }
        int delta2 = o1.second.compareTo(o2.second);
        if(delta2 != 0) {
          return delta2;
        }
      }
      else if(o2.second != null) {
        return +1;
      }
      // try comparing by first
      if(o1.first != null) {
        if(o2.first == null) {
          return -1;
        }
        int delta1 = o1.first.compareTo(o2.first);
        if(delta1 != 0) {
          return delta1;
        }
      }
      else if(o2.first != null) {
        return +1;
      }
      return 0;
    }

  }

  /**
   * Compare two SimplePairs based on two comparators
   * 
   * @param <FIRST> first type
   * @param <SECOND> second type
   */
  public static class Compare<FIRST, SECOND> implements Comparator<Pair<? extends FIRST, ? extends SECOND>> {
    /**
     * A comparator for type FIRST.
     */
    private Comparator<? super FIRST> fcomparator;

    /**
     * A comparator for type FIRST.
     */
    private Comparator<? super SECOND> scomparator;

    /**
     * Provides a comparator for an {@link Pair} based on the given Comparator
     * for type <code>P</code>.
     * 
     * @param fcomparator Comparator for the first component
     * @param scomparator Comparator for the second component
     */
    public Compare(Comparator<? super FIRST> fcomparator, Comparator<? super SECOND> scomparator) {
      this.fcomparator = fcomparator;
      this.scomparator = scomparator;
    }

    /**
     * Two Objects of type {@link Pair} are compared based on the comparison of
     * their property using the comparators {@link #fcomparator}, then
     * {@link #scomparator}.
     * 
     * @param o1 First object
     * @param o2 Second object
     * @return comparison result
     * @see java.util.Comparator#compare
     */
    @Override
    public int compare(Pair<? extends FIRST, ? extends SECOND> o1, Pair<? extends FIRST, ? extends SECOND> o2) {
      int delta1 = fcomparator.compare(o1.getFirst(), o2.getFirst());
      if(delta1 != 0) {
        return delta1;
      }
      return scomparator.compare(o1.getSecond(), o2.getSecond());
    }
  }

  /**
   * Compare two SimplePairs based on a comparator for the first component.
   * 
   * @param <FIRST> first type
   * @param <SECOND> second type
   */
  public static class CompareByFirst<FIRST, SECOND> implements Comparator<Pair<? extends FIRST, ? extends SECOND>> {
    /**
     * A comparator for type P.
     */
    private Comparator<? super FIRST> comparator;

    /**
     * Provides a comparator for an {@link Pair} based on the given Comparator
     * for type <code>P</code>.
     * 
     * @param comparator a Comparator for type <code>P</code> to base the
     *        comparison of an {@link Pair} on
     */
    public CompareByFirst(Comparator<? super FIRST> comparator) {
      this.comparator = comparator;
    }

    /**
     * To Objects of type {@link Pair} are compared based on the comparison of
     * their property using the current {@link #comparator}.
     * 
     * @param o1 First object
     * @param o2 Second object
     * @return comparison result
     * @see java.util.Comparator#compare
     */
    @Override
    public int compare(Pair<? extends FIRST, ? extends SECOND> o1, Pair<? extends FIRST, ? extends SECOND> o2) {
      return comparator.compare(o1.getFirst(), o2.getFirst());
    }
  }

  /**
   * Compare two SimplePairs based on a comparator for the second component.
   * 
   * @param <FIRST> first type
   * @param <SECOND> second type
   */
  public static class CompareBySecond<FIRST, SECOND> implements Comparator<Pair<? extends FIRST, ? extends SECOND>> {
    /**
     * A comparator for type P.
     */
    private Comparator<? super SECOND> comparator;

    /**
     * Provides a comparator for an {@link Pair} based on the given Comparator
     * for type <code>P</code>.
     * 
     * @param comparator a Comparator for type <code>P</code> to base the
     *        comparison of an {@link Pair} on
     */
    public CompareBySecond(Comparator<? super SECOND> comparator) {
      this.comparator = comparator;
    }

    /**
     * To Objects of type {@link Pair} are compared based on the comparison of
     * their property using the current {@link #comparator}.
     * 
     * @param o1 First object
     * @param o2 Second object
     * @return comparison result
     * @see java.util.Comparator#compare
     */
    @Override
    public int compare(Pair<? extends FIRST, ? extends SECOND> o1, Pair<? extends FIRST, ? extends SECOND> o2) {
      return comparator.compare(o1.getSecond(), o2.getSecond());
    }
  }

  /**
   * Compare two SimplePairs based on two comparators, but by second component
   * first.
   * 
   * @param <FIRST> first type
   * @param <SECOND> second type
   */
  public static class CompareSwapped<FIRST, SECOND> implements Comparator<Pair<? extends FIRST, ? extends SECOND>> {
    /**
     * A comparator for type FIRST.
     */
    private Comparator<? super FIRST> fcomparator;

    /**
     * A comparator for type FIRST.
     */
    private Comparator<? super SECOND> scomparator;

    /**
     * Provides a comparator for an {@link Pair} based on the given Comparator
     * for type <code>P</code>.
     * 
     * @param fcomparator Comparator for the first component
     * @param scomparator Comparator for the second component
     */
    public CompareSwapped(Comparator<? super FIRST> fcomparator, Comparator<? super SECOND> scomparator) {
      this.fcomparator = fcomparator;
      this.scomparator = scomparator;
    }

    /**
     * Two Objects of type {@link Pair} are compared based on the comparison of
     * their property using the given comparators {@link #scomparator}, then
     * {@link #fcomparator}.
     * 
     * @param o1 First object
     * @param o2 Second object
     * @return comparison result
     * @see java.util.Comparator#compare
     */
    @Override
    public int compare(Pair<? extends FIRST, ? extends SECOND> o1, Pair<? extends FIRST, ? extends SECOND> o2) {
      int delta2 = scomparator.compare(o1.getSecond(), o2.getSecond());
      if(delta2 != 0) {
        return delta2;
      }
      return fcomparator.compare(o1.getFirst(), o2.getFirst());
    }
  }

}
