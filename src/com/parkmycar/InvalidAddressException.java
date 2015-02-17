package com.parkmycar;

public class InvalidAddressException extends RuntimeException
{
	public InvalidAddressException() {
		super();
	}
	public InvalidAddressException(String message)
	{
		super(message);
	}

	
}
