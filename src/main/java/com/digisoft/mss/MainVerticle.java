package com.digisoft.mss;


import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;

/**
 * A verticle that works as a micro-service for the management of subscriptions.
 */
public class MainVerticle extends AbstractVerticle {
    private static final String DEFAULT_RABBIT_HOST = "rabbit";
    private static final int DEFAULT_RABBIT_PORT = 5672;
    private static final int DEFAULT_HTTP_PORT = 8080;

    private Logger logger = LoggerFactory.getLogger(MainVerticle.class);

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        // fist we need operational parameters, either from
        // environment or from vertx configuration file
        String rabbitHost = System.getenv("RABBIT_HOST");
        if (rabbitHost == null) {
            rabbitHost = config().getString("rabbit.host", DEFAULT_RABBIT_HOST);
        }
        logger.info("Using rabbit host " + rabbitHost);

        Integer rabbitPort = Integer.getInteger("RABBIT_PORT");
        if (rabbitPort == null) {
            rabbitPort = config().getInteger("rabbit.port", DEFAULT_RABBIT_PORT);
        }
        logger.info("Using rabbit port " + rabbitPort);

        Integer httpPort = Integer.getInteger("HTTP_PORT");
        if (httpPort == null) {
            httpPort = config().getInteger("http.port", DEFAULT_HTTP_PORT);
        }
        logger.info("Using http port " + httpPort);

        // create the routes that are recognised by the service
        // and send requests to appropriate handler/catalog
        Router router = Router.router(vertx);
        router.get("/subscriptions/:id").handler(this::handleGetSubscription);
        router.put("/subscriptions").handler(this::handlePutSubscription);
        router.post("/messages").handler(this::handlePostMessage);

        // finally, create the http server, using the created router
        vertx
                .createHttpServer()
                .requestHandler(router::accept)
                .listen(httpPort, result -> {
                    if (result.succeeded()) {
                        logger.info("Messaging server started");
                        startFuture.complete();
                    } else {
                        logger.error("Couldn't start messaging server");
                        startFuture.fail(result.cause());
                    }
                });
    }

    /**
     * This HTTP-related method is responsible for handling the requests of subscription
     * definitions.
     *
     * @param routingContext routing context as provided by vertx-web
     */
    private void handleGetSubscription(RoutingContext routingContext) {
        routingContext.response().setStatusCode(OK.code()).end("Ok!");
    }

    /**
     * This HTTP-related method is responsible for handling the creation and modification of
     * subscriptions.
     *
     * @param routingContext routing context as provided by vertx-web
     */
    private void handlePutSubscription(RoutingContext routingContext) {

    }

    /**
     * This HTTP-related method is responsible for handling the sending of messages to subscribers.
     *
     * @param routingContext routing context as provided by vertx-web
     */
    private void handlePostMessage(RoutingContext routingContext) {

    }
}
