# SKSE Inventory Management System

A comprehensive inventory management system for SKSE Footwear Manufacturing, built with Spring Boot and Thymeleaf.

## Features

### 🏭 Manufacturing Process Management
- **Plans Management**: Create and track production plans with article, color, and size-quantity specifications
- **Process Flow**: Track plans through cutting → printing → stitching → completion → machine processing
- **Vendor Assignment**: Assign vendors for cutting, printing, and stitching tasks
- **Final Quantity Tracking**: Record actual quantities produced vs planned quantities

### 📦 Stock Management
- **Upper Stock**: Track stock after cutting and stitching (before machine processing)
- **Finished Stock**: Track completed products after machine processing
- **Stock Movements**: Record all stock movements with timestamps
- **Stock Valuation**: Calculate total value of stock based on selling prices

### 👥 Vendor Management
- **Vendor Profiles**: Manage vendor information, roles, and contact details
- **Payment Tracking**: Track payments due to vendors for each task
- **Payment History**: Maintain complete payment and order history
- **Payment Reports**: Generate payment summaries and reports

### 📊 Business Intelligence
- **Dashboard Overview**: Real-time view of active plans, stock levels, and payments
- **Status Tracking**: Monitor plan progress through different manufacturing stages
- **Financial Reports**: Track vendor payments and stock valuations

## System Architecture

### Core Entities

1. **Article**: Slipper models with cost breakdowns
   - Name, cutting cost, printing cost, stitching cost, selling price

2. **Plan**: Production orders
   - Article, color, size-quantity pairs, vendors, status tracking
   - Progress through: Pending_Cutting → Cutting → Pending_Printing → Printing → Pending_Stitching → Stitching → Completed

3. **Vendor**: Service providers
   - Roles: Cutting, Printing, Stitching
   - Payment tracking and history

4. **Stock**: Inventory management
   - UpperStock: After cutting/stitching, before machine
   - FinishedStock: After machine processing
   - StockMovementRequest: Track all movements

### Key Workflows

#### Plan Creation and Execution
1. Create plan with article, color, and size-quantity specifications
2. Assign vendors for cutting, printing, and stitching
3. Move plan through statuses as work progresses
4. Record final quantities during cutting
5. Send to machine processing (moves stock from upper to finished)

#### Stock Management
1. Upper stock created when cutting is completed
2. Stock moved to finished when sent to machine
3. All movements tracked with timestamps and quantities

#### Vendor Payment Management
1. Payment amounts calculated based on quantities and article costs
2. Payments recorded when tasks are completed
3. Comprehensive payment history and reporting

## Technology Stack

- **Backend**: Spring Boot 3.3.5, Spring Data JPA, Spring Web
- **Database**: MySQL
- **Frontend**: Thymeleaf templates with modern CSS
- **Build Tool**: Maven
- **Java Version**: 17

## Database Configuration

The system uses MySQL database. Configure your database connection in `application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/inventory_db
spring.datasource.username=your_username
spring.datasource.password=your_password
```

## Running the Application

1. **Prerequisites**:
   - Java 17 or higher
   - MySQL database
   - Maven

2. **Setup Database**:
   ```sql
   CREATE DATABASE inventory_db;
   CREATE USER 'inventory_user'@'localhost' IDENTIFIED BY 'admin';
   GRANT ALL PRIVILEGES ON inventory_db.* TO 'inventory_user'@'localhost';
   FLUSH PRIVILEGES;
   ```

3. **Run Application**:
   ```bash
   mvn spring-boot:run
   ```

4. **Access Application**:
   - Main application: http://localhost:8080
   - API Documentation: http://localhost:8080/swagger-ui.html

## Key Endpoints

### Web Interface
- `/` - Home dashboard
- `/plans` - Plans management
- `/stock` - Stock dashboard
- `/vendors` - Vendor management
- `/vendors/summary` - Payment summary
- `/articles` - Article management

### API Endpoints
- `/api/plans` - Plans API
- `/api/stock` - Stock API
- `/api/vendors` - Vendors API
- `/api/articles` - Articles API

## Business Logic Improvements

### Enhanced Plan Management
- ✅ Proper vendor relationships instead of string IDs
- ✅ Final quantity tracking for actual vs planned production
- ✅ Machine processing workflow
- ✅ Comprehensive status tracking with timestamps

### Improved Stock Management
- ✅ Stock movement tracking
- ✅ Upper to finished stock conversion
- ✅ Stock valuation calculations
- ✅ Movement history with timestamps

### Vendor Payment System
- ✅ Complete payment tracking
- ✅ Payment history and reports
- ✅ Role-based vendor management
- ✅ Payment summaries and analytics

### User Interface
- ✅ Modern, responsive design
- ✅ Comprehensive dashboards
- ✅ Intuitive navigation
- ✅ Real-time status updates

## Future Enhancements

1. **Advanced Analytics**: Sales forecasting, demand planning
2. **Barcode Integration**: QR codes for stock tracking
3. **Mobile App**: Mobile interface for shop floor operations
4. **Integration**: ERP system integration
5. **Notifications**: Email/SMS alerts for low stock, pending payments
6. **Multi-location**: Support for multiple manufacturing units

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## License

This project is proprietary software for SKSE slipper manufacturing business.

---

**Note**: This system is designed specifically for slipper manufacturing workflow. The business logic and data models are tailored to the specific requirements of cutting, printing, stitching, and machine processing operations. 