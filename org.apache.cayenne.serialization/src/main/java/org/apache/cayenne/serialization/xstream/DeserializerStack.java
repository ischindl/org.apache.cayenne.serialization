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

import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.reflect.ArcProperty;
import org.apache.cayenne.reflect.ToManyProperty;
import org.apache.cayenne.serialization.DeserializationCallback;
import org.apache.cayenne.serialization.SubgraphNode;

import com.thoughtworks.xstream.core.util.FastStack;

class DeserializerStack {

	private FastStack subgraphStack;
	private FastStack objectStack;
	private int counter;

	DeserializerStack(SubgraphNode root) {
		int maxDepth = root.getMaxDepth();
		objectStack = new FastStack(maxDepth);
		subgraphStack = new FastStack(maxDepth);
		subgraphStack.push(root);
	}

	void pushObject(Object object) {

		SubgraphNode node = (SubgraphNode) subgraphStack.peek();

		// connect to parent
		ArcProperty incoming = node.getIncomingProperty();
		if (incoming != null) {
			Object peek = objectStack.peek();

			if (incoming instanceof ToManyProperty) {
				((ToManyProperty) incoming).addTarget(peek, object, true);
			} else {
				incoming.writeProperty(peek, null, object);
			}
		}

		objectStack.push(object);
	}

	int popObject() {
		Persistent object = (Persistent) objectStack.pop();
		if (object != null) {
			counter++;
		}

		if (object != null) {

			SubgraphNode node = (SubgraphNode) subgraphStack.peek();

			// apply callbacks
			for (DeserializationCallback callback : node
					.getDeserializationCallbacks()) {
				callback.postDeserialize(node, object);
			}

			if (object.getPersistenceState() == PersistenceState.NEW) {
				counter++;
			}
		}

		return counter;
	}

	boolean pushPath(String name) {

		SubgraphNode child = ((SubgraphNode) subgraphStack.peek())
				.getChild(name);
		if (child == null) {
			return false;
		}

		subgraphStack.push(child);
		return true;
	}

	void popPath() {
		subgraphStack.popSilently();
	}
}
