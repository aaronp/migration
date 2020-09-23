mvn assembly:single

java -jar ./target/migrate-0.0.1-SNAPSHOT-jar-with-dependencies.jar url=https://storage.googleapis.com/mygration $@