package scouter.pulse.aws.monitor.util;

import org.springframework.util.StringUtils;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;

public class AWSCredentialUtil {

	public static AWSCredentialsProvider getCredentialProvider(String accessKey, String secretKey) {
		AWSCredentialsProvider provider;
		
		if (!StringUtils.isEmpty(accessKey) && !StringUtils.isEmpty(secretKey)) {
			provider = new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey));
		} else {
			provider = new DefaultAWSCredentialsProviderChain();
		}
		
		return provider;
	}
}