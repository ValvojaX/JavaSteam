package com.javasteam.steam.connection;

import com.javasteam.models.AbstractMessage;
import com.javasteam.models.Header;
import com.javasteam.models.messages.Message;
import com.javasteam.models.messages.ProtoMessage;
import com.javasteam.steam.HasListenerGroup;
import com.javasteam.steam.SteamProtocol;
import com.javasteam.steam.crypto.Crypto;
import com.javasteam.utils.common.ArrayUtils;
import com.javasteam.utils.proto.ProtoUtils;
import com.javasteam.utils.serializer.Serializer;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Base class for all connections to Steam servers. Provides basic functionality for reading and
 * writing. Handles encryption and decryption of messages. Also provides a listener system for
 * handling incoming messages.
 */
@Slf4j
public abstract class BaseConnection implements HasListenerGroup {
  @Setter private byte[] sessionKey;
  private final ListenerGroup listeners;
  private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

  public BaseConnection() {
    this.listeners = new ListenerGroup();
    this.executor.scheduleWithFixedDelay(
        this::read, 0, 100, java.util.concurrent.TimeUnit.MILLISECONDS);
  }

  public BaseConnection(int threads) {
    this.listeners = new ListenerGroup(threads);
    this.executor.scheduleWithFixedDelay(
        this::read, 0, 100, java.util.concurrent.TimeUnit.MILLISECONDS);
  }

  private void read() {
    if (!isConnected()) {
      return;
    }

    readData(SteamProtocol.PACKET_HEADER_SIZE)
        .ifPresentOrElse(
            headerBytes -> {
              log.debug("Received header: [{}] {}", headerBytes.length, headerBytes);

              int messageLength =
                  Serializer.unpack(headerBytes, ByteBuffer::getInt, ByteOrder.LITTLE_ENDIAN);
              int messageMagic =
                  Serializer.unpack(headerBytes, ByteBuffer::getInt, ByteOrder.LITTLE_ENDIAN, 4);
              if (messageMagic != SteamProtocol.PACKET_MAGIC) {
                log.error(
                    "Invalid message magic %s, expected %s"
                        .formatted(messageMagic, SteamProtocol.PACKET_MAGIC));
                throw new RuntimeException(
                    "Invalid message magic %s, expected %s"
                        .formatted(messageMagic, SteamProtocol.PACKET_MAGIC));
              }

              readData(messageLength)
                  .ifPresent(
                      bodyData -> {
                        byte[] packet = ArrayUtils.concat(headerBytes, bodyData);
                        log.debug("Received message: [{}] {}", packet.length, packet);
                        this.onRawPacket(packet);
                      });
            },
            () -> log.warn("Connection closed or EOF received"));
  }

  private void onRawPacket(byte[] packet) {
    byte[] message =
        ArrayUtils.subarray(
            packet,
            SteamProtocol.PACKET_HEADER_SIZE,
            packet.length - SteamProtocol.PACKET_HEADER_SIZE);

    if (this.sessionKey != null) {
      byte[] channelHmac = ArrayUtils.subarray(this.sessionKey, 0, 16);
      byte[] decryptedMessage = Crypto.decryptMessage(message, this.sessionKey, channelHmac);
      log.debug("Decrypted message: [{}] {}", decryptedMessage.length, decryptedMessage);
      message = decryptedMessage;
    }

    this.onRawMessage(message);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  public void onRawMessage(byte[] message) {
    int EMsgId = Serializer.unpack(message, ByteBuffer::getInt, ByteOrder.LITTLE_ENDIAN);
    int EMsg = ProtoUtils.clearProtoMask(EMsgId);
    log.info(
        "Received message with EMsgId {} ({}): [{} bytes]",
        EMsg,
        ProtoUtils.resolveEMsg(EMsg).map(Enum::name).orElse("Unknown"),
        message.length);

    AbstractMessage<? extends Header, Object> msg =
        ProtoUtils.isProto(EMsgId)
            ? (AbstractMessage) ProtoMessage.fromBytes(EMsg, message)
            : (AbstractMessage) Message.fromBytes(EMsg, message);

    listeners.onMessage(EMsg, msg);
  }

  public <H extends Header, T> void write(AbstractMessage<H, T> msg) {
    byte[] data = msg.serialize();
    if (this.sessionKey != null) {
      data =
          Crypto.encryptMessage(data, this.sessionKey, ArrayUtils.subarray(this.sessionKey, 0, 16));
    }

    byte[] packet =
        ArrayUtils.concat(
            Serializer.pack(data.length, ByteBuffer::putInt, ByteOrder.LITTLE_ENDIAN, 4),
            Serializer.pack(
                SteamProtocol.PACKET_MAGIC, ByteBuffer::putInt, ByteOrder.LITTLE_ENDIAN, 4),
            data);
    log.debug("Sending packet: [{}] {}", packet.length, packet);
    writeData(packet);
  }

  public abstract void connect(String host, int port, int timeout);

  public abstract void disconnect();

  public abstract boolean isConnected();

  public abstract InetAddress getLocalAddress();

  protected abstract Optional<byte[]> readData(int length);

  protected abstract void writeData(byte[] data);

  @Override
  public ListenerGroup getListenerGroup() {
    return listeners;
  }
}
