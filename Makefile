docker-build:
	docker-compose pull
	docker-compose up -d
	docker build \
		-t fdns-ms-rules \
		--network=fdns-ms-rules_default \
		--rm \
		--build-arg RULES_PORT=8086 \
		--build-arg RULES_FLUENTD_HOST=fluentd \
		--build-arg RULES_FLUENTD_PORT=24224 \
		--build-arg OBJECT_URL=http://fdns-ms-object:8083 \
		--build-arg RULES_PROXY_HOSTNAME= \
		--build-arg OAUTH2_ACCESS_TOKEN_URI= \
		--build-arg OAUTH2_PROTECTED_URIS= \
		--build-arg OAUTH2_CLIENT_ID= \
		--build-arg OAUTH2_CLIENT_SECRET= \
		--build-arg SSL_VERIFYING_DISABLE=false \
		.
	docker-compose down

docker-run: docker-start
docker-start:
	docker-compose up -d
	docker run -d \
		-p 8086:8086 \
		--network=fdns-ms-rules_default  \
		--name=fdns-ms-rules_main \
		fdns-ms-rules

docker-stop:
	docker stop fdns-ms-rules_main || true
	docker rm fdns-ms-rules_main || true
	docker-compose down

docker-restart:
	make docker-stop 2>/dev/null || true
	make docker-start

sonarqube:
	docker-compose up -d
	docker run -d --name sonarqube -p 9000:9000 -p 9092:9092 sonarqube || true
	mvn -DOBJECT_URL=http://localhost:8083 clean test sonar:sonar
	docker-compose down
