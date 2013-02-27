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
package org.apache.cayenne.serialization;

/**
 * An interface providing a chain assembly API to build Subgraphs. Subgraph
 * class itself implements this interface.
 */
public interface SubgraphBuilder {

	SubgraphBuilder addCallbacks(SubgraphCallback... callbacks);

	/**
	 * Adds a subgraph path based on a relationship and sets an optional list of
	 * callbacks for this path. On deserialization a new object will be created
	 * in the database. Returns a SubgraphBuilder with the root node being the
	 * node pointed by the path.
	 */
	SubgraphBuilder addClonePath(String path, SubgraphCallback... callbacks);

	/**
	 * Adds a subgraph path to a related entity that should be serialized as a
	 * reference to its ObjectId and sets an optional list of callbacks for this
	 * path. Such objects will be matched against existing DB rows, and no new
	 * objects will be created for them.
	 */
	SubgraphBuilder addRefPath(String path, SubgraphCallback... callbacks);

	SubgraphBuilder excludeAttribute(String path);
}
