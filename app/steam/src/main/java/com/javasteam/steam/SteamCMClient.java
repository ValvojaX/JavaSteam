package com.javasteam.steam;

import static com.javasteam.protobufs.EnumsClientserver.EMsg;
import static com.javasteam.protobufs.SteammessagesBase.CMsgMulti;

import com.javasteam.models.AbstractMessage;
import com.javasteam.models.Header;
import com.javasteam.models.ProtoHeader;
import com.javasteam.models.headers.MessageHeader;
import com.javasteam.models.messages.Message;
import com.javasteam.models.structs.ChannelEncryptRequest;
import com.javasteam.models.structs.ChannelEncryptResponse;
import com.javasteam.models.structs.ChannelEncryptResult;
import com.javasteam.steam.common.EResult;
import com.javasteam.steam.connection.TCPConnection;
import com.javasteam.steam.crypto.Crypto;
import com.javasteam.utils.common.ArrayUtils;
import com.javasteam.utils.common.ZipUtils;
import com.javasteam.utils.serializer.Serializer;
import com.javasteam.webapi.endpoints.steamdirectory.SteamWebDirectoryRESTAPIClient;
import com.javasteam.webapi.endpoints.steamdirectory.models.SteamCMServer;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * Basic Steam CM client that connects to a CM server and listens for messages. It also handles the
 * channel encryption process. The client can be used to send and receive messages from the CM
 * server. Uses the {@link TCPConnection} class to handle the TCP connection.
 */
@Slf4j
public class SteamCMClient implements HasListenerGroup {
  private final List<SteamCMServer> cmList;
  private final TCPConnection socket;
  private byte[] sessionKey;

  public SteamCMClient(int threads) {
    this.cmList =
        SteamWebDirectoryRESTAPIClient.getInstance().getCMList(0).getResponse().getServerlist();
    this.socket = new TCPConnection(threads);
    this.initializeListeners();
  }

  public <H extends Header, T> void sendMessage(AbstractMessage<H, T> msg) {
    this.socket.write(msg);
  }

  private void initializeListeners() {
    this.addMessageListener(EMsg.k_EMsgChannelEncryptRequest_VALUE, this::onChannelEncryptRequest);
    this.addMessageListener(EMsg.k_EMsgChannelEncryptResult_VALUE, this::onChannelEncryptResult);
    this.addMessageListener(EMsg.k_EMsgMulti_VALUE, this::onMulti);
  }

  private void onChannelEncryptRequest(AbstractMessage<MessageHeader, ChannelEncryptRequest> msg) {
    log.info("Received channel encrypt request:\n{}", msg);

    ChannelEncryptRequest request =
        msg.getMsgBody().orElseThrow(() -> new RuntimeException("No body found in message"));
    log.debug("ChannelEncryptRequest: \n{}", request);

    this.sessionKey = Crypto.generateSessionKey();
    byte[] encryptedSessionKey = Crypto.encryptSessionKey(sessionKey, request.getChallenge());
    int crc = Crypto.calculateCRC32(encryptedSessionKey);

    ChannelEncryptResponse res = new ChannelEncryptResponse(1, 128, encryptedSessionKey, crc, 0);
    log.debug("ChannelEncryptResponse: \n{}", res);

    var response = Message.of(MessageHeader.of(EMsg.k_EMsgChannelEncryptResponse_VALUE), res);
    log.debug("Sending channel encrypt response");

    this.sendMessage(response);
  }

  private void onChannelEncryptResult(AbstractMessage<MessageHeader, ChannelEncryptResult> msg) {
    log.info("Received channel encrypt result:\n{}", msg);

    ChannelEncryptResult result =
        msg.getMsgBody().orElseThrow(() -> new RuntimeException("No body found in message"));

    log.debug("ChannelEncryptResult: \n{}", result);

    if (result.getResult() == EResult.OK) {
      this.socket.setSessionKey(this.sessionKey);
      log.info("Channel encryption successful");
    } else {
      log.error("Channel encryption failed");
    }
  }

  private void onMulti(AbstractMessage<ProtoHeader, CMsgMulti> msg) {
    log.info("Received multi message:\n{}", msg);

    CMsgMulti multi =
        msg.getMsgBody().orElseThrow(() -> new RuntimeException("No body found in message"));

    byte[] messages = multi.getMessageBody().toByteArray();
    if (multi.getSizeUnzipped() != 0) {
      messages = ZipUtils.unzip(multi.getMessageBody().toByteArray());
      log.info("Decompressed message body: {} bytes", messages.length);
    }

    int index = 0;
    while (index < messages.length) {
      int messageSize =
          Serializer.unpack(
              ArrayUtils.subarray(messages, index, 4), ByteBuffer::getInt, ByteOrder.LITTLE_ENDIAN);

      byte[] message = ArrayUtils.subarray(messages, index + 4, messageSize);
      socket.onRawMessage(message);
      index += 4 + messageSize;
    }
  }

  protected void connect() {
    if (socket.isConnected()) {
      log.debug("Tried to connect to Steam CM server while already connected");
      return;
    }

    Collections.shuffle(cmList);
    for (SteamCMServer server : cmList) {
      try {
        socket.setSessionKey(null);
        socket.connect(server.getHost(), server.getPort());
        log.info("Connected to Steam CM server: {}", server);
        break;
      } catch (Exception e) {
        log.error("Failed to connect to Steam CM server: {}", server);
      }
    }

    waitForMessage(EMsg.k_EMsgChannelEncryptResult_VALUE);
  }

  public boolean isConnected() {
    return socket.isConnected();
  }

  public InetAddress getLocalAddress() {
    return socket.getLocalAddress();
  }

  public void disconnect() {
    socket.disconnect();
  }

  @Override
  public HasListenerGroup getInstance() {
    return this.socket;
  }
}
