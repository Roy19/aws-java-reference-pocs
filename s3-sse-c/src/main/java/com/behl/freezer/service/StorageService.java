package com.behl.freezer.service;

import java.io.IOException;
import java.time.LocalDateTime;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.SSECustomerKey;
import com.behl.freezer.properties.AwsS3ConfigurationProperties;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Hardik Singh Behl
 */

@Service
@AllArgsConstructor
@EnableConfigurationProperties(value = AwsS3ConfigurationProperties.class)
@Slf4j
public class StorageService {

	private final AmazonS3 amazonS3;
	private final SSECustomerKey sseCustomerKey;
	private final AwsS3ConfigurationProperties awsS3ConfigurationProperties;

	/**
	 * 
	 * @param file: represents an object to be saved in configured S3 Bucket
	 * @return HttpStatus 200 OK if file was saved, HttpStatus 417
	 *         EXPECTATION_FAILED if file was not saved.
	 */
	public HttpStatus save(final MultipartFile file) {
		ObjectMetadata metadata = constructMetadata(file);

		try {
			final var putObjectRequest = new PutObjectRequest(awsS3ConfigurationProperties.getS3().getBucketName(),
					file.getOriginalFilename(), file.getInputStream(), metadata);
			putObjectRequest.setSSECustomerKey(sseCustomerKey);

			amazonS3.putObject(putObjectRequest);
		} catch (final SdkClientException | IOException exception) {
			log.error("UNABLE TO STORE {} IN S3: {} ", file.getOriginalFilename(), LocalDateTime.now(), exception);
			return HttpStatus.EXPECTATION_FAILED;
		}
		return HttpStatus.OK;
	}

	/**
	 * Method to object from configured S3 bucket
	 * 
	 * @param key of object
	 * @return S3Object representing the transcribed result
	 */
	public S3Object retrieve(final String objectKey) {
		try {
			final var getObjectRequest = new GetObjectRequest(awsS3ConfigurationProperties.getS3().getBucketName(),
					objectKey);
			getObjectRequest.setSSECustomerKey(sseCustomerKey);
			return amazonS3.getObject(getObjectRequest);
		} catch (final AmazonS3Exception exception) {
			log.error("Unable to retreive object {} from configured S3 Bucket {}", objectKey,
					awsS3ConfigurationProperties.getS3().getBucketName(), exception);
			if (exception.getMessage().contains("Access Denied"))
				throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
			else if (exception.getMessage().contains("The specified key does not exist"))
				throw new ResponseStatusException(HttpStatus.NOT_FOUND);
			else
				throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED);
		}
	}

	private ObjectMetadata constructMetadata(final MultipartFile file) {
		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentLength(file.getSize());
		metadata.setContentType(file.getContentType());
		metadata.setContentDisposition(file.getOriginalFilename());
		return metadata;
	}

}
