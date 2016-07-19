package org.sqsh.exttest;

import org.sqsh.CannotSetValueError;
import org.sqsh.Variable;

public class FooVar extends Variable {
	
	private int foo = 0;
	
	public FooVar() {
		
	}
	
	@Override
	public String setValue(String value) throws CannotSetValueError {
		
		String oldValue = Integer.toString(foo);
		try {
			
			foo = Integer.parseInt(value);
		}
		catch (NumberFormatException e) {
			
			throw new CannotSetValueError(e.getMessage());
		}
		
		return oldValue;
	}


	@Override
	public String toString() {
		
		return Integer.toString(foo);
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	public int getFoo() {

		return foo;
	}

	public void setFoo(int foo) {

		this.foo = foo;
	}
}
