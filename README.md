# Schema Registry Workshop

Exercises for the Confluent Schema Registry Workshop

## Exercise 0: Getting Ready

If you're reading this, you probably know where to find the repo with the instructions, since this is it! Now that you're here, follow these instructions to get ready for the workshop:

1. Install Docker ([Mac](https://docs.docker.com/docker-for-mac/install/), [Windows](https://docs.docker.com/docker-for-windows/install/)) on your system.

    * Mac/Windows only: in Docker’s advanced settings, increase the memory dedicated to Docker to at least 4GB.

    * Confirm that Docker has at least 4GB of memory available to it (on a Windows system, omit the `| grep Memory` and simply look for "Total Memory" in the output): 

          docker system info | grep Memory 

      _Should return a value greater than 4GB - if not, the Kafka stack will probably not work._

    * Smoke test your Docker environment, by running : 

        docker run -p 8080:8080 hello-world

      You should see: 

          $  docker run -p 8080:8080 hello-world
          Unable to find image 'hello-world:latest' locally
          latest: Pulling from library/hello-world
          d1725b59e92d: Pull complete
          Digest: sha256:0add3ace90ecb4adbf7777e9aacf18357296e799f81cabc9fde470971e499788
          Status: Downloaded newer image for hello-world:latest

          Hello from Docker!
          This message shows that your installation appears to be working correctly.
          [...]


3. Clone this repo by typing `git clone https://github.com/confluentinc/schema-registry-workshop` from a terminal.

4. From the `schema-registry-workshop` directory (which you just cloned), run `docker-compose pull`. This will kick off no small amount of downloading. *It is vitally important that you do this before coming to the workshop.*

5. After `docker-compose` is done, run the following:

        cd data/ratings
        ./gradlew build

    This will download dependencies for the data generator.

6. Tools you'll need:

    * A text editor
    * A means of interacting with a REST API. `curl` works well for this from applicable terminal environments. Various browser plugins can provide a GUI for this same purpose. (_Advanced REST Client_ for Chrome seems to do the trick.)
    * Tools like `jq` for manipulating JSON can be very handy, but you can use a text editor if all else fails.
    * A Java IDE may come in handy for the Java portions


## Exercise 2: Schemas with REST
In this exercise we'll design an Avro schema, register it in the Confluent Schema Registry, produce and consume events using this schema, and then modify the schema in compatible and incompatible ways.

0. Start up the Docker environment by running `docker-compose up -d` from the project root directory.

1. Check out the `data/movies-json.js` file. It schema looks something like this:

  * movie_id : int
  * title : string
  * release_year : int
  * genres : array of strings
  * actors : array of strings
  * directors : array of strings
  * composers : array of strings
  * screenwriters : array of strings
  * production_companies : array of strings
  * cinematographer : string

2. Create a minimal `.avsc` definition including only `movie_id`, `title`, and `release_year`.

   Avro's [schema definition file is documented online](https://avro.apache.org/docs/1.8.1/spec.html#schemas).

3. Register the schema in the Confluent Schema Registry.

   Instructions can be found in [Schema Registry documentation](https://docs.confluent.io/current/schema-registry/docs/intro.html#quickstart).

   We are registering a schema for values, not keys. We will be producing the records to a topic called `movies-raw`, so we'll register the schema under the _subject_ `movies-raw-value`.
   It is important to note the details of the Schema Registry API for [registering a schema](https://docs.confluent.io/current/schema-registry/docs/api.html#post--subjects-(string-%20subject)-versions). It says:
   ```  
   Request JSON Object:  
    
   schema – The Avro schema string
   ```
   Which means that we need to pass to the API a JSON record, with one key "schema" and the value is a string containing our schema. We can't pass the schema itself when registering it.

   If you have the `jq` tool, you can use it to wrap our Avro Schema as follows: `jq -n --slurpfile schema movies-raw.avsc  '$schema | {schema: tostring}'`
   And then pass the output of `jq` to `curl` with a pipe:    
   ```  
   jq -n --slurpfile schema movies-raw.avsc  '$schema | {schema: tostring}' | curl -X POST -H "Content-Type: application/vnd.schemaregistry.v1+json" --data @- http://localhost:8081/subjects/movies-raw-value/versions  
   ```  
   The output should be an ID. Remember the ID you got, so you can use it when producing and consuming events.  

      If you can't run `jq`, you can reformat the AVSC file into something that looks like this:
   ```
   {"schema": "{\"type\": \"record\", \"name\": \"movie\", \"fields\" : [{\"name\": \"movie_id\", \"type\": \"long\"},{\"name\": \"title\", \"type\": \"string\"},{\"name\": \"release_year\", \"type\": \"long\"}] }"}
    ```


4. Now it is time to produce an event with our schema. We'll use the REST Proxy for that.

   You can see [few examples for using Rest Proxy](https://docs.confluent.io/current/kafka-rest/docs/intro.html#produce-and-consume-avro-messages). Note that you don't have to include the entire schema in every single message, since the schema is registered, you can just include the ID: https://docs.confluent.io/current/kafka-rest/docs/api.html#post--topics-(string-topic_name)  
  
   For example, to produce to the movies topic, we can run:  
   ```
   curl -X POST -H "Content-Type: application/vnd.kafka.avro.v2+json" -H "Accept: application/vnd.kafka.v2+json" --data '{"value_schema_id": 1, "records": [{"value": {"movie":{"movie_id": 1, "title": "Ready Player One", "release_year":2018}}}]}'  http://localhost:8082/topics/movies-raw
   ```
  
5. Let's try to consume some messages. If you don't have `curl`, a browser-based REST client will work.

   ```
   curl -H "Accept: application/vnd.kafka.avro.v1+json" "http://localhost:8082/topics/movies-raw/partitions/0/messages?offset=0&count=10"
   ```

6. Schema changes

   Make some changes to the schema. Try each of these: adding fields, removing fields, and modifying fields. Is the result compatible? Let's check with schema registry:
   ```
   jq -n --slurpfile schema movies-raw-new.avsc  '$schema | {schema: tostring}' |curl -X POST -H "Content-Type: application/vnd.schemaregistry.v1+json" --data @- http://localhost:8081/compatibility/subjects/movies-raw-value/versions/latest
   ```

7. Use the REST Proxy to produce and consume messages with modified schemas, both compatible and in-compatible. Document what happens in each case (adding, removing, modifying).

8. Using a GET to `/config/subject/raw-movies-value`, report on what the schema comptibility is for the subject. (HINT: it should be `BACKWARD`.) Change it to `FOREWARD` and repeat the in the previous two steps.


# Exercise 3: Schemas in Java

In this exercise, we will repeat what we just did with REST, but in Java.

1. Drop the previous environment by running `docker-compose down` then `docker-compose up -d` again.

2. Open up the Java project in `data/ratings`. It has a Gradle build that you can run from the command line. If you have a Java IDE, you can generate project files like this:

    * For Eclipse: `gradlew eclipse`
    * For IntelliJ IDEA: `gradlew idea`

    You should now be able to open the project in the IDE of your choice.

3. The file `src/main/avro/movie.avsc` exists, but is not terribly interesting. Populate it with a minimal schema including `movie_id`, `title`, and `release_year`. NOTE: every time you change the AVSC files, you must run `gradlew generateAvroJava` from the command line to regenerate the Java classes. You can see the generated code in `build/generated-main-avro-java`.

4. `src/main/java/io/confluent/demo/AvroMovieLoader.java` is a partially functioning Kafka producer that loads and parses data from a text file and produces Avro Movie objects to a Kafka topic. Change this class in the following ways:

    * Produce to a topic called `raw-movies`
    * Load the four fields (`movie_id`, `title`, and `release_year`) in the `parseMovie()` method
    * Produce only a single line from the text file, not the whole file (for now)

5. `src/main/java/io/confluent/demo/DemoConsumer.java` is a partially functioning Kafka consumer. Modify it to consume Movie objects and print out their contents. Run this consumer to see the Movies you have produced.

6. Add the `directors` field to the `movie.avsc` file, rebuild the schema, and modify the `DemoProducer` and `DemoConsumer` to accommodate the new field. Can you still produce and consume? Try adding an `int` field to `movie.avsc`.

7. Change the compatibility mode of the value field of `raw-movies` to `FORWARD` and remove the fields added in the previous two steps, one at a time. Note the results.


