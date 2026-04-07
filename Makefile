run:
	mvn -q compile exec:java

test:
	mvn -q test

build:
	mvn -q package -DskipTests

deb: build
	jpackage --input target --main-jar cat.jar --name cat-java --type deb --app-version 1.0 --dest target/dist
