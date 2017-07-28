package scouter.pulse.aws.monitor.model;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.SerializedName;

public class RegisterModel {

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
		private String type;
		private String display;
		
		public String getType() {
			return type;
		}
		public void setType(String type) {
			this.type = type;
		}
		public String getDisplay() {
			return display;
		}
		public void setDisplay(String display) {
			this.display = display;
		}
	}
	
	public static class Counter {
		private String name;
		private String unit;
		private String display;
		
		public String getName() {
			return name;
		}
		
		public void setName(String name) {
			this.name = name;
		}
		
		public String getUnit() {
			return unit;
		}
		
		public void setUnit(String unit) {
			this.unit = unit;
		}
		
		public String getDisplay() {
			return display;
		}
		
		public void setDisplay(String display) {
			this.display = display;
		}
	}
}
