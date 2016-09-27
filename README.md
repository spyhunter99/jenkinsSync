# jenkinsSync

A tool to pull down the latest and greatest from jenkins for offline updating

# Build it

`mvn clean install`

# Run it to sync the files locally

````
cd target
java -jar jenkins-sync-1.0.0-SNAPSHOT-jar-with-dependencies.jar
````

now swivel chair the file `jenkinsSync.zip` to where ever your jenkins instance is. Ideally to a local web server of some
sort.

Enjoy