package io.github.balasis.taskmanager.maintenance;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
//TODO: create CICD github pipeline and dockerfile
@SpringBootApplication
public class MaintenanceCore {
    public static void main(String[] args){
        SpringApplication.run(MaintenanceCore.class,args);
    }
}
