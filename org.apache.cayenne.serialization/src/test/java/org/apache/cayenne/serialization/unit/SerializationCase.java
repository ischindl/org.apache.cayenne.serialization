/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/
package org.apache.cayenne.serialization.unit;

import java.io.File;

import junit.framework.TestCase;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.DbGenerator;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.log.CommonsJdbcEventLogger;
import org.apache.cayenne.map.DataMap;

public abstract class SerializationCase extends TestCase {

	static final String TEST_DIR = "target/testrun";

	private static boolean pkFixed;

	static ServerRuntime cRuntime;

	public SerializationCase() {
		// this is needed until CAY-1276 is fixed

		if (!pkFixed) {

			cRuntime = new ServerRuntime("cayenne-serialization.xml");
			DataDomain domain = cRuntime.getDataDomain();
			DataNode node = domain.getDataNodes().iterator().next();
			DataMap dataMap = domain.getDataMaps().iterator().next();

			DbGenerator dbGenerator = new DbGenerator(node.getAdapter(), dataMap, new CommonsJdbcEventLogger());
			dbGenerator.setShouldDropPKSupport(true);
			dbGenerator.setShouldCreatePKSupport(true);

			dbGenerator.setShouldCreateTables(false);
			dbGenerator.setShouldDropTables(false);
			dbGenerator.setShouldCreateFKConstraints(false);

			try {
				dbGenerator.runGenerator(node.getDataSource());
			} catch (Exception e) {
				throw new CayenneRuntimeException("Error generating PK", e);
			}

			pkFixed = true;
		}
	}

	protected ObjectContext newContext() {
		return cRuntime.getContext();
	}

	protected File tempFile(String extension) {
		File baseDir = new File(TEST_DIR);
		baseDir.mkdirs();

		if (extension == null) {
			extension = "";
		}

		String baseName = String.valueOf(System.currentTimeMillis());
		for (int i = 0; i < 1000; i++) {
			File file = new File(baseDir, baseName + extension);
			if (!file.exists()) {
				return file;
			}
		}

		// should never happen
		throw new IllegalStateException("Too many files with the same base name of " + baseName + "...");
	}
}
