package application.rest;


import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.health.Health;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;

import utils.JDBCConnection;

@Health
@ApplicationScoped

public class HealthEndpoint implements HealthCheck {
	
    public boolean isOrdersDbReady(){
	   //Checking if Orders database is UP
		
        JDBCConnection jdbcConnection = new JDBCConnection();
		
		java.sql.Connection connection = jdbcConnection.getConnection();
		
		if(connection!=null) 
			return true;
		else
		return false;
	}

	public boolean isRabbitMQReady() {
		
		//Checking if RabbitMQ is UP

		return true;

	}
	
    public boolean isAuthReady() {
		
		//Checking if Auth service is UP

		return true;

	}
    
    public boolean isInventoryReady() {
		
		//Checking if Inventory service is UP

		return true;

	}


	@Override
	public HealthCheckResponse call() {
		// TODO Auto-generated method stub
		if (!isOrdersDbReady()) {
		      return HealthCheckResponse.named(OrderService.class.getSimpleName())
		                                .withData("Inventory Database", "DOWN").down()
		                                .build();
		    }


		if (!isRabbitMQReady()) {
			  return HealthCheckResponse.named(OrderService.class.getSimpleName())
			                             .withData("RabbitMQ", "DOWN").down()
			                             .build();
			    }
		
		if (!isAuthReady()) {
		      return HealthCheckResponse.named(OrderService.class.getSimpleName())
		                                .withData("Auth Service", "DOWN").down()
		                                .build();
		    }
		
		if (!isInventoryReady()) {
		      return HealthCheckResponse.named(OrderService.class.getSimpleName())
		                                .withData("Inventory Service", "DOWN").down()
		                                .build();
		    }
		
		return HealthCheckResponse.named(OrderService.class.getSimpleName()).withData("Order Service", "UP").up().build();
	}

}
