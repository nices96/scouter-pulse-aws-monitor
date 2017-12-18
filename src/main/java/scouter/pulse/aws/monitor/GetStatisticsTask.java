package scouter.pulse.aws.monitor;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
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
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;
import com.google.gson.Gson;

import scouter.pulse.aws.monitor.model.AWSProperties;
import scouter.pulse.aws.monitor.model.CounterModel;
import scouter.pulse.aws.monitor.model.PulseInstance;
import scouter.pulse.aws.monitor.util.AWSCredentialUtil;
import scouter.pulse.aws.monitor.util.InstanceTypeUtil;

public class GetStatisticsTask implements Runnable {
	
	private static final Logger logger =  LoggerFactory.getLogger(GetStatisticsTask.class);
	
	private static AWSCredentialsProvider provider;
	
	private PulseInstance pulse;
	private String namespace;
	private AWSProperties properties;
	
	private AmazonCloudWatchClient cloudWatch;
	
	public GetStatisticsTask(PulseInstance pulse, String namespace, AWSProperties properties) {
		this.pulse = pulse;
		this.namespace = namespace;
		this.properties = properties;
		
		if (provider == null) {
			provider = AWSCredentialUtil.getCredentialProvider(properties.getCredential().getAccesskey(), properties.getCredential().getSecretkey());
		}
		
		cloudWatch = new AmazonCloudWatchClient(provider);
	    cloudWatch.setEndpoint("monitoring." + properties.getRegion() + ".amazonaws.com");
	}

	@Override
	public void run() {
		if (namespace.contains("EC2") || namespace.contains("RDS")) {
			try {
				GetMetricStatisticsResult result = getCpuUtilization(pulse.getId());
				
				int len = result.getDatapoints().size();
				
				if (len > 0) {
					Datapoint data = result.getDatapoints().get(len - 1);
					
					CounterModel model = new CounterModel();
					
					if (namespace.endsWith("EC2")) {
						model.getObject().setHost("AWS/EC2");
				    } else {
				    	model.getObject().setHost("AWS/RDS");
				    }
					
					model.getObject().setName(pulse.getName());
					model.getObject().setType("aws");
					model.getObject().setAddress(pulse.getAddress());
					
					CounterModel.Counter counter = new CounterModel.Counter();
					counter.setName("Cpu");
					counter.setValue(data.getAverage());
					model.getCounters().add(counter);
					
					counter(model);
				}
			} catch (Exception e) {
				logger.error("Unhandled exception occurred while get CPU utilization.", e);
			}
		}

		if (namespace.contains("EC2") || namespace.contains("RDS")) {
			try {
				GetMetricStatisticsResult result = getMemoryUtilization(pulse.getId());
				
				int len = result.getDatapoints().size();
				
				if (len > 0) {
					Datapoint data = result.getDatapoints().get(len - 1);
					
					CounterModel model = new CounterModel();
					
					if (namespace.endsWith("EC2")) {
						model.getObject().setHost("AWS/EC2");
				    } else {
				    		model.getObject().setHost("AWS/RDS");
				    }
					
					model.getObject().setName(pulse.getName());
					model.getObject().setType("aws");
					model.getObject().setAddress(pulse.getAddress());
					
					CounterModel.Counter counter = new CounterModel.Counter();
					counter.setName("Mem");
					
					if (namespace.endsWith("EC2")) {
						counter.setValue(data.getAverage());
					} else {
						counter.setValue(InstanceTypeUtil.getMemoryUtilization(pulse.getType(), data.getAverage()));
					}
					model.getCounters().add(counter);
					
					counter(model);
				}
			} catch (Exception e) {
				logger.error("Unhandled exception occurred while get Memory utilization.", e);
			}
		}

		if (namespace.contains("ELB")) {
			try {
				GetMetricStatisticsResult result = getRequestCount(pulse.getId());
				
				int len = result.getDatapoints().size();
				
				if (len > 0) {
					Datapoint data = result.getDatapoints().get(len - 1);
					
					CounterModel model = new CounterModel();
					model.getObject().setHost("AWS/ELB");
					model.getObject().setName(pulse.getName());
					model.getObject().setType("aws");
					model.getObject().setAddress(pulse.getAddress());
					
					CounterModel.Counter counter = new CounterModel.Counter();
					counter.setName("RequestCount");
					counter.setValue(data.getSum());
					model.getCounters().add(counter);
					
					counter(model);
				}
			} catch (Exception e) {
				logger.error("Unhandled exception occurred while get Request Count.", e);
			}
		}
	}
	
	public GetMetricStatisticsResult getCpuUtilization(String name) {
	    Dimension instanceDimension = new Dimension();
	    
	    if (namespace.endsWith("EC2")) {
		    instanceDimension.setName("InstanceId");
	    } else {
		    instanceDimension.setName("DBInstanceIdentifier");
	    }
	    instanceDimension.setValue(name);
	    
		Calendar startCal = Calendar.getInstance();
		Calendar endCal = Calendar.getInstance();
		
		startCal.add(Calendar.MINUTE, -10);
		startCal.set(Calendar.SECOND, 0);

	    GetMetricStatisticsRequest request = new GetMetricStatisticsRequest()
	    		.withStartTime(startCal.getTime())
	            .withEndTime(endCal.getTime())
	            .withNamespace(namespace)
	            .withPeriod(60)
	            .withMetricName("CPUUtilization")
	            .withStatistics("Average")
	            .withStatistics("Maximum")
	            .withDimensions(Arrays.asList(instanceDimension));

	    GetMetricStatisticsResult result = sort(cloudWatch.getMetricStatistics(request).getDatapoints());
	    
	    if (logger.isDebugEnabled()) {
		    if (result.getDatapoints() != null && result.getDatapoints().size() > 0) {
			    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			    logger.debug("[{}:{}] {}, {} ~ {}", new Object[] {namespace, pulse.getId(), result, sdf.format(startCal.getTime()), sdf.format(endCal.getTime())});
		    }
	    }
	    
	    return result;
	}
	
	public GetMetricStatisticsResult getMemoryUtilization(String name) {
	    Dimension instanceDimension = new Dimension();
	    
	    String ns = null;
		String metric = null;
	    if (namespace.endsWith("EC2")) {
	    		ns = "System/Linux";
		    instanceDimension.setName("InstanceId");
		    metric = "MemoryUtilization";
	    } else {
	    		ns = namespace;
		    instanceDimension.setName("DBInstanceIdentifier");
		    metric = "FreeableMemory";
	    }
	    instanceDimension.setValue(name);
	    
		Calendar startCal = Calendar.getInstance();
		Calendar endCal = Calendar.getInstance();
		
		startCal.add(Calendar.MINUTE, -10);
		startCal.set(Calendar.SECOND, 0);

	    GetMetricStatisticsRequest request = new GetMetricStatisticsRequest()
	    		.withStartTime(startCal.getTime())
	            .withEndTime(endCal.getTime())
	            .withNamespace(ns)
	            .withPeriod(60)
	            .withMetricName(metric)
	            .withStatistics("Average")
	            .withStatistics("Maximum")
	            .withDimensions(Arrays.asList(instanceDimension));

	    GetMetricStatisticsResult result = sort(cloudWatch.getMetricStatistics(request).getDatapoints());
	    
	    if (logger.isDebugEnabled()) {
		    if (result.getDatapoints() != null && result.getDatapoints().size() > 0) {
			    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			    logger.debug("[{}:{}] {}, {} ~ {}", new Object[] {namespace, pulse.getId(), result, sdf.format(startCal.getTime()), sdf.format(endCal.getTime())});
		    }
	    }
	    
	    return result;
	}
	
	public GetMetricStatisticsResult getRequestCount(String name) {
	    Dimension instanceDimension = new Dimension();
	    
	    String ns = null;
	    if (pulse.getType().equals("application")) {
	    		ns = "AWS/ApplicationELB";
		    instanceDimension.setName("LoadBalancer");
	    } else {
	    		ns = "AWS/ELB";
		    instanceDimension.setName("LoadBalancerName");
	    }
	    
	    instanceDimension.setValue(name);
	    
		Calendar startCal = Calendar.getInstance();
		Calendar endCal = Calendar.getInstance();
		
		startCal.add(Calendar.MINUTE, -10);
		startCal.set(Calendar.SECOND, 0);

	    GetMetricStatisticsRequest request = new GetMetricStatisticsRequest()
	    		.withStartTime(startCal.getTime())
	            .withEndTime(endCal.getTime())
	            .withNamespace(ns)
	            .withPeriod(60)
	            .withMetricName("RequestCount")
	            .withStatistics("Sum")
	            .withDimensions(Arrays.asList(instanceDimension));
	    
	    GetMetricStatisticsResult result = sort(cloudWatch.getMetricStatistics(request).getDatapoints());
	    
	    if (logger.isDebugEnabled()) {
		    if (result.getDatapoints() != null && result.getDatapoints().size() > 0) {
			    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			    logger.debug("[{}:{}] {}, {} ~ {}", new Object[] {namespace, pulse.getId(), result, sdf.format(startCal.getTime()), sdf.format(endCal.getTime())});
		    }
	    }
	    
	    return result;
	}
	
	private GetMetricStatisticsResult sort(List<Datapoint> resultSet) {
		Collections.sort(resultSet, new Comparator<Datapoint>() {
			@Override
			public int compare(Datapoint o1, Datapoint o2) {
				return o1.getTimestamp().compareTo(o2.getTimestamp());
			}
		});
		
		return new GetMetricStatisticsResult().withDatapoints(resultSet);
	}
	
	private void counter(CounterModel model) throws Exception {
		String url = "http://" + properties.getScouter().getHost() + ":" + properties.getScouter().getPort() + "/counter";
		
        String param = new Gson().toJson(model);

        HttpPost post = new HttpPost(url);
        post.addHeader("Content-Type","application/json");
        post.setEntity(new StringEntity(param));
      
        CloseableHttpClient client = HttpClientBuilder.create().build();
      
        // send the post request
        HttpResponse response = client.execute(post);
        
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK || response.getStatusLine().getStatusCode() == HttpStatus.SC_CREATED) {
        		logger.debug("Counter message sent to [{}] successfully.", url);
        } else {
	        	logger.warn("Counter message sent failed. Verify below information.");
	        	logger.warn("[URL] : " + url);
	        	logger.warn("[Message] : " + param);
	        	logger.warn("[Reason] : " + EntityUtils.toString(response.getEntity(), "UTF-8"));
        }
	}

}
