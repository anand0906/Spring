package com.anand;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class Phone {
	
//	@Autowired
//	@Qualifier("jio")
	Sim sim;
	
	
	public Phone(@Qualifier("jio") Sim sim) {
		super();
		this.sim = sim;
	}




	void call() {
		sim.makeCall();
	}

}
