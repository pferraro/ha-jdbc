package io.github.hajdbc.management;

import java.lang.management.ManagementFactory;

import io.github.hajdbc.Database;

public class DefaultMBeanRegistrarFactory implements MBeanRegistrarFactory
{
	@Override
	public <Z, D extends Database<Z>> MBeanRegistrar<Z, D> createMBeanRegistrar()
	{
		return new DefaultMBeanRegistrar<>(ManagementFactory.getPlatformMBeanServer());
	}
}
