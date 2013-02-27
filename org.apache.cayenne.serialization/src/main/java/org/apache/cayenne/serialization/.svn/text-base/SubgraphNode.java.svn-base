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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.reflect.ArcProperty;
import org.apache.cayenne.reflect.AttributeProperty;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.Property;
import org.apache.cayenne.reflect.PropertyVisitor;
import org.apache.cayenne.reflect.ToManyProperty;
import org.apache.cayenne.reflect.ToOneProperty;

/**
 * A node in the subgraph descriptor.
 */
public class SubgraphNode {

	private ClassDescriptor classDescriptor;
	private ArcProperty incomingProperty;
	private Map<String, SubgraphNode> children;
	private boolean serializedByReference;
	private List<AttributeProperty> attributeProperties;
	private List<SerializationCallback> serializationCallbacks;
	private List<DeserializationCallback> deserializationCallbacks;

	/**
	 * Creates a root subgraph node.
	 */
	SubgraphNode(ClassDescriptor classDescriptor) {

		this.serializationCallbacks = new ArrayList<SerializationCallback>(3);
		this.deserializationCallbacks = new ArrayList<DeserializationCallback>(
				3);
		this.classDescriptor = classDescriptor;

		// cache properties
		this.attributeProperties = new ArrayList<AttributeProperty>();

		classDescriptor.visitProperties(new PropertyVisitor() {
			public boolean visitAttribute(AttributeProperty property) {
				attributeProperties.add(property);
				return true;
			}

			public boolean visitToMany(ToManyProperty property) {
				return true;
			}

			public boolean visitToOne(ToOneProperty property) {
				return true;
			}
		});
	}

	SubgraphNode(ArcProperty incomingProperty) {
		this(incomingProperty.getTargetDescriptor());
		this.incomingProperty = incomingProperty;
	}

	public int getMaxDepth() {
		if (children != null) {
			int depth = 0;
			for (SubgraphNode child : children.values()) {
				int childDepth = child.getMaxDepth();

				if (depth < childDepth) {
					depth = childDepth;
				}
			}

			return depth + 1;
		} else {
			return 1;
		}
	}

	/**
	 * Returns true if only a reference should be serialized for this node,
	 * instead of serializing object data.
	 */
	public boolean isSerializedByReference() {
		return serializedByReference;
	}

	void setSerializedByReference(boolean reference) {
		this.serializedByReference = reference;
	}

	void excludeAttribute(String attributeName) {
		Iterator<AttributeProperty> it = attributeProperties.iterator();
		while (it.hasNext()) {
			AttributeProperty property = it.next();
			if (property.getName().equals(attributeName)) {
				it.remove();
				return;
			}
		}

		throw new IllegalArgumentException(
				"Attribute is either unmapped or was exlcuded before: "
						+ attributeName);
	}

	public SubgraphNode getChild(String name) {
		return children != null ? children.get(name) : null;
	}

	SubgraphNode getChild(String name, boolean create) {

		SubgraphNode child = null;
		if (children != null) {
			child = children.get(name);
		} else {
			children = new LinkedHashMap<String, SubgraphNode>();
		}

		if (child == null) {

			if (!create) {
				throw new IllegalArgumentException("Invalid child path: "
						+ name);
			}

			Property relationship = classDescriptor.getProperty(name);

			if (relationship == null) {
				throw new IllegalArgumentException("Path '" + name
						+ "' does not denote a mapped class property");
			}

			if (!(relationship instanceof ArcProperty)) {
				throw new IllegalArgumentException("Path '" + name
						+ "' does not denote a mapped relationship property. "
						+ "Is this an attribute?");
			}

			ArcProperty arc = (ArcProperty) relationship;

			child = new SubgraphNode(arc);
			children.put(name, child);
		}

		return child;
	}

	void addSerializationCallback(SerializationCallback callback) {
		serializationCallbacks.add(callback);
	}

	void addDeserializationCallback(DeserializationCallback callback) {
		deserializationCallbacks.add(callback);
	}

	public ClassDescriptor getClassDescriptor() {
		return classDescriptor;
	}

	public List<AttributeProperty> getAttributeProperties() {
		return attributeProperties;
	}

	/**
	 * Returns a property that connects parent node to this node. It will be
	 * null for the root node.
	 */
	public ArcProperty getIncomingProperty() {
		return incomingProperty;
	}

	public Collection<SubgraphNode> getChildren() {
		return children != null ? children.values() : Collections
				.<SubgraphNode> emptyList();
	}

	public List<SerializationCallback> getSerializationCallbacks() {
		return serializationCallbacks;
	}

	public List<DeserializationCallback> getDeserializationCallbacks() {
		return deserializationCallbacks;
	}
}