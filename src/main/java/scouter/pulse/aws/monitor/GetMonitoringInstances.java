package scouter.pulse.aws.monitor;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.AmazonRDSClientBuilder;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DescribeDBInstancesRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;
import com.amazonaws.services.rds.model.ListTagsForResourceRequest;
import com.amazonaws.services.rds.model.ListTagsForResourceResult;
import com.google.gson.Gson;

import scouter.pulse.aws.monitor.model.AWSProperties;
import scouter.pulse.aws.monitor.model.PulseInstance;
import scouter.pulse.aws.monitor.model.RegisterModel;
import scouter.pulse.aws.monitor.util.AWSCredentialUtil;

public class GetMonitoringInstances implements Runnable {
	
	private static final Logger logger =  LoggerFactory.getLogger(GetMonitoringInstances.class);
	
	private static AWSCredentialsProvider provider;
	
	private static List<PulseInstance> rds = new ArrayList<PulseInstance>();
	private static List<PulseInstance> ec2 = new ArrayList<PulseInstance>();
	private static List<PulseInstance> elb = new ArrayList<PulseInstance>();
	
	private AWSProperties properties;
	private AmazonEC2 ec2Client;
	private AmazonRDS rdsClient;
	private com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancing elbClient;
	private com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancing elbv2Client;
	
	public GetMonitoringInstances(AWSProperties properties) {
		this.properties = properties;
		
		if (provider == null) {
			provider = AWSCredentialUtil.getCredentialProvider(properties.getCredential().getAccesskey(), properties.getCredential().getSecretkey());
			
			// Regist object type & counters only once at start time
			RegisterModel model = new RegisterModel();
			model.getObject().setType("aws");
			model.getObject().setDisplay("AWS");
			
			RegisterModel.Counter counter = new RegisterModel.Counter();
			counter.setName("Cpu");
			counter.setUnit("%");
			counter.setDisplay("CPU");
			model.getCounters().add(counter);
			
			counter = new RegisterModel.Counter();
			counter.setName("Mem");
			counter.setUnit("%");
			counter.setDisplay("Memory");
			model.getCounters().add(counter);
			
			counter = new RegisterModel.Counter();
			counter.setName("RequestCount");
			counter.setUnit("cnt");
			counter.setDisplay("Request Count");
			model.getCounters().add(counter);
			
			try {
				register(model);
			} catch (Exception e) {
	        		logger.error("Unhandled exception occurred while regist an agent.", e);
			}
		}
	}

	public static List<PulseInstance> getRds() {
		return rds;
	}

	public static List<PulseInstance> getEc2() {
		return ec2;
	}

	public static List<PulseInstance> getElb() {
		return elb;
	}

	@Override
	public void run() {
		ec2Client = AmazonEC2ClientBuilder.standard()
				.withRegion(Regions.fromName(properties.getRegion()))
				.withCredentials(provider).build();
		
		rdsClient = AmazonRDSClientBuilder.standard()
				.withRegion(Regions.fromName(properties.getRegion()))
				.withCredentials(provider).build();
		
		elbClient = com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClientBuilder.standard()
				.withRegion(Regions.fromName(properties.getRegion()))
				.withCredentials(provider).build();
		
		elbv2Client = com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingClientBuilder.standard()
				.withRegion(Regions.fromName(properties.getRegion()))
				.withCredentials(provider).build();
		
		try {
			getEC2Instances();
		} catch (Exception e) {
			logger.error("Unhandld exception occurred while get EC2 Instances.", e);
		}

		try {
			getRDSInstances();
		} catch (Exception e) {
			logger.error("Unhandled exception occurred while get RDS Instances.", e);
		}
		
		try {
			getELB();
		} catch (Exception e) {
			logger.error("Unhandled exception occurred while get ELB.", e);
		}
		
		try {
			getELBv2();
		} catch (Exception e) {
			logger.error("Unhandled exception occurred while get ELBv2.", e);
		}
	}

	private void getEC2Instances() {
		DescribeInstancesRequest request = new DescribeInstancesRequest()
				.withFilters(new com.amazonaws.services.ec2.model.Filter().withName("tag:" + properties.getTag()).withValues("TRUE", "True", "true"));
		
		DescribeInstancesResult response = ec2Client.describeInstances(request);
		
		List<PulseInstance> ec2 = new ArrayList<PulseInstance>();
		
		PulseInstance pulse = null;
		for (Reservation resv : response.getReservations()) {
			for (Instance instance : resv.getInstances()) {
				pulse = new PulseInstance();
				pulse.setId(instance.getInstanceId());

				for (com.amazonaws.services.ec2.model.Tag tag : instance.getTags()) {
					if (tag.getKey().equals("Name")) {
						pulse.setName(tag.getValue());
					}
				}
				
				if (instance.getPublicIpAddress() != null) {
					pulse.setAddress(instance.getPublicIpAddress() + " / ");
				}
				
				pulse.setAddress(instance.getPrivateIpAddress());
				
				pulse.setType(instance.getInstanceType());
				
				ec2.add(pulse);
			}
		}
		
		GetMonitoringInstances.ec2 = ec2;
	}
	
	private void getRDSInstances() {
		DescribeDBInstancesResult response = rdsClient.describeDBInstances(new DescribeDBInstancesRequest());
		
		List<PulseInstance> rds = new ArrayList<PulseInstance>();

		PulseInstance pulse = null;
		for (DBInstance instance : response.getDBInstances()) {
			ListTagsForResourceResult result = rdsClient.listTagsForResource(new ListTagsForResourceRequest().withResourceName(instance.getDBInstanceArn()));
			
			for (com.amazonaws.services.rds.model.Tag tag : result.getTagList()) {
				if (tag.getKey().equals(properties.getTag()) && (tag.getValue().equals("TRUE") || tag.getValue().equals("True") || tag.getValue().equals("true"))) {
					pulse = new PulseInstance();
					pulse.setId(instance.getDBInstanceIdentifier());
					pulse.setName(instance.getDBInstanceIdentifier());
					pulse.setAddress(instance.getEndpoint().getAddress());
					pulse.setType(instance.getDBInstanceClass());
					
					rds.add(pulse);
				}
			}
		}
		
		GetMonitoringInstances.rds = rds;
	}

	private void getELB() {
		elb = new ArrayList<PulseInstance>();
		
		com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersResult response = 
				elbClient.describeLoadBalancers(new com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersRequest());
		
		PulseInstance pulse = null;
		for (com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription lb : response.getLoadBalancerDescriptions()) {
			com.amazonaws.services.elasticloadbalancing.model.DescribeTagsResult result = 
					elbClient.describeTags(new com.amazonaws.services.elasticloadbalancing.model.DescribeTagsRequest().withLoadBalancerNames(lb.getLoadBalancerName()));
			
			for (com.amazonaws.services.elasticloadbalancing.model.TagDescription desc : result.getTagDescriptions()) {
				for (com.amazonaws.services.elasticloadbalancing.model.Tag tag : desc.getTags()) {
					if (tag.getKey().equals(properties.getTag()) && (tag.getValue().equals("TRUE") || tag.getValue().equals("True") || tag.getValue().equals("true"))) {
						pulse = new PulseInstance();
						pulse.setId(lb.getLoadBalancerName());
						pulse.setName(lb.getLoadBalancerName());
						pulse.setAddress(lb.getDNSName());
						pulse.setType("standard");
						
						elb.add(pulse);
					}
				}
			}
		}
	}

	private void getELBv2() {
		// Do not initiate elb
		//elb = new ArrayList<PulseInstance>();
		
		com.amazonaws.services.elasticloadbalancingv2.model.DescribeLoadBalancersResult response = 
				elbv2Client.describeLoadBalancers(new com.amazonaws.services.elasticloadbalancingv2.model.DescribeLoadBalancersRequest());
		
		PulseInstance pulse = null;
		for (com.amazonaws.services.elasticloadbalancingv2.model.LoadBalancer lb : response.getLoadBalancers()) {
			com.amazonaws.services.elasticloadbalancingv2.model.DescribeTagsResult result = 
					elbv2Client.describeTags(new com.amazonaws.services.elasticloadbalancingv2.model.DescribeTagsRequest().withResourceArns(lb.getLoadBalancerArn()));
			
			for (com.amazonaws.services.elasticloadbalancingv2.model.TagDescription desc : result.getTagDescriptions()) {
				for (com.amazonaws.services.elasticloadbalancingv2.model.Tag tag : desc.getTags()) {
					if (tag.getKey().equals(properties.getTag()) && (tag.getValue().equals("TRUE") || tag.getValue().equals("True") || tag.getValue().equals("true"))) {
						pulse = new PulseInstance();
						
						String arn = lb.getLoadBalancerArn();
						arn = arn.substring(arn.indexOf("app/"));
						
						pulse.setId(arn);
						pulse.setName(lb.getLoadBalancerName());
						pulse.setAddress(lb.getDNSName());
						pulse.setType(lb.getType());
						
						elb.add(pulse);
					}
				}
			}
		}
	}
	
	private void register(RegisterModel model) throws Exception {
		String url = "http://" + properties.getScouter().getHost() + ":" + properties.getScouter().getPort() + "/register";
		
        String param = new Gson().toJson(model);

        HttpPost post = new HttpPost(url);
        post.addHeader("Content-Type","application/json");
        post.setEntity(new StringEntity(param));
      
        CloseableHttpClient client = HttpClientBuilder.create().build();
      
        // send the post request
        HttpResponse response = client.execute(post);
        
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK || response.getStatusLine().getStatusCode() == HttpStatus.SC_CREATED) {
        		logger.info("Register message sent to [{}] for [{}].", url, model.getObject().getDisplay());
        } else {
	        	logger.warn("Register message sent failed. Verify below information.");
	        	logger.warn("[URL] : " + url);
	        	logger.warn("[Message] : " + param);
	        	logger.warn("[Reason] : " + EntityUtils.toString(response.getEntity(), "UTF-8"));
        }
	}
}
