package com.javasteam.steam.connection;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** Connection context that holds the socket, writer and reader for a connection. */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ConnectionContext {
  private final Socket socket;
  private final DataOutputStream writer;
  private final DataInputStream reader;

  public static ConnectionContext create(Socket socket) {
    try {
      return new ConnectionContext(
          socket,
          new DataOutputStream(socket.getOutputStream()),
          new DataInputStream(socket.getInputStream()));
    } catch (IOException e) {
      throw new RuntimeException("Failed to create connection context", e);
    }
  }

  public boolean isConnected() {
    return socket.isConnected() && !socket.isClosed();
  }

  public void close() {
    try {
      socket.close();
    } catch (IOException e) {
      throw new RuntimeException("Failed to close connection context", e);
    }
  }
}
