package io.github.hajdbc.management;

import io.github.hajdbc.Database;

public interface MBeanRegistrarFactory
{
	<Z, D extends Database<Z>> MBeanRegistrar<Z, D> createMBeanRegistrar();
}
