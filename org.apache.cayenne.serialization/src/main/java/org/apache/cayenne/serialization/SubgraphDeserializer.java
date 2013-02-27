package org.apache.cayenne.serialization;

import java.io.Reader;

import org.apache.cayenne.ObjectContext;

public interface SubgraphDeserializer {

	<T> T deserialize(ObjectContext context, Subgraph<T> subgraph, Reader in);
}
