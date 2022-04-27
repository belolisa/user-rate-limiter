# user-rate-limiter
## Description
Simple web application with one endpoint runs on the root. 
Uses request number limitation per user in time frame


## How to run and use (example)
buildProject
```bash
./gradlew build  
```

build docker image
```bash
 docker build --tag=ratelimiter {$directory}
```

run docker
```bash
docker run -p8081:8081 ratelimiter:latest
```

request endpoint
```bash
curl http://localhost:8081/
```
