package com.practice.Gherman.service;

import com.practice.Gherman.model.ListResult;
import com.practice.Gherman.model.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@Service
public class S3Service {

    private final S3Client s3Client;
    private final String bucketName;

    public S3Service(S3Client s3Client, @Value("${aws.s3.bucket-name}") String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    public void printBuckets() {
        s3Client.listBuckets().buckets().forEach(bucket -> {
            System.out.println("Bucket Name: " + bucket.name());
            System.out.println(bucket.creationDate());
        });
    }

    public void printResultByDirectoryName(String directoryName) {
        Resource directory = new Resource(directoryName, directoryName, 1);
        ListResult<Resource> resourceListResult = listFolder(directory, null);

        System.out.println("Resources in directory " + directoryName + ":");
        resourceListResult.getResources().forEach(resource -> {
            System.out.println("Type : " + resource.type() + " and id: " + resource.id());
        });
    }

    public File downloadRandomFile(String directoryName) {
        Resource directory = new Resource(directoryName, directoryName, 1);
        ListResult<Resource> resourceListResult = listFolder(directory, null);

        Resource firstResource = resourceListResult.getResources().get(1);
        return getAsFile(firstResource);
    }

    private ListResult<Resource> listFolder(Resource parent, String cursor) {
        String prefix = parent != null ? parent.id() + "/" : "";
        ListObjectsV2Request.Builder requestBuilder = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix(prefix)
                .continuationToken(cursor);

        ListObjectsV2Response response = s3Client.listObjectsV2(requestBuilder.build());

        List<Resource> resources = new ArrayList<>();
        for (S3Object s3Object : response.contents()) {
            String key = s3Object.key();
            String name = key.substring(key.lastIndexOf("/") + 1);
            int type = key.endsWith("/") ? 1 : 0;
            resources.add(new Resource(key, name, type));
        }

        return new ListResult<>(resources, response.nextContinuationToken());
    }

    private Resource getResource(String id) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Resource ID cannot be null or empty");
        }

        HeadObjectRequest request = HeadObjectRequest.builder()
                .bucket(bucketName)
                .key(id)
                .build();

        try {
            s3Client.headObject(request);
            String name = id.substring(id.lastIndexOf("/") + 1);
            int type = id.endsWith("/") ? 1 : 0;
            return new Resource(id, name, type);
        } catch (NoSuchKeyException e) {
            throw new IllegalArgumentException("Resource not found for ID: " + id);
        }
    }

    private File getAsFile(Resource resource) {
        if (resource == null) {
            throw new IllegalArgumentException("Resource cannot be null");
        }

        String key = resource.id();
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        try {
            ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(request);
            File tempFile = Files.createTempFile("s3-", resource.name()).toFile();
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(response.asByteArray());
            }
            System.out.println("Downloaded file to: " + tempFile.getAbsolutePath());
            return tempFile;
        } catch (S3Exception | IOException e) {
            throw new RuntimeException("Failed to download resource as file: " + resource.name(), e);
        }
    }
}
