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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.serialization.SerializationCallback;
import org.apache.cayenne.serialization.Subgraph;
import org.apache.cayenne.serialization.SubgraphNode;
import org.apache.cayenne.serialization.persistent.Table1;
import org.apache.cayenne.serialization.persistent.Table2;
import org.apache.cayenne.serialization.unit.SerializationCase;
import org.apache.cayenne.util.Util;

public class XStreamSerializerTest extends SerializationCase {

	public void testCreatingCompactXML() {
		XStreamSerializer serializer = new XStreamSerializer();
		assertFalse(serializer.isCreatingCompactXML());
		serializer.setCreatingCompactXML(true);
		assertTrue(serializer.isCreatingCompactXML());
	}

	public void testSerializeByValueToOne() throws IOException {

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

		File file = tempFile(".xml");
		XStreamSerializer serializer = new XStreamSerializer();
		serializer.setCreatingCompactXML(true);

		FileOutputStream out = new FileOutputStream(file);
		try {
			serializer.serialize(t21, subgraph, out);
		} finally {
			out.close();
		}

		assertTrue(file.isFile());
		assertTrue(file.length() > 0);

		assertEquals("<Table2><name>t21</name><table1>"
				+ "<Table1><name>t11</name></Table1></table1></Table2>", Util
				.stringFromFile(file).trim());
	}

	public void testSerializeByValueToMany() throws IOException {

		ObjectContext context = newContext();
		Table1 t11 = context.newObject(Table1.class);
		t11.setName("t11");

		Table2 t21 = context.newObject(Table2.class);
		t21.setName("t21");
		t21.setTable1(t11);

		context.commitChanges();

		Subgraph<Table1> subgraph = new Subgraph<Table1>(Table1.class, context
				.getEntityResolver());
		subgraph.addClonePath(Table1.TABLE2S_PROPERTY);

		File file = tempFile(".xml");
		XStreamSerializer serializer = new XStreamSerializer();
		serializer.setCreatingCompactXML(true);

		FileOutputStream out = new FileOutputStream(file);
		try {
			serializer.serialize(t11, subgraph, out);
		} finally {
			out.close();
		}

		assertTrue(file.isFile());
		assertTrue(file.length() > 0);

		assertEquals("<Table1><name>t11</name><table2s>"
				+ "<Table2><name>t21</name></Table2>" + "</table2s></Table1>",
				Util.stringFromFile(file).trim());
	}

	public void testSerializeByReferenceToOne() throws IOException {

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

		File file = tempFile(".xml");
		XStreamSerializer serializer = new XStreamSerializer();
		serializer.setCreatingCompactXML(true);

		FileOutputStream out = new FileOutputStream(file);
		try {
			serializer.serialize(t21, subgraph, out);
		} finally {
			out.close();
		}

		assertTrue(file.isFile());
		assertTrue(file.length() > 0);

		int id = Cayenne.intPKForObject(t11);

		assertEquals(
				"<Table2><name>t21</name><table1><Table1 ref=\"true\"><PK>"
						+ id + "</PK></Table1></table1></Table2>", Util
						.stringFromFile(file).trim());
	}

	public void testExcludeAttribute() throws IOException {

		XStreamSerializer serializer = new XStreamSerializer();
		serializer.setCreatingCompactXML(true);

		ObjectContext context = newContext();
		Table1 t11 = context.newObject(Table1.class);
		t11.setName("t11");

		context.commitChanges();

		Subgraph<Table1> subgraph = new Subgraph<Table1>(Table1.class, context
				.getEntityResolver());

		File f1 = tempFile(".xml");

		FileOutputStream out = new FileOutputStream(f1);
		try {
			serializer.serialize(t11, subgraph, out);
		} finally {
			out.close();
		}

		assertTrue(f1.isFile());
		assertTrue(f1.length() > 0);

		assertEquals("<Table1><name>t11</name></Table1>", Util.stringFromFile(
				f1).trim());

		subgraph.excludeAttribute(Table1.NAME_PROPERTY);

		File f2 = tempFile(".xml");

		FileOutputStream out2 = new FileOutputStream(f2);
		try {
			serializer.serialize(t11, subgraph, out2);
		} finally {
			out.close();
		}

		assertTrue(f2.isFile());
		assertTrue(f2.length() > 0);

		assertEquals("<Table1/>", Util.stringFromFile(f2).trim());

	}

	public void testCallbackByValueToMany() throws IOException {

		ObjectContext context = newContext();
		Table1 t11 = context.newObject(Table1.class);
		t11.setName("t11");

		Table2 t21 = context.newObject(Table2.class);
		t21.setName("t21");
		t21.setTable1(t11);

		context.commitChanges();

		final boolean[] callbackInvoked = new boolean[1];

		Subgraph<Table1> subgraph = new Subgraph<Table1>(Table1.class, context
				.getEntityResolver());
		subgraph.addClonePath(Table1.TABLE2S_PROPERTY,
				new SerializationCallback() {
					public Query relationshipQuery(SubgraphNode node,
							Object sourceObject) {

						callbackInvoked[0] = true;
						return null;
					}
				});

		XStreamSerializer serializer = new XStreamSerializer();
		serializer.setCreatingCompactXML(true);

		OutputStream out = new ByteArrayOutputStream();
		try {
			serializer.serialize(t11, subgraph, out);
		} finally {
			out.close();
		}

		assertTrue(callbackInvoked[0]);
	}

}
