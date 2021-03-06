/*
 * HA-JDBC: High-Availability JDBC
 * Copyright (C) 2012  Paul Ferraro
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
package io.github.hajdbc.sync;

import java.io.Serializable;
import java.sql.SQLException;

import io.github.hajdbc.Database;
import io.github.hajdbc.TableProperties;

public interface TableSynchronizationStrategy extends Serializable
{
	<Z, D extends Database<Z>> void synchronize(SynchronizationContext<Z, D> context, TableProperties table) throws SQLException;
	
	<Z, D extends Database<Z>> void dropConstraints(SynchronizationContext<Z, D> context) throws SQLException;
	
	<Z, D extends Database<Z>> void restoreConstraints(SynchronizationContext<Z, D> context) throws SQLException;
}
