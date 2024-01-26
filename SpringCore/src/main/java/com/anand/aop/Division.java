package com.anand.aop;

import org.springframework.stereotype.Component;

@Component
public class Division {
	
	public int divide(int numerator,int denominator) {
		return numerator/denominator;
	}

}
