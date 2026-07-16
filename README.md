# The Kulture

- A nganya tracking app

## Local Development

Start the Spring Boot API first:

```bash
cd api
./mvnw spring-boot:run
```

The API serves JSON under `http://localhost:8080/api`.

Start the Angular app in another terminal:

```bash
cd app
npm start
```

Angular runs at `http://localhost:4200` and proxies `/api` requests to `http://localhost:8080`.

The backend also allows CORS from `localhost:4200` and `127.0.0.1:4200` for local development.
