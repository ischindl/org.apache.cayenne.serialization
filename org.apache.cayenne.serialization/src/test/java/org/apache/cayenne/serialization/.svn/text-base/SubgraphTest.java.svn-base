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

import org.apache.cayenne.query.Query;
import org.apache.cayenne.serialization.persistent.Table1;
import org.apache.cayenne.serialization.persistent.Table2;
import org.apache.cayenne.serialization.unit.SerializationCase;

public class SubgraphTest extends SerializationCase {

	public void testGetRootNode() {

		Subgraph<Table1> subgraph = new Subgraph<Table1>(Table1.class,
				newContext().getEntityResolver());
		assertNotNull(subgraph.getRootNode());
		assertEquals(0, subgraph.getRootNode().getChildren().size());
	}

	public void testAddCallbackDerserialization() {
		Subgraph<Table2> subgraph = new Subgraph<Table2>(Table2.class,
				newContext().getEntityResolver());
		subgraph.addRefPath(Table2.TABLE1_PROPERTY,
				new DeserializationCallback() {
					public void postDeserialize(SubgraphNode node, Object object) {
						// noop
					}
				});

		SubgraphNode node = subgraph.getRootNode().getChild(
				Table2.TABLE1_PROPERTY, false);

		assertEquals(0, node.getSerializationCallbacks().size());
		assertEquals(1, node.getDeserializationCallbacks().size());
	}

	public void testAddSerializationCallback() {
		Subgraph<Table2> subgraph = new Subgraph<Table2>(Table2.class,
				newContext().getEntityResolver());
		subgraph.addRefPath(Table2.TABLE1_PROPERTY,
				new SerializationCallback() {

					public Query relationshipQuery(SubgraphNode node,
							Object sourceObject) {
						// TODO Auto-generated method stub
						return null;
					}
				});

		SubgraphNode node = subgraph.getRootNode().getChild(
				Table2.TABLE1_PROPERTY, false);

		assertEquals(1, node.getSerializationCallbacks().size());
		assertEquals(0, node.getDeserializationCallbacks().size());
	}

	public void testAddSerializeByReferencePathToOne() {
		Subgraph<Table2> subgraph = new Subgraph<Table2>(Table2.class,
				newContext().getEntityResolver());
		subgraph.addRefPath(Table2.TABLE1_PROPERTY);

		SubgraphNode node = subgraph.getRootNode();
		assertEquals(1, node.getChildren().size());

		SubgraphNode child = node.getChildren().iterator().next();
		assertTrue(child.isSerializedByReference());
	}

	public void testAddSerializeByReferencePathToMany() {
		Subgraph<Table1> subgraph = new Subgraph<Table1>(Table1.class,
				newContext().getEntityResolver());
		subgraph.addRefPath(Table1.TABLE2S_PROPERTY);

		SubgraphNode node = subgraph.getRootNode();
		assertEquals(1, node.getChildren().size());

		SubgraphNode child = node.getChildren().iterator().next();
		assertTrue(child.isSerializedByReference());
	}
}
