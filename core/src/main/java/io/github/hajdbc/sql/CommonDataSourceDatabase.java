package io.github.hajdbc.sql;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Properties;

import javax.sql.CommonDataSource;

import io.github.hajdbc.Credentials;
import io.github.hajdbc.Locality;

public abstract class CommonDataSourceDatabase<Z extends CommonDataSource> extends AbstractDatabase<Z>
{
	protected CommonDataSourceDatabase(String id, Z connectionSource, Credentials credentials, int weight, Locality locality)
	{
		super(id, connectionSource, credentials, weight, locality);
	}

	@Override
	public String getLocation()
	{
		return this.getConnectionSource().getClass().getName();
	}

	@Override
	public Properties getProperties()
	{
		Properties properties = new Properties();
		Z dataSource = this.getConnectionSource();
		try
		{
			for (PropertyDescriptor descriptor: Introspector.getBeanInfo(dataSource.getClass()).getPropertyDescriptors())
			{
				Method method = descriptor.getReadMethod();
				if ((method != null) && (descriptor.getWriteMethod() != null))
				{
					try
					{
						Object value = method.invoke(dataSource);
						if (value != null)
						{
							PropertyEditor editor = PropertyEditorManager.findEditor(descriptor.getPropertyType());
							if (editor != null)
							{
								editor.setValue(value);
								properties.setProperty(descriptor.getName(), editor.getAsText());
							}
						}
					}
					catch (IllegalAccessException | InvocationTargetException e)
					{
						// Ignore
					}
				}
			}
		}
		catch (IntrospectionException e)
		{
			throw new IllegalStateException(e);
		}
		return properties;
	}
}
