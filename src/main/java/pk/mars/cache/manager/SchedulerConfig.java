package pk.mars.cache.manager;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class SchedulerConfig {
	
	private int id;
	private String name;
	/**
	 * Interval in minutes
	 */
	private int interval;
	
	private List<URI> uris=new ArrayList<URI>();
		
	public SchedulerConfig(int id, String name) {
		this.id=id;
		this.name=name;
	}
	
	public int getId() {
		return id;
	}
	
	public String getName() {
		return name;
	}
	
	public List<URI> getUris() {
		return uris;
	}
	public void setUris(List<URI> uris) {
		this.uris = uris;
	}
	
	public void setInterval(int interval) {
		this.interval = interval;
	}
	
	public int getInterval() {
		return interval;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof SchedulerConfig) {
			SchedulerConfig _this=(SchedulerConfig)obj;
			if(_this.id==getId()) return true;
		}
		return false;
	}
}
