package io.github.hajdbc.sql;

import io.github.hajdbc.Database;
import io.github.hajdbc.DatabaseCluster;

public interface ProxyContext<Z, D extends Database<Z>>
{
	DatabaseCluster<Z, D> getDatabaseCluster();
}
