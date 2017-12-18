package scouter.pulse.aws.monitor.util;

import java.util.HashMap;
import java.util.Map;

public class InstanceTypeUtil {

	private static Map<String, Double> memoryMap;
	
	static {
		memoryMap = new HashMap<String, Double>();
		
		memoryMap.put("db.m4.large", (double) (8.0 * 1024 * 1024 * 1024));
		memoryMap.put("db.m4.xlarge", (double) (16.0 * 1024 * 1024 * 1024));
		memoryMap.put("db.m4.2xlarge", (double) (32.0 * 1024 * 1024 * 1024));
		memoryMap.put("db.m4.4xlarge", (double) (64.0 * 1024 * 1024 * 1024));
		memoryMap.put("db.m4.10xlarge", (double) (160.0 * 1024 * 1024 * 1024));
		memoryMap.put("db.m4.16xlarge", (double) (256.0 * 1024 * 1024 * 1024));
		
		memoryMap.put("db.m3.medium", (double) (3.75 * 1024 * 1024 * 1024));
		memoryMap.put("db.m3.large", (double) (7.5 * 1024 * 1024 * 1024));
		memoryMap.put("db.m3.xlarge", (double) (15.0 * 1024 * 1024 * 1024));
		memoryMap.put("db.m3.2xlarge", (double) (30.0 * 1024 * 1024 * 1024));
		
		memoryMap.put("db.r4.large", (double) (15.25 * 1024 * 1024 * 1024));
		memoryMap.put("db.r4.xlarge", (double) (30.5 * 1024 * 1024 * 1024));
		memoryMap.put("db.r4.2xlarge", (double) (61.0 * 1024 * 1024 * 1024));
		memoryMap.put("db.r4.4xlarge", (double) (122.0 * 1024 * 1024 * 1024));
		memoryMap.put("db.r4.8xlarge", (double) (244.0 * 1024 * 1024 * 1024));
		memoryMap.put("db.r4.16xlarge", (double) (488.0 * 1024 * 1024 * 1024));
		
		memoryMap.put("db.r3.large", (double) (15.25 * 1024 * 1024 * 1024));
		memoryMap.put("db.r3.xlarge", (double) (30.5 * 1024 * 1024 * 1024));
		memoryMap.put("db.r3.2xlarge", (double) (61.0 * 1024 * 1024 * 1024));
		memoryMap.put("db.r3.4xlarge", (double) (122.0 * 1024 * 1024 * 1024));
		memoryMap.put("db.r3.8xlarge", (double) (244.0 * 1024 * 1024 * 1024));
		
		memoryMap.put("db.t2.micro", (double) (1.0 * 1024 * 1024 * 1024));
		memoryMap.put("db.t2.small", (double) (2.0 * 1024 * 1024 * 1024));
		memoryMap.put("db.t2.medium", (double) (4.0 * 1024 * 1024 * 1024));
		memoryMap.put("db.t2.large", (double) (8.0 * 1024 * 1024 * 1024));
		memoryMap.put("db.t2.xlarge", (double) (16.0 * 1024 * 1024 * 1024));
		memoryMap.put("db.t2.2xlarge", (double) (32.0 * 1024 * 1024 * 1024));
	}
	
	public static Double getMemoryUtilization(String type, Double memory) {
		return memory / memoryMap.get(type) * 100;
	}
}