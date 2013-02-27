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
package org.apache.cayenne.serialization.xstream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.serialization.DeserializationCallback;
import org.apache.cayenne.serialization.Subgraph;
import org.apache.cayenne.serialization.SubgraphNode;
import org.apache.cayenne.serialization.persistent.Table1;
import org.apache.cayenne.serialization.persistent.Table2;
import org.apache.cayenne.serialization.unit.SerializationCase;

public class XStreamDeserializerTest extends SerializationCase {

	public void testDeserializeRoot() throws IOException {

		ObjectContext context = newContext();
		Table1 t11 = context.newObject(Table1.class);
		t11.setName("t11");

		context.commitChanges();

		Subgraph<Table1> subgraph = new Subgraph<Table1>(Table1.class, context
				.getEntityResolver());

		XStreamDeserializer deserializer = new XStreamDeserializer();

		String xml = "<Table1><name>t11</name></Table1>";

		Table1 result;
		InputStream in = new ByteArrayInputStream(xml.getBytes());
		try {
			result = deserializer.deserialize(context, subgraph, in);
		} finally {
			in.close();
		}

		assertNotNull(result);
		assertEquals(PersistenceState.COMMITTED, result.getPersistenceState());
		assertEquals("t11", result.getName());
	}

	public void testDeserializeByValueToOne() throws IOException {

		ObjectContext context = newContext();
		Table1 t11 = context.newObject(Table1.class);
		t11.setName("t11");

		Table2 t21 = context.newObject(Table2.class);
		t21.setName("t21");
		t21.setTable1(t11);

		context.commitChanges();

		Subgraph<Table2> subgraph = new Subgraph<Table2>(Table2.class, context
				.getEntityResolver());
		subgraph.addClonePath(Table2.TABLE1_PROPERTY);

		XStreamDeserializer deserializer = new XStreamDeserializer();

		String xml = "<Table2><name>t21</name><table1><Table1><name>t11</name></Table1></table1></Table2>";

		Table2 result;
		InputStream in = new ByteArrayInputStream(xml.getBytes());
		try {
			result = deserializer.deserialize(context, subgraph, in);
		} finally {
			in.close();
		}

		assertNotNull(result);
		assertNotNull(result.getTable1());
		assertEquals(PersistenceState.COMMITTED, result.getPersistenceState());
		assertEquals(PersistenceState.COMMITTED, result.getTable1()
				.getPersistenceState());

		assertEquals("t21", result.getName());
		assertNotSame(t21, result);
		assertNotSame(t11, result.getTable1());
		assertEquals("t11", result.getTable1().getName());

	}

	public void testDeserializeByReferenceToOne() throws IOException {

		ObjectContext context = newContext();
		Table1 t11 = context.newObject(Table1.class);
		t11.setName("t11");

		Table2 t21 = context.newObject(Table2.class);
		t21.setName("t21");
		t21.setTable1(t11);

		context.commitChanges();

		Subgraph<Table2> subgraph = new Subgraph<Table2>(Table2.class, context
				.getEntityResolver());
		subgraph.addRefPath(Table2.TABLE1_PROPERTY);

		XStreamDeserializer deserializer = new XStreamDeserializer();

		int id = Cayenne.intPKForObject(t11);
		String xml = "<Table2><name>t21</name><table1><Table1 ref=\"true\"><PK>"
				+ id + "</PK></Table1></table1></Table2>";

		Table2 result;
		InputStream in = new ByteArrayInputStream(xml.getBytes());
		try {
			result = deserializer.deserialize(context, subgraph, in);
		} finally {
			in.close();
		}

		assertNotNull(result);
		assertNotNull(result.getTable1());
		assertEquals(PersistenceState.COMMITTED, result.getPersistenceState());
		assertEquals(PersistenceState.COMMITTED, result.getTable1()
				.getPersistenceState());

		assertEquals("t21", result.getName());
		assertNotSame(t21, result);
		assertSame(t11, result.getTable1());

	}

	public void testDeserializeByValueToMany() throws IOException {

		ObjectContext context = newContext();

		Subgraph<Table1> subgraph = new Subgraph<Table1>(Table1.class, context
				.getEntityResolver());
		subgraph.addClonePath(Table1.TABLE2S_PROPERTY);

		XStreamDeserializer deserializer = new XStreamDeserializer();

		String xml = "<Table1><name>t11</name><table2s>"
				+ "<Table2><name>t21</name></Table2>"
				+ "<Table2><name>t22</name></Table2></table2s></Table1>";

		Table1 result;
		InputStream in = new ByteArrayInputStream(xml.getBytes());
		try {
			result = deserializer.deserialize(context, subgraph, in);
		} finally {
			in.close();
		}

		assertNotNull(result);
		assertEquals(2, result.getTable2s().size());
		assertEquals(PersistenceState.COMMITTED, result.getPersistenceState());
		assertEquals(PersistenceState.COMMITTED, result.getTable2s().get(0)
				.getPersistenceState());

		assertEquals("t11", result.getName());

		Set<String> names = new HashSet<String>();
		names.add(result.getTable2s().get(0).getName());
		names.add(result.getTable2s().get(1).getName());
		assertTrue(names.contains("t21"));
		assertTrue(names.contains("t22"));
	}

	public void testCallbackByReference() throws IOException {

		ObjectContext context = newContext();
		Table1 t11 = context.newObject(Table1.class);
		t11.setName("t11");

		Table2 t21 = context.newObject(Table2.class);
		t21.setName("t21");
		t21.setTable1(t11);

		context.commitChanges();

		final boolean[] callbackInvoked = new boolean[1];

		Subgraph<Table2> subgraph = new Subgraph<Table2>(Table2.class, context
				.getEntityResolver());
		subgraph.addRefPath(Table2.TABLE1_PROPERTY,
				new DeserializationCallback() {
					public void postDeserialize(SubgraphNode node, Object object) {
						assertNotNull(node);
						assertEquals(Table2.TABLE1_PROPERTY, node
								.getIncomingProperty().getName());
						assertNotNull(object);
						assertTrue(object instanceof Table1);

						callbackInvoked[0] = true;
					}
				});

		XStreamDeserializer deserializer = new XStreamDeserializer();

		int id = Cayenne.intPKForObject(t11);
		String xml = "<Table2><name>t21</name><table1><Table1 ref=\"true\"><PK>"
				+ id + "</PK></Table1></table1></Table2>";

		InputStream in = new ByteArrayInputStream(xml.getBytes());
		try {
			deserializer.deserialize(context, subgraph, in);
		} finally {
			in.close();
		}

		assertTrue(callbackInvoked[0]);
	}

	public void testCallbackByValue() throws IOException {

		ObjectContext context = newContext();

		final boolean[] callbackInvoked = new boolean[1];

		Subgraph<Table1> subgraph = new Subgraph<Table1>(Table1.class, context
				.getEntityResolver());
		subgraph.addClonePath(Table1.TABLE2S_PROPERTY,
				new DeserializationCallback() {
					public void postDeserialize(SubgraphNode node, Object object) {
						assertNotNull(node);
						assertEquals(Table1.TABLE2S_PROPERTY, node
								.getIncomingProperty().getName());
						assertNotNull(object);
						assertTrue(object instanceof Table2);

						Table2 t2 = (Table2) object;
						assertTrue(t2.getName() != null);

						callbackInvoked[0] = true;

					}
				});

		XStreamDeserializer deserializer = new XStreamDeserializer();

		String xml = "<Table1><name>t11</name><table2s>"
				+ "<Table2><name>t21</name></Table2>"
				+ "<Table2><name>t22</name></Table2></table2s></Table1>";

		InputStream in = new ByteArrayInputStream(xml.getBytes());
		try {
			deserializer.deserialize(context, subgraph, in);
		} finally {
			in.close();
		}

		assertTrue(callbackInvoked[0]);
	}

}
