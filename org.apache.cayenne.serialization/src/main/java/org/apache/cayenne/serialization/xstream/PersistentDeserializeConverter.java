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

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.reflect.ArcProperty;
import org.apache.cayenne.reflect.AttributeProperty;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.Property;
import org.apache.cayenne.reflect.ToManyProperty;
import org.apache.cayenne.serialization.SubgraphNode;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

class PersistentDeserializeConverter implements Converter {

	private static final String STACK_KEY = PersistentDeserializeConverter.class
			.getName()
			+ "_STACK";

	private ObjectContext objectContext;
	private int commitCountThreshold;
	private SubgraphNode rootNode;

	public PersistentDeserializeConverter(SubgraphNode rootNode,
			ObjectContext objectContext, int commitCountThreshold) {

		this.rootNode = rootNode;
		this.objectContext = objectContext;
		this.commitCountThreshold = commitCountThreshold;
	}

	@SuppressWarnings("all")
	public boolean canConvert(Class objectClass) {
		return Persistent.class.isAssignableFrom(objectClass);
	}

	public void marshal(Object source, HierarchicalStreamWriter writer,
			MarshallingContext context) {
		throw new UnsupportedOperationException();
	}

	public Object unmarshal(HierarchicalStreamReader reader,
			UnmarshallingContext context) {

		String ref = reader.getAttribute(Attributes.ref.name());
		if ("true".equals(ref)) {
			return deserializeExisting(reader, context);
		} else {
			return deserializeNew(reader, context);
		}
	}

	private Object deserializeExisting(HierarchicalStreamReader reader,
			UnmarshallingContext context) {

		ObjectId id = (ObjectId) context.convertAnother(null, ObjectId.class);

		Object object = Cayenne.objectForPK(objectContext, id);
		// TODO: handle deleted objects that no longer exist...

		if (object != null) {
			DeserializerStack stack = getStack(context);
			stack.pushObject(object);
			stack.popObject();
		}

		return object;
	}

	private Object deserializeNew(HierarchicalStreamReader reader,
			UnmarshallingContext context) {

		String entityName = reader.getNodeName();
		ClassDescriptor descriptor = objectContext.getEntityResolver()
				.getClassDescriptor(entityName);

		DeserializerStack stack = getStack(context);

		Object object = descriptor.createObject();
		objectContext.registerNewObject(object);

		stack.pushObject(object);

		while (reader.hasMoreChildren()) {
			reader.moveDown();

			Property property = descriptor.getProperty(reader.getNodeName());
			if (property instanceof AttributeProperty) {
				deserializeAttribute(reader, context, object,
						(AttributeProperty) property);
			} else {

				if (stack.pushPath(property.getName())) {

					ArcProperty arc = (ArcProperty) property;
					if (arc.getRelationship().isToMany()) {
						deserializeToManyRelationship(reader, context, object,
								(ToManyProperty) arc);
					} else {
						deserializeToOneRelationship(reader, context, object,
								arc);
					}

					stack.popPath();
				}
			}

			reader.moveUp();
		}

		int count = stack.popObject();

		if (commitCountThreshold > 0 && count % commitCountThreshold == 0) {
			objectContext.commitChanges();
		}

		return object;
	}

	private void deserializeAttribute(HierarchicalStreamReader reader,
			UnmarshallingContext context, Object parentObject,
			AttributeProperty property) {

		Class<?> javaType = property.getAttribute().getJavaClass();
		Object value = context.convertAnother(parentObject, javaType);
		property.writeProperty(parentObject, null, value);
	}

	private void deserializeToOneRelationship(HierarchicalStreamReader reader,
			UnmarshallingContext context, Object parentObject,
			ArcProperty property) {

		Class<?> javaType = property.getTargetDescriptor().getObjectClass();

		// check for children to handle optional to-one
		if (reader.hasMoreChildren()) {
			reader.moveDown();
			context.convertAnother(parentObject, javaType);
			reader.moveUp();
		}
	}

	private void deserializeToManyRelationship(HierarchicalStreamReader reader,
			UnmarshallingContext context, Object parentObject,
			ToManyProperty property) {

		Class<?> javaType = property.getTargetDescriptor().getObjectClass();

		while (reader.hasMoreChildren()) {
			reader.moveDown();
			context.convertAnother(parentObject, javaType);
			reader.moveUp();
		}
	}

	private DeserializerStack getStack(UnmarshallingContext context) {
		DeserializerStack stack = (DeserializerStack) context.get(STACK_KEY);

		if (stack == null) {
			stack = new DeserializerStack(rootNode);
			context.put(STACK_KEY, stack);
		}

		return stack;
	}
}
