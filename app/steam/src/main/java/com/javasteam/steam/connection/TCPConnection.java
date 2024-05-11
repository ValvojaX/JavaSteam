package com.javasteam.steam.connection;

import com.javasteam.models.HasReadWriteLock;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import lombok.extern.slf4j.Slf4j;

/**
 * TCP implementation of the {@link BaseConnection} class. This class is responsible for creating a
 * TCP connection to a remote host and sending and receiving data over the connection.
 */
@Slf4j
public class TCPConnection extends BaseConnection implements HasReadWriteLock {
  private ConnectionContext context;
  private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
  private static final int DEFAULT_SOCKET_TIMEOUT = 0;

  public TCPConnection() {
    super();
  }

  public TCPConnection(int threads) {
    super(threads);
  }

  public InetAddress getLocalAddress() {
    return getContext()
        .map(connectionContext -> connectionContext.getSocket().getLocalAddress())
        .orElse(null);
  }

  private Optional<ConnectionContext> getContext() {
    return withReadLock(() -> Optional.ofNullable(context).filter(ConnectionContext::isConnected));
  }

  public void connect(String host, int port) {
    connect(host, port, DEFAULT_SOCKET_TIMEOUT);
  }

  @Override
  public void connect(String host, int port, int timeout) {
    withWriteLock(
        () -> {
          try {
            Socket socket = new Socket(host, port);
            socket.setSoTimeout(timeout);
            context = ConnectionContext.create(socket);
          } catch (IOException e) {
            log.error("Failed to connect to {}:{}", host, port, e);
          }
        });
  }

  @Override
  public void disconnect() {
    withWriteLock(
        () -> {
          getContext()
              .ifPresent(
                  connectionContext -> {
                    try {
                      connectionContext.getSocket().close();
                    } catch (IOException e) {
                      log.error("Failed to close socket", e);
                    }
                  });
          context = null;
        });
  }

  @Override
  public boolean isConnected() {
    return getContext().map(ConnectionContext::isConnected).orElse(false);
  }

  @Override
  protected Optional<byte[]> readData(int length) {
    return getContext()
        .map(
            connectionContext -> {
              byte[] data = new byte[length];
              try {
                connectionContext.getReader().readFully(data);
                return data;
              } catch (SocketException | EOFException e) {
                connectionContext.close();
                log.warn("Connection closed or EOF received");
                return null;
              } catch (IOException e) {
                log.error("Failed to read data", e);
                return null;
              }
            });
  }

  @Override
  protected void writeData(byte[] data) {
    getContext()
        .ifPresent(
            connectionContext -> {
              try {
                connectionContext.getWriter().write(data);
                connectionContext.getWriter().flush();
              } catch (IOException e) {
                log.error("Failed to write data", e);
              }
            });
  }

  @Override
  public ReentrantReadWriteLock getLock() {
    return lock;
  }
}
