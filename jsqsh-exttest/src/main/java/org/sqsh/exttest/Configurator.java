package org.sqsh.exttest;

import org.sqsh.ExtensionConfigurator;
import org.sqsh.SqshContext;

public class Configurator extends ExtensionConfigurator {

	@Override
	public void configure(SqshContext context) {
		
		context.setVariable("exttest_configured", "true");
	}
}
