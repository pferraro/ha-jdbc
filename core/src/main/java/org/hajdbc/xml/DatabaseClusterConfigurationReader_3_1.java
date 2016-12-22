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
package net.sf.hajdbc.xml;

import java.util.Locale;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.sf.hajdbc.Database;
import net.sf.hajdbc.DatabaseBuilder;
import net.sf.hajdbc.Locality;
import net.sf.hajdbc.messages.Messages;
import net.sf.hajdbc.messages.MessagesFactory;

/**
 * @author Paul Ferraro
 */
public class DatabaseClusterConfigurationReader_3_1<Z, D extends Database<Z>, B extends DatabaseBuilder<Z, D>> extends DatabaseClusterConfigurationReader_3_0<Z, D, B>
{
	private static Messages messages = MessagesFactory.getMessages();
	
	public static final DatabaseClusterConfigurationReaderFactory FACTORY = new DatabaseClusterConfigurationReaderFactory()
	{
		@Override
		public <Z, D extends Database<Z>, B extends DatabaseBuilder<Z, D>> DatabaseClusterConfigurationReader<Z, D, B> createReader()
		{
			return new DatabaseClusterConfigurationReader_3_1<>();
		}
	};

	@Override
	void readDatabaseAttributes(XMLStreamReader reader, B builder) throws XMLStreamException
	{
		for (int i = 0; i < reader.getAttributeCount(); ++i)
		{
			String value = reader.getAttributeValue(i);
			switch (reader.getAttributeLocalName(i))
			{
				case ID:
				{
					break;
				}
				case LOCATION:
				{
					builder.location(value);
					break;
				}
				case WEIGHT:
				{
					builder.weight(Integer.parseInt(value));
					break;
				}
				case LOCALITY:
				{
					builder.locality(Locality.valueOf(value.toUpperCase(Locale.US)));
					break;
				}
				default:
				{
					throw new XMLStreamException(messages.unexpectedAttribute(reader, i));
				}
			}
		}
	}
}
