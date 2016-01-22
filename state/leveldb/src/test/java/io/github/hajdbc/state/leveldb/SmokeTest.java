/*
 * HA-JDBC: High-Availability JDBC
 * Copyright (C) 2016  Paul Ferraro
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
package io.github.hajdbc.state.leveldb;

import java.sql.SQLException;

import org.junit.Ignore;


/**
 * @author Paul Ferraro
 */
@Ignore
public class SmokeTest extends io.github.hajdbc.SmokeTest
{
	@Override
	public void test() throws SQLException
	{
		LevelDBStateManagerFactory factory = new LevelDBStateManagerFactory();
		factory.setLocationPattern("./target/leveldb/{0}");
		testState(factory);
	}
}
