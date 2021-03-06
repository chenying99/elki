package de.lmu.ifi.dbs.elki.algorithm.clustering;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.algorithm.AbstractSimpleAlgorithmTest;
import de.lmu.ifi.dbs.elki.algorithm.clustering.gdbscan.GeneralizedDBSCAN;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Performs a full DBSCAN run, and compares the result with a clustering derived
 * from the data set labels. This test ensures that DBSCAN performance doesn't
 * unexpectedly drop on this data set (and also ensures that the algorithms
 * work, as a side effect).
 * 
 * @author Elke Achtert
 * @author Erich Schubert
 * @author Katharina Rausch
 */
public class TestDBSCANResults extends AbstractSimpleAlgorithmTest implements JUnit4Test {
  /**
   * Run DBSCAN with fixed parameters and compare the result to a golden
   * standard.
   * 
   * @throws ParameterException
   */
  @Test
  public void testDBSCANResults() {
    Database db = makeSimpleDatabase(UNITTEST + "3clusters-and-noise-2d.csv", 330);

    // setup algorithm
    ListParameterization params = new ListParameterization();
    params.addParameter(DBSCAN.Parameterizer.EPSILON_ID, 0.04);
    params.addParameter(DBSCAN.Parameterizer.MINPTS_ID, 20);
    DBSCAN<DoubleVector, DoubleDistance> dbscan = ClassGenericsUtil.parameterizeOrAbort(DBSCAN.class, params);
    testParameterizationOk(params);

    // run DBSCAN on database
    Clustering<Model> result = dbscan.run(db);

    testFMeasure(db, result, 0.996413);
    testClusterSizes(result, new int[] { 29, 50, 101, 150 });
  }

  /**
   * Run DBSCAN with fixed parameters and compare the result to a golden
   * standard.
   * 
   * @throws ParameterException
   */
  @Test
  public void testDBSCANOnSingleLinkDataset() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);

    // Setup algorithm
    ListParameterization params = new ListParameterization();
    params.addParameter(DBSCAN.Parameterizer.EPSILON_ID, 11.5);
    params.addParameter(DBSCAN.Parameterizer.MINPTS_ID, 120);
    DBSCAN<DoubleVector, DoubleDistance> dbscan = ClassGenericsUtil.parameterizeOrAbort(DBSCAN.class, params);
    testParameterizationOk(params);

    // run DBSCAN on database
    Clustering<Model> result = dbscan.run(db);
    testFMeasure(db, result, 0.954382);
    testClusterSizes(result, new int[] { 11, 200, 203, 224 });
  }

  /**
   * Run DBSCAN with fixed parameters and compare the result to a golden
   * standard.
   * 
   * @throws ParameterException
   */
  @Test
  public void testGDBSCANResults() {
    Database db = makeSimpleDatabase(UNITTEST + "3clusters-and-noise-2d.csv", 330);

    // setup algorithm
    ListParameterization params = new ListParameterization();
    params.addParameter(DBSCAN.Parameterizer.EPSILON_ID, 0.04);
    params.addParameter(DBSCAN.Parameterizer.MINPTS_ID, 20);
    GeneralizedDBSCAN dbscan = ClassGenericsUtil.parameterizeOrAbort(GeneralizedDBSCAN.class, params);
    testParameterizationOk(params);

    // run DBSCAN on database
    Clustering<Model> result = dbscan.run(db);

    testClusterSizes(result, new int[] { 29, 50, 101, 150 });
    testFMeasure(db, result, 0.996413);
  }

  /**
   * Run DBSCAN with fixed parameters and compare the result to a golden
   * standard.
   * 
   * @throws ParameterException
   */
  @Test
  public void testGDBSCANOnSingleLinkDataset() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);

    // Setup algorithm
    ListParameterization params = new ListParameterization();
    params.addParameter(DBSCAN.Parameterizer.EPSILON_ID, 11.5);
    params.addParameter(DBSCAN.Parameterizer.MINPTS_ID, 120);
    GeneralizedDBSCAN dbscan = ClassGenericsUtil.parameterizeOrAbort(GeneralizedDBSCAN.class, params);
    testParameterizationOk(params);

    // run DBSCAN on database
    Clustering<Model> result = dbscan.run(db);
    testClusterSizes(result, new int[] { 11, 200, 203, 224 });
    testFMeasure(db, result, 0.954382);
  }
}