# Kafka Workshop

Exercises for the Kafka Workshop

## Exercise 0: Getting Ready

If you're reading this, you probably know where to find the repo with the instructions, since this is it! Now that you're here, follow these instructions to get ready for the workshop:

1. Install Docker ([Mac](https://docs.docker.com/docker-for-mac/install/), [Windows](https://docs.docker.com/docker-for-windows/install/)) on your system.

    * Mac/Windows only: in Docker’s advanced settings, increase the memory dedicated to Docker to at least 8GB.

    * Confirm that Docker has at least 8GB of memory available to it: 

          docker system info | grep Memory 

      _Should return a value greater than 8GB - if not, the Kafka stack will probably not work._

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


3. Clone this repo by typing `git clone https://github.com/confluentinc/kafka-workshop` from a terminal.

4. From the `kafka-workshop` directory (which you just cloned), run `docker-compose pull`. This will kick off no small amount of downloading. Get this primed before Exercise 1 begins later on!

## Exercise 1: Producing and Consuming to Kafka topics

1. Run the workshop application by typing 

        docker-compose up -d
    
    from the project root. This will start up a Kafka broker, a Zookeeper node, and a KSQL server.

    We're using the `kafkacat` tool to interact with the Kafka cluster, invoked here through Docker Compose. `kafkacat` is a similar command-line tool to others that you may have seen including `kafka-console-consumer` and `kafka-console-producer` etc. 

3. Verify that you have connectivity to your Kafka cluster by typing 

        docker-compose exec worker \
        kafkacat -b kafka1:9092 \
                 -L

    This will list all cluster metadata, which at this point isn't much.

4. Produce a single record into the `movies-raw` topic from the `movies-json.js` file which you can view your local `worker/data` folder, and is mounted to the container. 

        docker-compose exec worker \
        kafkacat -b kafka1:9092 \
                 -P -c 1 \
                 -t movies-raw \
                 -l /data/movies-json.js


    Hint: you can see the flags available to you with kafkacat by running: `docker-compose exec kafkacat kafkacat -h`.

5. Once you've produced a record to the topic, open up a new terminal tab or window and consume it using `kafkacat` and the `-C` switch.

        docker-compose exec worker \
        kafkacat -b kafka1:9092 \
                 -C \
                 -t movies-raw

6. Go back to the producer terminal tab and send two records to the topic using `tail -n 2`. (It's okay that one of these is a duplicate.)

        docker-compose exec worker \
        kafkacat -b kafka1:9092 \
                 -P -c 2 \
                 -t movies-raw \
                 -l /data/movies-json.js

7. For fun, keep the consumer tab visible and run this shell script in the producer tab:

        docker-compose exec worker bash -c '
        cat /data/movies-json.js | while read in;
        do
        echo $in | kafkacat -b kafka1:9092 -P -t movies-raw
        sleep 1
        done'

    Press Ctrl-C to cancel this script after you've observed the messages arriving in the consumer.

8. Be sure to finish up by dumping all movie data into the `movies-raw` topic with 

        docker-compose exec worker \
        kafkacat -b kafka1:9092 \
                 -P \
                 -t movies-raw \
                 -l /data/movies-json.js


## Exercise 2: Kafka Connect

You need to confirm that Docker has at least 8GB of memory available to it: 

    docker system info | grep Memory 

Should return a value greater than 8GB - if not, the Kafka stack will probably not work.

----

The Docker Compose environment includes a Postgres database called `workshop`, pre-populated with a `movies` table. Using Kafka Connect and the JDBC connector you can stream the contents of a database table, along with any future changes, into a Kafka topic. 

First, let's check that Kafka Connect has started up. Run the following:

```bash
$ docker-compose logs -f connect|grep "Kafka Connect started"
```

Wait until you see the output `INFO Kafka Connect started (org.apache.kafka.connect.runtime.Connect)`. Press Ctrl-C twice to cancel and return to the command prompt. 

Now create the JDBC connector, by sending the configuration to the Connect REST API. We'll do this from within the Kafka Connect container: 

```
docker-compose exec connect bash -c 'curl -i -X POST -H "Accept:application/json" \
        -H  "Content-Type:application/json" http://localhost:8083/connectors/ \
        -d @/connect/postgres-source.json'
```

Now check that the connector's been created: 

```
docker-compose exec connect bash -c 'curl -s "http://localhost:8083/connectors"'

["jdbc_source_postgres_movies"]
```

and that it is running successfully: 

```
docker-compose exec connect bash -c 'curl -s "http://localhost:8083/connectors/jdbc_source_postgres_movies/status"'

{"name":"jdbc_source_postgres_movies","connector":{"state":"RUNNING","worker_id":"kafka-connect:8083"},"tasks":[{"state":"RUNNING","id":0,"worker_id":"kafka-connect:8083"}],"type":"source"}
```

---

* _NOTE: if you have `curl` and [`jq`](https://stedolan.github.io/jq/) on your *host* machine, you can use the following bash snippet to use the REST API to easily see the status of the connector that you've created._

  ```
   curl -s "http://localhost:8083/connectors" | \
     jq '.[]' | \
     xargs -I{connector_name} curl -s "http://localhost:8083/connectors/"{connector_name}"/status" | \
     jq -c -M '[.name,.connector.state,.tasks[].state]|join(":|:")' | \
     column -s : -t | \
     sed 's/\"//g' | \
     sort
  ```

  You should get output that looks like this: 

  ```
  jdbc_source_postgres_movies  |  RUNNING  |  RUNNING
  ```

--- 

The JDBC connector will have pulled across all existing rows from the database into a Kafka topic. Run the following, to list the current Kafka topics: 

```bash
docker-compose exec kafka1 bash -c 'kafka-topics --zookeeper zookeeper:2181 --list'
```

You should see, amongst other topics, one called `postgres-movies`. Now let's inspect the data on the topic. Because Kafka Connect is configured to use Avro serialisation we'll use the `kafka-avro-console-consumer` to view it: 

```bash
docker-compose exec connect \
                    kafka-avro-console-consumer \
                    --bootstrap-server kafka1:9092 \
                    --property schema.registry.url=http://schemaregistry:8081 \
                    --topic postgres-movies --from-beginning
```

You should see the contents of the movies table spooled to your terminal. 

**Leave the above command running**, and then in a new window launch a postgres shell: 

```bash
docker-compose exec database bash -c 'psql --username postgres --d WORKSHOP'
```

Arrange your terminal windows so that you can see both the `psql` prompt, and also the `kafka-avro-console-consumer` from the previous step (this should still be running; re-run it if not). 

Now insert a row in the Postgres `movies` table—you should see almost instantly the same data appear in the Kafka topic. 

```sql
INSERT INTO movies(id,title,release_year) VALUES (937,'Top Gun',1986);
```

_Bonus credit: The connector only captures `INSERT`s currently. Can you update the connector configuration to also capture `UPDATE`s? Can you suggest why the JDBC connector cannot capture `DELETE`s?_


## Exercise 3: Your Own Schema Design

1. Break into groups of two to four people. In your groups, discuss some simple business problems within each person's domain. 

2. Agree on one business problem to model. Draw a simple entity diagram of it, and make a list of operations your application must perform on the model. For example, if your business is retail, you might make a simple model of inventory, with entities for location, item, and supplier, plus operations for receiving, transferring, selling, and analysis.

3. Sketch out a simple application to provide a front end and necessary APIs for the system. For each service in the application, indicate what interface it provides (web front-end, HTTP API, etc.) and what computation it does over the data.

4. Some of the entities in your model are truly static, and some are not. Revisit your entity model and decide which entities are should be streams and which are truly tables.

5. Re-draw your diagram from step three with the appropriate tables replaced by streams. For each service in the application, keep its interface constant, but re-consider what computation it does over the data.

6. Time permitting, present your final architecture to the class. Explain how you adjudicated each stream/table duality and what streaming computations you planned.

## Exercise 4: Enriching Data with KSQL

1. Clean up the topic you created in the previous exercise as follows:

    ```
    docker-compose exec kafka1 \
    kafka-topics --zookeeper zookeeper:2181 \
                 --delete \
                 --topic movies-raw
    ```

2. In the worker container, send a single event of the movie data file to the topic: 

        docker-compose exec worker \
        kafkacat -b kafka1:9092 \
                 -P -c 1 \
                 -t movies-raw \
                 -l /data/movies-json.js

3. Launch the KSQL CLI: 


        docker-compose exec ksql-cli ksql http://ksql-server:8088

    You should see the startup screen: 

                        ===========================================
                        =        _  __ _____  ____  _             =
                        =       | |/ // ____|/ __ \| |            =
                        =       | ' /| (___ | |  | | |            =
                        =       |  <  \___ \| |  | | |            =
                        =       | . \ ____) | |__| | |____        =
                        =       |_|\_\_____/ \___\_\______|       =
                        =                                         =
                        =  Streaming SQL Engine for Apache Kafka® =
                        ===========================================

        Copyright 2017-2018 Confluent Inc.

        CLI v5.0.0, Server v5.0.0 located at http://ksql-server:8088

        Having trouble? Type 'help' (case-insensitive) for a rundown of how things work!

        ksql>

3. In the KSQL container, create a stream around the raw movie data: 

        CREATE STREAM movies_src (movie_id BIGINT, title VARCHAR, release_year INT) \
        WITH (VALUE_FORMAT='JSON', KAFKA_TOPIC='movies-raw');

    _The `\` is a line continuation character; you must include it if splitting the command over more than one line_

4. As you can see by selecting the records from that stream, the key (`ROWKEY`) is null:

        SELECT ROWKEY, movie_id, title, release_year FROM movies_src LIMIT 1;

    Re-key it: 
    
        CREATE STREAM movies_rekeyed WITH (PARTITIONS=1) AS \
        SELECT * FROM movies_src PARTITION BY movie_id;
        
    and verify that the messages are now correctly keyed: 

        SELECT ROWKEY, movie_id, title, release_year FROM movies_rekeyed;

    Leave the `SELECT` query running. 

5. With the `SELECT` from the previous step still running, stream ten more movie records into the `movies-raw` topic. 

        docker-compose exec worker \
        kafkacat -b kafka1:9092 \
                 -P -c 10 \
                 -t movies-raw \
                 -l /data/movies-json.js

    Watch them appear in the rekeyed stream. Cancel the continuous `SELECT` query by pressing Ctrl-C. 

6. Turn the movies into a table of reference data using the rekeyed stream:

        CREATE TABLE movies_ref (movie_id BIGINT, title VARCHAR, release_year INT) \
        WITH (VALUE_FORMAT='JSON', KAFKA_TOPIC='MOVIES_REKEYED', KEY='movie_id');

7. Launch the demo application to generate a stream of ratings events

        docker-compose exec worker bash -c 'cd streams-demo;./gradlew streamJsonRatings'

    After a minute or two you should see output similar to this:  

        Starting a Gradle Daemon, 1 incompatible and 1 stopped Daemons could not be reused, use --status for details
        Download https://jcenter.bintray.com/org/glassfish/javax.json/1.1.2/javax.json-1.1.2.pom
        Download https://jcenter.bintray.com/org/glassfish/javax.json/1.1.2/javax.json-1.1.2.jar

        > Task :streamJsonRatings
        Streaming ratings to kafka1:9092
        log4j:WARN No appenders could be found for logger (org.apache.kafka.clients.producer.ProducerConfig)
        log4j:WARN Please initialize the log4j system properly.
        log4j:WARN See http://logging.apache.org/log4j/1.2/faq.html#noconfig for more info.
        1539005442
        RATINGS PRODUCED 0
        RATINGS PRODUCED 1
        RATINGS PRODUCED 105
        RATINGS PRODUCED 520
        [...]    

8. In KSQL, create a KSQL stream to represent the ratings: 

        CREATE STREAM ratings (movie_id BIGINT, rating DOUBLE) WITH (VALUE_FORMAT='JSON', KAFKA_TOPIC='ratings');

    Query the stream to sample the first five records: 

        ksql> SELECT movie_id, rating FROM ratings LIMIT 5;
        658 | 5.892434316207264
        592 | 0.9017764100506152
        780 | 3.5642802867920924
        25 | 5.742257919645975
        802 | 5.683675232040815
        Limit Reached
        Query terminated
        ksql>

9. Join ratings to the movie data 

        ksql> SELECT m.title, m.release_year, r.rating \
              FROM ratings r \
                   LEFT OUTER JOIN movies_ref m \
                   ON r.movie_id = m.movie_id;

        null | null | 5.892434316207264
        null | null | 0.9017764100506152
        null | null | 3.5642802867920924
        [...]

    Note the nulls! We need more movies in the reference stream.

10. Leave the `SELECT` statement from the previous step running (and visible on your screen). In a new terminal window, stream the full contents of the `movies-json.js` file into the movies topic. 

        docker-compose exec worker \
        kafkacat -b kafka1:9092 \
                 -P \
                 -t movies-raw \
                 -l /data/movies-json.js

        Notice that the join starts working!

    Once you're happy with the results of the join, press Ctrl-C to cancel the `SELECT` statement.

11. Create a table containing average ratings as follows:

        CREATE TABLE movie_ratings AS \
            SELECT m.title, SUM(r.rating)/COUNT(r.rating) AS avg_rating, COUNT(r.rating) AS num_ratings \
              FROM ratings r \
                   LEFT OUTER JOIN movies_ref m \
                   ON m.movie_id = r.movie_id \
            GROUP BY m.title;

12. Select from that table and inspect the average ratings. Do you agree with them? Discuss. (If you want the table to stop updating, kill the Gradle task that is streaming the ratings—it's been going this whole time.)

### Extra Credit

Rewrite the KSQL queries to use the Avro topic you created in the Connect exercise.
