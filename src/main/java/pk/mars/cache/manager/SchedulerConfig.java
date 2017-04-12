package pk.mars.cache.manager;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
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
	
	private State state=State.INIT;
	private ScheduledExecutorService executor;
	private CloseableHttpClient httpclient = HttpClients.createDefault();
	ByteBuffer buffer=ByteBuffer.allocateDirect(256*1024); // 256KB
	private Logger logger=LoggerFactory.getLogger(SchedulerConfig.class);
	
	
	protected enum State {
		INIT,
		ACTIVE,
		SHUTDOWN
	}
	
	public class HTTPResource extends Thread {
		
		private SchedulerConfig scheduler;

		public HTTPResource(SchedulerConfig scheduler) {
			this.scheduler=scheduler;
		}
		
		@Override
		public void run() {
			int i=0;
			for(URI u : scheduler.getUris()) {
				if(scheduler.state==SchedulerConfig.State.SHUTDOWN)
					break;
				try {
					fetchAndWrite(u,++i);
				} catch(Exception e) {
					; // catching exception to start fetching next URL
				}
			}
		}

		private void fetchAndWrite(URI uri, int index) throws Exception {
			logger.info("FETCH START: URI {} in scheduler {}", uri, scheduler.getName());
			HttpGet httpGet = new HttpGet(uri);
			CloseableHttpResponse closeableResponse = null;			
			FileChannel writeChannel=null;
			ReadableByteChannel readChannel=null;
			HttpEntity httpEntity =null;
			buffer.clear();
			try {
				closeableResponse=httpclient.execute(httpGet);
			    httpEntity = closeableResponse.getEntity();
			    //logger.info("isChunked {}, isRepetable {}, isStreaming {}", httpEntity.isChunked(), httpEntity.isRepeatable(), httpEntity.isStreaming());
			    readChannel=Channels.newChannel(httpEntity.getContent());			    			    
			    int read=readChannel.read(buffer);
			    writeChannel=FileChannel.open(Paths.get(config.get(CacheManager.CACHE_DIR), scheduler.getName()+"."+index+".html"), StandardOpenOption.CREATE,StandardOpenOption.WRITE);
			    long totalBytesRead=read;
			    while(read!=-1) {
				    buffer.flip();
				    writeChannel.write(buffer);
				    buffer.clear();
				    read=readChannel.read(buffer);
				    totalBytesRead+=read;
			    }
			    writeChannel.truncate(totalBytesRead+1);			    
			    logger.info("FETCH END: URI {} in scheduler {}", uri, scheduler.getName());
			} catch (Exception e) {
				if(!(e instanceof ClosedByInterruptException))
				logger.error("An exception occured while fetching content", e);
				throw e;
			} finally {
			    try {
			    	if(closeableResponse!=null)
			    		closeableResponse.close();
				    writeChannel.close();
				    readChannel.close();
				    EntityUtils.consume(httpEntity);
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
		this.state=State.ACTIVE;
		
		executor=Executors.newScheduledThreadPool(1);
		executor.scheduleAtFixedRate(new HTTPResource(this), 0, getInterval(), TimeUnit.MINUTES);		
		logger.info("Scheduler: {} is activated", getName());
	}
	
	private void checkIfAlreadyActive() {
		if(this.state==State.ACTIVE) throw new IllegalStateException("Scheduler is in active state...");
	}
	
	public void shutdown() {
		logger.info("Shutdown request received for scheduler: {}", getName());
		this.state=State.SHUTDOWN;
		executor.shutdownNow();
	}
}
