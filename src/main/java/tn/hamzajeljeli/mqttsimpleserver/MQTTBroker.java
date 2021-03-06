package tn.hamzajeljeli.mqttsimpleserver;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.mqtt.MqttEndpoint;
import io.vertx.mqtt.MqttServer;
import io.vertx.mqtt.MqttTopicSubscription;
import io.vertx.mqtt.messages.MqttPublishMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class MQTTBroker {

    public MQTTBroker() {
        MqttServer mqttServer = MqttServer.create(Vertx.vertx());
        init(mqttServer);
    }

    private static void init(MqttServer mqttServer) {
        mqttServer.endpointHandler(endpoint -> {
            System.out.println("MQTT client [" + endpoint.clientIdentifier() + "] request to connect");
            endpoint.accept(false);
            handleSubscription(endpoint);
            handleUnsubscription(endpoint);
            publishHandler(endpoint);
            handleClientDisconnect(endpoint);
        }).listen(ar -> {
            if (ar.succeeded()) {
                System.out.println("MQTT server is listening on port " + ar.result().actualPort());
            } else {
                System.out.println("Error on starting the server");
                ar.cause().printStackTrace();
            }
        });
    }

    private static void handleSubscription(MqttEndpoint endpoint) {
        endpoint.subscribeHandler(subscribe -> {
            List<MqttQoS> grantedQosLevels = new ArrayList<>();
            for (MqttTopicSubscription s: subscribe.topicSubscriptions()) {
                System.out.println("Subscription for " + s.topicName() + " with QoS " + s.qualityOfService());
                grantedQosLevels.add(s.qualityOfService());
            }
            endpoint.subscribeAcknowledge(subscribe.messageId(), grantedQosLevels);
        });
    }

    private static void handleUnsubscription(MqttEndpoint endpoint) {
        endpoint.unsubscribeHandler(unsubscribe -> {
            for (String t: unsubscribe.topics()) {
                System.out.println("Unsubscription for " + t);
            }
            endpoint.unsubscribeAcknowledge(unsubscribe.messageId());
        });
    }

    private static void publishHandler(MqttEndpoint endpoint) {
        endpoint.publishHandler(message -> {
            handleQoS(message, endpoint);
            System.out.println("Topic : "+message.topicName()+"\nGot message : "+new String(message.payload().getBytes()));
            endpoint.publish("toPublish/", Buffer.buffer("Server Response : GOT IT !\nID : "+ new Random().ints().toString()),MqttQoS.AT_MOST_ONCE,false,false);
        }).publishReleaseHandler(messageId -> {
            endpoint.publishComplete(messageId);
        });
    }


    private static void handleQoS(MqttPublishMessage message, MqttEndpoint endpoint) {
        if (message.qosLevel() == MqttQoS.AT_LEAST_ONCE) {
            String topicName = message.topicName();
            /*switch(topicName) {
                case TOPIC_LIGHTS:
                    LightsController.handler(message);
                    break;
//                    Future implementation for Camera
//                    case TOPIC_CAMERA:
//                        CamController.handler(message);
            }*/

            endpoint.publishAcknowledge(message.messageId());

        } else if (message.qosLevel() == MqttQoS.EXACTLY_ONCE) {
            endpoint.publishRelease(message.messageId());
        }
    }

    private static void handleClientDisconnect(MqttEndpoint endpoint) {
        endpoint.disconnectHandler(h -> {
            System.out.println("The remote client has closed the connection.");
        });
    }
}
