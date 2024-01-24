package com.anand;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class Jio implements Sim {

	@Override
	public void makeCall() {
		System.out.println("Calling Using Jio Sim !");
	}

}
