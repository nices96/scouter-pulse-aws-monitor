package scouter.pulse.aws.monitor;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner.Mode;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import scouter.pulse.aws.monitor.model.AWSProperties;
import scouter.pulse.aws.monitor.model.PulseInstance;

@SpringBootApplication
public class ScouterPulseApplication implements CommandLineRunner {
	
	@Autowired
	private AWSProperties properties;
	
	public static void main(String[] args) {
	    	SpringApplication app = new SpringApplication(ScouterPulseApplication.class);
	    	app.setBannerMode(Mode.OFF);
	    	app.run(args);
	}

	@Override
	public void run(String... args) throws Exception {
		ScheduledExecutorService taskExecutor = Executors.newScheduledThreadPool(100);
		
		taskExecutor.scheduleAtFixedRate(new GetMonitoringInstances(properties), 0, 1, TimeUnit.MINUTES);
		
		taskExecutor.scheduleAtFixedRate(new Runnable(){
			public void run() {
				for (PulseInstance pulse : GetMonitoringInstances.getRds()) {
					Runnable r = new GetStatisticsTask(pulse, "AWS/RDS", properties);
					new Thread(r).start();
				}
			}
		}, 0, properties.getPeriod(), TimeUnit.SECONDS);
		
		taskExecutor.scheduleAtFixedRate(new Runnable(){
			public void run() {
				for (PulseInstance pulse : GetMonitoringInstances.getEc2()) {
					Runnable r = new GetStatisticsTask(pulse, "AWS/EC2", properties);
					new Thread(r).start();
				}
			}
		}, 0, properties.getPeriod(), TimeUnit.SECONDS);
		
		taskExecutor.scheduleAtFixedRate(new Runnable(){
			public void run() {
				for (PulseInstance pulse : GetMonitoringInstances.getElb()) {
					Runnable r = new GetStatisticsTask(pulse, "AWS/ELB", properties);
					new Thread(r).start();
				}
			}
		}, 0, properties.getPeriod(), TimeUnit.SECONDS);
	}
}
