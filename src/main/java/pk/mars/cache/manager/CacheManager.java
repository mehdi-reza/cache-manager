package pk.mars.cache.manager;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CacheManager {

	private Logger logger = LoggerFactory.getLogger(CacheManager.class);

	private List<SchedulerConfig> schedulers=new ArrayList<SchedulerConfig>();
	private Map<String,String> config=new HashMap<String,String>();
	
	public static final String CACHE_DIR="cache.dir";
	
	public static void main(String[] args) {
		new CacheManager().startCacheManager();
	}

	private void startCacheManager() {
		
		logger.info("Started at {}",new Date());
		readConfig();
		logConfig();

		// activate all schedulers()
		for(SchedulerConfig scheduler : schedulers) {
			scheduler.activate(config);
		}
		
		Scanner scanner=new Scanner(System.in);
		boolean alive=true;
		while(alive) {
			int i=scanner.nextInt();
			
			if(i==0) alive=false;
		}
		// initiate shutdown of all schedulers
		for(SchedulerConfig scheduler : schedulers) {
			scheduler.shutdown();
		}
		scanner.close();
	}

	private void logConfig() {		
		StringBuilder configSummary=new StringBuilder();
		StringBuilder schedulersSummary=new StringBuilder();
		for(String c : config.keySet()) {
			configSummary.append(String.format("%1$s = %2$s", c, config.get(c)));
			configSummary.append("\n");
		}
		
		logger.info(configSummary.toString());
		
		for(SchedulerConfig scheduler : schedulers) {
			schedulersSummary.append(String.format("Scheduler id: %1$s, name %2$s",scheduler.getId(), scheduler.getName())).append("\n");
			schedulersSummary.append(String.format("interval: %1$s", scheduler.getInterval())).append("\n");
			for(URI u : scheduler.getUris()) {
				schedulersSummary.append(u).append("\n");
			}
			schedulersSummary.append("\n");
		}
		logger.info(schedulersSummary.toString());
	}

	private void readConfig() {
		BufferedReader reader = null;

		Pattern configPattern = Pattern
				.compile("config\\.(.*)\\=(.*)");

		Pattern idNamePattern = Pattern
				.compile("scheduler\\.([0-9])\\.name\\=(.*)");
		Pattern urlPattern = Pattern
				.compile("scheduler\\.([0-9])\\.(url|interval)\\=(.*)");		
		try {
			reader = new BufferedReader(new FileReader(CacheManager.class
					.getResource("/scheduler.properties").getFile()));
			String line = reader.readLine();
			SchedulerConfig schedulerConfig = null;
			while (line != null) {
				if(line.trim().equals("")) { // ignore empty lines
					line=reader.readLine();
					continue;
				}
				Matcher configMatcher=configPattern.matcher(line);
				// is it a config statement?
				if(configMatcher.matches()) {					
					config.put(configMatcher.group(1), configMatcher.group(2));
					line=reader.readLine();
					continue;
				}
				// is it a new scheduler definition?
				Matcher idNameMatcher = idNamePattern.matcher(line); // id and name				
				if (idNameMatcher.matches()) {
					String id = idNameMatcher.group(1);
					String name = idNameMatcher.group(2);
					schedulerConfig = new SchedulerConfig(Integer.parseInt(id),
							name);
					schedulers.add(schedulerConfig);
				} else {
					Matcher urlMatcher = urlPattern.matcher(line);
					while (urlMatcher.matches() && line != null) {
						
						int schedulerId=new Integer(urlMatcher.group(1));
						SchedulerConfig _schedulerConfig=null;
						// get the scheduler config from list if available
						for(SchedulerConfig s : schedulers) {
							if(s.getId()==schedulerId) {
								_schedulerConfig=s;
								break;
							}
						}
						if(_schedulerConfig==null) throw new IllegalStateException("Scheduler "+schedulerId+" not defined yet..");
						
						String type = urlMatcher.group(2);
						// is it interval property?
						if(type.equals("interval")) {
							_schedulerConfig.setInterval(new Integer(urlMatcher.group(3)));
							line=reader.readLine();
							if (line != null)
								urlMatcher = urlPattern.matcher(line);
							continue;
						}
						// it must be the url
						String url = urlMatcher.group(3);
						_schedulerConfig.getUris().add(new URI(url));
						line = reader.readLine();
						if (line != null)
							urlMatcher = urlPattern.matcher(line);
					}
				}
				line = reader.readLine();
			}

			reader.close();
		} catch (Exception e) {
			e.printStackTrace(System.out);
		} finally {
			if (reader != null)
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
	}
}
