package com.example.bloom_filter;

import com.example.bloom_filter.service.UserGenerationService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class BloomFilterApplication implements CommandLineRunner {


    private final UserGenerationService userGenerationService;

    public BloomFilterApplication(UserGenerationService userGenerationService) {
        this.userGenerationService = userGenerationService;
    }

    public static void main(String[] args) {
        SpringApplication.run(BloomFilterApplication.class, args);
    }

    @Override
    public void run(String... args) {
//        long total = 10_000_000L; // 10 million
//        int threadCount = 10;
//        long chunkSize = total / threadCount;
//
//        Thread[] threads = new Thread[threadCount];
//
//        for (int i = 0; i < threadCount; i++) {
//            long start = i * chunkSize + 1;
//            long end = (i == threadCount - 1) ? total : (i + 1) * chunkSize;
//
//            int finalI = i;
//            threads[i] = new Thread(() -> {
//                System.out.println("Thread " + finalI + " inserting from " + start + " to " + end);
//                userGenerationService.generateUsers(start, end);
//            });
//            threads[i].start();
//        }
//
//        // Optional: Wait for all threads to finish
//        for (Thread thread : threads) {
//            try {
//                thread.join();
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
//        }
//
//        System.out.println("Data generation completed.");
    }


}
