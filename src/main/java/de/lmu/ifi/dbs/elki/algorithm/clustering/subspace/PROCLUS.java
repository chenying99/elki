package de.lmu.ifi.dbs.elki.algorithm.clustering.subspace;

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

import gnu.trove.iterator.TIntIterator;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import de.lmu.ifi.dbs.elki.algorithm.clustering.AbstractProjectedClustering;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.Subspace;
import de.lmu.ifi.dbs.elki.data.model.SubspaceModel;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.distance.DistanceDBIDList;
import de.lmu.ifi.dbs.elki.database.ids.distance.DistanceDBIDPair;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.distance.distanceresultlist.DistanceDBIDResultUtil;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.IndefiniteProgress;
import de.lmu.ifi.dbs.elki.math.Mean;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Centroid;
import de.lmu.ifi.dbs.elki.utilities.RandomFactory;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.RandomParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.CTriple;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * <p/>
 * Provides the PROCLUS algorithm, an algorithm to find subspace clusters in
 * high dimensional spaces.
 * </p>
 * <p/>
 * Reference: <br>
 * C. C. Aggarwal, C. Procopiuc, J. L. Wolf, P. S. Yu, J. S. Park: Fast
 * Algorithms for Projected Clustering. <br>
 * In: Proc. ACM SIGMOD Int. Conf. on Management of Data (SIGMOD '99).
 * </p>
 * 
 * @author Elke Achtert
 * 
 * @apiviz.has SubspaceModel
 * 
 * @param <V> the type of NumberVector handled by this Algorithm
 */
// TODO: optimize by creating much less objects
@Title("PROCLUS: PROjected CLUStering")
@Description("Algorithm to find subspace clusters in high dimensional spaces.")
@Reference(authors = "C. C. Aggarwal, C. Procopiuc, J. L. Wolf, P. S. Yu, J. S. Park", title = "Fast Algorithms for Projected Clustering", booktitle = "Proc. ACM SIGMOD Int. Conf. on Management of Data (SIGMOD '99)", url = "http://dx.doi.org/10.1145/304181.304188")
public class PROCLUS<V extends NumberVector<?>> extends AbstractProjectedClustering<Clustering<SubspaceModel<V>>, V> implements SubspaceClusteringAlgorithm<SubspaceModel<V>> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(PROCLUS.class);

  /**
   * Parameter to specify the multiplier for the initial number of medoids, must
   * be an integer greater than 0.
   * <p>
   * Default value: {@code 10}
   * </p>
   * <p>
   * Key: {@code -proclus.mi}
   * </p>
   */
  public static final OptionID M_I_ID = new OptionID("proclus.mi", "The multiplier for the initial number of medoids.");

  /**
   * Holds the value of {@link #M_I_ID}.
   */
  private int m_i;

  /**
   * Random generator
   */
  private RandomFactory rnd;

  /**
   * Java constructor.
   * 
   * @param k k Parameter
   * @param k_i k_i Parameter
   * @param l l Parameter
   * @param m_i m_i Parameter
   * @param rnd Random generator
   */
  public PROCLUS(int k, int k_i, int l, int m_i, RandomFactory rnd) {
    super(k, k_i, l);
    this.m_i = m_i;
    this.rnd = rnd;
  }

  /**
   * Performs the PROCLUS algorithm on the given database.
   * 
   * @param database Database to process
   * @param relation Relation to process
   */
  public Clustering<SubspaceModel<V>> run(Database database, Relation<V> relation) {
    DistanceQuery<V, DoubleDistance> distFunc = this.getDistanceQuery(database);
    RangeQuery<V, DoubleDistance> rangeQuery = database.getRangeQuery(distFunc);
    final Random random = rnd.getSingleThreadedRandom();

    if (RelationUtil.dimensionality(relation) < l) {
      throw new IllegalStateException("Dimensionality of data < parameter l! " + "(" + RelationUtil.dimensionality(relation) + " < " + l + ")");
    }

    // TODO: use a StepProgress!
    // initialization phase
    if (LOG.isVerbose()) {
      LOG.verbose("1. Initialization phase...");
    }
    int sampleSize = Math.min(relation.size(), k_i * k);
    DBIDs sampleSet = DBIDUtil.randomSample(relation.getDBIDs(), sampleSize, random.nextLong());

    int medoidSize = Math.min(relation.size(), m_i * k);
    DBIDs medoids = greedy(distFunc, sampleSet, medoidSize, random);

    if (LOG.isDebugging()) {
      StringBuilder msg = new StringBuilder();
      msg.append('\n');
      msg.append("sampleSize ").append(sampleSize).append('\n');
      msg.append("sampleSet ").append(sampleSet).append('\n');
      msg.append("medoidSize ").append(medoidSize).append('\n');
      msg.append("m ").append(medoids).append('\n');
      LOG.debugFine(msg.toString());
    }

    // iterative phase
    if (LOG.isVerbose()) {
      LOG.verbose("2. Iterative phase...");
    }
    double bestObjective = Double.POSITIVE_INFINITY;
    ModifiableDBIDs m_best = null;
    ModifiableDBIDs m_bad = null;
    ModifiableDBIDs m_current = initialSet(medoids, k, random);

    if (LOG.isDebugging()) {
      StringBuilder msg = new StringBuilder();
      msg.append('\n');
      msg.append("m_c ").append(m_current).append('\n');
      LOG.debugFine(msg.toString());
    }

    IndefiniteProgress cprogress = LOG.isVerbose() ? new IndefiniteProgress("Current number of clusters:", LOG) : null;

    // TODO: Use DataStore and Trove for performance
    Map<DBID, PROCLUSCluster> clusters = null;
    int loops = 0;
    while (loops < 10) {
      Map<DBID, TIntSet> dimensions = findDimensions(m_current, relation, distFunc, rangeQuery);
      clusters = assignPoints(dimensions, relation);
      double objectiveFunction = evaluateClusters(clusters, dimensions, relation);

      if (objectiveFunction < bestObjective) {
        // restart counting loops
        loops = 0;
        bestObjective = objectiveFunction;
        m_best = m_current;
        m_bad = computeBadMedoids(clusters, (int) (relation.size() * 0.1 / k));
      }

      m_current = computeM_current(medoids, m_best, m_bad, random);
      loops++;
      if (cprogress != null) {
        cprogress.setProcessed(clusters.size(), LOG);
      }
    }

    if (cprogress != null) {
      cprogress.setCompleted(LOG);
    }

    // refinement phase
    if (LOG.isVerbose()) {
      LOG.verbose("3. Refinement phase...");
    }

    List<Pair<V, TIntSet>> dimensions = findDimensions(new ArrayList<>(clusters.values()), relation);
    List<PROCLUSCluster> finalClusters = finalAssignment(dimensions, relation);

    // build result
    int numClusters = 1;
    Clustering<SubspaceModel<V>> result = new Clustering<>("ProClus clustering", "proclus-clustering");
    for (PROCLUSCluster c : finalClusters) {
      Cluster<SubspaceModel<V>> cluster = new Cluster<>(c.objectIDs);
      cluster.setModel(new SubspaceModel<>(new Subspace(c.getDimensions()), c.centroid));
      cluster.setName("cluster_" + numClusters++);

      result.addToplevelCluster(cluster);
    }
    return result;
  }

  /**
   * Returns a piercing set of k medoids from the specified sample set.
   * 
   * @param distFunc the distance function
   * @param sampleSet the sample set
   * @param m the number of medoids to be returned
   * @param random random number generator
   * @return a piercing set of m medoids from the specified sample set
   */
  private ModifiableDBIDs greedy(DistanceQuery<V, DoubleDistance> distFunc, DBIDs sampleSet, int m, Random random) {
    ArrayModifiableDBIDs s = DBIDUtil.newArray(sampleSet);
    ModifiableDBIDs medoids = DBIDUtil.newHashSet();

    // m_1 is random point of S
    DBID m_i = s.remove(random.nextInt(s.size()));
    medoids.add(m_i);
    if (LOG.isDebugging()) {
      LOG.debugFiner("medoids " + medoids);
    }

    // compute distances between each point in S and m_i
    // FIXME: don't use maps, so we can work with DBIDRef
    Map<DBID, DistanceDBIDPair<DoubleDistance>> distances = new HashMap<>();
    for (DBIDIter iter = s.iter(); iter.valid(); iter.advance()) {
      DBID id = DBIDUtil.deref(iter);
      DoubleDistance dist = distFunc.distance(id, m_i);
      distances.put(id, DBIDUtil.newDistancePair(dist, id));
    }

    for (int i = 1; i < m; i++) {
      // choose medoid m_i to be far from previous medoids
      List<DistanceDBIDPair<DoubleDistance>> d = new ArrayList<>(distances.values());
      DistanceDBIDResultUtil.sortByDistance(d);

      m_i = DBIDUtil.deref(d.get(d.size() - 1));
      medoids.add(m_i);
      s.remove(m_i);
      distances.remove(m_i);

      // compute distances of each point to closest medoid
      for (DBIDIter iter = s.iter(); iter.valid(); iter.advance()) {
        DBID id = DBIDUtil.deref(iter);
        DoubleDistance dist_new = distFunc.distance(id, m_i);
        DoubleDistance dist_old = distances.get(id).getDistance();

        DoubleDistance dist = dist_new.compareTo(dist_old) < 0 ? dist_new : dist_old;
        distances.put(id, DBIDUtil.newDistancePair(dist, id));
      }

      if (LOG.isDebugging()) {
        LOG.debugFiner("medoids " + medoids);
      }
    }

    return medoids;
  }

  /**
   * Returns a set of k elements from the specified sample set.
   * 
   * @param sampleSet the sample set
   * @param k the number of samples to be returned
   * @param random random number generator
   * @return a set of k elements from the specified sample set
   */
  private ModifiableDBIDs initialSet(DBIDs sampleSet, int k, Random random) {
    ArrayModifiableDBIDs s = DBIDUtil.newArray(sampleSet);
    ModifiableDBIDs initialSet = DBIDUtil.newHashSet();
    while (initialSet.size() < k) {
      DBID next = s.remove(random.nextInt(s.size()));
      initialSet.add(next);
    }
    return initialSet;
  }

  /**
   * Computes the set of medoids in current iteration.
   * 
   * @param m the medoids
   * @param m_best the best set of medoids found so far
   * @param m_bad the bad medoids
   * @param random random number generator
   * @return m_current, the set of medoids in current iteration
   */
  private ModifiableDBIDs computeM_current(DBIDs m, DBIDs m_best, DBIDs m_bad, Random random) {
    ArrayModifiableDBIDs m_list = DBIDUtil.newArray(m);
    m_list.removeDBIDs(m_best);

    ModifiableDBIDs m_current = DBIDUtil.newHashSet();
    for (DBIDIter iter = m_best.iter(); iter.valid(); iter.advance()) {
      DBID m_i = DBIDUtil.deref(iter);
      if (m_bad.contains(m_i)) {
        int currentSize = m_current.size();
        while (m_current.size() == currentSize) {
          DBID next = m_list.remove(random.nextInt(m_list.size()));
          m_current.add(next);
        }
      } else {
        m_current.add(m_i);
      }
    }

    return m_current;
  }

  /**
   * Computes the localities of the specified medoids: for each medoid m the
   * objects in the sphere centered at m with radius minDist are determined,
   * where minDist is the minimum distance between medoid m and any other medoid
   * m_i.
   * 
   * @param medoids the ids of the medoids
   * @param database the database holding the objects
   * @param distFunc the distance function
   * @return a mapping of the medoid's id to its locality
   */
  private Map<DBID, DistanceDBIDList<DoubleDistance>> getLocalities(DBIDs medoids, Relation<V> database, DistanceQuery<V, DoubleDistance> distFunc, RangeQuery<V, DoubleDistance> rangeQuery) {
    Map<DBID, DistanceDBIDList<DoubleDistance>> result = new HashMap<>();

    for (DBIDIter iter = medoids.iter(); iter.valid(); iter.advance()) {
      DBID m = DBIDUtil.deref(iter);
      // determine minimum distance between current medoid m and any other
      // medoid m_i
      DoubleDistance minDist = null;
      for (DBIDIter iter2 = medoids.iter(); iter2.valid(); iter2.advance()) {
        DBID m_i = DBIDUtil.deref(iter2);
        if (DBIDUtil.equal(m_i, m)) {
          continue;
        }
        DoubleDistance currentDist = distFunc.distance(m, m_i);
        if (minDist == null || currentDist.compareTo(minDist) < 0) {
          minDist = currentDist;
        }
      }

      // determine points in sphere centered at m with radius minDist
      assert minDist != null;
      DistanceDBIDList<DoubleDistance> qr = rangeQuery.getRangeForDBID(m, minDist);
      result.put(m, qr);
    }

    return result;
  }

  /**
   * Determines the set of correlated dimensions for each medoid in the
   * specified medoid set.
   * 
   * @param medoids the set of medoids
   * @param database the database containing the objects
   * @param distFunc the distance function
   * @return the set of correlated dimensions for each medoid in the specified
   *         medoid set
   */
  private Map<DBID, TIntSet> findDimensions(DBIDs medoids, Relation<V> database, DistanceQuery<V, DoubleDistance> distFunc, RangeQuery<V, DoubleDistance> rangeQuery) {
    // get localities
    Map<DBID, DistanceDBIDList<DoubleDistance>> localities = getLocalities(medoids, database, distFunc, rangeQuery);

    // compute x_ij = avg distance from points in l_i to medoid m_i
    int dim = RelationUtil.dimensionality(database);
    Map<DBID, double[]> averageDistances = new HashMap<>();

    for (DBIDIter iter = medoids.iter(); iter.valid(); iter.advance()) {
      DBID m_i = DBIDUtil.deref(iter);
      V medoid_i = database.get(m_i);
      DistanceDBIDList<DoubleDistance> l_i = localities.get(m_i);
      double[] x_i = new double[dim];
      for (DBIDIter qr = l_i.iter(); qr.valid(); qr.advance()) {
        V o = database.get(qr);
        for (int d = 0; d < dim; d++) {
          x_i[d] += Math.abs(medoid_i.doubleValue(d) - o.doubleValue(d));
        }
      }
      for (int d = 0; d < dim; d++) {
        x_i[d] /= l_i.size();
      }
      averageDistances.put(m_i, x_i);
    }

    Map<DBID, TIntSet> dimensionMap = new HashMap<>();
    List<CTriple<Double, DBID, Integer>> z_ijs = new ArrayList<>();
    for (DBIDIter iter = medoids.iter(); iter.valid(); iter.advance()) {
      DBID m_i = DBIDUtil.deref(iter);
      TIntSet dims_i = new TIntHashSet();
      dimensionMap.put(m_i, dims_i);

      double[] x_i = averageDistances.get(m_i);
      // y_i
      double y_i = 0;
      for (int j = 0; j < dim; j++) {
        y_i += x_i[j];
      }
      y_i /= dim;

      // sigma_i
      double sigma_i = 0;
      for (int j = 0; j < dim; j++) {
        double diff = x_i[j] - y_i;
        sigma_i += diff * diff;
      }
      sigma_i /= (dim - 1);
      sigma_i = Math.sqrt(sigma_i);

      for (int j = 0; j < dim; j++) {
        z_ijs.add(new CTriple<>((x_i[j] - y_i) / sigma_i, m_i, j));
      }
    }
    Collections.sort(z_ijs);

    int max = Math.max(k * l, 2);
    for (int m = 0; m < max; m++) {
      CTriple<Double, DBID, Integer> z_ij = z_ijs.get(m);
      TIntSet dims_i = dimensionMap.get(z_ij.getSecond());
      dims_i.add(z_ij.getThird());

      if (LOG.isDebugging()) {
        StringBuilder msg = new StringBuilder();
        msg.append('\n');
        msg.append("z_ij ").append(z_ij).append('\n');
        msg.append("D_i ").append(dims_i).append('\n');
        LOG.debugFiner(msg.toString());
      }
    }
    return dimensionMap;
  }

  /**
   * Refinement step that determines the set of correlated dimensions for each
   * cluster centroid.
   * 
   * @param clusters the list of clusters
   * @param database the database containing the objects
   * @return the set of correlated dimensions for each specified cluster
   *         centroid
   */
  private List<Pair<V, TIntSet>> findDimensions(List<PROCLUSCluster> clusters, Relation<V> database) {
    // compute x_ij = avg distance from points in c_i to c_i.centroid
    int dim = RelationUtil.dimensionality(database);
    Map<Integer, double[]> averageDistances = new HashMap<>();

    for (int i = 0; i < clusters.size(); i++) {
      PROCLUSCluster c_i = clusters.get(i);
      double[] x_i = new double[dim];
      for (DBIDIter iter = c_i.objectIDs.iter(); iter.valid(); iter.advance()) {
        V o = database.get(iter);
        for (int d = 0; d < dim; d++) {
          x_i[d] += Math.abs(c_i.centroid.doubleValue(d) - o.doubleValue(d));
        }
      }
      for (int d = 0; d < dim; d++) {
        x_i[d] /= c_i.objectIDs.size();
      }
      averageDistances.put(i, x_i);
    }

    List<CTriple<Double, Integer, Integer>> z_ijs = new ArrayList<>();
    for (int i = 0; i < clusters.size(); i++) {
      double[] x_i = averageDistances.get(i);
      // y_i
      double y_i = 0;
      for (int j = 0; j < dim; j++) {
        y_i += x_i[j];
      }
      y_i /= dim;

      // sigma_i
      double sigma_i = 0;
      for (int j = 0; j < dim; j++) {
        double diff = x_i[j] - y_i;
        sigma_i += diff * diff;
      }
      sigma_i /= (dim - 1);
      sigma_i = Math.sqrt(sigma_i);

      for (int j = 0; j < dim; j++) {
        z_ijs.add(new CTriple<>((x_i[j] - y_i) / sigma_i, i, j));
      }
    }
    Collections.sort(z_ijs);

    // mapping cluster index -> dimensions
    Map<Integer, TIntSet> dimensionMap = new HashMap<>();
    int max = Math.max(k * l, 2);
    for (int m = 0; m < max; m++) {
      CTriple<Double, Integer, Integer> z_ij = z_ijs.get(m);
      TIntSet dims_i = dimensionMap.get(z_ij.getSecond());
      if (dims_i == null) {
        dims_i = new TIntHashSet();
        dimensionMap.put(z_ij.getSecond(), dims_i);
      }
      dims_i.add(z_ij.getThird());

      if (LOG.isDebugging()) {
        StringBuilder msg = new StringBuilder();
        msg.append('\n');
        msg.append("z_ij ").append(z_ij).append('\n');
        msg.append("D_i ").append(dims_i).append('\n');
        LOG.debugFiner(msg.toString());
      }
    }

    // mapping cluster -> dimensions
    List<Pair<V, TIntSet>> result = new ArrayList<>();
    for (int i : dimensionMap.keySet()) {
      TIntSet dims_i = dimensionMap.get(i);
      PROCLUSCluster c_i = clusters.get(i);
      result.add(new Pair<>(c_i.centroid, dims_i));
    }
    return result;
  }

  /**
   * Assigns the objects to the clusters.
   * 
   * @param dimensions set of correlated dimensions for each medoid of the
   *        cluster
   * @param database the database containing the objects
   * @return the assignments of the object to the clusters
   */
  private Map<DBID, PROCLUSCluster> assignPoints(Map<DBID, TIntSet> dimensions, Relation<V> database) {
    Map<DBID, ModifiableDBIDs> clusterIDs = new HashMap<>();
    for (DBID m_i : dimensions.keySet()) {
      clusterIDs.put(m_i, DBIDUtil.newHashSet());
    }

    for (DBIDIter it = database.iterDBIDs(); it.valid(); it.advance()) {
      DBID p_id = DBIDUtil.deref(it);
      V p = database.get(p_id);
      DistanceDBIDPair<DoubleDistance> minDist = null;
      for (DBID m_i : dimensions.keySet()) {
        V m = database.get(m_i);
        DistanceDBIDPair<DoubleDistance> currentDist = DBIDUtil.newDistancePair(manhattanSegmentalDistance(p, m, dimensions.get(m_i)), m_i);
        if (minDist == null || currentDist.compareByDistance(minDist) < 0) {
          minDist = currentDist;
        }
      }
      // add p to cluster with mindist
      assert minDist != null;
      ModifiableDBIDs ids = clusterIDs.get(DBIDUtil.deref(minDist));
      ids.add(p_id);
    }

    Map<DBID, PROCLUSCluster> clusters = new HashMap<>();
    for (DBID m_i : dimensions.keySet()) {
      ModifiableDBIDs objectIDs = clusterIDs.get(m_i);
      if (!objectIDs.isEmpty()) {
        TIntSet clusterDimensions = dimensions.get(m_i);
        V centroid = Centroid.make(database, objectIDs).toVector(database);
        clusters.put(m_i, new PROCLUSCluster(objectIDs, clusterDimensions, centroid));
      }
    }

    if (LOG.isDebugging()) {
      StringBuilder msg = new StringBuilder();
      msg.append('\n');
      msg.append("clusters ").append(clusters).append('\n');
      LOG.debugFine(msg.toString());
    }
    return clusters;
  }

  /**
   * Refinement step to assign the objects to the final clusters.
   * 
   * @param dimensions pair containing the centroid and the set of correlated
   *        dimensions for the centroid
   * @param database the database containing the objects
   * @return the assignments of the object to the clusters
   */
  private List<PROCLUSCluster> finalAssignment(List<Pair<V, TIntSet>> dimensions, Relation<V> database) {
    Map<Integer, ModifiableDBIDs> clusterIDs = new HashMap<>();
    for (int i = 0; i < dimensions.size(); i++) {
      clusterIDs.put(i, DBIDUtil.newHashSet());
    }

    for (DBIDIter it = database.iterDBIDs(); it.valid(); it.advance()) {
      DBID p_id = DBIDUtil.deref(it);
      V p = database.get(p_id);
      Pair<DoubleDistance, Integer> minDist = null;
      for (int i = 0; i < dimensions.size(); i++) {
        Pair<V, TIntSet> pair_i = dimensions.get(i);
        V c_i = pair_i.first;
        TIntSet dimensions_i = pair_i.second;
        DoubleDistance currentDist = manhattanSegmentalDistance(p, c_i, dimensions_i);
        if (minDist == null || currentDist.compareTo(minDist.first) < 0) {
          minDist = new Pair<>(currentDist, i);
        }
      }
      // add p to cluster with mindist
      assert minDist != null;
      ModifiableDBIDs ids = clusterIDs.get(minDist.second);
      ids.add(p_id);
    }

    List<PROCLUSCluster> clusters = new ArrayList<>();
    for (int i = 0; i < dimensions.size(); i++) {
      ModifiableDBIDs objectIDs = clusterIDs.get(i);
      if (!objectIDs.isEmpty()) {
        TIntSet clusterDimensions = dimensions.get(i).second;
        V centroid = Centroid.make(database, objectIDs).toVector(database);
        clusters.add(new PROCLUSCluster(objectIDs, clusterDimensions, centroid));
      }
    }

    if (LOG.isDebugging()) {
      StringBuilder msg = new StringBuilder();
      msg.append('\n');
      msg.append("clusters ").append(clusters).append('\n');
      LOG.debugFine(msg.toString());
    }
    return clusters;
  }

  /**
   * Returns the Manhattan segmental distance between o1 and o2 relative to the
   * specified dimensions.
   * 
   * @param o1 the first object
   * @param o2 the second object
   * @param dimensions the dimensions to be considered
   * @return the Manhattan segmental distance between o1 and o2 relative to the
   *         specified dimensions
   */
  private DoubleDistance manhattanSegmentalDistance(V o1, V o2, TIntSet dimensions) {
    double result = 0;
    for (TIntIterator iter = dimensions.iterator(); iter.hasNext();) {
      final int d = iter.next();
      result += Math.abs(o1.doubleValue(d) - o2.doubleValue(d));
    }
    result /= dimensions.size();
    return new DoubleDistance(result);
  }

  /**
   * Evaluates the quality of the clusters.
   * 
   * @param clusters the clusters to be evaluated
   * @param dimensions the dimensions associated with each cluster
   * @param database the database holding the objects
   * @return a measure for the cluster quality
   */
  private double evaluateClusters(Map<DBID, PROCLUSCluster> clusters, Map<DBID, TIntSet> dimensions, Relation<V> database) {
    double result = 0;
    for (DBID m_i : clusters.keySet()) {
      PROCLUSCluster c_i = clusters.get(m_i);
      V centroid_i = c_i.centroid;

      TIntSet dims_i = dimensions.get(m_i);
      double w_i = 0;
      for (TIntIterator iter = dims_i.iterator(); iter.hasNext();) {
        final int j = iter.next();
        w_i += avgDistance(centroid_i, c_i.objectIDs, database, j);
      }

      w_i /= dimensions.keySet().size();
      result += c_i.objectIDs.size() * w_i;
    }

    return result / database.size();
  }

  /**
   * Computes the average distance of the objects to the centroid along the
   * specified dimension.
   * 
   * @param centroid the centroid
   * @param objectIDs the set of objects ids
   * @param database the database holding the objects
   * @param dimension the dimension for which the average distance is computed
   * @return the average distance of the objects to the centroid along the
   *         specified dimension
   */
  private double avgDistance(V centroid, DBIDs objectIDs, Relation<V> database, int dimension) {
    Mean avg = new Mean();
    for (DBIDIter iter = objectIDs.iter(); iter.valid(); iter.advance()) {
      V o = database.get(iter);
      avg.put(Math.abs(centroid.doubleValue(dimension) - o.doubleValue(dimension)));
    }
    return avg.getMean();
  }

  /**
   * Computes the bad medoids, where the medoid of a cluster with less than the
   * specified threshold of objects is bad.
   * 
   * @param clusters the clusters
   * @param threshold the threshold
   * @return the bad medoids
   */
  private ModifiableDBIDs computeBadMedoids(Map<DBID, PROCLUSCluster> clusters, int threshold) {
    ModifiableDBIDs badMedoids = DBIDUtil.newHashSet();
    for (DBID m_i : clusters.keySet()) {
      PROCLUSCluster c_i = clusters.get(m_i);
      if (c_i.objectIDs.size() < threshold) {
        badMedoids.add(m_i);
      }
    }
    return badMedoids;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Encapsulates the attributes of a cluster.
   * 
   * @apiviz.exclude
   */
  private class PROCLUSCluster {
    /**
     * The ids of the objects belonging to this cluster.
     */
    ModifiableDBIDs objectIDs;

    /**
     * The correlated dimensions of this cluster.
     */
    TIntSet dimensions;

    /**
     * The centroids of this cluster along each dimension.
     */
    V centroid;

    /**
     * Provides a new cluster with the specified parameters.
     * 
     * @param objectIDs the ids of the objects belonging to this cluster
     * @param dimensions the correlated dimensions of this cluster
     * @param centroid the centroid of this cluster
     */
    public PROCLUSCluster(ModifiableDBIDs objectIDs, TIntSet dimensions, V centroid) {
      this.objectIDs = objectIDs;
      this.dimensions = dimensions;
      this.centroid = centroid;
    }

    @Override
    public String toString() {
      StringBuilder result = new StringBuilder();
      result.append("Dimensions: [");
      boolean notFirst = false;
      for (TIntIterator iter = dimensions.iterator(); iter.hasNext();) {
        if (notFirst) {
          result.append(',');
        } else {
          notFirst = true;
        }
        result.append(iter.next());
      }
      result.append(']');

      result.append("\nCentroid: ").append(centroid);
      return result.toString();
    }

    /**
     * Returns the correlated dimensions of this cluster as BitSet.
     * 
     * @return the correlated dimensions of this cluster as BitSet
     */
    public BitSet getDimensions() {
      BitSet result = new BitSet();
      for (TIntIterator iter = dimensions.iterator(); iter.hasNext();) {
        result.set(iter.next());
      }
      return result;
    }
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<V extends NumberVector<?>> extends AbstractProjectedClustering.Parameterizer {
    /**
     * Parameter to specify the random generator seed.
     */
    public static final OptionID SEED_ID = new OptionID("proclus.seed", "The random number generator seed.");

    protected int m_i = -1;

    protected RandomFactory rnd;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      configK(config);
      configKI(config);
      configL(config);

      IntParameter m_iP = new IntParameter(M_I_ID, 10);
      m_iP.addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if (config.grab(m_iP)) {
        m_i = m_iP.getValue();
      }

      RandomParameter rndP = new RandomParameter(SEED_ID);
      if (config.grab(rndP)) {
        rnd = rndP.getValue();
      }
    }

    @Override
    protected PROCLUS<V> makeInstance() {
      return new PROCLUS<>(k, k_i, l, m_i, rnd);
    }
  }
}
