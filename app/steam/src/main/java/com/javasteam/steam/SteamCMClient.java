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
import com.javasteam.steam.handlers.HasMessageHandler;
import com.javasteam.utils.common.ArrayUtils;
import com.javasteam.utils.common.CryptoUtils;
import com.javasteam.utils.common.ZipUtils;
import com.javasteam.utils.serializer.Serializer;
import com.javasteam.webapi.endpoints.steamdirectory.SteamWebDirectoryRESTAPIClient;
import com.javasteam.webapi.endpoints.steamdirectory.models.SteamCMServer;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * Basic Steam CM client that connects to a CM server and listens for messages. Handles the channel
 * encryption process and multi msg processing. The client can be used to send and receive messages
 * from the CM server. Uses the {@link TCPConnection} class for the connection.
 */
@Slf4j
public class SteamCMClient implements HasMessageHandler {
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
    ChannelEncryptRequest request = msg.getBody(ChannelEncryptRequest.class);

    this.sessionKey = CryptoUtils.getRandomBytes(32);
    log.trace("Generated session key: {}", Arrays.toString(sessionKey));

    byte[] encryptedSessionKey = Crypto.encryptSessionKey(sessionKey, request.getChallenge());
    int crc = CryptoUtils.calculateCRC32(encryptedSessionKey);

    ChannelEncryptResponse res = new ChannelEncryptResponse(1, 128, encryptedSessionKey, crc, 0);
    var response = Message.of(MessageHeader.of(EMsg.k_EMsgChannelEncryptResponse_VALUE), res);

    this.sendMessage(response);
  }

  private void onChannelEncryptResult(AbstractMessage<MessageHeader, ChannelEncryptResult> msg) {
    ChannelEncryptResult result = msg.getBody(ChannelEncryptResult.class);

    if (result.getResult() == EResult.OK) {
      this.socket.setSessionKey(this.sessionKey);
      log.debug("Channel encryption successful");
    } else {
      log.error("Channel encryption failed");
    }
  }

  private void onMulti(AbstractMessage<ProtoHeader, CMsgMulti> msg) {
    log.debug("Received multi message:\n{}", msg);

    CMsgMulti multi = msg.getBody(CMsgMulti.class);

    byte[] messages = multi.getMessageBody().toByteArray();
    if (multi.getSizeUnzipped() != 0) {
      messages = ZipUtils.unzip(multi.getMessageBody().toByteArray());
      log.trace("Decompressed message body: {} bytes", messages.length);
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
  public HasMessageHandler getInstance() {
    return this.socket;
  }
}
