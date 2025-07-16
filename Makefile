.PHONY:all tika

all = tika

all:$(all)
	@echo 'done!'

tika:
	@echo 'build tika ...'
	mvn package -Ddetail=true -DskipTests -am

clean:
	mvn clean
