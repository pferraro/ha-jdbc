package org.hajdbc.sql;

import org.hajdbc.Database;
import org.hajdbc.DatabaseCluster;

public interface ProxyContext<Z, D extends Database<Z>>
{
	DatabaseCluster<Z, D> getDatabaseCluster();
}
