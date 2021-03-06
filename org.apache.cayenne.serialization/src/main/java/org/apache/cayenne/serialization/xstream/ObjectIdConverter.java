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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.reflect.AttributeProperty;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.util.Util;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

class ObjectIdConverter implements Converter {

	private EntityResolver entityResolver;
	private Map<String, Map<String, Class<?>>> idTypesMap;

	ObjectIdConverter(EntityResolver entityResolver) {
		this.entityResolver = entityResolver;
		this.idTypesMap = new ConcurrentHashMap<String, Map<String, Class<?>>>();
	}

	public void marshal(Object source, HierarchicalStreamWriter writer,
			MarshallingContext context) {

		ObjectId id = (ObjectId) source;
		writer.startNode(id.getEntityName());
		writer.addAttribute(Attributes.ref.name(), "true");

		for (Map.Entry<String, Object> entry : id.getIdSnapshot().entrySet()) {

			writer.startNode(entry.getKey());
			context.convertAnother(entry.getValue());
			writer.endNode();
		}
		writer.endNode();
	}

	public Object unmarshal(HierarchicalStreamReader reader,
			UnmarshallingContext context) {

		String entityName = reader.getNodeName();

		Map<String, Class<?>> idTypeMap = getIdTypesMap(entityName);

		Map<String, Object> id = new HashMap<String, Object>();
		while (reader.hasMoreChildren()) {
			reader.moveDown();

			String key = reader.getNodeName();

			Object value = context.convertAnother(id, idTypeMap.get(key));
			id.put(key, value);

			reader.moveUp();
		}

		return new ObjectId(entityName, id);
	}

	public boolean canConvert(Class type) {
		return ObjectId.class.isAssignableFrom(type);
	}

	private Map<String, Class<?>> getIdTypesMap(String entityName) {

		Map<String, Class<?>> typesMap = idTypesMap.get(entityName);

		if (typesMap == null) {
			typesMap = new HashMap<String, Class<?>>();
			ClassDescriptor descriptor = entityResolver
					.getClassDescriptor(entityName);

			Collection<AttributeProperty> mappedIdProperties = descriptor.getIdProperties();
			for (AttributeProperty property : mappedIdProperties) {

				ObjAttribute attribute = property.getAttribute();
				typesMap.put(attribute.getDbAttributeName(), attribute.getJavaClass());
			}

			DbEntity dbEntity = descriptor.getEntity().getDbEntity();
			if (dbEntity != null) {
				for (DbAttribute attribute : dbEntity.getPrimaryKeys()) {
					if (!typesMap.containsKey(attribute.getName())) {
						String type = TypesMapping.getJavaBySqlType(attribute.getType());
						try {
							typesMap.put(attribute.getName(), Util
									.getJavaClass(type));
						} catch (ClassNotFoundException e) {
							throw new IllegalStateException("Invalid type: "
									+ type, e);
						}
					}
				}
			}

			idTypesMap.put(entityName, typesMap);
		}

		return typesMap;
	}

}
