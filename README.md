# AirlineFetcher

This project is a Kotlin application that fetches and processes aircraft data using the [AeroDataBox API](https://rapidapi.com/aedbx-aedbx/api/aerodatabox).

## Technologies Used

- Kotlin
- Java
- Gradle
- H2 Database
- OkHttp
- Gson
- HikariCP
- kotlinx-coroutines-core

## Setup

To run this project, you need to have Kotlin and Gradle installed on your machine.

1. Clone the repository:

```bash
git clone https://github.com/SinonCute/AirlineFetcher.git
```

2. Navigate to the project directory:

```bash
cd AirlineFetcher
```

3. Build the project:

```bash
gradle build
```

4. Run the project:

```bash
gradle run
```

## Features

- Fetch aircraft data from the AeroDataBox API
- Store aircraft data in an H2 database
- Retrieve aircraft data from the database
- Display aircraft data in the console

## License
- Free to use and modify
