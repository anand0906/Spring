package com.anand.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Component
@Aspect
public class AroundAOP {
	
	@Around("execution(* com.anand.aop.Division.divide(..))")
	public Object checkDenominator(ProceedingJoinPoint joinPoint) throws Throwable {
		System.out.println("Before Executing Divide !");
		Object[] args=joinPoint.getArgs();
		int a=(int)args[0];
		int b=(int)args[1];
		System.out.println("Arguments are : "+a+","+b);
		if(b==0) {
			System.out.println("Can't Call Divide Method Since Denominator is zero");
			return Double.NaN;
		}
		Object temp=joinPoint.proceed();
		System.out.println("After Executing Divide !");
		return temp;
	}

}
