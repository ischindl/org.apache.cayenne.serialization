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

import java.io.OutputStream;

import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.serialization.BaseSerializer;
import org.apache.cayenne.serialization.Subgraph;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class XStreamSerializer extends BaseSerializer {

	protected boolean creatingCompactXML;

	@Override
	public <T> void serialize(T object, Subgraph<T> subgraph, OutputStream out) {

		// TODO: make sure all the converters are stateless... then we can cache
		// xstream instances by subgraph and reuse them
		XStream xstream = createXStream(subgraph.getRootNode().getClassDescriptor());
		xstream.registerConverter(new PersistentSerializeConverter(subgraph, statementFetchSize));
		xstream.registerConverter(new ObjectIdConverter(null));

		xstream.toXML(object, out);
	}

	protected XStream createXStream(ClassDescriptor rootDescriptor) {
		XStream xstream = new XStream(new DomDriver());

		// this is required to get a real streaming API an release all
		// serialized objects...
		xstream.setMode(XStream.NO_REFERENCES);

		// TODO: we need to implement our own algorithm for object cycle
		// detection... XStream also supports Immutable classes. we may classify
		// objects according to their participation in a cycle.

		xstream.alias(rootDescriptor.getEntity().getName(), rootDescriptor.getObjectClass());
		return xstream;
	}

	public boolean isCreatingCompactXML() {
		return creatingCompactXML;
	}

	/**
	 * Sets whether serializer should skip spaces and line breaks used for pretty formatting, and output unformatted XML.
	 */
	public void setCreatingCompactXML(boolean createCompactXML) {
		this.creatingCompactXML = createCompactXML;
	}
}
