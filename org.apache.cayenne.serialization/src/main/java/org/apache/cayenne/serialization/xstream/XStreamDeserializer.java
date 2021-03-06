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

import java.io.Reader;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.access.Transaction;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.serialization.BaseDeserializer;
import org.apache.cayenne.serialization.Subgraph;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.core.util.CompositeClassLoader;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class XStreamDeserializer extends BaseDeserializer {

	static final Log logger = LogFactory.getLog(XStreamDeserializer.class);

	@SuppressWarnings("all")
	@Override
	public <T> T deserialize(ObjectContext context, Subgraph<T> subgraph, Reader in) {

		long t0 = System.currentTimeMillis();

		XStream xstream = createXStream(subgraph.getRootNode().getClassDescriptor());

		int commitCountThreshold = isCommitting() ? getCommitCountThreshold() : 0;

		xstream.registerConverter(new PersistentDeserializeConverter(subgraph.getRootNode(), context, commitCountThreshold));
		xstream.registerConverter(new ObjectIdConverter(context.getEntityResolver()));

		T object;

		if (isCommitting()) {
			object = (T) deserializeInTransaction(context, xstream, in);
		} else {
			object = (T) deserialize(xstream, in);
		}

		long t1 = System.currentTimeMillis();
		logger.info("Deserialized in " + (t1 - t0) + " ms.");

		return object;
	}

	protected Object deserializeInTransaction(ObjectContext context, XStream xstream, Reader in) {

		// since multiple intermediate context commits are possible, wrap them
		// in a manual transaction to allow for atomic rollback

		Transaction tx = ((DataContext) context).getParentDataDomain().createTransaction();

		Transaction.bindThreadTransaction(tx);

		try {
			Object result = deserialize(xstream, in);
			context.commitChanges();
			tx.commit();
			return result;

		} catch (Exception ex) {
			tx.setRollbackOnly();
			throw new CayenneRuntimeException("Error deserializing", ex);
		} finally {
			Transaction.bindThreadTransaction(null);

			if (tx.getStatus() == Transaction.STATUS_MARKED_ROLLEDBACK) {
				try {
					tx.rollback();
				} catch (Exception rollbackEx) {
				}
			}
		}
	}

	protected Object deserialize(XStream xstream, Reader in) {
		return xstream.fromXML(in);
	}

	protected XStream createXStream(ClassDescriptor rootDescriptor) {
		XStream xstream = new XStream(new DomDriver());

		// this is required to get a real streaming API an release all
		// serialized objects...
		xstream.setMode(XStream.NO_REFERENCES);

		((CompositeClassLoader) xstream.getClassLoader()).add(rootDescriptor.getObjectClass().getClassLoader());
		// TODO: we need to implement our own algorithm for object cycle
		// detection... XStream also supports Immutable classes. we may classify
		// objects according to their participation in a cycle.

		xstream.alias(rootDescriptor.getEntity().getName(), rootDescriptor.getObjectClass());
		return xstream;
	}
}
