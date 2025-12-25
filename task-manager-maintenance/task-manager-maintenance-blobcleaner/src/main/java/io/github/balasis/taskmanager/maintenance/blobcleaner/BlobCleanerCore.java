package io.github.balasis.taskmanager.maintenance.blobcleaner;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BlobCleanerCore {
    public static void main(String[] args){
        SpringApplication.run(BlobCleanerCore.class,args);
    }
}
