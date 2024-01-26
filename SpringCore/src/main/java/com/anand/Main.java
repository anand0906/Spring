package com.anand;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.anand.aop.Division;

@SpringBootApplication
public class Main {
	
	public static void main(String[] args) {
		
		ApplicationContext context=new AnnotationConfigApplicationContext(JavaConfig.class);
		
//		Phone phone=context.getBean(Phone.class);
//		
//		Jio jio1=(Jio)context.getBean("jio");
//		Jio jio2=(Jio)context.getBean("jio");
//		System.out.print("Prototype : ");
//		System.out.println(jio1==jio2);
//		
//		Airtel airtel1=(Airtel)context.getBean("airtel");
//		Airtel airtel2=(Airtel)context.getBean("airtel");
//		System.out.print("Singleton : ");
//		System.out.println(airtel1==airtel2);
//		
//		phone.call();
		
		Division division=context.getBean(Division.class);
		division.divide(1, 0);
		division.divide(2, 1);
		
		((ConfigurableApplicationContext)context).close();
	}

}
