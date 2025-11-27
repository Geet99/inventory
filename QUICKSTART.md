# 🚀 Quick Start Guide

## Temporary Database Setup (H2)

Your application is now configured with an **in-memory H2 database** for quick development and testing!

### What's Been Set Up:

✅ **H2 Database dependency** added to `pom.xml`  
✅ **Development profile** (`application-dev.properties`) configured with H2  
✅ **Production profile** (`application-prod.properties`) configured with MySQL  
✅ **Default profile** set to `dev` for instant startup  

---

## 🎯 Running the Application

### Start with H2 (No Database Setup Required!)

```bash
mvn spring-boot:run
```

That's it! The application will:
- Start on port 8080
- Create an in-memory database automatically
- Initialize all tables based on your JPA entities
- Reset data on each restart (great for testing!)

---

## 🌐 Access Points

Once running, access:

| Service | URL | Description |
|---------|-----|-------------|
| **Main App** | http://localhost:8080 | Home dashboard |
| **H2 Console** | http://localhost:8080/h2-console | Database browser |
| **Swagger UI** | http://localhost:8080/swagger-ui.html | API documentation |

### H2 Console Login Credentials:

```
JDBC URL:  jdbc:h2:mem:inventory_db
Username:  sa
Password:  (leave empty)
```

---

## 🔄 Switching Between Databases

### Use H2 (Development - Default)
```bash
mvn spring-boot:run
```

### Use MySQL (Production)
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

Or modify `application.properties`:
```properties
spring.profiles.active=prod
```

---

## 📋 Configuration Files

| File | Purpose |
|------|---------|
| `application.properties` | Main config - sets active profile |
| `application-dev.properties` | H2 in-memory database config |
| `application-prod.properties` | MySQL database config |

---

## 💡 Tips

### Data Persistence
- **H2 (dev)**: Data is lost when you stop the application (great for testing!)
- **MySQL (prod)**: Data persists across restarts

### View Database Schema
1. Start the application
2. Go to http://localhost:8080/h2-console
3. Connect using the credentials above
4. Browse your tables and data

### Debug SQL Queries
Both profiles have SQL logging enabled:
```properties
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

Watch the console output to see all SQL queries!

---

## 🔧 Troubleshooting

### Port Already in Use
```bash
# Find process on port 8080
lsof -i :8080

# Kill it (replace PID with actual process ID)
kill -9 PID
```

### Maven Issues
```bash
# Clean and rebuild
mvn clean install

# Skip tests if needed
mvn clean install -DskipTests
```

### H2 Console Not Loading
Make sure you're using the dev profile:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

---

## 📚 Next Steps

1. ✅ Start the application with `mvn spring-boot:run`
2. ✅ Visit http://localhost:8080 to see your inventory system
3. ✅ Check http://localhost:8080/h2-console to browse the database
4. ✅ Use http://localhost:8080/swagger-ui.html to explore the API
5. ✅ Add some test data and experiment!

---

**Happy Coding! 🎉**

*When you're ready for production, simply switch to the `prod` profile and MySQL will be used instead.*

