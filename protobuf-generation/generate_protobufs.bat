@echo off
set protobufs_path=.\protobufs
set protobuf_lib_path=.\protobuf-lib\src
set output_path=.\generated_proto

@if not exist %output_path% mkdir %output_path%

for %%i in (%protobufs_path%\*.proto) do (
    @echo Running Protocol Buffer Compiler on %%~nxi...
    @protoc.exe --proto_path=%protobufs_path% --proto_path=%protobuf_lib_path% --java_out=%output_path% %%~nxi
)