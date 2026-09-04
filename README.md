# Seen

Seen is an Android watchlist app for keeping track of anime, K-dramas, TV shows, and movies.

## Features

- Search for anime through AniList.
- Search for TV shows, movies, and K-dramas through TVmaze.
- Keep separate watching and watched lists.
- Track episode progress and update episode counts in the background.
- Undo recent additions to a watched list.
- Store watchlists locally on the device.

## Requirements

- Android Studio with Android SDK 35 installed
- JDK 17 or newer
- An Android device or emulator running API 24 or newer

## Build and test

Clone the repository and open it in Android Studio. Allow Gradle to sync, then run the `app` configuration on a device or emulator.

From a terminal at the project root:

```text
./gradlew test
./gradlew assembleDebug
```

On Windows, use `gradlew.bat` instead of `./gradlew`.

The generated debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Data sources

Seen uses these public services for search and episode information:

- [AniList GraphQL API](https://anilist.co/graphiql)
- [TVmaze API](https://www.tvmaze.com/api)

Their availability and terms may change independently of this project.

## Project structure

- `app/src/main/java/com/aareno/seen/data` contains Room databases, repositories, and background work.
- `app/src/main/java/com/aareno/seen/ui` contains the anime, K-drama, and TV/movie screens.
- `app/src/main/res` contains Android layouts, drawables, and themes.

## Contributing

Bug reports and pull requests are welcome. Please include the Android version, device or emulator details, and steps to reproduce when reporting a problem.

## License

No license has been selected for this project yet. Until a license is added, all rights are reserved by the copyright holder.
