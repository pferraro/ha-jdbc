package io.github.hajdbc.durability;

import io.github.hajdbc.ExceptionType;

public interface DurabilityEventFactory
{
	DurabilityEvent createEvent(Object transactionId, Durability.Phase phase);

	InvocationEvent createInvocationEvent(Object transactionId, Durability.Phase phase, ExceptionType exceptionType);

	InvokerEvent createInvokerEvent(Object transactionId, Durability.Phase phase, String databaseId);
}
