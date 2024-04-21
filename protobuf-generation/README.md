## Protobuf generation

---

### Usage

1. [OPTIONAL] For latest protobuf files you can install them
   from [here](https://github.com/SteamDatabase/Protobufs/tree/master/steam)
   into ``protobuf-generation/protobufs`` folder
2. Install protoc and protobuf lib from [here](https://github.com/protocolbuffers/protobuf/releases)
   into ``protobuf-generation/protoc.exe`` and ``protobuf-generation/protobuf-lib``
3. Run generate_protobufs.bat
4. Generated files will be in ``protobuf-generation/generated`` folder

---

### Notes

- protoc version must match the version of the protobuf lib
- game_coordinator_messages.proto is not included in the steam protobuf files. It must be manually added. It should
  contain
  messages needed for CMsgProtoBufHeader. Needed messages can be found
  for example
  from [CS2 protobufs](https://github.com/SteamDatabase/GameTracking-CS2/blob/master/Protobufs/steammessages.proto)
  or [Dota 2 protoufs](https://github.com/SteamDatabase/GameTracking-Dota2/blob/master/Protobufs/steammessages.proto)