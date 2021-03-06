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
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLTransientConnectionException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.transaction.xa.XAException;

import io.github.hajdbc.ColumnProperties;
import io.github.hajdbc.ColumnPropertiesFactory;
import io.github.hajdbc.Database;
import io.github.hajdbc.DumpRestoreSupport;
import io.github.hajdbc.ForeignKeyConstraint;
import io.github.hajdbc.ForeignKeyConstraintFactory;
import io.github.hajdbc.IdentifierNormalizer;
import io.github.hajdbc.IdentityColumnSupport;
import io.github.hajdbc.QualifiedName;
import io.github.hajdbc.QualifiedNameFactory;
import io.github.hajdbc.SequenceProperties;
import io.github.hajdbc.SequencePropertiesFactory;
import io.github.hajdbc.SequenceSupport;
import io.github.hajdbc.TableProperties;
import io.github.hajdbc.TriggerEvent;
import io.github.hajdbc.TriggerSupport;
import io.github.hajdbc.TriggerTime;
import io.github.hajdbc.UniqueConstraint;
import io.github.hajdbc.UniqueConstraintFactory;
import io.github.hajdbc.codec.Decoder;
import io.github.hajdbc.util.Strings;

/**
 * @author  Paul Ferraro
 * @since   1.1
 */
public class StandardDialect implements Dialect, SequenceSupport, IdentityColumnSupport, TriggerSupport
{
	// Taken from SQL:2003 column of: http://www.postgresql.org/docs/9.1/static/sql-keywords-appendix.html
	// Only includes reserved keywords, since we only care about those keywords that require quoting to be used as an identifier
	protected static final String[] SQL_2003_RESERVED_KEY_WORDS = new String[] {
		"ABS", "ALL", "ALLOCATE", "ALTER", "AND", "ANY", "ARE", "ARRAY", "AS", "ASENSITIVE", "ASYMMETRIC", "AT", "ATOMIC", "AUTHORIZATION", "AVG",
		"BEGIN", "BETWEEN", "BIGINT", "BINARY", "BLOB", "BOOLEAN", "BOTH", "BY",
		"CALL", "CALLED", "CARDINALITY", "CASCADED", "CASE", "CAST", "CEIL", "CEILING", "CHAR", "CHARACTER", "CHARACTER_LENGTH", "CHAR_LENGTH", "CHECK", "CLOB", "CLOSE", "COALESCE", "COLLATE", "COLLECT", "COLUMN", "COMMIT", "CONDITION", "CONNECT", "CONSTRAINT", "CONVERT", "CORR", "CORRESPONDING", "COUNT", "COVAR_POP", "COVAR_SAMP", "CREATE", "CROSS", "CUBE", "CUME_DIST", "CURRENT", "CURRENT_DATE", "CURRENT_DEFAULT_TRANSFORM_GROUP", "CURRENT_PATH", "CURRENT_ROLE", "CURRENT_TIME", "CURRENT_TIMESTAMP", "CURRENT_TRANSFORM_GROUP_FOR_TYPE", "CURRENT_USER", "CURSOR", "CYCLE",
		"DATALINK", "DATE", "DAY", "DEALLOCATE", "DEC", "DECIMAL", "DECLARE", "DEFAULT", "DELETE", "DENSE_RANK", "DEREF", "DESCRIBE", "DETERMINISTIC", "DISCONNECT", "DISTINCT", "DLNEWCOPY", "DLPREVIOUSCOPY", "DLURLCOMPLETE", "DLURLCOMPLETEONLY", "DLURLCOMPLETEWRITE", "DLURLPATH", "DLURLPATHONLY", "DLURLPATHWRITE", "DLURLSCHEME", "DLURLSERVER", "DLVALUE", "DOUBLE", "DROP", "DYNAMIC",
		"EACH", "ELEMENT", "ELSE", "END", "END-EXEC", "ESCAPE", "EVERY", "EXCEPT", "EXEC", "EXECUTE", "EXISTS", "EXP", "EXTERNAL", "EXTRACT",
		"FALSE", "FETCH", "FILTER", "FLOAT", "FLOOR", "FOR", "FOREIGN", "FREE", "FROM", "FULL", "FUNCTION", "FUSION",
		"GET", "GLOBAL", "GRANT", "GROUP", "GROUPING",
		"HAVING", "HOLD", "HOUR",
		"IDENTITY", "IMPORT", "IN", "INDICATOR", "INNER", "INOUT", "INSENSITIVE", "INSERT", "INT", "INTEGER", "INTERSECT", "INTERSECTION", "INTERVAL", "INTO", "IS",
		"JOIN",
		"LANGUAGE", "LARGE", "LATERAL", "LEADING", "LEFT", "LIKE", "LN", "LOCAL", "LOCALTIME", "LOCALTIMESTAMP", "LOWER",
		"MATCH", "MAX", "MEMBER", "MERGE", "METHOD", "MIN", "MINUTE", "MOD", "MODIFIES", "MODULE", "MONTH", "MULTISET",
		"NATIONAL", "NATURAL", "NCHAR", "NCLOB", "NEW", "NO", "NONE", "NORMALIZE", "NOT", "NULL", "NULLIF", "NUMERIC",
		"OCTET_LENGTH", "OF", "OLD", "ON", "ONLY", "OPEN", "OR", "ORDER", "OUT", "OUTER", "OVER", "OVERLAPS", "OVERLAY",
		"PARAMETER", "PARTITION", "PERCENTILE_CONT", "PERCENTILE_DISC", "PERCENT_RANK", "POSITION", "POWER", "PRECISION", "PREPARE", "PRIMARY", "PROCEDURE",
		"RANGE", "RANK", "READS", "REAL", "RECURSIVE", "REF", "REFERENCES", "REFERENCING", "REGR_AVGX", "REGR_AVGY", "REGR_COUNT", "REGR_INTERCEPT", "REGR_R2", "REGR_SLOPE", "REGR_SXX", "REGR_SXY", "REGR_SYY", "RELEASE", "RESULT", "RETURN", "RETURNS", "REVOKE", "RIGHT", "ROLLBACK", "ROLLUP", "ROW", "ROWS", "ROW_NUMBER",
		"SAVEPOINT", "SCOPE", "SCROLL", "SEARCH", "SECOND", "SELECT", "SENSITIVE", "SESSION_USER", "SET", "SIMILAR", "SMALLINT", "SOME", "SPECIFIC", "SPECIFICTYPE", "SQL", "SQLEXCEPTION", "SQLSTATE", "SQLWARNING", "SQRT", "START", "STATIC", "STDDEV_POP", "STDDEV_SAMP", "SUBMULTISET", "SUBSTRING", "SUM", "SYMMETRIC", "SYSTEM", "SYSTEM_USER",
		"TABLE", "TABLESAMPLE", "THEN", "TIME", "TIMESTAMP", "TIMEZONE_HOUR", "TIMEZONE_MINUTE", "TO", "TRAILING", "TRANSLATE", "TRANSLATION", "TREAT", "TRIGGER", "TRIM", "TRUE",
		"UESCAPE", "UNION", "UNIQUE", "UNKNOWN", "UNNEST", "UPDATE", "UPPER", "USER", "USING",
		"VALUE", "VALUES", "VARCHAR", "VARYING", "VAR_POP", "VAR_SAMP",
		"WHEN", "WHENEVER", "WHERE", "WIDTH_BUCKET", "WINDOW", "WITH", "WITHIN", "WITHOUT",
		"XML", "XMLAGG", "XMLATTRIBUTES", "XMLBINARY", "XMLCOMMENT", "XMLCONCAT", "XMLELEMENT", "XMLFOREST", "XMLNAMESPACES", "XMLPARSE", "XMLPI", "XMLROOT", "XMLSERIALIZE",
		"YEAR",
	};
	
	private final Pattern selectForUpdatePattern = compile(this.selectForUpdatePattern());
	private final Pattern insertIntoTablePattern = compile(this.insertIntoTablePattern());
	private final Pattern sequencePattern = compile(this.sequencePattern());
	private final Pattern currentTimestampPattern = compile(this.currentTimestampPattern());
	private final Pattern currentDatePattern = compile(this.currentDatePattern());
	private final Pattern currentTimePattern = compile(this.currentTimePattern());
	private final Pattern randomPattern = compile(this.randomPattern());
	private final Pattern urlPattern = Pattern.compile(String.format("jdbc\\:%s\\:%s", this.vendorPattern(), this.locatorPattern()));
	
	protected String vendorPattern()
	{
		return "[^\\:]+";
	}

	protected String locatorPattern()
	{
		return "(?://(?<host>[^\\:/]+)(?:\\:(?<port>\\d+))?/)?(?<database>[^\\?]+)";
	}
	
	private static Pattern compile(String pattern)
	{
		return Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
	}
	
	protected String selectForUpdatePattern()
	{
		return "SELECT\\s+.+\\s+FOR\\s+UPDATE";
	}

	protected String insertIntoTablePattern()
	{
		return "INSERT\\s+(?:INTO\\s+)?'?([^'\\s\\(]+)";
	}

	protected String sequencePattern()
	{
		return "NEXT\\s+VALUE\\s+FOR\\s+'?([^',\\s\\(\\)]+)";
	}
	
	protected String currentDatePattern()
	{
		return "(?<=\\W)CURRENT_DATE(?=\\W)";
	}
	
	protected String currentTimePattern()
	{
		return "(?<=\\W)CURRENT_TIME(?:\\s*\\(\\s*\\d+\\s*\\))?(?=\\W)|(?<=\\W)LOCALTIME(?:\\s*\\(\\s*\\d+\\s*\\))?(?=\\W)";
	}

	protected String currentTimestampPattern()
	{
		return "(?<=\\W)CURRENT_TIMESTAMP(?:\\s*\\(\\s*\\d+\\s*\\))?(?=\\W)|(?<=\\W)LOCALTIMESTAMP(?:\\s*\\(\\s*\\d+\\s*\\))?(?=\\W)";
	}
	
	protected String randomPattern()
	{
		return "(?<=\\W)RAND\\s*\\(\\s*\\)";
	}

	@SuppressWarnings("unused")
	protected String schemaPattern(DatabaseMetaData metaData) throws SQLException
	{
		return null;
	}

	protected String executeFunctionFormat()
	{
		StringBuilder builder = new StringBuilder("SELECT {0}");
		
		String dummyTable = this.dummyTable();
		
		if (dummyTable != null)
		{
			builder.append(" FROM ").append(dummyTable);
		}
		
		return builder.toString();
	}
	
	protected String executeFunctionSQL(String function)
	{
		return MessageFormat.format(this.executeFunctionFormat(), function);
	}

	protected String dummyTable()
	{
		return null;
	}

	@Override
	public String getTruncateTableSQL(TableProperties properties)
	{
		return MessageFormat.format(this.truncateTableFormat(), properties.getName().getDMLName());
	}
	
	protected String truncateTableFormat()
	{
		return "DELETE FROM {0}";
	}

	@Override
	public String getCreateForeignKeyConstraintSQL(ForeignKeyConstraint key)
	{
		return MessageFormat.format(this.createForeignKeyConstraintFormat(), key.getName(), key.getTable().getDDLName(), Strings.join(key.getColumnList(), Strings.PADDED_COMMA), key.getForeignTable().getDDLName(), Strings.join(key.getForeignColumnList(), Strings.PADDED_COMMA), key.getDeleteRule(), key.getUpdateRule(), key.getDeferrability());
	}
	
	protected String createForeignKeyConstraintFormat()
	{
		return "ALTER TABLE {1} ADD CONSTRAINT {0} FOREIGN KEY ({2}) REFERENCES {3} ({4}) ON DELETE {5,choice,0#CASCADE|1#RESTRICT|2#SET NULL|3#NO ACTION|4#SET DEFAULT} ON UPDATE {6,choice,0#CASCADE|1#RESTRICT|2#SET NULL|3#NO ACTION|4#SET DEFAULT} {7,choice,5#DEFERRABLE INITIALLY DEFERRED|6#DEFERRABLE INITIALLY IMMEDIATE|7#NOT DEFERRABLE}";
	}
	
	@Override
	public String getDropForeignKeyConstraintSQL(ForeignKeyConstraint key)
	{
		return MessageFormat.format(this.dropForeignKeyConstraintFormat(), key.getName(), key.getTable().getDDLName());
	}
	
	protected String dropForeignKeyConstraintFormat()
	{
		return this.dropConstraintFormat();
	}
	
	protected String dropConstraintFormat()
	{
		return "ALTER TABLE {1} DROP CONSTRAINT {0}";
	}

	@Override
	public String getCreateUniqueConstraintSQL(UniqueConstraint constraint)
	{
		return MessageFormat.format(this.createUniqueConstraintFormat(), constraint.getName(), constraint.getTable().getDDLName(), Strings.join(constraint.getColumnList(), Strings.PADDED_COMMA));
	}
	
	protected String createUniqueConstraintFormat()
	{
		return "ALTER TABLE {1} ADD CONSTRAINT {0} UNIQUE ({2})";
	}

	@Override
	public String getDropUniqueConstraintSQL(UniqueConstraint constraint)
	{
		return MessageFormat.format(this.dropUniqueConstraintFormat(), constraint.getName(), constraint.getTable().getDDLName());
	}
	
	protected String dropUniqueConstraintFormat()
	{
		return this.dropConstraintFormat();
	}

	@Override
	public boolean isSelectForUpdate(String sql)
	{
		return this.selectForUpdatePattern.matcher(sql).find();
	}

	@Override
	public List<String> getDefaultSchemas(DatabaseMetaData metaData) throws SQLException
	{
		return Collections.singletonList(metaData.getUserName());
	}

	protected String executeFunction(Connection connection, String function) throws SQLException
	{
		try (Statement statement = connection.createStatement())
		{
			try (ResultSet resultSet = statement.executeQuery(this.executeFunctionSQL(function)))
			{
				resultSet.next();
				
				return resultSet.getString(1);
			}
		}
	}

	protected List<String> executeQuery(Connection connection, String sql) throws SQLException
	{
		try (Statement statement = connection.createStatement())
		{
			try (ResultSet resultSet = statement.executeQuery(sql))
			{
				List<String> resultList = new LinkedList<>();
				
				while (resultSet.next())
				{
					resultList.add(resultSet.getString(1));
				}
				
				return resultList;
			}
		}
	}

	@Override
	public SequenceSupport getSequenceSupport()
	{
		return null;
	}

	@Override
	public String parseSequence(String sql)
	{
		return this.parse(this.sequencePattern, sql);
	}

	@Override
	public int getColumnType(ColumnProperties properties)
	{
		return properties.getType();
	}

	@Override
	public Collection<SequenceProperties> getSequences(DatabaseMetaData metaData, SequencePropertiesFactory factory) throws SQLException
	{
		try (ResultSet resultSet = metaData.getTables(Strings.EMPTY, null, Strings.ANY, new String[] { this.sequenceTableType() }))
		{
			List<SequenceProperties> sequences = new LinkedList<>();
			
			while (resultSet.next())
			{
				sequences.add(factory.createSequenceProperties(resultSet.getString("TABLE_SCHEM"), resultSet.getString("TABLE_NAME"), 1));
			}
			
			return sequences;
		}
	}

	protected String sequenceTableType()
	{
		return "SEQUENCE";
	}

	@Override
	public String getNextSequenceValueSQL(SequenceProperties sequence)
	{
		return this.executeFunctionSQL(MessageFormat.format(this.nextSequenceValueFormat(), sequence.getName().getDMLName()));
	}
	
	protected String nextSequenceValueFormat()
	{
		return "NEXT VALUE FOR {0}";
	}
	
	@Override
	public String getAlterSequenceSQL(SequenceProperties sequence, long value)
	{
		return MessageFormat.format(this.alterSequenceFormat(), sequence.getName().getDDLName(), String.valueOf(value), String.valueOf(sequence.getIncrement()));
	}
	
	protected String alterSequenceFormat()
	{
		return "ALTER SEQUENCE {0} RESTART WITH {1}";
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport()
	{
		return null;
	}
	
	@Override
	public String parseInsertTable(String sql)
	{
		return this.parse(this.insertIntoTablePattern, sql);
	}

	@Override
	public String getAlterIdentityColumnSQL(TableProperties table, ColumnProperties column, long value)
	{
		return MessageFormat.format(this.alterIdentityColumnFormat(), table.getName().getDDLName(), column.getName(), String.valueOf(value));
	}

	protected String alterIdentityColumnFormat()
	{
		return "ALTER TABLE {0} ALTER COLUMN {1} RESTART WITH {2}";
	}

	protected String parse(Pattern pattern, String string)
	{
		Matcher matcher = pattern.matcher(string);
		
		return matcher.find() ? matcher.group(1) : null;
	}

	@Override
	public String evaluateCurrentDate(String sql, java.sql.Date date)
	{
		return evaluateTemporal(sql, this.currentDatePattern, date, this.dateLiteralFormat());
	}
	
	protected String dateLiteralFormat()
	{
		return "DATE ''{0}''";
	}

	@Override
	public String evaluateCurrentTime(String sql, java.sql.Time time)
	{
		return evaluateTemporal(sql, this.currentTimePattern, time, this.timeLiteralFormat());
	}
	
	protected String timeLiteralFormat()
	{
		return "TIME ''{0}''";
	}

	@Override
	public String evaluateCurrentTimestamp(String sql, java.sql.Timestamp timestamp)
	{
		return evaluateTemporal(sql, this.currentTimestampPattern, timestamp, this.timestampLiteralFormat());
	}
	
	protected String timestampLiteralFormat()
	{
		return "TIMESTAMP ''{0}''";
	}

	private static String evaluateTemporal(String sql, Pattern pattern, java.util.Date date, String format)
	{
		return pattern.matcher(sql).replaceAll(MessageFormat.format(format, date.toString()));
	}

	@Override
	public String evaluateRand(String sql)
	{	
		StringBuffer buffer = new StringBuffer();
		Matcher matcher = this.randomPattern.matcher(sql);
		
		while (matcher.find())
		{
			matcher.appendReplacement(buffer, Double.toString(Math.random()));
		}
		
		return matcher.appendTail(buffer).toString();
	}

	@Override
	public boolean indicatesFailure(SQLException e)
	{
		if ((e instanceof SQLNonTransientConnectionException) || (e instanceof SQLTransientConnectionException))
		{
			return true;
		}
		
		String sqlState = e.getSQLState();
		if ((sqlState != null) && this.indicatesFailure(sqlState))
		{
			return true;
		}
		
		return this.indicatesFailure(e.getErrorCode());
	}

	/**
	 * Indicates whether the specified SQLState indicates a database failure that should result in a database deactivation.
	 * @param sqlState a SQL:2003 or X/Open SQLState
	 * @return true if the SQLState indicate a failure, false otherwise
	 */
	protected boolean indicatesFailure(String sqlState)
	{
		// 08 class SQLStates indicate connection failures
		return sqlState.startsWith("08");
	}

	/**
	 * Indicates whether the specified vendor-specific error code indicates a database failure that should result in a database deactivation.
	 * @param code a vendor-specific error code
	 * @return true if the error code indicate a failure, false otherwise
	 */
	protected boolean indicatesFailure(int code)
	{
		return false;
	}
	
	@Override
	public boolean indicatesFailure(XAException e)
	{
		return this.failureXAErrorCodes().contains(e.errorCode);
	}
	
	protected Set<Integer> failureXAErrorCodes()
	{
		return Collections.singleton(XAException.XAER_RMFAIL);
	}

	@Override
	public DumpRestoreSupport getDumpRestoreSupport()
	{
		return null;
	}
	
	@Override
	public TriggerSupport getTriggerSupport()
	{
		return null;
	}

	@Override
	public String getCreateTriggerSQL(String name, TableProperties table, TriggerEvent event, String action)
	{
		return MessageFormat.format(this.createTriggerFormat(), name, event.getTime().toString(), event.toString(), table.getName().getDDLName(), action);
	}

	protected String createTriggerFormat()
	{
		return "CREATE TRIGGER {0} {1} {2} ON {3} FOR EACH ROW BEGIN {4} END";
	}
	
	@Override
	public String getDropTriggerSQL(String name, TableProperties table)
	{
		return MessageFormat.format(this.dropTriggerFormat(), name, table.getName().getDDLName());
	}

	protected String dropTriggerFormat()
	{
		return "DROP TRIGGER {1} ON {2}";
	}
	
	@Override
	public String getTriggerRowAlias(TriggerTime time)
	{
		return time.getAlias();
	}

	@Override
	public String getCreateSchemaSQL(String schema)
	{
		return MessageFormat.format(this.createSchemaFormat(), schema);
	}

	protected String createSchemaFormat()
	{
		return "CREATE SCHEMA {0}";
	}
	
	@Override
	public String getDropSchemaSQL(String schema)
	{
		return MessageFormat.format(this.dropSchemaFormat(), schema);
	}

	protected String dropSchemaFormat()
	{
		return "DROP SCHEMA {0}";
	}

	protected boolean meetsRequirement(int minMajor, int minMinor)
	{
		Driver driver = this.findDriver();

		if (driver != null)
		{
			int major = driver.getMajorVersion();
			int minor = driver.getMinorVersion();
			return (major > minMajor) || ((major == minMajor) && (minor >= minMinor));
		}
		
		return false;
	}
	
	private Driver findDriver()
	{
		String url = String.format("jdbc:%s:test", this.vendorPattern());
		
		List<Driver> drivers = Collections.list(DriverManager.getDrivers());
		for (Driver driver: drivers)
		{
			try
			{
				if (driver.acceptsURL(url))
				{
					return driver;
				}
			}
			catch (SQLException e)
			{
				// Skip
			}
		}
		return null;
	}
	
	@Override
	public Collection<QualifiedName> getTables(DatabaseMetaData metaData, QualifiedNameFactory factory) throws SQLException
	{
		try (ResultSet resultSet = metaData.getTables(getCatalog(metaData), this.schemaPattern(metaData), Strings.ANY, new String[] { "TABLE" }))
		{
			List<QualifiedName> list = new LinkedList<>();
			
			while (resultSet.next())
			{
				list.add(factory.createQualifiedName(resultSet.getString("TABLE_SCHEM"), resultSet.getString("TABLE_NAME")));
			}
			
			return list;
		}
	}

	@Override
	public Map<String, ColumnProperties> getColumns(DatabaseMetaData metaData, QualifiedName table, ColumnPropertiesFactory factory) throws SQLException
	{
		try (Statement statement = metaData.getConnection().createStatement())
		{
			Map<String, ColumnProperties> map = new HashMap<>();
			
			ResultSetMetaData resultSet = statement.executeQuery(String.format("SELECT * FROM %s WHERE 0=1", table.getDMLName())).getMetaData();
			
			for (int i = 1; i <= resultSet.getColumnCount(); ++i)
			{
				String column = resultSet.getColumnName(i);
				int type = resultSet.getColumnType(i);
				String nativeType = resultSet.getColumnTypeName(i);
				boolean autoIncrement = resultSet.isAutoIncrement(i);
				
				ColumnProperties properties = factory.createColumnProperties(column, type, nativeType, null, null, autoIncrement);
				map.put(properties.getName(), properties);
			}
			
			return map;
		}
	}

	@Override
	public UniqueConstraint getPrimaryKey(DatabaseMetaData metaData, QualifiedName table, UniqueConstraintFactory factory) throws SQLException
	{
		try (ResultSet resultSet = metaData.getPrimaryKeys(getCatalog(metaData), table.getSchema(), table.getName()))
		{
			UniqueConstraint constraint = null;

			while (resultSet.next())
			{
				if (constraint == null)
				{
					constraint = factory.createUniqueConstraint(resultSet.getString("PK_NAME"), table);
				}
				
				constraint.getColumnList().add(resultSet.getString("COLUMN_NAME"));
			}
			
			return constraint;
		}
	}

	@Override
	public Collection<ForeignKeyConstraint> getForeignKeyConstraints(DatabaseMetaData metaData, QualifiedName table, ForeignKeyConstraintFactory factory) throws SQLException
	{
		try (ResultSet resultSet = metaData.getImportedKeys(getCatalog(metaData), table.getSchema(), table.getName()))
		{
			Map<String, ForeignKeyConstraint> foreignKeyMap = new HashMap<>();
			
			while (resultSet.next())
			{
				String name = resultSet.getString("FK_NAME");
				
				ForeignKeyConstraint foreignKey = foreignKeyMap.get(name);
				
				if (foreignKey == null)
				{
					foreignKey = factory.createForeignKeyConstraint(name, table, factory.getQualifiedNameFactory().createQualifiedName(resultSet.getString("PKTABLE_SCHEM"), resultSet.getString("PKTABLE_NAME")), resultSet.getInt("DELETE_RULE"), resultSet.getInt("UPDATE_RULE"), resultSet.getInt("DEFERRABILITY"));
					
					foreignKeyMap.put(name, foreignKey);
				}
				
				foreignKey.getColumnList().add(resultSet.getString("FKCOLUMN_NAME"));
				foreignKey.getForeignColumnList().add(resultSet.getString("PKCOLUMN_NAME"));
			}
			
			return foreignKeyMap.values();
		}
	}

	@Override
	public Collection<UniqueConstraint> getUniqueConstraints(DatabaseMetaData metaData, QualifiedName table, UniqueConstraint primaryKey, UniqueConstraintFactory factory) throws SQLException
	{
		try (ResultSet resultSet = metaData.getIndexInfo(getCatalog(metaData), table.getSchema(), table.getName(), true, false))
		{
			Map<String, UniqueConstraint> keyMap = new HashMap<>();
			
			while (resultSet.next())
			{
				if (resultSet.getShort("TYPE") == DatabaseMetaData.tableIndexHashed)
				{
					String name = resultSet.getString("INDEX_NAME");
					
					UniqueConstraint key = keyMap.get(name);
					
					if (key == null)
					{
						key = factory.createUniqueConstraint(name, table);
						
						// Don't include the primary key
						if (key.equals(primaryKey)) continue;
						
						keyMap.put(name, key);
					}
					
					key.getColumnList().add(resultSet.getString("COLUMN_NAME"));
				}
			}
			return keyMap.values();
		}
	}
	
	private static String getCatalog(DatabaseMetaData metaData) throws SQLException
	{
		String catalog = metaData.getConnection().getCatalog();
		
		return (catalog != null) ? catalog : Strings.EMPTY;
	}
	
	@Override
	public Collection<String> getIdentityColumns(Collection<ColumnProperties> columns) throws SQLException
	{
		List<String> columnList = new LinkedList<>();
		
		for (ColumnProperties column: columns)
		{
			if (column.isAutoIncrement())
			{
				columnList.add(column.getName());
			}
		}
		
		return columnList;
	}

	@Override
	public Map<Integer, Entry<String, Integer>> getTypes(DatabaseMetaData metaData) throws SQLException
	{
		try (ResultSet resultSet = metaData.getTypeInfo())
		{
			Map<Integer, Map.Entry<String, Integer>> types = new HashMap<>();
			
			while (resultSet.next())
			{
				int type = resultSet.getInt("DATA_TYPE");
				if (!types.containsKey(type))
				{
					String name = resultSet.getString("TYPE_NAME");
					String params = resultSet.getString("CREATE_PARAMS");
					types.put(type, new AbstractMap.SimpleImmutableEntry<>(name, (params != null) ? resultSet.getInt("PRECISION") : null));
				}
			}
			
			return types;
		}
	}

	@Override
	public IdentifierNormalizer createIdentifierNormalizer(DatabaseMetaData metaData) throws SQLException
	{
		return new StandardIdentifierNormalizer(metaData, this.identifierPattern(metaData), this.reservedIdentifiers(metaData));
	}

	protected Pattern identifierPattern(DatabaseMetaData metaData) throws SQLException
	{
		return Pattern.compile(MessageFormat.format("[a-zA-Z][\\w{0}]*", Pattern.quote(metaData.getExtraNameCharacters())));
	}
	
	protected Set<String> reservedIdentifiers(DatabaseMetaData metaData) throws SQLException
	{
		Set<String> set = new HashSet<>(Arrays.asList(SQL_2003_RESERVED_KEY_WORDS));
		
		for (String word: metaData.getSQLKeywords().split(Strings.COMMA))
		{
			set.add(word.toUpperCase());
		}
		
		return set;
	}

	@Override
	public QualifiedNameFactory createQualifiedNameFactory(DatabaseMetaData metaData, IdentifierNormalizer normalizer) throws SQLException
	{
		return new StandardQualifiedNameFactory(metaData, normalizer);
	}

	@Override
	public ColumnPropertiesFactory createColumnPropertiesFactory(IdentifierNormalizer normalizer)
	{
		return new StandardColumnPropertiesFactory(normalizer);
	}

	@Override
	public SequencePropertiesFactory createSequencePropertiesFactory(QualifiedNameFactory factory)
	{
		return new StandardSequencePropertiesFactory(factory);
	}

	@Override
	public ForeignKeyConstraintFactory createForeignKeyConstraintFactory(QualifiedNameFactory factory)
	{
		return new StandardForeignKeyConstraintFactory(factory);
	}

	@Override
	public UniqueConstraintFactory createUniqueConstraintFactory(IdentifierNormalizer normalizer)
	{
		return new StandardUniqueConstraintFactory(normalizer);
	}

	@Override
	public boolean isValid(Connection connection) throws SQLException
	{
		return connection.isValid(0);
	}

	@Override
	public <Z, D extends Database<Z>> ConnectionProperties getConnectionProperties(D database, Decoder decoder) throws SQLException
	{
		final String password = database.getCredentials().decodePassword(decoder);
		try (Connection connection = database.connect(decoder))
		{
			DatabaseMetaData metaData = connection.getMetaData();
			String url = metaData.getURL();
			
			if (url == null)
			{
				throw new UnsupportedOperationException();
			}
				
			Matcher matcher = this.urlPattern.matcher(url);
			
			if (!matcher.find())
			{
				throw new UnsupportedOperationException(url);
			}
			
			final String host = matcher.group("host");
			final String port = matcher.group("port");
			final String databaseName = matcher.group("database");
			final String user = metaData.getUserName();
			
			return new ConnectionProperties()
			{
				@Override
				public String getHost()
				{
					return host;
				}
	
				@Override
				public String getPort()
				{
					return port;
				}
	
				@Override
				public String getDatabase()
				{
					return databaseName;
				}
	
				@Override
				public String getUser()
				{
					return user;
				}
	
				@Override
				public String getPassword()
				{
					return password;
				}
			};
		}
	}
}
