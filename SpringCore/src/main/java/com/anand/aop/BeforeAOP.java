package com.anand.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Component
@Aspect
public class BeforeAOP {
	
	@Before("execution(* com.anand.aop.Division.divide(..))")
	public void beforeDivison(JoinPoint joinPoint) {
		System.out.println("Calling Before Division "+joinPoint.getSignature());
	}

}
