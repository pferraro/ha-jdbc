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
package io.github.hajdbc.dialect;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;
import java.util.regex.Pattern;

import io.github.hajdbc.ColumnProperties;
import io.github.hajdbc.QualifiedName;
import io.github.hajdbc.SequenceProperties;
import io.github.hajdbc.SequenceSupport;
import io.github.hajdbc.TableProperties;
import io.github.hajdbc.dialect.postgresql.PostgreSQLDialectFactory;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Paul Ferraro
 *
 */
public class PostgreSQLDialectTest extends StandardDialectTest
{
	public PostgreSQLDialectTest()
	{
		super(new PostgreSQLDialectFactory());
	}

	@Override
	public void getSequenceSupport()
	{
		assertSame(this.dialect, this.dialect.getSequenceSupport());
	}

	@Override
	public void getIdentityColumnSupport()
	{
		assertSame(this.dialect, this.dialect.getIdentityColumnSupport());
	}

	@Override
	public void getColumnType() throws SQLException
	{
		ColumnProperties column = mock(ColumnProperties.class);
		
		when(column.getNativeType()).thenReturn("oid");
		
		int result = this.dialect.getColumnType(column);
		
		assertEquals(Types.BLOB, result);
		
		when(column.getNativeType()).thenReturn("int");		
		when(column.getType()).thenReturn(Types.INTEGER);
		
		result = this.dialect.getColumnType(column);
		
		assertEquals(Types.INTEGER, result);
	}

	@Override
	public void getTruncateTableSQL() throws SQLException
	{
		TableProperties table = mock(TableProperties.class);
		QualifiedName name = mock(QualifiedName.class);
		
		when(table.getName()).thenReturn(name);
		when(name.getDMLName()).thenReturn("table");
		
		String result = this.dialect.getTruncateTableSQL(table);
		
		assertEquals("TRUNCATE TABLE table", result);
	}

	@Override
	public void getNextSequenceValueSQL() throws SQLException
	{
		SequenceProperties sequence = mock(SequenceProperties.class);
		QualifiedName name = mock(QualifiedName.class);
		
		when(sequence.getName()).thenReturn(name);
		when(name.getDMLName()).thenReturn("sequence");
		
		String result = this.dialect.getSequenceSupport().getNextSequenceValueSQL(sequence);
		
		assertEquals("SELECT NEXTVAL('sequence')", result);
	}
	
	@Override
	public void parseSequence() throws SQLException
	{
		SequenceSupport support = this.dialect.getSequenceSupport();
		
		assertEquals("sequence", support.parseSequence("SELECT CURRVAL('sequence')"));
		assertEquals("sequence", support.parseSequence("SELECT nextval('sequence'), * FROM table"));
		assertEquals("sequence", support.parseSequence("INSERT INTO table VALUES (NEXTVAL('sequence'), 0)"));
		assertEquals("sequence", support.parseSequence("UPDATE table SET id = NEXTVAL('sequence')"));
		assertNull(support.parseSequence("SELECT NEXT VALUE FOR sequence"));
	}

	@Override
	public void getDefaultSchemas() throws SQLException
	{
		DatabaseMetaData metaData = mock(DatabaseMetaData.class);
		Connection connection = mock(Connection.class);
		Statement statement = mock(Statement.class);
		ResultSet resultSet = mock(ResultSet.class);
		
		when(metaData.getConnection()).thenReturn(connection);
		when(connection.createStatement()).thenReturn(statement);
		
		when(statement.executeQuery("SHOW search_path")).thenReturn(resultSet);
		when(resultSet.next()).thenReturn(false);
		when(resultSet.getString(1)).thenReturn("$user,public");
		
		when(metaData.getUserName()).thenReturn("user");
		
		List<String> result = this.dialect.getDefaultSchemas(metaData);

		assertEquals(2, result.size());
		assertEquals("user", result.get(0));
		assertEquals("public", result.get(1));
		
		verify(resultSet).close();
		verify(statement).close();
	}
	
	@Override
	public void getAlterIdentityColumnSQL() throws SQLException
	{
		TableProperties table = mock(TableProperties.class);
		ColumnProperties column = mock(ColumnProperties.class);
		QualifiedName name = mock(QualifiedName.class);
		
		when(table.getName()).thenReturn(name);
		when(name.getDDLName()).thenReturn("table");
		when(column.getName()).thenReturn("column");
		
		String result = this.dialect.getIdentityColumnSupport().getAlterIdentityColumnSQL(table, column, 1000L);

		assertEquals("ALTER SEQUENCE table_column_seq RESTART WITH 1000", result);
	}

	@Override
	public void evaluateRand()
	{
		assertTrue(Pattern.matches("SELECT ((0\\.\\d+)|([1-9]\\.\\d+E\\-\\d+)) FROM test", this.dialect.evaluateRand("SELECT RANDOM() FROM test")));
		assertTrue(Pattern.matches("SELECT ((0\\.\\d+)|([1-9]\\.\\d+E\\-\\d+)) FROM test", this.dialect.evaluateRand("SELECT RANDOM ( ) FROM test")));
		assertEquals("SELECT RAND() FROM test", this.dialect.evaluateRand("SELECT RAND() FROM test"));
		assertEquals("SELECT OPERANDOM() FROM test", this.dialect.evaluateRand("SELECT OPERANDOM() FROM test"));
	}
}
