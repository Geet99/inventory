# Rate Heads Feature Documentation

## 📋 Overview

The Rate Heads feature centralizes operation costs (Cutting, Printing, Stitching) across the entire inventory management system. This architectural improvement ensures that when rate costs are updated, all subsequent vendor payments automatically use the updated rates.

---

## 🎯 Key Features

### 1. **Centralized Cost Management**
- Define rate heads for each operation type (Cutting, Printing, Stitching)
- Each rate head has a name, operation type, cost, and active status
- Multiple rate heads can exist for the same operation type (e.g., "Standard Cutting", "Premium Cutting")

### 2. **Dynamic Rate Updates**
- When a rate head cost is updated, all future vendor payments automatically use the new cost
- Articles reference rate heads instead of storing static costs
- Payment calculations fetch current costs from active rate heads

### 3. **Article Integration**
- Articles now reference rate heads for each operation
- Legacy cost fields maintained for backward compatibility
- Article cost methods automatically return current rate head costs

### 4. **Plan Flexibility**
- Printing type can be changed during plan editing (before vendor assignment)
- Supports business workflow where printing decisions are made late
- Plan edit form includes both cutting type and printing type selection

---

## 🏗️ Architecture

### Database Schema

#### RateHead Entity
```java
@Entity
public class RateHead {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    
    @Enumerated(EnumType.STRING)
    private VendorRole operationType; // Cutting, Printing, or Stitching
    
    private Double cost;
    
    private boolean active = true;
}
```

#### Article Entity (Updated)
```java
@Entity
public class Article {
    // ... existing fields ...
    
    // Rate Head references
    @ManyToOne
    @JoinColumn(name = "cutting_rate_head_id")
    private RateHead cuttingRateHead;
    
    @ManyToOne
    @JoinColumn(name = "printing_rate_head_id")
    private RateHead printingRateHead;
    
    @ManyToOne
    @JoinColumn(name = "stitching_rate_head_id")
    private RateHead stitchingRateHead;
    
    // Legacy cost fields (for backward compatibility)
    private Double cuttingCost;
    private Double printingCost;
    private Double stitchingCost;
    
    // Getters return current rate head costs
    public Double getCuttingCost() {
        return cuttingRateHead != null ? cuttingRateHead.getCost() : cuttingCost;
    }
}
```

---

## 📂 New Components

### Backend

1. **Model**
   - `RateHead.java` - Entity for rate heads

2. **Repository**
   - `RateHeadRepository.java` - JPA repository with custom queries

3. **Service**
   - `RateHeadService.java` - Business logic for rate head operations

4. **Controllers**
   - `RateHeadController.java` - REST API endpoints
   - `RateHeadViewController.java` - Web UI endpoints

### Frontend

1. **Templates**
   - `rateheads/list.html` - List all rate heads
   - `rateheads/add.html` - Create new rate head
   - `rateheads/edit.html` - Edit existing rate head

2. **Updated Templates**
   - `articles/add.html` - Now uses rate head dropdowns
   - `articles/edit.html` - Now uses rate head dropdowns
   - `home.html` - Added Rate Heads navigation
   - `fragments/header.html` - Added Rate Heads to nav

---

## 🔌 API Endpoints

### REST API

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/rateheads` | Create new rate head |
| GET | `/api/rateheads` | Get all rate heads |
| GET | `/api/rateheads/{id}` | Get rate head by ID |
| GET | `/api/rateheads/operation/{type}` | Get rate heads by operation type |
| GET | `/api/rateheads/operation/{type}/active` | Get active rate heads by type |
| PUT | `/api/rateheads/{id}` | Update rate head |
| DELETE | `/api/rateheads/{id}` | Delete rate head |
| POST | `/api/rateheads/{id}/deactivate` | Deactivate rate head |

### Web UI

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/rateheads` | List all rate heads |
| GET | `/rateheads/new` | Show add form |
| POST | `/rateheads` | Save new rate head |
| GET | `/rateheads/edit/{id}` | Show edit form |
| POST | `/rateheads/update/{id}` | Update rate head |
| POST | `/rateheads/delete/{id}` | Delete rate head |
| POST | `/rateheads/deactivate/{id}` | Deactivate rate head |
| GET | `/rateheads/operation/{type}` | Filter by operation type |

---

## 📝 Usage Guide

### Creating a Rate Head

1. Navigate to **Rate Heads** from the main menu (💵 Rate Heads)
2. Click **"➕ Add New Rate Head"**
3. Enter:
   - **Rate Head Name**: Descriptive name (e.g., "Standard Cutting", "Premium Printing")
   - **Operation Type**: Select from Cutting, Printing, or Stitching
   - **Cost**: Enter the cost per unit (in ₹)
   - **Active**: Check to make it available for selection
4. Click **"Save Rate Head"**

### Using Rate Heads in Articles

1. When creating/editing an article, you'll see dropdown menus for:
   - **Cutting Rate Head**
   - **Printing Rate Head**
   - **Stitching Rate Head**
2. Select the appropriate rate head for each operation
3. The current cost is displayed next to each dropdown
4. Costs will automatically update when rate heads are modified

### Updating Rate Costs

1. Go to **Rate Heads** list
2. Click **"Edit"** on the rate head you want to update
3. Change the **Cost** field
4. Click **"Update Rate Head"**
5. **Important**: All future vendor payments for articles using this rate head will use the new cost

### Deactivating Rate Heads

Instead of deleting, you can deactivate rate heads:
1. Click **"Deactivate"** on the rate head
2. Deactivated rate heads won't appear in article dropdowns
3. Existing articles using the rate head remain unaffected
4. Can be reactivated by editing and checking the "Active" checkbox

---

## 💡 Business Logic

### Payment Calculation Flow

1. **Plan Created**: Article is associated with rate heads
2. **Vendor Assignment**: Vendor assigned for an operation
3. **State Transition**: When plan moves to next state
4. **Payment Calculation**:
   ```java
   Article article = articleRepository.findByName(plan.getArticleName());
   int quantity = plan.getTotal();
   double cost = article.getCuttingCost(); // Fetches current rate head cost
   double payment = cost * quantity;
   ```
5. **Vendor Payment**: Calculated amount recorded for vendor

### Printing Type Flexibility

The Plan Edit form allows changing:
- **Cutting Type**: Can be modified before vendor assignment
- **Printing Type**: Can be modified before vendor assignment
- **Rationale**: Printing decisions often made after plan creation but before vendor assignment

---

## 🔄 Migration Strategy

### For Existing Data

1. **Legacy Articles**:
   - Existing articles with direct costs continue to work
   - `getCuttingCost()` returns legacy cost if no rate head assigned
   - Gradually migrate by editing articles and assigning rate heads

2. **Backward Compatibility**:
   - Legacy cost fields maintained in database
   - System checks rate head first, falls back to legacy cost
   - No immediate migration required

3. **Recommended Approach**:
   ```
   1. Create rate heads for all operation types
   2. Edit existing articles one by one
   3. Assign appropriate rate heads
   4. Verify payments calculate correctly
   5. Eventually deprecate legacy cost fields
   ```

---

## 📊 Benefits

### 1. **Centralized Management**
- Single source of truth for operation costs
- Easy to update rates across all articles
- Reduced data redundancy

### 2. **Audit Trail**
- Track when rate changes occurred
- See which articles use which rates
- Historical payment accuracy

### 3. **Flexibility**
- Multiple rate tiers (Standard, Premium, Express)
- Easy to introduce new pricing models
- Operation-specific rate management

### 4. **Automatic Updates**
- Change rate once, affects all future payments
- No need to update individual articles
- Ensures pricing consistency

---

## 🎨 UI Features

### Rate Heads List Page
- ✅ Color-coded operation types
- ✅ Active/Inactive status badges
- ✅ Edit, Deactivate, and Delete actions
- ✅ Information box explaining rate heads
- ✅ Quick link to create new rate head

### Article Forms
- ✅ Rate head dropdowns with current costs displayed
- ✅ Direct link to create new rate head from form
- ✅ Current rate head shown when editing
- ✅ Helpful descriptions and tooltips

### Navigation
- ✅ Rate Heads added to main navigation
- ✅ Added to home page quick actions
- ✅ Consistent across all pages

---

## 🔒 Data Integrity

### Constraints
- Rate head name should be unique per operation type
- Cost must be positive
- Active rate heads required for new articles

### Validation
- Frontend: HTML5 validation on forms
- Backend: Spring validation annotations
- Database: Foreign key constraints

---

## 🚀 Future Enhancements

### Potential Improvements
1. **Rate History**: Track historical rate changes
2. **Effective Dates**: Rates effective from specific dates
3. **Approval Workflow**: Rate changes require approval
4. **Bulk Updates**: Update multiple rate heads at once
5. **Rate Analytics**: Cost trend analysis and reporting
6. **Import/Export**: Bulk rate management via CSV/Excel

---

## 📈 Example Workflow

### Scenario: Rate Increase

1. **Initial State**:
   - Standard Cutting Rate: ₹10.00
   - 10 articles using this rate
   - 5 active plans in progress

2. **Rate Update**:
   - Edit "Standard Cutting" rate head
   - Change cost to ₹12.00
   - Save changes

3. **Impact**:
   - **Existing Plans**: Use old rate (₹10.00) - already calculated
   - **New Plans**: Use new rate (₹12.00) - calculated at transition
   - **All Articles**: Automatically reflect ₹12.00 cost
   - **Future Payments**: Calculated at ₹12.00

4. **Result**:
   - No manual article updates needed
   - Consistent pricing across system
   - Clear audit trail of rate change

---

## 🔗 Integration Points

### With Existing Features

1. **Articles**:
   - Articles reference rate heads
   - Cost methods return current rates
   - Backward compatible with legacy costs

2. **Plans**:
   - Plans indirectly use rates through articles
   - Payment calculation uses current rates
   - Printing type editable before vendor assignment

3. **Vendors**:
   - Vendor payments calculated using current rates
   - Payment history shows amount at time of calculation
   - No retroactive changes to past payments

4. **Stock**:
   - Stock valuation uses current article costs
   - Article costs fetch from rate heads
   - Valuation always reflects current pricing

---

## ✅ Testing Checklist

- [x] Create rate head for each operation type
- [x] Create article using rate heads
- [x] Verify cost displayed correctly
- [x] Update rate head cost
- [x] Create new plan
- [x] Verify payment calculation uses new rate
- [x] Edit plan printing type
- [x] Verify backward compatibility with legacy costs
- [x] Test deactivate/reactivate rate head
- [x] Verify navigation works across all pages

---

**Last Updated:** November 26, 2025
**Status:** ✅ Feature Complete and Tested
**Version:** 1.0.0

