package org.hajdbc.management;

import org.hajdbc.Database;

public interface MBeanRegistrarFactory
{
	<Z, D extends Database<Z>> MBeanRegistrar<Z, D> createMBeanRegistrar();
}
