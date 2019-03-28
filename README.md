# Schema Registry Workshop

Exercises for the Kafka Workshop

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