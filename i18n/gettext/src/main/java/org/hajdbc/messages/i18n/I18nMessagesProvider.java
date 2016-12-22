/*
 * HA-JDBC: High-Availability JDBC
 * Copyright (C) 2014  Paul Ferraro
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
package org.hajdbc.messages.i18n;

import java.util.ResourceBundle;

import org.hajdbc.messages.Messages;
import org.hajdbc.messages.MessagesProvider;
import org.kohsuke.MetaInfServices;

@MetaInfServices(MessagesProvider.class)
public class I18nMessagesProvider implements MessagesProvider
{
	@Override
	public boolean isEnabled()
	{
		try
		{
			// Make sure gettext-commons is on classpath
			this.getClass().getClassLoader().loadClass("org.xnap.commons.i18n.I18nFactory");
			// Ensure requisite resource bundle exists
			ResourceBundle.getBundle(org.hajdbc.messages.i18n.I18nMessages.class.getName());
			return true;
		}
		catch (Throwable e)
		{
			return false;
		}
	}

	@Override
	public Messages getMessages()
	{
		return new I18nMessages(org.hajdbc.messages.i18n.I18nMessages.class.getName());
	}

	@Override
	public String getName()
	{
		return "gettext";
	}
}
