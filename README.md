"# cache-manager" 
It helps in fetching pre-defined URLs with multiple schedulers to keep fetching from the upstream server with fixed intervals.

Schedulers can be defined as following:
```
config.cache.dir=/etc/static/cache

scheduler.1.name=Home_Categories
scheduler.1.interval=1
scheduler.1.url=http://localhost:8780/konakart/SelectHomeCat.action?catId=1&skipSEORedirect=1
scheduler.1.url=http://localhost:8780/konakart/SelectHomeCat.action?catId=2&skipSEORedirect=1
scheduler.1.url=http://localhost:8780/konakart/SelectHomeCat.action?catId=3&skipSEORedirect=1
```

The above scheduler will fetch every 1 minute the defined URLs in order. The output from upstream server will be stored in the provided cache dir "/etc/static/cache" which can be pulled by NGINX using ssi.


mvn clean package exec:java -Dexec.mainClass=pk.mars.cache.manager.CacheManager



