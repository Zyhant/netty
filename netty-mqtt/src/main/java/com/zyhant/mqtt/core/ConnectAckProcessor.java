package com.zyhant.mqtt.core;

import com.zyhant.common.domain.NettyConfig;
import com.zyhant.mqtt.domain.CacheConstants;
import com.zyhant.mqtt.domain.MqttClient;
import com.zyhant.mqtt.domain.MqttLogin;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * 连接处理
 * @author zyhant
 * @date 2024/7/23 22:57
 */
public class ConnectAckProcessor {

    private static final Logger log = LoggerFactory.getLogger(ConnectAckProcessor.class);

    public static void handle(ChannelHandlerContext ctx, MqttConnectMessage message, NettyConfig config, Callback callback) {
        try {
            Channel channel = ctx.channel();
            MqttConnectPayload payload = message.payload();
            MqttConnectVariableHeader mqttConnectVariableHeaderInfo = message.variableHeader();
            InetSocketAddress inetSocketAddress = (InetSocketAddress) channel.remoteAddress();
            String password = "";
            byte[] passwordInBytes = payload.passwordInBytes();
            if (passwordInBytes != null) {
                password = new String(passwordInBytes, StandardCharsets.UTF_8);
            }
            MqttLogin login = new MqttLogin();
            Field fieldAccount = login.getClass().getDeclaredField("account");
            fieldAccount.setAccessible(true);
            fieldAccount.set(login, payload.userName());
            fieldAccount.setAccessible(false);
            Field fieldPassword = login.getClass().getDeclaredField("password");
            fieldPassword.setAccessible(true);
            fieldPassword.set(login, password);
            fieldPassword.setAccessible(false);
            boolean check;
            if (callback != null) {
                check = callback.login(login);
            } else {
                check = false;
            }
            MqttConnectReturnCode connectReturnCode = check ? MqttConnectReturnCode.CONNECTION_ACCEPTED : MqttConnectReturnCode.CONNECTION_REFUSED_NOT_AUTHORIZED;
            // 回复连接报文
            MqttFixedHeader mqttFixedHeaderInfo = message.fixedHeader();
            //	构建返回报文， 可变报头
            MqttConnAckVariableHeader mqttConnAckVariableHeaderBack = new MqttConnAckVariableHeader(connectReturnCode, mqttConnectVariableHeaderInfo.isCleanSession());
            //	构建返回报文， 固定报头
            MqttFixedHeader mqttFixedHeaderBack = new MqttFixedHeader(MqttMessageType.CONNACK, mqttFixedHeaderInfo.isDup(), MqttQoS.AT_MOST_ONCE, mqttFixedHeaderInfo.isRetain(), 0x02);
            //	构建CONNACK消息体
            MqttConnAckMessage connAck = new MqttConnAckMessage(mqttFixedHeaderBack, mqttConnAckVariableHeaderBack);
            channel.writeAndFlush(connAck);
            MqttClient client = new MqttClient();
            Field fieldClientId = client.getClass().getDeclaredField("clientId");
            fieldClientId.setAccessible(true);
            fieldClientId.set(client, payload.clientIdentifier());
            fieldClientId.setAccessible(false);
            Field fieldAgreement = client.getClass().getDeclaredField("agreement");
            fieldAgreement.setAccessible(true);
            String agreementVersion = "V";
            int version = mqttConnectVariableHeaderInfo.version();
            switch (version) {
                case 4:
                    agreementVersion += "3.1.1";
                    break;
                default:
                    agreementVersion = version + "";
                    break;
            }
            fieldAgreement.set(client, agreementVersion);
            fieldAgreement.setAccessible(false);
            Field fieldAddress = client.getClass().getDeclaredField("address");
            fieldAddress.setAccessible(true);
            fieldAddress.set(client, inetSocketAddress.getAddress().getHostAddress());
            fieldAddress.setAccessible(false);
            Field fieldPort = client.getClass().getDeclaredField("port");
            fieldPort.setAccessible(true);
            fieldPort.set(client, inetSocketAddress.getPort());
            fieldPort.setAccessible(false);
            Field fieldHeartbeat = client.getClass().getDeclaredField("heartbeat");
            fieldHeartbeat.setAccessible(true);
            fieldHeartbeat.set(client, mqttConnectVariableHeaderInfo.keepAliveTimeSeconds());
            fieldHeartbeat.setAccessible(false);
            Field fieldCleanSession = client.getClass().getDeclaredField("cleanSession");
            fieldCleanSession.setAccessible(true);
            fieldCleanSession.set(client, mqttConnectVariableHeaderInfo.isCleanSession());
            fieldCleanSession.setAccessible(false);
            Field fieldChannel = client.getClass().getDeclaredField("channel");
            fieldChannel.setAccessible(true);
            fieldChannel.set(client, channel);
            fieldChannel.setAccessible(false);
            config.setCacheObject(CacheConstants.MQTT_CLIENT_KEY + channel.id().asLongText(), client, client.getHeartbeat() + 60, TimeUnit.SECONDS);
            log.info("MqttServer数据,设备:" + client.getClientId() + "," + (check ? "登录成功" : "登录失败"));
        } catch (Exception e) {
            log.error("MqttServer错误", e);
        }
    }

    public interface Callback {

        boolean login(MqttLogin login);

    }

}
