package io.github.hajdbc.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.util.Map;

import io.github.hajdbc.logging.Level;
import io.github.hajdbc.logging.Logger;
import io.github.hajdbc.logging.LoggerFactory;
import io.github.hajdbc.messages.Messages;
import io.github.hajdbc.messages.MessagesFactory;

public class Processes
{
	private static final Messages messages = MessagesFactory.getMessages();
	private static final Logger logger = LoggerFactory.getLogger(Processes.class);

	public static Map<String, String> environment(final ProcessBuilder builder)
	{
		PrivilegedAction<Map<String, String>> action = new PrivilegedAction<Map<String, String>>()
		{
			@Override
			public Map<String, String> run()
			{
				return builder.environment();
			}
		};
		return AccessController.doPrivileged(action);
	}
	
	public static void run(final ProcessBuilder processBuilder) throws Exception
	{
		processBuilder.redirectErrorStream(true);
		
		logger.log(Level.DEBUG, Strings.join(processBuilder.command(), " "));
		
		PrivilegedExceptionAction<Process> action = new PrivilegedExceptionAction<Process>()
		{
			@Override
			public Process run() throws Exception
			{
				return processBuilder.start();
			}
		};
		
		Process process = AccessController.doPrivileged(action);
		
		try
		{
			int status = process.waitFor();
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line = reader.readLine();
			while (line != null)
			{
				logger.log(Level.DEBUG, line);
				line = reader.readLine();
			}
			
			if (status != 0)
			{
				throw new Exception(messages.status(processBuilder, status));
			}
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
			
			throw new Exception(processBuilder.toString(), e);
		}
	}

	private Processes()
	{
		// Hide
	}
}
