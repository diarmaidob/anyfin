# Anyfin

A Jellyfin client built with modern development practices.

<p align="center">
<table align="center" width="100%">
  <tr>
    <td width="20%" align="center">
      <img src="https://github.com/user-attachments/assets/02e51ee2-5071-4aab-9283-5619d0daf80d" width="100%" />
    </td>
    <td width="20%" align="center">
      <img src="https://github.com/user-attachments/assets/7e2da941-fdce-405a-9ad3-0188c6589caa" width="100%" />
    </td>
    <td width="20%" align="center">
      <img src="https://github.com/user-attachments/assets/4f91778c-22e3-4b17-a315-2b74419cc742" width="100%" />
    </td>
    <td width="20%" align="center">
      <img src="https://github.com/user-attachments/assets/4c86eb30-0891-46ca-90d8-fb4be7ca1437" width="100%" />
    </td>
    <td width="20%" align="center">
      <img src="https://github.com/user-attachments/assets/3665212b-181b-444e-b666-24c9103f233c" width="100%" />
    </td>
  </tr>
</table>
</p>

## Overview

Anyfin is a Jellyfin client which hopes to play anything on anything. This is currently version 0.1 and represents the initial implementation.
It's currently only Android but the next stage is to port to Kotlin Multiplatform and add desktop and iOS apps.

## Features

- Media playback using ExoPlayer
- Modern UI built with Jetpack Compose
- Local data persistence with SQLDelight
- Network communication via Retrofit

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose, Material3
- **Architecture:** MVVM with Hilt for dependency injection
- **Database:** SQLDelight
- **Networking:** Retrofit, OkHttp, Moshi
- **Media:** ExoPlayer (Media3)
- **Image Loading:** Coil

## Current Status

Version 0.1 - Early development stage with core functionality implemented.

## Future Roadmap

- Migrate to Kotlin Multiplatform (KMP) to support additional platforms
- Introduce proper cache based queries
- Search/discover & request
- Server management etc
- UI/UX improvements

## Development

Built following Android development best practices with a focus on clean architecture and maintainable code.
