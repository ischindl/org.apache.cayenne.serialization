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

import java.util.Map;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataObject;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.ResultIterator;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.RelationshipQuery;
import org.apache.cayenne.reflect.ArcProperty;
import org.apache.cayenne.reflect.AttributeProperty;
import org.apache.cayenne.reflect.PropertyDescriptor;
import org.apache.cayenne.serialization.SerializationCallback;
import org.apache.cayenne.serialization.Subgraph;
import org.apache.cayenne.serialization.SubgraphNode;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;

class PersistentSerializeConverter implements Converter {

	private static final String STACK_KEY = PersistentSerializeConverter.class.getName() + "_STACK";

	private Subgraph<?> subgraph;
	private int statementFetchSize;

	public PersistentSerializeConverter(Subgraph<?> subgraph, int statementFetchSize) {
		this.subgraph = subgraph;
		this.statementFetchSize = statementFetchSize;
	}

	public void marshal(Object object, HierarchicalStreamWriter writer, MarshallingContext context) {

		if (subgraph.getRootNode().isSerializedByReference()) {
			Persistent persistent = (Persistent) object;
			((PrettyPrintWriter)writer).addAttribute(Attributes.ref.name(), "true");
			for (Map.Entry<String, Object> entry : persistent.getObjectId().getIdSnapshot().entrySet()) {

				writer.startNode(entry.getKey());
				context.convertAnother(entry.getValue());
				writer.endNode();
			}
		} else {
			SerializerStack serializerContext = getStack(context);

			SubgraphNode node = serializerContext.peekNode();

			// don't generate tags for the root node, as they are generated via the
			// 'alias' mechanism
			if (node.getIncomingProperty() != null) {
				Persistent persistent = (Persistent) object;
				writer.startNode(persistent.getObjectId().getEntityName());
			}

			for (AttributeProperty property : node.getAttributeProperties()) {
				marshalAttribute(object, property, writer, context);
			}

			// marshal specified related entities
			for (SubgraphNode child : node.getChildren()) {

				serializerContext.pushNode(child);

				Query query = null;
				for (SerializationCallback callback : child.getSerializationCallbacks()) {
					query = callback.relationshipQuery(child, object);
					if (query != null) {
						break;
					}
				}

				ArcProperty incoming = child.getIncomingProperty();
				boolean byReference = child.isSerializedByReference();

				if (incoming.getRelationship().isToMany()) {
					marshalToMany(object, incoming, writer, context, byReference, query);
				} else {
					marshalToOne(object, incoming, writer, context, byReference, query);
				}

				serializerContext.popNode();
			}

			if (node.getIncomingProperty() != null) {
				writer.endNode();
			}
		}
	}

	private void marshalAttribute(Object object, PropertyDescriptor property, HierarchicalStreamWriter writer, MarshallingContext context) {

		Object value = property.readProperty(object);

		if (value != null) {
			writer.startNode(property.getName());
			context.convertAnother(value);
			writer.endNode();
		}
	}

	private void marshalToOne(Object object, ArcProperty arc, HierarchicalStreamWriter writer, MarshallingContext context, boolean byReference, Query query) {

		Persistent value = null;

		if (query != null) {
			ObjectContext objectContext = ((Persistent) object).getObjectContext();
			value = (Persistent) Cayenne.objectForQuery(objectContext, query);
		} else {
			value = (Persistent) arc.readProperty(object);
		}

		// note that we don't even write an empty tag for NULL to-one. This may
		// be a problem only if we allow non-reference nodes to be attached to
		// reference-serialized nodes
		if (value != null) {
			writer.startNode(arc.getName());
			context.convertAnother(byReference ? value.getObjectId() : value);
			writer.endNode();
		}
	}

	private void marshalToMany(Object object, ArcProperty arc, HierarchicalStreamWriter writer, MarshallingContext context, boolean byReference, Query query) {

		writer.startNode(arc.getName());

		Persistent persistent = (Persistent) object;

		if (query == null) {
			RelationshipQuery relationshipQuery = new RelationshipQuery(persistent.getObjectId(), arc.getName());

			// "fetchSize" is absolutely critical to avoid storing the entire
			// ResultSet in memory.
			relationshipQuery.setStatementFetchSize(statementFetchSize);
			query = relationshipQuery;
		}

		DataContext dataContext = (DataContext) persistent.getObjectContext();

		try {

			// use ResultIterator for to-many to avoid huge fetches
			// in memory
			ResultIterator it = dataContext.performIteratedQuery(query);

			try {
				while (it.hasNextRow()) {
					DataRow next = (DataRow) it.nextRow();

					DataObject target = dataContext.objectFromDataRow(next.getEntityName(), next);

					context.convertAnother(byReference ? target.getObjectId() : target);
				}
			} finally {
				it.close();
			}
		} catch (Exception e) {
			throw new CayenneRuntimeException("Error reading relationship " + arc.getName(), e);
		}

		writer.endNode();
	}

	private SerializerStack getStack(MarshallingContext context) {
		SerializerStack stack = (SerializerStack) context.get(STACK_KEY);

		if (stack == null) {
			stack = new SerializerStack(subgraph.getRootNode());
			context.put(STACK_KEY, stack);
		}

		return stack;
	}

	public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
		throw new UnsupportedOperationException();
	}

	@SuppressWarnings("all")
	public boolean canConvert(Class objectClass) {
		return Persistent.class.isAssignableFrom(objectClass);
	}

}
