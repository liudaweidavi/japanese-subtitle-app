cmake_minimum_required(VERSION 3.22)
project("whisper-jni")

set(WHISPER_SRC_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../../../../whisper.cpp)

# Disable whisper.cpp unnecessary builds
set(WHISPER_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(WHISPER_ALL_WARNINGS OFF CACHE BOOL "" FORCE)
set(WHISPER_ALL_WARNINGS_3RD_PARTY OFF CACHE BOOL "" FORCE)

# Disable ggml features not needed on Android
set(GGML_ALL_WARNINGS OFF CACHE BOOL "" FORCE)
set(GGML_CCACHE OFF CACHE BOOL "" FORCE)
set(GGML_NATIVE OFF CACHE BOOL "" FORCE)

# Include whisper.cpp as subdirectory (handles ggml + whisper sources)
add_subdirectory(${WHISPER_SRC_DIR} whisper.cpp)

# Add whisper-jni shared library
add_library(whisper-jni SHARED
    whisper-jni.cpp
)

# Link against whisper.cpp (which transitively includes ggml)
target_link_libraries(whisper-jni
    whisper
    log
    android
)

# Compile options for JNI layer
target_compile_options(whisper-jni PRIVATE
    -O3
    -fPIC
    -Wall
    -Wextra
    -Wno-unused-parameter
)
