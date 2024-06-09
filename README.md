# JavaSteam

A library to interact with the Steam CM servers and the Steam API.

## Contribution

Feel free to contribute to this project by creating a fork and submitting a pull request.

## Bug reports

Please report any bugs or issues in the issue tracker. It is more than likely that there are bugs in the library.

## Auth sessions

Auth sessions are stored in home directory at `~/javasteam/sessions/${username}.bin`. This is a JSON file containing the
auth session for each user. The session files are encrypted using AES-256 with a key derived from the user's password.
The password is not stored in the session file.

An auth session is created if the user logs in with a password and sets **shouldRememberPassword** to be true. The
session is then stored in the session file. After this the user can log in with the session file and the password using
the ``LoginParameters.withSessionFile()`` method.

## Usage

```java
import com.javasteam.steam.LoginParameters;
import com.javasteam.steam.SteamClient;

import static com.javasteam.protobufs.EnumsClientserver.EMsg;

public class SteamClientExample {
    public static void main(String[] args) {
        // Create new client
        SteamClient steamClient = new SteamClient();

        // Add listeners (login is blocking, so we can add the listener before logging in
        steamClient.addMessageListener(
                EMsg.k_EMsgClientLogOnResponse_VALUE, message -> log.info("Logged in"));

        // Do login, LoginParameters supports multiple ways to login, this is the simplest 
        steamClient.
                steamClient.login(LoginParameters.with("username", "password"));
    }
}
```