package scouter.pulse.aws.monitor.model;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.SerializedName;

public class CounterModel {

	@SerializedName("object")
	private Obj object;
	
	@SerializedName("counters")
	private List<Counter> counters;
	
	public Obj getObject() {
		if (object == null) {
			object = new Obj();
		}
		
		return object;
	}

	public void setObject(Obj object) {
		this.object = object;
	}

	public List<Counter> getCounters() {
		if (counters == null) {
			counters = new ArrayList<Counter>();
		}
		
		return counters;
	}

	public void setCounters(List<Counter> counters) {
		this.counters = counters;
	}

	public static class Obj {
		private String host;
		private String name;
		private String type;
		private String address;
		
		public String getHost() {
			return host;
		}
		
		public void setHost(String host) {
			this.host = host;
		}
		
		public String getName() {
			return name;
		}
		
		public void setName(String name) {
			this.name = name;
		}
		
		public String getType() {
			return type;
		}
		
		public void setType(String type) {
			this.type = type;
		}
		
		public String getAddress() {
			return address;
		}
		
		public void setAddress(String address) {
			this.address = address;
		}
	}
	
	public static class Counter {
		private String name;
		private Number value;
		
		public String getName() {
			return name;
		}
		
		public void setName(String name) {
			this.name = name;
		}

		public Number getValue() {
			return value;
		}

		public void setValue(Number value) {
			this.value = value;
		}
	}
}
