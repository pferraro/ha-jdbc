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
package io.github.hajdbc.dialect.mysql;

import java.io.File;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.github.hajdbc.Database;
import io.github.hajdbc.DumpRestoreSupport;
import io.github.hajdbc.codec.Decoder;
import io.github.hajdbc.dialect.ConnectionProperties;
import io.github.hajdbc.dialect.StandardDialect;
import io.github.hajdbc.util.Processes;
import io.github.hajdbc.util.Strings;

/**
 * Dialect for <a href="http://www.mysql.com/products/database/mysql/">MySQL</a>
 * @author Paul Ferraro
 */
@SuppressWarnings("nls")
public class MySQLDialect extends StandardDialect implements DumpRestoreSupport
{
	// Taken from: http://dev.mysql.com/doc/refman/5.7/en/reserved-words.html
	protected static final String[] RESERVED_KEY_WORDS = new String[] {
		"ACCESSIBLE", "ADD", "ALL", "ALTER", "ANALYZE", "AND", "AS", "ASC", "ASENSITIVE",
		"BEFORE", "BETWEEN", "BIGINT", "BINARY", "BLOB", "BOTH", "BY",
		"CALL", "CASCADE", "CASE", "CHANGE", "CHAR", "CHARACTER", "CHECK", "COLLATE", "COLUMN", "CONDITION", "CONSTRAINT", "CONTINUE", "CONVERT", "CREATE", "CROSS", "CURRENT_DATE", "CURRENT_TIME", "CURRENT_TIMESTAMP", "CURRENT_USER", "CURSOR",
		"DATABASE", "DATABASES", "DAY_HOUR", "DAY_MICROSECOND", "DAY_MINUTE", "DAY_SECOND", "DEC", "DECIMAL", "DECLARE", "DEFAULT", "DELAYED", "DELETE", "DESC", "DESCRIBE", "DETERMINISTIC", "DISTINCT", "DISTINCTROW", "DIV", "DOUBLE", "DROP", "DUAL",
		"EACH", "ELSE", "ELSEIF", "ENCLOSED", "ESCAPED", "EXISTS", "EXIT", "EXPLAIN",
		"FALSE", "FETCH", "FLOAT", "FLOAT4", "FLOAT8", "FOR", "FORCE", "FOREIGN", "FROM", "FULLTEXT",
		"GET", "GRANT", "GROUP",
		"HAVING", "HIGH_PRIORITY", "HOUR_MICROSECOND", "HOUR_MINUTE", "HOUR_SECOND",
		"IF", "IGNORE", "IN", "INDEX", "INFILE", "INNER", "INOUT", "INSENSITIVE", "INSERT", "INT", "INT1", "INT2", "INT3", "INT4", "INT8", "INTEGER", "INTERVAL", "INTO", "IO_AFTER_GTIDS", "IO_BEFORE_GTIDS", "IS", "ITERATE",
		"JOIN",
		"KEY", "KEYS", "KILL",
		"LEADING", "LEAVE", "LEFT", "LIKE", "LIMIT", "LINEAR", "LINES", "LOAD", "LOCALTIME", "LOCALTIMESTAMP", "LOCK", "LONG", "LONGBLOB", "LONGTEXT", "LOOP", "LOW_PRIORITY",
		"MASTER_BIND", "MASTER_SSL_VERIFY_SERVER_CERT", "MATCH", "MAXVALUE", "MEDIUMBLOB", "MEDIUMINT", "MEDIUMTEXT", "MIDDLEINT", "MINUTE_MICROSECOND", "MINUTE_SECOND", "MOD", "MODIFIES",
		"NATURAL", "NONBLOCKING", "NOT", "NO_WRITE_TO_BINLOG", "NULL", "NUMERIC",
		"ON", "OPTIMIZE", "OPTION", "OPTIONALLY", "OR", "ORDER", "OUT", "OUTER", "OUTFILE",
		"PARTITION", "PRECISION", "PRIMARY", "PROCEDURE", "PURGE",
		"RANGE", "READ", "READS", "READ_WRITE", "REAL", "REFERENCES", "REGEXP", "RELEASE", "RENAME", "REPEAT", "REPLACE", "REQUIRE", "RESIGNAL", "RESTRICT", "RETURN", "REVOKE", "RIGHT", "RLIKE",
		"SCHEMA", "SCHEMAS", "SECOND_MICROSECOND", "SELECT", "SENSITIVE", "SEPARATOR", "SET", "SHOW", "SIGNAL", "SMALLINT", "SPATIAL", "SPECIFIC", "SQL", "SQLEXCEPTION", "SQLSTATE", "SQLWARNING", "SQL_BIG_RESULT", "SQL_CALC_FOUND_ROWS", "SQL_SMALL_RESULT", "SSL", "STARTING", "STRAIGHT_JOIN",
		"TABLE", "TERMINATED", "THEN", "TINYBLOB", "TINYINT", "TINYTEXT", "TO", "TRAILING", "TRIGGER", "TRUE",
		"UNDO", "UNION", "UNIQUE", "UNLOCK", "UNSIGNED", "UPDATE", "USAGE", "USE", "USING", "UTC_DATE", "UTC_TIME", "UTC_TIMESTAMP",
		"VALUES", "VARBINARY", "VARCHAR", "VARCHARACTER", "VARYING",
		"WHEN", "WHERE", "WHILE", "WITH", "WRITE",
		"XOR",
		"YEAR_MONTH",
		"ZEROFILL",
	};
	private static final File PASSWORD_FILE = new File(String.format("%s%s.my.cnf", Strings.USER_HOME, Strings.FILE_SEPARATOR));
	
	@Override
	protected String vendorPattern()
	{
		return "mysql";
	}

	@Override
	protected Set<String> reservedIdentifiers(DatabaseMetaData metaData)
	{
		return new HashSet<>(Arrays.asList(RESERVED_KEY_WORDS));
	}

	@Override
	public List<String> getDefaultSchemas(DatabaseMetaData metaData) throws SQLException
	{
		return Collections.singletonList(this.executeFunction(metaData.getConnection(), "DATABASE()"));
	}

	/**
	 * Deferrability clause is not supported.
	 */
	@Override
	protected String createForeignKeyConstraintFormat()
	{
		return "ALTER TABLE {1} ADD CONSTRAINT {0} FOREIGN KEY ({2}) REFERENCES {3} ({4}) ON DELETE {5,choice,0#CASCADE|1#RESTRICT|2#SET NULL|3#NO ACTION|4#SET DEFAULT} ON UPDATE {6,choice,0#CASCADE|1#RESTRICT|2#SET NULL|3#NO ACTION|4#SET DEFAULT}";
	}

	@Override
	protected String createUniqueConstraintFormat()
	{
		return "ALTER TABLE {1} ADD UNIQUE {0} ({2})";
	}

	@Override
	protected String dropForeignKeyConstraintFormat()
	{
		return "ALTER TABLE {1} DROP FOREIGN KEY {0}";
	}

	@Override
	protected String dropUniqueConstraintFormat()
	{
		return "ALTER TABLE {1} DROP INDEX {0}";
	}

	@Override
	protected String alterIdentityColumnFormat()
	{
		return "ALTER TABLE {0} AUTO_INCREMENT = {2}";
	}

	@Override
	protected String currentDatePattern()
	{
		return super.currentDatePattern() + "|(?<=\\W)CURDATE\\s*\\(\\s*\\)";
	}

	@Override
	protected String currentTimePattern()
	{
		return super.currentTimePattern() + "|(?<=\\W)CURTIME\\s*\\(\\s*\\)";
	}

	@Override
	protected String currentTimestampPattern()
	{
		return super.currentTimestampPattern() + "|(?<=\\W)NOW\\s*\\(\\s*\\)|(?<=\\W)SYSDATE\\s*\\(\\s*\\)";
	}
	
	@Override
	protected String dateLiteralFormat()
	{
		return this.timestampLiteralFormat();
	}

	@Override
	protected String timeLiteralFormat()
	{
		return this.timestampLiteralFormat();
	}

	@Override
	protected String timestampLiteralFormat()
	{
		return "''{0}''";
	}

	@Override
	public DumpRestoreSupport getDumpRestoreSupport()
	{
		return this;
	}

	@Override
	public <Z, D extends Database<Z>> void dump(D database, Decoder decoder, File file, boolean dataOnly) throws Exception
	{
		ConnectionProperties properties = this.getConnectionProperties(database, decoder);
		ProcessBuilder builder = new ProcessBuilder("mysqldump");
		List<String> args = builder.command();
		if (properties.getHost() != null)
		{
			args.add("--host=" + properties.getHost());
		}
		if (properties.getPort() != null)
		{
			args.add("--port=" + properties.getPort());
		}
		args.add("--user=" + properties.getUser());
		args.add("--result-file=" + file.getPath());
		args.add("--compress");
		if (dataOnly)
		{
			args.add("--no-create-info");
			args.add("--skip-triggers");
		}
		args.add(properties.getDatabase());
		Processes.run(setPassword(builder, properties));
	}

	@Override
	public <Z, D extends Database<Z>> void restore(D database, Decoder decoder, File file, boolean dataOnly) throws Exception
	{
		ConnectionProperties properties = this.getConnectionProperties(database, decoder);
		ProcessBuilder builder = new ProcessBuilder("mysql").redirectInput(file);
		List<String> args = builder.command();
		if (properties.getHost() != null)
		{
			args.add("--host=" + properties.getHost());
		}
		if (properties.getPort() != null)
		{
			args.add("--port=" + properties.getPort());
		}
		args.add("--user=" + properties.getUser());
		args.add(properties.getDatabase());
		Processes.run(setPassword(builder, properties));
	}
	
	private static ProcessBuilder setPassword(final ProcessBuilder builder, final ConnectionProperties properties)
	{
		String password = properties.getPassword();
		if ((password != null) && !PASSWORD_FILE.exists())
		{
			Processes.environment(builder).put("MYSQL_PWD", properties.getPassword());
		}
		return builder;
	}
}
