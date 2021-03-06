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

import java.io.Reader;

import org.apache.cayenne.ObjectContext;

/**
 * A common superclass for deserializers defining some implementation
 * independent parameters.
 */
public abstract class BaseDeserializer implements SubgraphDeserializer {

	protected int commitCountThreshold = 1000;
	protected boolean committing = true;

	public abstract <T> T deserialize(ObjectContext context, Subgraph<T> subgraph, Reader in);

	/**
	 * Returns the max size of the commit batch. This is needed to constrain
	 * memory use. The default is 1000 objects. Note that setting "committing"
	 * flag to false would cause deserializer to ignore this setting.
	 */
	public int getCommitCountThreshold() {
		return commitCountThreshold;
	}

	public void setCommitCountThreshold(int commitCountThreshold) {
		this.commitCountThreshold = commitCountThreshold;
	}

	/**
	 * Returns true if deserializer will commit deserialized objects. True is
	 * the default.
	 */
	public boolean isCommitting() {
		return committing;
	}

	public void setCommitting(boolean committing) {
		this.committing = committing;
	}
}
