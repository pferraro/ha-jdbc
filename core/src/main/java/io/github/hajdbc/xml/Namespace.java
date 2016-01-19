/*
 * HA-JDBC: High-Availability JDBC
 * Copyright (C) 2014  Paul Ferraro
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.github.hajdbc.xml;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.xml.sax.SAXException;

import io.github.hajdbc.messages.Messages;
import io.github.hajdbc.messages.MessagesFactory;

public enum Namespace
{
	VERSION_3_0(3, 0),
	VERSION_4_0(4, 0),
	;

	public static final Namespace CURRENT_VERSION = VERSION_4_0;

	private static final Messages messages = MessagesFactory.getMessages();
	private static final Map<String, Namespace> namespaces = new HashMap<>();
	static
	{
		for (Namespace namespace: Namespace.values())
		{
			namespaces.put(namespace.getURI(), namespace);
		}
	}

	public static Namespace forReader(XMLStreamReader reader) throws XMLStreamException
	{
		Namespace namespace = namespaces.get(reader.getNamespaceURI());
		if (namespace == null)
		{
			throw new XMLStreamException(messages.unsupportedNamespace(reader));
		}
		return namespace;
	}

	private final int major;
	private final int minor;
	private final Schema schema;

	private Namespace(int major, int minor)
	{
		this.major = major;
		this.minor = minor;
		
		String resource = String.format("ha-jdbc-%d.%d.xsd", major, minor);
		URL url = this.getClass().getClassLoader().getResource(resource);

		if (url == null)
		{
			throw new IllegalArgumentException(resource);
		}

		try {
			this.schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(url);
		} catch (SAXException e) {
			throw new IllegalStateException(e);
		}
	}

	public Schema getSchema()
	{
		return this.schema;
	}

	public String getURI()
	{
		return String.format("urn:ha-jdbc:cluster:%d.%d", this.major, this.minor);
	}

	public boolean since(Namespace namespace)
	{
		return (this.major == namespace.major) ? (this.minor > namespace.minor) : (this.major > namespace.major);
	}
}
