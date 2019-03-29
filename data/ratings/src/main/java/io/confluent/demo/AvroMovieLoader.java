package io.confluent.demo;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.stream.Stream;

public class AvroMovieLoader {

    public static void main(String[] args) {
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:29092");
        props.put("key.serializer", "org.apache.kafka.common.serialization.LongSerializer");
        props.put("value.serializer", "io.confluent.kafka.serializers.KafkaAvroSerializer");
        props.put("schema.registry.url", "http://localhost:8081");
        KafkaProducer producer = new KafkaProducer(props);

        try {
            Path path = Paths.get("../movies.dat");
            Stream<String> lines = Files.lines(path);
            lines.forEach(line -> {
                Movie movie = parseMovie(line);
                ProducerRecord<Long, Movie> pr;
                pr = new ProducerRecord<Long, Movie>("raw-movies",
                        movie.getMovieId(),
                        movie);
                producer.send(pr);
            });

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            producer.close();
        }
    }


    static Movie parseMovie(String text) {
        String[] tokens = text.split("\\:\\:");
        String id = tokens[0];
        String title = tokens[1];
        String releaseYear = tokens[2];
        String country = tokens[4];
        //String rating = tokens[5];
        String genres = tokens[7];
        String actors = tokens[8];
        String directors = tokens[9];
        String composers = tokens[10];
        String screenwriters = tokens[11];
        String cinematographer = tokens[12];
        String productionCompanies = "";
        if(tokens.length > 13) {
            productionCompanies = tokens[13];
        }

        Movie movie = new Movie();
//        movie.setMovieId(Long.parseLong(id));
//        movie.setTitle(title);
//        movie.setReleaseYear(Integer.parseInt(releaseYear));
//        movie.setCountry(country);
//        movie.setGenres(Parser.parseArray(genres));
//        movie.setActors(Parser.parseArray(actors));
//        movie.setDirectors(Parser.parseArray(directors));
//        movie.setComposers(Parser.parseArray(composers));
//        movie.setScreenwriters(Parser.parseArray(screenwriters));
//        movie.setCinematographer(cinematographer);
//        movie.setProductionCompanies(Parser.parseArray(productionCompanies));

        return movie;
    }
}