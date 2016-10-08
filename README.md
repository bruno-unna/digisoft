# *Digicom*'s message subscription service

## TL;DR

* Instructions for building and running the exercise are provided [at the end](#how_to_run).
* A RabbitMQ server is required (but also provided).
* This project can be executed in several ways, depending on your preferences:
    * A fat-jar (also called an über-jar) is produced, than can be run autonomously.
    * The service can also be started using the vert.x executable.
    * A docker-compose file for the wrapped-up final product is included, that takes care of all servers and configuration.

## Problem

A simple service for managing subscriptions of users to message types is required. A subscription, in this sense, is the mapping from a *subscription identifier* to a set of *message types*.

### Requirements

#### Functional

The message subscription service must expose a RESTful interface for operations:

* Create a subscription (would have at least one parameter, which would be a list of `messageTypes` the subscription wants to keep track of).
* Read a subscription.  
    Must include information about how many times a particular message type has been received by a subscription. There may be more than one subscription listening for the same message type(s).
* Update a subscription.
* Post a message.  
    The message would have at least a `messageType` property.

#### Non-functional

* The solution must take the form of a runnable service.
* It must be written in a Java 8 technology.
* Clean, maintainable code must be used.
* Good programming practices should be displayed.

## Solution

It is nowadays commonly understood that expressing distributed systems in terms of micro-services provides a strong support towards a domain-based, scalable, maintainable, long-term solution.

Micro-services, even though they provide a solid starting point in the quest of a responsive and reliable distributed architecture, are only the beginning, they are not enough by themselves. A modern solution, in order to compete and be useful under the ever more stressing demands of current distributed systems, should be responsive, resilient and elastic. More often than not, these desired features are achievable by means of event-based architectures.

### Reactivity

The aforementioned features of a modern architecture have been described in the [Reactive Manifesto](http://www.reactivemanifesto.org/). Several approaches have emerged to its fulfillment, with two outstanding schools: reactive extensions (Rx) and reactive streams (RS). Each of them have implementations in the form of libraries, toolboxes or frameworks. Some implementations offer the two approaches at the same time.

A key concept of the reactive way is called *back pressure propagation*. It refers to the up-stream signaling (from consumers to producers) for the control of information traffic. This way, the traditional problems of consumer starvation and -more importantly- of buffer overflows are efficiently solved. A common trait of reactive technologies is the inclusion of automatic back pressure propagation in distributed systems.

### Architecture

The overall architecture of this solution is based on micro-services, but in addition to that, reactivity has been considered in the design. A Vert.x service (main *verticle*) receives requests from the REST clients, and hands them asynchronously to other *verticles* for processing.

In order to focus in the interesting aspects of the design (asynchronous event-passing, distributed architecture, back-pressure propagation, REST endpoints handling), the message brokerage heavy-lifting has been delegated to an AMPQ implementation (RabbitMQ).

### Technology stack

As stated before, RabbitMQ has been chosen as a message broker for this exercise. This provides the solution with the whole bunch of benefits from RabbitMQ, namely high-efficiency, high-availability, durability, security and management, practically for free. The key to leveraging this integration is the existence of [reactive drivers for the message broker](http://vertx.io/docs/vertx-rabbitmq-client/java/).

The solution is flexible enough to be run in any environment as long as it includes a RabbitMQ server (or cluster). For the purposes of the exercise, a docker image has been used, but that is configurable. The build script also generates a docker image with the runnable service. A [docker-compose](https://docs.docker.com/compose/overview/) 
specification file has been provided, that includes both the runnable service and the RabbitMQ dependency. That is possibly the easiest way to test the solution.

Since the solution was devised as a reactive application, a stack was chosen, as simple as possible but fit for purpose. Based on that reasoning, [Vert.x](http://vertx.io/) was selected.

### Discussion

#### Code structure

TBD

#### <a name="how_to_run"></a>How to run

1. Make sure you have the following tools available:
    - A Java 8 JDK.
    - Maven builder (tested with version 3.3.9).
    - A `docker` client available (tested with version 1.12.1).
    - `docker-compose` (tested with version 1.8.0).
1. Clone [this repository](https://github.com/bruno-unna/digisoft).
1. Within the base directory of the project, run `mvn package`.
1. Run `docker-compose -p digisoft up`.
1. Connect using a browser to [the proxy server](http://localhost:8080/pcw/PCW45-12345-12345-1234X/address/ie/D02X285).
1. Play changing the values in the URL and observe the cache hits and misses in the console.

#### Next steps and enhancements

- Adding a TTL (time to live) to the cached information. This is trivial 
  when using Redis but needs discussion and needs to be configured 
  in the solution.
- Writing integration tests. Several libraries exist to help with 
  this, AssertJ and Rest-Assured are known to play nicely with Vert.x. 
  It is recognised that this should have been done beforehand, but 
  time constraints forced the lowering of its priority.
- Providing ex-ante thorough unit tests. It is generally preferred 
  to write tests first (TDD), specially in complex or multi-component 
  systems. Again, time constraints limited very much the amount 
  of tests that were automated in the solution.
- Soak test the solution, hammering and stressing it to discover 
  how well it can potentially scale. JMeter can be used for this.