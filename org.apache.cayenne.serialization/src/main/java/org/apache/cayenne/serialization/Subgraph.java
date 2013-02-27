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

import java.util.StringTokenizer;

import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.reflect.ClassDescriptor;

/**
 * Represents a subgraph of the Cayenne-mapped persistent object graph. Used as
 * a model for serialization.
 */
public class Subgraph<T> implements SubgraphBuilder {

	private SubgraphNode rootNode;

	public Subgraph(Class<T> rootPersistentClass, EntityResolver entityResolver) {

		if (rootPersistentClass == null) {
			throw new NullPointerException("Null rootPersistentClass");
		}

		ObjEntity entity = entityResolver.lookupObjEntity(rootPersistentClass);
		if (entity == null) {
			throw new IllegalArgumentException(
					"Invalid/unmapped persistent class: "
							+ rootPersistentClass.getSimpleName());
		}

		ClassDescriptor descriptor = entityResolver.getClassDescriptor(entity
				.getName());

		this.rootNode = new SubgraphNode(descriptor);
	}

	Subgraph(SubgraphNode rootNode) {
		this.rootNode = rootNode;
	}

	public SubgraphNode getRootNode() {
		return rootNode;
	}

	/**
	 * Adds the name of the ObjAttribute to exclude from serialization. This is
	 * useful for excluding mapped ids or attributes auto-generated with
	 * listeners. The path must end in the ObjAttribute.
	 */
	public SubgraphBuilder excludeAttribute(String path) {
		StringTokenizer tokens = new StringTokenizer(path, ".");

		SubgraphNode node = rootNode;
		while (tokens.hasMoreTokens()) {

			String token = tokens.nextToken();

			if (tokens.hasMoreTokens()) {
				node = node.getChild(token, false);
			} else {
				// last token is the attribute name
				node.excludeAttribute(token);
			}
		}
		
		return new Subgraph<Object>(node);
	}

	/**
	 * Adds a subgraph path based on a relationship. On deserialization a new
	 * object will be created in the database.
	 */
	public SubgraphBuilder addClonePath(String path,
			SubgraphCallback... callbacks) {
		StringTokenizer tokens = new StringTokenizer(path, ".");

		SubgraphNode node = rootNode;
		while (tokens.hasMoreTokens()) {
			node = node.getChild(tokens.nextToken(), true);
		}

		addCallbacks(node, callbacks);

		return new Subgraph<Object>(node);
	}

	/**
	 * Adds a subgraph path to a related entity that should be serialized as a
	 * reference to its ObjectId. Such objects will be matched against the
	 * existing DB rows, and no new objects will be created for them.
	 */
	public SubgraphBuilder addRefPath(String path,
			SubgraphCallback... callbacks) {
		StringTokenizer tokens = new StringTokenizer(path, ".");

		SubgraphNode node = rootNode;
		while (tokens.hasMoreTokens()) {
			String token = tokens.nextToken();
			node = node.getChild(token, true);
			node.setSerializedByReference(!tokens.hasMoreTokens());
		}

		addCallbacks(node, callbacks);
		return new Subgraph<Object>(node);
	}
	
	public SubgraphBuilder addCallbacks(SubgraphCallback... callbacks) {
		addCallbacks(rootNode, callbacks);
		return this;
	}

	private void addCallbacks(SubgraphNode node, SubgraphCallback... callbacks) {
		if (callbacks != null && callbacks.length > 0) {
			for (SubgraphCallback callback : callbacks) {
				if (callback instanceof DeserializationCallback) {
					node
							.addDeserializationCallback((DeserializationCallback) callback);
				} else if (callback instanceof SerializationCallback) {
					node
							.addSerializationCallback((SerializationCallback) callback);
				} else {
					throw new IllegalArgumentException(
							"Unsupported callback type: " + callback);
				}
			}
		}
	}
}
