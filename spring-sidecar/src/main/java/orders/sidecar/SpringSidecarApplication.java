package orders.sidecar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.sidecar.EnableSidecar;
 
@SpringBootApplication
@EnableSidecar
public class SpringSidecarApplication {
 
    public static void main(String[] args) {
        SpringApplication.run(SpringSidecarApplication.class, args);
    }
}
