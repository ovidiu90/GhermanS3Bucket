package com.practice.Gherman.controller;

import com.practice.Gherman.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/s3")
public class AwsS3Controller {

    private final S3Service s3Service;

    @GetMapping("/buckets")
    public void printBuckets() {
        s3Service.printBuckets();
    }

    //S3 bucket contains "documents" directory
    @GetMapping("/buckets/{directoryName}")
    public void printDirectory(@PathVariable String directoryName) {
        s3Service.printResultByDirectoryName(directoryName);
    }

    @GetMapping("/buckets/random/{directoryName}")
    public void downloadFirstFile(@PathVariable String directoryName) {
        s3Service.downloadRandomFile(directoryName);
    }
}
