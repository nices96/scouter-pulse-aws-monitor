package scouter.pulse.aws.monitor.model;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConfigurationProperties("aws")
public class AWSProperties {
	
	private Scouter scouter;
	private String region;
	private Credential credential;
	private String tag;
	private Integer period;

	public Scouter getScouter() {
		return scouter;
	}

	public void setScouter(Scouter scouter) {
		this.scouter = scouter;
	}

	public String getRegion() {
		return region;
	}

	public void setRegion(String region) {
		if (!StringUtils.isEmpty(System.getProperty("aws.region"))) {
			region = System.getProperty("aws.region");
		}
		
		this.region = region;
	}

	public Credential getCredential() {
		return credential;
	}

	public void setCredential(Credential credential) {
		this.credential = credential;
	}

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public Integer getPeriod() {
		return period;
	}

	public void setPeriod(Integer period) {
		if (!StringUtils.isEmpty(System.getProperty("aws.period"))) {
			period = Integer.parseInt(System.getProperty("aws.period"));
		}
		
		this.period = period;
	}
	
	public static class Scouter {
		private String host;
		private String port;
		
		public String getHost() {
			return host;
		}
		
		public void setHost(String host) {
			if (!StringUtils.isEmpty(System.getProperty("aws.scouter.host"))) {
				host = System.getProperty("aws.scouter.host");
			}
			
			this.host = host;
		}
		
		public String getPort() {
			return port;
		}
		
		public void setPort(String port) {
			if (!StringUtils.isEmpty(System.getProperty("aws.scouter.port"))) {
				port = System.getProperty("aws.scouter.port");
			}
			
			this.port = port;
		}
	}
	
	public static class Credential {
		private String accesskey;
		private String secretkey;
		
		public String getAccesskey() {
			return accesskey;
		}

		public void setAccesskey(String accesskey) {
			this.accesskey = accesskey;
		}

		public String getSecretkey() {
			return secretkey;
		}

		public void setSecretkey(String secretkey) {
			this.secretkey = secretkey;
		}
	}
}
