# Web Server Project

This project is a simple web server built using Java. It demonstrates how to set up a basic web server and configure it using properties.

## Project Structure

```
web-server
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com
│   │   │       └── example
│   │   │           └── App.java
│   │   └── resources
│   │       └── application.properties
├── pom.xml
└── README.md
```

## Requirements

- Java 11 or higher
- Maven

## Building the Project

To build the project, navigate to the project directory and run:

```
mvn clean install
```

## Running the Web Server

After building the project, you can run the web server using the following command:

```
mvn exec:java -Dexec.mainClass="com.example.App"
```

## Configuration

The server configuration can be modified in the `src/main/resources/application.properties` file. You can set properties such as the server port and context path.

## License

This project is licensed under the MIT License.