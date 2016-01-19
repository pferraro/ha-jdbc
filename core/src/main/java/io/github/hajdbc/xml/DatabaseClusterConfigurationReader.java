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

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import io.github.hajdbc.Database;
import io.github.hajdbc.DatabaseBuilder;
import io.github.hajdbc.DatabaseClusterConfigurationBuilder;

/**
 * @author Paul Ferraro
 */
public interface DatabaseClusterConfigurationReader<Z, D extends Database<Z>, B extends DatabaseBuilder<Z, D>>
{
	void read(XMLStreamReader reader, DatabaseClusterConfigurationBuilder<Z, D, B> builder) throws XMLStreamException;
}
