# Smart Carpool and Ride-Sharing System 🚗🌱

> **An intelligent, route-matching, and transparent fare-sharing web platform engineered for sustainable green mobility.**

Built with **Java 21/25**, **Spring Boot 3.3.5**, **Spring Data JPA & Hibernate**, **MySQL 8+**, **Spring Security 6**, **Thymeleaf**, and **OpenStreetMap with Leaflet.js**.

---

## 📋 Table of Contents
1. [Overview & Project Objectives](#1-overview--project-objectives)
2. [Technology Stack](#2-technology-stack)
3. [User Roles & Permissions](#3-user-roles--permissions)
4. [Smart Ride-Matching Algorithm](#4-smart-ride-matching-algorithm)
5. [Transparent Fare-Sharing System](#5-transparent-fare-sharing-system)
6. [Database Schema & Architecture](#6-database-schema--architecture)
7. [Installation & Windows Setup Guide](#7-installation--windows-setup-guide)
8. [MySQL Database Setup Instructions](#8-mysql-database-setup-instructions)
9. [Development Demo Accounts](#9-development-demo-accounts)
10. [Step-by-Step Demonstration Sequence](#10-step-by-step-demonstration-sequence)
11. [Testing & Verification](#11-testing--verification)

---

## 1. Overview & Project Objectives

The **Smart Carpool and Ride-Sharing System** addresses the critical challenge of commuter traffic congestion and individual vehicular travel expenses by intelligently matching private car owners (drivers) who have empty seats with fellow commuters (passengers) heading along similar transit routes.

### Key Capabilities:
- **Spatial Route Matching**: Evaluates route corridors, pickups, drop-offs, and intermediate stops using geodesic spherical mathematics (Haversine formula).
- **Proportional Expense Sharing**: Eliminates surge pricing and commercial cab markups. Calculates fuel and toll cost allocations per occupant.
- **Environmental Impact Auditing**: Tracks real-time carbon emissions ($kg\ CO_2$) prevented and commuter fuel money saved.
- **Atomic Concurrency Protection**: Utilizes database transactions and optimistic concurrency locking (`@Version`) to completely prevent seat race conditions and overbooking.
- **Community Governance**: Complete moderation suite with user status controls, driver performance ratings, and safety reporting.

---

## 2. Technology Stack

| Layer | Technology | Details |
|---|---|---|
| **Backend Runtime** | Java 21 LTS (Java 25 compatible) | Eclipse Adoptium OpenJDK |
| **Framework** | Spring Boot 3.3.5 | Spring MVC, Spring Data JPA, Spring Security 6 |
| **Persistence / ORM** | Hibernate 6.5 & Spring Data JPA | MySQL 8+ Dialect & H2 test profile |
| **Database** | MySQL 8.0+ | `ridesharing_db` (UTF-8 MB4) |
| **Security** | Spring Security 6 + BCrypt | Form-based auth, RBAC, CSRF, Session protection |
| **Frontend** | Thymeleaf 3, HTML5, CSS3, Vanilla JS | Modern carpool theme, responsive cards, modals |
| **Mapping & GIS** | Leaflet.js 1.9.4 & OpenStreetMap | Zero paid API keys required; route polylines & stop pins |
| **Build Tool** | Apache Maven 3.9.9 + Maven Wrapper | `mvnw.cmd` included for Windows |

---

## 3. User Roles & Permissions

The application implements three primary security roles enforced at both URL pattern and method authorization levels:

### A. 🎒 Passenger (`ROLE_PASSENGER`)
- Register and login securely.
- Search rides by origin, destination, date, and seat count.
- View ranked compatible rides with match scores and route maps.
- Itemized pre-booking fare preview and instant reservation.
- Monitor booking history and cancel reservations according to policy.
- Submit post-ride driver reviews and 1–5 star ratings.
- Track personal cumulative cost savings and $CO_2$ emission reductions.

### B. 🚗 Driver (`ROLE_DRIVER`)
- Register and manage personal passenger vehicles (Make, Model, Year, Plate, Capacity).
- Publish upcoming rides with origin, destination, date, departure time, seats, total trip expense, and intermediate pickup checkpoints.
- Configure driver's own expense contribution (0% to 90%).
- Real-time in-app dashboard showing incoming passenger bookings.
- Accept or reject passenger booking requests.
- Mark trips as completed (prompting passengers for reviews) or cancel trips (notifying passengers).
- View average rating score and written passenger reviews.

### C. 🛡️ Administrator (`ROLE_ADMIN`)
- Dedicated administrative dashboard accessible exclusively to admin accounts.
- View real-time platform statistics: registered users, active drivers, rides, bookings, and cancellation rates.
- Verified environmental savings audit (net $kg\ CO_2$ prevented and passenger fuel money saved).
- User governance: Activate or deactivate accounts with a single click.
- Community safety queue: Inspect and resolve passenger/driver conduct reports.

---

## 4. Smart Ride-Matching Algorithm

The smart matching algorithm is implemented in [`SmartRideMatchingService.java`](file:///c:/Users/Dell/Desktop/ridesharing/src/main/java/com/carpool/ridesharing/service/SmartRideMatchingService.java).

When a passenger searches for a ride:
1. **Candidate Retrieval**: Filters out cancelled, completed, full, or past rides.
2. **Geodesic Spatial Calculation**: Uses the **Haversine formula** to calculate exact spherical surface distance between passenger coordinates and ride waypoints:
   $$d = 2R \cdot \arcsin\left(\sqrt{\sin^2\left(\frac{\Delta\text{lat}}{2}\right) + \cos(\text{lat}_1)\cos(\text{lat}_2)\sin^2\left(\frac{\Delta\text{lon}}{2}\right)}\right)$$
3. **Multi-Criteria Scoring Engine**:

| Component | Weight | Calculation Basis |
|---|---|---|
| **Route Compatibility** | **50%** | Measures pickup-to-origin and dropoff-to-destination distance (including intermediate stops). Linearly decays up to 30 km. |
| **Departure-Time Alignment** | **20%** | Full 20 pts if departure is within 15 minutes of request; decays up to a 180-minute window. |
| **Cost Compatibility** | **20%** | Normalizes passenger per-seat cost against benchmark solo ride expenses. |
| **Seat Availability** | **10%** | Ratio of available seats ensuring comfortable travel without overcrowding. |

$$\text{Composite Match Score} = S_{\text{route}}(50\%) + S_{\text{time}}(20\%) + S_{\text{cost}}(20\%) + S_{\text{seats}}(10\%)$$

4. **Ranking & Explanation**: Candidates are sorted in descending order of compatibility score and presented with an itemized score breakdown (e.g. *94.5% Match: Route 48/50, Time 18/20, Cost 18/20, Seats 10/10*).

---

## 5. Transparent Fare-Sharing System

The fair expense allocation engine is implemented in [`FareSharingService.java`](file:///c:/Users/Dell/Desktop/ridesharing/src/main/java/com/carpool/ridesharing/service/FareSharingService.java).

### Cost Allocation Formula:
1. Driver defines estimated **Total Trip Expense** ($T$) (fuel, tolls, maintenance).
2. Driver configures **Driver Contribution %** ($D\%$).
3. Driver specifies **Passenger Capacity** ($N$).
4. The per-passenger seat fare ($F$) is calculated as:
   $$F = \frac{T \times (1.0 - \frac{D}{100})}{N}$$

### Concurrency & Overbooking Prevention:
- Database transactions (`@Transactional`) wrap all seat modifications.
- JPA `@Version` optimistic locking on the `Ride` entity ensures that if two users attempt to book the final available seat concurrently, one succeeds while the other receives an atomic seat rejection rather than causing an overbooked vehicle.

---

## 6. Database Schema & Architecture

The database schema is defined in [`mysql_setup.sql`](file:///c:/Users/Dell/Desktop/ridesharing/mysql_setup.sql) and automatically mapped by Hibernate entities:

```
[User] 1 ---- * [Vehicle]
  | 1
  +----------- * [Ride] 1 ---- * [RideStop]
  |                | 1
  |                +---------- * [Booking] 1 ---- 1 [Rating]
  |                                 |
  +----------- * [Booking] ---------+
  |
  +----------- * [Notification]
  |
  +----------- * [Report]
```

- **`users`**: User profile, hashed password, contact info, active status.
- **`roles` & `user_roles`**: Role-based access control mappings.
- **`vehicles`**: Vehicle make, model, year, capacity, unique license plate.
- **`rides`**: Origin/destination coordinates, departure date/time, total cost, seat capacity, version.
- **`ride_stops`**: Intermediate checkpoints along the trip route.
- **`bookings`**: Seat reservations, pickup/dropoff points, fare paid, status, version.
- **`ratings`**: Passenger reviews, 1–5 star scores, driver feedback.
- **`notifications`**: In-app event alerts with read tracking.
- **`reports`**: Moderation incident records for administrator review.

---

## 7. Installation & Windows Setup Guide

### Prerequisites
- **Operating System**: Windows 10 or 11
- **Java**: Java 21 or Java 25 (Temurin OpenJDK verified)
- **Maven**: Bundled Maven wrapper (`.\mvnw.cmd`) is pre-configured in the repository root.

To verify your Java installation in Windows PowerShell:
```powershell
java -version
```

---

## 8. MySQL Database Setup Instructions

### Step 1: Install MySQL Community Server
If MySQL is not yet installed:
```powershell
winget install Oracle.MySQL
```
*Or download the installer from [dev.mysql.com/downloads/installer/](https://dev.mysql.com/downloads/installer/).*

### Step 2: Start MySQL Service & Create Database
Open PowerShell as Administrator:
```powershell
Start-Service MySQL80
```
Open the MySQL command line client:
```powershell
mysql -u root -p
```
Run the setup script:
```sql
source c:/Users/Dell/Desktop/ridesharing/mysql_setup.sql;
```

### Step 3: Configure Environment Variables
You can specify your MySQL credentials in PowerShell before launching:
```powershell
$env:SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/ridesharing_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true"
$env:SPRING_DATASOURCE_USERNAME="root"
$env:SPRING_DATASOURCE_PASSWORD="your_mysql_password"
```

### ⚡ Quick Zero-Config Demonstration Mode (In-Memory H2)
If you wish to demonstrate or evaluate the application **immediately without starting MySQL**, simply launch with the `h2` profile:
```powershell
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=h2
```
This loads an in-memory MySQL-compatible database with all seed data populated instantly!

---

## 9. Development Demo Accounts

The application automatically seeds realistic demo data on initial launch ([`DataInitializer.java`](file:///c:/Users/Dell/Desktop/ridesharing/src/main/java/com/carpool/ridesharing/config/DataInitializer.java)):

| Role | Email Address | Password | Pre-seeded Resources |
|---|---|---|---|
| **Driver** | `driver@carpool.com` | `Driver123!` | Toyota Camry (XYZ-7890), 2 upcoming routes, past reviews |
| **Passenger** | `passenger@carpool.com` | `Passenger123!` | Active profile, past trip history, savings log |
| **Administrator** | `admin@carpool.com` | `Admin123!` | Access to `/admin/**` console, moderation tools, stats |

> 💡 **Quick Login Button**: On the login page (`/login`), click the **Demo Driver**, **Demo Passenger**, or **Demo Admin** buttons to autofill credentials instantly.

---

## 10. Step-by-Step Demonstration Sequence

Follow this exact sequence for your college project viva or demonstration:

1. **Start the Application**:
   ```powershell
   .\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=h2
   ```
   Open your browser to `http://localhost:8080`.

2. **Step 1: Driver Login & Vehicle Check**:
   - Go to `/login`, click **Demo Driver** (`driver@carpool.com` / `Driver123!`), and click **Sign In**.
   - Notice the driver lands on the **Driver Portal** (`/driver/dashboard`).
   - Click **My Vehicles** (`/driver/vehicles`) to view the registered Toyota Camry.

3. **Step 2: Publish a New Ride**:
   - Click **+ Publish Ride** (`/driver/rides/new`).
   - Enter Origin: `Downtown Transit Center`, Destination: `Tech Innovation Park`.
   - Add Stop: `University Campus`.
   - Set departure date for tomorrow at `08:30`.
   - Adjust the **Driver Contribution slider** and observe the live cost calculator update the passenger seat fare in real time!
   - Click **Publish Ride**.

4. **Step 3: Passenger Login & Smart Search**:
   - Log out, then log in as **Demo Passenger** (`passenger@carpool.com` / `Passenger123!`).
   - Click **Find Rides** (`/passenger/search`).
   - Enter Pickup: `Downtown Transit Center`, Destination: `Tech Innovation Park`.
   - Click **Find Matches**.
   - Inspect the **Ranked Matches**: View the **94.5% Match Score**, score breakdown pills (Route 48/50, Time 18/20, Cost 18/20, Seats 10/10), and click **View on Map** to see the route rendered on OpenStreetMap.

5. **Step 4: Transparent Booking & Overbooking Protection**:
   - Click **Book Seat(s)** on the top match card.
   - Review the financial comparison: Carpool share (\$3.75) vs Solo cab (\$9.38) with \$5.63 savings highlighted.
   - Click **Confirm & Book Seats**.
   - View your confirmed reservation under **Bookings** (`/passenger/bookings`).

6. **Step 5: Driver Approval & Trip Completion**:
   - Log back in as Driver (`driver@carpool.com`).
   - In the **Driver Dashboard**, view the new confirmed passenger booking.
   - Click **Manage** on the trip, then click **✓ Mark Completed**.

7. **Step 6: Submit Driver Rating**:
   - Switch back to Passenger (`passenger@carpool.com`).
   - On the **Bookings** page, click **Rate Driver ⭐**.
   - Select 5 Stars, enter feedback, and submit.
   - Visit the Driver's **Reviews** tab to see the updated average rating.

8. **Step 7: Administrator Governance & Environmental Audit**:
   - Log in as Admin (`admin@carpool.com` / `Admin123!`).
   - The Admin console opens directly at `/admin/dashboard`.
   - Inspect the live environmental impact: Total $kg\ CO_2$ avoided and commuter fuel money saved.
   - Explore `/admin/users` and toggle an account status to demonstrate user deactivation controls.

---

## 11. Testing & Verification

Run the comprehensive JUnit 5 and Spring Boot test suite from PowerShell:
```powershell
.\mvnw.cmd test
```

### Test Suite Summary:
- **`FareSharingServiceTest`**: Validates proportional cost sharing, driver contribution formulas, and passenger expense caps.
- **`SmartRideMatchingServiceTest`**: Validates Haversine spherical math, coordinate resolution, and multi-criteria ranked scoring.
- **`BookingConcurrencyAndOverbookingTest`**: Tests atomic seat reservation, seat count deductions, overbooking prevention, and seat returns upon cancellation.
- **`UserServiceTest`**: Tests BCrypt password hashing, role assignment, and duplicate email prevention.
- **`SecurityAndRoleAuthorizationTest`**: Verifies HTTP 403 Forbidden protection on `/admin/**` endpoints for non-admin roles and public endpoint accessibility.

```
[INFO] Results:
[INFO] Tests run: 20, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

## 12. Final Verification Checklist

- [x] Java 21/25 & Spring Boot 3.3.5 monolithic architecture configured.
- [x] Maven wrapper (`mvnw.cmd`) ready for execution without external global Maven.
- [x] MySQL schema script (`mysql_setup.sql`) and dual environment support (`application.properties` & `application-h2.properties`).
- [x] Role-Based Access Control (`ROLE_PASSENGER`, `ROLE_DRIVER`, `ROLE_ADMIN`) with URL lockdown.
- [x] Smart Ride-Matching algorithm with 4-factor scoring (50% Route, 20% Time, 20% Cost, 10% Seats).
- [x] OpenStreetMap & Leaflet.js interactive maps with intermediate stops and polylines.
- [x] Dynamic proportional fare-sharing calculator with real-time UI preview.
- [x] Concurrency-safe seat reservation with JPA optimistic locking (`@Version`).
- [x] In-app notification center for all ride and booking lifecycle transitions.
- [x] Post-trip driver review and 5-star rating system.
- [x] Admin dashboard with environmental audit ($CO_2$ and \$ savings) and user moderation.
- [x] Comprehensive JUnit 5 test suite passing with 0 failures and 0 errors.
