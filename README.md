# Air Quality Monitoring & Analysis Desktop Application

A modular, multithreaded Java desktop application designed to fetch, process, and visualize real-time environmental metrics and air pollution data from public monitoring APIs.

---

## Features
- **Real-Time Data Ingestion:** Connects to public environmental REST APIs to retrieve real-time air quality measurements across monitoring stations.
- **Pollution Index Tracking:** Ingests and monitors key particulate and gaseous metrics, including $PM_{10}$, $PM_{2.5}$, $O_3$, $NO_2$, and $CO$.
- **Multithreaded Architecture:** Implements asynchronous data fetching and background network processing to maintain a responsive, non-blocking user interface.
- **Robust Error Handling:** Features fault-tolerant JSON deserialization and graceful degradation during network timeouts or missing sensor data.

---

## Architecture & OOP Design
- **MVC Pattern:** Strict separation of concerns between data models, business logic services, and the presentation layer.
- **Clean Code Principles:** Modular structure leveraging encapsulation, inheritance, and clean interfaces for data retrieval services.

---

## Tech Stack
- **Language:** Java 17+
- **GUI Framework:** JavaFX
- **Build & Dependency Tool:** Maven
- **Protocols & Formats:** REST APIs, JSON
