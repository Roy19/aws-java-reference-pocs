package com.behl.transcribe.service;

import java.io.IOException;

import org.joda.time.LocalDateTime;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.behl.transcribe.properties.AwsS3ConfigurationProperties;

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
	private final AwsS3ConfigurationProperties awsS3ConfigurationProperties;

	/**
	 * 
	 * @param file: represents an object to be saved in configured S3 Bucket
	 * @return HttpStatus 200 OK if file was saved, HttpStatus 417
	 *         EXPECTATION_FAILED if file was not saved.
	 */
	public HttpStatus save(final MultipartFile file) {
		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentLength(file.getSize());
		metadata.setContentType(file.getContentType());
		metadata.setContentDisposition(file.getOriginalFilename());

		try {
			amazonS3.putObject(awsS3ConfigurationProperties.getS3().getInputBucketName(), file.getOriginalFilename(),
					file.getInputStream(), metadata);
		} catch (SdkClientException | IOException e) {
			log.error("UNABLE TO STORE {} IN S3: {} ", file.getOriginalFilename(), LocalDateTime.now());
			return HttpStatus.EXPECTATION_FAILED;
		}
		return HttpStatus.OK;
	}

	/**
	 * Method to retrieve transcribed JSON result from configured output bucket
	 * 
	 * @param name of StartTranscriptionJobRequest
	 * @return S3Object representing the transcribed result
	 */
	public S3Object retrieve(final String jobName) {
		try {
			return amazonS3.getObject(awsS3ConfigurationProperties.getS3().getOutputBucketName(), jobName + ".json");
		} catch (AmazonS3Exception e) {
			return null;
		}
	}

	/**
	 * Method to delete an object from the configured S3 Bucket
	 * 
	 * @param file to be deleted from the configured S3 Bucket after transcribe
	 *             processing is completed
	 */

	public void delete(final MultipartFile file) {
		amazonS3.deleteObject(awsS3ConfigurationProperties.getS3().getInputBucketName(), file.getOriginalFilename());
	}

	/**
	 * Method to delete JSON result object from the configured output S3 Bucket
	 * 
	 * @param name of StartTranscriptionJobRequest
	 */
	public void delete(final String jobName) {
		amazonS3.deleteObject(awsS3ConfigurationProperties.getS3().getOutputBucketName(), jobName + ".json");
	}

}