package pk.mars.cache.manager;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SchedulerConfig {
	
	private int id;
	private String name;
	private Map<String,String> config;
	
	private boolean activated=false;
	private ScheduledExecutorService executor;
	
	private Logger logger=LoggerFactory.getLogger(SchedulerConfig.class);
	
	public class HTTPResource extends Thread {
		
		private SchedulerConfig scheduler;

		public HTTPResource(SchedulerConfig scheduler) {
			this.scheduler=scheduler;
		}
		
		@Override
		public void run() {
			int i=0;
			for(URI u : scheduler.getUris()) {
				fetchAndWrite(u,++i);
			}
		}

		private void fetchAndWrite(URI uri, int index) {
			logger.info("running scheduled service for scheduler {}", scheduler.getName());
			CloseableHttpClient httpclient = HttpClients.createDefault();
			HttpGet httpGet = new HttpGet(uri);
			CloseableHttpResponse closeableResponse = null;			
			
			try {
				closeableResponse=httpclient.execute(httpGet);
			    HttpEntity httpEntity = closeableResponse.getEntity();
			    ReadableByteChannel readChannel=Channels.newChannel(httpEntity.getContent());
			    ByteBuffer buffer=ByteBuffer.allocateDirect(1024*512); // 512K
			    
			    int read=readChannel.read(buffer);
			    FileChannel writeChannel=FileChannel.open(Paths.get(config.get(CacheManager.CACHE_DIR), scheduler.getName()+"."+index+".html"), StandardOpenOption.CREATE,StandardOpenOption.WRITE);
			    while(read!=-1) {
				    buffer.flip();
				    writeChannel.write(buffer);
				    buffer.clear();
				    read=readChannel.read(buffer);
			    }
			    writeChannel.close();
			    readChannel.close();
			    EntityUtils.consume(httpEntity);
			} catch (Exception e) {
				logger.error("An exception occured while fetching content",e);
			} finally {
			    try {
			    	if(closeableResponse!=null)
			    		closeableResponse.close();
				} catch (IOException e) {
					logger.error("An exeception occured while closing response",e);
				}
			}
		}
	}
	
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
		checkIfAlreadyActive();
		this.uris = uris;
	}
	
	public void setInterval(int interval) {
		checkIfAlreadyActive();
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
	
	public void activate(Map<String,String> config) {
		checkIfAlreadyActive();
		this.config=config;
		this.activated=true;
		
		executor=Executors.newScheduledThreadPool(1);
		executor.scheduleAtFixedRate(new HTTPResource(this), 0, getInterval(), TimeUnit.MINUTES);
	}
	
	private void checkIfAlreadyActive() {
		if(this.activated) throw new IllegalStateException("Scheduler is in active state...");
	}
}
