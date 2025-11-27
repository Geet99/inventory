# Monthly Vendor Payment Settlement System

## 🎯 Overview

The system has been revamped to handle vendor payments on a **monthly settlement basis**. Instead of tracking a cumulative total, payments are now tracked by month and settled monthly.

---

## 🔑 Key Changes

### 1. **Monthly Tracking**
- Payments are tracked per month using format `"MMyyyy"` (e.g., "012025" for January 2025)
- Each vendor has separate monthly payment records for each operation type
- Each month shows: Total Due, Paid Amount, and Balance

### 2. **Completion Date Accounting**
- **Critical Rule**: Payment is added to the month when work is **completed**, not started
- **Example**: 
  - Plan cutting started: January 30th
  - Plan cutting finished: February 2nd
  - **Result**: Payment added to February's account ✅

### 3. **Vendor Summary Changes**
- **Old**: Shows total cumulative due across all time
- **New**: Shows **current month's due** prominently
- Monthly breakdown available

### 4. **Payment Recording**
- **Old**: Record payment against total due
- **New**: Record payment for **previous month** (last month's settlement)
- Tracks payment status: PENDING, PARTIAL, PAID

---

## 🏗️ New Database Structure

### VendorMonthlyPayment Entity

```java
@Entity
public class VendorMonthlyPayment {
    private Long id;
    private Vendor vendor;
    private String monthYear;          // Format: "MMyyyy"
    private VendorRole operationType;  // Cutting, Printing, Stitching
    private double totalDue;           // Total amount due for this month
    private double paidAmount;         // Amount already paid
    private PaymentStatus status;      // PENDING, PARTIAL, PAID
    private LocalDate createdDate;
    private LocalDate lastUpdatedDate;
}
```

### PaymentStatus Enum

```java
public enum PaymentStatus {
    PENDING,  // No payment made yet
    PARTIAL,  // Some payment made
    PAID      // Fully paid
}
```

---

## 📊 How It Works

### Recording Vendor Orders (By Completion Date)

**Old Method:**
```java
vendorService.recordVendorOrder(vendorId, planNumber, amount, role);
// Added to cumulative total immediately
```

**New Method:**
```java
vendorService.recordVendorOrderToMonth(
    vendorId, 
    planNumber, 
    amount, 
    role,
    completionDate  // The key difference!
);
// Added to the MONTH when work was completed
```

### Example Workflow

```
Plan Timeline:
- Cutting Started: Jan 28, 2025
- Cutting Finished: Feb 3, 2025
- Printing Finished: Feb 15, 2025  
- Stitching Finished: Feb 28, 2025

Monthly Accounts:
January 2025:  ₹0 (nothing completed)
February 2025: 
  - Cutting: ₹1,000 (completed Feb 3)
  - Printing: ₹800 (completed Feb 15)
  - Stitching: ₹1,200 (completed Feb 28)
  Total: ₹3,000
```

---

## 🔄 Integration with PlanService

### Updated State Transitions

```java
case Pending_Printing:
    plan.setCuttingEndDate(LocalDate.now());
    if (plan.getCuttingVendor() != null) {
        double payment = calculatePayment(plan, VendorRole.Cutting);
        // Use cutting END date for monthly accounting
        vendorService.recordVendorOrderToMonth(
            plan.getCuttingVendor().getId(),
            planNumber,
            payment,
            VendorRole.Cutting,
            plan.getCuttingEndDate()  // Completion date!
        );
    }
    break;
```

Same pattern for Printing and Stitching completion.

---

## 📱 UI Changes

### Vendor Summary Page (`/vendors/summary`)

**Shows:**
- **Current Month Header**: "Payment Due - January 2025"
- **Current Month Total**: ₹15,000
- **Breakdown by Operation**: 
  - Cutting: ₹5,000
  - Printing: ₹4,000
  - Stitching: ₹6,000
- **All Vendors**: Current month dues per vendor

### Vendor Details Page (`/vendors/{id}/details`)

**Shows:**
- **Current Month Section**: 
  - Total due this month
  - Breakdown by operation type
  - Status (PENDING/PARTIAL/PAID)

- **Previous Month Section**:
  - Last month's dues
  - Settlement status

- **Monthly History**:
  - All past months
  - Payment records for each

### Payment Recording Page (`/vendors/{id}/payment`)

**Shows:**
- **Previous Month Dues**: "Settlement for December 2024"
- **Amount Due**: ₹12,000
- **Breakdown**:
  - Cutting: ₹4,000
  - Printing: ₹3,500
  - Stitching: ₹4,500
- **Payment Form**: Record payment for last month

---

## 🎯 Key Methods in VendorService

### 1. Record Order to Month
```java
void recordVendorOrderToMonth(
    Long vendorId, 
    String planNumber, 
    double amount,
    VendorRole role, 
    LocalDate completionDate
)
```
- Adds payment to the month based on completion date
- Creates monthly record if doesn't exist
- Updates status automatically

### 2. Record Monthly Payment
```java
void recordMonthlyPayment(
    Long vendorId, 
    String monthYear,
    VendorRole operationType, 
    double amount, 
    String planNumber
)
```
- Records payment for a specific month
- Updates paid amount and status
- Creates payment history record

### 3. Get Current Month Due
```java
double getCurrentMonthDue(Long vendorId)
```
- Returns total balance for current month
- Sums across all operation types

### 4. Get Previous Month Due
```java
double getPreviousMonthDue(Long vendorId)
```
- Returns total balance for previous month
- Used in payment recording

### 5. Get Monthly Summary
```java
Map<String, Object> getMonthlyPaymentSummary()
```
- Returns current month summary for all vendors
- Includes breakdown by operation type

### 6. Get Vendor Monthly Summary
```java
Map<String, Object> getVendorMonthlySummary(Long vendorId)
```
- Returns complete monthly breakdown for one vendor
- Includes current month, previous month, and history

---

## 💡 Business Logic

### Monthly Settlement Cycle

```
Month End (e.g., Jan 31):
1. Month closes with total dues calculated
2. Status shows as PENDING

Next Month (e.g., Feb 5):
3. Accountant reviews previous month (January)
4. Records payment via payment form
5. Status updates to PARTIAL or PAID

Current Month (February):
6. New work completions add to February
7. February balance accumulates
8. Will be settled in March
```

### Status Transitions

```
PENDING → PARTIAL: When first payment made (paid < total)
PARTIAL → PAID: When fully settled (paid >= total)
PENDING → PAID: When full payment made at once
```

---

## 🔒 Data Integrity

### Backward Compatibility
- Legacy `paymentDue` field in Vendor still maintained
- Old methods still work alongside new ones
- Gradual migration possible

### Automatic Status Updates
- Status calculated automatically when amounts change
- No manual status management needed

### Completion Date Accuracy
- Uses plan end dates (cuttingEndDate, printingEndDate, stitchingEndDate)
- Ensures accurate monthly accounting
- Reflects when work was actually completed

---

## 📈 Reporting Capabilities

### Current Month Report
- Who owes what this month
- Breakdown by vendor and operation
- Real-time balance tracking

### Previous Month Settlement
- What needs to be paid now
- Outstanding balances from last month
- Payment deadlines

### Historical Analysis
- Month-over-month trends
- Vendor payment patterns
- Operation-wise cost analysis

---

## 🚀 Example Scenarios

### Scenario 1: Cross-Month Completion
```
Plan A - Cutting:
- Started: Jan 30, 2025
- Finished: Feb 2, 2025
- Amount: ₹1,000

Result:
- January dues: ₹0
- February dues: ₹1,000 ✅
```

### Scenario 2: Multiple Operations
```
Plan B:
- Cutting finished: Feb 5 → ₹1,000 to February
- Printing finished: Feb 12 → ₹800 to February
- Stitching finished: Mar 2 → ₹1,200 to March ✅

Result:
- February: ₹1,800
- March: ₹1,200
```

### Scenario 3: Partial Payment
```
Vendor X - February 2025:
- Total Due: ₹10,000
- Payment 1 (Feb 28): ₹6,000 → Status: PARTIAL
- Payment 2 (Mar 5): ₹4,000 → Status: PAID ✅
```

---

## 🔗 API Integration

### REST Endpoints (can be added)
```
GET  /api/vendors/{id}/monthly-payments
POST /api/vendors/{id}/monthly-payments/{monthYear}/payment
GET  /api/monthly-payments/summary
GET  /api/monthly-payments/{monthYear}
```

---

## ✅ Benefits

### 1. **Cleaner Accounting**
- Monthly books are clear
- Easy month-end reconciliation
- Audit trail by month

### 2. **Accurate Cash Flow**
- Know exactly what's due when
- Plan payments by month
- Better financial forecasting

### 3. **Fair Vendor Relations**
- Payments tied to completion, not start
- Clear monthly statements
- Transparent settlement process

### 4. **Better Reporting**
- Monthly expense tracking
- Operation-wise cost analysis
- Historical trends

---

## 🎓 Developer Notes

### Adding New Features

**To add quarterly settlements:**
```java
public double getQuarterlyDue(Long vendorId, int quarter, int year) {
    // Sum 3 months for the quarter
}
```

**To add advance payments:**
```java
public void recordAdvancePayment(Long vendorId, double amount) {
    // Apply to current month with negative amount
}
```

### Database Queries

**Get all pending months:**
```java
List<VendorMonthlyPayment> pending = 
    repository.findByVendorAndStatus(vendor, PaymentStatus.PENDING);
```

**Get year total:**
```java
List<VendorMonthlyPayment> yearPayments = 
    repository.findByVendorAndMonthYearStartingWith(vendor, "2025");
```

---

## 📋 Migration Notes

### For Existing Systems

1. **Phase 1**: Deploy new code (backward compatible)
2. **Phase 2**: Start using monthly methods for new plans
3. **Phase 3**: Migrate historical data (optional)
4. **Phase 4**: Update all UI to show monthly breakdown

### Data Migration Script (if needed)
```sql
-- Migrate existing VendorOrderHistory to VendorMonthlyPayment
INSERT INTO vendor_monthly_payment (vendor_id, month_year, operation_type, total_due, paid_amount, status)
SELECT 
    vendor_id,
    DATE_FORMAT(order_date, '%m%Y') as month_year,
    role as operation_type,
    SUM(amount) as total_due,
    0 as paid_amount,
    'PENDING' as status
FROM vendor_order_history
WHERE type = 'ORDER'
GROUP BY vendor_id, DATE_FORMAT(order_date, '%m%Y'), role;
```

---

**Implementation Date:** November 26, 2025  
**Status:** ✅ Complete and Ready for Testing  
**Breaking Changes:** None (backward compatible)  
**Migration Required:** No (optional)

