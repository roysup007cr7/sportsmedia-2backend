server:
  port: ${PORT:8080}

spring:
  application:
    name: sportsmedia

  datasource:
    url: ${DB_URL:jdbc:h2:file:./data/sportsmedia;DB_CLOSE_ON_EXIT=FALSE}
    username: ${DB_USER:sa}
    password: ${DB_PASS:}

  jpa:
    hibernate:
      ddl-auto: update
    open-in-view: false

  h2:
    console:
      enabled: true
      path: /h2-console

  jackson:
    serialization:
      write-dates-as-timestamps: false

app:
  jwt:
    secret: ${JWT_SECRET:change-this-to-a-long-random-secret-key-min-32-chars}
    expiry-hours: 12

  admin:
    username: ${ADMIN_USER:supriyo}
    password: ${ADMIN_PASS:Admin@12345}

  cors:
    allowed-origins: ${CORS_ORIGINS:http://localhost:5500,http://127.0.0.1:5500,https://*.vercel.app}

football:
  api:
    enabled: ${FOOTBALL_API_ENABLED:false}
    token: ${FOOTBALL_API_TOKEN:}
    base-url: https://api.football-data.org/v4
    competitions: PD,CL,PL,SA,BL1
    days-ahead: 14
    cron: "0 */30 * * * *"