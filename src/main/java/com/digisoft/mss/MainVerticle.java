package com.digisoft.mss;


import com.digisoft.mss.model.Subscription;

import java.util.HashSet;
import java.util.Set;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.rabbitmq.RabbitMQClient;

import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CREATED;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;

/**
 * A verticle that works as a micro-service for the management of subscriptions.
 */
public class MainVerticle extends AbstractVerticle {
    private static final String DEFAULT_RABBIT_HOST = "rabbit";
    private static final int DEFAULT_RABBIT_PORT = 5672;
    private static final int DEFAULT_HTTP_PORT = 8080;
    private static final String EXCHANGE_NAME = "mss.direct";
    private RabbitMQClient rabbitMQClient;
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

        // a rabbitMQ client is needed
        rabbitMQClient = RabbitMQClient.create(vertx, new JsonObject()
                .put("host", rabbitHost)
                .put("port", rabbitPort));
        rabbitMQClient.start(startResult -> {
            if (startResult.succeeded()) {
                logger.info("RabbitMQ client has been started");
                rabbitMQClient.exchangeDeclare(EXCHANGE_NAME, "direct", true, false, exchangeDeclarationResult -> {
                    if (exchangeDeclarationResult.failed()) {
                        logger.error("Can't declare the '" + EXCHANGE_NAME + "' exchange", exchangeDeclarationResult.cause());
                        startFuture.fail(exchangeDeclarationResult.toString());
                    } else {
                        logger.info("Exchange '" + EXCHANGE_NAME + "' has been declared");
                    }
                });
            } else {
                logger.error("RabbitMQ client couldn't start", startResult.cause());
                startFuture.fail(startResult.toString());
            }
        });

        // create the routes that are recognised by the service
        // and send requests to appropriate handler/catalog
        Router router = Router.router(vertx);

        // allow CORS, so that we can use swagger (and other tools) for testing

        Set<HttpMethod> allowedMethods = new HashSet<>();
        allowedMethods.add(GET);
        allowedMethods.add(PUT);
        allowedMethods.add(POST);

        Set<String> allowedHeaders = new HashSet<>();
        allowedHeaders.add("x-requested-with");
        allowedHeaders.add("Access-Control-Allow-Origin");
        allowedHeaders.add("origin");
        allowedHeaders.add("Content-Type");
        allowedHeaders.add("accept");

        router.route().handler(CorsHandler.create("*")
                .allowedMethods(allowedMethods)
                .allowedHeaders(allowedHeaders));

        router.route(GET, "/subscriptions/:id").handler(this::handleGetSubscription);
        router.route(PUT, "/subscriptions/:id").handler(this::handlePutSubscription);
        router.route(POST, "/messages").handler(this::handlePostMessage);

        // finally, create the http server, using the created router
        vertx
                .createHttpServer()
                .requestHandler(router::accept)
                .listen(httpPort, result -> {
                    if (result.succeeded()) {
                        logger.info("Messaging server started");
                        startFuture.complete();
                    } else {
                        logger.error("Couldn't start messaging server", result.cause());
                        startFuture.fail(result.cause());
                    }
                });

    }

    @Override
    public void stop() throws Exception {
        rabbitMQClient.exchangeDelete(EXCHANGE_NAME, exchangeDeletionResult -> {
            if (exchangeDeletionResult.succeeded()) {
                logger.info("'" + EXCHANGE_NAME + "' exchange has been deleted");
                rabbitMQClient.stop(stopResult -> {
                    if (stopResult.succeeded()) {
                        logger.info("RabbitMQ client has been stopped");
                    } else {
                        logger.error("RabbitMQ client couldn't stop", stopResult.cause());
                    }
                });
            } else {
                logger.error("Exchange '" + EXCHANGE_NAME + "' can't be deleted", exchangeDeletionResult.cause());
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
        routingContext.response().putHeader(CONTENT_TYPE.toString(), APPLICATION_JSON);

        String subscriptionId = routingContext.request().getParam("id");
        logger.info("received a get request, subscription_id=" + subscriptionId);

        if (subscriptionId == null) {
            routingContext
                    .response()
                    .setStatusCode(BAD_REQUEST.code())
                    .end(new JsonObject()
                            .put("code", BAD_REQUEST.code())
                            .put("message", BAD_REQUEST.reasonPhrase())
                            .encodePrettily());
        } else {
            // TODO replace this fake response with a real one
            routingContext
                    .response()
                    .setStatusCode(OK.code())
                    .end(new JsonObject()
                            .put("subscription_id", subscriptionId)
                            .put("counters", new JsonArray())
                            .encodePrettily());
        }
    }

    /**
     * This HTTP-related method is responsible for handling the creation and modification of
     * subscriptions.
     *
     * @param routingContext routing context as provided by vertx-web
     */
    private void handlePutSubscription(RoutingContext routingContext) {
        routingContext.response().putHeader(CONTENT_TYPE.toString(), APPLICATION_JSON);

        String subscriptionId = routingContext.request().getParam("id");
        logger.info("received a put request, subscription_id=" + subscriptionId);

        if (subscriptionId == null) {
            endWithError(routingContext, BAD_REQUEST);
        } else {
            routingContext.request().bodyHandler(body -> {
                final String bodyAsString = body.toString();
                try {
                    Subscription subscription = Json.decodeValue(bodyAsString, Subscription.class);
                    rabbitMQClient.queueDelete(subscriptionId, queueDeletionResult -> {
                        if (queueDeletionResult.succeeded()) {
                            logger.info("Queue '" + subscriptionId + "' has been deleted");
                        } else {
                            logger.warn("Queue '" + subscriptionId + "' couldn't be deleted", queueDeletionResult.cause());
                        }
                        rabbitMQClient.queueDeclare(subscriptionId, false, false, false, queueDeclarationResult -> {
                            if (queueDeclarationResult.succeeded()) {
                                logger.info("Queue '" + subscriptionId + "' has been declared");

                                final boolean[] bindError = {false};
                                subscription.getMessageTypes().forEach(messageType -> {
                                    rabbitMQClient.queueBind(subscriptionId, EXCHANGE_NAME, messageType, queueBindResult -> {
                                        if (queueBindResult.succeeded()) {
                                            logger.info("Queue '" + subscriptionId
                                                    + "' has been bound to exchange '" + EXCHANGE_NAME + "' with routing key "
                                                    + messageType);
                                        } else {
                                            bindError[0] = true;
                                            logger.error("Queue '" + subscriptionId
                                                    + "' can't be bound to exchange '" + EXCHANGE_NAME + "' with routing key "
                                                    + messageType, queueBindResult.cause());
                                        }
                                    });
                                });
                                if (bindError[0]) {
                                    endWithError(routingContext, INTERNAL_SERVER_ERROR);
                                } else {
                                    routingContext
                                            .response()
                                            .setStatusCode(OK.code())
                                            .end("{}");
                                }
                            } else {
                                logger.error("Queue '" + subscriptionId
                                        + "' couldn't be declared", queueDeclarationResult.cause());
                                endWithError(routingContext, INTERNAL_SERVER_ERROR);
                            }
                        });
                    });
                } catch (DecodeException e) {
                    logger.error("Error decoding '" + bodyAsString + "' as a subscription", e);
                    endWithError(routingContext, BAD_REQUEST);
                }
            });
        }

    }

    /**
     * This HTTP-related method is responsible for handling the sending of messages to subscribers.
     *
     * @param routingContext routing context as provided by vertx-web
     */
    private void handlePostMessage(RoutingContext routingContext) {
        routingContext.response().putHeader(CONTENT_TYPE.toString(), APPLICATION_JSON);

        logger.info("received a post request");

        // TODO replace this fake response with a real one
        routingContext
                .response()
                .setStatusCode(CREATED.code())
                .end("{}");
    }

    /**
     * Ends a routingContext's response with an error object.
     *
     * @param routingContext context who's response will be ended
     * @param responseStatus http response status to be given
     */
    private void endWithError(RoutingContext routingContext, HttpResponseStatus responseStatus) {
        routingContext
                .response()
                .setStatusCode(responseStatus.code())
                .end(Json.encodePrettily(
                        new com.digisoft.mss.model.Error(
                                responseStatus.code(),
                                responseStatus.reasonPhrase())));
    }
}
