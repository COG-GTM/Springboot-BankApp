# Agent notes for this repository

- Stack: Spring Boot 3 / Java 17, Thymeleaf, MySQL 8. App listens on http://localhost:8080.
- First time on a machine, run `demo/setup-demo.sh`. It installs JDK 17, Node 20, pulls MySQL 8 (Docker Hub, then `mirror.gcr.io` fallback), warms Maven and Playwright Chromium, and starts the app. It ends with a `READY` banner or a `SETUP FAILED: <step>` line.
- `demo/start-demo.sh` resets to an empty `bankappdb` and restarts the app (`--build` rebuilds the jar offline first).
- Maven Central rate-limits cloud VMs. Always build with `./mvnw -s demo/maven-settings.xml ...` (GCS mirror). CI should do the same.
- Docker Hub may rate-limit. Use `mirror.gcr.io/library/mysql:8.0` where you can (including CI service images).
- Start long-running processes with `setsid`, not plain `nohup`.
