java-virtual-threads-poc

Requirements
- JDK 21+ (Virtual Threads are available in Java 21)

Run
1) Compile
   javac -d out src/main/java/poc/*.java

2) Run benchmark demo (platform vs virtual threads)
   java -cp out poc.VirtualThreadsPoc

3) Run HTTP server demo (each request handled by a virtual thread)
   java -cp out poc.VirtualThreadHttpServer

4) In another terminal, hit the server
   curl "http://localhost:8080/hello?name=world"
   curl "http://localhost:8080/sleep?ms=200"

Notes
- The benchmark uses Thread.sleep to simulate blocking I/O and shows how virtual threads can handle large concurrency with low overhead.
- The server uses the built-in com.sun.net.httpserver.HttpServer and dispatches requests to an Executor created with Executors.newVirtualThreadPerTaskExecutor().
