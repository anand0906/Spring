package com.anand;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class Main {
	
	public static void main(String[] args) {
		
		ApplicationContext context=new AnnotationConfigApplicationContext(JavaConfig.class);
		
		Phone phone=context.getBean(Phone.class);
		
		Jio jio1=(Jio)context.getBean("jio");
		Jio jio2=(Jio)context.getBean("jio");
		System.out.print("Prototype : ");
		System.out.println(jio1==jio2);
		
		Airtel airtel1=(Airtel)context.getBean("airtel");
		Airtel airtel2=(Airtel)context.getBean("airtel");
		System.out.print("Singleton : ");
		System.out.println(airtel1==airtel2);
		
		phone.call();
		
		((ConfigurableApplicationContext)context).close();
	}

}
