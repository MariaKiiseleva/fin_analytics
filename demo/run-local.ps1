$env:SPRING_PROFILES_ACTIVE = "local"
Write-Host "Starting Spring Boot with SPRING_PROFILES_ACTIVE=$env:SPRING_PROFILES_ACTIVE"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
