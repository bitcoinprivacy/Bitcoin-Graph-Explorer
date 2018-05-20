export VERSION=SNAPSHOT
export REPOSITORY=jorgemartinezpizarro
export FOLDER=/root/Bitcoin-Graph-Explorer/src/test/docker
## start
start:
	docker-compose up -d

## stop
stop:
	docker-compose kill

## clean
clean:
	docker-compose rm

##logs
logs:
	docker-compose logs --tail="all" --follow bitcoin postgres

## show this help screen
help:
	@printf "Available targets\n\n"
	@awk '/^[a-zA-Z\-\_0-9]+:/ { \
		helpMessage = match(lastLine, /^## (.*)/); \
		if (helpMessage) { \
			helpCommand = substr($$1, 0, index($$1, ":")); \
			helpMessage = substr(lastLine, RSTART + 3, RLENGTH); \
			printf "%-15s %s\n", helpCommand, helpMessage; \
		} \
	} \
	{ lastLine = $$0 }' $(MAKEFILE_LIST)

