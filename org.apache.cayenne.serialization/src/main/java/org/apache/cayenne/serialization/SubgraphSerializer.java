package org.apache.cayenne.serialization;

import java.io.Writer;

public interface SubgraphSerializer {

	<T> void serialize(T object, Subgraph<T> subgraph, Writer out);
}
