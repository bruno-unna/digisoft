# Extending from a ready-to-use vert.x image
FROM vertx/vertx3
MAINTAINER Bruno Unna <bruno.unna@gmail.com>

# Information about the verticle
ENV VERTICLE_NAME com.digisoft.mss.MainVerticle
ENV VERTICLE_HOME /usr/verticles

# Preferred exposed port
EXPOSE 8080

# Copy the actual verticle to the container
COPY ./verticles $VERTICLE_HOME

# And launch the verticle
WORKDIR $VERTICLE_HOME
ENTRYPOINT ["sh", "-c"]
CMD ["vertx run $VERTICLE_NAME -cp $VERTICLE_HOME/*"]